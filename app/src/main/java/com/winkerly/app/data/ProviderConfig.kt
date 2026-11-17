package com.winkerly.app.data

data class ProviderConfig(
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val model: String = "nvidia/nemotron-3-super-120b-a12b:free"
)
