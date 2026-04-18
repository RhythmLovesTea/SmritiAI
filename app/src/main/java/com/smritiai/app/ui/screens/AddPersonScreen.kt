package com.smritiai.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.smritiai.app.viewmodel.MemoryViewModel
import com.smritiai.app.ui.components.AppleCard
import com.smritiai.app.ui.components.PrimaryButton
import com.smritiai.app.data.LocalSummarizationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    var hasRecorded by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }

    var fullTranscript by remember { mutableStateOf("") }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val newText = matches[0]
                    fullTranscript = if (fullTranscript.isEmpty()) newText else "$fullTranscript. $newText"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedImagePath = imageUri?.path
        } else {
            imageUri = null
            capturedImagePath = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "face_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "com.smritiai.app.fileprovider", file)
            imageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!isRecording) {
                isRecording = true
                hasRecorded = false
                fullTranscript = ""
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                speechRecognizer.startListening(intent)
            }
        } else {
            Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Add Person", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            ) 
        },
        containerColor = Color(0xFFF5F5F7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AppleCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E5EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null && capturedImagePath != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUri),
                                contentDescription = "Captured Face",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Capture Face",
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val file = File(context.cacheDir, "face_capture_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "com.smritiai.app.fileprovider", file)
                                imageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        isSecondary = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.White)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Relationship") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.White)
            )
            Spacer(modifier = Modifier.height(24.dp))

            AppleCard {
                Text("Audio Memories", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                if (isAnalyzing) {
                    CircularProgressIndicator()
                } else if (!hasRecorded) {
                    PrimaryButton(
                        text = if (isRecording) "Stop Recording" else "Record Conversation",
                        onClick = {
                            if (!isRecording) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    isRecording = true
                                    hasRecorded = false
                                    fullTranscript = ""
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    }
                                    speechRecognizer.startListening(intent)
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                speechRecognizer.stopListening()
                                isRecording = false
                                hasRecorded = true
                                isAnalyzing = true
                                coroutineScope.launch {
                                    delay(1500)
                                    if (fullTranscript.isBlank()) {
                                        summary = "No speech detected"
                                    } else {
                                        summary = LocalSummarizationEngine.summarize(fullTranscript)
                                            ?: "Memory note recorded."
                                    }
                                    isAnalyzing = false
                                }
                            }
                        },
                        isSecondary = false
                    )
                } else {
                    Text("Recorded Successfully", color = Color(0xFF007AFF), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Record Again",
                        onClick = { hasRecorded = false; summary = ""; fullTranscript = "" },
                        isSecondary = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("AI Summary / Voice Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.White)
            )

            if (fullTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                AppleCard {
                    Text("Full Transcript", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF6E6E73))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fullTranscript, fontSize = 16.sp, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            if (isProcessing) {
                CircularProgressIndicator(color = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
            } else {
                PrimaryButton(
                    text = "Save Person",
                    onClick = {
                        if (name.isBlank() || relationship.isBlank()) {
                            Toast.makeText(context, "Required fields missing", Toast.LENGTH_SHORT).show()
                            return@PrimaryButton
                        }
                        if (isProcessing) return@PrimaryButton

                        isProcessing = true
                        coroutineScope.launch {
                            var embedding: FloatArray? = null
                            var qualityScore: Float? = null
                            var enrollmentBitmap: android.graphics.Bitmap? = null
                            if (imageUri != null) {
                                try {
                                    val istream = context.contentResolver.openInputStream(imageUri!!)
                                    val bitmap = BitmapFactory.decodeStream(istream)
                                    enrollmentBitmap = bitmap
                                    val quality = viewModel.checkFaceQuality(bitmap)
                                    if (!quality.passed) {
                                        Toast.makeText(context, quality.reason ?: "Face quality too low.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        embedding = viewModel.getFaceEmbedding(bitmap)
                                        qualityScore = quality.qualityScore
                                        if (embedding == null) {
                                            Toast.makeText(context, "Could not extract face features. Will save without face data.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            if (summary == "No speech detected" && fullTranscript.isBlank() && imageUri == null) {
                                Toast.makeText(context, "Cannot save empty memory", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                return@launch
                            }

                            val finalTranscript = fullTranscript.takeIf { it.isNotBlank() } ?: ""
                            val emotionStr = viewModel.detectEmotion(finalTranscript)

                            val personId = viewModel.addPersonAndReturnId(
                                name = name,
                                relationship = relationship,
                                summary = summary.takeIf { it != "No speech detected" } ?: "",
                                imagePath = null,
                                audioPath = null,
                                faceEmbedding = embedding,
                                faceEmbeddingQuality = qualityScore,
                                poseType = "enroll_photo",
                                transcript = fullTranscript.takeIf { it.isNotBlank() },
                                emotion = emotionStr
                            )

                            // Lightweight multi-sample enrollment: derive a few safe crops as extra samples.
                            val baseBitmap = enrollmentBitmap
                            if (baseBitmap != null && embedding != null) {
                                val rects = buildEnrollmentRects(baseBitmap.width, baseBitmap.height)
                                val poses = listOf("center", "left", "right", "up", "down")
                                rects.zip(poses).forEach { (r, pose) ->
                                    runCatching {
                                        val crop = android.graphics.Bitmap.createBitmap(
                                            baseBitmap,
                                            r.left,
                                            r.top,
                                            r.width(),
                                            r.height()
                                        )
                                        val emb = viewModel.getFaceEmbedding(crop)
                                        if (emb != null) {
                                            viewModel.addFaceEmbeddingSample(
                                                personId = personId,
                                                embedding = emb,
                                                qualityScore = qualityScore ?: 0.5f,
                                                poseType = "auto_$pose"
                                            )
                                        }
                                    }
                                }
                            }
                            Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { 
                Text("Cancel", color = Color(0xFF007AFF), fontSize = 18.sp) 
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun buildEnrollmentRects(w: Int, h: Int): List<Rect> {
    val cropW = (w * 0.80f).toInt().coerceAtMost(w)
    val cropH = (h * 0.80f).toInt().coerceAtMost(h)
    val cx = w / 2
    val cy = h / 2
    val dx = ((w - cropW) * 0.20f).toInt().coerceAtLeast(1)
    val dy = ((h - cropH) * 0.20f).toInt().coerceAtLeast(1)
    fun rect(x: Int, y: Int): Rect {
        val left = (x - cropW / 2).coerceIn(0, w - cropW)
        val top = (y - cropH / 2).coerceIn(0, h - cropH)
        return Rect(left, top, left + cropW, top + cropH)
    }
    return listOf(
        rect(cx, cy),
        rect(cx - dx, cy),
        rect(cx + dx, cy),
        rect(cx, cy - dy),
        rect(cx, cy + dy)
    )
}
