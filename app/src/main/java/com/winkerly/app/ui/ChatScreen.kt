package com.winkerly.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winkerly.app.data.db.ChatMessage
import com.winkerly.app.ui.components.MarkdownText
import com.winkerly.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onSettings: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val sessions by vm.filteredSessions.collectAsState()
    val messages by vm.activeMessages.collectAsState()
    val activeId by vm.activeSessionId.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val streamingText by vm.streamingText.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val fontSize by vm.fontSize.collectAsState()
    val hapticEnabled by vm.hapticEnabled.collectAsState()
    val autoCopy by vm.autoCopy.collectAsState()
    val speed by vm.ttsSpeed.collectAsState()
    val pitch by vm.ttsPitch.collectAsState()

    var input by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) input = spokenText
        }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) { 
        tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) { tts?.language = Locale.getDefault(); tts?.setSpeechRate(speed); tts?.setPitch(pitch) } } 
    }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    LaunchedEffect(messages.size, streamingText) {
        if (autoScroll && messages.isNotEmpty()) {
            listState.animateScrollToItem(if (isGenerating) messages.size else messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Riwayat Chat", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = searchQuery, onValueChange = { vm.setSearchQuery(it) }, placeholder = { Text("Cari chat...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { vm.createNewSession(); scope.launch { drawerState.close() } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Baru")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.deleteAllSessions() }) { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red) }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(sessions) { session ->
                        ListItem(
                            headlineContent = { Text(session.title, maxLines = 1) },
                            modifier = Modifier.clickable { vm.selectSession(session.id); scope.launch { drawerState.close() } },
                            trailingContent = {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                        DropdownMenuItem(text = { Text("Rename") }, onClick = { vm.renameSession(session.id, "Renamed Chat"); showMenu = false })
                                        DropdownMenuItem(text = { Text("Hapus") }, onClick = { vm.deleteSession(session.id); showMenu = false })
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = if (session.id == activeId) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Winkerly Max") },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = null) } },
                    actions = {
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = {
                                val shareText = messages.joinToString("\n\n") { "${if (it.role == "user") "You" else "Winkerly"}: ${it.content}" }
                                val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }
                                context.startActivity(Intent.createChooser(sendIntent, "Export JSON/TXT"))
                            }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                        }
                        IconButton(onClick = { autoScroll = !autoScroll }) { Icon(if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.Pause, contentDescription = "Auto Scroll") }
                        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = null) }
                    }
                )
            },
            floatingActionButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (listState.firstVisibleItemIndex > 3) {
                        FloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                        }
                        FloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(messages.size) } }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (activeId == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Mulai chat baru untuk melihat keajaiban Winkerly.", color = Color.Gray) }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                        items(messages) { msg -> MessageBubble(msg, context, tts, fontSize, autoCopy, onRegenerate = { vm.regenerateLastMessage() }) }
                        if (isGenerating) {
                            item {
                                if (streamingText.isEmpty()) TypingIndicator()
                                else MessageBubble(ChatMessage(sessionId = activeId ?: 0L, role = "assistant", content = streamingText, timestamp = System.currentTimeMillis()), context, tts, fontSize, false, isStreaming = true)
                            }
                        }
                    }
                }

                Column(Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) }
                            speechLauncher.launch(intent)
                        }) { Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary) }

                        OutlinedTextField(
                            value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message Winkerly...") },
                            trailingIcon = { if (input.isNotEmpty()) IconButton(onClick = { input = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") } },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                                val txt = input.trim()
                                if (txt.isNotEmpty()) { if(hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.sendMessage(txt); input = "" }
                            })
                        )
                        Spacer(Modifier.width(8.dp))
                        if (isGenerating) {
                            Button(onClick = { vm.stopGeneration() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Icon(Icons.Default.Stop, contentDescription = "Stop") }
                        } else {
                            Button(onClick = { 
                                val txt = input.trim()
                                if (txt.isNotEmpty()) { if(hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.sendMessage(txt); input = "" } 
                            }) { Text("Send") }
                        }
                    }
                    Text("${input.length} karakter | ~${input.length / 4} token", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 48.dp))
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val dot1 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse))
    val dot2 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = 150), RepeatMode.Reverse))
    val dot3 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = 300), RepeatMode.Reverse))
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(Color.Gray.copy(alpha = dot1), CircleShape))
        Box(Modifier.size(8.dp).background(Color.Gray.copy(alpha = dot2), CircleShape))
        Box(Modifier.size(8.dp).background(Color.Gray.copy(alpha = dot3), CircleShape))
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, context: Context, tts: TextToSpeech?, fontSize: Float, autoCopy: Boolean, isStreaming: Boolean = false, onRegenerate: () -> Unit = {}) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = formatter.format(Date(msg.timestamp))

    if (autoCopy && msg.role == "assistant" && !isStreaming) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Winkerly", msg.content))
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start) {
        if (msg.role != "user") {
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text("W", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(colors = CardDefaults.cardColors(containerColor = if (msg.role == "user") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface), modifier = Modifier.widthIn(max = 300.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (msg.role == "user") "You" else "Winkerly", fontWeight = FontWeight.Bold, color = if (msg.role == "user") MaterialTheme.colorScheme.primary else Color.White, fontSize = 12.sp)
                    Text(timeString, color = Color.Gray, fontSize = 10.sp)
                }
                Spacer(Modifier.height(4.dp))
                
                SelectionContainer {
                    if (msg.role == "user") {
                        Text(msg.content, color = Color.White, fontSize = fontSize.sp)
                    } else {
                        MarkdownText(msg.content, fontSize)
                    }
                }

                if (msg.role == "assistant" && !isStreaming) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { tts?.speak(msg.content, TextToSpeech.QUEUE_FLUSH, null, "") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color.Gray) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Winkerly", msg.content)) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, msg.content); type = "text/plain" }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Message"))
                        }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color.Gray) }
                    }
                }
            }
        }

        if (msg.role == "user") {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
                Text("U", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
