@file:Suppress("ktlint:standard:enum-entry-name-case")

package com.lifuyue.kora.core.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ResponseEnvelope<T>(
    val code: Int,
    val statusText: String = "",
    val message: String = "",
    val data: T? = null,
)

fun ResponseEnvelope<*>.toNetworkError(): NetworkError =
    NetworkError(
        code = code,
        statusText = statusText,
        message = message,
        data = data as? JsonElement,
    )

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    OLED_DARK,
}

data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledEnabled: Boolean = false,
    val streamEnabled: Boolean = true,
    val autoScroll: Boolean = true,
    val fontSizeScale: Float = 1f,
    val showCitationsByDefault: Boolean = true,
    val languageTag: String? = null,
)

data class ConnectionSnapshot(
    val serverBaseUrl: String? = null,
    val apiKey: String? = null,
    val selectedAppId: String? = null,
    val onboardingCompleted: Boolean = false,
    val appearancePreferences: AppearancePreferences = AppearancePreferences(),
) {
    val hasApiKey: Boolean
        get() = !apiKey.isNullOrBlank()

    val hasValidConnection: Boolean
        get() = !serverBaseUrl.isNullOrBlank() && hasApiKey
}

fun interface ConnectionSnapshotProvider {
    fun getSnapshot(): ConnectionSnapshot
}

sealed interface ConnectionTestResult {
    data class Success(
        val normalizedBaseUrl: String,
        val apps: List<ConnectionTestApp>,
        val latencyMs: Long,
    ) : ConnectionTestResult

    data class ValidationError(
        val reason: ConnectionValidationError,
    ) : ConnectionTestResult

    data class AuthError(
        val error: NetworkError,
        val latencyMs: Long,
    ) : ConnectionTestResult

    data class NetworkFailure(
        val message: String,
    ) : ConnectionTestResult

    data class ServerError(
        val error: NetworkError,
        val latencyMs: Long,
    ) : ConnectionTestResult
}

data class ConnectionTestApp(
    val id: String,
    val name: String,
)

enum class ConnectionValidationError {
    EMPTY_SERVER_URL,
    INVALID_SERVER_URL,
    EMPTY_API_KEY,
    INVALID_API_KEY,
    NO_AVAILABLE_APPS,
}

@Serializable
enum class ChatRole {
    System,
    Human,
    AI,
}

@Serializable
@Suppress("ktlint:standard:enum-entry-name-case")
enum class ChatFileType {
    image,
    file,
}

@Serializable
@Suppress("ktlint:standard:enum-entry-name-case")
enum class ChatSource {
    test,
    online,
    share,
    api,
    cronJob,
    team,
    feishu,
    official_account,
    wecom,
    wechat,
    mcp,
}

@Serializable
@Suppress("ktlint:standard:enum-entry-name-case")
enum class SseEvent {
    error,
    workflowDuration,
    answer,
    fastAnswer,
    flowNodeStatus,
    flowNodeResponse,
    toolCall,
    toolParams,
    toolResponse,
    flowResponses,
    updateVariables,
    interactive,
    plan,
    stepTitle,
    collectionForm,
    topAgentConfig,
}

fun sseEventFromWireName(name: String): SseEvent? = SseEvent.entries.firstOrNull { it.name == name }
