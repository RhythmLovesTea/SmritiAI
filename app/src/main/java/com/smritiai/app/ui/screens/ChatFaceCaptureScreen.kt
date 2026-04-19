package com.smritiai.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.smritiai.app.viewmodel.MemoryViewModel
import com.smritiai.app.utils.FaceRecognitionEngine
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatFaceCaptureScreen(
    viewModel: MemoryViewModel,
    query: String,
    onCancel: () -> Unit,
    onPersonChosen: (personId: String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    var hasPermission by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Point the camera at the person") }
    var recognizedPersonId by remember { mutableStateOf<String?>(null) }
    var recognizedLabel by remember { mutableStateOf<String?>(null) }

    val imageCaptureState: MutableState<ImageCapture?> = remember { mutableStateOf(null) }
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            imageCapture
        )
        imageCaptureState.value = imageCapture
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Who is this?") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Query: $query",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFF8E8E93)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Camera permission required", color = Color.White)
                }

                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = statusText, color = Color(0xFF1C1C1E))

                recognizedLabel?.let {
                    Text(
                        text = it,
                        color = Color(0xFF34C759),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !isCapturing
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val imageCapture = imageCaptureState.value ?: return@Button
                            if (isCapturing) return@Button
                            isCapturing = true
                            statusText = "Capturing…"
                            recognizedPersonId = null
                            recognizedLabel = null

                            val outFile = File(context.cacheDir, "chat_face_${System.currentTimeMillis()}.jpg")
                            val output = ImageCapture.OutputFileOptions.Builder(outFile).build()
                            imageCapture.takePicture(
                                output,
                                mainExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        statusText = "Capture failed. Try again."
                                    }

                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        coroutineScope.launch {
                                            try {
                                                statusText = "Analyzing face…"
                                                val bitmap = BitmapFactory.decodeFile(outFile.absolutePath)
                                                val result = viewModel.recognizeFaceStabilized(bitmap, frames = 7)
                                                when (result.decision) {
                                                    FaceRecognitionEngine.Decision.CONFIDENT,
                                                    FaceRecognitionEngine.Decision.LIKELY -> {
                                                        val person = result.person
                                                        if (person != null) {
                                                            recognizedPersonId = person.id
                                                            val pct = (result.confidence * 100).toInt()
                                                            recognizedLabel = "Matched: ${person.name} ($pct%)"
                                                            statusText = "Tap “Use match” to answer your question."
                                                        } else {
                                                            statusText = "No match found. Try again."
                                                        }
                                                    }
                                                    FaceRecognitionEngine.Decision.UNKNOWN -> {
                                                        statusText = result.message ?: "Not sure. Try again with better light."
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                statusText = "Could not analyze the image."
                                            } finally {
                                                isCapturing = false
                                            }
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = hasPermission && !isCapturing
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color.Transparent, CircleShape)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Capture")
                    }
                }

                Button(
                    onClick = { onPersonChosen(recognizedPersonId) },
                    enabled = recognizedPersonId != null && !isCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use match")
                }

                OutlinedButton(
                    onClick = { onPersonChosen(null) },
                    enabled = !isCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip (no face)")
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
