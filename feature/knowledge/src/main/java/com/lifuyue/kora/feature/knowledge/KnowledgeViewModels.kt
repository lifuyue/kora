package com.lifuyue.kora.feature.knowledge

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.database.LocalKnowledgeStore
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Qualifier
import javax.inject.Inject
import javax.inject.Singleton

@Qualifier
annotation class KnowledgeIoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object KnowledgeDispatchersModule {
    @Provides
    @Singleton
    @KnowledgeIoDispatcher
    fun provideKnowledgeIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@HiltViewModel
class KnowledgeOverviewViewModel
    @Inject
    constructor(
        connectionRepository: ConnectionRepository,
        repository: KnowledgeRepository,
    ) : ViewModel() {
        val uiState: StateFlow<KnowledgeOverviewUiState> =
            combine(connectionRepository.snapshot, repository.observeDatasets()) { snapshot, datasets ->
                KnowledgeOverviewUiState(
                    connectionType = snapshot.connectionType,
                    selectedAppId = snapshot.selectedAppId,
                    datasetCount = datasets.size,
                    recentDatasets = datasets.take(3),
                    status = if (datasets.isEmpty()) KnowledgeLoadState.Empty else KnowledgeLoadState.Content,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnowledgeOverviewUiState())
    }

@HiltViewModel
class KnowledgeModeViewModel
    @Inject
    constructor(
        connectionRepository: ConnectionRepository,
    ) : ViewModel() {
        val connectionType: StateFlow<ConnectionType> =
            connectionRepository.snapshot
                .map { it.connectionType }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionType.OPENAI_COMPATIBLE)
    }

@HiltViewModel
class LocalKnowledgeOverviewViewModel
    @Inject
    constructor(
        localKnowledgeStore: LocalKnowledgeStore,
    ) : ViewModel() {
        val uiState: StateFlow<KnowledgeOverviewUiState> =
            localKnowledgeStore.observeDocuments()
                .map { documents ->
                    KnowledgeOverviewUiState(
                        connectionType = ConnectionType.OPENAI_COMPATIBLE,
                        selectedAppId = null,
                        datasetCount = documents.size,
                        recentDatasets =
                            documents.take(3).map { item ->
                                DatasetListItemUiModel(
                                    datasetId = item.documentId,
                                    name = item.title,
                                    intro = item.previewText,
                                    type = "local_text",
                                    status = if (item.isEnabled) "enabled" else "paused",
                                    vectorModel = "",
                                    updateTime = item.updatedAt,
                                )
                            },
                        status = if (documents.isEmpty()) KnowledgeLoadState.Empty else KnowledgeLoadState.Content,
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnowledgeOverviewUiState(connectionType = ConnectionType.OPENAI_COMPATIBLE))
    }

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LocalKnowledgeLibraryViewModel
    @Inject
    constructor(
        private val localKnowledgeStore: LocalKnowledgeStore,
        private val strings: KnowledgeStrings,
        @KnowledgeIoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val meta = MutableStateFlow(LocalKnowledgeLibraryUiState())

        val uiState: StateFlow<LocalKnowledgeLibraryUiState> =
            combine(meta, localKnowledgeStore.observeDocuments()) { state, documents -> state to documents }
                .mapLatest { (state, documents) ->
                    val visibleDocuments =
                        if (state.query.isBlank()) {
                            documents
                        } else {
                            withContext(ioDispatcher) {
                                val documentsById = documents.associateBy { it.documentId }
                                localKnowledgeStore
                                    .search(state.query, limit = 20)
                                    .map { it.documentId }
                                    .distinct()
                                    .mapNotNull(documentsById::get)
                            }
                        }
                    state.copy(
                        items =
                            visibleDocuments.map { item ->
                                LocalKnowledgeDocumentUiModel(
                                    documentId = item.documentId,
                                    title = item.title,
                                    sourceLabel = item.sourceLabel,
                                    previewText = item.previewText,
                                    chunkCount = item.chunkCount,
                                    isEnabled = item.isEnabled,
                                    indexStatus = item.indexStatus,
                                    indexErrorMessage = item.indexErrorMessage,
                                    updatedAt = item.updatedAt,
                                )
                            },
                        status =
                            when {
                                documents.isEmpty() -> KnowledgeLoadState.Empty
                                visibleDocuments.isEmpty() -> KnowledgeLoadState.Empty
                                else -> KnowledgeLoadState.Content
                            },
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalKnowledgeLibraryUiState())

        fun updateQuery(value: String) {
            meta.update { it.copy(query = value) }
        }

        fun updateDraftTitle(value: String) {
            meta.update { it.copy(draftTitle = value) }
        }

        fun updateDraftSource(value: String) {
            meta.update { it.copy(draftSource = value) }
        }

        fun updateDraftText(value: String) {
            meta.update { it.copy(draftText = value) }
        }

        fun importDraft() {
            val snapshot = meta.value
            val title = snapshot.draftTitle.trim()
            val text = snapshot.draftText.trim()
            if (title.isBlank() || text.isBlank()) {
                meta.update { it.copy(errorMessage = strings.localImportValidationFailed()) }
                return
            }
            viewModelScope.launch {
                runCatching {
                    withContext(ioDispatcher) {
                        localKnowledgeStore.importText(
                            title = title,
                            text = text,
                            sourceLabel = snapshot.draftSource.trim().ifBlank { strings.localManualImportSource() },
                        )
                    }
                }.onSuccess {
                    meta.update { it.copy(draftTitle = "", draftSource = "", draftText = "", errorMessage = null) }
                }.onFailure { error ->
                    meta.update { it.copy(errorMessage = error.message ?: strings.localImportFailed()) }
                }
            }
        }

        fun deleteDocument(documentId: String) {
            viewModelScope.launch(ioDispatcher) {
                localKnowledgeStore.deleteDocument(documentId)
            }
        }

        fun setEnabled(
            documentId: String,
            enabled: Boolean,
        ) {
            viewModelScope.launch(ioDispatcher) {
                localKnowledgeStore.setEnabled(documentId, enabled)
            }
        }
    }

@HiltViewModel
class LocalKnowledgeDocumentViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        localKnowledgeStore: LocalKnowledgeStore,
    ) : ViewModel() {
        private val documentId: String = checkNotNull(savedStateHandle["datasetId"])

        val uiState: StateFlow<LocalKnowledgeDocumentUiState> =
            localKnowledgeStore.observeDocumentWithChunks(documentId)
                .map { (document, chunks) ->
                    LocalKnowledgeDocumentUiState(
                        documentId = documentId,
                        title = document?.title.orEmpty(),
                        sourceLabel = document?.sourceLabel.orEmpty(),
                        isEnabled = document?.isEnabled ?: true,
                        chunks =
                            chunks.map { chunk ->
                                ChunkItemUiModel(
                                    dataId = chunk.chunkId,
                                    chunkIndex = chunk.chunkIndex + 1,
                                    question = "片段 ${chunk.chunkIndex + 1}",
                                    answer = chunk.text,
                                    status = "active",
                                )
                            },
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalKnowledgeDocumentUiState(documentId = documentId))
    }

@HiltViewModel
class DatasetBrowserViewModel
    @Inject
    constructor(
        private val repository: KnowledgeRepository,
        private val strings: KnowledgeStrings,
    ) : ViewModel() {
        private val meta = MutableStateFlow(DatasetBrowserUiState())

        val uiState: StateFlow<DatasetBrowserUiState> =
            combine(meta, repository.observeDatasets()) { state, datasets ->
                val filtered =
                    datasets.filter {
                        (
                            state.query.isBlank() ||
                                it.name.contains(state.query, ignoreCase = true) ||
                                it.intro.contains(state.query, ignoreCase = true)
                        ) &&
                            (state.selectedTypeFilter == null || it.type == state.selectedTypeFilter) &&
                            (state.selectedStatusFilter == null || it.status == state.selectedStatusFilter)
                    }
                state.copy(
                    items = filtered,
                    availableTypes = datasets.map { it.type }.filter { it.isNotBlank() }.distinct(),
                    availableStatuses = datasets.map { it.status }.filter { it.isNotBlank() }.distinct(),
                    status =
                        when {
                            state.errorMessage != null -> KnowledgeLoadState.Error
                            filtered.isEmpty() -> KnowledgeLoadState.Empty
                            else -> KnowledgeLoadState.Content
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

        fun selectTypeFilter(value: String?) {
            meta.update { it.copy(selectedTypeFilter = value) }
        }

        fun selectStatusFilter(value: String?) {
            meta.update { it.copy(selectedStatusFilter = value) }
        }

        fun refresh() {
            viewModelScope.launch {
                meta.update { it.copy(isRefreshing = true, errorMessage = null) }
                runCatching { repository.refreshDatasets(meta.value.query) }
                    .onFailure {
                        meta.update { state ->
                            state.copy(
                                errorMessage = it.message ?: strings.refreshFailed(),
                                status = KnowledgeLoadState.Error,
                            )
                        }
                    }
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
                    }.onFailure {
                        meta.update { state ->
                            state.copy(errorMessage = it.message ?: strings.createFailed())
                        }
                    }
            }
        }

        fun deleteDataset(datasetId: String) {
            viewModelScope.launch {
                runCatching { repository.deleteDataset(datasetId) }
                    .onFailure {
                        meta.update { state ->
                            state.copy(errorMessage = it.message ?: strings.deleteFailed())
                        }
                    }
            }
        }
    }

@HiltViewModel
class CollectionManagementViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: KnowledgeRepository,
        private val strings: KnowledgeStrings,
    ) : ViewModel() {
        val datasetId: String = checkNotNull(savedStateHandle["datasetId"])
        private val meta = MutableStateFlow(CollectionManagementUiState(datasetId = datasetId))

        val uiState: StateFlow<CollectionManagementUiState> =
            combine(
                meta,
                repository.observeDataset(datasetId),
                repository.observeCollections(datasetId),
                repository.observeImportTasks(datasetId),
            ) { state, dataset, collections, tasks ->
                state.copy(
                    datasetName = dataset?.name.orEmpty(),
                    items = collections,
                    tasks = tasks,
                    status =
                        when {
                            state.errorMessage != null -> KnowledgeLoadState.Error
                            collections.isEmpty() && tasks.isEmpty() -> KnowledgeLoadState.Empty
                            else -> KnowledgeLoadState.Content
                        },
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CollectionManagementUiState(datasetId = datasetId, status = KnowledgeLoadState.Loading),
            )

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                runCatching { repository.refreshCollections(datasetId) }
                    .onFailure {
                        meta.update { state ->
                            state.copy(errorMessage = it.message ?: strings.refreshFailed())
                        }
                    }
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

        fun setSelectedDocument(
            uri: Uri?,
            displayName: String,
        ) {
            val errorMessage =
                if (displayName.isNotBlank() && !displayName.isSupportedDocument()) {
                    strings.unsupportedDocument()
                } else {
                    null
                }
            meta.update { it.copy(selectedDocumentUri = uri, selectedDocumentName = displayName, errorMessage = errorMessage) }
        }

        fun submit() {
            viewModelScope.launch {
                meta.update { it.copy(isSubmitting = true, errorMessage = null) }
                runCatching {
                    when (meta.value.createMode) {
                        CollectionCreateMode.TEXT ->
                            repository.importText(
                                datasetId = datasetId,
                                name = meta.value.textDraftName.ifBlank { strings.defaultTextImportName() },
                                text = meta.value.textDraftValue,
                                trainingType = "chunk",
                            )
                        CollectionCreateMode.QA ->
                            repository.importText(
                                datasetId = datasetId,
                                name = meta.value.textDraftName.ifBlank { strings.defaultQaImportName() },
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
                            check(meta.value.selectedDocumentName.isSupportedDocument()) {
                                strings.unsupportedDocument()
                            }
                            repository.importDocument(
                                datasetId = datasetId,
                                uri = uri,
                                displayName = meta.value.selectedDocumentName.ifBlank { strings.defaultDocumentName() },
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
                            errorMessage = null,
                        )
                    }
                }.onFailure { error ->
                    val message =
                        when (error) {
                            SelectedDocumentUnreadableException -> strings.selectedDocumentUnreadable()
                            else -> error.message ?: strings.submitFailed()
                        }
                    meta.update { it.copy(isSubmitting = false, errorMessage = message) }
                }
            }
        }
    }

@HiltViewModel
class ChunkViewerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: KnowledgeRepository,
        private val strings: KnowledgeStrings,
    ) : ViewModel() {
        companion object {
            private const val PAGE_SIZE = 20
        }

        private val datasetId: String = checkNotNull(savedStateHandle["datasetId"])
        private val collectionId: String = checkNotNull(savedStateHandle["collectionId"])
        private val highlightedDataId: String? = savedStateHandle["dataId"]
        private val meta =
            MutableStateFlow(
                ChunkViewerUiState(
                    datasetId = datasetId,
                    collectionId = collectionId,
                    highlightedDataId = highlightedDataId,
                ),
            )

        val uiState: StateFlow<ChunkViewerUiState> = meta.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                meta.update { it.copy(isLoading = true, nextOffset = 0) }
                runCatching { repository.listChunks(datasetId, collectionId, offset = 0, pageSize = PAGE_SIZE) }
                    .onSuccess {
                        meta.update { state ->
                            state.copy(
                                items = it,
                                canLoadMore = it.size >= PAGE_SIZE,
                                nextOffset = it.size,
                                isLoading = false,
                                errorMessage = null,
                            )
                        }
                    }.onFailure {
                        meta.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = it.message ?: strings.loadFailed(),
                            )
                        }
                    }
            }
        }

        fun loadMore() {
            if (meta.value.isLoading || !meta.value.canLoadMore) return
            viewModelScope.launch {
                meta.update { it.copy(isLoading = true) }
                runCatching {
                    repository.listChunks(
                        datasetId = datasetId,
                        collectionId = collectionId,
                        offset = meta.value.nextOffset,
                        pageSize = PAGE_SIZE,
                    )
                }.onSuccess { newItems ->
                    meta.update { state ->
                        state.copy(
                            items = state.items + newItems,
                            nextOffset = state.nextOffset + newItems.size,
                            canLoadMore = newItems.size >= PAGE_SIZE,
                            isLoading = false,
                        )
                    }
                }.onFailure { error ->
                    meta.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: strings.loadFailed())
                    }
                }
            }
        }

        fun startEditing(item: ChunkItemUiModel) {
            meta.update {
                it.copy(
                    editingChunkId = item.dataId,
                    editingQuestion = item.question,
                    editingAnswer = item.answer,
                    editingDisabled = item.isDisabled,
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
                        forbid = meta.value.editingDisabled,
                    )
                }.onSuccess {
                    meta.update {
                        it.copy(
                            editingChunkId = null,
                            editingQuestion = "",
                            editingAnswer = "",
                            editingDisabled = false,
                        )
                    }
                    refresh()
                }.onFailure {
                    meta.update { state ->
                        state.copy(errorMessage = it.message ?: strings.saveFailed())
                    }
                }
            }
        }

        fun updateEditingDisabled(value: Boolean) {
            meta.update { it.copy(editingDisabled = value) }
        }

        fun deleteChunk(chunkId: String) {
            viewModelScope.launch {
                runCatching { repository.deleteChunk(chunkId) }
                    .onSuccess { refresh() }
                    .onFailure {
                        meta.update { state ->
                            state.copy(errorMessage = it.message ?: strings.deleteFailed())
                        }
                    }
            }
        }
    }

