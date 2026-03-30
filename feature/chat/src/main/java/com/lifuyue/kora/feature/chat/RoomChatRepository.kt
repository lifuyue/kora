package com.lifuyue.kora.feature.chat

import android.content.Context
import android.net.Uri
import com.lifuyue.kora.feature.chat.buildChatCompletionContent
import com.lifuyue.kora.feature.chat.chatString
import com.lifuyue.kora.feature.chat.resolveAttachmentMetadata
import com.lifuyue.kora.feature.chat.toChatAttachmentConfig
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionSnapshotProvider
import com.lifuyue.kora.core.common.DIRECT_OPENAI_APP_ID
import com.lifuyue.kora.core.common.NetworkError
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.ConversationFolderAssignmentRow
import com.lifuyue.kora.core.database.dao.ConversationFolderDao
import com.lifuyue.kora.core.database.dao.ConversationTagAssignmentRow
import com.lifuyue.kora.core.database.dao.ConversationTagDao
import com.lifuyue.kora.core.database.dao.InteractiveDraftDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.database.LocalKnowledgeHit
import com.lifuyue.kora.core.database.LocalKnowledgeStore
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.database.entity.ConversationFolderCrossRef
import com.lifuyue.kora.core.database.entity.ConversationFolderEntity
import com.lifuyue.kora.core.database.entity.ConversationTagCrossRef
import com.lifuyue.kora.core.database.entity.ConversationTagEntity
import com.lifuyue.kora.core.database.entity.InteractiveDraftEntity
import com.lifuyue.kora.core.database.entity.MessageEntity
import com.lifuyue.kora.core.network.ChatCompletionMessageParam
import com.lifuyue.kora.core.network.ChatCompletionRequest
import com.lifuyue.kora.core.network.ChatHistoriesRequest
import com.lifuyue.kora.core.network.ChatHistoryItemDto
import com.lifuyue.kora.core.network.ChatRecordItemDto
import com.lifuyue.kora.core.network.CitationDto
import com.lifuyue.kora.core.network.DeleteChatItemRequest
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.NetworkException
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.OpenAiChatCompletionRequest
import com.lifuyue.kora.core.network.PaginationRecordsRequest
import com.lifuyue.kora.core.network.QuestionGuideRequest
import com.lifuyue.kora.core.network.SseEventData
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.UpdateHistoryRequest
import com.lifuyue.kora.core.network.UpdateUserFeedbackRequest
import com.lifuyue.kora.core.network.UploadedAssetRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.time.Instant

typealias RoomBackedChatRepository = RoomChatRepository

