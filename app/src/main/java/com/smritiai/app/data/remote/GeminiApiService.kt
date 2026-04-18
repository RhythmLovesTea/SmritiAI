package com.smritiai.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {

    /** Primary: Gemini 2.0 Flash */
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent20Flash(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    /** Fallback: Gemini 1.5 Flash */
    @POST("v1beta/models/gemini-1.5-flash-latest:generateContent")
    suspend fun generateContent15Flash(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}