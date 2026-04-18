package com.smritiai.app.utils

import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.data.local.PersonWithEmbeddings
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class FaceRecognitionEngine(
    private val helper: FaceRecognitionHelper = FaceRecognitionHelper()
) {
    data class Config(
        val topK: Int = 5,
        val topKWeight: Float = 0.70f,
        val centroidWeight: Float = 0.30f,
        val minSamplesToMatch: Int = 2,
        val unknownThreshold: Float = 0.75f,
        val confidentThreshold: Float = 0.85f
    )

    enum class Decision { CONFIDENT, LIKELY, UNKNOWN }

    data class Result(
        val decision: Decision,
        val person: PersonMemory? = null,
        val confidence: Float = 0f,
        val message: String? = null
    )

    /**
     * Scores one live embedding against all persons:
     * - per-person top-K weighted similarity voting (cosine)
     * - plus centroid similarity
     * - plus sample quality weighting (stored qualityScore)
     */
    fun recognize(
        liveEmbedding: FloatArray,
        persons: List<PersonWithEmbeddings>,
        config: Config = Config()
    ): Result {
        var bestPerson: PersonMemory? = null
        var bestScore = -1f
        var bestSamples = 0

        for (p in persons) {
            val samples = p.embeddings
                .mapNotNull { e -> e.vector.takeIf { it.isNotEmpty() }?.let { it to e.qualityScore } }
            if (samples.size < config.minSamplesToMatch) continue

            val sampleVectors = samples.map { it.first }
            val centroid = helper.centroid(sampleVectors)

            val scored = samples.map { (vec, q) ->
                val sim = helper.cosineSimilarity(liveEmbedding, vec)
                // Quality soft weighting: avoid giving trash frames equal power.
                val qW = 0.5f + 0.5f * q.coerceIn(0f, 1f)
                (sim * qW).coerceIn(-1f, 1f)
            }.sortedDescending()

            val topK = scored.take(config.topK.coerceAtMost(scored.size))
            val voteScore = weightedTopKScore(topK)

            val centroidSim = if (centroid == null) 0f else helper.cosineSimilarity(liveEmbedding, centroid)

            val combined = (config.topKWeight * voteScore) + (config.centroidWeight * centroidSim)
            if (combined > bestScore) {
                bestScore = combined
                bestPerson = p.person
                bestSamples = samples.size
            }
        }

        if (bestPerson == null) {
            return Result(Decision.UNKNOWN, null, 0f, "No enrolled faces yet.")
        }

        // Confidence mapping: cosine ~ [0..1] in practice for normalized vectors.
        val confidence = scoreToConfidence(bestScore, bestSamples).coerceIn(0f, 1f)
        val decision = when {
            confidence >= config.confidentThreshold -> Decision.CONFIDENT
            confidence >= config.unknownThreshold -> Decision.LIKELY
            else -> Decision.UNKNOWN
        }
        return when (decision) {
            Decision.CONFIDENT -> Result(decision, bestPerson, confidence)
            Decision.LIKELY -> Result(decision, bestPerson, confidence, "Please confirm.")
            Decision.UNKNOWN -> Result(decision, null, confidence, "Not sure enough.")
        }
    }

    private fun weightedTopKScore(topK: List<Float>): Float {
        if (topK.isEmpty()) return 0f
        // Weight closer matches more; use softmax over similarities (temperature tuned for cosine).
        val temp = 12f
        val exps = topK.map { exp((it * temp).toDouble()).toFloat() }
        val sum = exps.sum().let { if (it <= 1e-8f) 1e-8f else it }
        var out = 0f
        for (i in topK.indices) {
            val w = exps[i] / sum
            out += topK[i] * w
        }
        return out.coerceIn(-1f, 1f)
    }

    private fun scoreToConfidence(score: Float, sampleCount: Int): Float {
        // Map cosine-like score to [0..1] and slightly reward more samples.
        val base = ((score + 1f) / 2f).coerceIn(0f, 1f)
        val sampleBonus = min(1f, max(0f, (sampleCount - 2) / 12f)) * 0.05f
        return (base + sampleBonus).coerceIn(0f, 1f)
    }
}

