package com.lifuyue.kora.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.AudioPreferences
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendUsesRepositoryAndClearsComposer() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val strings = FakeChatStrings()
            val viewModel =
                createViewModel(
                    repository = repository,
                    strings = strings,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("你好")
            viewModel.send()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.input)
            assertEquals("chat-1", state.chatId)
            assertEquals(2, state.messages.size)
            assertEquals("你好", repository.sentText)
            collectJob.cancel()
        }

    @Test
    fun sendFailureShowsInlineError() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository(shouldFailSend = true)
            val strings = FakeChatStrings()
            val viewModel =
                createViewModel(
                    repository = repository,
                    strings = strings,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("失败案例")
            viewModel.send()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(strings.sendFailed(), state.errorMessage)
            assertEquals("失败案例", state.input)
            collectJob.cancel()
        }

    @Test
    fun sendIsBlockedWhileAssistantIsStreaming() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                    chatId = "chat-1",
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            repository.emitMessages(
                "chat-1",
                listOf(
                    ChatMessageUiModel(
                        messageId = "assistant-1",
                        chatId = "chat-1",
                        appId = "app-1",
                        role = ChatRole.AI,
                        markdown = "streaming",
                        isStreaming = true,
                        deliveryState = MessageDeliveryState.Streaming,
                    ),
                ),
            )
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canSend)

            viewModel.updateInput("should not send")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(null, repository.sentText)
            assertEquals("should not send", viewModel.uiState.value.input)
            collectJob.cancel()
        }

    @Test
    fun regenerateStopAndFeedbackDelegateToRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                    chatId = "chat-1",
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            val message =
                ChatMessageUiModel(
                    messageId = "assistant-1",
                    chatId = "chat-1",
                    appId = "app-1",
                    role = ChatRole.AI,
                    markdown = "hello",
                )
            repository.emitMessages("chat-1", listOf(message))

            viewModel.regenerate(message)
            viewModel.stopGeneration()
            viewModel.updateFeedback(message, MessageFeedback.Downvote)
            advanceUntilIdle()

            assertEquals("assistant-1", repository.regeneratedMessageId)
            assertEquals("chat-1", repository.stoppedChatId)
            assertEquals(MessageFeedback.Downvote, repository.feedback)
            collectJob.cancel()
        }

    @Test
    fun continueGenerationDelegatesToRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                    chatId = "chat-1",
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.continueGeneration()
            advanceUntilIdle()

            assertEquals("chat-1", repository.continuedChatId)
            collectJob.cancel()
        }

    @Test
    fun addAttachmentsUploadsAndEnforcesLimit() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val strings = FakeChatStrings()
            val repository = RecordingChatRepository(
                bootstrapAttachmentConfig =
                    ChatAttachmentConfig(
                        maxFiles = 1,
                        canSelectCustomFileExtension = true,
                        customFileExtensionList = listOf(".png"),
                    ),
            )
            val viewModel =
                createViewModel(
                    repository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addAttachments(listOf(android.net.Uri.parse("content://local/photo.png")))
            advanceUntilIdle()

            val uploaded = viewModel.uiState.value.attachments.single()
            assertEquals("photo.png", uploaded.displayName)
            assertEquals(AttachmentUploadStatus.Uploaded, uploaded.uploadStatus)
            assertNotNull(uploaded.uploadedRef)

            viewModel.addAttachments(listOf(android.net.Uri.parse("content://local/second.png")))
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.attachments.size)
            assertEquals(strings.attachmentLimitReached(1), viewModel.uiState.value.errorMessage)
            collectJob.cancel()
        }

    @Test
    fun failedAttachmentCanRetryAndCancelledAttachmentStopsUploading() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val strings = FakeChatStrings()
            val startedUpload = CompletableDeferred<Unit>()
            val attemptsByName = mutableMapOf<String, Int>()
            val repository =
                RecordingChatRepository(
                    bootstrapAttachmentConfig =
                        ChatAttachmentConfig(
                            maxFiles = 2,
                            canSelectCustomFileExtension = true,
                        customFileExtensionList = listOf(".png"),
                    ),
                    uploadBehavior = { attachment, onProgress ->
                        onProgress(0.5f)
                        val attempt = (attemptsByName[attachment.displayName] ?: 0) + 1
                        attemptsByName[attachment.displayName] = attempt
                        if (attachment.displayName == "cancel.png") {
                            startedUpload.complete(Unit)
                            CompletableDeferred<Unit>().await()
                        }
                        if (attachment.displayName == "fail.png" && attempt == 1) {
                            throw IllegalStateException()
                        }
                        com.lifuyue.kora.core.network.UploadedAssetRef(
                            name = attachment.displayName,
                            url = "https://example.com/${attachment.displayName}",
                            key = attachment.localUri,
                            mimeType = attachment.mimeType,
                            size = attachment.sizeBytes ?: 0L,
                        )
                    },
                )
            val viewModel =
                createViewModel(
                    repository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addAttachments(listOf(android.net.Uri.parse("content://local/fail.png")))
            advanceUntilIdle()
            val failed = viewModel.uiState.value.attachments.single()
            assertEquals(AttachmentUploadStatus.Failed, failed.uploadStatus)
            assertTrue(failed.errorMessage?.isNotBlank() == true)

            viewModel.retryAttachment(failed.localUri)
            advanceUntilIdle()
            val retried = viewModel.uiState.value.attachments.single()
            assertEquals(AttachmentUploadStatus.Uploaded, retried.uploadStatus)
            assertNotNull(retried.uploadedRef)

            viewModel.addAttachments(listOf(android.net.Uri.parse("content://local/cancel.png")))
            startedUpload.await()
            viewModel.cancelAttachmentUpload("content://local/cancel.png")
            advanceUntilIdle()
            val cancelled = viewModel.uiState.value.attachments.first { it.localUri == "content://local/cancel.png" }
            assertEquals(AttachmentUploadStatus.Cancelled, cancelled.uploadStatus)

            collectJob.cancel()
        }

    @Test
    fun attachmentOnlySendUsesUploadedAttachments() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                RecordingChatRepository(
                    bootstrapAttachmentConfig =
                        ChatAttachmentConfig(
                            maxFiles = 2,
                            canSelectCustomFileExtension = true,
                            customFileExtensionList = listOf(".png"),
                        ),
                )
            val viewModel =
                createViewModel(
                    repository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.addAttachments(listOf(android.net.Uri.parse("content://local/photo.png")))
            advanceUntilIdle()
            viewModel.send()
            advanceUntilIdle()

            assertEquals("", repository.sentText)
            assertEquals(0, viewModel.uiState.value.attachments.size)
            collectJob.cancel()
        }

    @Test
    fun uiStateExposesFoundationDefaultsAfterBootstrap() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(emptyList<AttachmentDraftUiModel>(), state.attachments)
            assertEquals(null, state.pendingInteractiveCard)
            assertEquals(SpeechInputStatus.Idle, state.speechInputState.status)
            assertEquals(TtsPlaybackStatus.Idle, state.ttsPlaybackState.status)
            assertEquals(null, state.shareExportState.selection)
            collectJob.cancel()
        }

    @Test
    fun uiStatePromotesLastPendingInteractiveCardFromMessages() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                    chatId = "chat-1",
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            val card =
                InteractiveCardUiModel(
                    kind = InteractiveCardKind.UserSelect,
                    messageDataId = "assistant-1",
                    responseValueId = "response-1",
                    options = listOf("Alpha", "Beta"),
                )
            repository.emitMessages(
                "chat-1",
                listOf(
                    ChatMessageUiModel(
                        messageId = "assistant-1",
                        chatId = "chat-1",
                        appId = "app-1",
                        role = ChatRole.AI,
                        markdown = "请选择",
                        interactiveCard = card,
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals(card, viewModel.uiState.value.pendingInteractiveCard)
            collectJob.cancel()
        }

    @Test
    fun submitInteractiveResponseDelegatesToRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                    chatId = "chat-1",
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            val message =
                ChatMessageUiModel(
                    messageId = "assistant-1",
                    chatId = "chat-1",
                    appId = "app-1",
                    role = ChatRole.AI,
                    markdown = "请选择",
                    interactiveCard =
                        InteractiveCardUiModel(
                            kind = InteractiveCardKind.UserSelect,
                            messageDataId = "assistant-1",
                            responseValueId = "response-1",
                            options = listOf("Alpha"),
                        ),
                )

            viewModel.submitInteractiveResponse(message, "Alpha")
            advanceUntilIdle()

            assertEquals("chat-1", repository.submittedInteractiveChatId)
            assertEquals("assistant-1", repository.submittedInteractiveCard?.messageDataId)
            assertEquals("Alpha", repository.submittedInteractiveValue)
            collectJob.cancel()
        }

    @Test
    fun speechInputStateBackfillsDraftAndAutoSendsFinalTranscript() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val audioPreferencesSource =
                FakeChatAudioPreferencesSource(
                    AudioPreferences(
                        speechToTextEngine = SpeechToTextEngine.System,
                        autoSendTranscripts = true,
                    ),
                )
            val speechEngine = FakeSpeechRecognitionEngine()
            val viewModel =
                createViewModel(
                    repository = repository,
                    audioPreferencesSource = audioPreferencesSource,
                    speechRecognitionEngine = speechEngine,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("draft")
            viewModel.startSpeechInput()
            advanceUntilIdle()
            assertEquals(SpeechInputStatus.Recording, viewModel.uiState.value.speechInputState.status)
            assertEquals(SpeechToTextEngine.System, speechEngine.lastSpeechToTextEngine)

            speechEngine.emitPartial("partial transcript")
            advanceUntilIdle()
            assertEquals("partial transcript", viewModel.uiState.value.input)
            assertEquals("partial transcript", viewModel.uiState.value.speechInputState.transcript)

            speechEngine.emitFinal("final transcript")
            advanceUntilIdle()

            assertEquals("final transcript", repository.sentText)
            assertEquals("", viewModel.uiState.value.input)
            assertEquals(SpeechInputStatus.Idle, viewModel.uiState.value.speechInputState.status)
            collectJob.cancel()
        }

    @Test
    fun speechInputStateStopAndCancelRestoreDraft() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val audioPreferencesSource = FakeChatAudioPreferencesSource()
            val speechEngine = FakeSpeechRecognitionEngine()
            val viewModel =
                createViewModel(
                    repository = repository,
                    audioPreferencesSource = audioPreferencesSource,
                    speechRecognitionEngine = speechEngine,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("draft")
            viewModel.startSpeechInput()
            advanceUntilIdle()
            speechEngine.emitPartial("live transcript")
            advanceUntilIdle()

            viewModel.stopSpeechInput()
            advanceUntilIdle()
            assertTrue(speechEngine.lastSession?.stopCalled == true)
            assertEquals(SpeechInputStatus.Recognizing, viewModel.uiState.value.speechInputState.status)

            viewModel.cancelSpeechInput()
            advanceUntilIdle()
            assertTrue(speechEngine.lastSession?.cancelCalled == true)
            assertEquals("draft", viewModel.uiState.value.input)
            assertEquals(SpeechInputStatus.Idle, viewModel.uiState.value.speechInputState.status)
            collectJob.cancel()
        }

    @Test
    fun speechPermissionDeniedShowsErrorMessageAndRestoresDraft() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                createViewModel(
                    repository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("draft")
            viewModel.startSpeechInput()
            advanceUntilIdle()
            viewModel.onSpeechPermissionDenied()
            advanceUntilIdle()

            val state = viewModel.uiState.value.speechInputState
            assertEquals(SpeechInputStatus.Error, state.status)
            assertEquals("draft", state.transcript)
            assertEquals("需要麦克风权限", state.errorMessage)
            assertEquals("draft", viewModel.uiState.value.input)
            collectJob.cancel()
        }

    @Test
    fun playMessageStopsPreviousPlaybackBeforeStartingNewOne() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val audioPreferencesSource =
                FakeChatAudioPreferencesSource(
                    AudioPreferences(
                        textToSpeechEngine = TextToSpeechEngine.System,
                        speechRate = 1.25f,
                    ),
                )
            val ttsController = FakeTtsPlaybackController()
            val viewModel =
                createViewModel(
                    repository = repository,
                    audioPreferencesSource = audioPreferencesSource,
                    ttsPlaybackController = ttsController,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.playMessage("msg-1", "First")
            advanceUntilIdle()
            viewModel.playMessage("msg-2", "Second")
            advanceUntilIdle()

            assertEquals(
                listOf("play:msg-1:First", "stop:msg-1", "play:msg-2:Second"),
                ttsController.events,
            )
            assertEquals("msg-2", ttsController.lastRequest?.messageId)
            assertEquals(TextToSpeechEngine.System, ttsController.lastRequest?.audioPreferences?.textToSpeechEngine)
            assertEquals(TtsPlaybackStatus.Playing, viewModel.uiState.value.ttsPlaybackState.status)
            assertEquals("msg-2", viewModel.uiState.value.ttsPlaybackState.messageId)
            collectJob.cancel()
        }

    @Test
    fun hostStoppedClearsPlaybackState() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val ttsController = FakeTtsPlaybackController()
            val viewModel =
                createViewModel(
                    repository = repository,
                    ttsPlaybackController = ttsController,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.playMessage("msg-1", "Hello")
            advanceUntilIdle()
            viewModel.onHostStopped()
            advanceUntilIdle()

            assertEquals(listOf("play:msg-1:Hello", "stop:msg-1"), ttsController.events)
            assertEquals(TtsPlaybackStatus.Stopped, viewModel.uiState.value.ttsPlaybackState.status)
            collectJob.cancel()
        }
}

