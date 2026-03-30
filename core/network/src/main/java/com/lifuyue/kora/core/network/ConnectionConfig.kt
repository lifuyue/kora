package com.lifuyue.kora.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ConnectionConfig {
    fun normalizeBaseUrl(input: String): String {
        return normalizeUrl(input) { segments ->
            if (segments.lastOrNull() == "api") {
                segments.removeLast()
            }
        }
    }

    fun normalizeOpenAiCompatibleBaseUrl(input: String): String {
        return normalizeUrl(input) { segments ->
            if (segments.lastOrNull() == "v1") {
                segments.removeLast()
            }
        }
    }

    private fun normalizeUrl(
        input: String,
        trimTrailingSegments: (MutableList<String>) -> Unit,
    ): String {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Server URL is required" }
        require(!trimmed.contains("://") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "Server URL is invalid"
        }

        val withScheme =
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }

        val parsed = requireNotNull(withScheme.toHttpUrlOrNull()) { "Server URL is invalid" }
        val rawSegments = parsed.pathSegments.filter { it.isNotBlank() }.toMutableList()
        trimTrailingSegments(rawSegments)

        val normalizedPath =
            if (rawSegments.isEmpty()) {
                "/"
            } else {
                "/${rawSegments.joinToString("/")}/"
            }

        return parsed.newBuilder()
            .encodedPath(normalizedPath)
            .query(null)
            .fragment(null)
            .build()
            .toString()
    }

    fun isValidApiKey(apiKey: String): Boolean = apiKey.trim().startsWith("fastgpt-")

    fun redactApiKey(apiKey: String?): String =
        apiKey
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                when {
                    value.length <= 8 -> "${value.take(4)}***"
                    else -> "${value.take(8)}***${value.takeLast(4)}"
                }
            } ?: "<empty>"
}
