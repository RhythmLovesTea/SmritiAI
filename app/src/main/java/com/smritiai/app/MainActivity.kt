package com.smritiai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smritiai.app.ui.navigation.AppNavigation
import com.smritiai.app.ui.theme.SmritiAITheme
import com.smritiai.app.viewmodel.MemoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmritiAITheme {
                val viewModel: MemoryViewModel = viewModel()
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
