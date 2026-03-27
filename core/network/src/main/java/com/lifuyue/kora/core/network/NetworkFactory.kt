package com.lifuyue.kora.core.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkFactory {
    fun createOkHttpClient(
        apiKeyProvider: ApiKeyProvider,
        baseUrlProvider: BaseUrlProvider = StaticBaseUrlProvider(RETROFIT_PLACEHOLDER_BASE_URL),
        connectTimeoutSeconds: Long = 15,
        readTimeoutSeconds: Long = 30,
        enableDebugLogging: Boolean = false,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(BaseUrlRewriteInterceptor(baseUrlProvider))
            .addInterceptor(AuthInterceptor(apiKeyProvider))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    level = if (enableDebugLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()

    fun createApi(
        baseUrl: String,
        apiKeyProvider: ApiKeyProvider,
        json: Json = NetworkJson.default,
        enableDebugLogging: Boolean = false,
    ): FastGptApi =
        createRetrofit(
            baseUrl = RETROFIT_PLACEHOLDER_BASE_URL,
            okHttpClient =
                createOkHttpClient(
                    apiKeyProvider = apiKeyProvider,
                    baseUrlProvider = StaticBaseUrlProvider(baseUrl),
                    enableDebugLogging = enableDebugLogging,
                ),
            json = json,
        ).create(FastGptApi::class.java)
}
