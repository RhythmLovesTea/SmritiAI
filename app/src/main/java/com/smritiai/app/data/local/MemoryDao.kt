package com.smritiai.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM persons ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<PersonMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: PersonMemory)

    @Query("SELECT * FROM persons WHERE name LIKE :query LIMIT 1")
    suspend fun findByName(query: String): PersonMemory?

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: String): PersonMemory?

    @Query("SELECT * FROM persons")
    suspend fun getAllMemoriesSync(): List<PersonMemory>

    @Transaction
    @Query("SELECT * FROM persons ORDER BY timestamp DESC")
    suspend fun getAllPersonsWithEmbeddings(): List<PersonWithEmbeddings>

    @Delete
    suspend fun deleteMemory(memory: PersonMemory)

    @Query("DELETE FROM face_embeddings WHERE personId = :personId")
    suspend fun deleteEmbeddingsForPerson(personId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceEmbedding(embedding: FaceEmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceEmbeddings(embeddings: List<FaceEmbeddingEntity>)

    @Query("SELECT * FROM face_embeddings WHERE personId = :personId ORDER BY timestamp DESC")
    suspend fun getFaceEmbeddingsForPerson(personId: String): List<FaceEmbeddingEntity>

    @Query("DELETE FROM face_embeddings WHERE id = :embeddingId")
    suspend fun deleteFaceEmbeddingById(embeddingId: String)

    @Query("""
        SELECT * FROM persons 
        WHERE name LIKE '%' || :keyword || '%' 
           OR summary LIKE '%' || :keyword || '%'
           OR relationship LIKE '%' || :keyword || '%'
           OR transcript LIKE '%' || :keyword || '%'
           OR emotion LIKE '%' || :keyword || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchMemories(keyword: String, limit: Int = 10): List<PersonMemory>

    @Query("""
        SELECT * FROM persons 
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentMemories(limit: Int = 10): List<PersonMemory>
}
