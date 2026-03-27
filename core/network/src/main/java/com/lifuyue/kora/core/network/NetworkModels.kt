package com.lifuyue.kora.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
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
data class AppQuestionGuideConfigDto(
    val open: Boolean = false,
    val model: String? = null,
    val customPrompt: String? = null,
)

@Serializable
data class AppBootstrapDto(
    val welcomeText: String? = null,
    val variables: JsonObject = JsonObject(emptyMap()),
    val chatModels: JsonArray = JsonArray(emptyList()),
    val fileSelectConfig: JsonObject? = null,
    val ttsConfig: JsonObject? = null,
    val whisperConfig: JsonObject? = null,
    val questionGuide: AppQuestionGuideConfigDto? = null,
)

@Serializable
data class ChatInitData(
    val chatId: String? = null,
    val appId: String,
    val title: String? = null,
    val userAvatar: String? = null,
    val variables: JsonObject = JsonObject(emptyMap()),
    val app: JsonObject = JsonObject(emptyMap()),
    val welcomeText: String? = null,
    val chatModels: JsonArray = JsonArray(emptyList()),
    val fileSelectConfig: JsonObject? = null,
    val ttsConfig: JsonObject? = null,
    val whisperConfig: JsonObject? = null,
    val questionGuide: AppQuestionGuideConfigDto? = null,
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
data class CitationDto(
    val datasetId: String? = null,
    val collectionId: String? = null,
    val dataId: String? = null,
    val title: String = "",
    val sourceName: String = "",
    val snippet: String = "",
    val score: Double? = null,
    val scoreType: String? = null,
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

@Serializable
data class DatasetListRequest(
    val parentId: String? = null,
    val searchKey: String? = null,
)

@Serializable
data class DatasetSummaryDto(
    @SerialName("_id")
    val id: String,
    val name: String = "",
    val intro: String = "",
    val type: String = "",
    val avatar: String = "",
    val vectorModel: String = "",
    val updateTime: String = "",
    val permission: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class DatasetCreateRequest(
    val parentId: String? = null,
    val name: String,
    val intro: String = "",
    val type: String = "dataset",
)

@Serializable
data class DatasetDeleteRequest(
    val id: String,
)

@Serializable
data class CollectionListRequest(
    val datasetId: String,
    val parentId: String? = null,
)

@Serializable
data class CollectionSummaryDto(
    @SerialName("_id")
    val id: String,
    val datasetId: String,
    val name: String = "",
    val type: String = "",
    val updateTime: String = "",
    val trainingType: String = "",
    val status: String? = null,
    val trainingStatus: String? = null,
    val rawLink: String? = null,
    val fileId: String? = null,
    val rawTextLength: Int? = null,
    val forbid: Boolean? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class TextCollectionCreateRequest(
    val datasetId: String,
    val parentId: String? = null,
    val trainingType: String = "chunk",
    val text: String,
    val name: String,
)

@Serializable
data class LinkCollectionCreateItemRequest(
    val url: String,
    val selector: String? = null,
)

@Serializable
data class LinkCollectionCreateRequest(
    val datasetId: String,
    val parentId: String? = null,
    val trainingType: String = "chunk",
    val links: List<LinkCollectionCreateItemRequest>,
)

@Serializable
data class ChunkListRequest(
    val datasetId: String,
    val collectionId: String,
    val offset: Int = 0,
    val pageSize: Int = 20,
)

@Serializable
data class ChunkSummaryDto(
    @SerialName("_id")
    val id: String,
    val datasetId: String,
    val collectionId: String,
    val chunkIndex: Int = 0,
    val q: String = "",
    val a: String? = null,
    val updateTime: String = "",
    val forbid: Boolean? = null,
    val rebuilding: Boolean? = null,
    val indexes: JsonArray = JsonArray(emptyList()),
)

@Serializable
data class ChunkListResponseDto(
    val list: List<ChunkSummaryDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ChunkUpdateRequest(
    val id: String,
    val q: String,
    val a: String? = null,
    val forbid: Boolean? = null,
)

@Serializable
data class ChunkDeleteRequest(
    val id: String,
)

@Serializable
data class SearchTestRequest(
    val datasetId: String,
    val text: String,
    val limit: Int? = null,
    val similarity: Double? = null,
    val searchMode: String? = null,
    val embeddingWeight: Double? = null,
    val usingReRank: Boolean? = null,
    val rerankModel: String? = null,
    val rerankWeight: Double? = null,
    val datasetSearchUsingExtensionQuery: Boolean? = null,
)

@Serializable
data class SearchTestResultItemDto(
    val datasetId: String? = null,
    val collectionId: String? = null,
    val dataId: String? = null,
    val q: String? = null,
    val a: String? = null,
    val sourceName: String? = null,
    val score: Double? = null,
    val scoreType: String? = null,
)

@Serializable
data class SearchTestResponseDto(
    val list: List<SearchTestResultItemDto> = emptyList(),
    val duration: String = "",
    val queryExtensionModel: String? = null,
    val usingReRank: Boolean = false,
)

@Serializable
data class QuestionGuideRequest(
    val appId: String,
    val chatId: String,
    val questionGuide: AppQuestionGuideConfigDto? = null,
)
