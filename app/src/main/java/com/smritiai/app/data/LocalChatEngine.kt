package com.smritiai.app.data

import com.smritiai.app.data.local.PersonMemory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LocalChatEngine  (Part 3)
 *
 * Answers common user questions about their saved memories without any
 * network call.  Uses Room results + keyword reasoning to produce warm,
 * natural-language answers.
 */
object LocalChatEngine {

    private val dateFormat = SimpleDateFormat("EEEE, MMM dd 'at' hh:mm a", Locale.getDefault())
    private val shortDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // ── Question-intent patterns ──────────────────────────────────────────────
    private val PERSON_LOOKUP = setOf(
        "who is", "who are", "tell me about", "what do you know about",
        "describe", "show me"
    )
    private val BEST_FRIEND_LOOKUP = setOf(
        "best friend", "closest friend", "dearest friend", "favourite person",
        "favorite person"
    )
    private val FAMILY_LOOKUP = setOf("family", "relatives", "parents", "siblings")
    private val OBJECT_LOOKUP = setOf(
        "where", "placed", "kept", "stored", "put", "left"
    )
    private val RECENT_LOOKUP = setOf(
        "recent", "latest", "last", "what happened", "today", "yesterday"
    )
    private val HEALTH_LOOKUP = setOf(
        "medicine", "tablet", "pill", "doctor", "appointment",
        "medication", "health reminder"
    )
    private val EMOTION_LOOKUP = setOf(
        "how was i feeling", "my mood", "emotional", "how did i feel"
    )

