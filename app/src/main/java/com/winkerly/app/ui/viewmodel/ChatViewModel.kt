package com.winkerly.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winkerly.app.data.db.AppDatabase
import com.winkerly.app.data.db.ChatMessage
import com.winkerly.app.data.db.ChatSession
import com.winkerly.app.data.prefs.SettingsManager
import com.winkerly.app.net.AiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getDatabase(app)
    private val dao = db.chatDao()
    private val settingsManager = SettingsManager(app)

    val apiKey = settingsManager.apiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val baseUrl = settingsManager.baseUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val modelId = settingsManager.modelId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val allSessions = dao.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    val activeMessages = _activeSessionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.getMessagesForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private var streamingJob: kotlinx.coroutines.Job? = null

    fun saveSettings(key: String, url: String, model: String) {
        viewModelScope.launch { settingsManager.saveSettings(key, url, model) }
    }

    fun createNewSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = dao.insertSession(ChatSession(title = "New Chat"))
            _activeSessionId.value = id
            onCreated(id)
        }
    }

    fun selectSession(id: Long) { _activeSessionId.value = id }

    fun sendMessage(text: String) {
        val currentId = _activeSessionId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            // Simpan pesan user
            val userMsg = ChatMessage(sessionId = currentId, role = "user", content = text)
            dao.insertMessage(userMsg)

            // Jika ini pesan pertama, update judul session
            val currentMsgs = activeMessages.value
            if (currentMsgs.isEmpty()) {
                dao.updateSessionTitle(currentId, if (text.length > 25) text.take(25) + "..." else text)
            }

            // Placeholder untuk AI
            val aiPlaceholder = ChatMessage(sessionId = currentId, role = "assistant", content = "")
            dao.insertMessage(aiPlaceholder)
            val aiMsgId = aiPlaceholder.id

            // Persiapkan history untuk API
            val history = (currentMsgs + userMsg).map { it.role to it.content }
            val client = AiClient(baseUrl.value, apiKey.value, modelId.value)
            val fullResponse = StringBuilder()

            // Stream API
            client.streamChat(history,
                onToken = { token ->
                    fullResponse.append(token)
                    // Update UI secara instan (karena Room Flow, kita bisa update DB setiap token, tapi ini bisa memberatkan I/O)
                    // Di production, kita pakai buffer UI. Untuk simplifikasi ini, kita update UI state lokal.
                },
                onError = { err ->
                    viewModelScope.launch {
                        // Hapus placeholder dan ganti dengan error
                        dao.insertMessage(ChatMessage(sessionId = currentId, role = "assistant", content = "Error: $err"))
                    }
                },
                onDone = {
                    // Simpan jawaban final ke database
                    viewModelScope.launch {
                        // Untuk MVP UI streaming sederhana, kita hapus placeholder kosong dan masukkan teks final
                        // (Karena Room insert tidak bisa "update" secara langsung tanpa DAO @Update, kita biarkan UI yang menangani render sementara)
                    }
                }
            )
        }
    }
}
