package com.smritiai.app.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Lightweight embedding + math helpers.
 *
 * Note: this project currently uses a simple 32x32 grayscale embedding. The upgraded
 * recognition accuracy mainly comes from better matching mathematics, multi-sample
 * storage, and stabilization — with a clear upgrade path to a stronger TFLite model.
 */
class FaceRecognitionHelper {

    data class QualityResult(
        val passed: Boolean,
        val qualityScore: Float,
        val reason: String? = null
    )

    suspend fun getFaceEmbedding(bitmap: Bitmap): FloatArray? {
        // Simple, fast embedding (32x32 grayscale). Output is L2-normalized.
        val resized = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val embedding = FloatArray(32 * 32)
        var index = 0
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val pixel = resized.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3.0f
                embedding[index++] = gray / 255.0f
            }
        }
        return l2NormalizeInPlace(embedding)
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val len = min(a.size, b.size)
        var dot = 0f
        var an = 0f
        var bn = 0f
        for (i in 0 until len) {
            val av = a[i]
            val bv = b[i]
            dot += av * bv
            an += av * av
            bn += bv * bv
        }
        val denom = sqrt(an) * sqrt(bn)
        return if (denom <= 1e-8f) 0f else (dot / denom)
    }

    fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        val len = min(a.size, b.size)
        var sum = 0f
        for (i in 0 until len) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return sqrt(sum)
    }

    fun centroid(vectors: List<FloatArray>): FloatArray? {
        if (vectors.isEmpty()) return null
        val size = vectors[0].size
        val out = FloatArray(size)
        var count = 0
        for (v in vectors) {
            if (v.size != size) continue
            for (i in 0 until size) out[i] += v[i]
            count++
        }
        if (count == 0) return null
        for (i in 0 until size) out[i] /= count.toFloat()
        return l2NormalizeInPlace(out)
    }

    /**
     * Fast face/image quality gate without heavy ML:
     * - brightness: reject too dark
     * - sharpness: reject too blurry (approx Laplacian variance)
     * - contrast: reject low contrast / washed out
     */
    fun checkQuality(bitmap: Bitmap): QualityResult {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 80 || h < 80) {
            return QualityResult(false, 0f, "Face too small / image too low-res")
        }

        // Downsample to limit CPU.
        val sampleW = 96
        val sampleH = (h * (sampleW.toFloat() / w)).toInt().coerceIn(96, 160)
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        var sum = 0f
        var sumSq = 0f
        val gray = FloatArray(sampleW * sampleH)
        var idx = 0
        for (y in 0 until sampleH) {
            for (x in 0 until sampleW) {
                val pixel = scaled.getPixel(x, y)
                val g = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / (3f * 255f)
                gray[idx++] = g
                sum += g
                sumSq += g * g
            }
        }
        val n = gray.size.toFloat()
        val mean = sum / n
        val variance = max(0f, (sumSq / n) - (mean * mean))
        val contrast = sqrt(variance) // ~stddev

        // Sharpness via simple Laplacian energy (approx).
        var lapSum = 0f
        var lapSumSq = 0f
        var lapCount = 0
        fun at(x: Int, y: Int): Float = gray[y * sampleW + x]
        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val c = at(x, y)
                val lap = (at(x - 1, y) + at(x + 1, y) + at(x, y - 1) + at(x, y + 1)) - (4f * c)
                val a = abs(lap)
                lapSum += a
                lapSumSq += a * a
                lapCount++
            }
        }
        val lapMean = if (lapCount == 0) 0f else lapSum / lapCount.toFloat()
        val lapVar = if (lapCount == 0) 0f else max(0f, (lapSumSq / lapCount.toFloat()) - (lapMean * lapMean))
        val sharpness = sqrt(lapVar)

        val brightnessOk = mean >= 0.25f
        val contrastOk = contrast >= 0.12f
        val sharpnessOk = sharpness >= 0.015f

        val qualityScore =
            (0.45f * min(1f, (mean / 0.45f))) +
                (0.25f * min(1f, (contrast / 0.22f))) +
                (0.30f * min(1f, (sharpness / 0.035f)))

        val passed = brightnessOk && contrastOk && sharpnessOk
        val reason = when {
            !brightnessOk -> "Too dark. Please move to better light."
            !sharpnessOk -> "Too blurry. Hold still and try again."
            !contrastOk -> "Low contrast. Avoid backlight and try again."
            else -> null
        }
        return QualityResult(passed, qualityScore.coerceIn(0f, 1f), reason)
    }

    private fun l2NormalizeInPlace(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm <= 1e-8f) return v
        val inv = 1f / norm
        for (i in v.indices) v[i] *= inv
        return v
    }
}

