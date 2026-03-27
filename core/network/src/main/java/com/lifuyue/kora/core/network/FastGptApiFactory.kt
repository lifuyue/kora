package com.lifuyue.kora.core.network

import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastGptApiFactory @Inject constructor(
    private val json: Json,
) {
    constructor() : this(NetworkJson.default)

    fun create(
        baseUrl: String,
        apiKey: String? = null,
        enableDebugLogging: Boolean = false,
    ): FastGptApi =
        createRetrofit(
            baseUrl = RETROFIT_PLACEHOLDER_BASE_URL,
            okHttpClient =
                NetworkFactory.createOkHttpClient(
                    apiKeyProvider = StaticApiKeyProvider(apiKey),
                    baseUrlProvider = StaticBaseUrlProvider(ConnectionConfig.normalizeBaseUrl(baseUrl)),
                    enableDebugLogging = enableDebugLogging,
                ),
            json = json,
        ).create(FastGptApi::class.java)
}
