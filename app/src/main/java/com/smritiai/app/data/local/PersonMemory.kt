package com.smritiai.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "persons")
@TypeConverters(Converters::class)
data class PersonMemory(
    @PrimaryKey val id: String,
    val name: String,
    val relationship: String,
    val summary: String,
    val imagePath: String?,
    val audioPath: String?,
    val timestamp: Long,
    val transcript: String? = null,
    val emotion: String? = null
)