@HiltViewModel
class SearchTestViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: KnowledgeRepository,
        private val strings: KnowledgeStrings,
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

        fun openResultPreview(result: SearchResultUiModel) {
            mutableState.update { it.copy(activePreviewResult = result) }
        }

        fun dismissResultPreview() {
            mutableState.update { it.copy(activePreviewResult = null) }
        }

        fun search() {
            viewModelScope.launch {
                mutableState.update { it.copy(isSearching = true, errorMessage = null, activePreviewResult = null) }
                runCatching {
                    repository.search(
                        datasetId = datasetId,
                        query = mutableState.value.query,
                        searchMode = mutableState.value.searchMode,
                        similarity = mutableState.value.similarity.toDoubleOrNull(),
                        embeddingWeight = mutableState.value.embeddingWeight.toDoubleOrNull(),
                        usingReRank = mutableState.value.useReRank,
                    )
                }.onSuccess { (duration, extensionInfo, results) ->
                    mutableState.update {
                        it.copy(
                            isSearching = false,
                            duration = duration,
                            extensionInfo = extensionInfo,
                            results = results,
                            activePreviewResult = null,
                            status = if (results.isEmpty()) KnowledgeLoadState.Empty else KnowledgeLoadState.Content,
                        )
                    }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSearching = false,
                            activePreviewResult = null,
                            errorMessage = error.message ?: strings.searchFailed(),
                            status = KnowledgeLoadState.Error,
                        )
                    }
                }
            }
        }
    }

