package com.smritiai.app.data

/**
 * LocalSummarizationEngine
 *
 * Converts raw voice transcripts into warm, intelligent-sounding summaries
 * using pure rule-based keyword analysis — zero API calls, works fully offline.
 *
 * Covers: relationships, emotions, health, objects, locations, time,
 *         actions, and personal-value words.
 */
object LocalSummarizationEngine {

    // ─────────────────────────────────────────────────────────────────────────
    //  RULE DATABASE  (Part 2)
    // ─────────────────────────────────────────────────────────────────────────

    // RELATIONSHIPS
    private val CLOSE_FAMILY = setOf(
        "mother", "mom", "mummy", "maa", "father", "dad", "papa",
        "brother", "sister", "son", "daughter", "wife", "husband",
        "grandfather", "grandmother", "grandpa", "grandma", "nana", "dadi", "nani"
    )
    private val EXTENDED_FAMILY = setOf(
        "uncle", "aunt", "aunty", "cousin", "nephew", "niece",
        "father-in-law", "mother-in-law", "sister-in-law", "brother-in-law"
    )
    private val SOCIAL_RELATIONS = setOf(
        "friend", "best friend", "close friend", "childhood friend",
        "neighbour", "neighbor", "colleague", "classmate", "roommate"
    )
    private val PROFESSIONAL = setOf(
        "doctor", "nurse", "teacher", "professor", "caregiver", "helper",
        "driver", "cook", "maid", "manager", "boss", "officer"
    )
    private val ALL_RELATIONS = CLOSE_FAMILY + EXTENDED_FAMILY + SOCIAL_RELATIONS + PROFESSIONAL

    // EMOTIONS
    private val POSITIVE_EMOTIONS = setOf(
        "happy", "happiness", "joy", "joyful", "excited", "excited",
        "love", "loved", "grateful", "thankful", "content", "peaceful",
        "proud", "cheerful", "smile", "smiling", "laugh", "laughing",
        "glad", "delighted", "relief", "relieved"
    )
    private val NEGATIVE_EMOTIONS = setOf(
        "sad", "sadness", "unhappy", "crying", "cry", "tears",
        "angry", "anger", "frustrated", "upset", "annoyed",
        "worried", "worry", "anxious", "anxiety", "scared", "fear",
        "lonely", "alone", "depressed", "hopeless", "stressed", "stress",
        "hurt", "pain", "miss", "missing"
    )
    private val NEUTRAL_EMOTIONS = setOf(
        "calm", "okay", "fine", "normal", "usual", "alright"
    )

    // HEALTH
    private val HEALTH_WORDS = setOf(
        "medicine", "tablet", "pill", "capsule", "medication", "dose",
        "doctor", "hospital", "clinic", "appointment", "checkup", "check-up",
        "pain", "fever", "headache", "cough", "cold", "sick", "ill", "illness",
        "surgery", "operation", "test", "blood test", "bp", "pressure",
        "injection", "vaccine"
    )

    // OBJECTS
    private val OBJECTS = mapOf(
        "keys" to "keys", "key" to "keys",
        "wallet" to "wallet", "purse" to "purse",
        "phone" to "phone", "mobile" to "phone",
        "glasses" to "glasses", "spectacles" to "glasses",
        "bag" to "bag", "handbag" to "bag",
        "medicine" to "medicine", "tablet" to "medicine",
        "book" to "book", "diary" to "diary",
        "charger" to "charger", "remote" to "remote"
    )

    // LOCATIONS
    private val LOCATIONS = mapOf(
        "home" to "home", "house" to "home",
        "bedroom" to "bedroom", "room" to "room",
        "kitchen" to "kitchen", "hall" to "hall",
        "bathroom" to "bathroom", "toilet" to "bathroom",
        "market" to "market", "shop" to "shop", "store" to "store",
        "school" to "school", "college" to "college",
        "office" to "office", "work" to "office",
        "hospital" to "hospital", "clinic" to "clinic",
        "temple" to "temple", "church" to "church", "mosque" to "mosque",
        "park" to "park", "garden" to "garden",
        "road" to "road", "street" to "street"
    )

