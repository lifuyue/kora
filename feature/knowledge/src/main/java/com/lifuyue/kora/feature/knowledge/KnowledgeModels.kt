package com.lifuyue.kora.feature.knowledge

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class DatasetListItemUiModel(
    val datasetId: String,
    val name: String,
    val intro: String,
    val type: String,
    val vectorModel: String,
    val updateTimeLabel: String,
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
    val updateTimeLabel: String,
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
)

@Immutable
data class SearchResultUiModel(
    val dataId: String?,
    val title: String,
    val snippet: String,
    val scoreLabel: String,
)

data class KnowledgeOverviewUiState(
    val selectedAppId: String? = null,
    val datasetCount: Int = 0,
    val recentDatasets: List<DatasetListItemUiModel> = emptyList(),
)

data class DatasetBrowserUiState(
    val query: String = "",
    val createName: String = "",
    val items: List<DatasetListItemUiModel> = emptyList(),
    val isRefreshing: Boolean = false,
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
    val errorMessage: String? = null,
)

data class ChunkViewerUiState(
    val datasetId: String,
    val collectionId: String,
    val items: List<ChunkItemUiModel> = emptyList(),
    val editingChunkId: String? = null,
    val editingQuestion: String = "",
    val editingAnswer: String = "",
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
    val errorMessage: String? = null,
    val isSearching: Boolean = false,
)
