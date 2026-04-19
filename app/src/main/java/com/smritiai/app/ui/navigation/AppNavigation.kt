package com.smritiai.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smritiai.app.data.local.AppDatabase
import com.smritiai.app.data.MemoryRepository
import com.smritiai.app.ui.screens.AddPersonScreen
import com.smritiai.app.ui.screens.HomeScreen
import com.smritiai.app.ui.screens.RecognizePersonScreen
import com.smritiai.app.ui.screens.MemoryHistoryScreen
import com.smritiai.app.ui.screens.MemoryDetailScreen
import com.smritiai.app.ui.screens.SmritiChatScreen
import com.smritiai.app.ui.screens.ChatFaceCaptureScreen
import com.smritiai.app.viewmodel.MemoryViewModel
import com.smritiai.app.viewmodel.SmritiChatViewModel
import com.smritiai.app.viewmodel.SmritiChatViewModelFactory

@Composable
fun AppNavigation(viewModel: MemoryViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val memoryRepository = MemoryRepository(database.memoryDao())
    val smritiChatViewModel: SmritiChatViewModel = viewModel(factory = SmritiChatViewModelFactory(memoryRepository))
    
    NavHost(
        navController = navController, 
        startDestination = "home",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToAddPerson = { navController.navigate("add_person") },
                onNavigateToRecognize = { navController.navigate("recognize_person") },
                onNavigateToHistory = { navController.navigate("memory_history") },
                onNavigateToChat = { navController.navigate("smriti_chat") }
            )
        }
        composable("add_person") {
            AddPersonScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("recognize_person") {
            RecognizePersonScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("memory_history") {
            MemoryHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("memory_detail/$id") }
            )
        }
        composable("memory_detail/{memoryId}") { backStackEntry ->
            val memoryId = backStackEntry.arguments?.getString("memoryId") ?: ""
            MemoryDetailScreen(
                viewModel = viewModel,
                personId = memoryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("smriti_chat") {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val recognizedPersonIdFlow =
                backStackEntry?.savedStateHandle?.getStateFlow("recognized_person_id", "")
            val recognizedPersonId by recognizedPersonIdFlow?.collectAsState(initial = "") ?: androidx.compose.runtime.mutableStateOf("")

            LaunchedEffect(recognizedPersonId) {
                if (recognizedPersonId.isNotBlank()) {
                    if (recognizedPersonId == "__NONE__") {
                        smritiChatViewModel.onFaceRecognitionResult(null)
                    } else {
                        smritiChatViewModel.onFaceRecognitionResult(recognizedPersonId)
                    }
                    backStackEntry?.savedStateHandle?.remove<String>("recognized_person_id")
                }
            }

            SmritiChatScreen(
                viewModel = smritiChatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRequestFaceRecognition = { query ->
                    navController.navigate("chat_face_capture/${Uri.encode(query)}")
                }
            )
        }

        composable("chat_face_capture/{query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query").orEmpty()
            ChatFaceCaptureScreen(
                viewModel = viewModel,
                query = query,
                onCancel = { navController.popBackStack() },
                onPersonChosen = { personId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("recognized_person_id", personId ?: "__NONE__")
                    navController.popBackStack()
                }
            )
        }
    }
}
