package com.winkerly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.winkerly.app.ui.viewmodel.ChatViewModel

@Composable
fun SettingsScreen(vm: ChatViewModel, onClose: () -> Unit) {
    val savedKey by vm.apiKey.collectAsState()
    val savedUrl by vm.baseUrl.collectAsState()
    val savedModel by vm.modelId.collectAsState()

    var key by remember { mutableStateOf(savedKey) }
    var url by remember { mutableStateOf(savedUrl) }
    var model by remember { mutableStateOf(savedModel) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan Winkerly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Rekomendasi Model: openrouter/free, google/gemma-2-9b-it:free", style = MaterialTheme.typography.bodySmall)
        
        Spacer(Modifier.height(24.dp))
        Button(onClick = { 
            vm.saveSettings(key, url, model)
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { 
            Text("Simpan & Mulai") 
        }
    }
}
