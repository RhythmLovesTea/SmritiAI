package com.smritiai.app.data.remote

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    @SerializedName("contents") val contents: List<Content>,
    @SerializedName("systemInstruction") val systemInstruction: SystemInstruction? = null
)

data class Content(
    @SerializedName("parts") val parts: List<Part>
)

data class Part(
    @SerializedName("text") val text: String
)

data class SystemInstruction(
    @SerializedName("parts") val parts: List<Part>
)

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>? = null
)

data class Candidate(
    @SerializedName("content") val content: Content? = null,
    @SerializedName("finishReason") val finishReason: String? = null,
    @SerializedName("safetyRatings") val safetyRatings: List<SafetyRating>? = null
)

data class SafetyRating(
    @SerializedName("category") val category: String? = null,
    @SerializedName("probability") val probability: String? = null
)

data class GeminiErrorResponse(
    @SerializedName("error") val error: ErrorDetail? = null
)

data class ErrorDetail(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: List<Any>? = null
)