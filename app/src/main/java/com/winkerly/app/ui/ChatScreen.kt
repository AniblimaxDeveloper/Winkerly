package com.winkerly.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winkerly.app.ui.components.MarkdownText
import com.winkerly.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onSettings: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sessions by vm.allSessions.collectAsState()
    val messages by vm.activeMessages.collectAsState()
    val activeId by vm.activeSessionId.collectAsState()
    
    var input by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Riwayat Chat", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { 
                        vm.createNewSession { }
                        scope.launch { drawerState.close() }
                    }, 
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Chat Baru")
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(sessions) { session ->
                        ListItem(
                            headlineContent = { Text(session.title) },
                            modifier = Modifier.clickable {
                                vm.selectSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (session.id == activeId) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Winkerly") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFF0B0F19))) {
                // Jika belum ada chat terpilih
                if (activeId == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Klik ikon menu (kiri atas) lalu pilih 'Chat Baru'", color = Color.Gray)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                        items(messages) { msg ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.role == "user") Color(0xFF2C3E50) else Color(0xFF141A29)
                                    ),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (msg.role == "user") "You" else "Winkerly",
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.role == "user") Color(0xFF7C9CFF) else Color.White
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        if (msg.role == "user") {
                                            Text(msg.content, color = Color.White)
                                        } else {
                                            MarkdownText(msg.content)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Bar
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Winkerly...") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val txt = input.trim()
                            if (txt.isNotEmpty()) {
                                vm.sendMessage(txt)
                                input = ""
                            }
                        }
                    ) { Text("Send") }
                }
            }
        }
    }
}
