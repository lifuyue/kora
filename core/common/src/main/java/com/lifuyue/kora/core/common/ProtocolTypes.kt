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
