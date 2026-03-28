package com.lifuyue.kora.feature.chat

import androidx.compose.runtime.Immutable
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.network.UploadedAssetRef

enum class AttachmentKind {
    Image,
    File,
}

enum class AttachmentUploadStatus {
    Idle,
    Uploading,
    Uploaded,
    Failed,
    Cancelled,
}

enum class InteractiveCardKind {
    UserSelect,
    UserInput,
    CollectionForm,
}

enum class InteractiveCardStatus {
    Pending,
    Submitting,
    Resolved,
    Expired,
}

enum class SpeechInputStatus {
    Idle,
    Recording,
    Recognizing,
    Error,
}

enum class TtsPlaybackStatus {
    Idle,
    Playing,
    Paused,
    Stopped,
    Error,
}

enum class ConversationExportFormat {
    Txt,
    Json,
    Pdf,
}

@Immutable
data class AttachmentDraftUiModel(
    val displayName: String,
    val localUri: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val kind: AttachmentKind,
    val uploadStatus: AttachmentUploadStatus = AttachmentUploadStatus.Idle,
    val uploadedRef: UploadedAssetRef? = null,
    val progress: Float = 0f,
    val errorMessage: String? = null,
)

@Immutable
data class ChatAttachmentConfig(
    val maxFiles: Int = 10,
    val canSelectFile: Boolean = false,
    val canSelectImg: Boolean = false,
    val canSelectVideo: Boolean = false,
    val canSelectAudio: Boolean = false,
    val canSelectCustomFileExtension: Boolean = false,
    val customFileExtensionList: List<String> = emptyList(),
) {
    val hasAnySelectionType: Boolean
        get() =
            canSelectFile ||
                canSelectImg ||
                canSelectVideo ||
                canSelectAudio ||
                canSelectCustomFileExtension
}

@Immutable
data class InteractiveFieldUiModel(
    val id: String,
    val label: String,
    val value: String = "",
    val required: Boolean = true,
)

@Immutable
data class InteractiveCardUiModel(
    val kind: InteractiveCardKind,
    val messageDataId: String,
    val responseValueId: String? = null,
    val status: InteractiveCardStatus = InteractiveCardStatus.Pending,
    val fields: List<InteractiveFieldUiModel> = emptyList(),
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
)

@Immutable
data class SpeechInputUiState(
    val status: SpeechInputStatus = SpeechInputStatus.Idle,
    val transcript: String = "",
    val errorMessage: String? = null,
)

@Immutable
data class TtsPlaybackUiState(
    val messageId: String? = null,
    val status: TtsPlaybackStatus = TtsPlaybackStatus.Idle,
    val progress: Float = 0f,
    val errorMessage: String? = null,
)

@Immutable
data class MessageRangeSelection(
    val startMessageId: String,
    val endMessageId: String,
)

@Immutable
data class ShareExportUiState(
    val selection: MessageRangeSelection? = null,
    val exportFormat: ConversationExportFormat = ConversationExportFormat.Txt,
    val previewText: String = "",
)

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
    val sourceName: String = "",
    val snippet: String,
    val scoreType: String? = null,
    val score: Double? = null,
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
    val interactiveCard: InteractiveCardUiModel? = null,
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
    val isArchived: Boolean = false,
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
    val showArchived: Boolean = false,
) {
    val isEmpty: Boolean
        get() = items.isEmpty()

    val pinnedItems: List<ConversationListItemUiModel>
        get() = items.filter { it.isPinned }

    val otherItems: List<ConversationListItemUiModel>
        get() = items.filterNot { it.isPinned }

    val canClear: Boolean
        get() = items.isNotEmpty()

    val selectedFolderName: String?
        get() = folders.firstOrNull { it.folderId == selectedFolderId }?.name

    val selectedTagName: String?
        get() = tags.firstOrNull { it.tagId == selectedTagId }?.name
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
    val autoScrollEnabled: Boolean = true,
    val isInitialLoading: Boolean = false,
    val attachments: List<AttachmentDraftUiModel> = emptyList(),
    val attachmentConfig: ChatAttachmentConfig = ChatAttachmentConfig(),
    val pendingInteractiveCard: InteractiveCardUiModel? = null,
    val speechInputState: SpeechInputUiState = SpeechInputUiState(),
    val ttsPlaybackState: TtsPlaybackUiState = TtsPlaybackUiState(),
    val shareExportState: ShareExportUiState = ShareExportUiState(),
) {
    val canStopGeneration: Boolean
        get() = messages.any { it.isStreaming }

    val canSend: Boolean
        get() =
            !isSending &&
                !canStopGeneration &&
                speechInputState.status != SpeechInputStatus.Recording &&
                speechInputState.status != SpeechInputStatus.Recognizing &&
                (input.isNotBlank() || attachments.any { it.uploadStatus == AttachmentUploadStatus.Uploaded }) &&
                attachments.none { it.uploadStatus == AttachmentUploadStatus.Uploading || it.uploadStatus == AttachmentUploadStatus.Failed }
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

@Immutable
data class AppDetailSectionUiModel(
    val title: String,
    val items: List<String>,
)

data class AppDetailUiState(
    val appId: String,
    val appName: String = "",
    val intro: String = "",
    val type: String = "",
    val welcomeText: String? = null,
    val sections: List<AppDetailSectionUiModel> = emptyList(),
    val showAnalyticsEntry: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

enum class AnalyticsRange(val raw: String) {
    Last7Days("7d"),
    Last30Days("30d"),
    Last90Days("90d"),
}

enum class AnalyticsStatus {
    Loading,
    Success,
    Empty,
    Error,
}

data class AppAnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.Last7Days,
    val requestCount: Int = 0,
    val conversationCount: Int = 0,
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val status: AnalyticsStatus = AnalyticsStatus.Loading,
    val errorMessage: String? = null,
)
