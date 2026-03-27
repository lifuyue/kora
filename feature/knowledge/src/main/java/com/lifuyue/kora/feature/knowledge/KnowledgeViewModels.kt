package com.lifuyue.kora.feature.knowledge

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class KnowledgeOverviewViewModel @Inject constructor(
    connectionRepository: ConnectionRepository,
    repository: KnowledgeRepository,
) : ViewModel() {
    val uiState: StateFlow<KnowledgeOverviewUiState> =
        combine(connectionRepository.snapshot, repository.observeDatasets()) { snapshot, datasets ->
            KnowledgeOverviewUiState(
                selectedAppId = snapshot.selectedAppId,
                datasetCount = datasets.size,
                recentDatasets = datasets.take(3),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnowledgeOverviewUiState())
}

@HiltViewModel
class DatasetBrowserViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
) : ViewModel() {
    private val meta = MutableStateFlow(DatasetBrowserUiState())

    val uiState: StateFlow<DatasetBrowserUiState> =
        combine(meta, repository.observeDatasets()) { state, datasets ->
            state.copy(
                items = datasets.filter {
                    state.query.isBlank() ||
                        it.name.contains(state.query, ignoreCase = true) ||
                        it.intro.contains(state.query, ignoreCase = true)
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DatasetBrowserUiState())

    init {
        refresh()
    }

    fun updateQuery(value: String) {
        meta.update { it.copy(query = value) }
    }

    fun updateCreateName(value: String) {
        meta.update { it.copy(createName = value) }
    }

    fun refresh() {
        viewModelScope.launch {
            meta.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching { repository.refreshDatasets(meta.value.query) }
                .onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "刷新失败") } }
            meta.update { it.copy(isRefreshing = false) }
        }
    }

    fun createDataset() {
        val name = meta.value.createName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.createDataset(name) }
                .onSuccess {
                    meta.update { it.copy(createName = "") }
                    repository.refreshDatasets(meta.value.query)
                }.onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "创建失败") } }
        }
    }

    fun deleteDataset(datasetId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteDataset(datasetId) }
                .onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "删除失败") } }
        }
    }
}

@HiltViewModel
class CollectionManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: KnowledgeRepository,
) : ViewModel() {
    val datasetId: String = checkNotNull(savedStateHandle["datasetId"])
    private val meta = MutableStateFlow(CollectionManagementUiState(datasetId = datasetId))

    val uiState: StateFlow<CollectionManagementUiState> =
        combine(
            meta,
            repository.observeCollections(datasetId),
            repository.observeImportTasks(datasetId),
        ) { state, collections, tasks ->
            state.copy(items = collections, tasks = tasks)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollectionManagementUiState(datasetId = datasetId),
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.refreshCollections(datasetId) }
                .onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "刷新失败") } }
        }
    }

    fun switchMode(mode: CollectionCreateMode) {
        meta.update { it.copy(createMode = mode) }
    }

    fun updateTextName(value: String) {
        meta.update { it.copy(textDraftName = value) }
    }

    fun updateTextValue(value: String) {
        meta.update { it.copy(textDraftValue = value) }
    }

    fun updateLinkValue(value: String) {
        meta.update { it.copy(linkDraftValue = value) }
    }

    fun updateLinkSelector(value: String) {
        meta.update { it.copy(linkSelector = value) }
    }

    fun setSelectedDocument(uri: Uri?, displayName: String) {
        meta.update { it.copy(selectedDocumentUri = uri, selectedDocumentName = displayName) }
    }

    fun submit() {
        viewModelScope.launch {
            meta.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                when (meta.value.createMode) {
                    CollectionCreateMode.TEXT ->
                        repository.importText(
                            datasetId = datasetId,
                            name = meta.value.textDraftName.ifBlank { "文本导入" },
                            text = meta.value.textDraftValue,
                            trainingType = "chunk",
                        )
                    CollectionCreateMode.QA ->
                        repository.importText(
                            datasetId = datasetId,
                            name = meta.value.textDraftName.ifBlank { "QA 导入" },
                            text = meta.value.textDraftValue,
                            trainingType = "qa",
                        )
                    CollectionCreateMode.LINK ->
                        repository.importLinks(
                            datasetId = datasetId,
                            urls = meta.value.linkDraftValue.lines().map(String::trim).filter(String::isNotBlank),
                            selector = meta.value.linkSelector,
                            trainingType = "chunk",
                        )
                    CollectionCreateMode.FILE -> {
                        val uri = checkNotNull(meta.value.selectedDocumentUri)
                        repository.importDocument(
                            datasetId = datasetId,
                            uri = uri,
                            displayName = meta.value.selectedDocumentName.ifBlank { "document" },
                            trainingType = "chunk",
                        )
                    }
                }
            }.onSuccess {
                meta.update {
                    it.copy(
                        isSubmitting = false,
                        textDraftName = "",
                        textDraftValue = "",
                        linkDraftValue = "",
                        linkSelector = "",
                        selectedDocumentUri = null,
                        selectedDocumentName = "",
                    )
                }
            }.onFailure { error ->
                meta.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "提交失败") }
            }
        }
    }
}

