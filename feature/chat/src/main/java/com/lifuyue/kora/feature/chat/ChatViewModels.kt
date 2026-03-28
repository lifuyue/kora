package com.lifuyue.kora.feature.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.network.FastGptApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.Job
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
        @ApplicationContext private val context: Context,
        private val chatRepository: ChatRepository,
        private val chatAudioPreferencesSource: ChatAudioPreferencesSource,
        private val speechRecognitionEngine: SpeechRecognitionEngine,
        private val ttsPlaybackController: TtsPlaybackController,
        private val strings: ChatStrings,
    ) : ViewModel() {
        private val appId: String = checkNotNull(savedStateHandle["appId"])
        private val chatId = MutableStateFlow(savedStateHandle.get<String?>("chatId"))
        private val input = MutableStateFlow("")
        private val metaState = MutableStateFlow(ChatMetaState())
        private val attachments = MutableStateFlow<List<AttachmentDraftUiModel>>(emptyList())
        private val attachmentConfig = MutableStateFlow(ChatAttachmentConfig())
        private val speechInputState = MutableStateFlow(SpeechInputUiState())
        private val uploadJobs = linkedMapOf<String, Job>()
        private var speechSession: SpeechRecognitionSession? = null
        private var speechSessionToken = 0L
        private var activeSpeechSessionId: Long? = null
        private var speechDraftBeforeStart: String = ""

        init {
            viewModelScope.launch {
                val existingChatId = chatId.value
                runCatching { chatRepository.bootstrapChat(appId, existingChatId) }
                    .onSuccess { bootstrap ->
                        if (existingChatId == null) {
                            chatId.value = bootstrap.chatId
                        }
                        attachmentConfig.value = bootstrap.attachmentConfig
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

                if (existingChatId != null) {
                    runCatching { chatRepository.restoreMessages(appId, existingChatId) }
                        .onSuccess {
                            metaState.update { it.copy(errorMessage = null) }
                        }.onFailure { error ->
                            metaState.update { it.copy(errorMessage = error.message ?: strings.restoreFailed()) }
                        }
                }
            }
        }

        val uiState: StateFlow<ChatUiState> =
            combine(
                input,
                metaState,
                observeMessages(),
                attachments,
                attachmentConfig,
                speechInputState,
                ttsPlaybackController.state,
            ) { values ->
                val currentInput = values[0] as String
                val meta = values[1] as ChatMetaState
                val messages = values[2] as List<ChatMessageUiModel>
                val currentAttachments = values[3] as List<AttachmentDraftUiModel>
                val currentAttachmentConfig = values[4] as ChatAttachmentConfig
                val currentSpeechInputState = values[5] as SpeechInputUiState
                val currentTtsPlaybackState = values[6] as TtsPlaybackUiState
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
                    attachments = currentAttachments,
                    attachmentConfig = currentAttachmentConfig,
                    pendingInteractiveCard = pendingInteractiveCard,
                    speechInputState = currentSpeechInputState,
                    ttsPlaybackState = currentTtsPlaybackState,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ChatUiState(
                    appId = appId,
                    chatId = chatId.value,
                    welcomeText = metaState.value.welcomeText,
                    isInitialLoading = metaState.value.isLoading,
                    attachments = attachments.value,
                    attachmentConfig = attachmentConfig.value,
                    speechInputState = speechInputState.value,
                    ttsPlaybackState = ttsPlaybackController.state.value,
                ),
            )

        fun updateInput(value: String) {
            input.value = value
        }

        fun startSpeechInput() {
            if (speechInputState.value.status == SpeechInputStatus.Recording || speechInputState.value.status == SpeechInputStatus.Recognizing) {
                return
            }

            val audioPreferences = chatAudioPreferencesSource.currentAudioPreferences()
            speechDraftBeforeStart = input.value
            val sessionId = ++speechSessionToken
            activeSpeechSessionId = sessionId
            speechInputState.value =
                SpeechInputUiState(
                    status = SpeechInputStatus.Recording,
                    transcript = "",
                    errorMessage = null,
                )
            val session =
                speechRecognitionEngine.start(
                    speechToTextEngine = audioPreferences.speechToTextEngine,
                    onPartialTranscript = { transcript ->
                        if (isCurrentSpeechSession(sessionId)) {
                            input.value = transcript
                            speechInputState.value =
                                speechInputState.value.copy(
                                    status = SpeechInputStatus.Recording,
                                    transcript = transcript,
                                    errorMessage = null,
                                )
                        }
                    },
                    onFinalTranscript = { transcript ->
                        if (isCurrentSpeechSession(sessionId)) {
                            handleSpeechFinalTranscript(
                                transcript = transcript,
                                autoSendTranscripts = audioPreferences.autoSendTranscripts,
                                sessionId = sessionId,
                            )
                        }
                    },
                    onError = { error ->
                        if (isCurrentSpeechSession(sessionId)) {
                            handleSpeechError(error, sessionId)
                        }
                    },
                )
            if (isCurrentSpeechSession(sessionId)) {
                speechSession = session
            }
        }

        fun stopSpeechInput() {
            if (speechInputState.value.status != SpeechInputStatus.Recording) {
                return
            }
            speechInputState.value =
                speechInputState.value.copy(
                    status = SpeechInputStatus.Recognizing,
                    errorMessage = null,
                )
            speechSession?.stop()
        }

        fun cancelSpeechInput() {
            if (speechInputState.value.status == SpeechInputStatus.Idle) {
                return
            }
            speechSession?.cancel()
            finishSpeechSession()
            input.value = speechDraftBeforeStart
            speechInputState.value = SpeechInputUiState()
        }

        fun onSpeechPermissionDenied() {
            finishSpeechSession()
            input.value = speechDraftBeforeStart
            speechInputState.value =
                SpeechInputUiState(
                    status = SpeechInputStatus.Error,
                    transcript = speechDraftBeforeStart,
                    errorMessage = strings.speechPermissionRequired(),
                )
        }

        fun playMessage(
            messageId: String,
            text: String,
        ) {
            val currentMessageId = ttsPlaybackController.state.value.messageId
            if (currentMessageId != null && currentMessageId != messageId) {
                ttsPlaybackController.stop()
            }
            ttsPlaybackController.play(
                TtsPlaybackRequest(
                    messageId = messageId,
                    text = text,
                    audioPreferences = chatAudioPreferencesSource.currentAudioPreferences(),
                ),
            )
        }

        fun pausePlayback() {
            ttsPlaybackController.pause()
        }

        fun stopPlayback() {
            ttsPlaybackController.stop()
        }

        fun onHostStopped() {
            ttsPlaybackController.stop()
        }

        private fun isCurrentSpeechSession(sessionId: Long): Boolean = activeSpeechSessionId == sessionId

        private fun finishSpeechSession(sessionId: Long? = null) {
            if (sessionId == null || speechSessionToken == sessionId) {
                speechSession = null
                activeSpeechSessionId = null
            }
        }

        private fun handleSpeechFinalTranscript(
            transcript: String,
            autoSendTranscripts: Boolean,
            sessionId: Long,
        ) {
            input.value = transcript
            speechInputState.value =
                SpeechInputUiState(
                    status = SpeechInputStatus.Idle,
                    transcript = transcript,
                    errorMessage = null,
                )
            finishSpeechSession(sessionId)
            if (autoSendTranscripts && transcript.isNotBlank()) {
                sendDraftFromSpeech()
            }
        }

        private fun handleSpeechError(
            error: SpeechRecognitionError,
            sessionId: Long,
        ) {
            finishSpeechSession(sessionId)
            input.value = speechDraftBeforeStart
            speechInputState.value =
                SpeechInputUiState(
                    status = SpeechInputStatus.Error,
                    transcript = speechDraftBeforeStart,
                    errorMessage =
                        when (error) {
                            is SpeechRecognitionError.PermissionDenied -> strings.speechPermissionRequired()
                            is SpeechRecognitionError.Unavailable -> strings.speechUnavailable()
                            is SpeechRecognitionError.RecognitionFailed -> strings.speechFailed()
                        },
                )
        }

        private fun sendDraftFromSpeech() {
            sendCurrentDraft(clearSpeechStateAfterSuccess = true)
        }

        private fun sendCurrentDraft(clearSpeechStateAfterSuccess: Boolean) {
            val text = input.value.trim()
            val sendableAttachments = attachments.value.filter { it.uploadStatus == AttachmentUploadStatus.Uploaded && it.uploadedRef != null }
            if (
                metaState.value.isSending ||
                    uiState.value.canStopGeneration ||
                    attachments.value.any { it.uploadStatus == AttachmentUploadStatus.Uploading || it.uploadStatus == AttachmentUploadStatus.Failed } ||
                    (text.isEmpty() && sendableAttachments.isEmpty())
            ) {
                return
            }

            viewModelScope.launch {
                metaState.update { it.copy(isSending = true, errorMessage = null) }
                runCatching {
                    chatRepository.sendMessage(
                        appId = appId,
                        chatId = chatId.value,
                        text = text,
                        attachments = sendableAttachments,
                    )
                }.onSuccess { resolvedChatId ->
                    chatId.value = resolvedChatId
                    input.value = ""
                    attachments.value = emptyList()
                    metaState.update { it.copy(isSending = false) }
                    if (clearSpeechStateAfterSuccess) {
                        speechInputState.value = SpeechInputUiState()
                        speechDraftBeforeStart = ""
                    }
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

        fun addAttachments(uris: List<Uri>) {
            val config = attachmentConfig.value
            if (uris.isEmpty() || !config.hasAnySelectionType) {
                return
            }

            viewModelScope.launch {
                val currentAttachments = attachments.value
                val availableSlots = config.maxFiles - currentAttachments.size
                if (availableSlots <= 0) {
                    metaState.update { it.copy(errorMessage = strings.attachmentLimitReached(config.maxFiles)) }
                    return@launch
                }

                val accepted =
                    uris.asSequence()
                        .map { uri ->
                            val metadata = resolveAttachmentMetadata(context, uri)
                            Triple(uri, metadata, config.canAcceptSelection(metadata.mimeType, metadata.displayName))
                        }
                        .filter { (_, metadata, allowed) -> allowed && config.canAddMore(attachments.value.size) && metadata.displayName.isNotBlank() }
                        .take(availableSlots)
                        .toList()

                if (accepted.isEmpty()) {
                    metaState.update { it.copy(errorMessage = strings.attachmentTypeNotAllowed()) }
                    return@launch
                }

                val drafts =
                    accepted.map { (uri, metadata, _) ->
                        AttachmentDraftUiModel(
                            displayName = metadata.displayName,
                            localUri = uri.toString(),
                            mimeType = metadata.mimeType,
                            sizeBytes = metadata.sizeBytes,
                            kind = resolveAttachmentKind(metadata.mimeType, metadata.displayName),
                        )
                    }

                attachments.update { currentAttachments + drafts }
                drafts.forEach { startAttachmentUpload(it) }
            }
        }

        fun removeAttachment(localUri: String) {
            uploadJobs.remove(localUri)?.cancel()
            attachments.update { items -> items.filterNot { it.localUri == localUri } }
        }

        fun retryAttachment(localUri: String) {
            val attachment = attachments.value.firstOrNull { it.localUri == localUri } ?: return
            startAttachmentUpload(
                attachment.copy(
                    uploadStatus = AttachmentUploadStatus.Idle,
                    progress = 0f,
                    errorMessage = null,
                    uploadedRef = null,
                ),
            )
        }

        fun cancelAttachmentUpload(localUri: String) {
            uploadJobs.remove(localUri)?.cancel()
            attachments.update { items ->
                items.map { attachment ->
                    if (attachment.localUri == localUri) {
                        attachment.copy(uploadStatus = AttachmentUploadStatus.Cancelled, errorMessage = null)
                    } else {
                        attachment
                    }
                }
            }
        }

        fun send() {
            sendCurrentDraft(clearSpeechStateAfterSuccess = false)
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
                    draftPayloadJson = value,
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

        private fun startAttachmentUpload(attachment: AttachmentDraftUiModel) {
            val resolvedChatId = chatId.value
            uploadJobs.remove(attachment.localUri)?.cancel()
            uploadJobs[attachment.localUri] =
                viewModelScope.launch {
                    attachments.update { items ->
                        items.map { current ->
                            if (current.localUri == attachment.localUri) {
                                current.copy(uploadStatus = AttachmentUploadStatus.Uploading, progress = 0f, errorMessage = null)
                            } else {
                                current
                            }
                        }
                    }
                    try {
                        val uploaded =
                            chatRepository.uploadAttachment(
                                appId = appId,
                                chatId = resolvedChatId,
                                attachment = attachment,
                            ) { progress ->
                                attachments.update { items ->
                                    items.map { current ->
                                        if (current.localUri == attachment.localUri) {
                                            current.copy(progress = progress.coerceIn(0f, 1f))
                                        } else {
                                            current
                                        }
                                    }
                                }
                            }
                        attachments.update { items ->
                            items.map { current ->
                                if (current.localUri == attachment.localUri) {
                                    current.copy(
                                        uploadStatus = AttachmentUploadStatus.Uploaded,
                                        uploadedRef = uploaded,
                                        progress = 1f,
                                        errorMessage = null,
                                    )
                                } else {
                                    current
                                }
                            }
                        }
                    } catch (_: CancellationException) {
                        attachments.update { items ->
                            items.map { current ->
                                if (current.localUri == attachment.localUri) {
                                    current.copy(uploadStatus = AttachmentUploadStatus.Cancelled)
                                } else {
                                    current
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        attachments.update { items ->
                            items.map { current ->
                                if (current.localUri == attachment.localUri) {
                                    current.copy(
                                        uploadStatus = AttachmentUploadStatus.Failed,
                                        errorMessage = error.message ?: strings.attachmentUploadFailed(),
                                    )
                                } else {
                                    current
                                }
                            }
                        }
                    } finally {
                        uploadJobs.remove(attachment.localUri)
                    }
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
        open fun restoreFailed(): String = context.chatString("chat_error_restore_failed")

        open fun bootstrapFailed(): String = context.chatString("chat_error_bootstrap_failed")

        open fun sendFailed(): String = context.chatString("chat_error_send_failed")

        open fun continueFailed(): String = context.chatString("chat_error_continue_failed")

        open fun regenerateFailed(): String = context.chatString("chat_error_regenerate_failed")

        open fun loadAppsFailed(): String = context.chatString("chat_error_load_apps_failed")

        open fun loadAppDetailFailed(): String = context.chatString("chat_error_load_app_detail_failed")

        open fun attachmentTypeNotAllowed(): String = context.chatString("chat_error_attachment_type_not_allowed")

        open fun attachmentLimitReached(maxFiles: Int): String =
            context.chatString("chat_error_attachment_limit_reached", maxFiles)

        open fun attachmentUploadFailed(): String = context.chatString("chat_error_attachment_upload_failed")

        open fun speechPermissionRequired(): String = context.appString("chat_error_speech_permission_required")

        open fun speechUnavailable(): String = context.appString("chat_error_speech_unavailable")

        open fun speechFailed(): String = context.appString("chat_error_speech_failed")
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
