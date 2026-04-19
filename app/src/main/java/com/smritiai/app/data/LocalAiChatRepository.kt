package com.smritiai.app.data

import android.util.Log
import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.data.remote.localai.LocalAiApiClient
import com.smritiai.app.data.remote.localai.LocalAiChatContext
import com.smritiai.app.data.remote.localai.LocalAiChatRequest
import com.smritiai.app.data.remote.localai.MemoryContext
import com.smritiai.app.data.remote.localai.PersonContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class LocalAiChatRepository(
    private val memoryRepository: MemoryRepository
) {

    suspend fun chat(
        query: String,
        recognizedPerson: PersonMemory? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext Result.success("")

            val memories = buildRelevantMemories(trimmed)
            val ctx = LocalAiChatContext(
                person = recognizedPerson?.toPersonContext(),
                memories = memories.map { it.toMemoryContext() }
            )
            val response = LocalAiApiClient.apiService.chat(
                LocalAiChatRequest(query = trimmed, context = ctx)
            )
            Result.success(response.reply.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Local AI chat failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun buildRelevantMemories(query: String): List<PersonMemory> {
        // Keep it very lightweight: search with a few keywords.
        val keywords = extractKeywords(query).take(4)
        if (keywords.isEmpty()) return emptyList()

        val out = ArrayList<PersonMemory>()
        for (k in keywords) {
            out.addAll(memoryRepository.searchMemories(k, 5))
        }
        return out.distinctBy { it.id }.take(6)
    }

    private fun extractKeywords(query: String): List<String> {
        val stopWords = setOf(
            "who", "what", "where", "when", "why", "how", "is", "are", "was", "were",
            "the", "a", "an", "in", "on", "at", "to", "for", "of", "and", "or", "but",
            "my", "me", "i", "you", "he", "she", "it", "they", "we",
            "this", "that", "these", "those", "about", "tell", "show", "find", "list",
            "did", "do", "does", "have", "has", "had", "will", "can", "could",
            "please", "hi", "hello", "okay", "yes", "no"
        )
        return query.lowercase()
            .replace(Regex("[^a-zA-Z\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }

    private fun PersonMemory.toPersonContext(): PersonContext {
        return PersonContext(
            name = name,
            relationship = relationship,
            lastVisit = humanLastSeen(timestamp),
            summary = summary,
            emotion = emotion
        )
    }

    private fun PersonMemory.toMemoryContext(): MemoryContext {
        return MemoryContext(
            name = name,
            relationship = relationship,
            summary = summary,
            timestamp = timestamp
        )
    }

    private fun humanLastSeen(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        if (diff <= 0) return "today"
        val days = max(0, (diff / (1000L * 60L * 60L * 24L)).toInt())
        return when {
            days == 0 -> "today"
            days == 1 -> "yesterday"
            days < 7 -> "$days days ago"
            else -> "${days / 7} weeks ago"
        }
    }
}

private const val TAG = "LocalAiChatRepository"

