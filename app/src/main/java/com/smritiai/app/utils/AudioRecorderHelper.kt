package com.smritiai.app.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var currentAudioPath: String? = null
        private set

    fun startRecording(): Boolean {
        val audioFile = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.3gp")
        currentAudioPath = audioFile.absolutePath

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(currentAudioPath)

            try {
                prepare()
                start()
                return true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return false
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
    }

    fun deleteRecording() {
        currentAudioPath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
        currentAudioPath = null
    }
}
