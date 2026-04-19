package com.smritiai.app.data

object ChatIntentRules {

    private val FACE_QUERY_PHRASES = listOf(
        "who is this",
        "who am i talking to",
        "identify person",
        "identify this person",
        "recognize person",
        "recognise person",
        "who are you",
        "who is he",
        "who is she"
    )

    fun isFaceQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        return FACE_QUERY_PHRASES.any { q.contains(it) }
    }
}

