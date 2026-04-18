package com.smritiai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smritiai.app.ui.components.PrimaryButton

@Composable
fun HomeScreen(
    onNavigateToAddPerson: () -> Unit,
    onNavigateToRecognize: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SmritiAI",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your memory assistant.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6E6E73)
            )
            
            Spacer(modifier = Modifier.height(60.dp))

            PrimaryButton(
                text = "Ask Smriti AI",
                onClick = onNavigateToChat
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Recognize Person",
                onClick = onNavigateToRecognize,
                isSecondary = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PrimaryButton(
                text = "Add Memory",
                onClick = onNavigateToAddPerson,
                isSecondary = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Memory History",
                onClick = onNavigateToHistory,
                isSecondary = true
            )
        }
    }
}