    /**
     * Generate a local answer from [memories] for the given [query].
     *
     * @param query        The user's question.
     * @param memories     Memories already retrieved by the search layer.
     * @return             A warm, human-friendly answer string.
     */
    fun answer(query: String, memories: List<PersonMemory>): String {
        val lower = query.lowercase().trim()

        // ── Best-friend lookup ────────────────────────────────────────────────
        if (BEST_FRIEND_LOOKUP.any { lower.contains(it) }) {
            return answerBestFriend(memories)
        }

        // ── Object / location lookup ──────────────────────────────────────────
        val obj = LocalSummarizationEngine.detectObject(lower)
        if (obj != null || OBJECT_LOOKUP.any { lower.contains(it) }) {
            return answerObjectLocation(lower, obj, memories)
        }

        // ── Health / medicine query ───────────────────────────────────────────
        if (HEALTH_LOOKUP.any { lower.contains(it) }) {
            return answerHealthQuery(memories)
        }

        // ── Recent memories ───────────────────────────────────────────────────
        if (RECENT_LOOKUP.any { lower.contains(it) }) {
            return answerRecentMemories(lower, memories)
        }

        // ── Emotion / mood query ──────────────────────────────────────────────
        if (EMOTION_LOOKUP.any { lower.contains(it) }) {
            return answerEmotionQuery(memories)
        }

        // ── Person-specific lookup ────────────────────────────────────────────
        if (PERSON_LOOKUP.any { lower.contains(it) }) {
            return answerPersonLookup(query, lower, memories)
        }

        // ── Relationship category lookup (family, friends…) ───────────────────
        val relation = LocalSummarizationEngine.detectRelation(lower)
        if (relation != null) {
            return answerByRelation(relation, memories)
        }

        // ── Generic multi-memory list ─────────────────────────────────────────
        return answerGenericList(memories)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Answer builders
    // ─────────────────────────────────────────────────────────────────────────

    private fun answerBestFriend(memories: List<PersonMemory>): String {
        // Score candidates: "best friend" > positive emotion > positive quality
        val ranked = memories.sortedWith(
            Comparator { a, b ->
                score(b) - score(a)
            }
        )
        val top = ranked.firstOrNull()
            ?: return "I haven't found a best friend mentioned in your memories yet. " +
                       "Try adding memories about the people closest to you."

        val dateStr = shortDate.format(Date(top.timestamp))
        val emo = if (top.emotion == "Positive") " — you feel very positively about them" else ""
        return "Based on your saved memories, **${top.name}** appears to be someone " +
               "very close and important to you$emo.\n" +
               "Last remembered on $dateStr." +
               if (top.summary.isNotBlank()) "\n\n${top.summary}" else ""
    }

    private fun score(m: PersonMemory): Int {
        var s = 0
        val lower = "${m.summary} ${m.transcript ?: ""} ${m.relationship}".lowercase()
        if (lower.contains("best friend")) s += 10
        if (lower.contains("close friend") || lower.contains("friend")) s += 5
        if (m.emotion == "Positive") s += 3
        if (lower.contains("love") || lower.contains("dear")) s += 2
        return s
    }

    private fun answerObjectLocation(
        lower: String,
        obj: String?,
        memories: List<PersonMemory>
    ): String {
        if (memories.isEmpty()) {
            val objName = obj ?: "that item"
            return "I don't have a saved memory about where your $objName are. " +
                   "Try recording a memory the next time you place them somewhere."
        }
        val top = memories.first()
        val summary = top.summary.ifBlank { top.transcript ?: "" }
        val dateStr = shortDate.format(Date(top.timestamp))
        val objName = obj ?: "item"
        return "Based on your last saved memory:\n\n" +
               "Your $objName — **${summary.trim()}**\n" +
               "This was noted on $dateStr."
    }

    private fun answerHealthQuery(memories: List<PersonMemory>): String {
        val healthMemories = memories.filter {
            LocalSummarizationEngine.isHealthRelated("${it.summary} ${it.transcript ?: ""}".lowercase())
        }.sortedByDescending { it.timestamp }

        if (healthMemories.isEmpty() && memories.isEmpty()) {
            return "No health-related memories found yet. You can record doctor visits, medications, or appointments."
        }
        val display = if (healthMemories.isNotEmpty()) healthMemories else memories
        val sb = StringBuilder("Here are your health-related memories:\n\n")
        display.take(5).forEach { m ->
            val dateStr = shortDate.format(Date(m.timestamp))
            sb.append("• **${m.name}** — ${m.summary.ifBlank { "Health note" }}\n")
            sb.append("  Saved: $dateStr\n\n")
        }
        return sb.toString().trimEnd()
    }

    private fun answerRecentMemories(lower: String, memories: List<PersonMemory>): String {
        if (memories.isEmpty()) {
            return "You haven't saved any memories yet. Start recording to build your memory diary."
        }
        val isYesterday = lower.contains("yesterday")
        val isToday = lower.contains("today")

        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val filtered = when {
            isToday -> memories.filter { now - it.timestamp < oneDayMs }
            isYesterday -> memories.filter { (now - it.timestamp) in oneDayMs..(2 * oneDayMs) }
            else -> memories
        }

        val display = filtered.ifEmpty { memories }
        val timeLabel = when {
            isToday -> "today"
            isYesterday -> "yesterday"
            else -> "recently"
        }

        val sb = StringBuilder("Here's what you remembered $timeLabel:\n\n")
        display.take(5).forEach { m ->
            val dateStr = dateFormat.format(Date(m.timestamp))
            sb.append("• **${m.name}**")
            if (m.relationship.isNotBlank()) sb.append(" (${m.relationship})")
            sb.append("\n  ${m.summary.ifBlank { m.transcript?.take(80) ?: "Memory saved" }}\n")
            sb.append("  🕐 $dateStr\n\n")
        }
        if (filtered.isEmpty() && isYesterday) {
            sb.clear()
            sb.append("I don't see specific memories from yesterday.\n\n")
            sb.append("Your most recent memory is:\n\n")
            memories.firstOrNull()?.let { m ->
                sb.append("• **${m.name}** — ${m.summary.ifBlank { "Memory saved" }}\n")
                sb.append("  Saved: ${dateFormat.format(Date(m.timestamp))}")
            }
        }
        return sb.toString().trimEnd()
    }

    private fun answerEmotionQuery(memories: List<PersonMemory>): String {
        val withEmotion = memories.filter { !it.emotion.isNullOrBlank() }
        if (withEmotion.isEmpty()) {
            return "I haven't detected any specific emotional notes in your memories yet."
        }
        val moodCounts = withEmotion.groupBy { it.emotion }
        val dominant = moodCounts.maxByOrNull { it.value.size }?.key ?: "Neutral"
        val total = withEmotion.size
        val positiveCount = moodCounts["Positive"]?.size ?: 0
        val negativeCount = moodCounts["Negative"]?.size ?: 0
        return "Based on your saved memories:\n\n" +
               "• Total emotional memories: **$total**\n" +
               "• Positive moments: **$positiveCount**\n" +
               "• Low moments: **$negativeCount**\n\n" +
               "Your overall mood pattern appears to be **$dominant**. \uD83C\uDF1F\n\n" +
               "Would you like to add more memories to help track how you're feeling over time?"
    }

    private fun answerPersonLookup(
        original: String,
        lower: String,
        memories: List<PersonMemory>
    ): String {
        if (memories.isEmpty()) {
            return "I couldn't find anyone by that name in your saved memories yet."
        }
        if (memories.size == 1) {
            return buildDetailedPersonAnswer(memories.first())
        }
        val sb = StringBuilder("Here's what I found in your memories:\n\n")
        memories.take(5).forEach { m ->
            val dateStr = shortDate.format(Date(m.timestamp))
            sb.append("• **${m.name}**")
            if (m.relationship.isNotBlank()) sb.append(" — ${m.relationship}")
            sb.append("\n  ${m.summary.ifBlank { "Saved memory" }}\n")
            sb.append("  Added: $dateStr\n\n")
        }
        return sb.toString().trimEnd()
    }

    private fun buildDetailedPersonAnswer(m: PersonMemory): String {
        val sb = StringBuilder()
        sb.append("**${m.name}**")
        if (m.relationship.isNotBlank()) sb.append(" — ${m.relationship}")
        sb.append("\n\n")
        if (m.summary.isNotBlank()) sb.append("${m.summary}\n\n")
        m.emotion?.let {
            val emoIcon = when (it) {
                "Positive" -> "😊"
                "Negative" -> "😔"
                else -> "😐"
            }
            sb.append("Mood: $emoIcon $it\n")
        }
        sb.append("Saved on: ${dateFormat.format(Date(m.timestamp))}")
        return sb.toString()
    }

    private fun answerByRelation(relation: String, memories: List<PersonMemory>): String {
        if (memories.isEmpty()) {
            return "I haven't found any memories about your ${relation.lowercase()} yet."
        }
        val sb = StringBuilder("Here are memories about your ${relation.lowercase()}:\n\n")
        memories.take(5).forEach { m ->
            val dateStr = shortDate.format(Date(m.timestamp))
            sb.append("• **${m.name}**\n")
            sb.append("  ${m.summary.ifBlank { "Saved memory" }}\n")
            sb.append("  Last noted: $dateStr\n\n")
        }
        return sb.toString().trimEnd()
    }

    private fun answerGenericList(memories: List<PersonMemory>): String {
        if (memories.isEmpty()) {
            return "I couldn't find relevant memories for your question. " +
                   "Try asking about a specific person, object, or recent event."
        }
        val sb = StringBuilder("Based on your saved memories:\n\n")
        memories.take(5).forEach { m ->
            val dateStr = shortDate.format(Date(m.timestamp))
            sb.append("• **${m.name}**")
            if (m.relationship.isNotBlank()) sb.append(" (${m.relationship})")
            sb.append("\n  ${m.summary.ifBlank { "Memory saved" }}\n")
            m.emotion?.let { sb.append("  Feeling: $it\n") }
            sb.append("  $dateStr\n\n")
        }
        if (memories.size > 5) sb.append("…and ${memories.size - 5} more.\n")
        return sb.toString().trimEnd()
    }
}
