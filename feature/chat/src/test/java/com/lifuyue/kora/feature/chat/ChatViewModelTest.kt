package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.OpenAiCompatibleApiFactory
import com.lifuyue.kora.core.network.UploadedAssetRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ChatViewModelTest {
    @Test
    fun sendClearsDraftAfterRepositoryAcceptsMessage() =
        runTest {
            val repository = FakeChatRepository()
            val viewModel = createChatViewModel(repository = repository)

            viewModel.updateInput("hello")
            viewModel.send()

            assertEquals("hello", repository.sentTexts.single())
            assertEquals("", viewModel.uiState.value.input)
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
            )
        return chatId ?: "chat-1"
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
        savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1")),
        context = ApplicationProvider.getApplicationContext(),
        chatRepository = repository,
        conversationExportManager = FakeConversationExportManager(),
        connectionRepository = createConnectionRepository(),
        strings = ChatStrings(ApplicationProvider.getApplicationContext()),
    )

private fun createConnectionRepository(): ConnectionRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferencesFile = File.createTempFile("chat-view-model", ".preferences_pb").apply { deleteOnExit() }
    return ConnectionRepository(
        preferencesStore =
            ConnectionPreferencesStore.createForTest(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                file = preferencesFile,
            ),
        apiKeySecureStore = ApiKeySecureStore(context, "chat-view-model-secure"),
        connectionProvider = MutableConnectionProvider(),
        apiFactory = FastGptApiFactory(),
        openAiApiFactory = OpenAiCompatibleApiFactory(),
    )
}
