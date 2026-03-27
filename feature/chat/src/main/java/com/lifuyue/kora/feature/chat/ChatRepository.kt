package com.lifuyue.kora.feature.chat

import com.lifuyue.kora.core.common.ChatRole
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface ConversationRepository {
    fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>>

    suspend fun refreshConversations(appId: String)

    suspend fun renameConversation(appId: String, chatId: String, title: String)

    suspend fun togglePinConversation(appId: String, chatId: String, pinned: Boolean)

    suspend fun deleteConversation(appId: String, chatId: String)

    suspend fun clearConversations(appId: String)
}

interface ChatRepository {
    fun observeMessages(appId: String, chatId: String?): Flow<List<ChatMessageUiModel>>

    suspend fun sendMessage(appId: String, chatId: String?, text: String): String

    suspend fun stopStreaming(appId: String, chatId: String)

    suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String

    suspend fun setFeedback(
        appId: String,
        chatId: String,
        messageId: String,
        feedback: MessageFeedback,
    )
}

data class AssistantResponseRequest(
    val appId: String,
    val chatId: String,
    val prompt: String,
    val attempt: Int,
)

data class AssistantResponseStep(
    val markdownDelta: String = "",
    val reasoningDelta: String = "",
    val delayMillis: Long = 0L,
    val terminalError: String? = null,
)

fun interface AssistantResponsePlanner {
    fun plan(request: AssistantResponseRequest): List<AssistantResponseStep>
}

