package com.winkerly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.winkerly.app.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onClose: () -> Unit) {
    val savedKey by vm.apiKey.collectAsState()
    var key by remember { mutableStateOf(savedKey) }

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
        Button(onClick = { 
            // URL dan Model dikunci otomatis di sini
            vm.saveSettings(key, "https://openrouter.ai/api/v1", "openrouter/free", "You are Winkerly, a helpful AI.")
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { 
            Text("Simpan") 
        }
    }
}
