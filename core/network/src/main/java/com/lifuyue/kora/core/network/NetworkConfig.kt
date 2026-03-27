package com.lifuyue.kora.core.network

fun interface BaseUrlProvider {
    fun getBaseUrl(): String
}

data class StaticBaseUrlProvider(
    private val baseUrl: String,
) : BaseUrlProvider {
    override fun getBaseUrl(): String = baseUrl
}
