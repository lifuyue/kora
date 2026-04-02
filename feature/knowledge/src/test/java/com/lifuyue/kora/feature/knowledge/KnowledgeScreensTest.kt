package com.lifuyue.kora.feature.knowledge

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus
import org.junit.Assert.assertTrue
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
    fun knowledgeOverviewShowsWorkspaceSummaryCard() {
        composeRule.setContent {
            KnowledgeOverviewScreen(
                state =
                    KnowledgeOverviewUiState(
                        selectedAppId = "app-1",
                        datasetCount = 12,
                        recentDatasets =
                            listOf(
                                DatasetListItemUiModel(
                                    datasetId = "dataset-1",
                                    name = "Architecture Notes",
                                    intro = "核心知识集",
                                    type = "qa",
                                    status = "active",
                                    vectorModel = "bge-m3",
                                    updateTime = 1_743_120_000_000,
                                ),
                            ),
                    ),
                onOpenDatasets = {},
                onOpenRecentDataset = {},
            )
        }

        composeRule.onNodeWithTag("knowledge_overview_summary_card").assertIsDisplayed()
        composeRule.onNodeWithText("Architecture Notes").fetchSemanticsNode()
    }

    @Test
    fun knowledgeOverviewShowsReturnToChatActionWhenContextProvided() {
        var returnedToChat = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            KnowledgeOverviewScreen(
                state = KnowledgeOverviewUiState(selectedAppId = "app-1", datasetCount = 3, status = KnowledgeLoadState.Content),
                onOpenDatasets = {},
                onOpenRecentDataset = {},
                onReturnToChat = { returnedToChat = true },
            )
        }

        val label = context.getString(R.string.knowledge_return_to_chat)
        composeRule.onNodeWithText(label).assertExists()
        composeRule.onNodeWithText(label).performClick()
        composeRule.runOnIdle {
            assertTrue(returnedToChat)
        }
    }

    @Test
    @Config(qualifiers = "en")
    fun localKnowledgeLibraryShowsFabAndDocumentActions() {
        composeRule.setContent {
            LocalKnowledgeLibraryScreen(
                state =
                    LocalKnowledgeLibraryUiState(
                        items =
                            listOf(
                                LocalKnowledgeDocumentUiModel(
                                    documentId = "doc-1",
                                    title = "Release Notes",
                                    sourceLabel = "Manual import",
                                    previewText = "This note explains the latest OpenAI setup.",
                                    chunkCount = 2,
                                    isEnabled = true,
                                    indexStatus = LocalKnowledgeIndexStatus.Ready,
                                    updatedAt = 1_743_120_000_000,
                                ),
                            ),
                        status = KnowledgeLoadState.Content,
                    ),
                onBack = {},
                onQueryChanged = {},
                onTitleChanged = {},
                onSourceChanged = {},
                onTextChanged = {},
                onImport = {},
                onOpenDocument = {},
                onDeleteDocument = {},
                onToggleEnabled = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("Local references").assertExists()
        composeRule.onNodeWithTag("knowledge_local_import_fab").assertIsDisplayed()
        composeRule.onAllNodesWithText("Import text reference").assertCountEquals(0)
        composeRule.onNodeWithText("Release Notes").assertExists()
        composeRule.onNodeWithText("View snippets").assertExists()
        composeRule.onNodeWithText("Delete").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun localKnowledgeLibraryOpensImportSheet() {
        composeRule.setContent {
            LocalKnowledgeLibraryScreen(
                state = LocalKnowledgeLibraryUiState(status = KnowledgeLoadState.Empty),
                onBack = {},
                onQueryChanged = {},
                onTitleChanged = {},
                onSourceChanged = {},
                onTextChanged = {},
                onImport = {},
                onOpenDocument = {},
                onDeleteDocument = {},
                onToggleEnabled = { _, _ -> },
            )
        }

        composeRule.onAllNodesWithTag("knowledge_local_import_sheet").assertCountEquals(0)
        composeRule.onNodeWithTag("knowledge_local_import_fab").performClick()
        composeRule.onNodeWithTag("knowledge_local_import_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Import text reference").assertExists()
        composeRule.onNodeWithText("Reference title").assertExists()
        composeRule.onNodeWithText("Source label").assertExists()
        composeRule.onNodeWithText("Text content").assertExists()
        composeRule.onNodeWithTag("knowledge_local_import_submit").assertExists()
    }

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
                        availableTypes = listOf("qa"),
                        availableStatuses = listOf("active"),
                        items =
                            listOf(
                                DatasetListItemUiModel(
                                    datasetId = "dataset-1",
                                    name = "Dataset A",
                                    intro = "",
                                    type = "qa",
                                    status = "active",
                                    vectorModel = "",
                                    updateTime = 1_743_120_000_000,
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
        composeRule.onNodeWithText("Q&A · Active").assertExists()
        composeRule.onNodeWithText("Vector model: Unspecified").assertExists()
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
                        results =
                            listOf(
                                SearchResultUiModel(
                                    datasetId = "dataset-1",
                                    collectionId = "collection-1",
                                    dataId = "data-1",
                                    sourceName = "",
                                    question = "",
                                    answer = "",
                                    scoreType = "semantic",
                                    score = 0.98,
                                ),
                            ),
                        status = KnowledgeLoadState.Empty,
                    ),
                onBack = {},
                onQueryChanged = {},
                onSearchModeChanged = {},
                onSimilarityChanged = {},
                onEmbeddingWeightChanged = {},
                onUseReRankChanged = {},
                onSearch = {},
                onOpenResultPreview = {},
                onDismissPreview = {},
                onOpenResultContext = {},
            )
        }

        composeRule.onNodeWithText("Search test").assertExists()
        composeRule.onNodeWithText("Question").assertExists()
        composeRule.onNodeWithText("Use ReRank").assertExists()
        composeRule.onNodeWithText("Start search").assertExists()
        composeRule.onNodeWithText("Duration: 120 ms").assertExists()
        composeRule.onNodeWithText("Query extension model: gpt-4.1").assertExists()
        composeRule.onNodeWithText("Matched snippet").assertExists()
        composeRule.onNodeWithText("No preview available").assertExists()
        composeRule.onNodeWithText("semantic · 0.98").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun searchResultOpensPreviewBeforeContextNavigation() {
        composeRule.setContent {
            SearchTestScreen(
                state =
                    SearchTestUiState(
                        datasetId = "dataset-1",
                        results =
                            listOf(
                                SearchResultUiModel(
                                    datasetId = "dataset-1",
                                    collectionId = "collection-1",
                                    dataId = "data-1",
                                    sourceName = "Doc A",
                                    question = "Question A",
                                    answer = "Answer A",
                                    scoreType = "semantic",
                                    score = 0.91,
                                ),
                            ),
                        activePreviewResult =
                            SearchResultUiModel(
                                datasetId = "dataset-1",
                                collectionId = "collection-1",
                                dataId = "data-1",
                                sourceName = "Doc A",
                                question = "Question A",
                                answer = "Answer A",
                                scoreType = "semantic",
                                score = 0.91,
                            ),
                    ),
                onBack = {},
                onQueryChanged = {},
                onSearchModeChanged = {},
                onSimilarityChanged = {},
                onEmbeddingWeightChanged = {},
                onUseReRankChanged = {},
                onSearch = {},
                previewResult =
                    SearchResultUiModel(
                        datasetId = "dataset-1",
                        collectionId = "collection-1",
                        dataId = "data-1",
                        sourceName = "Doc A",
                        question = "Question A",
                        answer = "Answer A",
                        scoreType = "semantic",
                        score = 0.91,
                    ),
                onOpenResultPreview = {},
                onDismissPreview = {},
                onOpenResultContext = {},
            )
        }

        composeRule.onNodeWithTag("knowledge_search_result_preview_content").assertExists()
        composeRule.onNodeWithText("Reference preview").assertExists()
        composeRule.onNodeWithText("Source: Doc A").assertExists()
        composeRule.onNodeWithText("Question A\n\nAnswer A").assertExists()
        composeRule.onNodeWithTag("knowledge_search_result_preview_open_context").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun searchResultPreviewSheetOpensContextAction() {
        var openContextCount = 0
        composeRule.setContent {
            SearchResultPreviewSheet(
                result =
                    SearchResultUiModel(
                        datasetId = "dataset-1",
                        collectionId = "collection-1",
                        dataId = "data-1",
                        sourceName = "Doc A",
                        question = "Question A",
                        answer = "Answer A",
                        scoreType = "semantic",
                        score = 0.91,
                    ),
                onOpenContext = { openContextCount += 1 },
            )
        }

        composeRule.onNodeWithTag("knowledge_search_result_preview_open_context").performClick()
        composeRule.runOnIdle {
            assertTrue(openContextCount == 1)
        }
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
                                    updateTime = 1_743_120_000_000,
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

    @Test
    @Config(qualifiers = "en")
    fun overviewRecentDatasetUsesLocalizedTypeLabelInEnglish() {
        composeRule.setContent {
            KnowledgeOverviewScreen(
                state =
                    KnowledgeOverviewUiState(
                        selectedAppId = "app-1",
                        datasetCount = 1,
                        recentDatasets =
                            listOf(
                                DatasetListItemUiModel(
                                    datasetId = "dataset-1",
                                    name = "Dataset A",
                                    intro = "",
                                    type = "qa",
                                    status = "active",
                                    vectorModel = "",
                                    updateTime = 1L,
                                ),
                            ),
                        status = KnowledgeLoadState.Content,
                    ),
                onOpenDatasets = {},
                onOpenRecentDataset = {},
            )
        }

        composeRule.onNodeWithText("Q&A").assertExists()
    }
}
