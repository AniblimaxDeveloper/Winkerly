package com.winkerly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winkerly.app.ui.ChatScreen
import com.winkerly.app.ui.SettingsScreen
import com.winkerly.app.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ChatViewModel = viewModel()
            // Default langsung ke layar Chat
            var showSettings by remember { mutableStateOf(false) }

            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0B0F19), surface = Color(0xFF141A29), primary = Color(0xFF7C9CFF))) {
                if (showSettings) {
                    SettingsScreen(vm = vm, onClose = { showSettings = false })
                } else {
                    ChatScreen(vm = vm, onSettings = { showSettings = true })
                }
            }
        }
    }
}