@HiltViewModel
class ChunkViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: KnowledgeRepository,
) : ViewModel() {
    private val datasetId: String = checkNotNull(savedStateHandle["datasetId"])
    private val collectionId: String = checkNotNull(savedStateHandle["collectionId"])
    private val meta = MutableStateFlow(ChunkViewerUiState(datasetId = datasetId, collectionId = collectionId))

    val uiState: StateFlow<ChunkViewerUiState> = meta.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.listChunks(datasetId, collectionId) }
                .onSuccess { meta.update { state -> state.copy(items = it, errorMessage = null) } }
                .onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "加载失败") } }
        }
    }

    fun startEditing(item: ChunkItemUiModel) {
        meta.update {
            it.copy(
                editingChunkId = item.dataId,
                editingQuestion = item.question,
                editingAnswer = item.answer,
            )
        }
    }

    fun updateEditingQuestion(value: String) {
        meta.update { it.copy(editingQuestion = value) }
    }

    fun updateEditingAnswer(value: String) {
        meta.update { it.copy(editingAnswer = value) }
    }

    fun saveEditing() {
        val chunkId = meta.value.editingChunkId ?: return
        viewModelScope.launch {
            runCatching {
                repository.updateChunk(
                    dataId = chunkId,
                    question = meta.value.editingQuestion,
                    answer = meta.value.editingAnswer,
                )
            }.onSuccess {
                meta.update { it.copy(editingChunkId = null, editingQuestion = "", editingAnswer = "") }
                refresh()
            }.onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "保存失败") } }
        }
    }

    fun deleteChunk(chunkId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteChunk(chunkId) }
                .onSuccess { refresh() }
                .onFailure { meta.update { state -> state.copy(errorMessage = it.message ?: "删除失败") } }
        }
    }
}

@HiltViewModel
class SearchTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: KnowledgeRepository,
) : ViewModel() {
    private val datasetId: String = checkNotNull(savedStateHandle["datasetId"])
    private val mutableState = MutableStateFlow(SearchTestUiState(datasetId = datasetId))
    val uiState: StateFlow<SearchTestUiState> = mutableState.asStateFlow()

    fun updateQuery(value: String) {
        mutableState.update { it.copy(query = value) }
    }

    fun updateMode(value: String) {
        mutableState.update { it.copy(searchMode = value) }
    }

    fun updateSimilarity(value: String) {
        mutableState.update { it.copy(similarity = value) }
    }

    fun updateEmbeddingWeight(value: String) {
        mutableState.update { it.copy(embeddingWeight = value) }
    }

    fun updateReRank(enabled: Boolean) {
        mutableState.update { it.copy(useReRank = enabled) }
    }

    fun search() {
        viewModelScope.launch {
            mutableState.update { it.copy(isSearching = true, errorMessage = null) }
            runCatching {
                repository.search(
                    datasetId = datasetId,
                    query = mutableState.value.query,
                    searchMode = mutableState.value.searchMode,
                    similarity = mutableState.value.similarity.toDoubleOrNull(),
                    embeddingWeight = mutableState.value.embeddingWeight.toDoubleOrNull(),
                    usingReRank = mutableState.value.useReRank,
                )
            }.onSuccess { (duration, results) ->
                mutableState.update { it.copy(isSearching = false, duration = duration, results = results) }
            }.onFailure { error ->
                mutableState.update { it.copy(isSearching = false, errorMessage = error.message ?: "检索失败") }
            }
        }
    }
}
