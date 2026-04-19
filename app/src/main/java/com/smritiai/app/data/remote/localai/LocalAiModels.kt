package com.smritiai.app.data.remote.localai

import com.google.gson.annotations.SerializedName

data class LocalAiChatRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("context")
    val context: LocalAiChatContext? = null
)

data class LocalAiChatContext(
    @SerializedName("person")
    val person: PersonContext? = null,
    @SerializedName("memories")
    val memories: List<MemoryContext> = emptyList()
)

data class PersonContext(
    @SerializedName("name")
    val name: String,
    @SerializedName("relationship")
    val relationship: String? = null,
    @SerializedName("last_visit")
    val lastVisit: String? = null,
    @SerializedName("summary")
    val summary: String? = null,
    @SerializedName("emotion")
    val emotion: String? = null
)

data class MemoryContext(
    @SerializedName("name")
    val name: String,
    @SerializedName("relationship")
    val relationship: String? = null,
    @SerializedName("summary")
    val summary: String? = null,
    @SerializedName("timestamp")
    val timestamp: Long? = null
)

data class LocalAiChatResponse(
    @SerializedName("reply")
    val reply: String
)

