package com.lifuyue.kora.feature.knowledge

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KnowledgeScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chunkViewerHighlightsCitationTarget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ChunkViewerScreen(
                state =
                    ChunkViewerUiState(
                        datasetId = "dataset-1",
                        collectionId = "collection-1",
                        highlightedDataId = "data-1",
                        items =
                            listOf(
                                ChunkItemUiModel(
                                    dataId = "data-1",
                                    chunkIndex = 1,
                                    question = "命中结果",
                                    answer = "片段内容",
                                    status = "active",
                                ),
                            ),
                    ),
                onBack = {},
                onStartEditing = {},
                onQuestionChanged = {},
                onAnswerChanged = {},
                onDisabledChanged = {},
                onSave = {},
                onDelete = {},
                onLoadMore = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.knowledge_highlighted_hit)).assertExists()
        composeRule.onNodeWithText("命中结果").assertExists()
        composeRule.onNodeWithText("片段内容").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun datasetBrowserLocalizesPrimaryActionsInEnglish() {
        composeRule.setContent {
            DatasetBrowserScreen(
                state =
                    DatasetBrowserUiState(
                        items =
                            listOf(
                                DatasetListItemUiModel(
                                    datasetId = "dataset-1",
                                    name = "Dataset A",
                                    intro = "Intro",
                                    type = "qa",
                                    vectorModel = "",
                                    updateTimeLabel = "2026-03-28",
                                ),
                            ),
                    ),
                onBack = {},
                onQueryChanged = {},
                onCreateNameChanged = {},
                onTypeFilterSelected = {},
                onStatusFilterSelected = {},
                onRefresh = {},
                onCreateDataset = {},
                onDeleteDataset = {},
                onOpenDataset = {},
                onOpenSearch = {},
            )
        }

        composeRule.onNodeWithText("Datasets").assertExists()
        composeRule.onNodeWithText("Search datasets").assertExists()
        composeRule.onNodeWithText("Create").assertExists()
        composeRule.onNodeWithText("Refresh").assertExists()
        composeRule.onNodeWithText("Vector model: Unspecified").assertExists()
        composeRule.onNodeWithText("Updated: 2026-03-28").assertExists()
        composeRule.onNodeWithText("Collections").assertExists()
        composeRule.onNodeWithText("Search test").assertExists()
        composeRule.onNodeWithText("Delete").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun collectionChunkAndSearchFlowsLocalizeInEnglish() {
        composeRule.setContent {
            SearchTestScreen(
                state =
                    SearchTestUiState(
                        datasetId = "dataset-1",
                        query = "hello",
                        searchMode = "embedding",
                        similarity = "0.8",
                        embeddingWeight = "0.5",
                        duration = "120 ms",
                        extensionInfo = "gpt-4.1",
                        status = KnowledgeLoadState.Empty,
                    ),
                onBack = {},
                onQueryChanged = {},
                onSearchModeChanged = {},
                onSimilarityChanged = {},
                onEmbeddingWeightChanged = {},
                onUseReRankChanged = {},
                onSearch = {},
                onOpenResult = {},
            )
        }

        composeRule.onNodeWithText("Search test").assertExists()
        composeRule.onNodeWithText("Question").assertExists()
        composeRule.onNodeWithText("Use ReRank").assertExists()
        composeRule.onNodeWithText("Start search").assertExists()
        composeRule.onNodeWithText("Duration: 120 ms").assertExists()
        composeRule.onNodeWithText("Query extension model: gpt-4.1").assertExists()
        composeRule.onNodeWithText("No results found.").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun collectionAndChunkSummariesUseLocalizedLabelsInEnglish() {
        composeRule.setContent {
            CollectionManagementScreen(
                state =
                    CollectionManagementUiState(
                        datasetId = "dataset-1",
                        datasetName = "",
                        createMode = CollectionCreateMode.TEXT,
                        tasks =
                            listOf(
                                ImportTaskUiModel(
                                    taskId = "task-1",
                                    displayName = "Task A",
                                    sourceType = "file",
                                    status = "running",
                                    progress = 40,
                                ),
                            ),
                        items =
                            listOf(
                                CollectionListItemUiModel(
                                    collectionId = "collection-1",
                                    datasetId = "dataset-1",
                                    name = "Collection A",
                                    type = "file",
                                    trainingType = "chunk",
                                    status = "active",
                                    sourceName = "doc.pdf",
                                    updateTimeLabel = "2026-03-28",
                                ),
                            ),
                        status = KnowledgeLoadState.Content,
                    ),
                onBack = {},
                onRefresh = {},
                onModeChanged = {},
                onTextNameChanged = {},
                onTextValueChanged = {},
                onLinkValueChanged = {},
                onLinkSelectorChanged = {},
                onPickDocument = {},
                onSubmit = {},
                onOpenCollection = {},
                onOpenSearch = {},
            )
        }

        composeRule.onNodeWithText("Text").assertExists()
        composeRule.onNodeWithText("File · Running · 40%").assertExists()
        composeRule.onNodeWithText("File · Chunked · Active").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun chunkViewerHeaderUsesLocalizedStatusInEnglish() {
        composeRule.setContent {
            ChunkViewerScreen(
                state =
                    ChunkViewerUiState(
                        datasetId = "dataset-1",
                        collectionId = "collection-1",
                        items =
                            listOf(
                                ChunkItemUiModel(
                                    dataId = "data-1",
                                    chunkIndex = 1,
                                    question = "Question A",
                                    answer = "Answer B",
                                    status = "active",
                                ),
                            ),
                    ),
                onBack = {},
                onStartEditing = {},
                onQuestionChanged = {},
                onAnswerChanged = {},
                onDisabledChanged = {},
                onSave = {},
                onDelete = {},
                onLoadMore = {},
            )
        }

        composeRule.onNodeWithText("Chunk 1 · Active").assertExists()
    }
}
