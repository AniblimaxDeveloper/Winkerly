package com.winkerly.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    val savedTheme by vm.themeMode.collectAsState()
    val savedLock by vm.appLock.collectAsState()
    val savedPin by vm.appPin.collectAsState()

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
    var theme by remember { mutableStateOf(savedTheme) }
    var lock by remember { mutableStateOf(savedLock) }
    var pin by remember { mutableStateOf(savedPin) }
    var showKey by remember { mutableStateOf(false) }

    val colors = listOf("#10A37F", "#7C9CFF", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("API Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null) } }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model ID") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(Modifier.height(16.dp))
            Text("Advanced Parameters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            Text("Temperature: ${String.format("%.2f", temp)}")
            Slider(value = temp, onValueChange = { temp = it }, valueRange = 0f..2f)
            Text("Max Tokens: ${tokens.toInt()}")
            Slider(value = tokens, onValueChange = { tokens = it }, valueRange = 256f..4096f, steps = 15)

            Spacer(Modifier.height(16.dp))
            Text("UI & Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = theme == "system", onClick = { theme = "system" }, label = { Text("System") })
                FilterChip(selected = theme == "light", onClick = { theme = "light" }, label = { Text("Light") })
                FilterChip(selected = theme == "dark", onClick = { theme = "dark" }, label = { Text("Dark") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                colors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Box(modifier = Modifier.size(40.dp).background(color, CircleShape).border(if (selectedColor == hex) 3.dp else 0.dp, Color.White, CircleShape).clickable { selectedColor = hex })
                }
            }
            Text("Font Size: ${font.toInt()} sp")
            Slider(value = font, onValueChange = { font = it }, valueRange = 12f..24f)

            Spacer(Modifier.height(16.dp))
            Text("Security & Audio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("App Lock (PIN)"); Switch(checked = lock, onCheckedChange = { lock = it })
            }
            if (lock) {
                OutlinedTextField(value = pin, onValueChange = { if(it.length <= 4) pin = it }, label = { Text("Set 4-Digit PIN") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Haptic Feedback"); Switch(checked = haptic, onCheckedChange = { haptic = it })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-Copy Response"); Switch(checked = autoCopy, onCheckedChange = { autoCopy = it })
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = { 
                vm.saveSettings(key, url, model, "You are Winkerly.", selectedColor, temp, tokens, font, speed, pitch, haptic, autoCopy, theme, lock, pin)
                onClose() 
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Settings") }
            Spacer(Modifier.height(32.dp))
        }
    }
}
