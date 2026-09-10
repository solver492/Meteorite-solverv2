package com.example.wormhole.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun EdgeCometApp(viewModel: EdgeCometViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0B18)
    ) {
        BrowserScreen(viewModel = viewModel)

        // Contextual AI Chat Sheet
        if (viewModel.isChatSheetOpen) {
            ContextualChatSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.isChatSheetOpen = false }
            )
        }

        // Tab Manager Sheet
        if (viewModel.isTabManagerOpen) {
            TabManagerSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.isTabManagerOpen = false }
            )
        }

        // Agent Mode Plan & Confirmation Dialog
        if (viewModel.isAgentModeOpen) {
            AgentModeDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.isAgentModeOpen = false }
            )
        }

        // On-Device Model Manager & Benchmark
        if (viewModel.isModelManagerOpen) {
            ModelManagerScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.isModelManagerOpen = false }
            )
        }

        // Python Automation Scripts Screen
        if (viewModel.isAutomationsOpen) {
            AutomationsScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.isAutomationsOpen = false }
            )
        }

        // Settings & Privacy Dashboard
        if (viewModel.isSettingsOpen) {
            SettingsScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.isSettingsOpen = false }
            )
        }

        // On-Device AI Task Scheduler
        if (viewModel.isTaskSchedulerOpen) {
            TaskSchedulerScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.isTaskSchedulerOpen = false }
            )
        }
    }
}
