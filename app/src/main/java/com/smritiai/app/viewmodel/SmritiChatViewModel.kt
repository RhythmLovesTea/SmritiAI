package com.smritiai.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smritiai.app.data.ChatIntentRules
import com.smritiai.app.data.LocalAiChatRepository
import com.smritiai.app.data.MemoryRepository
import com.smritiai.app.data.model.ChatMessage
import com.smritiai.app.data.SmritiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ChatUiState {
    data object Loading : ChatUiState()
    data class Ready(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String, val messages: List<ChatMessage>) : ChatUiState()
}

data class SmritiChatState(
    val uiState: ChatUiState = ChatUiState.Ready(emptyList()),
    val isLoading: Boolean = false,
    val inputText: String = ""
)

sealed class ChatEvent {
    data class RequestFaceRecognition(val query: String) : ChatEvent()
}

class SmritiChatViewModel(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val localAiRepository = LocalAiChatRepository(memoryRepository)
    private val fallbackRepository = SmritiRepository(memoryRepository)

    private val _state = MutableStateFlow(SmritiChatState())
    val state: StateFlow<SmritiChatState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var pendingFaceQuery: String? = null
    private var pendingFaceQueryMessageId: String? = null

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val userInput = _state.value.inputText.trim()
        if (userInput.isEmpty()) return

        // Prevent duplicate in-flight requests
        if (_state.value.isLoading) {
            Log.d(TAG, "sendMessage() ignored — request already in progress")
            return
        }

        val userMessage = ChatMessage(content = userInput, isFromUser = true)

        _state.update { currentState ->
            val currentMessages = currentState.messages()
            currentState.copy(
                uiState = ChatUiState.Ready(currentMessages + userMessage),
                inputText = "",
                isLoading = false
            )
        }

        if (ChatIntentRules.isFaceQuery(userInput)) {
            pendingFaceQuery = userInput
            pendingFaceQueryMessageId = userMessage.id
            _events.tryEmit(ChatEvent.RequestFaceRecognition(userInput))
            return
        }

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = localAiRepository.chat(userInput)
            result.fold(
                onSuccess = { answer ->
                    val botMessage = ChatMessage(content = answer, isFromUser = false)
                    _state.update { it.copy(uiState = ChatUiState.Ready(it.messages() + botMessage), isLoading = false) }
                },
                onFailure = { error ->
                    val fallback = fallbackRepository.getAnswer(userInput)
                    fallback.fold(
                        onSuccess = { answer ->
                            val botMessage = ChatMessage(content = answer, isFromUser = false)
                            _state.update {
                                it.copy(uiState = ChatUiState.Ready(it.messages() + botMessage), isLoading = false)
                            }
                        },
                        onFailure = { fbErr ->
                            val msg = fbErr.message ?: error.message ?: "Unknown error"
                            Log.e(TAG, "Error: $msg")
                            _state.update { it.copy(uiState = ChatUiState.Error(msg, it.messages()), isLoading = false) }
                        }
                    )
                }
            )
        }
    }

    fun onFaceRecognitionResult(personId: String?) {
        val query = pendingFaceQuery ?: return
        pendingFaceQuery = null
        pendingFaceQueryMessageId = null

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val person = personId?.let { memoryRepository.getMemoryById(it) }
            val result = localAiRepository.chat(query, recognizedPerson = person)
            result.fold(
                onSuccess = { answer ->
                    val botMessage = ChatMessage(content = answer, isFromUser = false)
                    _state.update { it.copy(uiState = ChatUiState.Ready(it.messages() + botMessage), isLoading = false) }
                },
                onFailure = { error ->
                    val msg = error.message ?: "Could not reach local AI server."
                    _state.update { it.copy(uiState = ChatUiState.Error(msg, it.messages()), isLoading = false) }
                }
            )
        }
    }

    /** Convenience: send a pre-written suggested question. */
    fun sendSuggestion(question: String) {
        _state.update { it.copy(inputText = question) }
        sendMessage()
    }

    fun clearError() {
        _state.update { it.copy(uiState = ChatUiState.Ready(it.messages())) }
    }

    fun retry() {
        val lastUserMessage = when (val uiState = _state.value.uiState) {
            is ChatUiState.Error -> uiState.messages.lastOrNull { it.isFromUser }
            else -> null
        } ?: return

        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val query = lastUserMessage.content
            val result = localAiRepository.chat(query)
            result.fold(
                onSuccess = { answer ->
                    val botMessage = ChatMessage(content = answer, isFromUser = false)
                    _state.update { currentState ->
                        val msgs = when (val s = currentState.uiState) {
                            is ChatUiState.Error -> s.messages
                            else -> emptyList()
                        }
                        currentState.copy(uiState = ChatUiState.Ready(msgs + botMessage), isLoading = false)
                    }
                },
                onFailure = {
                    val fb = fallbackRepository.getAnswer(query)
                    fb.fold(
                        onSuccess = { answer ->
                            val botMessage = ChatMessage(content = answer, isFromUser = false)
                            _state.update { currentState ->
                                val msgs = when (val s = currentState.uiState) {
                                    is ChatUiState.Error -> s.messages
                                    else -> emptyList()
                                }
                                currentState.copy(uiState = ChatUiState.Ready(msgs + botMessage), isLoading = false)
                            }
                        },
                        onFailure = { _state.update { s -> s.copy(isLoading = false) } }
                    )
                }
            )
        }
    }

    /**
     * Clear the Gemini response cache — call this when new memories are added
     * so stale answers don't persist.
     */
    fun clearGeminiCache() {
        fallbackRepository.clearCache()
    }

    // Helper: extract message list from current state regardless of subtype
    private fun SmritiChatState.messages(): List<ChatMessage> = when (val s = uiState) {
        is ChatUiState.Ready   -> s.messages
        is ChatUiState.Error   -> s.messages
        is ChatUiState.Loading -> emptyList()
    }
}

class SmritiChatViewModelFactory(
    private val memoryRepository: MemoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmritiChatViewModel::class.java)) {
            return SmritiChatViewModel(memoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private const val TAG = "SmritiChatViewModel"
