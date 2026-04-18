package com.smritiai.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "face_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = PersonMemory::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId"]),
        Index(value = ["timestamp"])
    ]
)
@TypeConverters(Converters::class)
data class FaceEmbeddingEntity(
    @PrimaryKey val id: String,
    val personId: String,
    val vector: FloatArray,
    val qualityScore: Float,
    val timestamp: Long,
    val poseType: String
)