    // TIME REFERENCES
    private val TIME_WORDS = mapOf(
        "today" to "today",
        "yesterday" to "yesterday",
        "tomorrow" to "tomorrow",
        "morning" to "this morning",
        "evening" to "this evening",
        "night" to "at night",
        "monday" to "on Monday", "tuesday" to "on Tuesday",
        "wednesday" to "on Wednesday", "thursday" to "on Thursday",
        "friday" to "on Friday", "saturday" to "on Saturday",
        "sunday" to "on Sunday",
        "last week" to "last week", "next week" to "next week",
        "last month" to "last month"
    )

    // ACTIONS
    private val POSITIVE_ACTIONS = setOf(
        "helped", "help", "gave", "given", "supported", "support",
        "visited", "visit", "called", "call", "met", "meet", "found", "remembered",
        "cooked", "made", "brought", "bought", "taken care", "took care",
        "loved", "hugged", "talked", "spoke", "said", "told"
    )
    private val NEGATIVE_ACTIONS = setOf(
        "lost", "forgot", "forgave", "missed", "left", "dropped",
        "broke", "fallen", "fell"
    )
    private val REMINDER_ACTIONS = setOf(
        "kept", "placed", "put", "stored", "left", "need", "need to", "should",
        "must", "have to", "reminder", "remember to", "don't forget"
    )
    private val ALL_ACTIONS = POSITIVE_ACTIONS + NEGATIVE_ACTIONS + REMINDER_ACTIONS

    // PERSONAL VALUE / QUALITY WORDS
    private val POSITIVE_QUALITIES = setOf(
        "good", "kind", "best", "favourite", "favorite", "important",
        "helpful", "caring", "loving", "wonderful", "amazing", "great",
        "nice", "sweet", "generous", "honest", "sincere", "gentle",
        "smart", "intelligent", "brave", "strong"
    )
    private val NEGATIVE_QUALITIES = setOf(
        "bad", "rude", "angry", "selfish", "cruel", "mean",
        "lazy", "difficult", "stubborn"
    )

    // URGENCY / REMINDER
    private val URGENCY_WORDS = setOf(
        "important", "urgent", "must", "need to", "remember", "don't forget",
        "take", "schedule", "appointment", "deadline", "tomorrow"
    )

