package com.winkerly.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winkerly.app.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onClose: () -> Unit) {
    val savedKey by vm.apiKey.collectAsState()
    val savedColor by vm.accentColor.collectAsState()
    
    var key by remember { mutableStateOf(savedKey) }
    var selectedColor by remember { mutableStateOf(savedColor) }

    val colors = listOf("#7C9CFF", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan Winkerly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = key, 
            onValueChange = { key = it }, 
            label = { Text("API Key OpenRouter") }, 
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Text("Warna Tema", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (selectedColor == hex) 3.dp else 0.dp,
                            color = if (selectedColor == hex) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = hex }
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Button(onClick = { 
            vm.saveSettings(key, "https://openrouter.ai/api/v1", "openrouter/free", "You are Winkerly.", selectedColor)
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { 
            Text("Simpan Pengaturan") 
        }
    }
}
