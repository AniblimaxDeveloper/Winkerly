package com.winkerly.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = floatPreferencesKey("max_tokens")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val AUTO_COPY = booleanPreferencesKey("auto_copy")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val APP_LOCK = booleanPreferencesKey("app_lock")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val baseUrl: Flow<String> = context.dataStore.data.map { it[BASE_URL] ?: "https://openrouter.ai/api/v1" }
    val modelId: Flow<String> = context.dataStore.data.map { it[MODEL_ID] ?: "openrouter/free" }
    val systemPrompt: Flow<String> = context.dataStore.data.map { it[SYSTEM_PROMPT] ?: "You are Winkerly." }
    val accentColor: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "#7C9CFF" }
    val temperature: Flow<Float> = context.dataStore.data.map { it[TEMPERATURE] ?: 0.7f }
    val maxTokens: Flow<Float> = context.dataStore.data.map { it[MAX_TOKENS] ?: 2048f }
    val fontSize: Flow<Float> = context.dataStore.data.map { it[FONT_SIZE] ?: 16f }
    val ttsSpeed: Flow<Float> = context.dataStore.data.map { it[TTS_SPEED] ?: 1.0f }
    val ttsPitch: Flow<Float> = context.dataStore.data.map { it[TTS_PITCH] ?: 1.0f }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED] ?: true }
    val autoCopy: Flow<Boolean> = context.dataStore.data.map { it[AUTO_COPY] ?: false }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: true }
    val appLock: Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK] ?: false }

    suspend fun saveSettings(
        key: String, url: String, model: String, prompt: String, color: String, 
        temp: Float, tokens: Float, font: Float, speed: Float, pitch: Float, 
        haptic: Boolean, autoCopy: Boolean, dark: Boolean, lock: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = key
            prefs[BASE_URL] = url
            prefs[MODEL_ID] = model
            prefs[SYSTEM_PROMPT] = prompt
            prefs[ACCENT_COLOR] = color
            prefs[TEMPERATURE] = temp
            prefs[MAX_TOKENS] = tokens
            prefs[FONT_SIZE] = font
            prefs[TTS_SPEED] = speed
            prefs[TTS_PITCH] = pitch
            prefs[HAPTIC_ENABLED] = haptic
            prefs[AUTO_COPY] = autoCopy
            prefs[DARK_MODE] = dark
            prefs[APP_LOCK] = lock
        }
    }
}