private class FakeChatStrings : ChatStrings(context = ApplicationProvider.getApplicationContext()) {
    override fun restoreFailed(): String = "恢复历史失败"

    override fun bootstrapFailed(): String = "初始化会话失败"

    override fun sendFailed(): String = "发送失败"

    override fun continueFailed(): String = "继续生成失败"

    override fun regenerateFailed(): String = "重新生成失败"

    override fun loadAppsFailed(): String = "加载 App 失败"

    override fun loadAppDetailFailed(): String = "加载 App 详情失败"

    override fun attachmentTypeNotAllowed(): String = "附件类型不支持"

    override fun attachmentLimitReached(maxFiles: Int): String = "最多只能添加 $maxFiles 个附件"

    override fun attachmentUploadFailed(): String = "附件上传失败"

    override fun speechPermissionRequired(): String = "需要麦克风权限"

    override fun speechUnavailable(): String = "语音识别不可用"

    override fun speechFailed(): String = "语音识别失败"
}

private class FakeChatAudioPreferencesSource(
    private var preferences: AudioPreferences = AudioPreferences(),
) : ChatAudioPreferencesSource {
    override fun currentAudioPreferences(): AudioPreferences = preferences
}

private class FakeSpeechRecognitionEngine : SpeechRecognitionEngine {
    var lastSpeechToTextEngine: SpeechToTextEngine? = null
        private set
    var lastSession: FakeSpeechRecognitionSession? = null
        private set

