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
    val savedUrl by vm.baseUrl.collectAsState()
    val savedModel by vm.modelId.collectAsState()
    val savedColor by vm.accentColor.collectAsState()
    val savedTemp by vm.temperature.collectAsState()
    val savedTokens by vm.maxTokens.collectAsState()
    val savedFont by vm.fontSize.collectAsState()
    val savedSpeed by vm.ttsSpeed.collectAsState()
    val savedPitch by vm.ttsPitch.collectAsState()
    val savedHaptic by vm.hapticEnabled.collectAsState()
    val savedAutoCopy by vm.autoCopy.collectAsState()
    val savedDark by vm.darkMode.collectAsState()
    val savedLock by vm.appLock.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    var key by remember { mutableStateOf(savedKey) }
    var url by remember { mutableStateOf(savedUrl) }
    var model by remember { mutableStateOf(savedModel) }
    var selectedColor by remember { mutableStateOf(savedColor) }
    var temp by remember { mutableStateOf(savedTemp) }
    var tokens by remember { mutableStateOf(savedTokens) }
    var font by remember { mutableStateOf(savedFont) }
    var speed by remember { mutableStateOf(savedSpeed) }
    var pitch by remember { mutableStateOf(savedPitch) }
    var haptic by remember { mutableStateOf(savedHaptic) }
    var autoCopy by remember { mutableStateOf(savedAutoCopy) }
    var darkMode by remember { mutableStateOf(savedDark) }
    var appLock by remember { mutableStateOf(savedLock) }
    var showKey by remember { mutableStateOf(false) }

    val colors = listOf("#7C9CFF", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0")

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan Winkerly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null) } }
        )
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model ID") }, modifier = Modifier.fillMaxWidth())
        if (errorMsg != null) Text(errorMsg!!, color = if(errorMsg!!.contains("Berhasil")) Color.Green else Color.Red)

        Spacer(Modifier.height(16.dp))
        Text("Warna Tema", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            colors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                Box(modifier = Modifier.size(40.dp).background(color, CircleShape).border(if (selectedColor == hex) 3.dp else 0.dp, Color.White, CircleShape).clickable { selectedColor = hex })
            }
        }

        Text("Kreativitas (Temp): ${String.format("%.1f", temp)}")
        Slider(value = temp, onValueChange = { temp = it }, valueRange = 0f..1f)
        Text("Max Tokens: ${tokens.toInt()}")
        Slider(value = tokens, onValueChange = { tokens = it }, valueRange = 256f..4096f, steps = 15)
        Text("Ukuran Font: ${font.toInt()} sp")
        Slider(value = font, onValueChange = { font = it }, valueRange = 12f..24f)
        Text("Kecepatan TTS: ${String.format("%.1f", speed)}x")
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2.0f)
        Text("Nada TTS: ${String.format("%.1f", pitch)}x")
        Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Getaran (Haptic)"); Switch(checked = haptic, onCheckedChange = { haptic = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Auto-Copy Jawaban"); Switch(checked = autoCopy, onCheckedChange = { autoCopy = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mode Gelap"); Switch(checked = darkMode, onCheckedChange = { darkMode = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("App Lock (PIN)"); Switch(checked = appLock, onCheckedChange = { appLock = it })
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { 
            vm.saveSettings(key, url, model, "You are Winkerly.", selectedColor, temp, tokens, font, speed, pitch, haptic, autoCopy, darkMode, appLock)
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Pengaturan") }
        Text("Winkerly v8.0.0 - Infinity", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 32.dp).align(Alignment.CenterHorizontally))
    }
}
