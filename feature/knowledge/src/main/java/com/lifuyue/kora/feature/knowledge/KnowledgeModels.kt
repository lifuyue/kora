package com.lifuyue.kora.feature.knowledge

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus

enum class KnowledgeLoadState {
    Loading,
    Content,
    Empty,
    Error,
}

@Immutable
data class DatasetListItemUiModel(
    val datasetId: String,
    val name: String,
    val intro: String,
    val type: String,
    val status: String,
    val vectorModel: String,
    val updateTime: Long,
)

@Immutable
data class CollectionListItemUiModel(
    val collectionId: String,
    val datasetId: String,
    val name: String,
    val type: String,
    val trainingType: String,
    val status: String,
    val sourceName: String,
    val updateTime: Long,
)

@Immutable
data class ImportTaskUiModel(
    val taskId: String,
    val displayName: String,
    val sourceType: String,
    val status: String,
    val progress: Int,
    val errorMessage: String? = null,
)

@Immutable
data class ChunkItemUiModel(
    val dataId: String,
    val chunkIndex: Int,
    val question: String,
    val answer: String,
    val status: String,
    val isDisabled: Boolean = false,
    val isRebuilding: Boolean = false,
)

@Immutable
data class SearchResultUiModel(
    val datasetId: String? = null,
    val collectionId: String? = null,
    val dataId: String?,
    val sourceName: String = "",
    val question: String = "",
    val answer: String = "",
    val scoreType: String? = null,
    val score: Double? = null,
)

data class KnowledgeOverviewUiState(
    val connectionType: ConnectionType = ConnectionType.FAST_GPT,
    val selectedAppId: String? = null,
    val datasetCount: Int = 0,
    val recentDatasets: List<DatasetListItemUiModel> = emptyList(),
    val status: KnowledgeLoadState = KnowledgeLoadState.Loading,
    val errorMessage: String? = null,
)

@Immutable
data class LocalKnowledgeDocumentUiModel(
    val documentId: String,
    val title: String,
    val sourceLabel: String,
    val previewText: String,
    val chunkCount: Int,
    val isEnabled: Boolean,
    val indexStatus: LocalKnowledgeIndexStatus,
    val indexErrorMessage: String? = null,
    val updatedAt: Long,
)

data class LocalKnowledgeLibraryUiState(
    val query: String = "",
    val draftTitle: String = "",
    val draftSource: String = "",
    val draftText: String = "",
    val items: List<LocalKnowledgeDocumentUiModel> = emptyList(),
    val status: KnowledgeLoadState = KnowledgeLoadState.Loading,
    val errorMessage: String? = null,
)

data class LocalKnowledgeDocumentUiState(
    val documentId: String = "",
    val title: String = "",
    val sourceLabel: String = "",
    val chunks: List<ChunkItemUiModel> = emptyList(),
    val isEnabled: Boolean = true,
    val errorMessage: String? = null,
)

data class DatasetBrowserUiState(
    val query: String = "",
    val createName: String = "",
    val items: List<DatasetListItemUiModel> = emptyList(),
    val availableTypes: List<String> = emptyList(),
    val availableStatuses: List<String> = emptyList(),
    val selectedTypeFilter: String? = null,
    val selectedStatusFilter: String? = null,
    val isRefreshing: Boolean = false,
    val status: KnowledgeLoadState = KnowledgeLoadState.Loading,
    val errorMessage: String? = null,
)

enum class CollectionCreateMode {
    FILE,
    LINK,
    TEXT,
    QA,
}

data class CollectionManagementUiState(
    val datasetId: String,
    val datasetName: String = "",
    val items: List<CollectionListItemUiModel> = emptyList(),
    val tasks: List<ImportTaskUiModel> = emptyList(),
    val createMode: CollectionCreateMode = CollectionCreateMode.FILE,
    val textDraftName: String = "",
    val textDraftValue: String = "",
    val linkDraftValue: String = "",
    val linkSelector: String = "",
    val selectedDocumentName: String = "",
    val selectedDocumentUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val status: KnowledgeLoadState = KnowledgeLoadState.Loading,
    val errorMessage: String? = null,
)

data class ChunkViewerUiState(
    val datasetId: String,
    val collectionId: String,
    val items: List<ChunkItemUiModel> = emptyList(),
    val highlightedDataId: String? = null,
    val editingChunkId: String? = null,
    val editingQuestion: String = "",
    val editingAnswer: String = "",
    val editingDisabled: Boolean = false,
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = false,
    val nextOffset: Int = 0,
    val errorMessage: String? = null,
)

data class SearchTestUiState(
    val datasetId: String,
    val query: String = "",
    val searchMode: String = "mixedRecall",
    val similarity: String = "0.7",
    val embeddingWeight: String = "0.5",
    val useReRank: Boolean = false,
    val results: List<SearchResultUiModel> = emptyList(),
    val duration: String = "",
    val extensionInfo: String = "",
    val status: KnowledgeLoadState = KnowledgeLoadState.Content,
    val errorMessage: String? = null,
    val isSearching: Boolean = false,
)
