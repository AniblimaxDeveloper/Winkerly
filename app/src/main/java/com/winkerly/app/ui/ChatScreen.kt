package com.winkerly.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winkerly.app.data.ProviderConfig
import com.winkerly.app.net.AiClient

@Composable
fun ChatScreen(onSettings: () -> Unit) {
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var input by remember { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    val config = remember { ProviderConfig() }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0F19)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Winkerly", color = Color.White, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = false) {
            items(messages) { (role, text) ->
                Text(text = "${if (role == "user") "You" else "Winkerly"}: $text", color = if (role == "user") Color(0xFF7C9CFF) else Color.White, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message Winkerly...") }, enabled = !streaming)
            Spacer(Modifier.width(8.dp))
            Button(enabled = !streaming && input.isNotBlank(), onClick = {
                val userMsg = input.trim()
                if (userMsg.isEmpty()) return@Button
                input = ""
                messages.add("user" to userMsg)
                messages.add("assistant" to "")
                streaming = true
                val client = AiClient(config.baseUrl, config.apiKey, config.model)
                client.streamChat(history = messages.dropLast(1),
                    onToken = { token -> val last = messages.last(); messages[messages.lastIndex] = last.first to (last.second + token) },
                    onError = { err -> val last = messages.last(); messages[messages.lastIndex] = last.first to "Error: $err"; streaming = false },
                    onDone = { streaming = false }
                )
            }) { Text(if (streaming) "..." else "Send") }
        }
    }
}
