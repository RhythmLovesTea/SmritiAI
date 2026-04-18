package com.smritiai.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smritiai.app.data.Person
import com.smritiai.app.data.PersonRepository
import com.smritiai.app.utils.FaceRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SmritiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PersonRepository(application)
    private val faceHelper = FaceRecognitionHelper()

    private val _persons = MutableStateFlow<List<Person>>(emptyList())
    val persons: StateFlow<List<Person>> = _persons.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _persons.value = repository.getPersons()
        }
    }

    fun addPerson(
        name: String,
        relationship: String,
        summary: String,
        imagePath: String?,
        audioPath: String?,
        faceEmbedding: List<Float>?
    ) {
        val newPerson = Person(
            id = UUID.randomUUID().toString(),
            name = name,
            relationship = relationship,
            summary = summary,
            imagePath = imagePath,
            audioPath = audioPath,
            faceEmbedding = faceEmbedding
        )
        val currentList = _persons.value.toMutableList()
        currentList.add(newPerson)
        _persons.value = currentList
        repository.savePersons(currentList)
    }

    fun getPersonByName(name: String): Person? {
        return _persons.value.find { it.name.equals(name, ignoreCase = true) }
    }

    suspend fun getFaceEmbedding(bitmap: Bitmap): FloatArray? {
        return faceHelper.getFaceEmbedding(bitmap)
    }

    fun findPersonByFace(embedding: FloatArray): Person? {
        var bestMatch: Person? = null
        var bestSim = -1f

        for (person in _persons.value) {
            val personEmb = person.faceEmbedding?.toFloatArray() ?: continue
            val sim = faceHelper.cosineSimilarity(embedding, personEmb)
            android.util.Log.d("FACE_MATCH", "Cosine similarity: $sim")
            if (sim > 0.85f && sim > bestSim) {
                bestSim = sim
                bestMatch = person
            }
        }
        if (bestMatch != null) {
            android.util.Log.d("FACE_MATCH", "Matched! Person: ${bestMatch.name} with similarity: $bestSim")
        } else {
            android.util.Log.d("FACE_MATCH", "No match found")
        }
        return bestMatch
    }
}