class RoomChatRepository
    constructor(
        private val api: FastGptApi,
        private val sseStreamClient: SseStreamClient,
        private val conversationDao: ConversationDao,
        private val conversationFolderDao: ConversationFolderDao,
        private val conversationTagDao: ConversationTagDao,
        private val interactiveDraftDao: InteractiveDraftDao,
        private val messageDao: MessageDao,
        private val context: Context,
        private val json: Json = NetworkJson.default,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        private val responsePlanner: AssistantResponsePlanner? = null,
        private val clock: () -> Long = { System.currentTimeMillis() },
        private val connectionSnapshotProvider: ConnectionSnapshotProvider = ConnectionSnapshotProvider { ConnectionSnapshot() },
        private val localKnowledgeStore: LocalKnowledgeStore? = null,
    ) : ChatRepository, ConversationRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private val streamingJobs = linkedMapOf<String, Job>()
        private val restoringJobs = linkedMapOf<String, Job>()
        private val chatBootstrapByChatId = linkedMapOf<String, ChatBootstrap>()
        private var localId = 0L
        private var lastCreatedAt = 0L

        override suspend fun bootstrapChat(
            appId: String,
            chatId: String?,
        ): ChatBootstrap {
            if (isOpenAiMode(appId)) {
                val resolvedChatId = chatId?.takeIf { it.isNotBlank() } ?: nextId("chat")
                return ChatBootstrap(
                    chatId = resolvedChatId,
                    title = context.getString(R.string.chat_repository_new_conversation_title),
                    welcomeText = null,
                    attachmentConfig = ChatAttachmentConfig(),
                ).also { chatBootstrapByChatId[resolvedChatId] = it }
            }
            val data = api.initChat(appId = appId, chatId = chatId).data
            val resolvedChatId = data?.chatId?.takeIf { it.isNotBlank() } ?: chatId?.takeIf { it.isNotBlank() } ?: nextId("chat")
            return ChatBootstrap(
                chatId = resolvedChatId,
                title = data?.title.orEmpty(),
                welcomeText = data?.welcomeText,
                questionGuide = data?.questionGuide,
                attachmentConfig = data?.fileSelectConfig.toChatAttachmentConfig(),
            ).also { chatBootstrapByChatId[resolvedChatId] = it }
        }

        override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> =
            combine(
                conversationDao.observeConversationsForApp(appId),
                conversationFolderDao.observeAssignments(appId),
                conversationTagDao.observeAssignments(appId),
            ) { conversations, folders, tags ->
                val folderByChatId = folders.associateBy { it.chatId }
                val tagsByChatId = tags.groupBy { it.chatId }
                conversations.map { entity ->
                    entity.toUiModel(
                        folder = folderByChatId[entity.chatId],
                        tags = tagsByChatId[entity.chatId].orEmpty(),
                    )
                }
            }

        override fun observeFolders(appId: String): Flow<List<ConversationFolderUiModel>> =
            conversationFolderDao.observeFolders(appId).map { rows ->
                rows.map { ConversationFolderUiModel(folderId = it.folderId, name = it.name) }
            }

        override fun observeTags(appId: String): Flow<List<ConversationTagUiModel>> =
            conversationTagDao.observeTags(appId).map { rows ->
                rows.map { ConversationTagUiModel(tagId = it.tagId, name = it.name, colorToken = it.colorToken) }
            }

        override suspend fun refreshConversations(appId: String) {
            if (isOpenAiMode(appId)) {
                return
            }
            val histories = api.getHistories(ChatHistoriesRequest(appId = appId)).data?.list.orEmpty()
            onIo {
                val existing = conversationDao.getConversationsForAppIncludingDeleted(appId = appId, limit = Int.MAX_VALUE, offset = 0)
                val existingByChatId = existing.associateBy { it.chatId }
                val remoteChatIds = histories.map { it.chatId }
                val missingChatIds = existing.map { it.chatId }.filterNot(remoteChatIds::contains)

                if (histories.isEmpty()) {
                    val existingChatIds = existing.map { it.chatId }
                    conversationDao.clearByAppId(appId)
                    if (existingChatIds.isNotEmpty()) {
                        conversationFolderDao.clearAssignments(existingChatIds)
                        conversationTagDao.clearAssignments(existingChatIds)
                    }
                    existingChatIds.forEach(messageDao::deleteMessagesForChat)
                    return@onIo
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
                missingChatIds.forEach(conversationDao::softDelete)
                if (missingChatIds.isNotEmpty()) {
                    conversationFolderDao.clearAssignments(missingChatIds)
                    conversationTagDao.clearAssignments(missingChatIds)
                }
                missingChatIds.forEach(messageDao::deleteMessagesForChat)
            }
        }

        override suspend fun renameConversation(
            appId: String,
            chatId: String,
            title: String,
        ) {
            api.updateHistory(
                UpdateHistoryRequest(
                    appId = appId,
                    chatId = chatId,
                    title = title,
                    customTitle = title,
                ),
            )
            onIo {
                conversationDao.updateConversation(
                    chatId = chatId,
                    title = title,
                    customTitle = title,
                    isPinned = null,
                    updateTime = clock(),
                    lastMessagePreview = null,
                )
            }
        }

        override suspend fun togglePinConversation(
            appId: String,
            chatId: String,
            pinned: Boolean,
        ) {
            api.updateHistory(
                UpdateHistoryRequest(
                    appId = appId,
                    chatId = chatId,
                    top = pinned,
                ),
            )
            onIo {
                conversationDao.updateConversation(
                    chatId = chatId,
                    title = null,
                    customTitle = conversationDao.getConversationByChatId(chatId)?.customTitle,
                    isPinned = pinned,
                    updateTime = clock(),
                    lastMessagePreview = null,
                )
            }
        }

        override suspend fun deleteConversation(
            appId: String,
            chatId: String,
        ) {
            streamingJobs.remove(chatId)?.cancel()
            api.deleteHistory(appId = appId, chatId = chatId)
            onIo {
                conversationDao.softDelete(chatId)
                conversationFolderDao.clearAssignment(chatId)
                conversationTagDao.clearAssignments(chatId)
                messageDao.deleteMessagesForChat(chatId)
            }
        }

        override suspend fun clearConversations(appId: String) {
            api.clearHistories(appId)
            onIo {
                val chatIds =
                    conversationDao.getConversationsForAppIncludingDeleted(
                        appId = appId,
                        limit = Int.MAX_VALUE,
                        offset = 0,
                    ).map { it.chatId }
                conversationDao.clearByAppId(appId)
                if (chatIds.isNotEmpty()) {
                    conversationFolderDao.clearAssignments(chatIds)
                    conversationTagDao.clearAssignments(chatIds)
                }
                chatIds.forEach(messageDao::deleteMessagesForChat)
            }
        }

        override suspend fun createFolder(
            appId: String,
            name: String,
        ) {
            onIo {
                conversationFolderDao.upsert(
                    ConversationFolderEntity(
                        folderId = nextId("folder"),
                        appId = appId,
                        name = name,
                        sortOrder = (conversationFolderDao.getLastFolder(appId)?.sortOrder ?: -1L) + 1L,
                    ),
                )
            }
        }

        override suspend fun renameFolder(
            appId: String,
            folderId: String,
            name: String,
        ) {
            onIo { conversationFolderDao.rename(folderId = folderId, name = name) }
        }

        override suspend fun deleteFolder(
            appId: String,
            folderId: String,
        ) {
            onIo { conversationFolderDao.delete(folderId) }
        }

        override suspend fun createTag(
            appId: String,
            name: String,
        ) {
            onIo {
                val nextSortOrder = (conversationTagDao.getLastTag(appId)?.sortOrder ?: -1L) + 1L
                val colorToken = tagColorTokens[(nextSortOrder % tagColorTokens.size.toLong()).toInt()]
                conversationTagDao.upsert(
                    ConversationTagEntity(
                        tagId = nextId("tag"),
                        appId = appId,
                        name = name,
                        colorToken = colorToken,
                        sortOrder = nextSortOrder,
                    ),
                )
            }
        }

        override suspend fun renameTag(
            appId: String,
            tagId: String,
            name: String,
        ) {
            onIo { conversationTagDao.rename(tagId = tagId, name = name) }
        }

        override suspend fun deleteTag(
            appId: String,
            tagId: String,
        ) {
            onIo { conversationTagDao.delete(tagId) }
        }

        override suspend fun moveConversationToFolder(
            appId: String,
            chatId: String,
            folderId: String?,
        ) {
            onIo {
                conversationFolderDao.clearAssignment(chatId)
                if (!folderId.isNullOrBlank()) {
                    conversationFolderDao.upsertAssignment(ConversationFolderCrossRef(chatId = chatId, folderId = folderId))
                }
            }
        }

        override suspend fun setConversationTags(
            appId: String,
            chatId: String,
            tagIds: List<String>,
        ) {
            onIo {
                conversationTagDao.clearAssignments(chatId)
                val refs = tagIds.distinct().map { tagId -> ConversationTagCrossRef(chatId = chatId, tagId = tagId) }
                if (refs.isNotEmpty()) {
                    conversationTagDao.upsertAssignments(refs)
                }
            }
        }

        override suspend fun setConversationArchived(
            appId: String,
            chatId: String,
            archived: Boolean,
        ) {
            onIo { conversationDao.updateArchived(chatId = chatId, isArchived = archived) }
        }

        override fun observeMessages(
            appId: String,
            chatId: String?,
        ): Flow<List<ChatMessageUiModel>> {
            if (chatId.isNullOrBlank()) {
                return flowOf(emptyList())
            }
            return flow {
                val needsRestore = onIo { messageDao.getMessagesForChat(chatId).isEmpty() }
                if (needsRestore) {
                    ensureMessagesRestored(appId = appId, chatId = chatId)
                    emit(emptyList())
                }
                emitAll(
                    combine(
                        messageDao.observeMessagesForChat(chatId),
                        interactiveDraftDao.observeByChatId(chatId),
                    ) { rows, draft ->
                        rows.map { row -> messageToUiModel(row, draft) }
                    },
                )
            }
        }

        override suspend fun restoreMessages(
            appId: String,
            chatId: String,
        ) {
            if (isOpenAiMode(appId)) {
                return
            }
            if (onIo { messageDao.getMessagesForChat(chatId).isNotEmpty() }) {
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
                        payload = record.toStoredPayload(),
                    )
                }
            onIo {
                messageDao.upsertAll(entities)
                updateConversationPreview(chatId, entities.lastOrNull()?.let { messageToUiModel(it, null) }?.markdown.orEmpty())
            }
        }

        override suspend fun sendMessage(
            appId: String,
            chatId: String?,
            text: String,
            attachments: List<AttachmentDraftUiModel>,
        ): String {
            return onIo {
                val prompt = text.trim()
                val uploadedAttachments = attachments.filter { it.uploadStatus == AttachmentUploadStatus.Uploaded && it.uploadedRef != null }
                require(prompt.isNotEmpty() || uploadedAttachments.isNotEmpty()) { "消息不能为空" }

                val initData =
                    if (chatId == null && !isOpenAiMode(appId)) {
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
                            attachmentConfig = initData.fileSelectConfig.toChatAttachmentConfig(),
                        )
                }
                val createdAt = nextCreatedAt()
                val previewText =
                    prompt.ifBlank {
                        uploadedAttachments.joinToString(separator = "、") { it.displayName }
                    }
                val currentUserMessage =
                    ChatCompletionMessageParam(
                        role = "user",
                        content = buildChatCompletionContent(prompt, uploadedAttachments),
                    )
                val localHits =
                    if (isOpenAiMode(appId)) {
                        localKnowledgeStore?.search(prompt).orEmpty()
                    } else {
                        emptyList()
                    }
                val humanMessage =
                    newMessageEntity(
                        dataId = nextId("human"),
                        chatId = resolvedChatId,
                        appId = appId,
                        role = ChatRole.Human,
                        markdown = previewText,
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
                            ?: previewText.take(18).ifBlank { context.getString(R.string.chat_repository_new_conversation_title) },
                    preview = previewText,
                )
                messageDao.upsertAll(listOf(humanMessage, assistantMessage))

                startStreaming(
                    appId = appId,
                    chatId = resolvedChatId,
                    prompt = prompt,
                    assistantMessageId = assistantMessage.dataId,
                    userMessage = currentUserMessage,
                    openAiMessages =
                        if (isOpenAiMode(appId)) {
                            buildOpenAiConversationMessages(
                                chatId = resolvedChatId,
                                currentUserMessage = currentUserMessage,
                                localHits = localHits,
                            )
                        } else {
                            null
                        },
                    localCitations = localHits.map(LocalKnowledgeHit::toCitationPayload),
                )
                resolvedChatId
            }
        }

        override suspend fun stopStreaming(
            appId: String,
            chatId: String,
        ) {
            streamingJobs.remove(chatId)?.cancel()
            onIo {
                val current = messageDao.getMessagesForChat(chatId).lastOrNull { it.role == ChatRole.AI.name && it.isStreaming } ?: return@onIo
                val stoppedMessage = context.getString(R.string.chat_repository_stopped_message)
                messageDao.updateMessageState(
                    dataId = current.dataId,
                    payloadJson = updateStoredPayload(current.payloadJson, errorMessage = stoppedMessage, json = json),
                    isStreaming = false,
                    sendStatus = "stopped",
                    errorCode = current.errorCode,
                )
                updateConversationPreview(
                    chatId,
                    messageToUiModel(
                        current.copy(payloadJson = updateStoredPayload(current.payloadJson, errorMessage = stoppedMessage, json = json)),
                        null,
                    ).markdown,
                )
            }
        }

        override suspend fun continueGeneration(
            appId: String,
            chatId: String,
        ): String {
            return onIo {
                val prompt =
                    messageDao.getMessagesForChat(chatId)
                        .lastOrNull { it.role == ChatRole.Human.name }
                        ?.let { messageToUiModel(it, null) }
                        ?.markdown
                        .orEmpty()
                        .ifBlank { context.getString(R.string.chat_repository_continue_prompt) }
                val assistantMessage =
                    newStreamingAssistantEntity(
                        chatId = chatId,
                        appId = appId,
                        createdAt = nextCreatedAt(),
                    )
                messageDao.upsert(assistantMessage)
                updateConversationPreview(chatId, context.getString(R.string.chat_repository_continue_preview))
                val localHits = if (isOpenAiMode(appId)) localKnowledgeStore?.search(prompt).orEmpty() else emptyList()
                startStreaming(
                    appId = appId,
                    chatId = chatId,
                    prompt = prompt,
                    assistantMessageId = assistantMessage.dataId,
                    openAiMessages =
                        if (isOpenAiMode(appId)) {
                            buildOpenAiConversationMessages(
                                chatId = chatId,
                                currentUserMessage = ChatCompletionMessageParam(role = "user", content = JsonPrimitive(prompt)),
                                localHits = localHits,
                            )
                        } else {
                            null
                        },
                    localCitations = localHits.map(LocalKnowledgeHit::toCitationPayload),
                )
                chatId
            }
        }

        override suspend fun regenerateResponse(
            appId: String,
            chatId: String,
            messageId: String,
        ): String {
            if (!isOpenAiMode(appId)) {
                api.deleteChatItem(DeleteChatItemRequest(appId = appId, chatId = chatId, contentId = messageId))
            }
            return onIo {
                val messages = messageDao.getMessagesForChat(chatId)
                val targetIndex = messages.indexOfFirst { it.dataId == messageId }.takeIf { it >= 0 } ?: messages.lastIndex
                val prompt =
                    messages
                        .take(targetIndex + 1)
                        .lastOrNull { it.role == ChatRole.Human.name }
                        ?.let { messageToUiModel(it, null) }
                        ?.markdown
                        .orEmpty()
                        .ifBlank { context.getString(R.string.chat_repository_continue_prompt) }

                messageDao.deleteMessage(messageId)

                val assistantMessage =
                    newStreamingAssistantEntity(
                        chatId = chatId,
                        appId = appId,
                        createdAt = nextCreatedAt(),
                    )
                messageDao.upsert(assistantMessage)
                updateConversationPreview(chatId, context.getString(R.string.chat_repository_regenerate_preview))
                val localHits = if (isOpenAiMode(appId)) localKnowledgeStore?.search(prompt).orEmpty() else emptyList()
                startStreaming(
                    appId = appId,
                    chatId = chatId,
                    prompt = prompt,
                    assistantMessageId = assistantMessage.dataId,
                    openAiMessages =
                        if (isOpenAiMode(appId)) {
                            buildOpenAiConversationMessages(
                                chatId = chatId,
                                currentUserMessage = ChatCompletionMessageParam(role = "user", content = JsonPrimitive(prompt)),
                                localHits = localHits,
                            )
                        } else {
                            null
                        },
                    localCitations = localHits.map(LocalKnowledgeHit::toCitationPayload),
                )
                chatId
            }
        }

        override suspend fun setFeedback(
            appId: String,
            chatId: String,
            messageId: String,
            feedback: MessageFeedback,
        ) {
            onIo { messageDao.updateFeedback(messageId, feedback.toFeedbackType()) }
            if (!isOpenAiMode(appId)) {
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
        }

        override suspend fun uploadAttachment(
            appId: String,
            chatId: String?,
            attachment: AttachmentDraftUiModel,
            onProgress: (Float) -> Unit,
        ): UploadedAssetRef {
            val uri = Uri.parse(attachment.localUri)
            val metadata = resolveAttachmentMetadata(context, uri)
            val displayName = metadata.displayName.ifBlank { attachment.displayName }
            val mimeType = metadata.mimeType.ifBlank { attachment.mimeType }.ifBlank { "application/octet-stream" }
            val tempFile = File.createTempFile("chat-upload-", displayName.sanitizeUploadSuffix(), context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException(context.chatString("chat_error_attachment_upload_failed"))

                val response =
                    api.uploadChatAttachment(
                        file =
                            MultipartBody.Part.createFormData(
                                "file",
                                displayName,
                                tempFile.asProgressRequestBody(
                                    contentType = mimeType.toMediaType(),
                                    onProgress = onProgress,
                                ),
                            ),
                        appId = appId.toRequestBody("text/plain".toMediaType()),
                        chatId = chatId?.takeIf(String::isNotBlank)?.toRequestBody("text/plain".toMediaType()),
                    )
                return response.data ?: throw IllegalStateException(context.chatString("chat_error_attachment_upload_failed"))
            } finally {
                tempFile.delete()
            }
        }

        override suspend fun savePendingInteractiveDraft(
            appId: String,
            chatId: String,
            card: InteractiveCardUiModel,
            draftPayloadJson: String?,
        ) {
            onIo {
                interactiveDraftDao.upsert(
                    InteractiveDraftEntity(
                        chatId = chatId,
                        messageDataId = card.messageDataId,
                        responseValueId = card.responseValueId,
                        rawPayloadJson = card.toRawPayloadJson(),
                        draftPayloadJson = draftPayloadJson,
                        updatedAt = clock(),
                    ),
                )
            }
        }

        override suspend fun clearPendingInteractiveDraft(
            appId: String,
            chatId: String,
        ) {
            onIo { interactiveDraftDao.deleteByChatId(chatId) }
        }

        override suspend fun submitInteractiveResponse(
            appId: String,
            chatId: String,
            card: InteractiveCardUiModel,
            value: String,
        ): String {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "交互输入不能为空" }
            val displayValue = card.displaySubmissionValue(trimmed)

            val createdAt = nextCreatedAt()
            val submissionPayload = card.toDraftPayloadJson(value = trimmed, status = InteractiveCardStatus.Submitting)
            onIo {
                interactiveDraftDao.upsert(
                    InteractiveDraftEntity(
                        chatId = chatId,
                        messageDataId = card.messageDataId,
                        responseValueId = card.responseValueId,
                        rawPayloadJson = card.toRawPayloadJson(),
                        draftPayloadJson = submissionPayload,
                        updatedAt = clock(),
                    ),
                )
                updateInteractiveMessageState(
                    messageDataId = card.messageDataId,
                    status = InteractiveCardStatus.Submitting,
                    submissionPayloadJson = submissionPayload,
                )
            }
            val humanMessage =
                newMessageEntity(
                    dataId = nextId("human"),
                    chatId = chatId,
                    appId = appId,
                    role = ChatRole.Human,
                    markdown = displayValue,
                    sendStatus = "sent",
                    isStreaming = false,
                    createdAt = createdAt,
                )
            val assistantMessage =
                newStreamingAssistantEntity(
                    chatId = chatId,
                    appId = appId,
                    createdAt = createdAt + 1L,
                )
            onIo {
                messageDao.upsertAll(listOf(humanMessage, assistantMessage))
                updateConversationPreview(chatId, displayValue)
            }
            val localHits = if (isOpenAiMode(appId)) localKnowledgeStore?.search(displayValue).orEmpty() else emptyList()
            startStreaming(
                appId = appId,
                chatId = chatId,
                prompt = displayValue,
                assistantMessageId = assistantMessage.dataId,
                userMessage =
                    ChatCompletionMessageParam(
                        role = "user",
                        content = JsonPrimitive(displayValue),
                        interactive = card.toSubmissionPayload(trimmed),
                    ),
                openAiMessages =
                    if (isOpenAiMode(appId)) {
                        buildOpenAiConversationMessages(
                            chatId = chatId,
                            currentUserMessage = ChatCompletionMessageParam(role = "user", content = JsonPrimitive(displayValue)),
                            localHits = localHits,
                        )
                    } else {
                        null
                    },
                localCitations = localHits.map(LocalKnowledgeHit::toCitationPayload),
                onSuccess = {
                    interactiveDraftDao.deleteByChatId(chatId)
                    updateInteractiveMessageState(
                        messageDataId = card.messageDataId,
                        status = InteractiveCardStatus.Resolved,
                        submissionPayloadJson = submissionPayload,
                    )
                },
            )
            return chatId
        }

        private fun ensureMessagesRestored(
            appId: String,
            chatId: String,
        ) {
            if (restoringJobs.containsKey(chatId)) {
                return
            }
            restoringJobs[chatId] =
                scope.launch {
                    try {
                        if (messageDao.getMessagesForChat(chatId).isEmpty()) {
                            restoreMessages(appId = appId, chatId = chatId)
                        }
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
            userMessage: ChatCompletionMessageParam? = null,
            openAiMessages: List<ChatCompletionMessageParam>? = null,
            localCitations: List<CitationPayload> = emptyList(),
            onSuccess: suspend () -> Unit = {},
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
                                coroutineContext.ensureActive()
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
                            streamAssistantUpdates(
                                appId = appId,
                                chatId = chatId,
                                assistantMessageId = assistantMessageId,
                                prompt = prompt,
                                userMessage = userMessage,
                                openAiMessages = openAiMessages,
                            ).collect { event ->
                                    val update =
                                        event.toAssistantUpdate(
                                            malformedMessage = context.getString(R.string.chat_repository_malformed_sse),
                                        )
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
                            coroutineContext.ensureActive()
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
                                localCitations = localCitations,
                            )
                            onSuccess()
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
                            errorMessage =
                                networkError?.message
                                    ?: error.message
                                    ?: context.getString(R.string.chat_repository_stream_failed),
                        )
                        updateConversationPreview(
                            chatId,
                            markdown.ifBlank {
                                error.message
                                    ?: context.getString(R.string.chat_repository_stream_failed)
                            },
                        )
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

        private fun updateConversationPreview(
            chatId: String,
            preview: String,
        ) {
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

        private fun messageToUiModel(
            entity: MessageEntity,
            draft: InteractiveDraftEntity?,
        ): ChatMessageUiModel {
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
                interactiveCard = storedPayload.toInteractiveCard(messageId = entity.dataId, draft = draft, json = json),
            )
        }

        private fun updateInteractiveMessageState(
            messageDataId: String,
            status: InteractiveCardStatus,
            submissionPayloadJson: String,
        ) {
            val source = messageDao.getMessageById(messageDataId) ?: return
            val payload = source.payloadJson.toStoredPayload(json)
            messageDao.updateMessageState(
                dataId = messageDataId,
                payloadJson =
                    payload.copy(
                        interactiveStatus = status.name,
                        interactiveSubmissionJson = submissionPayloadJson,
                    ).toJsonString(),
                isStreaming = source.isStreaming,
                sendStatus = source.sendStatus,
                errorCode = source.errorCode,
            )
        }

        private suspend fun attachAssistantEnhancements(
            appId: String,
            chatId: String,
            assistantMessageId: String,
            localCitations: List<CitationPayload> = emptyList(),
        ) {
            if (isOpenAiMode(appId)) {
                if (localCitations.isNotEmpty()) {
                    val current = onIo { messageDao.getMessagesForChat(chatId).firstOrNull { it.dataId == assistantMessageId } } ?: return
                    val currentPayload = current.payloadJson.toStoredPayload(json)
                    onIo {
                        messageDao.updateMessageState(
                            dataId = assistantMessageId,
                            payloadJson = currentPayload.copy(citations = localCitations).toJsonString(),
                            isStreaming = current.isStreaming,
                            sendStatus = current.sendStatus,
                            errorCode = current.errorCode,
                        )
                    }
                }
                return
            }
            val current = onIo { messageDao.getMessagesForChat(chatId).firstOrNull { it.dataId == assistantMessageId } } ?: return
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
                        (
                            api.getResData(
                                appId = appId,
                                dataId = assistantMessageId,
                                chatId = chatId,
                            ).data ?: JsonArray(emptyList())
                        ).extractCitations()
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
            onIo {
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

        private fun streamAssistantUpdates(
            appId: String,
            chatId: String,
            assistantMessageId: String,
            prompt: String,
            userMessage: ChatCompletionMessageParam?,
            openAiMessages: List<ChatCompletionMessageParam>? = null,
        ): Flow<SseEventData> {
            val resolvedUserMessage = userMessage ?: ChatCompletionMessageParam(role = "user", content = JsonPrimitive(prompt))
            return if (isOpenAiMode(appId)) {
                sseStreamClient.streamOpenAiChatCompletions(
                    OpenAiChatCompletionRequest(
                        model = connectionSnapshotProvider.getSnapshot().model.orEmpty(),
                        messages = openAiMessages ?: listOf(resolvedUserMessage),
                    ),
                )
            } else {
                sseStreamClient.streamChatCompletions(
                    ChatCompletionRequest(
                        chatId = chatId,
                        appId = appId,
                        responseChatItemId = assistantMessageId,
                        messages = listOf(resolvedUserMessage),
                    ),
                )
            }
        }

        private fun isOpenAiMode(appId: String): Boolean = appId == DIRECT_OPENAI_APP_ID

        private fun buildOpenAiConversationMessages(
            chatId: String,
            currentUserMessage: ChatCompletionMessageParam,
            localHits: List<LocalKnowledgeHit>,
        ): List<ChatCompletionMessageParam> {
            val history =
                messageDao.getMessagesForChat(chatId)
                    .takeLast(12)
                    .mapNotNull { entity ->
                        val payload = entity.payloadJson.toStoredPayload(json)
                        val markdown = payload.markdown.trim()
                        when (entity.role) {
                            ChatRole.Human.name ->
                                markdown.takeIf { it.isNotBlank() }?.let {
                                    ChatCompletionMessageParam(role = "user", content = JsonPrimitive(it))
                                }
                            ChatRole.AI.name ->
                                markdown.takeIf { it.isNotBlank() }?.let {
                                    ChatCompletionMessageParam(role = "assistant", content = JsonPrimitive(it))
                                }
                            else -> null
                        }
                    }
            val localContextMessage =
                localHits.takeIf { it.isNotEmpty() }?.let { hits ->
                    ChatCompletionMessageParam(
                        role = "system",
                        content =
                            JsonPrimitive(
                                buildString {
                                    appendLine("Use the following local references when relevant.")
                                    hits.forEachIndexed { index, hit ->
                                        appendLine("[${index + 1}] ${hit.title} (${hit.sourceLabel})")
                                        appendLine(hit.snippet)
                                        appendLine()
                                    }
                                }.trim(),
                            ),
                    )
                }
            return buildList {
                localContextMessage?.let(::add)
                addAll(history)
                add(currentUserMessage)
            }
        }

        private suspend fun <T> onIo(block: suspend () -> T): T = withContext(ioDispatcher) { block() }
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
    val interactiveStatus: String? = null,
    val interactiveSubmissionJson: String? = null,
)

private data class CitationPayload(
    val datasetId: String? = null,
    val collectionId: String? = null,
    val dataId: String? = null,
    val title: String = "",
    val sourceName: String = "",
    val snippet: String = "",
    val scoreType: String? = null,
    val score: Double? = null,
)

private data class InteractivePayload(
    val kind: InteractiveCardKind,
    val responseValueId: String? = null,
    val fields: List<InteractiveFieldUiModel> = emptyList(),
    val options: List<String> = emptyList(),
)

private data class InteractiveDraftSnapshot(
    val status: InteractiveCardStatus? = null,
    val singleValue: String? = null,
    val fieldValues: Map<String, String> = emptyMap(),
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

private fun ChatRecordItemDto.toStoredPayload(): StoredMessagePayload =
    StoredMessagePayload(
        markdown = extractVisibleMarkdown(value),
        eventPayloads = value.extractInteractiveEventPayloads(),
    )

private fun ConversationEntity.displayTitle(): String = customTitle?.takeIf { it.isNotBlank() } ?: title

private fun ConversationEntity.toUiModel(
    folder: ConversationFolderAssignmentRow?,
    tags: List<ConversationTagAssignmentRow>,
): ConversationListItemUiModel =
    ConversationListItemUiModel(
        chatId = chatId,
        appId = appId,
        title = displayTitle,
        preview = lastMessagePreview.orEmpty(),
        folderId = folder?.folderId,
        folderName = folder?.folderName,
        tags =
            tags.map {
                ConversationTagUiModel(
                    tagId = it.tagId,
                    name = it.tagName,
                    colorToken = it.colorToken,
                )
            },
        isPinned = isPinned,
        isArchived = isArchived,
        updateTime = updateTime,
    )

private val ConversationEntity.displayTitle: String
    get() = displayTitle()

private fun String.toChatRole(): ChatRole =
    ChatRole.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: ChatRole.Human

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

private val tagColorTokens =
    listOf(
        "amber",
        "mint",
        "sky",
        "coral",
        "indigo",
        "rose",
        "teal",
        "slate",
    )

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
                        ?.map { element -> element.stringContentOrNull() ?: element.toString() }
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
                                    sourceName = citation["sourceName"].stringContentOrNull().orEmpty(),
                                    snippet = citation["snippet"].stringContentOrNull().orEmpty(),
                                    scoreType = citation["scoreType"].stringContentOrNull(),
                                    score = citation["score"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                                )
                            }
                        }.orEmpty(),
                suggestedQuestions =
                    payload["suggestedQuestions"]
                        ?.jsonArray
                        ?.mapNotNull { it.stringContentOrNull() }
                        .orEmpty(),
                interactiveStatus = payload["interactiveStatus"].stringContentOrNull(),
                interactiveSubmissionJson = payload["interactiveSubmissionJson"].stringContentOrNull(),
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

private fun String.toEpochMillis(): Long = runCatching { Instant.parse(this).toEpochMilli() }.getOrElse { 0L }

private fun SseEventData.toAssistantUpdate(malformedMessage: String): AssistantUpdate {
    if (isMalformed) {
        return AssistantUpdate(
            error =
                NetworkError(
                    code = -1,
                    statusText = "malformed",
                    message = parseExceptionMessage ?: malformedMessage,
                ),
        )
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

private fun extractVisibleMarkdown(value: JsonElement?): String =
    when (value) {
        null -> ""
        is JsonArray ->
            value.joinToString(separator = "") { element ->
                if (element is JsonObject && (element.containsKey("interactive") || element.containsKey("collectionForm"))) {
                    ""
                } else {
                    extractMarkdown(element)
                }
            }
        else -> extractMarkdown(value)
    }

private fun JsonElement?.stringContentOrNull(): String? = (this as? JsonPrimitive)?.content

private fun JsonElement?.extractInteractiveEventPayloads(): List<String> =
    when (this) {
        is JsonArray -> flatMap { it.extractInteractiveEventPayloads() }
        is JsonObject ->
            buildList {
                this@extractInteractiveEventPayloads["interactive"]?.let { add(it.toString()) }
                this@extractInteractiveEventPayloads["collectionForm"]?.let { add(it.toString()) }
            }
        else -> emptyList()
    }

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
                                put("sourceName", JsonPrimitive(it.sourceName))
                                put("snippet", JsonPrimitive(it.snippet))
                                it.scoreType?.let { scoreType -> put("scoreType", JsonPrimitive(scoreType)) }
                                it.score?.let { score -> put("score", JsonPrimitive(score)) }
                            },
                        )
                    },
                ),
            )
            put("suggestedQuestions", JsonArray(suggestedQuestions.map(::JsonPrimitive)))
            errorMessage?.let { put("errorMessage", JsonPrimitive(it)) }
            interactiveStatus?.let { put("interactiveStatus", JsonPrimitive(it)) }
            interactiveSubmissionJson?.let { put("interactiveSubmissionJson", JsonPrimitive(it)) }
        },
    ).toString()

