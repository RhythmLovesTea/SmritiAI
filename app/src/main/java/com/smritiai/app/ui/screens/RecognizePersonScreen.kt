package com.smritiai.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.viewmodel.MemoryViewModel
import com.smritiai.app.ui.components.AppleCard
import com.smritiai.app.ui.components.PrimaryButton
import com.smritiai.app.utils.FaceRecognitionEngine
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private fun getLastSeenText(timestamp: Long): String {
    val diffMillis = System.currentTimeMillis() - timestamp
    if (diffMillis < 0) return "You met them today"
    val diffHours = diffMillis / (1000 * 60 * 60)
    val diffDays = diffHours / 24
    
    return when {
        diffHours < 24 -> "You met them today"
        diffHours < 48 -> "You met them yesterday"
        diffDays < 7 -> "You met them $diffDays days ago"
        diffDays < 30 -> {
            val weeks = diffDays / 7
            "You met them $weeks week${if (weeks > 1L) "s" else ""} ago"
        }
        else -> {
            val months = diffDays / 30
            "You met them $months month${if (months > 1L) "s" else ""} ago"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognizePersonScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var recognizedPerson by remember { mutableStateOf<PersonMemory?>(null) }
    var resultMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // TTS Setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialize to default language
            }
        }
        textToSpeech.language = Locale.US
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            isProcessing = true
            resultMessage = "Analyzing face..."
            recognizedPerson = null
            
            coroutineScope.launch {
                try {
                    val istream = context.contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(istream)
                    val result = viewModel.recognizeFaceStabilized(bitmap, frames = 7)
                        when (result.decision) {
                            FaceRecognitionEngine.Decision.CONFIDENT -> {
                                val match = result.person
                                if (match != null) {
                                    recognizedPerson = match
                                    resultMessage = ""
                                    val moodText = match.emotion?.let { " Last interaction was $it." } ?: " Last interaction was Neutral."
                                    val lastSeenText = getLastSeenText(match.timestamp)
                                    val pct = (result.confidence * 100).toInt()
                                    val speechText = "This is ${match.name}, your ${match.relationship}. Confidence $pct percent. $lastSeenText ${match.summary}$moodText"
                                    tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    resultMessage = "I don’t recognize this person"
                                    tts?.speak("I don't recognize this person.", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                            FaceRecognitionEngine.Decision.LIKELY -> {
                                val match = result.person
                                if (match != null) {
                                    recognizedPerson = match
                                    val pct = (result.confidence * 100).toInt()
                                    resultMessage = "This looks like ${match.name} ($pct%). Please confirm."
                                    tts?.speak(resultMessage, TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    resultMessage = "I’m not fully sure. Please try again."
                                    tts?.speak(resultMessage, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                            FaceRecognitionEngine.Decision.UNKNOWN -> {
                                recognizedPerson = null
                                resultMessage = result.message ?: "I don’t recognize this person"
                                tts?.speak(resultMessage, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        }
                } catch (e: Exception) {
                    resultMessage = "Error analyzing image."
                }
                isProcessing = false
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "face_capture_recog_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "com.smritiai.app.fileprovider", file)
            imageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recognize", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFFBFBFD) // Apple-like soft grey/white background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            if (recognizedPerson == null && resultMessage.isEmpty() && !isProcessing) {
                Text(
                    text = "Tap to recognize a face",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(60.dp), strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Analyzing...", fontSize = 24.sp, color = Color.Gray)
            } else if (resultMessage == "I don’t recognize this person") {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = resultMessage,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3B30),
                    textAlign = TextAlign.Center
                )
            }

            recognizedPerson?.let { person ->
                AppleCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (person.imagePath != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = Uri.parse(person.imagePath)),
                                contentDescription = "Face Image",
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE5E5EA)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        Text(
                            text = "This is ${person.name}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Your ${person.relationship}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6E6E73),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = Color(0xFFE5E5EA),
                            thickness = 1.dp
                        )
                        
                        Text(
                            text = "Interaction Notes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6E6E73),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${person.summary}\"",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        val mood = person.emotion ?: "Neutral"
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mood: $mood",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (mood == "Positive") Color(0xFF34C759) else if (mood == "Negative") Color(0xFFFF3B30) else Color(0xFF8E8E93),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getLastSeenText(person.timestamp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6E6E73),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        PrimaryButton(
                            text = "Read Aloud",
                            onClick = {
                                val moodText = person.emotion?.let { " Last interaction was $it." } ?: " Last interaction was Neutral."
                                val lastSeenText = getLastSeenText(person.timestamp)
                                val speechText = "This is ${person.name}, your ${person.relationship}. $lastSeenText ${person.summary}$moodText"
                                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            isSecondary = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = "Open Camera",
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val file = File(context.cacheDir, "face_capture_recog_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "com.smritiai.app.fileprovider", file)
                        imageUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Back to Home", fontSize = 20.sp, color = Color(0xFF007AFF))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
