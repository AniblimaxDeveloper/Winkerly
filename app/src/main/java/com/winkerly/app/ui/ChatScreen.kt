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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winkerly.app.data.db.ChatMessage
import com.winkerly.app.ui.components.MarkdownText
import com.winkerly.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
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
    val currentModel by vm.modelId.collectAsState()

    var input by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    var selectedMessageForSheet by remember { mutableStateOf<ChatMessage?>(null) }
    var expandedModelMenu by remember { mutableStateOf(false) }
    
    val models = listOf("openrouter/free", "google/gemma-2-9b-it:free", "meta-llama/llama-3-8b-instruct:free")

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
        if (autoScroll && messages.isNotEmpty()) listState.animateScrollToItem(if (isGenerating) messages.size else messages.size - 1)
    }

    if (selectedMessageForSheet != null) {
        ModalBottomSheet(onDismissRequest = { selectedMessageForSheet = null }) {
            val msgContent = selectedMessageForSheet?.content ?: ""
            Column(Modifier.padding(16.dp)) {
                Text("Message Options", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { tts?.speak(msgContent, TextToSpeech.QUEUE_FLUSH, null, ""); selectedMessageForSheet = null }) { Icon(Icons.Default.VolumeUp, contentDescription = "TTS") }
                    IconButton(onClick = { 
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Winkerly", msgContent))
                        selectedMessageForSheet = null 
                    }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                    IconButton(onClick = { 
                        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, msgContent); type = "text/plain" }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Message")); selectedMessageForSheet = null 
                    }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    IconButton(onClick = { vm.regenerateLastMessage(); selectedMessageForSheet = null }) { Icon(Icons.Default.Refresh, contentDescription = "Regenerate") }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(16.dp)) {
                    Button(
                        onClick = { vm.createNewSession(); scope.launch { drawerState.close() } }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Chat", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { vm.setSearchQuery(it) }, placeholder = { Text("Search chats...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                
                LazyColumn(Modifier.weight(1f)) {
                    items(sessions) { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { vm.selectSession(session.id); scope.launch { drawerState.close() } }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text(session.title, maxLines = 1, modifier = Modifier.weight(1f), fontSize = 14.sp, color = if (session.id == activeId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray) }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(text = { Text(if (session.isPinned) "Unpin" else "Pin") }, onClick = { vm.togglePinSession(session.id, session.isPinned); showMenu = false })
                                    DropdownMenuItem(text = { Text("Rename") }, onClick = { vm.renameSession(session.id, "Renamed"); showMenu = false })
                                    DropdownMenuItem(text = { Text("Delete") }, onClick = { vm.deleteSession(session.id); showMenu = false })
                                }
                            }
                        }
                    }
                }
                
                Divider()
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Winkerly User", fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expandedModelMenu = true }) {
                                Text(currentModel.substringAfterLast("/"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expandedModelMenu, onDismissRequest = { expandedModelMenu = false }) {
                                models.forEach { model ->
                                    DropdownMenuItem(text = { Text(model) }, onClick = { 
                                        vm.saveSettings("", "", model, "", "", 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, true, false, true) // Just to update model logic (will be refined)
                                        expandedModelMenu = false 
                                    })
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = null) } },
                    actions = {
                        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = null) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            floatingActionButton = {
                if (listState.firstVisibleItemIndex > 3) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(messages.size) } }, 
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant, 
                        shape = CircleShape, 
                        modifier = Modifier.size(40.dp)
                    ) { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (activeId == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { 
                        Text("How can I help you today?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) 
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(messages) { msg -> 
                            ChatGPTMessageRow(
                                msg = msg, 
                                context = context, 
                                fontSize = fontSize, 
                                autoCopy = autoCopy, 
                                onLongPress = { selectedMessageForSheet = msg },
                                onDelete = { vm.deleteMessage(msg.id) }
                            )
                        }
                        if (isGenerating) {
                            item {
                                if (streamingText.isEmpty()) TypingIndicator()
                                else ChatGPTMessageRow(
                                    msg = ChatMessage(sessionId = activeId ?: 0L, role = "assistant", content = streamingText, timestamp = System.currentTimeMillis()), 
                                    context = context, 
                                    fontSize = fontSize, 
                                    autoCopy = false, 
                                    onLongPress = {},
                                    onDelete = {},
                                    isStreaming = true
                                ) 
                            }
                        }
                    }
                }

                // ChatGPT Style Input Box
                Box(Modifier.fillMaxWidth().padding(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* Attachment placeholder */ }) { Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color.Gray) }
                            
                            BasicTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 4,
                                decorationBox = { innerTextField ->
                                    if (input.isEmpty()) {
                                        Text("Message Winkerly...", color = Color.Gray)
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) }
                                speechLauncher.launch(intent)
                            }) { Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.Gray) }

                            if (input.isNotBlank()) {
                                IconButton(
                                    onClick = { 
                                        if (isGenerating) vm.stopGeneration()
                                        else {
                                            if(hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            vm.sendMessage(input)
                                            input = ""
                                        }
                                    },
                                    modifier = Modifier.background(if (isGenerating) Color.Red else MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatGPTMessageRow(
    msg: ChatMessage, 
    context: Context, 
    fontSize: Float, 
    autoCopy: Boolean, 
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    isStreaming: Boolean = false
) {
    if (autoCopy && msg.role == "assistant" && !isStreaming) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Winkerly", msg.content))
    }

    val isUser = msg.role == "user"
    val bgColor = if (isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(Modifier.fillMaxWidth().background(bgColor).clickable(onClick = onLongPress).padding(vertical = 16.dp, horizontal = 16.dp), contentAlignment = alignment) {
        Row(Modifier.widthIn(max = 600.dp), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
            if (!isUser) {
                Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
            }

            SelectionContainer {
                if (isUser) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                        Text(msg.content, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = fontSize.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(Modifier.weight(1f, fill = false)) {
                        MarkdownText(msg.content, fontSize)
                    }
                }
            }
            
            if (isUser) {
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
                    Text("U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
        Box(Modifier.size(6.dp).background(Color.Gray.copy(alpha = dot1), CircleShape))
        Box(Modifier.size(6.dp).background(Color.Gray.copy(alpha = dot2), CircleShape))
        Box(Modifier.size(6.dp).background(Color.Gray.copy(alpha = dot3), CircleShape))
    }
}