private fun StoredMessagePayload.toInteractiveCard(
    messageId: String,
    draft: InteractiveDraftEntity?,
    json: Json,
): InteractiveCardUiModel? {
    val payload =
        eventPayloads.firstNotNullOfOrNull { raw ->
            runCatching { json.parseToJsonElement(raw) }.getOrNull()?.extractInteractivePayload()
        } ?: draft
            ?.takeIf { it.messageDataId == messageId }
            ?.rawPayloadJson
            ?.let { raw -> runCatching { json.parseToJsonElement(raw) }.getOrNull()?.extractInteractivePayload() }
            ?: return null
    val draftSnapshot = draft?.takeIf { it.messageDataId == messageId }?.draftPayloadJson.extractInteractiveDraftSnapshot(json)
    val submissionSnapshot = interactiveSubmissionJson.extractInteractiveDraftSnapshot(json)
    val status = draftSnapshot?.status ?: interactiveStatus.toInteractiveCardStatus() ?: InteractiveCardStatus.Pending
    val valueSource = draftSnapshot ?: submissionSnapshot
    val resolvedFields =
        payload.fields.map { field ->
            field.copy(value = valueSource?.fieldValues?.get(field.id).orEmpty())
        }
    return InteractiveCardUiModel(
        kind = payload.kind,
        messageDataId = messageId,
        responseValueId = draft?.responseValueId ?: payload.responseValueId,
        status = status,
        fields = resolvedFields,
        options = payload.options,
        selectedOption = valueSource?.singleValue,
    )
}