private fun String.isSupportedDocument(): Boolean {
    val extension = substringAfterLast('.', "").lowercase()
    return extension in setOf("pdf", "docx", "txt", "md", "csv", "html", "htm")
}

open class KnowledgeStrings
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        open fun refreshFailed(): String = context.getString(R.string.knowledge_error_refresh_failed)

        open fun createFailed(): String = context.getString(R.string.knowledge_error_create_failed)

        open fun deleteFailed(): String = context.getString(R.string.knowledge_error_delete_failed)

        open fun selectedDocumentUnreadable(): String = context.getString(R.string.knowledge_selected_document_unreadable)

        open fun unsupportedDocument(): String = context.getString(R.string.knowledge_selected_document_unsupported)

        open fun defaultTextImportName(): String = context.getString(R.string.knowledge_default_text_import_name)

        open fun defaultQaImportName(): String = context.getString(R.string.knowledge_default_qa_import_name)

        open fun defaultDocumentName(): String = context.getString(R.string.knowledge_default_document_name)

        open fun submitFailed(): String = context.getString(R.string.knowledge_error_submit_failed)

        open fun loadFailed(): String = context.getString(R.string.knowledge_error_load_failed)

        open fun saveFailed(): String = context.getString(R.string.knowledge_error_save_failed)

        open fun searchFailed(): String = context.getString(R.string.knowledge_error_search_failed)

        open fun localImportValidationFailed(): String = context.getString(R.string.knowledge_local_error_validation)

        open fun localManualImportSource(): String = context.getString(R.string.knowledge_local_manual_source)

        open fun localImportFailed(): String = context.getString(R.string.knowledge_local_error_import_failed)
    }
