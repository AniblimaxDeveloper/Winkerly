package com.winkerly.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winkerly.app.data.db.AppDatabase
import com.winkerly.app.data.db.ChatMessage
import com.winkerly.app.data.db.ChatSession
import com.winkerly.app.data.prefs.SettingsManager
import com.winkerly.app.net.AiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getDatabase(app)
    private val dao = db.chatDao()
    private val settingsManager = SettingsManager(app)

    val apiKey = settingsManager.apiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val baseUrl = settingsManager.baseUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val modelId = settingsManager.modelId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val systemPrompt = settingsManager.systemPrompt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val accentColor = settingsManager.accentColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "#10A37F")
    val temperature = settingsManager.temperature.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.7f)
    val topP = settingsManager.topP.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
    val freqPenalty = settingsManager.freqPenalty.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0f)
    val presPenalty = settingsManager.presPenalty.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0f)
    val maxTokens = settingsManager.maxTokens.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 2048f)
    val fontSize = settingsManager.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 16f)
    val ttsSpeed = settingsManager.ttsSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
    val ttsPitch = settingsManager.ttsPitch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
    val hapticEnabled = settingsManager.hapticEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val autoCopy = settingsManager.autoCopy.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val darkMode = settingsManager.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val allSessions = dao.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    val filteredSessions = combine(allSessions, _searchQuery) { sessions, query ->
        if (query.isBlank()) sessions else sessions.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    val activeMessages = _activeSessionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.getMessagesForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private var streamingJob: Job? = null
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText = _streamingText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun clearError() { _errorMessage.value = null }

    fun saveSettings(
        key: String, url: String, model: String, prompt: String, color: String, 
        temp: Float, topP: Float, freq: Float, pres: Float, tokens: Float, 
        font: Float, speed: Float, pitch: Float, haptic: Boolean, autoCopy: Boolean, dark: Boolean
    ) {
        viewModelScope.launch { 
            settingsManager.saveSettings(key, url, model, prompt, color, temp, topP, freq, pres, tokens, font, speed, pitch, haptic, autoCopy, dark) 
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val id = dao.insertSession(ChatSession(title = "New Chat"))
            _activeSessionId.value = id
            _streamingText.value = ""
            _isGenerating.value = false
        }
    }

    fun selectSession(id: Long) { stopGeneration(); _activeSessionId.value = id; _streamingText.value = "" }
    fun renameSession(id: Long, newTitle: String) { viewModelScope.launch { dao.updateSessionTitle(id, newTitle) } }
    fun togglePinSession(id: Long, currentPinState: Boolean) { viewModelScope.launch { dao.togglePinSession(id, !currentPinState) } }
    fun deleteMessage(id: Long) { viewModelScope.launch { dao.deleteMessageById(id) } }
    
    fun clearCurrentChat() {
        val id = _activeSessionId.value ?: return
        viewModelScope.launch { dao.deleteMessagesForSession(id) }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            dao.deleteMessagesForSession(id)
            dao.deleteSessionById(id)
            if (_activeSessionId.value == id) _activeSessionId.value = null
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch { dao.deleteAllMessages(); dao.deleteAllSessions(); _activeSessionId.value = null }
    }

    fun stopGeneration() { streamingJob?.cancel(); _isGenerating.value = false }

    fun sendMessage(text: String, isRegenerate: Boolean = false, overrideModel: String? = null) {
        val currentId = _activeSessionId.value ?: return
        if (text.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _streamingText.value = ""

        streamingJob = viewModelScope.launch {
            if (!isRegenerate) {
                val userMsg = ChatMessage(sessionId = currentId, role = "user", content = text)
                dao.insertMessage(userMsg)
                if (activeMessages.value.isEmpty()) dao.updateSessionTitle(currentId, if (text.length > 25) text.take(25) + "..." else text)
            }

            val history = mutableListOf<Pair<String, String>>()
            if (systemPrompt.value.isNotBlank()) history.add("system" to systemPrompt.value)
            activeMessages.value.forEach { history.add(it.role to it.content) }

            val selectedModel = overrideModel ?: modelId.value
            val client = AiClient(
                baseUrl.value, apiKey.value, selectedModel, 
                temperature.value, maxTokens.value.toInt(), 
                topP.value, freqPenalty.value, presPenalty.value
            )
            val fullResponse = StringBuilder()

            client.streamChat(history,
                onToken = { token -> fullResponse.append(token); _streamingText.value = fullResponse.toString() },
                onError = { err -> _isGenerating.value = false; viewModelScope.launch { _errorMessage.value = "Error: $err"; _streamingText.value = "" } },
                onDone = {
                    _isGenerating.value = false
                    viewModelScope.launch {
                        dao.insertMessage(ChatMessage(sessionId = currentId, role = "assistant", content = fullResponse.toString(), modelUsed = selectedModel))
                        _streamingText.value = ""
                    }
                }
            )
        }
    }

    fun regenerateLastMessage(overrideModel: String? = null) {
        val msgs = activeMessages.value
        if (msgs.isNotEmpty() && msgs.last().role == "assistant") {
            val lastUserMsg = msgs.findLast { it.role == "user" } ?: return
            viewModelScope.launch { sendMessage(lastUserMsg.content, isRegenerate = true, overrideModel = overrideModel) }
        }
    }
}
