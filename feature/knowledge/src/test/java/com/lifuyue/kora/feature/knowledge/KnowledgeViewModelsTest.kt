package com.lifuyue.kora.feature.knowledge

import android.content.ContextWrapper
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun chunkViewerRefreshAndLoadMoreKeepHighlight() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                FakeKnowledgeRepository(
                    chunkPages =
                        mapOf(
                            0 to
                                List(20) {
                                    ChunkItemUiModel(
                                        dataId = "data-$it",
                                        chunkIndex = it,
                                        question = "q$it",
                                        answer = "a$it",
                                        status = "active",
                                    )
                                },
                            20 to
                                listOf(
                                    ChunkItemUiModel(
                                        dataId = "data-20",
                                        chunkIndex = 20,
                                        question = "q20",
                                        answer = "a20",
                                        status = "active",
                                    ),
                                ),
                        ),
                )
            val strings = FakeKnowledgeStrings()

            val viewModel =
                ChunkViewerViewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(
                                "datasetId" to "dataset-1",
                                "collectionId" to "collection-1",
                                "dataId" to "data-3",
                            ),
                        ),
                    repository = repository,
                    strings = strings,
                )

            advanceUntilIdle()
            assertEquals("data-3", viewModel.uiState.value.highlightedDataId)
            assertTrue(viewModel.uiState.value.canLoadMore)

            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(21, viewModel.uiState.value.items.size)
            assertEquals("data-3", viewModel.uiState.value.highlightedDataId)
        }

    @Test
    fun searchTestStoresExtensionInfoAndResults() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                FakeKnowledgeRepository(
                    searchResults =
                        Triple(
                            "120ms",
                            "rewrite-model",
                            listOf(
                                SearchResultUiModel(
                                    datasetId = "dataset-1",
                                    collectionId = "collection-1",
                                    dataId = "data-1",
                                    sourceName = "命中",
                                    question = "片段问题",
                                    answer = "片段",
                                    scoreType = "semantic",
                                    score = 0.98,
                                ),
                            ),
                        ),
                )
            val strings = FakeKnowledgeStrings()
            val viewModel =
                SearchTestViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("datasetId" to "dataset-1")),
                    repository = repository,
                    strings = strings,
                )

            viewModel.updateQuery("hello")
            viewModel.search()
            advanceUntilIdle()

            assertEquals("120ms", viewModel.uiState.value.duration)
            assertEquals("rewrite-model", viewModel.uiState.value.extensionInfo)
            assertEquals(1, viewModel.uiState.value.results.size)
        }

    @Test
    fun searchPreviewStateOpensAndDismisses() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel =
                SearchTestViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("datasetId" to "dataset-1")),
                    repository = FakeKnowledgeRepository(),
                    strings = FakeKnowledgeStrings(),
                )
            val result =
                SearchResultUiModel(
                    datasetId = "dataset-1",
                    collectionId = "collection-1",
                    dataId = "data-1",
                    sourceName = "source",
                    question = "question",
                    answer = "answer",
                )

            viewModel.openResultPreview(result)
            assertEquals(result, viewModel.uiState.value.activePreviewResult)

            viewModel.dismissResultPreview()
            assertEquals(null, viewModel.uiState.value.activePreviewResult)
        }

    @Test
    fun datasetBrowserFiltersByRawStatusField() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                FakeKnowledgeRepository(
                    datasets =
                        listOf(
                            DatasetListItemUiModel(
                                datasetId = "dataset-active",
                                name = "Active",
                                intro = "",
                                type = "qa",
                                status = "active",
                                vectorModel = "",
                                updateTime = 1L,
                            ),
                            DatasetListItemUiModel(
                                datasetId = "dataset-disabled",
                                name = "Disabled",
                                intro = "",
                                type = "text",
                                status = "disabled",
                                vectorModel = "",
                                updateTime = 2L,
                            ),
                        ),
                )
            val viewModel = DatasetBrowserViewModel(repository = repository, strings = FakeKnowledgeStrings())
            val collector = backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()
            viewModel.selectStatusFilter("disabled")
            advanceUntilIdle()

            assertEquals(listOf("dataset-disabled"), viewModel.uiState.value.items.map { it.datasetId })
            assertTrue(viewModel.uiState.value.availableStatuses.containsAll(listOf("active", "disabled")))
            collector.cancel()
        }

    @Test
    fun searchFailureUsesResourceFallbackMessage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val strings = FakeKnowledgeStrings()
            val repository = FakeKnowledgeRepository(searchFailure = IllegalStateException())
            val viewModel =
                SearchTestViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("datasetId" to "dataset-1")),
                    repository = repository,
                    strings = strings,
                )

            viewModel.search()
            advanceUntilIdle()

            assertEquals(strings.searchFailed(), viewModel.uiState.value.errorMessage)
        }
}

