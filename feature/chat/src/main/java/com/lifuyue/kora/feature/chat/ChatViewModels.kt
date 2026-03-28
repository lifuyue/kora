package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.network.FastGptApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val conversationRepository: ConversationRepository,
    ) : ViewModel() {
        val appId: String = checkNotNull(savedStateHandle["appId"])
        private val query = MutableStateFlow("")
        private val selectedFolderId = MutableStateFlow<String?>(null)
        private val selectedTagId = MutableStateFlow<String?>(null)
        private val isRefreshing = MutableStateFlow(false)
        private val filters =
            combine(query, selectedFolderId, selectedTagId, isRefreshing) { currentQuery, folderId, tagId, refreshing ->
                ConversationListFilters(
                    query = currentQuery,
                    folderId = folderId,
                    tagId = tagId,
                    isRefreshing = refreshing,
                )
            }

        val uiState: StateFlow<ConversationListUiState> =
            combine(
                filters,
                conversationRepository.observeConversations(appId),
                conversationRepository.observeFolders(appId),
                conversationRepository.observeTags(appId),
            ) { filters, items, folders, tags ->
                val filtered =
                    items.filter { item ->
                        val matchesQuery =
                            filters.query.isBlank() ||
                                item.title.contains(filters.query, ignoreCase = true) ||
                                item.preview.contains(filters.query, ignoreCase = true)
                        val matchesFolder = filters.folderId == null || item.folderId == filters.folderId
                        val matchesTag = filters.tagId == null || item.tags.any { it.tagId == filters.tagId }
                        matchesQuery && matchesFolder && matchesTag
                    }
                ConversationListUiState(
                    query = filters.query,
                    items = filtered,
                    folders = folders,
                    tags = tags,
                    selectedFolderId = filters.folderId,
                    selectedTagId = filters.tagId,
                    isRefreshing = filters.isRefreshing,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationListUiState())

        init {
            refresh()
        }

        fun updateQuery(value: String) {
            query.value = value
        }

        fun selectFolder(folderId: String?) {
            selectedFolderId.value = folderId
        }

        fun selectTag(tagId: String?) {
            selectedTagId.value = tagId
        }

        fun refresh() {
            viewModelScope.launch {
                isRefreshing.value = true
                runCatching { conversationRepository.refreshConversations(appId) }
                isRefreshing.value = false
            }
        }

        fun deleteConversation(chatId: String) {
            viewModelScope.launch { conversationRepository.deleteConversation(appId, chatId) }
        }

        fun clearConversations() {
            viewModelScope.launch { conversationRepository.clearConversations(appId) }
        }

        fun renameConversation(
            chatId: String,
            title: String,
        ) {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                return
            }
            viewModelScope.launch { conversationRepository.renameConversation(appId, chatId, trimmed) }
        }

        fun togglePin(
            chatId: String,
            pinned: Boolean,
        ) {
            viewModelScope.launch { conversationRepository.togglePinConversation(appId, chatId, pinned) }
        }

        fun createFolder(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                return
            }
            viewModelScope.launch { conversationRepository.createFolder(appId, trimmed) }
        }

        fun renameFolder(
            folderId: String,
            name: String,
        ) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                return
            }
            viewModelScope.launch { conversationRepository.renameFolder(appId, folderId, trimmed) }
        }

        fun deleteFolder(folderId: String) {
            viewModelScope.launch {
                conversationRepository.deleteFolder(appId, folderId)
                if (selectedFolderId.value == folderId) {
                    selectedFolderId.value = null
                }
            }
        }

        fun createTag(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                return
            }
            viewModelScope.launch { conversationRepository.createTag(appId, trimmed) }
        }

        fun renameTag(
            tagId: String,
            name: String,
        ) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                return
            }
            viewModelScope.launch { conversationRepository.renameTag(appId, tagId, trimmed) }
        }

        fun deleteTag(tagId: String) {
            viewModelScope.launch {
                conversationRepository.deleteTag(appId, tagId)
                if (selectedTagId.value == tagId) {
                    selectedTagId.value = null
                }
            }
        }

        fun moveConversation(
            chatId: String,
            folderId: String?,
        ) {
            viewModelScope.launch { conversationRepository.moveConversationToFolder(appId, chatId, folderId) }
        }

        fun setConversationTags(
            chatId: String,
            tagIds: List<String>,
        ) {
            viewModelScope.launch { conversationRepository.setConversationTags(appId, chatId, tagIds) }
        }
    }

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val chatRepository: ChatRepository,
        private val strings: ChatStrings,
    ) : ViewModel() {
        private val appId: String = checkNotNull(savedStateHandle["appId"])
        private val chatId = MutableStateFlow(savedStateHandle.get<String?>("chatId"))
        private val input = MutableStateFlow("")
        private val metaState = MutableStateFlow(ChatMetaState())

        init {
            viewModelScope.launch {
                val existingChatId = chatId.value
                if (existingChatId != null) {
                    runCatching { chatRepository.restoreMessages(appId, existingChatId) }
                        .onSuccess {
                            metaState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = null,
                                )
                            }
                        }
                        .onFailure { error ->
                            metaState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: strings.restoreFailed(),
                                )
                            }
                        }
                } else {
                    runCatching { chatRepository.bootstrapChat(appId) }
                        .onSuccess { bootstrap ->
                            chatId.value = bootstrap.chatId
                            metaState.update {
                                it.copy(
                                    isLoading = false,
                                    welcomeText = bootstrap.welcomeText,
                                    errorMessage = null,
                                )
                            }
                        }.onFailure { error ->
                            metaState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: strings.bootstrapFailed(),
                                )
                            }
                        }
                }
            }
        }

        val uiState: StateFlow<ChatUiState> =
            combine(
                input,
                metaState,
                observeMessages(),
            ) { currentInput, meta, messages ->
                val pendingInteractiveCard =
                    messages.lastOrNull { it.interactiveCard?.status == InteractiveCardStatus.Pending }?.interactiveCard
                ChatUiState(
                    appId = appId,
                    chatId = chatId.value,
                    welcomeText = meta.welcomeText,
                    input = currentInput,
                    isSending = meta.isSending,
                    errorMessage = meta.errorMessage,
                    messages = messages,
                    isInitialLoading = meta.isLoading && messages.isEmpty(),
                    pendingInteractiveCard = pendingInteractiveCard,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ChatUiState(
                    appId = appId,
                    chatId = chatId.value,
                    welcomeText = metaState.value.welcomeText,
                    isInitialLoading = metaState.value.isLoading,
                ),
            )

        fun updateInput(value: String) {
            input.value = value
        }

        fun send() {
            val text = input.value.trim()
            if (text.isEmpty() || metaState.value.isSending || uiState.value.canStopGeneration) {
                return
            }

            viewModelScope.launch {
                metaState.update { it.copy(isSending = true, errorMessage = null) }
                runCatching {
                    chatRepository.sendMessage(
                        appId = appId,
                        chatId = chatId.value,
                        text = text,
                    )
                }.onSuccess { resolvedChatId ->
                    chatId.value = resolvedChatId
                    input.value = ""
                    metaState.update { it.copy(isSending = false) }
                }.onFailure { error ->
                    metaState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: strings.sendFailed(),
                        )
                    }
                }
            }
        }

        fun stopGeneration() {
            val resolvedChatId = chatId.value ?: return
            viewModelScope.launch {
                chatRepository.stopStreaming(appId, resolvedChatId)
            }
        }

        fun continueGeneration() {
            val resolvedChatId = chatId.value ?: return
            viewModelScope.launch {
                runCatching {
                    chatRepository.continueGeneration(appId, resolvedChatId)
                }.onFailure { error ->
                    metaState.update { it.copy(errorMessage = error.message ?: strings.continueFailed()) }
                }
            }
        }

        fun regenerate(message: ChatMessageUiModel) {
            val resolvedChatId = chatId.value ?: return
            viewModelScope.launch {
                metaState.update { it.copy(errorMessage = null) }
                runCatching {
                    chatRepository.regenerateResponse(appId, resolvedChatId, message.messageId)
                }.onFailure { error ->
                    metaState.update { it.copy(errorMessage = error.message ?: strings.regenerateFailed()) }
                }
            }
        }

        fun updateFeedback(
            message: ChatMessageUiModel,
            feedback: MessageFeedback,
        ) {
            viewModelScope.launch {
                chatRepository.setFeedback(
                    appId = appId,
                    chatId = message.chatId,
                    messageId = message.messageId,
                    feedback = feedback,
                )
            }
        }

        fun updateInteractiveDraft(
            message: ChatMessageUiModel,
            value: String,
        ) {
            val resolvedChatId = chatId.value ?: message.chatId
            val card = message.interactiveCard ?: return
            viewModelScope.launch {
                chatRepository.savePendingInteractiveDraft(
                    appId = appId,
                    chatId = resolvedChatId,
                    card = card,
                    draftPayloadJson = """{"value":${JsonPrimitive(value)}}""",
                )
            }
        }

        fun submitInteractiveResponse(
            message: ChatMessageUiModel,
            value: String,
        ) {
            val resolvedChatId = chatId.value ?: message.chatId
            val card = message.interactiveCard ?: return
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                return
            }
            viewModelScope.launch {
                metaState.update { it.copy(isSending = true, errorMessage = null) }
                runCatching {
                    chatRepository.submitInteractiveResponse(
                        appId = appId,
                        chatId = resolvedChatId,
                        card = card,
                        value = trimmed,
                    )
                }.onSuccess { updatedChatId ->
                    chatId.value = updatedChatId
                    metaState.update { it.copy(isSending = false, errorMessage = null) }
                }.onFailure { error ->
                    metaState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: strings.sendFailed(),
                        )
                    }
                }
            }
        }

        private fun observeMessages(): Flow<List<ChatMessageUiModel>> =
            chatId.flatMapLatest { resolvedChatId ->
                if (resolvedChatId == null) {
                    flowOf(emptyList())
                } else {
                    chatRepository.observeMessages(appId, resolvedChatId)
                }
            }
    }

