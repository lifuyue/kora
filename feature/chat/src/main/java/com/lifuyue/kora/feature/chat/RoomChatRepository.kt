package com.lifuyue.kora.feature.chat

import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.NetworkError
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.database.entity.MessageEntity
import com.lifuyue.kora.core.network.ChatCompletionMessageParam
import com.lifuyue.kora.core.network.ChatCompletionRequest
import com.lifuyue.kora.core.network.ChatHistoriesRequest
import com.lifuyue.kora.core.network.ChatHistoryItemDto
import com.lifuyue.kora.core.network.CitationDto
import com.lifuyue.kora.core.network.ChatRecordItemDto
import com.lifuyue.kora.core.network.DeleteChatItemRequest
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.NetworkException
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.PaginationRecordsRequest
import com.lifuyue.kora.core.network.QuestionGuideRequest
import com.lifuyue.kora.core.network.SseEventData
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.UpdateHistoryRequest
import com.lifuyue.kora.core.network.UpdateUserFeedbackRequest
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias RoomBackedChatRepository = RoomChatRepository

class RoomChatRepository
    constructor(
    private val api: FastGptApi,
    private val sseStreamClient: SseStreamClient,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val json: Json = NetworkJson.default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val responsePlanner: AssistantResponsePlanner? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ChatRepository, ConversationRepository {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val streamingJobs = linkedMapOf<String, Job>()
    private val restoringJobs = linkedMapOf<String, Job>()
    private val chatBootstrapByChatId = linkedMapOf<String, ChatBootstrap>()
    private var localId = 0L
    private var lastCreatedAt = 0L

    override suspend fun bootstrapChat(appId: String): ChatBootstrap {
        val data = api.initChat(appId = appId, chatId = null).data
        val resolvedChatId = data?.chatId?.takeIf { it.isNotBlank() } ?: nextId("chat")
        return ChatBootstrap(
            chatId = resolvedChatId,
            title = data?.title.orEmpty(),
            welcomeText = data?.welcomeText,
            questionGuide = data?.questionGuide,
        ).also { chatBootstrapByChatId[resolvedChatId] = it }
    }

    override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> =
        conversationDao.observeConversationsForApp(appId).map { rows -> rows.map(::conversationToUiModel) }

    override suspend fun refreshConversations(appId: String) {
        val histories = api.getHistories(ChatHistoriesRequest(appId = appId)).data?.list.orEmpty()
        val existing = conversationDao.getConversationsForAppIncludingDeleted(appId = appId, limit = Int.MAX_VALUE, offset = 0)
        val existingByChatId = existing.associateBy { it.chatId }
        val remoteChatIds = histories.map { it.chatId }

        if (histories.isEmpty()) {
            val existingChatIds = existing.map { it.chatId }
            conversationDao.clearByAppId(appId)
            existingChatIds.forEach(messageDao::deleteMessagesForChat)
            return
        }

        histories.forEach { item ->
            val cached = existingByChatId[item.chatId]
            conversationDao.upsert(
                item.toConversationEntity(
                    cached = cached,
                    updateTime = item.updateTime.toEpochMillis(),
                ),
            )
        }
        existing.map { it.chatId }.filterNot(remoteChatIds::contains).forEach(conversationDao::softDelete)
        existing.map { it.chatId }.filterNot(remoteChatIds::contains).forEach(messageDao::deleteMessagesForChat)
    }

    override suspend fun renameConversation(appId: String, chatId: String, title: String) {
        api.updateHistory(
            UpdateHistoryRequest(
                appId = appId,
                chatId = chatId,
                title = title,
                customTitle = title,
            ),
        )
        conversationDao.updateConversation(
            chatId = chatId,
            title = title,
            customTitle = title,
            isPinned = null,
            updateTime = clock(),
            lastMessagePreview = null,
        )
    }

    override suspend fun togglePinConversation(appId: String, chatId: String, pinned: Boolean) {
        api.updateHistory(
            UpdateHistoryRequest(
                appId = appId,
                chatId = chatId,
                top = pinned,
            ),
        )
        conversationDao.updateConversation(
            chatId = chatId,
            title = null,
            customTitle = conversationDao.getConversationByChatId(chatId)?.customTitle,
            isPinned = pinned,
            updateTime = clock(),
            lastMessagePreview = null,
        )
    }

    override suspend fun deleteConversation(appId: String, chatId: String) {
        streamingJobs.remove(chatId)?.cancel()
        api.deleteHistory(appId = appId, chatId = chatId)
        conversationDao.softDelete(chatId)
        messageDao.deleteMessagesForChat(chatId)
    }

    override suspend fun clearConversations(appId: String) {
        val chatIds = conversationDao.getConversationsForAppIncludingDeleted(appId = appId, limit = Int.MAX_VALUE, offset = 0).map { it.chatId }
        api.clearHistories(appId)
        conversationDao.clearByAppId(appId)
        chatIds.forEach(messageDao::deleteMessagesForChat)
    }

    override fun observeMessages(appId: String, chatId: String?): Flow<List<ChatMessageUiModel>> {
        if (chatId.isNullOrBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val needsRestore = messageDao.getMessagesForChat(chatId).isEmpty()
            if (needsRestore) {
                ensureMessagesRestored(appId = appId, chatId = chatId)
                emit(emptyList())
            }
            emitAll(messageDao.observeMessagesForChat(chatId).map { rows -> rows.map(::messageToUiModel) })
        }
    }

    override suspend fun restoreMessages(appId: String, chatId: String) {
        if (messageDao.getMessagesForChat(chatId).isNotEmpty()) {
            return
        }
        val response =
            api.getPaginationRecords(
                PaginationRecordsRequest(
                    appId = appId,
                    chatId = chatId,
                    loadCustomFeedbacks = true,
                ),
            )
        val records = response.data?.list.orEmpty()
        if (records.isEmpty()) {
            return
        }
        val entities =
            records.mapIndexed { index, record ->
                record.toMessageEntity(
                    appId = appId,
                    chatId = chatId,
                    createdAt = index.toLong(),
                    payload = StoredMessagePayload(markdown = extractMarkdown(record.value)),
                )
            }
        messageDao.upsertAll(entities)
        updateConversationPreview(chatId, entities.lastOrNull()?.let(::messageToUiModel)?.markdown.orEmpty())
    }

    override suspend fun sendMessage(appId: String, chatId: String?, text: String): String {
        val prompt = text.trim()
        require(prompt.isNotEmpty()) { "消息不能为空" }

        val initData =
            if (chatId == null) {
                api.initChat(appId = appId, chatId = null).data
            } else {
                null
            }
        val resolvedChatId =
            chatId
                ?: initData?.chatId?.takeIf { it.isNotBlank() }
                ?: nextId("chat")
        if (initData != null) {
            chatBootstrapByChatId[resolvedChatId] =
                ChatBootstrap(
                    chatId = resolvedChatId,
                    title = initData.title.orEmpty(),
                    welcomeText = initData.welcomeText,
                    questionGuide = initData.questionGuide,
                )
        }
        val createdAt = nextCreatedAt()
        val humanMessage = newMessageEntity(
            dataId = nextId("human"),
            chatId = resolvedChatId,
            appId = appId,
            role = ChatRole.Human,
            markdown = prompt,
            sendStatus = "sent",
            isStreaming = false,
            createdAt = createdAt,
        )
        val assistantMessage =
            newStreamingAssistantEntity(
                chatId = resolvedChatId,
                appId = appId,
                createdAt = createdAt + 1L,
            )

        upsertConversation(
            appId = appId,
            chatId = resolvedChatId,
            title =
                conversationDao.getConversationByChatId(resolvedChatId)?.displayTitle
                    ?: initData?.title?.takeIf { it.isNotBlank() }
                    ?: prompt.take(18).ifBlank { "新会话" },
            preview = prompt,
        )
        messageDao.upsertAll(listOf(humanMessage, assistantMessage))

        startStreaming(
            appId = appId,
            chatId = resolvedChatId,
            prompt = prompt,
            assistantMessageId = assistantMessage.dataId,
        )
        return resolvedChatId
    }

    override suspend fun stopStreaming(appId: String, chatId: String) {
        streamingJobs.remove(chatId)?.cancel()
        val current = messageDao.getMessagesForChat(chatId).lastOrNull { it.role == ChatRole.AI.name && it.isStreaming } ?: return
        messageDao.updateMessageState(
            dataId = current.dataId,
            payloadJson = updateStoredPayload(current.payloadJson, errorMessage = "已停止生成", json = json),
            isStreaming = false,
            sendStatus = "stopped",
            errorCode = current.errorCode,
        )
        updateConversationPreview(chatId, messageToUiModel(current.copy(payloadJson = updateStoredPayload(current.payloadJson, errorMessage = "已停止生成", json = json))).markdown)
    }

    override suspend fun continueGeneration(appId: String, chatId: String): String {
        val prompt =
            messageDao.getMessagesForChat(chatId)
                .lastOrNull { it.role == ChatRole.Human.name }
                ?.let(::messageToUiModel)
                ?.markdown
                .orEmpty()
                .ifBlank { "继续上一个回答" }
        val assistantMessage =
            newStreamingAssistantEntity(
                chatId = chatId,
                appId = appId,
                createdAt = nextCreatedAt(),
            )
        messageDao.upsert(assistantMessage)
        updateConversationPreview(chatId, "继续生成中")
        startStreaming(appId = appId, chatId = chatId, prompt = prompt, assistantMessageId = assistantMessage.dataId)
        return chatId
    }

    override suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String {
        val messages = messageDao.getMessagesForChat(chatId)
        val targetIndex = messages.indexOfFirst { it.dataId == messageId }.takeIf { it >= 0 } ?: messages.lastIndex
        val prompt =
            messages
                .take(targetIndex + 1)
                .lastOrNull { it.role == ChatRole.Human.name }
                ?.let(::messageToUiModel)
                ?.markdown
                .orEmpty()
                .ifBlank { "继续上一个回答" }

        api.deleteChatItem(DeleteChatItemRequest(appId = appId, chatId = chatId, contentId = messageId))
        messageDao.deleteMessage(messageId)

        val assistantMessage =
            newStreamingAssistantEntity(
                chatId = chatId,
                appId = appId,
                createdAt = nextCreatedAt(),
            )
        messageDao.upsert(assistantMessage)
        updateConversationPreview(chatId, "重新生成中")
        startStreaming(appId = appId, chatId = chatId, prompt = prompt, assistantMessageId = assistantMessage.dataId)
        return chatId
    }

    override suspend fun setFeedback(appId: String, chatId: String, messageId: String, feedback: MessageFeedback) {
        messageDao.updateFeedback(messageId, feedback.toFeedbackType())
        api.updateUserFeedback(
            UpdateUserFeedbackRequest(
                appId = appId,
                chatId = chatId,
                dataId = messageId,
                userGoodFeedback = if (feedback == MessageFeedback.Upvote) "upvote" else null,
                userBadFeedback = if (feedback == MessageFeedback.Downvote) "downvote" else null,
            ),
        )
    }

    private fun ensureMessagesRestored(appId: String, chatId: String) {
        if (messageDao.getMessagesForChat(chatId).isNotEmpty()) {
            return
        }
        if (restoringJobs.containsKey(chatId)) {
            return
        }
        restoringJobs[chatId] =
            scope.launch {
                try {
                    restoreMessages(appId = appId, chatId = chatId)
                } finally {
                    restoringJobs.remove(chatId)
                }
            }
    }

    private fun startStreaming(
        appId: String,
        chatId: String,
        prompt: String,
        assistantMessageId: String,
    ) {
        streamingJobs.remove(chatId)?.cancel()
        streamingJobs[chatId] =
            scope.launch {
                var markdown = ""
                var reasoning = ""
                var eventPayloads = emptyList<String>()
                var failed = false
                try {
                    if (responsePlanner != null) {
                        val plannedSteps =
                            responsePlanner.plan(
                                AssistantResponseRequest(
                                    appId = appId,
                                    chatId = chatId,
                                    prompt = prompt,
                                    attempt = 1,
                                ),
                            )
                        for (step in plannedSteps) {
                            if (step.delayMillis > 0) {
                                delay(step.delayMillis)
                            }
                            markdown += step.markdownDelta
                            reasoning += step.reasoningDelta
                            persistAssistantState(
                                dataId = assistantMessageId,
                                markdown = markdown,
                                reasoning = reasoning,
                                eventPayloads = eventPayloads,
                                isStreaming = step.terminalError == null,
                                sendStatus = if (step.terminalError == null) "streaming" else "failed",
                                errorCode = null,
                                errorMessage = step.terminalError,
                            )
                            if (step.terminalError != null) {
                                failed = true
                                updateConversationPreview(chatId, markdown.ifBlank { step.terminalError })
                                break
                            }
                        }
                    } else {
                        sseStreamClient
                            .streamChatCompletions(
                                ChatCompletionRequest(
                                    chatId = chatId,
                                    appId = appId,
                                    responseChatItemId = assistantMessageId,
                                    messages =
                                        listOf(
                                            ChatCompletionMessageParam(
                                                role = "user",
                                                content = JsonPrimitive(prompt),
                                            ),
                                        ),
                                ),
                            ).collect { event ->
                                val update = event.toAssistantUpdate()
                                if (update.error != null) {
                                    failed = true
                                    persistAssistantState(
                                        dataId = assistantMessageId,
                                        markdown = markdown,
                                        reasoning = reasoning,
                                        eventPayloads = eventPayloads,
                                        isStreaming = false,
                                        sendStatus = "failed",
                                        errorCode = update.error.code,
                                        errorMessage = update.error.message,
                                    )
                                    updateConversationPreview(chatId, markdown.ifBlank { update.error.message })
                                } else if (
                                    update.markdownDelta.isNotEmpty() ||
                                    update.reasoningDelta.isNotEmpty() ||
                                    update.eventPayloads.isNotEmpty()
                                ) {
                                    markdown += update.markdownDelta
                                    reasoning += update.reasoningDelta
                                    eventPayloads = eventPayloads + update.eventPayloads
                                    persistAssistantState(
                                        dataId = assistantMessageId,
                                        markdown = markdown,
                                        reasoning = reasoning,
                                        eventPayloads = eventPayloads,
                                        isStreaming = true,
                                        sendStatus = "streaming",
                                        errorCode = null,
                                        errorMessage = null,
                                    )
                                }
                            }
                    }

                    if (!failed) {
                        persistAssistantState(
                            dataId = assistantMessageId,
                            markdown = markdown,
                            reasoning = reasoning,
                            eventPayloads = eventPayloads,
                            isStreaming = false,
                            sendStatus = "sent",
                            errorCode = null,
                            errorMessage = null,
                        )
                        updateConversationPreview(chatId, markdown.ifBlank { prompt })
                        attachAssistantEnhancements(
                            appId = appId,
                            chatId = chatId,
                            assistantMessageId = assistantMessageId,
                        )
                    }
                } catch (_: CancellationException) {
                    return@launch
                } catch (error: Throwable) {
                    val networkError = (error as? NetworkException)?.networkError
                    persistAssistantState(
                        dataId = assistantMessageId,
                        markdown = markdown,
                        reasoning = reasoning,
                        eventPayloads = eventPayloads,
                        isStreaming = false,
                        sendStatus = "failed",
                        errorCode = networkError?.code,
                        errorMessage = networkError?.message ?: error.message ?: "流式请求失败",
                    )
                    updateConversationPreview(chatId, markdown.ifBlank { error.message ?: "流式请求失败" })
                } finally {
                    streamingJobs.remove(chatId)
                }
            }
    }

    private fun persistAssistantState(
        dataId: String,
        markdown: String,
        reasoning: String,
        eventPayloads: List<String>,
        isStreaming: Boolean,
        sendStatus: String,
        errorCode: Int?,
        errorMessage: String?,
    ) {
        messageDao.updateMessageState(
            dataId = dataId,
            payloadJson =
                StoredMessagePayload(
                    markdown = markdown,
                    reasoning = reasoning,
                    eventPayloads = eventPayloads,
                    errorMessage = errorMessage,
                ).toJsonString(),
            isStreaming = isStreaming,
            sendStatus = sendStatus,
            errorCode = errorCode,
        )
    }

    private fun upsertConversation(
        appId: String,
        chatId: String,
        title: String,
        preview: String,
    ) {
        val current = conversationDao.getConversationByChatId(chatId)
        conversationDao.upsert(
            ConversationEntity(
                chatId = chatId,
                appId = appId,
                title = current?.title ?: title,
                customTitle = current?.customTitle,
                isPinned = current?.isPinned ?: false,
                source = current?.source ?: "local",
                updateTime = clock(),
                lastMessagePreview = preview.take(140),
                hasDraft = current?.hasDraft ?: false,
                isDeleted = false,
                isArchived = current?.isArchived ?: false,
            ),
        )
    }

    private fun updateConversationPreview(chatId: String, preview: String) {
        conversationDao.updateConversation(
            chatId = chatId,
            title = null,
            customTitle = conversationDao.getConversationByChatId(chatId)?.customTitle,
            isPinned = null,
            updateTime = clock(),
            lastMessagePreview = preview.take(140),
        )
    }

    private fun newMessageEntity(
        dataId: String,
        chatId: String,
        appId: String,
        role: ChatRole,
        markdown: String,
        sendStatus: String,
        isStreaming: Boolean,
        createdAt: Long = nextCreatedAt(),
        errorCode: Int? = null,
        reasoning: String = "",
        errorMessage: String? = null,
    ): MessageEntity =
        MessageEntity(
            dataId = dataId,
            chatId = chatId,
            appId = appId,
            role = role.name,
            payloadJson =
                StoredMessagePayload(
                    markdown = markdown,
                    reasoning = reasoning,
                    errorMessage = errorMessage,
                ).toJsonString(),
            createdAt = createdAt,
            isStreaming = isStreaming,
            sendStatus = sendStatus,
            errorCode = errorCode,
        )

    private fun newStreamingAssistantEntity(
        chatId: String,
        appId: String,
        createdAt: Long = nextCreatedAt(),
    ): MessageEntity =
        newMessageEntity(
            dataId = nextId("assistant"),
            chatId = chatId,
            appId = appId,
            role = ChatRole.AI,
            markdown = "",
            sendStatus = "streaming",
            isStreaming = true,
            createdAt = createdAt,
        )

    private fun nextId(prefix: String): String {
        localId += 1
        return "$prefix-$localId"
    }

    private fun nextCreatedAt(): Long {
        val now = clock()
        lastCreatedAt = maxOf(now, lastCreatedAt + 1L)
        return lastCreatedAt
    }

    private fun conversationToUiModel(entity: ConversationEntity): ConversationListItemUiModel =
        ConversationListItemUiModel(
            chatId = entity.chatId,
            appId = entity.appId,
            title = entity.displayTitle,
            preview = entity.lastMessagePreview.orEmpty(),
            isPinned = entity.isPinned,
            updateTime = entity.updateTime,
        )

    private fun messageToUiModel(entity: MessageEntity): ChatMessageUiModel {
        val storedPayload = entity.payloadJson.toStoredPayload(json)
        return ChatMessageUiModel(
            messageId = entity.dataId,
            chatId = entity.chatId,
            appId = entity.appId,
            role = entity.role.toChatRole(),
            markdown = storedPayload.markdown,
            reasoning = storedPayload.reasoning,
            isStreaming = entity.isStreaming,
            deliveryState = entity.sendStatus.toDeliveryState(entity.isStreaming),
            errorMessage = storedPayload.errorMessage,
            feedback = entity.feedbackType.toFeedback(),
            citations = storedPayload.citations.map(CitationPayload::toUiModel),
            suggestedQuestions = storedPayload.suggestedQuestions,
        )
    }

    private suspend fun attachAssistantEnhancements(
        appId: String,
        chatId: String,
        assistantMessageId: String,
    ) {
        val current = messageDao.getMessagesForChat(chatId).firstOrNull { it.dataId == assistantMessageId } ?: return
        val currentPayload = current.payloadJson.toStoredPayload(json)
        val shouldLoadCitations =
            currentPayload.eventPayloads.any { payload ->
                payload.contains("quoteList", ignoreCase = true) ||
                    payload.contains("datasetId", ignoreCase = true) ||
                    payload.contains("collectionId", ignoreCase = true)
            }
        val citations =
            runCatching {
                if (shouldLoadCitations) {
                    (api.getResData(appId = appId, dataId = assistantMessageId, chatId = chatId).data ?: JsonArray(emptyList())).extractCitations()
                } else {
                    emptyList()
                }
            }.getOrElse { emptyList() }
        val suggestedQuestions =
            runCatching {
                val config = chatBootstrapByChatId[chatId]?.questionGuide
                if (config?.open == true) {
                    api.createQuestionGuide(
                        QuestionGuideRequest(
                            appId = appId,
                            chatId = chatId,
                            questionGuide = config,
                        ),
                    ).data.orEmpty()
                } else {
                    emptyList()
                }
            }.getOrElse { emptyList() }
        messageDao.updateMessageState(
            dataId = assistantMessageId,
            payloadJson =
                currentPayload
                    .copy(
                        citations = citations.map(CitationDto::toPayload),
                        suggestedQuestions = suggestedQuestions,
                    ).toJsonString(),
            isStreaming = current.isStreaming,
            sendStatus = current.sendStatus,
            errorCode = current.errorCode,
        )
    }
}

private data class AssistantUpdate(
    val markdownDelta: String = "",
    val reasoningDelta: String = "",
    val eventPayloads: List<String> = emptyList(),
    val error: NetworkError? = null,
)

private data class StoredMessagePayload(
    val markdown: String = "",
    val reasoning: String = "",
    val eventPayloads: List<String> = emptyList(),
    val errorMessage: String? = null,
    val citations: List<CitationPayload> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
)

private data class CitationPayload(
    val datasetId: String? = null,
    val collectionId: String? = null,
    val dataId: String? = null,
    val title: String = "",
    val snippet: String = "",
    val scoreLabel: String = "",
)

private fun ChatHistoryItemDto.toConversationEntity(
    cached: ConversationEntity?,
    updateTime: Long,
): ConversationEntity =
    ConversationEntity(
        chatId = chatId,
        appId = appId,
        title = title,
        customTitle = customTitle,
        folderName = cached?.folderName,
        tags = cached?.tags.orEmpty(),
        isPinned = top == true,
        source = cached?.source ?: "online",
        updateTime = updateTime,
        lastMessagePreview = cached?.lastMessagePreview,
        hasDraft = cached?.hasDraft ?: false,
        isDeleted = false,
        isArchived = cached?.isArchived ?: false,
    )

private fun ChatRecordItemDto.toMessageEntity(
    appId: String,
    chatId: String,
    createdAt: Long,
    payload: StoredMessagePayload,
): MessageEntity =
    MessageEntity(
        dataId = dataId ?: "remote-$createdAt",
        chatId = chatId,
        appId = appId,
        role = obj ?: ChatRole.Human.name,
        payloadJson = payload.toJsonString(),
        createdAt = createdAt,
        isStreaming = false,
        sendStatus = "synced",
        errorCode = null,
    )

private fun ConversationEntity.displayTitle(): String = customTitle?.takeIf { it.isNotBlank() } ?: title

private val ConversationEntity.displayTitle: String
    get() = displayTitle()

private fun String.toChatRole(): ChatRole =
    ChatRole.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: ChatRole.Human

private fun String.toDeliveryState(isStreaming: Boolean): MessageDeliveryState =
    when {
        isStreaming || equals("streaming", ignoreCase = true) -> MessageDeliveryState.Streaming
        equals("failed", ignoreCase = true) -> MessageDeliveryState.Failed
        equals("stopped", ignoreCase = true) -> MessageDeliveryState.Stopped
        else -> MessageDeliveryState.Sent
    }

private fun Int?.toFeedback(): MessageFeedback =
    when (this) {
        1 -> MessageFeedback.Upvote
        -1 -> MessageFeedback.Downvote
        else -> MessageFeedback.None
    }

private fun MessageFeedback.toFeedbackType(): Int? =
    when (this) {
        MessageFeedback.None -> null
        MessageFeedback.Upvote -> 1
        MessageFeedback.Downvote -> -1
    }

private fun String.toStoredPayload(json: Json): StoredMessagePayload =
    runCatching {
        val payload = json.parseToJsonElement(this) as? JsonObject
        if (payload == null) {
            val fallback = extractMarkdown(json.parseToJsonElement(this))
            StoredMessagePayload(markdown = fallback.ifBlank { this })
        } else {
            StoredMessagePayload(
                markdown = payload["markdown"].stringContentOrNull().orEmpty(),
                reasoning = payload["reasoning"].stringContentOrNull().orEmpty(),
                eventPayloads =
                    payload["eventPayloads"]
                        ?.jsonArray
                        ?.mapNotNull { it.stringContentOrNull() }
                        .orEmpty(),
                errorMessage = payload["errorMessage"].stringContentOrNull(),
                citations =
                    payload["citations"]
                        ?.jsonArray
                        ?.mapNotNull { element ->
                            (element as? JsonObject)?.let { citation ->
                                CitationPayload(
                                    datasetId = citation["datasetId"].stringContentOrNull(),
                                    collectionId = citation["collectionId"].stringContentOrNull(),
                                    dataId = citation["dataId"].stringContentOrNull(),
                                    title = citation["title"].stringContentOrNull().orEmpty(),
                                    snippet = citation["snippet"].stringContentOrNull().orEmpty(),
                                    scoreLabel = citation["scoreLabel"].stringContentOrNull().orEmpty(),
                                )
                            }
                        }.orEmpty(),
                suggestedQuestions =
                    payload["suggestedQuestions"]
                        ?.jsonArray
                        ?.mapNotNull { it.stringContentOrNull() }
                        .orEmpty(),
            )
        }
    }.getOrElse {
        val fallback = runCatching { json.parseToJsonElement(this) }.getOrNull()?.let(::extractMarkdown).orEmpty()
        StoredMessagePayload(markdown = fallback.ifBlank { this })
    }

private fun updateStoredPayload(
    payloadJson: String,
    errorMessage: String?,
    json: Json,
): String {
    val current = payloadJson.toStoredPayload(json)
    return current.copy(errorMessage = errorMessage).toJsonString()
}

private fun String.toEpochMillis(): Long =
    runCatching { Instant.parse(this).toEpochMilli() }.getOrElse { 0L }

private fun SseEventData.toAssistantUpdate(): AssistantUpdate {
    if (isMalformed) {
        return AssistantUpdate(error = NetworkError(code = -1, statusText = "malformed", message = parseExceptionMessage ?: "Malformed SSE payload"))
    }
    if (rawEventName == "error") {
        val objectPayload = payload as? JsonObject
        return AssistantUpdate(
            error =
                NetworkError(
                    code = objectPayload?.get("code")?.jsonPrimitive?.intOrNull ?: -1,
                    statusText = objectPayload?.get("statusText").stringContentOrNull() ?: "error",
                    message = objectPayload?.get("message").stringContentOrNull() ?: rawData,
                ),
        )
    }

    val delta = payload?.deltaContent().orEmpty()
    val reasoning = payload?.deltaReasoning().orEmpty()
    val eventPayloads =
        if (
            rawEventName == "plan" ||
            rawEventName == "interactive" ||
            rawEventName == "flowResponses" ||
            rawEventName == "updateVariables" ||
            rawEventName?.startsWith("tool") == true
        ) {
            listOf(rawData)
        } else {
            emptyList()
        }
    return AssistantUpdate(markdownDelta = delta, reasoningDelta = reasoning, eventPayloads = eventPayloads)
}

private fun JsonElement.deltaContent(): String =
    jsonObject["choices"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("delta")
        ?.jsonObject
        ?.get("content")
        .stringContentOrNull()
        .orEmpty()

private fun JsonElement.deltaReasoning(): String =
    jsonObject["choices"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("delta")
        ?.jsonObject
        ?.get("reasoning_content")
        .stringContentOrNull()
        .orEmpty()

private fun extractMarkdown(value: JsonElement?): String {
    if (value == null) {
        return ""
    }
    return when (value) {
        is JsonPrimitive -> value.stringContentOrNull() ?: value.booleanOrNull?.toString().orEmpty()
        is JsonArray -> value.joinToString(separator = "") { extractMarkdown(it) }
        is JsonObject -> {
            value["content"].stringContentOrNull()
                ?: value.values.joinToString(separator = "") { child -> extractMarkdown(child) }
        }
        else -> ""
    }
}

private fun JsonElement?.stringContentOrNull(): String? =
    (this as? JsonPrimitive)?.content

private fun StoredMessagePayload.toJsonString(): String =
    JsonObject(
        buildMap {
            put("markdown", JsonPrimitive(markdown))
            put("reasoning", JsonPrimitive(reasoning))
            put("eventPayloads", JsonArray(eventPayloads.map(::JsonPrimitive)))
            put(
                "citations",
                JsonArray(
                    citations.map {
                        JsonObject(
                            buildMap {
                                it.datasetId?.let { datasetId -> put("datasetId", JsonPrimitive(datasetId)) }
                                it.collectionId?.let { collectionId -> put("collectionId", JsonPrimitive(collectionId)) }
                                it.dataId?.let { dataId -> put("dataId", JsonPrimitive(dataId)) }
                                put("title", JsonPrimitive(it.title))
                                put("snippet", JsonPrimitive(it.snippet))
                                put("scoreLabel", JsonPrimitive(it.scoreLabel))
                            },
                        )
                    },
                ),
            )
            put("suggestedQuestions", JsonArray(suggestedQuestions.map(::JsonPrimitive)))
            errorMessage?.let { put("errorMessage", JsonPrimitive(it)) }
        },
    ).toString()

private fun CitationPayload.toUiModel(): CitationItemUiModel =
    CitationItemUiModel(
        datasetId = datasetId,
        collectionId = collectionId,
        dataId = dataId,
        title = title,
        snippet = snippet,
        scoreLabel = scoreLabel,
    )

private fun CitationDto.toPayload(): CitationPayload =
    CitationPayload(
        datasetId = datasetId,
        collectionId = collectionId,
        dataId = dataId,
        title = title.ifBlank { sourceName.ifBlank { "引用" } },
        snippet = snippet,
        scoreLabel =
            listOfNotNull(
                scoreType,
                score?.let { String.format("%.3f", it) },
            ).joinToString(" · "),
    )

private fun JsonArray.extractCitations(): List<CitationDto> {
    val result = linkedMapOf<String, CitationDto>()
    forEach { element ->
        element.collectCitationCandidates().forEach { citation ->
            val key = listOf(citation.datasetId, citation.collectionId, citation.dataId, citation.title, citation.snippet).joinToString("|")
            if (citation.title.isNotBlank() || citation.snippet.isNotBlank()) {
                result[key] = citation
            }
        }
    }
    return result.values.toList()
}

private fun JsonElement.collectCitationCandidates(): List<CitationDto> =
    when (this) {
        is JsonArray -> flatMap { it.collectCitationCandidates() }
        is JsonObject -> {
            val direct =
                if (
                    containsKey("quoteList") ||
                    containsKey("sourceName") ||
                    containsKey("datasetId") ||
                    containsKey("collectionId")
                ) {
                    listOf(
                        CitationDto(
                            datasetId = this["datasetId"].stringContentOrNull(),
                            collectionId = this["collectionId"].stringContentOrNull(),
                            dataId = this["dataId"].stringContentOrNull(),
                            title =
                                this["title"].stringContentOrNull()
                                    ?: this["sourceName"].stringContentOrNull()
                                    ?: this["name"].stringContentOrNull()
                                    ?: "",
                            sourceName = this["sourceName"].stringContentOrNull().orEmpty(),
                            snippet =
                                this["content"].stringContentOrNull()
                                    ?: this["q"].stringContentOrNull()
                                    ?: this["quote"].stringContentOrNull()
                                    ?: this["a"].stringContentOrNull()
                                    ?: "",
                            score = this["score"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                            scoreType = this["scoreType"].stringContentOrNull(),
                        ),
                    )
                } else {
                    emptyList()
                }
            direct + values.flatMap { it.collectCitationCandidates() }
        }
        else -> emptyList()
    }
