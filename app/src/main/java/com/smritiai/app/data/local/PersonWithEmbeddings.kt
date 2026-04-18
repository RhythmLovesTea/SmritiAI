package com.smritiai.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PersonWithEmbeddings(
    @Embedded val person: PersonMemory,
    @Relation(
        parentColumn = "id",
        entityColumn = "personId",
        entity = FaceEmbeddingEntity::class
    )
    val embeddings: List<FaceEmbeddingEntity>
)