private data class ChatMetaState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val welcomeText: String? = null,
)

private data class ConversationListFilters(
    val query: String = "",
    val folderId: String? = null,
    val tagId: String? = null,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class AppSelectorViewModel
    @Inject
    constructor(
        private val api: FastGptApi,
        private val connectionRepository: ConnectionRepository,
        private val strings: ChatStrings,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(AppSelectorUiState())
        val uiState: StateFlow<AppSelectorUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                connectionRepository.snapshot.collect { snapshot ->
                    mutableState.update { it.copy(currentAppId = snapshot.selectedAppId) }
                    refresh()
                }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                runCatching { api.getAppList().data.orEmpty() }
                    .onSuccess { apps ->
                        val currentAppId = connectionRepository.snapshot.value.selectedAppId
                        mutableState.value =
                            AppSelectorUiState(
                                currentAppId = currentAppId,
                                currentAppName = apps.firstOrNull { it.id == currentAppId }?.name.orEmpty(),
                                items =
                                    apps.map {
                                        AppSelectorItemUiModel(
                                            appId = it.id,
                                            name = it.name,
                                            intro = it.intro,
                                        )
                                    },
                            )
                    }.onFailure { error ->
                        mutableState.update { it.copy(errorMessage = error.message ?: strings.loadAppsFailed()) }
                    }
            }
        }

        fun switchApp(
            appId: String,
            onSwitched: (String) -> Unit,
        ) {
            viewModelScope.launch {
                connectionRepository.updateSelectedAppId(appId)
                onSwitched(appId)
            }
        }
    }

