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

    val allSessions = dao.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    val activeMessages = _activeSessionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.getMessagesForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private var streamingJob: Job? = null
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    // State sementara untuk render teks yang sedang di-stream
    private val _streamingText = MutableStateFlow("")
    val streamingText = _streamingText.asStateFlow()

    fun saveSettings(key: String, url: String, model: String, prompt: String) {
        viewModelScope.launch { settingsManager.saveSettings(key, url, model, prompt) }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val id = dao.insertSession(ChatSession(title = "New Chat"))
            _activeSessionId.value = id
            _streamingText.value = ""
            _isGenerating.value = false
        }
    }

    fun selectSession(id: Long) {
        stopGeneration()
        _activeSessionId.value = id
        _streamingText.value = ""
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            dao.deleteMessagesForSession(id)
            dao.deleteSessionById(id)
            if (_activeSessionId.value == id) {
                _activeSessionId.value = null
            }
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isGenerating.value = false
    }

    fun sendMessage(text: String) {
        val currentId = _activeSessionId.value ?: return
        if (text.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _streamingText.value = ""

        streamingJob = viewModelScope.launch {
            val userMsg = ChatMessage(sessionId = currentId, role = "user", content = text)
            dao.insertMessage(userMsg)

            val currentMsgs = activeMessages.value
            if (currentMsgs.isEmpty()) {
                dao.updateSessionTitle(currentId, if (text.length > 25) text.take(25) + "..." else text)
            }

            // Persiapkan history dengan System Prompt
            val history = mutableListOf<Pair<String, String>>()
            if (systemPrompt.value.isNotBlank()) {
                history.add("system" to systemPrompt.value)
            }
            (currentMsgs + userMsg).forEach { history.add(it.role to it.content) }

            val client = AiClient(baseUrl.value, apiKey.value, modelId.value)
            val fullResponse = StringBuilder()

            client.streamChat(history,
                onToken = { token ->
                    fullResponse.append(token)
                    _streamingText.value = fullResponse.toString()
                },
                onError = { err ->
                    _isGenerating.value = false
                    viewModelScope.launch {
                        dao.insertMessage(ChatMessage(sessionId = currentId, role = "assistant", content = "Error: $err"))
                        _streamingText.value = ""
                    }
                },
                onDone = {
                    _isGenerating.value = false
                    viewModelScope.launch {
                        // Simpan hasil final ke Room Database
                        dao.insertMessage(ChatMessage(sessionId = currentId, role = "assistant", content = fullResponse.toString()))
                        _streamingText.value = ""
                    }
                }
            )
        }
    }
}
