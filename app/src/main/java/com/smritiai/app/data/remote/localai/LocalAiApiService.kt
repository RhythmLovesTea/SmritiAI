package com.smritiai.app.data.remote.localai

import retrofit2.http.Body
import retrofit2.http.POST

interface LocalAiApiService {
    @POST("chat")
    suspend fun chat(@Body request: LocalAiChatRequest): LocalAiChatResponse
}