@HiltViewModel
class AppDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val api: FastGptApi,
        private val strings: ChatStrings,
    ) : ViewModel() {
        private val appId: String = checkNotNull(savedStateHandle["appId"])
        private val chatId: String? = savedStateHandle["chatId"]
        private val mutableState = MutableStateFlow(AppDetailUiState(appId = appId))
        val uiState: StateFlow<AppDetailUiState> = mutableState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                mutableState.update { it.copy(isLoading = true, errorMessage = null) }
                runCatching {
                    val apps = api.getAppList().data.orEmpty()
                    val summary = apps.firstOrNull { it.id == appId }
                    val initData = api.initChat(appId = appId, chatId = chatId).data
                    AppDetailUiState(
                        appId = appId,
                        appName = summary?.name ?: initData?.app?.readString("name").orEmpty(),
                        intro = summary?.intro ?: initData?.app?.readString("intro").orEmpty(),
                        type = summary?.type ?: initData?.app?.readString("type").orEmpty(),
                        welcomeText = initData?.welcomeText,
                        sections =
                            buildList {
                                initData?.variables?.takeIf { it.isNotEmpty() }?.let { variables ->
                                    add(AppDetailSectionUiModel("变量", listOf("${variables.size} 个变量已配置")))
                                }
                                initData?.chatModels?.takeIf { it.isNotEmpty() }?.let { models ->
                                    add(AppDetailSectionUiModel("模型", models.toDisplayItems()))
                                }
                                initData?.fileSelectConfig?.takeIf { it.isNotEmpty() }?.let { config ->
                                    add(AppDetailSectionUiModel("附件", config.toDisplayItems()))
                                }
                                initData?.ttsConfig?.takeIf { it.isNotEmpty() }?.let { config ->
                                    add(AppDetailSectionUiModel("语音播报", config.toDisplayItems()))
                                }
                                initData?.whisperConfig?.takeIf { it.isNotEmpty() }?.let { config ->
                                    add(AppDetailSectionUiModel("语音输入", config.toDisplayItems()))
                                }
                                initData?.questionGuide?.let { config ->
                                    add(
                                        AppDetailSectionUiModel(
                                            "推荐问题",
                                            listOf(
                                                if (config.open) {
                                                    "已开启"
                                                } else {
                                                    "未开启"
                                                },
                                            ),
                                        ),
                                    )
                                }
                            },
                        isLoading = false,
                        errorMessage = null,
                    )
                }.onSuccess { mutableState.value = it }
                    .onFailure { error ->
                        mutableState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: strings.loadAppDetailFailed(),
                            )
                        }
                    }
            }
        }
    }

open class ChatStrings
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        open fun restoreFailed(): String = context.getString(R.string.chat_error_restore_failed)

        open fun bootstrapFailed(): String = context.getString(R.string.chat_error_bootstrap_failed)

        open fun sendFailed(): String = context.getString(R.string.chat_error_send_failed)

        open fun continueFailed(): String = context.getString(R.string.chat_error_continue_failed)

        open fun regenerateFailed(): String = context.getString(R.string.chat_error_regenerate_failed)

        open fun loadAppsFailed(): String = context.getString(R.string.chat_error_load_apps_failed)

        open fun loadAppDetailFailed(): String = context.getString(R.string.chat_error_load_app_detail_failed)
    }

private fun JsonArray.toDisplayItems(): List<String> = mapNotNull { element -> element.toString().trim('"').takeIf { it.isNotBlank() } }

private fun JsonObject.toDisplayItems(): List<String> =
    entries.mapNotNull { (key, value) ->
        val rendered =
            value.toString()
                .trim('"')
                .replace("\n", " ")
                .take(48)
                .ifBlank { return@mapNotNull null }
        "$key: $rendered"
    }

private fun JsonObject.readString(key: String): String = this[key]?.toString()?.trim('"').orEmpty()
