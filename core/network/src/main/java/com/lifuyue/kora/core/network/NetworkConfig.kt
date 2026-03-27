package com.lifuyue.kora.core.network

const val RETROFIT_PLACEHOLDER_BASE_URL = "https://placeholder.invalid/"

fun interface BaseUrlProvider {
    fun getBaseUrl(): String
}

fun interface ApiKeyProvider {
    fun getApiKey(): String?
}

data class StaticBaseUrlProvider(
    private val baseUrl: String,
) : BaseUrlProvider {
    override fun getBaseUrl(): String = baseUrl
}

data class StaticApiKeyProvider(
    private val apiKey: String?,
) : ApiKeyProvider {
    override fun getApiKey(): String? = apiKey
}
