package com.lifuyue.kora.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatCompletionMessageParam(
    val role: String,
    val content: JsonElement? = null,
    val reasoning_content: String? = null,
    val dataId: String? = null,
    val hideInUI: Boolean? = null,
    val name: String? = null,
    val interactive: JsonObject? = null,
)

@Serializable
data class ChatCompletionRequest(
    val chatId: String? = null,
    val appId: String? = null,
    val customUid: String? = null,
    val shareId: String? = null,
    val outLinkUid: String? = null,
    val teamId: String? = null,
    val teamToken: String? = null,
    val messages: List<ChatCompletionMessageParam>,
    val responseChatItemId: String? = null,
    val stream: Boolean = true,
    val detail: Boolean = true,
    val retainDatasetCite: Boolean? = null,
    val variables: JsonObject = JsonObject(emptyMap()),
    val metadata: JsonObject? = null,
)

@Serializable
data class ChatHistoriesRequest(
    val offset: Int? = null,
    val pageSize: Int? = null,
    val appId: String? = null,
    val source: String? = null,
)

@Serializable
data class PaginationRecordsRequest(
    val appId: String,
    val chatId: String,
    val offset: Int? = null,
    val pageSize: Int? = null,
    val loadCustomFeedbacks: Boolean? = null,
    val type: String? = null,
)

@Serializable
data class UpdateHistoryRequest(
    val appId: String,
    val chatId: String,
    val title: String? = null,
    val customTitle: String? = null,
    val top: Boolean? = null,
)

@Serializable
data class DeleteChatItemRequest(
    val appId: String,
    val chatId: String,
    val contentId: String,
)

@Serializable
data class UpdateUserFeedbackRequest(
    val appId: String,
    val chatId: String,
    val dataId: String,
    val userGoodFeedback: String? = null,
    val userBadFeedback: String? = null,
)

@Serializable
data class AppListItemDto(
    @SerialName("_id")
    val id: String,
    val parentId: String? = null,
    val tmbId: String = "",
    val name: String = "",
    val avatar: String = "",
    val intro: String = "",
    val type: String = "",
    val updateTime: String = "",
    val permission: JsonObject = JsonObject(emptyMap()),
    val sourceMember: JsonObject = JsonObject(emptyMap()),
    val hasInteractiveNode: Boolean? = null,
)

@Serializable
data class ChatInitData(
    val chatId: String? = null,
    val appId: String,
    val title: String? = null,
    val userAvatar: String? = null,
    val variables: JsonObject = JsonObject(emptyMap()),
    val app: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class ChatHistoryItemDto(
    val chatId: String,
    val updateTime: String,
    val appId: String,
    val customTitle: String? = null,
    val title: String,
    val top: Boolean? = null,
)

@Serializable
data class ChatHistoriesResponseData(
    val list: List<ChatHistoryItemDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ChatRecordItemDto(
    val dataId: String? = null,
    val obj: String? = null,
    val value: JsonElement? = null,
)

@Serializable
data class PaginationRecordsResponseData(
    val list: List<ChatRecordItemDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ChatResponseDataItemDto(
    val id: String? = null,
    val name: String? = null,
    val value: JsonElement? = null,
)

@Serializable
data class ChatCompletionAssistantMessageDto(
    val role: String,
    val content: String = "",
    val reasoning_content: String? = null,
)

@Serializable
data class ChatCompletionChoiceDto(
    val index: Int,
    val finish_reason: String? = null,
    val message: ChatCompletionAssistantMessageDto,
)

@Serializable
data class ChatCompletionResponseDto(
    val id: String? = null,
    val choices: List<ChatCompletionChoiceDto> = emptyList(),
)
