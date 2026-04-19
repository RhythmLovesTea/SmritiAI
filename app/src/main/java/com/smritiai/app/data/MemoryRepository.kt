package com.smritiai.app.data

import com.smritiai.app.data.local.MemoryDao
import com.smritiai.app.data.local.FaceEmbeddingEntity
import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.data.local.PersonWithEmbeddings
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {

    val allMemories: Flow<List<PersonMemory>> = memoryDao.getAllMemories()

    suspend fun insertMemory(memory: PersonMemory) {
        memoryDao.insertMemory(memory)
    }
    
    suspend fun getAllMemoriesSync(): List<PersonMemory> {
        return memoryDao.getAllMemoriesSync()
    }

    suspend fun getAllPersonsWithEmbeddings(): List<PersonWithEmbeddings> {
        return memoryDao.getAllPersonsWithEmbeddings()
    }

    suspend fun findByName(name: String): PersonMemory? {
        return memoryDao.findByName(name)
    }

    suspend fun getMemoryById(id: String): PersonMemory? {
        return memoryDao.getMemoryById(id)
    }

    suspend fun deleteMemory(memory: PersonMemory) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun insertFaceEmbedding(embedding: FaceEmbeddingEntity) {
        memoryDao.insertFaceEmbedding(embedding)
    }

    suspend fun insertFaceEmbeddings(embeddings: List<FaceEmbeddingEntity>) {
        memoryDao.insertFaceEmbeddings(embeddings)
    }

    suspend fun getFaceEmbeddingsForPerson(personId: String): List<FaceEmbeddingEntity> {
        return memoryDao.getFaceEmbeddingsForPerson(personId)
    }

    suspend fun deleteFaceEmbeddingById(embeddingId: String) {
        memoryDao.deleteFaceEmbeddingById(embeddingId)
    }

    suspend fun searchMemories(keyword: String, limit: Int = 10): List<PersonMemory> {
        return memoryDao.searchMemories(keyword, limit)
    }

    suspend fun getRecentMemories(limit: Int = 10): List<PersonMemory> {
        return memoryDao.getRecentMemories(limit)
    }
}
