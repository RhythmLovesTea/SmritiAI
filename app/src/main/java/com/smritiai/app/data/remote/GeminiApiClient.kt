package com.smritiai.app.data.remote

import com.smritiai.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Endpoint paths — keep in sync with GeminiApiService
    const val PATH_20_FLASH  = "v1beta/models/gemini-2.0-flash:generateContent"
    const val PATH_15_FLASH  = "v1beta/models/gemini-1.5-flash-latest:generateContent"

    private const val TAG = "GeminiApiClient"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC // BODY for full request/response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Returns the full URL that will be hit (key masked for logging).
     * Mirrors the path used as the primary endpoint in [GeminiApiService].
     */
    fun getRequestUrl(path: String = PATH_20_FLASH): String {
        val maskedKey = "***${BuildConfig.GEMINI_API_KEY.takeLast(4)}"
        return "${BASE_URL}${path}?key=$maskedKey"
    }
}