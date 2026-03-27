package com.lifuyue.kora.testing

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.feature.chat.ChatMessageUiModel
import com.lifuyue.kora.feature.chat.ChatRepository
import com.lifuyue.kora.feature.chat.ChatTestOverrides
import com.lifuyue.kora.feature.chat.ConversationFolderUiModel
import com.lifuyue.kora.feature.chat.ConversationListItemUiModel
import com.lifuyue.kora.feature.chat.ConversationRepository
import com.lifuyue.kora.feature.chat.ConversationTagUiModel
import com.lifuyue.kora.feature.chat.MessageDeliveryState
import com.lifuyue.kora.feature.chat.MessageFeedback
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.rules.ExternalResource

class AcceptanceAppHarnessRule(
    private val initialSnapshot: ConnectionSnapshot? = null,
    private val chatRepositoryOverride: ChatRepository? = null,
    private val conversationRepositoryOverride: ConversationRepository? = chatRepositoryOverride as? ConversationRepository,
) : ExternalResource() {
    override fun before() {
        clearAppState()
        KoraTestOverrides.reset()
        ChatTestOverrides.reset()
        (chatRepositoryOverride as? AcceptanceChatRepository)?.reset()
        KoraTestOverrides.snapshotOverride = initialSnapshot?.let(::MutableStateFlow)
        ChatTestOverrides.chatRepository = chatRepositoryOverride
        ChatTestOverrides.conversationRepository = conversationRepositoryOverride
    }

    override fun after() {
        (chatRepositoryOverride as? AcceptanceChatRepository)?.reset()
        KoraTestOverrides.reset()
        ChatTestOverrides.reset()
        clearAppState()
    }

    private fun clearAppState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("kora.db")
        context.deleteSharedPreferences("kora_secure_connection")
        context.deleteSharedPreferences("kora_secure_connection_fallback")
        context.preferencesDataStoreFile("connection.preferences_pb").delete()
    }
}