    private var onPartialTranscript: ((String) -> Unit)? = null
    private var onFinalTranscript: ((String) -> Unit)? = null
    private var onError: ((SpeechRecognitionError) -> Unit)? = null

    override fun start(
        speechToTextEngine: SpeechToTextEngine,
        onPartialTranscript: (String) -> Unit,
        onFinalTranscript: (String) -> Unit,
        onError: (SpeechRecognitionError) -> Unit,
    ): SpeechRecognitionSession {
        lastSpeechToTextEngine = speechToTextEngine
        this.onPartialTranscript = onPartialTranscript
        this.onFinalTranscript = onFinalTranscript
        this.onError = onError
        return FakeSpeechRecognitionSession().also { lastSession = it }
    }

    fun emitPartial(value: String) {
        onPartialTranscript?.invoke(value)
    }

    fun emitFinal(value: String) {
        onFinalTranscript?.invoke(value)
    }

    fun emitError(error: SpeechRecognitionError) {
        onError?.invoke(error)
    }
}

private class FakeSpeechRecognitionSession : SpeechRecognitionSession {
    var stopCalled: Boolean = false
        private set
    var cancelCalled: Boolean = false
        private set

    override fun stop() {
        stopCalled = true
    }

    override fun cancel() {
        cancelCalled = true
    }
}

