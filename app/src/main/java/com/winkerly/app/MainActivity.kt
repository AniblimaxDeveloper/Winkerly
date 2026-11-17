package com.winkerly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.winkerly.app.ui.ChatScreen
import com.winkerly.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSettings by remember { mutableStateOf(true) } 
            var apiKey by remember { mutableStateOf("") }
            var baseUrl by remember { mutableStateOf("https://openrouter.ai/api/v1") }
            var model by remember { mutableStateOf("nvidia/nemotron-3-super-120b-a12b:free") }

            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0B0F19), surface = Color(0xFF141A29), primary = Color(0xFF7C9CFF))) {
                if (showSettings) {
                    SettingsScreen(
                        currentApiKey = apiKey,
                        currentBaseUrl = baseUrl,
                        currentModel = model,
                        onSave = { newKey, newUrl, newModel ->
                            apiKey = newKey
                            baseUrl = newUrl
                            model = newModel
                            showSettings = false
                        },
                        onBack = { if (apiKey.isNotBlank()) showSettings = false }
                    )
                } else {
                    ChatScreen(
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        model = model,
                        onSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
