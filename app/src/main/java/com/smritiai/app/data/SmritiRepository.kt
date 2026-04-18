package com.smritiai.app.data

import android.util.Log
import com.smritiai.app.BuildConfig
import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.data.remote.Content
import com.smritiai.app.data.remote.GeminiApiClient
import com.smritiai.app.data.remote.GeminiRequest
import com.smritiai.app.data.remote.Part
import com.smritiai.app.data.remote.SystemInstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SmritiRepository — optimised for minimal Gemini usage  (Parts 4 & 5)
 *
 * Flow:
 *  1. Search Room DB for relevant memories
 *  2. If no memories → polite local message
 *  3. Classify query → local or complex
 *  4a. Local → LocalChatEngine.answer()  (zero API cost, instant)
 *  4b. Complex → check cache → call Gemini → cache result
 *  5. On 429 / empty key → fall back to local answer
 */
class SmritiRepository(private val memoryRepository: MemoryRepository) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    // ── Gemini response cache  (Part 5 — prevent duplicate calls) ─────────────
    // Key = normalised query string, Value = cached answer
    private val _geminiCache = LinkedHashMap<String, String>(16, 0.75f, true)
    private val CACHE_MAX = 50  // keep at most 50 entries
    private val geminiCache: MutableMap<String, String> = object :
        LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) =
            size > CACHE_MAX
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public entry point
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAnswer(userQuery: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = userQuery.trim()

            // ── STEP 0: Check if it's a simple local intent (no DB needed) ────────
            if (QueryClassifier.isSimpleLocalIntent(trimmedQuery)) {
                SmritiLocalDatabase.matchIntentOrNull(trimmedQuery)?.let { local ->
                    return@withContext Result.success(local)
                }
            }

            // ── STEP 1: search Room DB ────────────────────────────────────────
            val relevantMemories = searchRelevantMemories(trimmedQuery)

            // ── STEP 2: no memories at all → friendly prompt ──────────────────
            if (relevantMemories.isEmpty()) {
                return@withContext Result.success(noMemoryMessage(trimmedQuery))
            }

            // ── STEP 3: classify ──────────────────────────────────────────────
            val useGemini = QueryClassifier.shouldUseGemini(trimmedQuery)
            Log.d(TAG, "Query: \"$trimmedQuery\" | ${QueryClassifier.explain(trimmedQuery)}")

            if (!useGemini) {
                // ── STEP 4a: LOCAL path (most queries) ───────────────────────
                val localAnswer = LocalChatEngine.answer(trimmedQuery, relevantMemories)
                return@withContext Result.success(localAnswer)
            }

            // ── STEP 4b: COMPLEX path — check cache first ─────────────────────
            val cacheKey = trimmedQuery.lowercase().trim()
            geminiCache[cacheKey]?.let { cached ->
                Log.d(TAG, "Cache HIT for: \"$trimmedQuery\"")
                return@withContext Result.success(cached)
            }

            Log.d(TAG, "Cache MISS — calling Gemini for: \"$trimmedQuery\"")
            val context = buildContextFromMemories(relevantMemories)
            val prompt  = buildPrompt(trimmedQuery, context)
            val result  = callGemini(prompt, relevantMemories)

            // Cache successful Gemini responses
            result.onSuccess { answer -> geminiCache[cacheKey] = answer }

            result

        } catch (e: Exception) {
            Log.e(TAG, "getAnswer error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  No-memory messages  (warm, non-robotic — Part 5)
    // ─────────────────────────────────────────────────────────────────────────

    private fun noMemoryMessage(query: String): String {
        val lower = query.lowercase()
        
        // Check intent from local database first
        SmritiLocalDatabase.matchIntentOrNull(query)?.let { return it }
        
        // Category-specific fallback messages
        return when {
            lower.contains("keys") || lower.contains("wallet") ||
            lower.contains("phone") || lower.contains("glasses") ->
                "I haven't saved any memory about where you placed that yet. " +
                "Try recording a voice note the next time you keep it somewhere!"

            LocalSummarizationEngine.detectRelation(lower) != null ->
                "I don't have any saved memories about that person yet. " +
                "Record a voice note to help me remember them for you."

            lower.contains("yesterday") || lower.contains("today") ->
                "I don't see any memories from that time yet. " +
                "Keep recording your thoughts and I'll build your memory diary."

            else ->
                "I couldn't find anything related to that in your memories yet. " +
                "Try recording more. Every memory you add helps me serve you better."
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Memory search helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun searchRelevantMemories(query: String): List<PersonMemory> {
        val keywords = extractKeywords(query)

        if (keywords.isEmpty()) {
            return memoryRepository.getRecentMemories(10)
        }

        val allResults = mutableListOf<PersonMemory>()
        for (keyword in keywords) {
            allResults.addAll(memoryRepository.searchMemories(keyword, 10))
        }
        return allResults.distinctBy { it.id }.take(10)
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Prompt builders (for Gemini complex queries)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildContextFromMemories(memories: List<PersonMemory>): String {
        return memories.joinToString("\n\n") { memory ->
            buildString {
                appendLine("Memory: ${memory.name}")
                appendLine("Relationship: ${memory.relationship}")
                appendLine("Summary: ${memory.summary}")
                memory.transcript?.let { appendLine("Details: $it") }
                memory.emotion?.let { appendLine("Emotion: $it") }
                appendLine("Date: ${dateFormat.format(Date(memory.timestamp))}")
            }
        }
    }

    private fun buildPrompt(query: String, context: String): String {
        return """
You are Smriti AI, a warm and caring memory assistant designed for elderly users.

RULES:
1. Answer ONLY using the provided memory records below
2. If memories don't contain relevant information, say so gently
3. Be warm, concise, and compassionate — never robotic
4. Never make up or hallucinate any information
5. If you mention a person, include their relationship if known
6. Reference specific dates when relevant
7. Keep answers under 150 words unless a longer summary is requested

MEMORY RECORDS:
$context

USER QUESTION: $query

ANSWER (warm, clear, based only on the memories above):
        """.trimIndent()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Gemini API call  (only reached for complex queries)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun callGemini(
        prompt: String,
        memoriesForFallback: List<PersonMemory>
    ): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            Log.w(TAG, "No API key — falling back to local answer")
            return Result.success(LocalChatEngine.answer("", memoriesForFallback))
        }

        Log.d(TAG, "Calling Gemini — prompt length: ${prompt.length} chars")

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = SystemInstruction(
                parts = listOf(
                    Part(
                        text = "You are Smriti AI, a warm and caring memory assistant for elderly users. " +
                               "Answer only using the provided memory records. Be compassionate and concise."
                    )
                )
            )
        )

        // ── Primary model ─────────────────────────────────────────────────────
        try {
            val response = GeminiApiClient.apiService.generateContent20Flash(apiKey, request)
            val answer = response.candidates
                ?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return Result.success(LocalChatEngine.answer("", memoriesForFallback))
            return Result.success(answer.trim())

        } catch (primaryErr: retrofit2.HttpException) {
            Log.e(TAG, "HTTP ${primaryErr.code()} on gemini-2.0-flash")

            // 429 quota — return graceful local answer (Part 5)
            if (primaryErr.code() == 429) {
                Log.w(TAG, "429 rate limit — returning local answer")
                return Result.success(
                    LocalChatEngine.answer("", memoriesForFallback) +
                    "\n\n_Note: AI service is busy right now. This answer is from your local memories._"
                )
            }

            // 404 — try fallback model
            if (primaryErr.code() == 404) {
                Log.w(TAG, "404 — trying fallback model gemini-1.5-flash-latest")
                return try {
                    val fb = GeminiApiClient.apiService.generateContent15Flash(apiKey, request)
                    val answer = fb.candidates
                        ?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: LocalChatEngine.answer("", memoriesForFallback)
                    Result.success(answer.trim())
                } catch (fbErr: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP ${fbErr.code()} on fallback model")
                    if (fbErr.code() == 429) {
                        Result.success(LocalChatEngine.answer("", memoriesForFallback))
                    } else {
                        Result.failure(fbErr)
                    }
                } catch (fbErr: Exception) {
                    Log.e(TAG, "Fallback model error: ${fbErr.message}")
                    Result.failure(fbErr)
                }
            }

            return Result.failure(primaryErr)

        } catch (e: Exception) {
            Log.e(TAG, "Gemini network error: ${e.message}")
            // Network error — local fallback instead of crashing
            return Result.success(
                LocalChatEngine.answer("", memoriesForFallback) +
                "\n\n_Note: Couldn't reach AI service. Showing memory-based answer._"
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cache management
    // ─────────────────────────────────────────────────────────────────────────

    /** Clear all cached Gemini responses (e.g. when new memories are added). */
    fun clearCache() {
        geminiCache.clear()
        Log.d(TAG, "Gemini response cache cleared")
    }

    companion object {
        private const val TAG = "SmritiRepository"
    }
}
