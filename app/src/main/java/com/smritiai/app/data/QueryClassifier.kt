package com.smritiai.app.data

/**
 * QueryClassifier  (Part 4)
 *
 * Decides whether the Gemini API is needed for a query.
 *
 *   shouldUseGemini() == false  →  answer from LocalResponseDatabase + LocalChatEngine + Room DB only
 *   shouldUseGemini() == true   →  call Gemini with RAG context
 *
 * Default = LOCAL (false).  Gemini is called ONLY for genuinely complex tasks.
 */
object QueryClassifier {

    // ── Simple intents that can be answered purely from local database ───────────
    private val LOCAL_SIMPLE_INTENTS = setOf(
        // Greetings
        "hi", "hello", "hey", "namaste", "hii", "helo", "heya", "yo",
        // Basic queries
        "who am i", "how are you", "what can you do", "help",
        // Emotional support
        "sad", "lonely", "worried", "stress", "tired", "depress", "anxious", 
        "feeling down", "upset", "not okay",
        // Motivation
        "motivate", "encourage", "inspire", "need motivation",
        // Health (simple)
        "medicine", "tablet", "pill", "doctor", "sick", "headache", "fever",
        // Safety
        "unsafe", "danger", "emergency", "forgot gas", "door lock",
        // Routine
        "routine", "morning routine", "night routine", "schedule",
        // Farewell
        "bye", "goodbye", "see you", "tata", "chalo",
        // Gratitude
        "thank you", "thanks", "shukriya", "appreciate",
        // Appreciation
        "good job", "great", "awesome", "amazing", "wonderful",
        // Time/Weather
        "weather", "time", "what time"
    )

    // ── Queries answered from Room DB / LocalChatEngine ─────────────────────────
    private val LOCAL_DB_KEYWORDS = setOf(
        // --- person lookup ---
        "who is", "who are", "tell me about", "what is", "what are",
        "show me", "list", "find", "describe",
        // --- relationships ---
        "friend", "best friend", "close friend", "family",
        "sister", "brother", "mother", "father", "son", "daughter",
        "wife", "husband", "uncle", "aunt", "cousin",
        "grandma", "grandpa", "grandmother", "grandfather",
        "neighbor", "neighbour", "colleague", "classmate",
        "doctor", "nurse", "caregiver", "teacher",
        // --- object / location lookups ---
        "where", "where are", "where is", "where did i",
        "keys", "phone", "wallet", "bag", "glasses", "medicine",
        "purse", "charger", "remote", "book",
        // --- time queries ---
        "today", "yesterday", "recent", "last", "latest",
        "remind me", "remember", "memories", "my memories",
        // --- emotion simple ---
        "how was i", "my mood", "how did i feel"
    )

    // ── Queries that genuinely need AI reasoning — send to Gemini ─────────────
    private val COMPLEX_KEYWORDS = setOf(
        // summarisation
        "summarize", "summary", "week", "month", "overview", "recap",
        "all memories", "entire", "everything",
        // comparison / analysis
        "compare", "difference", "similar", "pattern",
        "analyse", "analyze", "insight", "trend",
        "relationship between", "between",
        // emotional support / advice
        "support me", "help me feel", "write me", "write a",
        "suggest", "recommend", "advice",
        "why do i", "how do i", "explain why",
        // combining many records
        "everyone", "all people", "combine", "together", "collective",
        // complex reasoning
        "what should i", "should i", "can you help me",
        "create a plan", "make a schedule"
    )

    /**
     * Returns **true** when Gemini is needed; **false** when local logic suffices.
     *
     * Decision order:
     *  1. Blank query                 → false (local fallback)
     *  2. COMPLEX keyword matched     → true (needs AI)
     *  3. LOCAL_SIMPLE_INTENT matched → false (local response database)
     *  4. LOCAL_DB_KEYWORDS matched   → false (Room DB + LocalChatEngine)
     *  5. Length heuristic            → short (≤ 5 words) = false, longer = true
     */
    fun shouldUseGemini(query: String): Boolean {
        if (query.isBlank()) return false

        val normalised = query.lowercase().trim()

        // Complex intent always wins — these need Gemini reasoning
        if (COMPLEX_KEYWORDS.any { normalised.contains(it) }) return true

        // Simple local intents — use LocalResponseDatabase
        if (LOCAL_SIMPLE_INTENTS.any { normalised.contains(it) }) return false

        // Database lookups — use Room + LocalChatEngine
        if (LOCAL_DB_KEYWORDS.any { normalised.contains(it) }) return false

        // Fallback heuristic: very short queries are almost always simple lookups
        val wordCount = normalised.split("\\s+".toRegex()).size
        return wordCount > 5
    }

    /**
     * Check if query is a simple intent that can be answered from local database only
     * (no Room DB search needed)
     */
    fun isSimpleLocalIntent(query: String): Boolean {
        val normalised = query.lowercase().trim()
        return LOCAL_SIMPLE_INTENTS.any { normalised.contains(it) }
    }

    /**
     * Returns a human-readable explanation of why the query was routed.
     * Useful for debug logs.
     */
    fun explain(query: String): String {
        val normalised = query.lowercase().trim()
        val complex = COMPLEX_KEYWORDS.firstOrNull { normalised.contains(it) }
        val simpleLocal = LOCAL_SIMPLE_INTENTS.firstOrNull { normalised.contains(it) }
        val localDb = LOCAL_DB_KEYWORDS.firstOrNull { normalised.contains(it) }
        return when {
            complex != null -> "Gemini needed — complex keyword: \"$complex\""
            simpleLocal != null -> "Local response — simple intent: \"$simpleLocal\""
            localDb != null -> "Local DB — keyword: \"$localDb\""
            else -> "Heuristic — word count: ${normalised.split(" ").size}"
        }
    }
}
