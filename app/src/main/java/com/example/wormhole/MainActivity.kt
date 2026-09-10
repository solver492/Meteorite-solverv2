package com.example.wormhole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.example.wormhole.ui.EdgeCometApp
import com.example.wormhole.ui.EdgeCometViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: EdgeCometViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val darkScheme = darkColorScheme(
            primary = Color(0xFFFFD54F),
            secondary = Color(0xFF64B5F6),
            background = Color(0xFF0D0B18),
            surface = Color(0xFF141226)
        )

        setContent {
            MaterialTheme(colorScheme = darkScheme) {
                EdgeCometApp(viewModel = viewModel)
            }
        }
    }
}
