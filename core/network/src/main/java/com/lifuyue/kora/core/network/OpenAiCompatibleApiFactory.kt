package com.lifuyue.kora.core.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class OpenAiCompatibleApiFactory
    constructor(
        private val json: Json = NetworkJson.default,
    ) {
        fun create(
            baseUrl: String,
            apiKey: String,
            enableDebugLogging: Boolean = false,
        ): OpenAiCompatibleApi {
            val connectionProvider =
                MutableConnectionProvider(
                    initialSnapshot =
                        com.lifuyue.kora.core.common.ConnectionSnapshot(
                            connectionType = com.lifuyue.kora.core.common.ConnectionType.OPENAI_COMPATIBLE,
                            serverBaseUrl = ConnectionConfig.normalizeOpenAiCompatibleBaseUrl(baseUrl),
                            apiKey = apiKey.trim(),
                        ),
                )
            val authInterceptor = AuthInterceptor(apiKeyProvider = connectionProvider)
            val logger =
                HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    level = if (enableDebugLogging) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                }
            val client =
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(BaseUrlRewriteInterceptor(connectionProvider))
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logger)
                    .build()

            return createRetrofit(RETROFIT_PLACEHOLDER_BASE_URL, client, json).create(OpenAiCompatibleApi::class.java)
        }
    }