class AcceptanceChatRepository :
    ChatRepository,
    ConversationRepository {
    data class AcceptanceProbe(
        val chatId: String,
        val lastAssistantId: String? = null,
        val lastAssistantState: MessageDeliveryState? = null,
        val lastAssistantMarkdown: String = "",
        val lastErrorMessage: String? = null,
        val assistantCompleted: Boolean = false,
        val errorVisible: Boolean = false,
        val regenerated: Boolean = false,
    )

    private val ids = AtomicLong()
    private val refreshCalls = AtomicLong()
    private val sendCalls = AtomicLong()
    private val conversations = MutableStateFlow<Map<String, List<ConversationListItemUiModel>>>(emptyMap())
    private val folders = MutableStateFlow<Map<String, List<ConversationFolderUiModel>>>(emptyMap())
    private val tags = MutableStateFlow<Map<String, List<ConversationTagUiModel>>>(emptyMap())
    private val messages = MutableStateFlow<Map<String, List<ChatMessageUiModel>>>(emptyMap())
    private val attempts = linkedMapOf<String, Int>()
    private val probes = MutableStateFlow<Map<String, AcceptanceProbe>>(emptyMap())

    fun reset() {
        ids.set(0L)
        refreshCalls.set(0L)
        sendCalls.set(0L)
        conversations.value = emptyMap()
        folders.value = emptyMap()
        tags.value = emptyMap()
        messages.value = emptyMap()
        attempts.clear()
        probes.value = emptyMap()
    }

    override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> =
        conversations.map { state -> state[appId].orEmpty() }

    override fun observeFolders(appId: String): Flow<List<ConversationFolderUiModel>> =
        folders.map { state -> state[appId].orEmpty() }

    override fun observeTags(appId: String): Flow<List<ConversationTagUiModel>> =
        tags.map { state -> state[appId].orEmpty() }

    fun hasRefreshed(): Boolean = refreshCalls.get() > 0

    fun hasSentMessage(): Boolean = sendCalls.get() > 0

    fun hasConversation(appId: String): Boolean = conversations.value[appId].orEmpty().isNotEmpty()

    fun latestConversationId(appId: String): String? = conversations.value[appId].orEmpty().lastOrNull()?.chatId

    fun containsMessage(appId: String, text: String): Boolean =
        messages.value.values
            .flatten()
            .any { message ->
                message.appId == appId && message.markdown.contains(text)
            }

    fun hasAssistantFinished(appId: String, text: String): Boolean =
        messages.value.values
            .flatten()
            .any { message ->
                message.appId == appId &&
                    message.role == ChatRole.AI &&
                    message.deliveryState == MessageDeliveryState.Sent &&
                    message.markdown.contains(text)
            }

    fun hasStreamingError(appId: String, text: String): Boolean =
        messages.value.values
            .flatten()
            .any { message ->
                message.appId == appId &&
                    message.role == ChatRole.AI &&
                    message.deliveryState == MessageDeliveryState.Failed &&
                    message.markdown.contains(text)
            }

    fun hasRegenerated(chatId: String): Boolean =
        probes.value[chatId]?.regenerated == true

    fun probe(chatId: String): AcceptanceProbe? = probes.value[chatId]

    override suspend fun refreshConversations(appId: String) {
        refreshCalls.incrementAndGet()
    }

    override suspend fun renameConversation(appId: String, chatId: String, title: String) {
        updateConversation(appId, chatId) { it.copy(title = title) }
    }

    override suspend fun togglePinConversation(appId: String, chatId: String, pinned: Boolean) {
        updateConversation(appId, chatId) { it.copy(isPinned = pinned) }
    }

    override suspend fun deleteConversation(appId: String, chatId: String) {
        conversations.value =
            conversations.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty().filterNot { it.chatId == chatId })
            }
        messages.value = messages.value.toMutableMap().apply { remove(chatId) }
        probes.value = probes.value.toMutableMap().apply { remove(chatId) }
    }

    override suspend fun clearConversations(appId: String) {
        val ids = conversations.value[appId].orEmpty().map { it.chatId }
        conversations.value = conversations.value.toMutableMap().apply { put(appId, emptyList()) }
        messages.value =
            messages.value.toMutableMap().apply {
                ids.forEach(::remove)
            }
        probes.value =
            probes.value.toMutableMap().apply {
                ids.forEach(::remove)
            }
    }

    override suspend fun createFolder(appId: String, name: String) {
        val folder = ConversationFolderUiModel(folderId = nextId("folder"), name = name)
        folders.value =
            folders.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty() + folder)
            }
    }

    override suspend fun renameFolder(appId: String, folderId: String, name: String) {
        folders.value =
            folders.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty().map { if (it.folderId == folderId) it.copy(name = name) else it })
            }
    }

    override suspend fun deleteFolder(appId: String, folderId: String) {
        folders.value =
            folders.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty().filterNot { it.folderId == folderId })
            }
        conversations.value =
            conversations.value.toMutableMap().apply {
                put(
                    appId,
                    get(appId).orEmpty().map { conversation ->
                        if (conversation.folderId == folderId) {
                            conversation.copy(folderId = null, folderName = null)
                        } else {
                            conversation
                        }
                    },
                )
            }
    }

    override suspend fun createTag(appId: String, name: String) {
        val tag = ConversationTagUiModel(tagId = nextId("tag"), name = name, colorToken = "sky")
        tags.value =
            tags.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty() + tag)
            }
    }

    override suspend fun renameTag(appId: String, tagId: String, name: String) {
        tags.value =
            tags.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty().map { if (it.tagId == tagId) it.copy(name = name) else it })
            }
        conversations.value =
            conversations.value.toMutableMap().apply {
                put(
                    appId,
                    get(appId).orEmpty().map { conversation ->
                        conversation.copy(
                            tags = conversation.tags.map { if (it.tagId == tagId) it.copy(name = name) else it },
                        )
                    },
                )
            }
    }

    override suspend fun deleteTag(appId: String, tagId: String) {
        tags.value =
            tags.value.toMutableMap().apply {
                put(appId, get(appId).orEmpty().filterNot { it.tagId == tagId })
            }
        conversations.value =
            conversations.value.toMutableMap().apply {
                put(
                    appId,
                    get(appId).orEmpty().map { conversation ->
                        conversation.copy(tags = conversation.tags.filterNot { it.tagId == tagId })
                    },
                )
            }
    }

    override suspend fun moveConversationToFolder(appId: String, chatId: String, folderId: String?) {
        val folder = folders.value[appId].orEmpty().firstOrNull { it.folderId == folderId }
        updateConversation(appId, chatId) {
            it.copy(folderId = folder?.folderId, folderName = folder?.name)
        }
    }

    override suspend fun setConversationTags(appId: String, chatId: String, tagIds: List<String>) {
        val selectedTags = tags.value[appId].orEmpty().filter { it.tagId in tagIds }
        updateConversation(appId, chatId) { it.copy(tags = selectedTags) }
    }

    override fun observeMessages(appId: String, chatId: String?) =
        messages.map { state ->
            if (chatId == null) {
                emptyList()
            } else {
                state[chatId].orEmpty().filter { it.appId == appId }
            }
        }

    override suspend fun bootstrapChat(appId: String): com.lifuyue.kora.feature.chat.ChatBootstrap =
        com.lifuyue.kora.feature.chat.ChatBootstrap(
            chatId = nextId("chat"),
            welcomeText = "欢迎使用 $appId",
        )

    override suspend fun restoreMessages(appId: String, chatId: String) = Unit

    override suspend fun sendMessage(appId: String, chatId: String?, text: String): String {
        sendCalls.incrementAndGet()
        val prompt = text.trim()
        val resolvedChatId = chatId?.takeIf { it.isNotBlank() } ?: nextId("chat")
        val humanMessage =
            ChatMessageUiModel(
                messageId = nextId("user"),
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

        upsertConversation(appId, resolvedChatId, prompt, prompt)
        appendMessages(resolvedChatId, listOf(humanMessage, assistantMessage))
        val attempt = (attempts[resolvedChatId] ?: 0) + 1
        attempts[resolvedChatId] = attempt
        updateProbe(resolvedChatId) { it.copy(regenerated = false) }
        startStreaming(appId, resolvedChatId, assistantMessage.messageId, prompt, attempt)
        return resolvedChatId
    }

    override suspend fun stopStreaming(appId: String, chatId: String) {
        updateLatestAssistant(chatId) {
            it.copy(
                isStreaming = false,
                deliveryState = MessageDeliveryState.Stopped,
                errorMessage = "已停止生成",
            )
        }
    }

    override suspend fun continueGeneration(appId: String, chatId: String): String {
        val prompt = messages.value[chatId].orEmpty().lastOrNull { it.role == ChatRole.Human }?.markdown.orEmpty()
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
        val attempt = (attempts[chatId] ?: 0) + 1
        attempts[chatId] = attempt
        startStreaming(appId, chatId, assistantMessage.messageId, prompt, attempt, continuation = true)
        return chatId
    }

    override suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String {
        val prompt = messages.value[chatId].orEmpty().lastOrNull { it.role == ChatRole.Human }?.markdown.orEmpty()
        messages.value =
            messages.value.toMutableMap().apply {
                put(chatId, get(chatId).orEmpty().filterNot { it.messageId == messageId })
            }
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
        val attempt = (attempts[chatId] ?: 0) + 1
        attempts[chatId] = attempt
        updateProbe(chatId) { it.copy(regenerated = true) }
        startStreaming(appId, chatId, assistantMessage.messageId, prompt, attempt, regenerated = true)
        return chatId
    }

    override suspend fun setFeedback(appId: String, chatId: String, messageId: String, feedback: MessageFeedback) {
        messages.value =
            messages.value.toMutableMap().apply {
                put(
                    chatId,
                    get(chatId).orEmpty().map { message ->
                        if (message.messageId == messageId) {
                            message.copy(feedback = feedback)
                        } else {
                            message
                        }
                    },
                )
            }
    }

    private fun startStreaming(
        appId: String,
        chatId: String,
        assistantMessageId: String,
        prompt: String,
        attempt: Int,
        continuation: Boolean = false,
        regenerated: Boolean = false,
    ) {
        val firstChunk =
            when {
                continuation -> "继续生成：$prompt\n\n"
                regenerated -> "第 $attempt 次生成\n\n"
                else -> "已收到：$prompt\n\n"
            }
        updateAssistant(chatId, assistantMessageId, firstChunk, true, MessageDeliveryState.Streaming, null)

        if (prompt.contains("错误")) {
            updateAssistant(
                chatId = chatId,
                messageId = assistantMessageId,
                markdown = "模拟网络错误\n\n已收到：$prompt\n\n网络请求中断。",
                isStreaming = false,
                deliveryState = MessageDeliveryState.Failed,
                errorMessage = "模拟网络错误",
            )
            updateConversation(appId, chatId) { it.copy(preview = "模拟网络错误") }
        } else {
            val finalMarkdown =
                buildString {
                    append(firstChunk)
                    append("```kotlin\n")
                    append("println(\"")
                    append(prompt)
                    append("\")\n")
                    append("```")
                }
            updateAssistant(
                chatId = chatId,
                messageId = assistantMessageId,
                markdown = finalMarkdown,
                isStreaming = false,
                deliveryState = MessageDeliveryState.Sent,
                errorMessage = null,
            )
            updateConversation(appId, chatId) { it.copy(preview = prompt) }
        }
    }

    private fun appendMessages(chatId: String, newMessages: List<ChatMessageUiModel>) {
        messages.value =
            messages.value.toMutableMap().apply {
                put(chatId, get(chatId).orEmpty() + newMessages)
            }
    }

    private fun updateAssistant(
        chatId: String,
        messageId: String,
        markdown: String,
        isStreaming: Boolean,
        deliveryState: MessageDeliveryState,
        errorMessage: String?,
    ) {
        messages.value =
            messages.value.toMutableMap().apply {
                put(
                    chatId,
                    get(chatId).orEmpty().map { message ->
                        if (message.messageId == messageId) {
                            message.copy(
                                markdown = markdown,
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
        updateProbe(chatId) {
            it.copy(
                lastAssistantId = messageId,
                lastAssistantState = deliveryState,
                lastAssistantMarkdown = markdown,
                lastErrorMessage = errorMessage,
                assistantCompleted = deliveryState == MessageDeliveryState.Sent && !isStreaming,
                errorVisible = errorMessage != null || deliveryState == MessageDeliveryState.Failed,
            )
        }
    }

    private fun updateLatestAssistant(
        chatId: String,
        transform: (ChatMessageUiModel) -> ChatMessageUiModel,
    ) {
        val current = messages.value[chatId].orEmpty()
        val targetId = current.lastOrNull { it.role == ChatRole.AI }?.messageId ?: return
        messages.value =
            messages.value.toMutableMap().apply {
                put(
                    chatId,
                    current.map { message ->
                        if (message.messageId == targetId) transform(message) else message
                    },
                )
            }
    }

    private fun upsertConversation(appId: String, chatId: String, title: String, preview: String) {
        val current = conversations.value[appId].orEmpty()
        val updated =
            current.filterNot { it.chatId == chatId } +
                ConversationListItemUiModel(
                    chatId = chatId,
                    appId = appId,
                    title = title.take(18),
                    preview = preview,
                    updateTime = System.currentTimeMillis(),
                )
        conversations.value = conversations.value.toMutableMap().apply { put(appId, updated) }
    }

    private fun updateConversation(
        appId: String,
        chatId: String,
        transform: (ConversationListItemUiModel) -> ConversationListItemUiModel,
    ) {
        conversations.value =
            conversations.value.toMutableMap().apply {
                put(
                    appId,
                    get(appId).orEmpty().map { item ->
                        if (item.chatId == chatId) transform(item) else item
                    },
                )
            }
    }

    private fun updateProbe(
        chatId: String,
        transform: (AcceptanceProbe) -> AcceptanceProbe,
    ) {
        probes.value =
            probes.value.toMutableMap().apply {
                val current = get(chatId) ?: AcceptanceProbe(chatId = chatId)
                put(chatId, transform(current))
            }
    }

    private fun nextId(prefix: String): String = "$prefix-${ids.incrementAndGet()}"
}
