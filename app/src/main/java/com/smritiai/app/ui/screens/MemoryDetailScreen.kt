package com.smritiai.app.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import coil.compose.rememberAsyncImagePainter
import com.smritiai.app.data.local.PersonMemory
import com.smritiai.app.viewmodel.MemoryViewModel
import java.util.Locale
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    viewModel: MemoryViewModel,
    personId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val memories by viewModel.memories.collectAsState()
    val person = memories.find { it.id == personId }

    // TTS Setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
            }
        }
        textToSpeech.language = Locale.US
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    var showTranscript by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Memory") },
            text = { Text("Are you sure you want to delete this memory?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        person?.let { p ->
                            viewModel.deleteMemory(p)
                            Toast.makeText(context, "Memory deleted", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFFBFBFD)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            person?.let { p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x33000000)),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (p.imagePath != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = Uri.parse(p.imagePath)),
                                contentDescription = "Face Image",
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE5E5EA)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = p.name,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = p.relationship,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF86868B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = Color(0xFFE5E5EA),
                            thickness = 1.dp
                        )
                        
                        Text(
                            text = "Summary:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF86868B),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = p.summary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D1D1F),
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (!p.transcript.isNullOrBlank()) {
                            TextButton(onClick = { showTranscript = !showTranscript }) {
                                Text(if (showTranscript) "Hide Full Conversation" else "View Full Conversation")
                            }
                            if (showTranscript) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = p.transcript,
                                    fontSize = 16.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        FilledTonalButton(
                            onClick = {
                                val speechText = "This is ${p.name}, your ${p.relationship}. ${p.summary}"
                                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE8F0FE),
                                contentColor = Color(0xFF1967D2)
                            )
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud")
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Read Aloud", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } ?: run {
                Text("Memory not found", color = Color.Red, fontSize = 20.sp)
            }
        }
    }
}