private fun JsonElement.extractInteractivePayload(): InteractivePayload? {
    val objectValue = this as? JsonObject ?: return null
    val candidate =
        when {
            objectValue.containsKey("interactive") -> objectValue["interactive"]?.jsonObject
            objectValue.containsKey("collectionForm") -> objectValue["collectionForm"]?.jsonObject
            else -> objectValue
        } ?: return null

    val rawKind =
        candidate["type"].stringContentOrNull()
            ?: candidate["kind"].stringContentOrNull()
            ?: if (objectValue.containsKey("collectionForm") || candidate.containsKey("fields")) "collectionForm" else null
            ?: return null
    val kind =
        when (rawKind.lowercase()) {
            "userselect" -> InteractiveCardKind.UserSelect
            "userinput" -> InteractiveCardKind.UserInput
            "collectionform" -> InteractiveCardKind.CollectionForm
            else -> return null
        }
    val responseValueId =
        candidate["responseValueId"].stringContentOrNull()
            ?: candidate["valueId"].stringContentOrNull()
    val options =
        candidate["options"]
            ?.jsonArray
            ?.mapNotNull { option ->
                when (option) {
                    is JsonObject ->
                        option["label"].stringContentOrNull()
                            ?: option["text"].stringContentOrNull()
                            ?: option["value"].stringContentOrNull()
                    else -> option.stringContentOrNull()
                }
            }.orEmpty()
    val fields =
        buildList {
            candidate["fields"]
                ?.jsonArray
                ?.mapNotNull { field ->
                    when (field) {
                        is JsonObject -> {
                            val id =
                                field["name"].stringContentOrNull()
                                    ?: field["key"].stringContentOrNull()
                                    ?: field["label"].stringContentOrNull()
                                    ?: return@mapNotNull null
                            InteractiveFieldUiModel(
                                id = id,
                                label = field["label"].stringContentOrNull() ?: id,
                                required = field["required"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true,
                            )
                        }
                        else -> {
                            val label = field.stringContentOrNull() ?: return@mapNotNull null
                            InteractiveFieldUiModel(id = label, label = label)
                        }
                    }
                }?.forEach(::add)
        }
    val normalizedFields =
        if (fields.isEmpty() && kind != InteractiveCardKind.UserSelect) {
            listOf(
                InteractiveFieldUiModel(
                    id = "value",
                    label =
                        candidate["header"].stringContentOrNull()
                            ?: candidate["prompt"].stringContentOrNull()
                            ?: "Input",
                ),
            )
        } else {
            fields.distinctBy { it.id }
        }
    if (options.isEmpty() && normalizedFields.isEmpty() && kind == InteractiveCardKind.UserSelect) {
        return null
    }
    return InteractivePayload(
        kind = kind,
        responseValueId = responseValueId,
        fields = normalizedFields,
        options = options.distinct(),
    )
}

private fun String?.extractInteractiveDraftSnapshot(json: Json): InteractiveDraftSnapshot? =
    this?.let { raw ->
        val element = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return@let null
        val status =
            when (element["status"].stringContentOrNull()?.lowercase()) {
                "pending" -> InteractiveCardStatus.Pending
                "submitting" -> InteractiveCardStatus.Submitting
                "resolved" -> InteractiveCardStatus.Resolved
                "expired" -> InteractiveCardStatus.Expired
                else -> null
            }
        val fieldValues =
            element["fieldValues"]
                ?.jsonObject
                ?.mapValues { (_, value) -> value.stringContentOrNull().orEmpty() }
                .orEmpty()
        InteractiveDraftSnapshot(
            status = status,
            singleValue = element["value"].stringContentOrNull() ?: element["selected"].stringContentOrNull(),
            fieldValues = fieldValues,
        )
    }

private fun String?.toInteractiveCardStatus(): InteractiveCardStatus? =
    when (this?.lowercase()) {
        "pending" -> InteractiveCardStatus.Pending
        "submitting" -> InteractiveCardStatus.Submitting
        "resolved" -> InteractiveCardStatus.Resolved
        "expired" -> InteractiveCardStatus.Expired
        else -> null
    }

private fun InteractiveCardUiModel.toRawPayloadJson(): String =
    JsonObject(
        buildMap {
            put(
                "type",
                JsonPrimitive(
                    when (kind) {
                        InteractiveCardKind.UserSelect -> "userSelect"
                        InteractiveCardKind.UserInput -> "userInput"
                        InteractiveCardKind.CollectionForm -> "collectionForm"
                    },
                ),
            )
            responseValueId?.let { put("responseValueId", JsonPrimitive(it)) }
            if (fields.isNotEmpty()) {
                put(
                    "fields",
                    JsonArray(
                        fields.map { field ->
                            JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive(field.id),
                                    "label" to JsonPrimitive(field.label),
                                ),
                            )
                        },
                    ),
                )
            }
            if (options.isNotEmpty()) {
                put("options", JsonArray(options.map(::JsonPrimitive)))
            }
        },
    ).toString()