private class FakeKnowledgeStrings : KnowledgeStrings(context = ContextWrapper(null)) {
    override fun refreshFailed(): String = "刷新失败"

    override fun createFailed(): String = "创建失败"

    override fun deleteFailed(): String = "删除失败"

    override fun selectedDocumentUnreadable(): String = "无法读取所选文件"

    override fun unsupportedDocument(): String = "仅支持 PDF、DOCX、TXT、MD、CSV、HTML"

    override fun defaultTextImportName(): String = "文本导入"

    override fun defaultQaImportName(): String = "QA 导入"

    override fun defaultDocumentName(): String = "document"

    override fun submitFailed(): String = "提交失败"

    override fun loadFailed(): String = "加载失败"

    override fun saveFailed(): String = "保存失败"

    override fun searchFailed(): String = "检索失败"
}

private class FakeKnowledgeRepository(
    datasets: List<DatasetListItemUiModel> = emptyList(),
    private val chunkPages: Map<Int, List<ChunkItemUiModel>> = emptyMap(),
    private val searchResults: Triple<String, String, List<SearchResultUiModel>> = Triple("", "", emptyList()),
    private val searchFailure: Throwable? = null,
) : KnowledgeRepository {
    private val datasetFlow = MutableStateFlow(datasets)

    override fun observeDatasets(): Flow<List<DatasetListItemUiModel>> = datasetFlow

    override fun observeDataset(datasetId: String): Flow<DatasetListItemUiModel?> =
        MutableStateFlow(datasetFlow.value.firstOrNull { it.datasetId == datasetId })

    override suspend fun refreshDatasets(query: String) = Unit

    override suspend fun createDataset(name: String) = Unit

    override suspend fun deleteDataset(datasetId: String) = Unit

    override fun observeCollections(datasetId: String): Flow<List<CollectionListItemUiModel>> = MutableStateFlow(emptyList())

    override fun observeImportTasks(datasetId: String): Flow<List<ImportTaskUiModel>> = MutableStateFlow(emptyList())

    override suspend fun refreshCollections(datasetId: String) = Unit

    override suspend fun importText(
        datasetId: String,
        name: String,
        text: String,
        trainingType: String,
    ) = Unit

    override suspend fun importLinks(
        datasetId: String,
        urls: List<String>,
        selector: String?,
        trainingType: String,
    ) = Unit

    override suspend fun importDocument(
        datasetId: String,
        uri: Uri,
        displayName: String,
        trainingType: String,
    ) = Unit

    override suspend fun listChunks(
        datasetId: String,
        collectionId: String,
        offset: Int,
        pageSize: Int,
    ): List<ChunkItemUiModel> = chunkPages[offset].orEmpty()

    override suspend fun updateChunk(
        dataId: String,
        question: String,
        answer: String,
        forbid: Boolean,
    ) = Unit

    override suspend fun deleteChunk(dataId: String) = Unit

    override suspend fun search(
        datasetId: String,
        query: String,
        searchMode: String,
        similarity: Double?,
        embeddingWeight: Double?,
        usingReRank: Boolean,
    ): Triple<String, String, List<SearchResultUiModel>> = searchFailure?.let { throw it } ?: searchResults
}
