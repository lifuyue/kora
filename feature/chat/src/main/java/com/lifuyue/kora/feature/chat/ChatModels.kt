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
data class CitationItemUiModel(
    val datasetId: String? = null,
    val collectionId: String? = null,
    val dataId: String? = null,
    val title: String,
    val snippet: String,
    val scoreLabel: String = "",
)

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
    val citations: List<CitationItemUiModel> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
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
    val folderId: String? = null,
    val folderName: String? = null,
    val tags: List<ConversationTagUiModel> = emptyList(),
    val isPinned: Boolean = false,
    val updateTime: Long = 0L,
)

@Immutable
data class ConversationFolderUiModel(
    val folderId: String,
    val name: String,
)

@Immutable
data class ConversationTagUiModel(
    val tagId: String,
    val name: String,
    val colorToken: String,
)

@Immutable
data class ConversationListUiState(
    val query: String = "",
    val items: List<ConversationListItemUiModel> = emptyList(),
    val folders: List<ConversationFolderUiModel> = emptyList(),
    val tags: List<ConversationTagUiModel> = emptyList(),
    val selectedFolderId: String? = null,
    val selectedTagId: String? = null,
    val isRefreshing: Boolean = false,
) {
    val isEmpty: Boolean
        get() = items.isEmpty()

    val pinnedItems: List<ConversationListItemUiModel>
        get() = items.filter { it.isPinned }

    val otherItems: List<ConversationListItemUiModel>
        get() = items.filterNot { it.isPinned }

    val canClear: Boolean
        get() = items.isNotEmpty()

    val selectedFolderName: String
        get() = folders.firstOrNull { it.folderId == selectedFolderId }?.name ?: "全部文件夹"

    val selectedTagName: String
        get() = tags.firstOrNull { it.tagId == selectedTagId }?.name ?: "全部标签"
}

@Immutable
data class ChatUiState(
    val appId: String,
    val chatId: String? = null,
    val welcomeText: String? = null,
    val input: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<ChatMessageUiModel> = emptyList(),
) {
    val canStopGeneration: Boolean
        get() = messages.any { it.isStreaming }
}

@Immutable
data class AppSelectorItemUiModel(
    val appId: String,
    val name: String,
    val intro: String,
)

data class AppSelectorUiState(
    val currentAppId: String? = null,
    val currentAppName: String = "",
    val items: List<AppSelectorItemUiModel> = emptyList(),
    val errorMessage: String? = null,
)
