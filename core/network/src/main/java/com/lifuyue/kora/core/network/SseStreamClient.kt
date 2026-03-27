package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.NetworkError
import com.lifuyue.kora.core.common.ResponseEnvelope
import com.lifuyue.kora.core.common.toNetworkError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SseStreamClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json = NetworkJson.default,
    private val baseUrlProvider: BaseUrlProvider,
    private val collector: ChatStreamCollector = ChatStreamCollector(),
) {
    fun streamChatCompletions(request: ChatCompletionRequest): Flow<SseEventData> =
        flow {
            val url =
                baseUrlProvider.getBaseUrl()
                    .toHttpUrl()
                    .newBuilder()
                    .addPathSegments("api/v1/chat/completions")
                    .build()

            val httpRequest =
                Request.Builder()
                    .url(url)
                    .header("Accept", "text/event-stream")
                    .header("Content-Type", "application/json")
                    .post(json.encodeToString(request).toRequestBody("application/json".toMediaType()))
                    .build()

            okHttpClient.newCall(httpRequest).execute().use { response ->
                val contentType = response.header("Content-Type").orEmpty()
                if (!response.isSuccessful || !contentType.startsWith("text/event-stream")) {
                    throw NetworkException(parseNetworkError(response.code, response.message, response.body?.string()))
                }

                val source = requireNotNull(response.body) { "Missing response body" }.source()
                collector.collect(source) { emit(it) }
            }
        }

    private fun parseNetworkError(
        httpCode: Int,
        message: String,
        rawBody: String?,
    ): NetworkError {
        val body = rawBody.orEmpty()
        if (body.isNotBlank()) {
            try {
                return json.decodeFromString<ResponseEnvelope<JsonElement?>>(body).toNetworkError()
            } catch (_: SerializationException) {
                // Fall through to generic error mapping.
            }
        }

        return NetworkError(
            code = httpCode,
            statusText = "error",
            message = body.ifBlank { message.ifBlank { "Network request failed" } },
        )
    }
}
