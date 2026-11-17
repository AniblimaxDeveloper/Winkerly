package com.winkerly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.winkerly.app.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onClose: () -> Unit) {
    val savedKey by vm.apiKey.collectAsState()
    val savedUrl by vm.baseUrl.collectAsState()
    val savedModel by vm.modelId.collectAsState()
    val savedPrompt by vm.systemPrompt.collectAsState()

    var key by remember { mutableStateOf(savedKey) }
    var url by remember { mutableStateOf(savedUrl) }
    var model by remember { mutableStateOf(savedModel) }
    var prompt by remember { mutableStateOf(savedPrompt) }
    
    var expanded by remember { mutableStateOf(false) }
    val models = listOf(
        "openrouter/free",
        "google/gemma-2-9b-it:free",
        "meta-llama/llama-3-8b-instruct:free",
        "microsoft/phi-3-mini-128k-instruct:free",
        "mistralai/mistral-7b-instruct:free"
    )

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan Winkerly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        // Dropdown Model
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("Model AI") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m) },
                        onClick = { model = m; expanded = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = prompt, onValueChange = { prompt = it },
            label = { Text("System Prompt (Persona)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        Spacer(Modifier.height(24.dp))
        Button(onClick = { 
            vm.saveSettings(key, url, model, prompt)
            onClose() 
        }, modifier = Modifier.fillMaxWidth()) { 
            Text("Simpan & Mulai") 
        }
    }
}
