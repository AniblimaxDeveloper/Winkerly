package com.winkerly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    currentApiKey: String,
    currentBaseUrl: String,
    currentModel: String,
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var key by remember { mutableStateOf(currentApiKey) }
    var url by remember { mutableStateOf(currentBaseUrl) }
    var model by remember { mutableStateOf(currentModel) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key (sk-or-v1-...)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onBack) { Text("Batal") }
            Button(onClick = { onSave(key, url, model) }) { Text("Simpan & Mulai") }
        }
    }
}
