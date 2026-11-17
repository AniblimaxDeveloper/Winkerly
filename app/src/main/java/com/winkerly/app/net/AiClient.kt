package com.winkerly.app.net

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiClient(
    private val baseUrl: String, private val apiKey: String, private val model: String,
    private val temperature: Float = 0.7f, private val maxTokens: Int = 2048
) {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(5, TimeUnit.MINUTES).build()

    fun streamChat(history: List<Pair<String, String>>, onToken: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit) {
        val messages = JSONArray()
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }
        val body = JSONObject().put("model", model).put("messages", messages).put("stream", true)
            .put("temperature", temperature).put("max_tokens", maxTokens).toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url("${baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "network error")
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) return onError("HTTP ${it.code}: ${it.body?.string()?.take(300)}")
                    val source = it.body?.source() ?: return onError("empty body")
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val delta = JSONObject(data).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content")
                            if (!delta.isNullOrEmpty()) onToken(delta)
                        } catch (_: Exception) { }
                    }
                    onDone()
                }
            }
        })
    }
}
