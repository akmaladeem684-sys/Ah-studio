package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.StudioViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioDarkBg

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: StudioViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()
        val isStartupLoading by viewModel.isStartupLoading.collectAsState()
        val startupProgress by viewModel.startupProgress.collectAsState()
        val startupStatus by viewModel.startupStatus.collectAsState()

        BackHandler(enabled = currentScreen != AppScreen.HOME && !isStartupLoading) {
          when (currentScreen) {
            AppScreen.EDITOR -> {
              viewModel.saveCurrentProject()
              viewModel.navigateTo(AppScreen.HOME)
            }
            AppScreen.EXPORT -> viewModel.navigateTo(AppScreen.EDITOR)
            AppScreen.TEMPLATES, AppScreen.AI_SUITE, AppScreen.EXPORTED_LIBRARY, AppScreen.SETTINGS -> {
              viewModel.navigateTo(AppScreen.HOME)
            }
            AppScreen.HOME -> {}
          }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          // Primary Application Screens (Home Screen is default on open)
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding()
              .background(StudioDarkBg),
            color = StudioDarkBg
          ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
              when (screen) {
                AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                AppScreen.EDITOR -> EditorScreen(viewModel = viewModel)
                AppScreen.EXPORT -> ExportScreen(viewModel = viewModel)
                AppScreen.TEMPLATES -> TemplatesScreen(viewModel = viewModel)
                AppScreen.AI_SUITE -> AISuiteScreen(viewModel = viewModel)
                AppScreen.EXPORTED_LIBRARY -> ExportedVideosScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
              }
            }
          }

          // Startup Loading Animation Screen with Editing Tools
          AnimatedVisibility(
            visible = isStartupLoading,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(500))
          ) {
            StartupLoadingScreen(
              progress = startupProgress,
              statusText = startupStatus,
              onFinished = { viewModel.finishStartupLoading() },
              modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
            )
          }
        }
      }
    }
  }
}
