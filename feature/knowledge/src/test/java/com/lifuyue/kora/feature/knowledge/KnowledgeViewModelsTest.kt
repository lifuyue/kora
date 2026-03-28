package com.lifuyue.kora.feature.knowledge

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
                                    title = "命中",
                                    snippet = "片段",
                                    scoreLabel = "semantic · 0.98",
                                ),
                            ),
                        ),
                )
            val viewModel =
                SearchTestViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("datasetId" to "dataset-1")),
                    repository = repository,
                )

            viewModel.updateQuery("hello")
            viewModel.search()
            advanceUntilIdle()

            assertEquals("120ms", viewModel.uiState.value.duration)
            assertEquals("rewrite-model", viewModel.uiState.value.extensionInfo)
            assertEquals(1, viewModel.uiState.value.results.size)
        }
}

private class FakeKnowledgeRepository(
    private val chunkPages: Map<Int, List<ChunkItemUiModel>> = emptyMap(),
    private val searchResults: Triple<String, String, List<SearchResultUiModel>> = Triple("", "", emptyList()),
) : KnowledgeRepository {
    private val datasetFlow = MutableStateFlow(emptyList<DatasetListItemUiModel>())

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
    ): Triple<String, String, List<SearchResultUiModel>> = searchResults
}
