package com.lifuyue.kora.feature.chat

import androidx.compose.runtime.Immutable
import com.lifuyue.kora.core.common.ChatRole

enum class MessageDeliveryState {
    Sent,
    Streaming,
    Failed,
    Stopped,
}

enum class MessageFeedback {
    None,
    Upvote,
    Downvote,
}

sealed interface AssistantBlock {
    data class Markdown(val markdown: String) : AssistantBlock

    data class CodeFence(
        val language: String,
        val code: String,
    ) : AssistantBlock
}

@Immutable
data class ChatMessageUiModel(
    val messageId: String,
    val chatId: String,
    val appId: String,
    val role: ChatRole,
    val markdown: String,
    val reasoning: String = "",
    val isStreaming: Boolean = false,
    val deliveryState: MessageDeliveryState = if (isStreaming) MessageDeliveryState.Streaming else MessageDeliveryState.Sent,
    val errorMessage: String? = null,
    val feedback: MessageFeedback = MessageFeedback.None,
) {
    val blocks: List<AssistantBlock>
        get() = parseAssistantBlocks(markdown)
}

@Immutable
data class ConversationListItemUiModel(
    val chatId: String,
    val appId: String,
    val title: String,
    val preview: String,
    val isPinned: Boolean = false,
    val updateTime: Long = 0L,
)

@Immutable
data class ConversationListUiState(
    val query: String = "",
    val items: List<ConversationListItemUiModel> = emptyList(),
    val isRefreshing: Boolean = false,
) {
    val isEmpty: Boolean
        get() = items.isEmpty()
}

@Immutable
data class ChatUiState(
    val appId: String,
    val chatId: String? = null,
    val input: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<ChatMessageUiModel> = emptyList(),
) {
    val canStopGeneration: Boolean
        get() = messages.any { it.isStreaming }
}
