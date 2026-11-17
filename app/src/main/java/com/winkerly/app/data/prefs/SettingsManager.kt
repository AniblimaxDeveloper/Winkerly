package com.winkerly.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "winkerly_settings")

class SettingsManager(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL_ID = stringPreferencesKey("model_id")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val baseUrl: Flow<String> = context.dataStore.data.map { it[BASE_URL] ?: "https://openrouter.ai/api/v1" }
    val modelId: Flow<String> = context.dataStore.data.map { it[MODEL_ID] ?: "openrouter/free" }
    val systemPrompt: Flow<String> = context.dataStore.data.map { it[SYSTEM_PROMPT] ?: "You are Winkerly, a helpful AI." }
    val accentColor: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "#7C9CFF" }

    suspend fun saveSettings(key: String, url: String, model: String, prompt: String, color: String) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = key
            prefs[BASE_URL] = url
            prefs[MODEL_ID] = model
            prefs[SYSTEM_PROMPT] = prompt
            prefs[ACCENT_COLOR] = color
        }
    }
}
