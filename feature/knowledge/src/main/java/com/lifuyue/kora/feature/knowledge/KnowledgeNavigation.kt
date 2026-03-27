package com.lifuyue.kora.feature.knowledge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object KnowledgeRoutes {
    const val overview = "knowledge"
    const val datasets = "knowledge/datasets"
    const val collections = "knowledge/datasets/{datasetId}/collections"
    const val chunks = "knowledge/datasets/{datasetId}/collections/{collectionId}"
    const val search = "knowledge/datasets/{datasetId}/search-test"

    fun collections(datasetId: String): String = "knowledge/datasets/$datasetId/collections"

    fun chunks(datasetId: String, collectionId: String): String =
        "knowledge/datasets/$datasetId/collections/$collectionId"

    fun search(datasetId: String): String = "knowledge/datasets/$datasetId/search-test"
}

fun NavGraphBuilder.knowledgeGraph(navController: NavController) {
    composable(KnowledgeRoutes.overview) {
        val viewModel: KnowledgeOverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        KnowledgeOverviewScreen(
            state = uiState,
            onOpenDatasets = { navController.navigate(KnowledgeRoutes.datasets) },
            onOpenRecentDataset = { navController.navigate(KnowledgeRoutes.collections(it)) },
        )
    }
    composable(KnowledgeRoutes.datasets) {
        val viewModel: DatasetBrowserViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DatasetBrowserScreen(
            state = uiState,
            onBack = { navController.popBackStack() },
            onQueryChanged = viewModel::updateQuery,
            onCreateNameChanged = viewModel::updateCreateName,
            onRefresh = viewModel::refresh,
            onCreateDataset = viewModel::createDataset,
            onDeleteDataset = viewModel::deleteDataset,
            onOpenDataset = { navController.navigate(KnowledgeRoutes.collections(it)) },
            onOpenSearch = { navController.navigate(KnowledgeRoutes.search(it)) },
        )
    }
    composable(
        route = KnowledgeRoutes.collections,
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
        route = KnowledgeRoutes.chunks,
        arguments =
            listOf(
                navArgument("datasetId") { type = NavType.StringType },
                navArgument("collectionId") { type = NavType.StringType },
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
            onSave = viewModel::saveEditing,
            onDelete = viewModel::deleteChunk,
        )
    }
    composable(
        route = KnowledgeRoutes.search,
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
        )
    }
}
