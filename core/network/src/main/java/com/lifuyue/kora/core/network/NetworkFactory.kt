package com.lifuyue.kora.core.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkFactory {
    fun createOkHttpClient(
        apiKeyProvider: () -> String?,
        connectTimeoutSeconds: Long = 15,
        readTimeoutSeconds: Long = 30,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(apiKeyProvider))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()

    fun createApi(
        baseUrl: String,
        apiKeyProvider: () -> String?,
        json: Json = NetworkJson.default,
    ): FastGptApi =
        createRetrofit(
            baseUrl = baseUrl,
            okHttpClient = createOkHttpClient(apiKeyProvider),
            json = json,
        ).create(FastGptApi::class.java)
}
