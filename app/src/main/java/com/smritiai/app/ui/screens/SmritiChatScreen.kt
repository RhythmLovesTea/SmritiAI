package com.smritiai.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.smritiai.app.data.model.ChatMessage
import com.smritiai.app.viewmodel.ChatEvent
import com.smritiai.app.viewmodel.ChatUiState
import com.smritiai.app.viewmodel.SmritiChatState
import com.smritiai.app.viewmodel.SmritiChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Suggested questions shown on empty chat screen ────────────────────────────
private val SUGGESTIONS = listOf(
    "Who is my best friend?",
    "Where are my keys?",
    "What happened yesterday?",
    "Tell me about my family",
    "Where did I keep my medicine?",
    "Show recent memories",
    "How was I feeling today?",
    "Tell me about my doctor"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmritiChatScreen(
    viewModel: SmritiChatViewModel,
    onNavigateBack: () -> Unit,
    onRequestFaceRecognition: (String) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // ── Voice input (SpeechRecognizer) ────────────────────────────────────────
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            isListening = true
            speechRecognizer.startListening(intent)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    viewModel.onInputChange(text)
                    viewModel.sendMessage()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // ── Text-to-Speech for assistant replies ─────────────────────────────────
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var lastSpokenMessageId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { }
        engine.language = Locale.US
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    LaunchedEffect(state.uiState) {
        val messages = when (val ui = state.uiState) {
            is ChatUiState.Ready -> ui.messages
            is ChatUiState.Error -> ui.messages
            else -> emptyList()
        }
        val lastBot = messages.lastOrNull { !it.isFromUser }
        if (lastBot != null && lastBot.id != lastSpokenMessageId) {
            lastSpokenMessageId = lastBot.id
            tts?.speak(lastBot.content, TextToSpeech.QUEUE_FLUSH, null, lastBot.id)
        }
    }

    // ── Face-recognition request events ──────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.RequestFaceRecognition -> onRequestFaceRecognition(event.query)
            }
        }
    }

    LaunchedEffect(state.uiState) {
        if (state.uiState is ChatUiState.Ready || state.uiState is ChatUiState.Error) {
            val messageCount = when (val uiState = state.uiState) {
                is ChatUiState.Ready -> uiState.messages.size
                is ChatUiState.Error -> uiState.messages.size
                else -> 0
            }
            if (messageCount > 0) {
                listState.animateScrollToItem(messageCount - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ask Smriti AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Powered by local intelligence",
                            fontSize = 11.sp,
                            color = Color(0xFF34C759)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val uiState = state.uiState) {
                is ChatUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF007AFF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                is ChatUiState.Ready -> {
                    if (uiState.messages.isEmpty()) {
                        EmptyChatState(
                            modifier = Modifier.weight(1f),
                            onSuggestionClick = { viewModel.sendSuggestion(it) }
                        )
                    } else {
                        ChatMessagesList(
                            messages = uiState.messages,
                            listState = listState,
                            isLoading = state.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is ChatUiState.Error -> {
                    ChatMessagesList(
                        messages = uiState.messages,
                        listState = listState,
                        isLoading = state.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    ErrorBanner(
                        message = uiState.message,
                        onRetry = { viewModel.retry() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            ChatInput(
                inputText = state.inputText,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                onMic = {
                    if (isListening) {
                        speechRecognizer.stopListening()
                        isListening = false
                        return@ChatInput
                    }

                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        }
                        isListening = true
                        speechRecognizer.startListening(intent)
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                isListening = isListening,
                isLoading = state.isLoading,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty state — with suggestion chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🧠", fontSize = 52.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ask about your memories",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Most answers are instant — powered by local AI",
            fontSize = 13.sp,
            color = Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Row 1 of suggestions
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SUGGESTIONS.take(4)) { suggestion ->
                SuggestionChip(text = suggestion, onClick = { onSuggestionClick(suggestion) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Row 2
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SUGGESTIONS.drop(4)) { suggestion ->
                SuggestionChip(text = suggestion, onClick = { onSuggestionClick(suggestion) })
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF0F4FF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF007AFF),
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Messages list  (with typing indicator)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatMessagesList(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            ChatBubble(message = message)
        }
        if (isLoading) {
            item { TypingIndicator() }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SmritiAvatar(size = 24.dp, modifier = Modifier.padding(top = 2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .background(Color(0xFFF5F5F7))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(text = "Smriti is thinking…", fontSize = 14.sp, color = Color(0xFF8E8E93))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chat bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    val bubbleColor = if (isUser) Color(0xFF007AFF) else Color(0xFFF5F5F7)
    val textColor   = if (isUser) Color.White else Color(0xFF1C1C1E)
    val boxAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = boxAlignment
    ) {
        if (isUser) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .clip(shape)
                        .background(bubbleColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        fontSize = 15.sp,
                        color = textColor,
                        lineHeight = 22.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                SmritiAvatar(size = 28.dp, modifier = Modifier.padding(top = 2.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Smriti AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .clip(shape)
                            .background(bubbleColor)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = message.content,
                            fontSize = 15.sp,
                            color = textColor,
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = formatTime(message.timestamp),
                            fontSize = 10.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmritiAvatar(
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color(0xFFF2F2F7)
    ) {
        Image(
            painter = painterResource(id = com.smritiai.app.R.drawable.smriti_assistant),
            contentDescription = "Smriti AI",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Error banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF3CD),
        onClick = onDismiss
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF856404),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF856404)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInput(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F7), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMic,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start voice input",
                tint = if (isListening) Color(0xFFFF3B30) else Color(0xFF007AFF)
            )
        }

        TextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            placeholder = {
                Text(
                    text = "Ask about your memories…",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                )
            },
            textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (inputText.isNotBlank() && !isLoading) Color(0xFF007AFF)
                    else Color(0xFFE5E5EA),
                    CircleShape
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) Color.White else Color(0xFF8E8E93)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