private class FakeTtsPlaybackController : TtsPlaybackController {
    var lastRequest: TtsPlaybackRequest? = null
        private set
    val events = mutableListOf<String>()
    private val mutableState = MutableStateFlow(TtsPlaybackUiState())

    override val state = mutableState

    override fun play(request: TtsPlaybackRequest) {
        lastRequest = request
        events += "play:${request.messageId}:${request.text}"
        mutableState.value =
            TtsPlaybackUiState(
                messageId = request.messageId,
                status = TtsPlaybackStatus.Playing,
            )
    }

    override fun pause() {
        val messageId = mutableState.value.messageId ?: return
        events += "pause:$messageId"
        mutableState.value = mutableState.value.copy(status = TtsPlaybackStatus.Paused)
    }

    override fun stop() {
        val messageId = mutableState.value.messageId ?: return
        events += "stop:$messageId"
        mutableState.value = mutableState.value.copy(status = TtsPlaybackStatus.Stopped)
    }
}

private fun createViewModel(
    repository: ChatRepository = RecordingChatRepository(),
    chatId: String? = null,
    strings: ChatStrings = FakeChatStrings(),
    audioPreferencesSource: ChatAudioPreferencesSource = FakeChatAudioPreferencesSource(),
    speechRecognitionEngine: SpeechRecognitionEngine = FakeSpeechRecognitionEngine(),
    ttsPlaybackController: TtsPlaybackController = FakeTtsPlaybackController(),
): ChatViewModel =
    ChatViewModel(
        savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to chatId)),
        context = ApplicationProvider.getApplicationContext(),
        chatRepository = repository,
        chatAudioPreferencesSource = audioPreferencesSource,
        speechRecognitionEngine = speechRecognitionEngine,
        ttsPlaybackController = ttsPlaybackController,
        strings = strings,
    )

