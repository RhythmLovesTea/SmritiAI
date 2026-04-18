package com.smritiai.app.data

import com.google.gson.annotations.SerializedName

data class Person(
    val id: String,
    val name: String,
    val relationship: String,
    val summary: String,
    @SerializedName("image_path") val imagePath: String? = null,
    @SerializedName("audio_path") val audioPath: String? = null,
    @SerializedName("face_embedding") val faceEmbedding: List<Float>? = null,
    @SerializedName("emotion") val emotion: String? = null
)
