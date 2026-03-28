package com.lifuyue.kora.feature.knowledge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object KnowledgeRoutes {
    const val OVERVIEW = "knowledge"
    const val DATASETS = "knowledge/datasets"
    const val COLLECTIONS = "knowledge/datasets/{datasetId}/collections"
    const val CHUNKS = "knowledge/datasets/{datasetId}/collections/{collectionId}?dataId={dataId}"
    const val SEARCH = "knowledge/datasets/{datasetId}/search-test"

    fun collections(datasetId: String): String = "knowledge/datasets/$datasetId/collections"

    fun chunks(
        datasetId: String,
        collectionId: String,
        dataId: String? = null,
    ): String =
        if (dataId.isNullOrBlank()) {
            "knowledge/datasets/$datasetId/collections/$collectionId?dataId="
        } else {
            "knowledge/datasets/$datasetId/collections/$collectionId?dataId=$dataId"
        }

    fun search(datasetId: String): String = "knowledge/datasets/$datasetId/search-test"
}

fun NavGraphBuilder.knowledgeGraph(navController: NavController) {
    composable(KnowledgeRoutes.OVERVIEW) {
        val viewModel: KnowledgeOverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        KnowledgeOverviewScreen(
            state = uiState,
            onOpenDatasets = { navController.navigate(KnowledgeRoutes.DATASETS) },
            onOpenRecentDataset = { navController.navigate(KnowledgeRoutes.collections(it)) },
        )
    }
    composable(KnowledgeRoutes.DATASETS) {
        val viewModel: DatasetBrowserViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DatasetBrowserScreen(
            state = uiState,
            onBack = { navController.popBackStack() },
            onQueryChanged = viewModel::updateQuery,
            onCreateNameChanged = viewModel::updateCreateName,
            onTypeFilterSelected = viewModel::selectTypeFilter,
            onStatusFilterSelected = viewModel::selectStatusFilter,
            onRefresh = viewModel::refresh,
            onCreateDataset = viewModel::createDataset,
            onDeleteDataset = viewModel::deleteDataset,
            onOpenDataset = { navController.navigate(KnowledgeRoutes.collections(it)) },
            onOpenSearch = { navController.navigate(KnowledgeRoutes.search(it)) },
        )
    }
    composable(
        route = KnowledgeRoutes.COLLECTIONS,
        arguments = listOf(navArgument("datasetId") { type = NavType.StringType }),
    ) {
        val viewModel: CollectionManagementViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val documentLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    viewModel.setSelectedDocument(uri, uri.lastPathSegment ?: "document")
                }
            }
        CollectionManagementScreen(
            state = uiState,
            onBack = { navController.popBackStack() },
            onRefresh = viewModel::refresh,
            onModeChanged = viewModel::switchMode,
            onTextNameChanged = viewModel::updateTextName,
            onTextValueChanged = viewModel::updateTextValue,
            onLinkValueChanged = viewModel::updateLinkValue,
            onLinkSelectorChanged = viewModel::updateLinkSelector,
            onPickDocument = { documentLauncher.launch(arrayOf("*/*")) },
            onSubmit = viewModel::submit,
            onOpenCollection = { collectionId -> navController.navigate(KnowledgeRoutes.chunks(viewModel.datasetId, collectionId)) },
            onOpenSearch = { navController.navigate(KnowledgeRoutes.search(viewModel.datasetId)) },
        )
    }
    composable(
        route = KnowledgeRoutes.CHUNKS,
        arguments =
            listOf(
                navArgument("datasetId") { type = NavType.StringType },
                navArgument("collectionId") { type = NavType.StringType },
                navArgument("dataId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        val viewModel: ChunkViewerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ChunkViewerScreen(
            state = uiState,
            onBack = { navController.popBackStack() },
            onStartEditing = viewModel::startEditing,
            onQuestionChanged = viewModel::updateEditingQuestion,
            onAnswerChanged = viewModel::updateEditingAnswer,
            onDisabledChanged = viewModel::updateEditingDisabled,
            onSave = viewModel::saveEditing,
            onDelete = viewModel::deleteChunk,
            onLoadMore = viewModel::loadMore,
        )
    }
    composable(
        route = KnowledgeRoutes.SEARCH,
        arguments = listOf(navArgument("datasetId") { type = NavType.StringType }),
    ) {
        val viewModel: SearchTestViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SearchTestScreen(
            state = uiState,
            onBack = { navController.popBackStack() },
            onQueryChanged = viewModel::updateQuery,
            onSearchModeChanged = viewModel::updateMode,
            onSimilarityChanged = viewModel::updateSimilarity,
            onEmbeddingWeightChanged = viewModel::updateEmbeddingWeight,
            onUseReRankChanged = viewModel::updateReRank,
            onSearch = viewModel::search,
            onOpenResult = { result ->
                if (!result.datasetId.isNullOrBlank() && !result.collectionId.isNullOrBlank()) {
                    navController.navigate(
                        KnowledgeRoutes.chunks(
                            datasetId = result.datasetId,
                            collectionId = result.collectionId,
                            dataId = result.dataId,
                        ),
                    )
                }
            },
        )
    }
}
