package com.winkerly.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.winkerly.app.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onClose: () -> Unit) {
    val savedKey by vm.apiKey.collectAsState()
    val savedColor by vm.accentColor.collectAsState()
    val savedTemp by vm.temperature.collectAsState()
    val savedTokens by vm.maxTokens.collectAsState()
    val savedFont by vm.fontSize.collectAsState()
    val savedDark by vm.darkMode.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    var key by remember { mutableStateOf(savedKey) }
    var selectedColor by remember { mutableStateOf(savedColor) }
    var temp by remember { mutableStateOf(savedTemp) }
    var tokens by remember { mutableStateOf(savedTokens) }
    var font by remember { mutableStateOf(savedFont) }
    var darkMode by remember { mutableStateOf(savedDark) }
    var showKey by remember { mutableStateOf(false) }

    val colors = listOf("#7C9CFF", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0")

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan Winkerly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = key, 
            onValueChange = { key = it }, 
            label = { Text("API Key OpenRouter") }, 
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.testConnection(key) }, modifier = Modifier.fillMaxWidth()) { Text("Test Koneksi") }
        
        if (errorMsg != null) {
            Text(errorMsg!!, color = if(errorMsg!!.contains("Berhasil")) Color.Green else Color.Red, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Warna Tema", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            colors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                Box(modifier = Modifier.size(40.dp).background(color, CircleShape).border(if (selectedColor == hex) 3.dp else 0.dp, Color.White, CircleShape).clickable { selectedColor = hex })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Kreativitas AI (Temperature): ${String.format("%.1f", temp)}")
        Slider(value = temp, onValueChange = { temp = it }, valueRange = 0f..1f)

        Spacer(Modifier.height(8.dp))
        Text("Panjang Jawaban (Max Tokens): $tokens")
        Slider(value = tokens.toFloatOrNull() ?: 2048f, onValueChange = { tokens = it.toInt().toString() }, valueRange = 256f..4096f, steps = 15)

        Spacer(Modifier.height(8.dp))
        Text("Ukuran Font Chat: ${font.toInt()} sp")
        Slider(value = font, onValueChange = { font = it }, valueRange = 12f..24f)

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mode Gelap", style = MaterialTheme.typography.titleMedium)
            Switch(checked = darkMode, onCheckedChange = { darkMode = it })
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { 
            vm.saveSettings(key, "https://openrouter.ai/api/v1", "openrouter/free", "You are Winkerly.", selectedColor, temp, tokens, font, darkMode)
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { 
            Text("Simpan Pengaturan") 
        }
        Text("Winkerly v6.0.0 - Ultra Edition", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 32.dp).align(Alignment.CenterHorizontally))
    }
}