private fun InteractiveCardUiModel.toSubmissionPayload(value: String): JsonObject =
    JsonObject(
        buildMap {
            put(
                "type",
                JsonPrimitive(
                    when (kind) {
                        InteractiveCardKind.UserSelect -> "userSelect"
                        InteractiveCardKind.UserInput -> "userInput"
                        InteractiveCardKind.CollectionForm -> "collectionForm"
                    },
                ),
            )
            put("messageDataId", JsonPrimitive(messageDataId))
            responseValueId?.let { put("responseValueId", JsonPrimitive(it)) }
            runCatching { NetworkJson.default.parseToJsonElement(value) }
                .getOrNull()
                ?.let { parsed ->
                    if (parsed is JsonObject && parsed.containsKey("fieldValues")) {
                        parsed["fieldValues"]?.let { put("fieldValues", it) }
                    } else {
                        put("value", JsonPrimitive(value))
                    }
                } ?: put("value", JsonPrimitive(value))
        },
    )

private fun InteractiveCardUiModel.displaySubmissionValue(value: String): String {
    if (kind == InteractiveCardKind.UserSelect) {
        return value
    }
    val parsed = runCatching { NetworkJson.default.parseToJsonElement(value) }.getOrNull() as? JsonObject
    val fieldValues =
        parsed?.get("fieldValues")
            ?.jsonObject
            ?.mapValues { (_, fieldValue) -> fieldValue.stringContentOrNull().orEmpty() }
            .orEmpty()
    if (fieldValues.isEmpty()) {
        return value
    }
    return fields.joinToString(separator = "\n") { field ->
        "${field.label}: ${fieldValues[field.id].orEmpty()}"
    }
}

