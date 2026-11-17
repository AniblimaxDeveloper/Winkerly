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
    val savedSpeed by vm.ttsSpeed.collectAsState()
    val savedPitch by vm.ttsPitch.collectAsState()
    val savedHaptic by vm.hapticEnabled.collectAsState()
    val savedAutoCopy by vm.autoCopy.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    var key by remember { mutableStateOf(savedKey) }
    var selectedColor by remember { mutableStateOf(savedColor) }
    var temp by remember { mutableStateOf(savedTemp) }
    var tokens by remember { mutableStateOf(savedTokens) }
    var font by remember { mutableStateOf(savedFont) }
    var speed by remember { mutableStateOf(savedSpeed) }
    var pitch by remember { mutableStateOf(savedPitch) }
    var haptic by remember { mutableStateOf(savedHaptic) }
    var autoCopy by remember { mutableStateOf(savedAutoCopy) }
    var showKey by remember { mutableStateOf(false) }

    var expandedModel by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("openrouter/free") }
    val models = listOf("openrouter/free", "meta-llama/llama-3-8b-instruct:free", "microsoft/phi-3-mini-128k-instruct:free", "google/gemma-2-9b-it:free")

    var expandedPersona by remember { mutableStateOf(false) }
    var selectedPersona by remember { mutableStateOf("Default") }
    val personas = listOf("Default", "Programmer Senior", "Penulis Kreatif", "Guru Sabar")

    val colors = listOf("#7C9CFF", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0")

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan Winkerly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = key, onValueChange = { key = it }, label = { Text("API Key OpenRouter") }, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null) } }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.testConnection(key) }, modifier = Modifier.fillMaxWidth()) { Text("Test Koneksi") }
        if (errorMsg != null) Text(errorMsg!!, color = if(errorMsg!!.contains("Berhasil")) Color.Green else Color.Red, modifier = Modifier.padding(vertical = 4.dp))

        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(expanded = expandedModel, onExpandedChange = { expandedModel = !expandedModel }) {
            OutlinedTextField(value = selectedModel, onValueChange = {}, readOnly = true, label = { Text("Model AI") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expandedModel, onDismissRequest = { expandedModel = false }) {
                models.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { selectedModel = m; expandedModel = false }) }
            }
        }

        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expandedPersona, onExpandedChange = { expandedPersona = !expandedPersona }) {
            OutlinedTextField(value = selectedPersona, onValueChange = {}, readOnly = true, label = { Text("Persona System Prompt") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPersona) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expandedPersona, onDismissRequest = { expandedPersona = false }) {
                personas.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { selectedPersona = p; expandedPersona = false }) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Warna Tema", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            colors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                Box(modifier = Modifier.size(40.dp).background(color, CircleShape).border(if (selectedColor == hex) 3.dp else 0.dp, Color.White, CircleShape).clickable { selectedColor = hex })
            }
        }

        Spacer(Modifier.height(8.dp))
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
            Text("Auto-Copy Jawaban AI"); Switch(checked = autoCopy, onCheckedChange = { autoCopy = it })
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { 
            val finalPrompt = when(selectedPersona) {
                "Programmer Senior" -> "You are a senior programmer. Provide clean, efficient code."
                "Penulis Kreatif" -> "You are a creative writer. Write engaging stories."
                "Guru Sabar" -> "You are a patient teacher. Explain things simply."
                else -> "You are Winkerly, a helpful AI."
            }
            vm.saveSettings(key, selectedModel, finalPrompt, selectedColor, temp, tokens, font, speed, pitch, haptic, autoCopy)
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Pengaturan") }
        Text("Winkerly v7.0.0 - Max Edition", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 32.dp).align(Alignment.CenterHorizontally))
    }
}
