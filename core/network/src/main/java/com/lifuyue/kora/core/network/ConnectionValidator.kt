package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ConnectionValidationError
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ConnectionValidator {
    fun normalizeServerBaseUrl(input: String): String = ConnectionConfig.normalizeBaseUrl(input)

    fun validateServerBaseUrl(input: String): ConnectionValidationError? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return ConnectionValidationError.EMPTY_SERVER_URL
        }

        if (trimmed.contains("://") &&
            !trimmed.startsWith("http://") &&
            !trimmed.startsWith("https://")
        ) {
            return ConnectionValidationError.INVALID_SERVER_URL
        }

        val withScheme =
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else if ("://" in trimmed) {
                return ConnectionValidationError.INVALID_SERVER_URL
            } else {
                "https://$trimmed"
            }

        val parsed = withScheme.toHttpUrlOrNull() ?: return ConnectionValidationError.INVALID_SERVER_URL
        return if (parsed.scheme == "http" || parsed.scheme == "https") {
            null
        } else {
            ConnectionValidationError.INVALID_SERVER_URL
        }
    }

    fun validateApiKey(
        connectionType: ConnectionType,
        apiKey: String,
    ): ConnectionValidationError? =
        when {
            apiKey.trim().isEmpty() -> ConnectionValidationError.EMPTY_API_KEY
            connectionType == ConnectionType.FAST_GPT && !ConnectionConfig.isValidApiKey(apiKey) ->
                ConnectionValidationError.INVALID_API_KEY
            else -> null
        }
}
