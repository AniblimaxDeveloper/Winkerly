package com.winkerly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(correctPin: String, onUnlock: () -> Unit) {
    var inputPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Winkerly is Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = inputPin,
            onValueChange = { 
                if (it.length <= 4) {
                    inputPin = it
                    error = false
                    if (it == correctPin) onUnlock()
                }
            },
            label = { Text("Enter PIN") },
            visualTransformation = PasswordVisualTransformation(),
            isError = error,
            modifier = Modifier.fillMaxWidth()
        )
        if (error) {
            Text("Incorrect PIN", color = MaterialTheme.colorScheme.error)
        }
    }
}