    // ─────────────────────────────────────────────────────────────────────────
    //  MAIN ENTRY: summarize a raw transcript
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convert raw voice transcript → warm, concise, AI-quality summary.
     * Returns null only when the transcript is blank.
     */
    fun summarize(transcript: String): String? {
        if (transcript.isBlank()) return null
        val text = transcript.trim()
        val lower = text.lowercase()

        // ── 1. Try specific pattern matching first ────────────────────────────
        val patternSummary = matchPatterns(text, lower)
        if (patternSummary != null) return patternSummary

        // ── 2. Category-driven summary ────────────────────────────────────────
        return buildCategorySummary(text, lower)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PATTERN MATCHING  (covers example cases from spec)
    // ─────────────────────────────────────────────────────────────────────────

    private fun matchPatterns(original: String, lower: String): String? {
        val timeRef = detectTime(lower)
        val location = detectLocation(lower)
        val detectedObject = detectObject(lower)

        // ── OBJECT PLACEMENT PATTERN ──────────────────────────────────────────
        // "I kept my keys near the sofa" → "Keys were placed near the sofa."
        if (REMINDER_ACTIONS.any { lower.contains(it) } && detectedObject != null) {
            val prepositions = listOf("near", "beside", "next to", "on", "in", "under", "behind", "at", "by", "inside")
            val locationPhrase = prepositions.firstNotNullOfOrNull { prep ->
                if (lower.contains(prep)) {
                    val idx = lower.indexOf(prep)
                    original.substring(idx).trim().replaceFirstChar { it.uppercase() }
                } else null
            }
            if (locationPhrase != null) {
                return "${detectedObject.replaceFirstChar { it.uppercase() }} were placed $locationPhrase."
            }
            if (location != null) {
                return "${detectedObject.replaceFirstChar { it.uppercase() }} were placed at the $location."
            }
        }

        // ── HEALTH / MEDICINE REMINDER PATTERN ───────────────────────────────
        // "doctor told me to take medicine at night" → "Medical reminder noted: take medicine at night."
        if (HEALTH_WORDS.any { lower.contains(it) } &&
            (lower.contains("take") || lower.contains("tablet") || lower.contains("pill") ||
             lower.contains("medicine") || lower.contains("medication"))
        ) {
            val action = extractHealthInstruction(lower)
            return "Medical reminder noted: $action"
        }

        // ── MEETING SOMEONE PATTERN ───────────────────────────────────────────
        // "I met Rahul yesterday in market" → "Met Rahul yesterday at the market."
        if (lower.contains("met ") || lower.contains("meet ")) {
            val name = extractProperName(original, lower)
            val timeStr = if (timeRef != null) " $timeRef" else ""
            val locStr = if (location != null) " at the $location" else ""
            val nameStr = if (name != null) " $name" else " someone"
            return "Met$nameStr$timeStr$locStr."
        }

        // ── EMOTIONAL STATE PATTERN ───────────────────────────────────────────
        // "I feel sad today" → "Emotional note recorded: feeling sad today."
        val emotion = detectEmotion(lower)
        if ((lower.contains("feel ") || lower.contains("feeling ") ||
             lower.contains("i am ") || lower.contains("i'm ")) && emotion != null
        ) {
            val timeStr = if (timeRef != null) " $timeRef" else ""
            val sentiment = if (POSITIVE_EMOTIONS.any { lower.contains(it) }) "positive" else "low"
            return "Emotional note recorded: feeling ${emotion.lowercase()}$timeStr. Mood appears $sentiment."
        }

        // ── RELATIONSHIP + QUALITY DESCRIPTION ───────────────────────────────
        // "my best friend he is very good person" → "Close friend remembered warmly."
        val relation = detectRelation(lower)
        val quality = detectQuality(lower)
        val name = extractProperName(original, lower)
        if (relation != null && quality != null) {
            val nameStr = if (name != null) " $name" else ""
            return buildRelationshipSummary(relation, quality, nameStr, original, lower)
        }

        // ── HELP / SUPPORT ACTION PATTERN ────────────────────────────────────
        // "my mother helped me when I was sick" → "Mother remembered as caring and supportive during illness."
        if (relation != null && POSITIVE_ACTIONS.any { lower.contains(it) }) {
            val actionWord = POSITIVE_ACTIONS.filter { lower.contains(it) }.maxByOrNull { it.length } ?: "helped"
            val context = extractHelpContext(lower)
            val nameStr = if (name != null) " ($name)" else ""
            val rLabel = humaniseRelation(relation)
            return "$rLabel$nameStr remembered as caring and supportive${if (context.isNotBlank()) " $context" else ""}."
        }

        // ── CUSTOM TEACHER / MENTOR PATTERN ──────────────────────────────────
        if ((lower.contains("teacher") || lower.contains("professor") || lower.contains("mentor")) &&
            POSITIVE_QUALITIES.any { lower.contains(it) }
        ) {
            return "Teacher remembered as motivating and supportive."
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CATEGORY-DRIVEN SUMMARY  (generic fallback)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildCategorySummary(original: String, lower: String): String {
        val parts = mutableListOf<String>()

        val name = extractProperName(original, lower)
        val relation = detectRelation(lower)
        val emotion = detectEmotion(lower)
        val timeRef = detectTime(lower)
        val location = detectLocation(lower)
        val detectedObject = detectObject(lower)
        val quality = detectQuality(lower)
        val isHealth = HEALTH_WORDS.any { lower.contains(it) }
        val isUrgent = URGENCY_WORDS.any { lower.contains(it) }
        val actionWord = (POSITIVE_ACTIONS + NEGATIVE_ACTIONS + REMINDER_ACTIONS)
            .filter { lower.contains(it) }
            .maxByOrNull { it.length }

        // Start building the sentence
        val subjectParts = mutableListOf<String>()
        if (relation != null) subjectParts.add(humaniseRelation(relation))
        if (name != null && relation == null) subjectParts.add(name)
        else if (name != null) subjectParts.add("($name)")

        val subject = if (subjectParts.isNotEmpty()) subjectParts.joinToString(" ") else null

        // HEALTH note
        if (isHealth) {
            val prefix = if (isUrgent) "Important health reminder" else "Health note"
            val timeStr = if (timeRef != null) " — $timeRef" else ""
            val locStr = if (location != null) " at $location" else ""
            parts.add("$prefix recorded$timeStr$locStr.")
        }

        // OBJECT location
        if (detectedObject != null) {
            val timeStr = if (timeRef != null) " $timeRef" else ""
            parts.add("${detectedObject.replaceFirstChar { it.uppercase() }} noted as stored$timeStr.")
        }

        // PERSON description
        if (subject != null) {
            val qualStr = if (quality != null) " as ${qualityPhrase(quality)}" else " warmly"
            val timeStr = if (timeRef != null) " — $timeRef" else ""
            val emotStr = if (emotion != null) " Mood: ${emotion.lowercase()}." else ""
            parts.add("$subject remembered$qualStr$timeStr.$emotStr")
        }

        // PURE EMOTION (no subject)
        if (subject == null && emotion != null) {
            val timeStr = if (timeRef != null) " $timeRef" else ""
            parts.add("Personal emotional note: feeling ${emotion.lowercase()}$timeStr.")
        }

        // ACTION fallback
        if (parts.isEmpty() && actionWord != null) {
            val timeStr = if (timeRef != null) " $timeRef" else ""
            val locStr = if (location != null) " at the $location" else ""
            parts.add("Memory recorded: $actionWord$timeStr$locStr.")
        }

        // Generic fallback
        if (parts.isEmpty()) {
            val timeStr = if (timeRef != null) " recorded $timeRef" else ""
            parts.add("Personal memory$timeStr.")
        }

        return parts.joinToString(" ").trim()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DETECT helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun detectEmotion(lower: String): String? {
        return when {
            POSITIVE_EMOTIONS.any { lower.contains(it) } -> {
                POSITIVE_EMOTIONS.firstOrNull { lower.contains(it) }
                    ?.replaceFirstChar { it.uppercase() }
            }
            NEGATIVE_EMOTIONS.any { lower.contains(it) } -> {
                NEGATIVE_EMOTIONS.firstOrNull { lower.contains(it) }
                    ?.replaceFirstChar { it.uppercase() }
            }
            NEUTRAL_EMOTIONS.any { lower.contains(it) } -> "Calm"
            else -> null
        }
    }

    fun detectRelation(lower: String): String? {
        // Check multi-word first
        val multiWord = listOf("best friend", "close friend", "childhood friend",
            "father-in-law", "mother-in-law", "sister-in-law", "brother-in-law",
            "taken care")
        val mwMatch = multiWord.firstOrNull { lower.contains(it) }
        if (mwMatch != null) return mwMatch

        return ALL_RELATIONS.firstOrNull { lower.contains("$it ") || lower.endsWith(it) }
    }

    fun detectTime(lower: String): String? {
        return TIME_WORDS.entries.firstOrNull { lower.contains(it.key) }?.value
    }

    fun detectLocation(lower: String): String? {
        return LOCATIONS.entries.firstOrNull { lower.contains(it.key) }?.value
    }

    fun detectObject(lower: String): String? {
        return OBJECTS.entries.firstOrNull { lower.contains(it.key) }?.value
    }

    fun detectQuality(lower: String): String? {
        return when {
            POSITIVE_QUALITIES.any { lower.contains(it) } ->
                POSITIVE_QUALITIES.firstOrNull { lower.contains(it) }
            NEGATIVE_QUALITIES.any { lower.contains(it) } ->
                NEGATIVE_QUALITIES.firstOrNull { lower.contains(it) }
            else -> null
        }
    }

    fun isHealthRelated(lower: String): Boolean = HEALTH_WORDS.any { lower.contains(it) }

    fun isUrgent(lower: String): Boolean = URGENCY_WORDS.any { lower.contains(it) }

    // ─────────────────────────────────────────────────────────────────────────
    //  STRING helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun humaniseRelation(relation: String): String {
        return when (relation) {
            "mother", "mom", "mummy", "maa" -> "Mother"
            "father", "dad", "papa" -> "Father"
            "brother" -> "Brother"
            "sister" -> "Sister"
            "wife" -> "Wife"
            "husband" -> "Husband"
            "son" -> "Son"
            "daughter" -> "Daughter"
            "grandmother", "grandma", "nani", "dadi" -> "Grandmother"
            "grandfather", "grandpa", "nana" -> "Grandfather"
            "uncle" -> "Uncle"
            "aunt", "aunty" -> "Aunt"
            "best friend" -> "Best friend"
            "close friend", "friend" -> "Friend"
            "childhood friend" -> "Childhood friend"
            "doctor" -> "Doctor"
            "nurse" -> "Nurse"
            "teacher", "professor" -> "Teacher"
            "caregiver" -> "Caregiver"
            "colleague" -> "Colleague"
            else -> relation.replaceFirstChar { it.uppercase() }
        }
    }

    private fun buildRelationshipSummary(
        relation: String,
        quality: String,
        nameStr: String,
        original: String,
        lower: String
    ): String {
        val rLabel = humaniseRelation(relation)
        val isPositive = POSITIVE_QUALITIES.contains(quality)
        val warmth = if (isPositive) "warmly and positively" else "with mixed feelings"
        val timeRef = detectTime(lower)
        val timeStr = if (timeRef != null) " — $timeRef" else ""
        return "$rLabel$nameStr remembered $warmth$timeStr."
    }

    private fun qualityPhrase(quality: String): String {
        return when {
            POSITIVE_QUALITIES.contains(quality) -> "someone warm and $quality"
            NEGATIVE_QUALITIES.contains(quality) -> "someone difficult"
            else -> "someone important"
        }
    }

    private fun extractProperName(original: String, lower: String): String? {
        // Grab capitalised words that are likely names (not stop-words)
        val stopWords = setOf(
            "I", "My", "The", "A", "An", "He", "She", "It", "We", "They",
            "This", "That", "Today", "Yesterday", "Tomorrow", "Morning",
            "Evening", "Night", "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday"
        )
        val words = original.split("\\s+".toRegex())
        return words.firstOrNull { w ->
            w.length > 2 && w[0].isUpperCase() && w !in stopWords &&
                !ALL_RELATIONS.contains(w.lowercase()) &&
                !HEALTH_WORDS.contains(w.lowercase())
        }?.replace(Regex("[^A-Za-z]"), "")
    }

    private fun extractHealthInstruction(lower: String): String {
        // Try to grab "take X at Y" type phrases
        val takeIdx = lower.indexOf("take")
        if (takeIdx >= 0) {
            val phrase = lower.substring(takeIdx).trim()
            return phrase.replaceFirstChar { it.uppercase() }.trimEnd('.') + "."
        }
        return "follow medical instructions as advised."
    }

    private fun extractHelpContext(lower: String): String {
        return when {
            lower.contains("sick") || lower.contains("ill") || lower.contains("fever") -> "during illness"
            lower.contains("study") || lower.contains("exam") -> "during studies"
            lower.contains("difficult") || lower.contains("hard time") -> "during difficult times"
            lower.contains("problem") -> "through a difficult situation"
            lower.contains("lonely") || lower.contains("alone") -> "during lonely moments"
            else -> ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EMOTION DETECTION for MemoryViewModel (public wrapper)
    // ─────────────────────────────────────────────────────────────────────────

    fun detectEmotionLabel(text: String): String {
        if (text.isBlank()) return "Neutral"
        val lower = text.lowercase()
        return when {
            POSITIVE_EMOTIONS.any { lower.contains(it) } -> "Positive"
            NEGATIVE_EMOTIONS.any { lower.contains(it) } -> "Negative"
            // Health or urgent → "Concern"
            HEALTH_WORDS.any { lower.contains(it) } -> "Concern"
            else -> "Neutral"
        }
    }
}