@Singleton
class InMemoryChatRepository internal constructor(
    private val scope: CoroutineScope,
    private val responsePlanner: AssistantResponsePlanner,
) : ChatRepository, ConversationRepository {
    constructor() : this(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        responsePlanner = defaultAssistantResponsePlanner(),
    )

    private val conversationsByApp =
        MutableStateFlow<Map<String, List<ConversationListItemUiModel>>>(emptyMap())
    private val messagesByChat =
        MutableStateFlow<Map<String, List<ChatMessageUiModel>>>(emptyMap())
    private val streamingJobs = linkedMapOf<String, Job>()
    private var nextId = 0L

    override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> =
        conversationsByApp.map { state ->
            state[appId].orEmpty().sortedWith(compareByDescending<ConversationListItemUiModel> { it.isPinned }
                .thenByDescending { it.updateTime }
                .thenByDescending { it.chatId })
        }

    override suspend fun refreshConversations(appId: String) = Unit

    override suspend fun renameConversation(appId: String, chatId: String, title: String) {
        updateConversation(appId, chatId) { current ->
            current.copy(title = title, updateTime = now())
        }
    }

    override suspend fun togglePinConversation(appId: String, chatId: String, pinned: Boolean) {
        updateConversation(appId, chatId) { current ->
            current.copy(isPinned = pinned, updateTime = now())
        }
    }

    override suspend fun deleteConversation(appId: String, chatId: String) {
        streamingJobs.remove(chatId)?.cancel()
        conversationsByApp.value =
            conversationsByApp.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty().filterNot { it.chatId == chatId })
            }
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                remove(chatId)
            }
    }

    override suspend fun clearConversations(appId: String) {
        val removedIds = conversationsByApp.value[appId].orEmpty().map { it.chatId }.toSet()
        removedIds.forEach { chatId -> streamingJobs.remove(chatId)?.cancel() }
        conversationsByApp.value =
            conversationsByApp.value.toMutableMap().apply {
                put(appId, emptyList())
            }
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                removedIds.forEach(::remove)
            }
    }

    override fun observeMessages(appId: String, chatId: String?): Flow<List<ChatMessageUiModel>> =
        messagesByChat.map { state ->
            val resolvedChatId = chatId ?: return@map emptyList()
            state[resolvedChatId].orEmpty().filter { it.appId == appId }
        }

    override suspend fun sendMessage(appId: String, chatId: String?, text: String): String {
        val prompt = text.trim()
        require(prompt.isNotEmpty()) { "消息不能为空" }

        val resolvedChatId = chatId ?: nextId("chat")
        val conversationSnapshot = conversationsByApp.value[appId].orEmpty()
        val now = now()
        val humanMessage =
            ChatMessageUiModel(
                messageId = nextId("human"),
                chatId = resolvedChatId,
                appId = appId,
                role = ChatRole.Human,
                markdown = prompt,
            )
        val assistantMessage =
            ChatMessageUiModel(
                messageId = nextId("assistant"),
                chatId = resolvedChatId,
                appId = appId,
                role = ChatRole.AI,
                markdown = "",
                isStreaming = true,
                deliveryState = MessageDeliveryState.Streaming,
            )

        upsertConversation(
            appId = appId,
            chatId = resolvedChatId,
            title = conversationSnapshot.firstOrNull { it.chatId == resolvedChatId }?.title ?: conversationTitle(prompt),
            preview = prompt,
            updateTime = now,
        )
        appendMessages(resolvedChatId, listOf(humanMessage, assistantMessage))
        startStreaming(
            request =
                AssistantResponseRequest(
                    appId = appId,
                    chatId = resolvedChatId,
                    prompt = prompt,
                    attempt = 1 + messagesByChat.value[resolvedChatId].orEmpty().count { it.role == ChatRole.AI },
                ),
            assistantMessageId = assistantMessage.messageId,
        )
        return resolvedChatId
    }

    override suspend fun stopStreaming(appId: String, chatId: String) {
        streamingJobs.remove(chatId)?.cancel()
        val currentMessages = messagesByChat.value[chatId].orEmpty()
        val updated =
            currentMessages.map { message ->
                if (message.role == ChatRole.AI && message.isStreaming) {
                    message.copy(
                        isStreaming = false,
                        deliveryState = MessageDeliveryState.Stopped,
                        errorMessage = "已停止生成",
                    )
                } else {
                    message
                }
            }
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                put(chatId, updated)
            }
        val preview = updated.lastOrNull { it.markdown.isNotBlank() }?.markdown.orEmpty().ifBlank { "已停止生成" }
        touchConversation(appId, chatId, preview)
    }

    override suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String {
        val messages = messagesByChat.value[chatId].orEmpty()
        val targetIndex = messages.indexOfFirst { it.messageId == messageId }.takeIf { it >= 0 } ?: messages.lastIndex
        val prompt =
            messages
                .take(targetIndex + 1)
                .lastOrNull { it.role == ChatRole.Human }
                ?.markdown
                .orEmpty()
                .ifBlank { "继续上一个回答" }
        val assistantMessage =
            ChatMessageUiModel(
                messageId = nextId("assistant"),
                chatId = chatId,
                appId = appId,
                role = ChatRole.AI,
                markdown = "",
                isStreaming = true,
                deliveryState = MessageDeliveryState.Streaming,
            )
        appendMessages(chatId, listOf(assistantMessage))
        touchConversation(appId, chatId, "重新生成中")
        startStreaming(
            request =
                AssistantResponseRequest(
                    appId = appId,
                    chatId = chatId,
                    prompt = prompt,
                    attempt = 1 + messages.count { it.role == ChatRole.AI },
                ),
            assistantMessageId = assistantMessage.messageId,
        )
        return chatId
    }

    override suspend fun setFeedback(appId: String, chatId: String, messageId: String, feedback: MessageFeedback) {
        val updated =
            messagesByChat.value[chatId].orEmpty().map { message ->
                if (message.messageId == messageId) {
                    message.copy(feedback = feedback)
                } else {
                    message
                }
            }
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                put(chatId, updated)
            }
        touchConversation(appId, chatId, updated.lastOrNull()?.markdown.orEmpty())
    }

    private fun startStreaming(request: AssistantResponseRequest, assistantMessageId: String) {
        streamingJobs.remove(request.chatId)?.cancel()
        streamingJobs[request.chatId] =
            scope.launch {
                var assistantMarkdown = ""
                var reasoning = ""
                try {
                    responsePlanner.plan(request).forEach { step ->
                        if (step.delayMillis > 0) {
                            delay(step.delayMillis)
                        }
                        if (step.terminalError != null) {
                            updateAssistantMessage(
                                chatId = request.chatId,
                                messageId = assistantMessageId,
                                markdown = assistantMarkdown,
                                reasoning = reasoning,
                                isStreaming = false,
                                deliveryState = MessageDeliveryState.Failed,
                                errorMessage = step.terminalError,
                            )
                            touchConversation(
                                appId = request.appId,
                                chatId = request.chatId,
                                preview = assistantMarkdown.ifBlank { step.terminalError },
                            )
                            streamingJobs.remove(request.chatId)
                            return@launch
                        }
                        assistantMarkdown += step.markdownDelta
                        reasoning += step.reasoningDelta
                        updateAssistantMessage(
                            chatId = request.chatId,
                            messageId = assistantMessageId,
                            markdown = assistantMarkdown,
                            reasoning = reasoning,
                            isStreaming = true,
                            deliveryState = MessageDeliveryState.Streaming,
                            errorMessage = null,
                        )
                    }
                    updateAssistantMessage(
                        chatId = request.chatId,
                        messageId = assistantMessageId,
                        markdown = assistantMarkdown,
                        reasoning = reasoning,
                        isStreaming = false,
                        deliveryState = MessageDeliveryState.Sent,
                        errorMessage = null,
                    )
                    touchConversation(
                        appId = request.appId,
                        chatId = request.chatId,
                        preview = assistantMarkdown.ifBlank { request.prompt },
                    )
                } catch (_: CancellationException) {
                    return@launch
                } finally {
                    streamingJobs.remove(request.chatId)
                }
            }
    }

    private fun appendMessages(chatId: String, newMessages: List<ChatMessageUiModel>) {
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                put(chatId, get(chatId).orEmpty() + newMessages)
            }
    }

    private fun updateAssistantMessage(
        chatId: String,
        messageId: String,
        markdown: String,
        reasoning: String,
        isStreaming: Boolean,
        deliveryState: MessageDeliveryState,
        errorMessage: String?,
    ) {
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                put(
                    chatId,
                    get(chatId).orEmpty().map { message ->
                        if (message.messageId == messageId) {
                            message.copy(
                                markdown = markdown,
                                reasoning = reasoning,
                                isStreaming = isStreaming,
                                deliveryState = deliveryState,
                                errorMessage = errorMessage,
                            )
                        } else {
                            message
                        }
                    },
                )
            }
    }

    private fun upsertConversation(
        appId: String,
        chatId: String,
        title: String,
        preview: String,
        updateTime: Long,
    ) {
        val current = conversationsByApp.value[appId].orEmpty()
        val existing = current.firstOrNull { it.chatId == chatId }
        val updated =
            current.filterNot { it.chatId == chatId } +
                ConversationListItemUiModel(
                    chatId = chatId,
                    appId = appId,
                    title = existing?.title ?: title,
                    preview = preview.take(140),
                    isPinned = existing?.isPinned ?: false,
                    updateTime = updateTime,
                )
        conversationsByApp.value =
            conversationsByApp.value.toMutableMap().apply {
                put(appId, updated)
            }
    }

    private fun touchConversation(appId: String, chatId: String, preview: String) {
        updateConversation(appId, chatId) { current ->
            current.copy(
                preview = preview.take(140),
                updateTime = now(),
            )
        }
    }

    private fun updateConversation(
        appId: String,
        chatId: String,
        transform: (ConversationListItemUiModel) -> ConversationListItemUiModel,
    ) {
        val current = conversationsByApp.value[appId].orEmpty()
        conversationsByApp.value =
            conversationsByApp.value.toMutableMap().apply {
                put(
                    appId,
                    current.map { item ->
                        if (item.chatId == chatId) {
                            transform(item)
                        } else {
                            item
                        }
                    },
                )
            }
    }

    private fun nextId(prefix: String): String {
        nextId += 1
        return "$prefix-$nextId"
    }

    private fun conversationTitle(prompt: String): String = prompt.take(18).ifBlank { "新会话" }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        fun defaultAssistantResponsePlanner(): AssistantResponsePlanner =
            AssistantResponsePlanner { request ->
                listOf(
                    AssistantResponseStep(markdownDelta = "已收到：${request.prompt}\n\n"),
                    AssistantResponseStep(
                        markdownDelta =
                            buildString {
                                append("```kotlin\n")
                                append("println(\"")
                                append(request.prompt.take(24).replace("\"", "\\\""))
                                append("\")\n")
                                append("```")
                            },
                    ),
                )
            }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ChatRepositoryModule {
    @Provides
    @Singleton
    fun provideInMemoryChatRepository(): InMemoryChatRepository = InMemoryChatRepository()

    @Provides
    fun provideChatRepository(repository: InMemoryChatRepository): ChatRepository = repository

    @Provides
    fun provideConversationRepository(repository: InMemoryChatRepository): ConversationRepository = repository
}
