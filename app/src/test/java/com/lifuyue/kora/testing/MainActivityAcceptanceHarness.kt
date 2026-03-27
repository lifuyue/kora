package com.lifuyue.kora.testing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.feature.chat.ChatMessageUiModel
import com.lifuyue.kora.feature.chat.ChatRepository
import com.lifuyue.kora.feature.chat.ChatScreen
import com.lifuyue.kora.feature.chat.ChatTestTags
import com.lifuyue.kora.feature.chat.ChatUiState
import com.lifuyue.kora.feature.chat.ConversationListItemUiModel
import com.lifuyue.kora.feature.chat.ConversationListScreen
import com.lifuyue.kora.feature.chat.ConversationListUiState
import com.lifuyue.kora.feature.chat.ConversationRepository
import com.lifuyue.kora.feature.chat.MessageDeliveryState
import com.lifuyue.kora.feature.chat.MessageFeedback
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.junit.rules.ExternalResource

class AcceptanceAppHarnessRule(
    initialSnapshot: ConnectionSnapshot,
    private val connectionRouteOverride: KoraTestOverrides.ConnectionRouteOverride? = null,
    private val shellRouteOverride: KoraTestOverrides.ShellRouteOverride? = null,
) : ExternalResource() {
    val snapshot = MutableStateFlow(initialSnapshot)

    override fun before() {
        KoraTestOverrides.snapshotOverride = snapshot
        KoraTestOverrides.connectionRouteOverride = connectionRouteOverride
        KoraTestOverrides.shellRouteOverride = shellRouteOverride
    }

    override fun after() {
        KoraTestOverrides.reset()
    }
}

class AcceptanceConnectionRouteOverride : KoraTestOverrides.ConnectionRouteOverride {
    @Composable
    override fun Render(onConnectionSaved: () -> Unit) {
        var tested by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("连接配置")
            OutlinedTextField(
                value = "https://api.fastgpt.in/api",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("acceptance-server-url"),
                label = { Text("Server URL") },
            )
            OutlinedTextField(
                value = "fastgpt-secret",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("acceptance-api-key"),
                label = { Text("API Key") },
            )
            Button(onClick = { tested = true }) {
                Text("测试连接")
            }
            if (tested) {
                Text("测试连接成功")
            }
            Button(
                onClick = {
                    (checkNotNull(KoraTestOverrides.snapshotOverride) as MutableStateFlow<ConnectionSnapshot>).value =
                        ConnectionSnapshot(
                            serverBaseUrl = "https://api.fastgpt.in/",
                            apiKey = "fastgpt-secret",
                            selectedAppId = "app-1",
                            onboardingCompleted = true,
                        )
                    onConnectionSaved()
                },
                enabled = tested,
            ) {
                Text("保存")
            }
        }
    }
}

class AcceptanceChatRouteOverride : KoraTestOverrides.ShellRouteOverride {
    @Composable
    override fun Render(snapshot: ConnectionSnapshot) {
        val appId = checkNotNull(snapshot.selectedAppId)
        AcceptanceChatShell(appId = appId)
    }
}

@Composable
private fun AcceptanceChatShell(appId: String) {
    val repository = remember { AcceptanceChatRepository() }
    val scope = rememberCoroutineScope()
    var currentChatId by remember { mutableStateOf<String?>(null) }
    var showingChat by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val conversations by repository.observeConversations(appId).collectAsState(initial = emptyList())
    val messages by repository.observeMessages(appId, currentChatId).collectAsState(initial = emptyList())

    if (showingChat) {
        ChatScreen(
            uiState =
                ChatUiState(
                    appId = appId,
                    chatId = currentChatId,
                    input = input,
                    messages = messages,
                ),
            onBack = { showingChat = false },
            onInputChanged = { input = it },
            onSend = {
                scope.launch {
                    currentChatId = repository.sendMessage(appId = appId, chatId = currentChatId, text = input)
                    input = ""
                }
            },
            onStopGenerating = {
                val resolvedChatId = currentChatId ?: return@ChatScreen
                scope.launch { repository.stopStreaming(appId, resolvedChatId) }
            },
            onContinueGeneration = {
                val resolvedChatId = currentChatId ?: return@ChatScreen
                scope.launch { currentChatId = repository.continueGeneration(appId, resolvedChatId) }
            },
            onFeedback = { message, feedback ->
                scope.launch { repository.setFeedback(appId, message.chatId, message.messageId, feedback) }
            },
            onRegenerate = { message ->
                val resolvedChatId = currentChatId ?: return@ChatScreen
                scope.launch { repository.regenerateResponse(appId, resolvedChatId, message.messageId) }
            },
        )
    } else {
        ConversationListScreen(
            uiState = ConversationListUiState(items = conversations),
            onQueryChanged = {},
            onOpenConversation = { chatId ->
                currentChatId = chatId
                showingChat = true
            },
            onNewConversation = {
                currentChatId = null
                showingChat = true
            },
            onDeleteConversation = { chatId ->
                scope.launch { repository.deleteConversation(appId, chatId) }
            },
            onRenameConversation = { chatId, title ->
                scope.launch { repository.renameConversation(appId, chatId, title) }
            },
            onTogglePin = { chatId, pinned ->
                scope.launch { repository.togglePinConversation(appId, chatId, pinned) }
            },
            onClearConversations = {
                scope.launch { repository.clearConversations(appId) }
            },
        )
    }
}

private class AcceptanceChatRepository :
    ChatRepository,
    ConversationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ids = AtomicLong()
    private val conversations = MutableStateFlow<Map<String, List<ConversationListItemUiModel>>>(emptyMap())
    private val messages = MutableStateFlow<Map<String, List<ChatMessageUiModel>>>(emptyMap())
    private val attempts = linkedMapOf<String, Int>()

    override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> =
        conversations.map { state -> state[appId].orEmpty() }

    override suspend fun refreshConversations(appId: String) = Unit

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
    }

    override suspend fun clearConversations(appId: String) {
        val ids = conversations.value[appId].orEmpty().map { it.chatId }
        conversations.value = conversations.value.toMutableMap().apply { put(appId, emptyList()) }
        messages.value =
            messages.value.toMutableMap().apply {
                ids.forEach(::remove)
            }
    }

    override fun observeMessages(appId: String, chatId: String?) =
        messages.map { state ->
            if (chatId == null) {
                emptyList()
            } else {
                state[chatId].orEmpty().filter { it.appId == appId }
            }
        }

    override suspend fun restoreMessages(appId: String, chatId: String) = Unit

    override suspend fun sendMessage(appId: String, chatId: String?, text: String): String {
        val prompt = text.trim()
        val resolvedChatId = chatId ?: nextId("chat")
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
        scope.launch {
            delay(120)
            val firstChunk =
                when {
                    continuation -> "继续生成：$prompt\n\n"
                    regenerated -> "第 $attempt 次生成\n\n"
                    else -> "已收到：$prompt\n\n"
                }
            updateAssistant(chatId, assistantMessageId, firstChunk, true, MessageDeliveryState.Streaming, null)

            delay(120)
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

    private fun nextId(prefix: String): String = "$prefix-${ids.incrementAndGet()}"
}
