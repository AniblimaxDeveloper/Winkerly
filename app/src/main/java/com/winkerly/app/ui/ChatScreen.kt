package com.winkerly.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winkerly.app.data.db.ChatMessage
import com.winkerly.app.ui.components.MarkdownText
import com.winkerly.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onSettings: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val sessions by vm.allSessions.collectAsState()
    val messages by vm.activeMessages.collectAsState()
    val activeId by vm.activeSessionId.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val streamingText by vm.streamingText.collectAsState()
    
    var input by remember { mutableStateOf("") }

    // Inisialisasi Text-to-Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(if (isGenerating) messages.size else messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Riwayat Chat", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { 
                        vm.createNewSession()
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
                            headlineContent = { Text(session.title, maxLines = 1) },
                            modifier = Modifier.clickable {
                                vm.selectSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            trailingContent = {
                                IconButton(onClick = { vm.deleteSession(session.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray)
                                }
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
                        // Tombol Share
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = {
                                val shareText = messages.joinToString("\n\n") { 
                                    "${if (it.role == "user") "You" else "Winkerly"}: ${it.content}" 
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Chat"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFF0B0F19))) {
                if (activeId == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Buat 'Chat Baru' untuk memulai", color = Color.Gray)
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                        items(messages) { msg ->
                            MessageBubble(
                                msg = msg, 
                                context = context, 
                                tts = tts,
                                onRegenerate = { vm.regenerateLastMessage() }
                            )
                        }
                        if (isGenerating) {
                            item {
                                MessageBubble(
                                    msg = ChatMessage(sessionId = activeId ?: 0L, role = "assistant", content = streamingText.ifEmpty { "..." }), 
                                    context = context, 
                                    tts = tts,
                                    isStreaming = true
                                )
                            }
                        }
                    }
                }

                // Quick Prompts (Hanya muncul kalau chat kosong)
                if (messages.isEmpty() && activeId != null && !isGenerating) {
                    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        val prompts = listOf("Tulis kode Kotlin", "Jelaskan Fisika", "Buat puisi", "Resep masakan")
                        items(prompts) { prompt ->
                            AssistChip(
                                onClick = { vm.sendMessage(prompt) },
                                label = { Text(prompt) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Winkerly...") }
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isGenerating) {
                        Button(onClick = { vm.stopGeneration() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { 
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                    } else {
                        Button(onClick = {
                            val txt = input.trim()
                            if (txt.isNotEmpty()) {
                                vm.sendMessage(txt)
                                input = ""
                            }
                        }) { Text("Send") }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: ChatMessage, 
    context: Context, 
    tts: TextToSpeech?, 
    isStreaming: Boolean = false,
    onRegenerate: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.role == "user") Color(0xFF2C3E50) else Color(0xFF141A29)
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (msg.role == "user") "You" else "Winkerly",
                        fontWeight = FontWeight.Bold,
                        color = if (msg.role == "user") Color(0xFF7C9CFF) else Color.White
                    )
                    if (msg.role == "assistant" && !isStreaming) {
                        Row {
                            // Tombol TTS (Baca Suara)
                            IconButton(onClick = { tts?.speak(msg.content, TextToSpeech.QUEUE_FLUSH, null, "") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color.Gray)
                            }
                            Spacer(Modifier.width(4.dp))
                            // Tombol Copy
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Winkerly", msg.content))
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray)
                            }
                            Spacer(Modifier.width(4.dp))
                            // Tombol Regenerate
                            IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color.Gray)
                            }
                        }
                    }
                }
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
