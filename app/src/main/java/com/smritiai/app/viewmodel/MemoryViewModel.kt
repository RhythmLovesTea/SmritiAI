package com.smritiai.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smritiai.app.data.LocalSummarizationEngine
import com.smritiai.app.data.MemoryRepository
import com.smritiai.app.data.local.AppDatabase
import com.smritiai.app.data.local.FaceEmbeddingEntity
import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.utils.FaceRecognitionHelper
import com.smritiai.app.utils.FaceRecognitionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID
import kotlin.math.max

/**
 * MemoryViewModel
 *
 * Upgraded (Part 1 & 6):
 * - Auto-generates warm summaries from raw transcripts using LocalSummarizationEngine
 * - Uses advanced emotion detection from LocalSummarizationEngine
 * - All heavy logic runs on Dispatchers.IO (Part 6)
 */
class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryDao = AppDatabase.getDatabase(application).memoryDao()
    private val repository = MemoryRepository(memoryDao)
    private val faceHelper = FaceRecognitionHelper()
    private val recognitionEngine = FaceRecognitionEngine(faceHelper)

    private val cachedPersonsWithEmbeddings =
        AtomicReference<List<com.smritiai.app.data.local.PersonWithEmbeddings>>(emptyList())

    val memories: StateFlow<List<PersonMemory>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Keep an in-memory cache for fast matching on low-end devices.
        viewModelScope.launch(Dispatchers.IO) {
            refreshFaceCache()
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Update cache when persons list changes.
            repository.allMemories.collect {
                refreshFaceCache()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Add person — auto-summarizes transcript locally (Part 1)
    // ─────────────────────────────────────────────────────────────────────────

    fun addPerson(
        name: String,
        relationship: String,
        summary: String,
        imagePath: String?,
        audioPath: String?,
        faceEmbedding: FloatArray?,
        faceEmbeddingQuality: Float?,
        poseType: String?,
        transcript: String?,
        emotion: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            addPersonAndReturnId(
                name = name,
                relationship = relationship,
                summary = summary,
                imagePath = imagePath,
                audioPath = audioPath,
                faceEmbedding = faceEmbedding,
                faceEmbeddingQuality = faceEmbeddingQuality,
                poseType = poseType,
                transcript = transcript,
                emotion = emotion
            )
        }
    }

    suspend fun addPersonAndReturnId(
        name: String,
        relationship: String,
        summary: String,
        imagePath: String?,
        audioPath: String?,
        faceEmbedding: FloatArray?,
        faceEmbeddingQuality: Float?,
        poseType: String?,
        transcript: String?,
        emotion: String?
    ): String {
        return withContext(Dispatchers.IO) {

            // ── Local summarization: if no hand-written summary is given,
            //    auto-generate one from the transcript using the local engine ──
            val finalSummary: String = when {
                summary.isNotBlank() -> summary
                !transcript.isNullOrBlank() -> {
                    LocalSummarizationEngine.summarize(transcript)
                        ?: buildFallbackSummary(name, relationship)
                }
                else -> buildFallbackSummary(name, relationship)
            }

            // ── Emotion: use advanced local engine if not provided ─────────────
            val finalEmotion: String = when {
                !emotion.isNullOrBlank() -> emotion
                !transcript.isNullOrBlank() ->
                    LocalSummarizationEngine.detectEmotionLabel(transcript)
                !summary.isNullOrBlank() ->
                    LocalSummarizationEngine.detectEmotionLabel(summary)
                else -> "Neutral"
            }

            val newMemory = PersonMemory(
                id = UUID.randomUUID().toString(),
                name = name,
                relationship = relationship,
                summary = finalSummary,
                imagePath = imagePath,
                audioPath = audioPath,
                timestamp = System.currentTimeMillis(),
                transcript = transcript,
                emotion = finalEmotion
            )
            repository.insertMemory(newMemory)

            if (faceEmbedding != null) {
                val emb = FaceEmbeddingEntity(
                    id = UUID.randomUUID().toString(),
                    personId = newMemory.id,
                    vector = faceEmbedding,
                    qualityScore = (faceEmbeddingQuality ?: 0.5f).coerceIn(0f, 1f),
                    timestamp = System.currentTimeMillis(),
                    poseType = poseType?.takeIf { it.isNotBlank() } ?: "unknown"
                )
                repository.insertFaceEmbedding(emb)
            }
            refreshFaceCache()
            newMemory.id
        }
    }

    suspend fun addFaceEmbeddingSample(
        personId: String,
        embedding: FloatArray,
        qualityScore: Float,
        poseType: String
    ) {
        withContext(Dispatchers.IO) {
            repository.insertFaceEmbedding(
                FaceEmbeddingEntity(
                    id = UUID.randomUUID().toString(),
                    personId = personId,
                    vector = embedding,
                    qualityScore = qualityScore.coerceIn(0f, 1f),
                    timestamp = System.currentTimeMillis(),
                    poseType = poseType
                )
            )
            refreshFaceCache()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Face / person helpers
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getPersonByName(name: String): PersonMemory? {
        return withContext(Dispatchers.IO) {
            repository.findByName(name)
        }
    }

    suspend fun getFaceEmbedding(bitmap: Bitmap): FloatArray? {
        return withContext(Dispatchers.Default) {
            faceHelper.getFaceEmbedding(bitmap)
        }
    }

    fun checkFaceQuality(bitmap: Bitmap): FaceRecognitionHelper.QualityResult {
        return faceHelper.checkQuality(bitmap)
    }

    suspend fun recognizeFace(
        embedding: FloatArray,
        config: FaceRecognitionEngine.Config = FaceRecognitionEngine.Config()
    ): FaceRecognitionEngine.Result {
        return withContext(Dispatchers.Default) {
            recognitionEngine.recognize(
                liveEmbedding = embedding,
                persons = cachedPersonsWithEmbeddings.get(),
                config = config
            )
        }
    }

    /**
     * Stabilized recognition from a single captured bitmap by running recognition over multiple
     * slightly different crops and voting across results. This reduces flicker from framing noise
     * without requiring a full CameraX streaming pipeline.
     */
    suspend fun recognizeFaceStabilized(
        bitmap: Bitmap,
        frames: Int = 7,
        config: FaceRecognitionEngine.Config = FaceRecognitionEngine.Config()
    ): FaceRecognitionEngine.Result {
        val quality = checkFaceQuality(bitmap)
        if (!quality.passed) {
            return FaceRecognitionEngine.Result(
                decision = FaceRecognitionEngine.Decision.UNKNOWN,
                person = null,
                confidence = 0f,
                message = quality.reason
            )
        }

        return withContext(Dispatchers.Default) {
            val crops = generateCrops(bitmap, frames.coerceIn(3, 10))
            val results = crops.mapNotNull { crop ->
                val emb = faceHelper.getFaceEmbedding(crop) ?: return@mapNotNull null
                recognitionEngine.recognize(emb, cachedPersonsWithEmbeddings.get(), config)
            }
            if (results.isEmpty()) {
                return@withContext FaceRecognitionEngine.Result(
                    decision = FaceRecognitionEngine.Decision.UNKNOWN,
                    person = null,
                    confidence = 0f,
                    message = "Could not extract face features."
                )
            }

            // Majority vote by personId (unknown results vote for nobody).
            val votes = results.mapNotNull { it.person?.id }
            val winnerId = votes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            if (winnerId == null) {
                return@withContext FaceRecognitionEngine.Result(
                    decision = FaceRecognitionEngine.Decision.UNKNOWN,
                    person = null,
                    confidence = results.map { it.confidence }.average().toFloat(),
                    message = "Not sure enough."
                )
            }

            val winnerPerson = results.firstOrNull { it.person?.id == winnerId }?.person
            val winnerConfAvg = results
                .filter { it.person?.id == winnerId }
                .map { it.confidence }
                .average()
                .toFloat()

            val winnerShare = votes.count { it == winnerId }.toFloat() / max(1f, results.size.toFloat())
            val stabilizedConfidence = (0.85f * winnerConfAvg + 0.15f * winnerShare).coerceIn(0f, 1f)

            val decision = when {
                stabilizedConfidence >= config.confidentThreshold -> FaceRecognitionEngine.Decision.CONFIDENT
                stabilizedConfidence >= config.unknownThreshold -> FaceRecognitionEngine.Decision.LIKELY
                else -> FaceRecognitionEngine.Decision.UNKNOWN
            }
            when (decision) {
                FaceRecognitionEngine.Decision.CONFIDENT ->
                    FaceRecognitionEngine.Result(decision, winnerPerson, stabilizedConfidence)
                FaceRecognitionEngine.Decision.LIKELY ->
                    FaceRecognitionEngine.Result(decision, winnerPerson, stabilizedConfidence, "Please confirm.")
                FaceRecognitionEngine.Decision.UNKNOWN ->
                    FaceRecognitionEngine.Result(decision, null, stabilizedConfidence, "Not sure enough.")
            }
        }
    }

    fun deleteMemory(person: PersonMemory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemory(person)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Emotion detection — delegates to advanced engine (Part 1)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Detects emotion label from any text using the local rule engine.
     * Returns: "Positive", "Negative", "Concern", or "Neutral"
     */
    fun detectEmotion(text: String): String =
        LocalSummarizationEngine.detectEmotionLabel(text)

    /**
     * Generates a local AI summary from a raw transcript.
     * Returns a warm, natural sentence — never a raw copy of the transcript.
     */
    fun generateLocalSummary(transcript: String, name: String, relationship: String): String {
        return LocalSummarizationEngine.summarize(transcript)
            ?: buildFallbackSummary(name, relationship)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildFallbackSummary(name: String, relationship: String): String {
        val rel = relationship.lowercase()
        return when {
            rel.isNotBlank() -> "$name ($relationship) added to your memory diary."
            name.isNotBlank() -> "$name added to your memory diary."
            else -> "New person added to your memory diary."
        }
    }

    private suspend fun refreshFaceCache() {
        cachedPersonsWithEmbeddings.set(repository.getAllPersonsWithEmbeddings())
    }

    private fun generateCrops(bitmap: Bitmap, count: Int): List<Bitmap> {
        val w = bitmap.width
        val h = bitmap.height
        val baseW = (w * 0.85f).toInt().coerceAtMost(w)
        val baseH = (h * 0.85f).toInt().coerceAtMost(h)
        val dx = ((w - baseW) * 0.18f).toInt().coerceAtLeast(1)
        val dy = ((h - baseH) * 0.18f).toInt().coerceAtLeast(1)

        val rects = listOf(
            Rect((w - baseW) / 2, (h - baseH) / 2, (w + baseW) / 2, (h + baseH) / 2),
            Rect((w - baseW) / 2 - dx, (h - baseH) / 2, (w + baseW) / 2 - dx, (h + baseH) / 2),
            Rect((w - baseW) / 2 + dx, (h - baseH) / 2, (w + baseW) / 2 + dx, (h + baseH) / 2),
            Rect((w - baseW) / 2, (h - baseH) / 2 - dy, (w + baseW) / 2, (h + baseH) / 2 - dy),
            Rect((w - baseW) / 2, (h - baseH) / 2 + dy, (w + baseW) / 2, (h + baseH) / 2 + dy)
        )

        val out = ArrayList<Bitmap>(count)
        var i = 0
        while (out.size < count) {
            val r = rects[i % rects.size]
            val left = r.left.coerceIn(0, w - 2)
            val top = r.top.coerceIn(0, h - 2)
            val right = r.right.coerceIn(left + 1, w)
            val bottom = r.bottom.coerceIn(top + 1, h)
            out.add(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top))
            i++
        }
        return out
    }
}
