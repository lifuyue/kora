package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.KoraFeedbackPhase
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.LocalKnowledgeStore
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.OpenAiCompatibleApiFactory
import com.lifuyue.kora.core.testing.RoomTestFactory
import com.lifuyue.kora.core.network.UploadedAssetRef
import com.lifuyue.kora.core.testing.MainDispatcherRule
import com.lifuyue.kora.feature.knowledge.CollectionListItemUiModel
import com.lifuyue.kora.feature.knowledge.DatasetListItemUiModel
import com.lifuyue.kora.feature.knowledge.KnowledgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @After
    fun tearDown() {
        trackedDatabases.forEach(RoomDatabase::close)
        trackedDatabases.clear()
    }

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

    @Test
    fun selectKnowledgeCollectionAddsReferencePillState() =
        runTest {
            val repository = FakeChatRepository()
            val knowledgeRepository = FakeKnowledgeRepository()
            val viewModel =
                createChatViewModel(
                    repository = repository,
                    knowledgeRepository = knowledgeRepository,
                    connectionType = ConnectionType.FAST_GPT,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            viewModel.openKnowledgePicker()
            viewModel.selectKnowledgeDataset("dataset-1")
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            viewModel.selectKnowledgeCollection("collection-1")
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals("dataset-1", viewModel.uiState.value.selectedKnowledgeReference?.datasetId)
            assertEquals("Collection Alpha", viewModel.uiState.value.selectedKnowledgeReference?.collectionName)
            assertEquals(false, viewModel.uiState.value.knowledgePickerState.isVisible)
        }

    @Test
    fun sendClearsSelectedKnowledgeReferenceAfterSuccess() =
        runTest {
            val repository = FakeChatRepository()
            val knowledgeRepository = FakeKnowledgeRepository()
            val viewModel =
                createChatViewModel(
                    repository = repository,
                    knowledgeRepository = knowledgeRepository,
                    connectionType = ConnectionType.FAST_GPT,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            viewModel.openKnowledgePicker()
            viewModel.selectKnowledgeDataset("dataset-1")
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            viewModel.selectKnowledgeCollection("collection-1")
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            viewModel.updateInput("hello")
            viewModel.send()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals("collection-1", repository.sentKnowledgeReference?.collectionId)
            assertNull(viewModel.uiState.value.selectedKnowledgeReference)
        }

    @Test
    fun knowledgePickerQueryFiltersCurrentDatasetLevelItems() =
        runTest {
            val repository = FakeChatRepository()
            val knowledgeRepository = FakeKnowledgeRepository()
            val viewModel =
                createChatViewModel(
                    repository = repository,
                    knowledgeRepository = knowledgeRepository,
                    connectionType = ConnectionType.FAST_GPT,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            viewModel.openKnowledgePicker()
            viewModel.updateKnowledgePickerQuery("secondary")
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals("secondary", viewModel.uiState.value.knowledgePickerState.query)
            assertEquals(1, viewModel.uiState.value.knowledgePickerState.filteredDatasets.size)
            assertEquals("dataset-2", viewModel.uiState.value.knowledgePickerState.filteredDatasets.single().datasetId)
        }

    @Test
    fun knowledgePickerQueryResetsWhenSwitchingDatasetLevel() =
        runTest {
            val repository = FakeChatRepository()
            val knowledgeRepository = FakeKnowledgeRepository()
            val viewModel =
                createChatViewModel(
                    repository = repository,
                    knowledgeRepository = knowledgeRepository,
                    connectionType = ConnectionType.FAST_GPT,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            viewModel.openKnowledgePicker()
            viewModel.updateKnowledgePickerQuery("primary")
            viewModel.selectKnowledgeDataset("dataset-1")
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.knowledgePickerState.query)

            viewModel.updateKnowledgePickerQuery("beta")
            viewModel.backToKnowledgeDatasets()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.knowledgePickerState.query)
        }

}

private class FakeChatRepository : ChatRepository {
    val sentTexts = mutableListOf<String>()
    var sentKnowledgeReference: ChatKnowledgeReferenceUiModel? = null
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
        selectedKnowledgeReference: ChatKnowledgeReferenceUiModel?,
    ): String {
        sentTexts += text
        sentKnowledgeReference = selectedKnowledgeReference
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

private class FakeKnowledgeRepository : KnowledgeRepository {
    private val datasets =
        MutableStateFlow(
            listOf(
                DatasetListItemUiModel(
                    datasetId = "dataset-1",
                    name = "Dataset One",
                    intro = "Primary dataset",
                    type = "dataset",
                    status = "active",
                    vectorModel = "",
                    updateTime = 1L,
                ),
                DatasetListItemUiModel(
                    datasetId = "dataset-2",
                    name = "Dataset Two",
                    intro = "Secondary dataset",
                    type = "dataset",
                    status = "active",
                    vectorModel = "",
                    updateTime = 2L,
                ),
            ),
        )
    var refreshDatasetsCallCount: Int = 0

    override fun observeDatasets(): Flow<List<DatasetListItemUiModel>> = datasets

    override fun observeDataset(datasetId: String): Flow<DatasetListItemUiModel?> =
        flowOf(datasets.value.firstOrNull { it.datasetId == datasetId })

    override suspend fun refreshDatasets(query: String) {
        refreshDatasetsCallCount += 1
    }

    override suspend fun createDataset(name: String) = Unit

    override suspend fun deleteDataset(datasetId: String) = Unit

    override fun observeCollections(datasetId: String): Flow<List<CollectionListItemUiModel>> =
        flowOf(
            listOf(
                CollectionListItemUiModel(
                    collectionId = "collection-1",
                    datasetId = datasetId,
                    name = "Collection Alpha",
                    type = "file",
                    trainingType = "chunk",
                    status = "ready",
                    sourceName = "",
                    updateTime = 1L,
                ),
                CollectionListItemUiModel(
                    collectionId = "collection-2",
                    datasetId = datasetId,
                    name = "Collection Beta",
                    type = "text",
                    trainingType = "chunk",
                    status = "ready",
                    sourceName = "",
                    updateTime = 2L,
                ),
            ),
        )

    override fun observeImportTasks(datasetId: String): Flow<List<com.lifuyue.kora.feature.knowledge.ImportTaskUiModel>> = flowOf(emptyList())

    override suspend fun refreshCollections(datasetId: String) = Unit

    override suspend fun importText(datasetId: String, name: String, text: String, trainingType: String) = Unit

    override suspend fun importLinks(datasetId: String, urls: List<String>, selector: String?, trainingType: String) = Unit

    override suspend fun importDocument(datasetId: String, uri: android.net.Uri, displayName: String, trainingType: String) = Unit

    override suspend fun listChunks(datasetId: String, collectionId: String, offset: Int, pageSize: Int) = emptyList<com.lifuyue.kora.feature.knowledge.ChunkItemUiModel>()

    override suspend fun updateChunk(dataId: String, question: String, answer: String, forbid: Boolean) = Unit

    override suspend fun deleteChunk(dataId: String) = Unit

    override suspend fun search(
        datasetId: String,
        query: String,
        searchMode: String,
        similarity: Double?,
        embeddingWeight: Double?,
        usingReRank: Boolean,
    ) = Triple("", "", emptyList<com.lifuyue.kora.feature.knowledge.SearchResultUiModel>())
}

private class FakeConversationExportManager : ConversationExportManager {
    override suspend fun export(
        conversationTitle: String,
        format: ConversationExportFormat,
        messages: List<ExportConversationMessage>,
    ): ConversationExportArtifact = ConversationExportArtifact("/tmp/export.txt", "text/plain", 12)
}

private fun createChatViewModel(
    repository: FakeChatRepository,
    knowledgeRepository: KnowledgeRepository = FakeKnowledgeRepository(),
    connectionType: ConnectionType = ConnectionType.FAST_GPT,
    appId: String = "app-1",
    localKnowledgeStore: LocalKnowledgeStore = createLocalKnowledgeStore(),
): ChatViewModel =
    ChatViewModel(
        savedStateHandle = SavedStateHandle(mapOf("appId" to appId, "chatId" to "chat-1")),
        context = ApplicationProvider.getApplicationContext(),
        chatRepository = repository,
        knowledgeRepository = knowledgeRepository,
        localKnowledgeStore = localKnowledgeStore,
        conversationExportManager = FakeConversationExportManager(),
        connectionRepository = testConnectionRepository(connectionType),
        strings = ChatStrings(ApplicationProvider.getApplicationContext()),
    )

private fun testConnectionRepository(connectionType: ConnectionType): ConnectionRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val provider =
        MutableConnectionProvider().apply {
            update(getSnapshot().copy(connectionType = connectionType))
        }
    return ConnectionRepository(
        preferencesStore =
            ConnectionPreferencesStore.createForTest(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
                file = File(context.filesDir, "chat-view-model-${System.nanoTime()}.preferences_pb"),
            ),
        apiKeySecureStore = ApiKeySecureStore(context, "chat-view-model-${System.nanoTime()}"),
        connectionProvider = provider,
        apiFactory = FastGptApiFactory(NetworkJson.default),
        openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
    )
}

private fun createLocalKnowledgeStore(
    ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
): LocalKnowledgeStore {
    val database =
        RoomTestFactory.inMemoryDatabase<KoraDatabase>().also { database ->
            trackedDatabases += database
        }
    return LocalKnowledgeStore(
        documentDao = database.localKnowledgeDocumentDao(),
        chunkDao = database.localKnowledgeChunkDao(),
        postingDao = database.localKnowledgePostingDao(),
        ioDispatcher = ioDispatcher,
    )
}

private val trackedDatabases = mutableListOf<RoomDatabase>()
