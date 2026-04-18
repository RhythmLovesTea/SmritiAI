package com.smritiai.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class PersonRepository(private val context: Context) {
    private val gson = Gson()
    private val file = File(context.filesDir, "persons.json")

    fun getPersons(): List<Person> {
        if (!file.exists()) {
            return getPreloadedData()
        }
        val json = file.readText()
        if (json.isBlank()) return getPreloadedData()
        
        val type = object : TypeToken<List<Person>>() {}.type
        return try {
            val list: List<Person> = gson.fromJson(json, type)
            if (list.isEmpty()) getPreloadedData() else list
        } catch (e: Exception) {
            getPreloadedData()
        }
    }

    fun savePersons(persons: List<Person>) {
        val json = gson.toJson(persons)
        file.writeText(json)
    }

    private fun getPreloadedData(): List<Person> {
        val defaultData = listOf(
            Person(
                id = "preloaded_1",
                name = "Sarah",
                relationship = "Daughter",
                summary = "She visits every Sunday. She asked about your doctor appointment and brought groceries.",
                imagePath = null,
                audioPath = null
            ),
            Person(
                id = "preloaded_2",
                name = "David",
                relationship = "Son",
                summary = "He called yesterday to check on your health. Mentioned he will fix the TV remote soon.",
                imagePath = null,
                audioPath = null
            )
        )
        savePersons(defaultData)
        return defaultData
    }
}
