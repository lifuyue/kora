package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.KoraFeedbackPhase
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.OpenAiCompatibleApiFactory
import com.lifuyue.kora.core.network.UploadedAssetRef
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendClearsDraftAfterRepositoryAcceptsMessage() =
        runTest {
            val repository = FakeChatRepository()
            val viewModel = createChatViewModel(repository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.updateInput("hello")
            viewModel.send()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals("hello", repository.sentTexts.single())
            assertEquals("", viewModel.uiState.value.input)
        }

    @Test
    fun sendEntersFirstByteWaitingBeforeFirstTokenArrives() =
        runTest {
            val repository = FakeChatRepository()
            val viewModel = createChatViewModel(repository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.updateInput("hello")
            viewModel.send()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(KoraFeedbackPhase.InFlightFirstByte, viewModel.uiState.value.conversationPhase)
            assertEquals(KoraFeedbackPhase.InFlightFirstByte, viewModel.uiState.value.messages.last().phase)
        }

    @Test
    fun firstTokenSwitchesConversationToStreaming() =
        runTest {
            val repository = FakeChatRepository()
            val viewModel = createChatViewModel(repository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.updateInput("hello")
            viewModel.send()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            repository.emitAssistantState(markdown = "partial answer", deliveryState = MessageDeliveryState.Streaming, isStreaming = true)
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(KoraFeedbackPhase.InFlightStreaming, viewModel.uiState.value.conversationPhase)
            assertEquals(KoraFeedbackPhase.InFlightStreaming, viewModel.uiState.value.messages.last().phase)
        }

    @Test
    fun stopKeepsReceivedContentAndMarksStopped() =
        runTest {
            val repository = FakeChatRepository()
            val viewModel = createChatViewModel(repository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.updateInput("hello")
            viewModel.send()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            repository.emitAssistantState(markdown = "partial answer", deliveryState = MessageDeliveryState.Stopped, isStreaming = false)
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(KoraFeedbackPhase.Stopped, viewModel.uiState.value.conversationPhase)
            assertEquals("partial answer", viewModel.uiState.value.messages.last().markdown)
        }
}

private class FakeChatRepository : ChatRepository {
    val sentTexts = mutableListOf<String>()
    private val messages = MutableStateFlow<List<ChatMessageUiModel>>(emptyList())

    override fun observeMessages(
        appId: String,
        chatId: String?,
    ): Flow<List<ChatMessageUiModel>> = messages

    override suspend fun bootstrapChat(
        appId: String,
        chatId: String?,
    ): ChatBootstrap = ChatBootstrap(chatId = chatId ?: "chat-1", welcomeText = "Hi")

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
        sentTexts += text
        messages.value =
            listOf(
                ChatMessageUiModel(
                    messageId = "msg-1",
                    chatId = chatId ?: "chat-1",
                    appId = appId,
                    role = ChatRole.Human,
                    markdown = text,
                ),
                ChatMessageUiModel(
                    messageId = "msg-2",
                    chatId = chatId ?: "chat-1",
                    appId = appId,
                    role = ChatRole.AI,
                    markdown = "",
                    isStreaming = true,
                    deliveryState = MessageDeliveryState.Streaming,
                ),
            )
        return chatId ?: "chat-1"
    }

    fun emitAssistantState(
        markdown: String,
        deliveryState: MessageDeliveryState,
        isStreaming: Boolean,
    ) {
        val current = messages.value.toMutableList()
        val index = current.indexOfLast { it.role == ChatRole.AI }
        if (index < 0) return
        current[index] =
            current[index].copy(
                markdown = markdown,
                isStreaming = isStreaming,
                deliveryState = deliveryState,
            )
        messages.value = current
    }

    override suspend fun stopStreaming(appId: String, chatId: String) = Unit

    override suspend fun continueGeneration(appId: String, chatId: String): String = chatId

    override suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String = chatId

    override suspend fun setFeedback(
        appId: String,
        chatId: String,
        messageId: String,
        feedback: MessageFeedback,
    ) = Unit

    override suspend fun uploadAttachment(
        appId: String,
        chatId: String?,
        attachment: AttachmentDraftUiModel,
        onProgress: (Float) -> Unit,
    ): UploadedAssetRef =
        UploadedAssetRef(
            name = attachment.displayName,
            url = "https://example.com/${attachment.displayName}",
            key = "upload-1",
            mimeType = attachment.mimeType,
            size = attachment.sizeBytes ?: 0L,
        )
}

private class FakeConversationExportManager : ConversationExportManager {
    override suspend fun export(
        conversationTitle: String,
        format: ConversationExportFormat,
        messages: List<ExportConversationMessage>,
    ): ConversationExportArtifact = ConversationExportArtifact("/tmp/export.txt", "text/plain", 12)
}

private fun createChatViewModel(repository: FakeChatRepository): ChatViewModel =
    ChatViewModel(
        savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to "chat-1")),
        context = ApplicationProvider.getApplicationContext(),
        chatRepository = repository,
        conversationExportManager = FakeConversationExportManager(),
        connectionRepository = testConnectionRepository(),
        strings = ChatStrings(ApplicationProvider.getApplicationContext()),
    )

private fun testConnectionRepository(): ConnectionRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return ConnectionRepository(
        preferencesStore =
            ConnectionPreferencesStore.createForTest(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
                file = File(context.filesDir, "chat-view-model-${System.nanoTime()}.preferences_pb"),
            ),
        apiKeySecureStore = ApiKeySecureStore(context, "chat-view-model-${System.nanoTime()}"),
        connectionProvider = MutableConnectionProvider(),
        apiFactory = FastGptApiFactory(NetworkJson.default),
        openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
    )
}