private class RecordingChatRepository(
    private val shouldFailSend: Boolean = false,
    private val bootstrapAttachmentConfig: ChatAttachmentConfig = ChatAttachmentConfig(),
    private val uploadBehavior: suspend (AttachmentDraftUiModel, (Float) -> Unit) -> com.lifuyue.kora.core.network.UploadedAssetRef =
        { attachment, onProgress ->
            onProgress(1f)
            com.lifuyue.kora.core.network.UploadedAssetRef(
                name = attachment.displayName,
                url = "https://example.com/${attachment.displayName}",
                key = attachment.localUri,
                mimeType = attachment.mimeType,
                size = attachment.sizeBytes ?: 0L,
            )
        },
) : ChatRepository {
    private val messagesByChat =
        MutableStateFlow<Map<String, List<ChatMessageUiModel>>>(emptyMap())

    var sentText: String? = null
    var regeneratedMessageId: String? = null
    var stoppedChatId: String? = null
    var continuedChatId: String? = null
    var feedback: MessageFeedback? = null
    var submittedInteractiveChatId: String? = null
    var submittedInteractiveCard: InteractiveCardUiModel? = null
    var submittedInteractiveValue: String? = null
    var uploadAttempts: Int = 0

    override fun observeMessages(
        appId: String,
        chatId: String?,
    ): Flow<List<ChatMessageUiModel>> = messagesByChat.map { it[chatId].orEmpty() }

    override suspend fun bootstrapChat(
        appId: String,
        chatId: String?,
    ): ChatBootstrap =
        ChatBootstrap(
            chatId = "chat-1",
            welcomeText = "欢迎语",
            attachmentConfig = bootstrapAttachmentConfig,
        )

    override suspend fun restoreMessages(
        appId: String,
        chatId: String,
    ) = Unit

    override suspend fun sendMessage(
        appId: String,
        chatId: String?,
        text: String,
        attachments: List<AttachmentDraftUiModel>,
    ): String {
        if (shouldFailSend) {
            throw IllegalStateException()
        }
        sentText = text
        val resolvedChatId = chatId ?: "chat-1"
        emitMessages(
            resolvedChatId,
            listOf(
                ChatMessageUiModel(
                    messageId = "user-1",
                    chatId = resolvedChatId,
                    appId = appId,
                    role = ChatRole.Human,
                    markdown = text,
                ),
                ChatMessageUiModel(
                    messageId = "assistant-1",
                    chatId = resolvedChatId,
                    appId = appId,
                    role = ChatRole.AI,
                    markdown = "已收到",
                ),
            ),
        )
        return resolvedChatId
    }

    override suspend fun uploadAttachment(
        appId: String,
        chatId: String?,
        attachment: AttachmentDraftUiModel,
        onProgress: (Float) -> Unit,
    ): com.lifuyue.kora.core.network.UploadedAssetRef =
        uploadBehavior(attachment, onProgress).also { uploadAttempts += 1 }

    override suspend fun stopStreaming(
        appId: String,
        chatId: String,
    ) {
        stoppedChatId = chatId
    }

    override suspend fun continueGeneration(
        appId: String,
        chatId: String,
    ): String {
        continuedChatId = chatId
        return chatId
    }

    override suspend fun regenerateResponse(
        appId: String,
        chatId: String,
        messageId: String,
    ): String {
        regeneratedMessageId = messageId
        return chatId
    }

    override suspend fun setFeedback(
        appId: String,
        chatId: String,
        messageId: String,
        feedback: MessageFeedback,
    ) {
        this.feedback = feedback
    }

    override suspend fun submitInteractiveResponse(
        appId: String,
        chatId: String,
        card: InteractiveCardUiModel,
        value: String,
    ): String {
        submittedInteractiveChatId = chatId
        submittedInteractiveCard = card
        submittedInteractiveValue = value
        return chatId
    }

    fun emitMessages(
        chatId: String,
        messages: List<ChatMessageUiModel>,
    ) {
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                put(chatId, messages)
            }
    }
}