private fun InteractiveCardUiModel.toDraftPayloadJson(
    value: String,
    status: InteractiveCardStatus,
): String {
    val json = NetworkJson.default
    val parsed = runCatching { json.parseToJsonElement(value) }.getOrNull() as? JsonObject
    val payload =
        buildMap<String, JsonElement> {
            put("status", JsonPrimitive(status.name))
            parsed?.get("fieldValues")?.let { put("fieldValues", it) }
            if (parsed?.containsKey("fieldValues") != true) {
                if (kind == InteractiveCardKind.UserSelect) {
                    put("selected", JsonPrimitive(value))
                } else {
                    put("value", JsonPrimitive(value))
                }
            }
        }
    return JsonObject(payload).toString()
}

private fun CitationPayload.toUiModel(): CitationItemUiModel =
    CitationItemUiModel(
        datasetId = datasetId,
        collectionId = collectionId,
        dataId = dataId,
        title = title,
        sourceName = sourceName,
        snippet = snippet,
        scoreType = scoreType,
        score = score,
    )

private fun LocalKnowledgeHit.toCitationPayload(): CitationPayload =
    CitationPayload(
        datasetId = documentId,
        collectionId = null,
        dataId = chunkId,
        title = title,
        sourceName = sourceLabel,
        snippet = snippet,
        scoreType = "local",
        score = score,
    )

private fun CitationDto.toPayload(): CitationPayload =
    CitationPayload(
        datasetId = datasetId,
        collectionId = collectionId,
        dataId = dataId,
        title = title,
        sourceName = sourceName.orEmpty(),
        snippet = snippet,
        scoreType = scoreType,
        score = score,
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

private fun File.asProgressRequestBody(
    contentType: okhttp3.MediaType,
    onProgress: (Float) -> Unit,
): RequestBody =
    object : RequestBody() {
        override fun contentType(): okhttp3.MediaType = contentType

        override fun contentLength(): Long = length()

        override fun writeTo(sink: BufferedSink) {
            val totalBytes = length().coerceAtLeast(0L)
            var writtenBytes = 0L
            inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) {
                        break
                    }
                    sink.write(buffer, 0, read)
                    writtenBytes += read
                    if (totalBytes > 0L) {
                        onProgress((writtenBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
            if (totalBytes == 0L) {
                onProgress(1f)
            }
        }
    }

private fun String.sanitizeUploadSuffix(): String {
    val suffix = substringAfterLast('.', missingDelimiterValue = "").takeIf { it.isNotBlank() }
    return if (suffix == null) ".bin" else ".$suffix"
}
