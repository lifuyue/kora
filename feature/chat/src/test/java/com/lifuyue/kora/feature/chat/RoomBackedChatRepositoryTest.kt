package com.lifuyue.kora.feature.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ResponseEnvelope
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.network.AppListItemDto
import com.lifuyue.kora.core.network.ChatCompletionRequest
import com.lifuyue.kora.core.network.ChatCompletionResponseDto
import com.lifuyue.kora.core.network.ChatHistoriesRequest
import com.lifuyue.kora.core.network.ChatHistoriesResponseData
import com.lifuyue.kora.core.network.ChatHistoryItemDto
import com.lifuyue.kora.core.network.ChatInitData
import com.lifuyue.kora.core.network.ChatRecordItemDto
import com.lifuyue.kora.core.network.ChunkDeleteRequest
import com.lifuyue.kora.core.network.ChunkListRequest
import com.lifuyue.kora.core.network.ChunkListResponseDto
import com.lifuyue.kora.core.network.ChunkUpdateRequest
import com.lifuyue.kora.core.network.CollectionListRequest
import com.lifuyue.kora.core.network.CollectionSummaryDto
import com.lifuyue.kora.core.network.DatasetCreateRequest
import com.lifuyue.kora.core.network.DatasetDeleteRequest
import com.lifuyue.kora.core.network.DatasetListRequest
import com.lifuyue.kora.core.network.DatasetSummaryDto
import com.lifuyue.kora.core.network.DeleteChatItemRequest
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.LinkCollectionCreateRequest
import com.lifuyue.kora.core.network.PaginationRecordsRequest
import com.lifuyue.kora.core.network.PaginationRecordsResponseData
import com.lifuyue.kora.core.network.QuestionGuideRequest
import com.lifuyue.kora.core.network.SearchTestRequest
import com.lifuyue.kora.core.network.SearchTestResponseDto
import com.lifuyue.kora.core.network.ShareAuthFinishRequest
import com.lifuyue.kora.core.network.ShareAuthInitRequest
import com.lifuyue.kora.core.network.ShareAuthStartRequest
import com.lifuyue.kora.core.network.ShareAuthStateDto
import com.lifuyue.kora.core.network.ShareSessionBootstrapDto
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.StaticBaseUrlProvider
import com.lifuyue.kora.core.network.TextCollectionCreateRequest
import com.lifuyue.kora.core.network.UploadedAssetRef
import com.lifuyue.kora.core.network.UpdateHistoryRequest
import com.lifuyue.kora.core.network.UpdateUserFeedbackRequest
import com.lifuyue.kora.core.network.AppAnalyticsSummaryDto
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
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
class RoomBackedChatRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refreshConversationsCachesHistoriesAndOrdersPinnedThenUpdateTimeThenChatId() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.api.histories =
                    listOf(
                        ChatHistoryItemDto(
                            chatId = "chat-1",
                            updateTime = "2026-03-27T01:00:00Z",
                            appId = "app-1",
                            customTitle = null,
                            title = "older pinned",
                            top = true,
                        ),
                        ChatHistoryItemDto(
                            chatId = "chat-2",
                            updateTime = "2026-03-27T01:00:00Z",
                            appId = "app-1",
                            customTitle = "newer pinned",
                            title = "ignored",
                            top = true,
                        ),
                        ChatHistoryItemDto(
                            chatId = "chat-3",
                            updateTime = "2026-03-27T00:00:00Z",
                            appId = "app-1",
                            customTitle = null,
                            title = "normal",
                            top = false,
                        ),
                    )

                fixture.repository.refreshConversations("app-1")
                advanceUntilIdle()

                val items = fixture.repository.observeConversations("app-1").first()
                assertEquals(listOf("chat-2", "chat-1", "chat-3"), items.map { it.chatId })
                assertEquals("newer pinned", items.first().title)
            }
        }

    @Test
    fun sendMessageWithoutChatIdUsesServerInitChatId() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.api.nextInitChatId = "chat-server"

                val chatId = fixture.repository.sendMessage(appId = "app-1", chatId = null, text = "hello")
                advanceUntilIdle()

                assertEquals("chat-server", chatId)
                assertEquals(listOf("app-1:null"), fixture.api.initChatCalls)
                assertEquals("chat-server", fixture.database.conversationDao().getConversationsForApp("app-1", 10, 0).single().chatId)
                assertEquals("chat-server", fixture.database.messageDao().getMessagesForChat("chat-server").first().chatId)
            }
        }

    @Test
    fun observeMessagesRestoresRemoteHistoryForExistingChatWhenCacheEmpty() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.database.conversationDao().upsert(
                    ConversationEntity(
                        chatId = "chat-existing",
                        appId = "app-1",
                        title = "Existing",
                        customTitle = null,
                        isPinned = false,
                        source = "online",
                        updateTime = 1L,
                        lastMessagePreview = null,
                        hasDraft = false,
                        isDeleted = false,
                        isArchived = false,
                    ),
                )
                fixture.api.recordsByChat["chat-existing"] =
                    listOf(
                        ChatRecordItemDto(
                            dataId = "msg-human",
                            obj = "Human",
                            value = jsonContent("hello"),
                        ),
                        ChatRecordItemDto(
                            dataId = "msg-ai",
                            obj = "AI",
                            value = jsonContent("world"),
                        ),
                    )

                fixture.repository.observeMessages("app-1", "chat-existing").first()
                advanceUntilIdle()
                val restored = fixture.repository.observeMessages("app-1", "chat-existing").first()

                assertEquals(listOf("msg-human", "msg-ai"), restored.map { it.messageId })
                assertEquals(listOf(ChatRole.Human, ChatRole.AI), restored.map { it.role })
                assertEquals("world", restored.last().markdown)
            }
        }

    @Test
    fun renamePinDeleteClearAndFeedbackPersistToLocalCache() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.api.histories =
                    listOf(
                        ChatHistoryItemDto(
                            chatId = "chat-1",
                            updateTime = "2026-03-27T00:00:00Z",
                            appId = "app-1",
                            customTitle = null,
                            title = "History",
                            top = false,
                        ),
                    )
                fixture.api.recordsByChat["chat-1"] =
                    listOf(
                        ChatRecordItemDto(
                            dataId = "msg-1",
                            obj = "AI",
                            value = jsonContent("cached"),
                        ),
                    )

                fixture.repository.refreshConversations("app-1")
                fixture.repository.observeMessages("app-1", "chat-1").first()
                advanceUntilIdle()

                fixture.repository.renameConversation("app-1", "chat-1", "Renamed")
                fixture.repository.togglePinConversation("app-1", "chat-1", true)
                fixture.repository.setFeedback("app-1", "chat-1", "msg-1", MessageFeedback.Upvote)
                advanceUntilIdle()

                val updatedConversation = fixture.database.conversationDao().getConversationByChatId("chat-1")!!
                val updatedMessage = fixture.database.messageDao().getMessagesForChat("chat-1").single()
                assertEquals("Renamed", updatedConversation.customTitle)
                assertTrue(updatedConversation.isPinned)
                assertEquals(1, updatedMessage.feedbackType)

                fixture.repository.deleteConversation("app-1", "chat-1")
                advanceUntilIdle()
                assertTrue(fixture.database.conversationDao().getConversationByChatId("chat-1")!!.isDeleted)
                assertTrue(fixture.database.messageDao().getMessagesForChat("chat-1").isEmpty())

                fixture.api.histories =
                    listOf(
                        ChatHistoryItemDto(
                            chatId = "chat-2",
                            updateTime = "2026-03-27T00:00:01Z",
                            appId = "app-1",
                            customTitle = null,
                            title = "Keep",
                            top = false,
                        ),
                    )
                fixture.repository.refreshConversations("app-1")
                advanceUntilIdle()
                fixture.repository.clearConversations("app-1")
                advanceUntilIdle()

                assertTrue(fixture.database.conversationDao().getConversationsForApp("app-1", 10, 0).isEmpty())
            }
        }

    @Test
    fun folderAndTagAssignmentsMergeIntoConversationListAndAreClearedOnDelete() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.api.histories =
                    listOf(
                        ChatHistoryItemDto(
                            chatId = "chat-1",
                            updateTime = "2026-03-27T00:00:00Z",
                            appId = "app-1",
                            customTitle = null,
                            title = "History",
                            top = false,
                        ),
                    )
                fixture.repository.refreshConversations("app-1")
                fixture.repository.createFolder("app-1", "工作")
                fixture.repository.createTag("app-1", "Kotlin")
                advanceUntilIdle()

                val folderId = fixture.repository.observeFolders("app-1").first().single().folderId
                val tagId = fixture.repository.observeTags("app-1").first().single().tagId
                fixture.repository.moveConversationToFolder("app-1", "chat-1", folderId)
                fixture.repository.setConversationTags("app-1", "chat-1", listOf(tagId))
                advanceUntilIdle()

                val item = fixture.repository.observeConversations("app-1").first().single()
                assertEquals("工作", item.folderName)
                assertEquals(listOf("Kotlin"), item.tags.map { it.name })

                fixture.repository.deleteConversation("app-1", "chat-1")
                advanceUntilIdle()

                assertTrue(fixture.database.conversationFolderDao().observeAssignments("app-1").first().isEmpty())
                assertTrue(fixture.database.conversationTagDao().observeAssignments("app-1").first().isEmpty())
            }
        }

    @Test
    fun observeMessagesRestoresInteractiveCardAndPendingDraftFromHistory() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.database.conversationDao().upsert(
                    ConversationEntity(
                        chatId = "chat-existing",
                        appId = "app-1",
                        title = "Existing",
                        customTitle = null,
                        isPinned = false,
                        source = "online",
                        updateTime = 1L,
                        lastMessagePreview = null,
                        hasDraft = false,
                        isDeleted = false,
                        isArchived = false,
                    ),
                )
                fixture.api.recordsByChat["chat-existing"] =
                    listOf(
                        ChatRecordItemDto(
                            dataId = "assistant-1",
                            obj = "AI",
                            value =
                                JsonArray(
                                    listOf(
                                        JsonObject(
                                            mapOf(
                                                "text" to JsonObject(mapOf("content" to JsonPrimitive("请选择"))),
                                            ),
                                        ),
                                        JsonObject(
                                            mapOf(
                                                "interactive" to
                                                    JsonObject(
                                                        mapOf(
                                                            "type" to JsonPrimitive("userSelect"),
                                                            "header" to JsonPrimitive("请选择一个方案"),
                                                            "responseValueId" to JsonPrimitive("response-1"),
                                                            "options" to
                                                                JsonArray(
                                                                    listOf(
                                                                        JsonObject(mapOf("label" to JsonPrimitive("Alpha"))),
                                                                        JsonObject(mapOf("label" to JsonPrimitive("Beta"))),
                                                                    ),
                                                                ),
                                                        ),
                                                    ),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    )
                fixture.database.interactiveDraftDao().upsert(
                    com.lifuyue.kora.core.database.entity.InteractiveDraftEntity(
                        chatId = "chat-existing",
                        messageDataId = "assistant-1",
                        responseValueId = "response-1",
                        rawPayloadJson =
                            """
                            {"type":"userSelect","header":"请选择一个方案","options":[{"label":"Alpha"},{"label":"Beta"}]}
                            """.trimIndent(),
                        draftPayloadJson = """{"selected":"Beta"}""",
                        updatedAt = 1L,
                    ),
                )

                fixture.repository.observeMessages("app-1", "chat-existing").first()
                advanceUntilIdle()
                val restored = fixture.repository.observeMessages("app-1", "chat-existing").first()

                val interactiveCard = restored.single().interactiveCard
                assertNotNull(interactiveCard)
                assertEquals(InteractiveCardKind.UserSelect, interactiveCard?.kind)
                assertEquals("assistant-1", interactiveCard?.messageDataId)
                assertEquals("response-1", interactiveCard?.responseValueId)
                assertEquals(listOf("Alpha", "Beta"), interactiveCard?.options)
                assertEquals(InteractiveCardStatus.Pending, interactiveCard?.status)
            }
        }

    @Test
    fun observeMessagesNormalizesCollectionFormFromCachedSsePayload() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.database.conversationDao().upsert(
                    ConversationEntity(
                        chatId = "chat-stream",
                        appId = "app-1",
                        title = "Streaming",
                        customTitle = null,
                        isPinned = false,
                        source = "online",
                        updateTime = 1L,
                        lastMessagePreview = null,
                        hasDraft = false,
                        isDeleted = false,
                        isArchived = false,
                    ),
                )
                fixture.database.messageDao().upsert(
                    com.lifuyue.kora.core.database.entity.MessageEntity(
                        dataId = "assistant-1",
                        chatId = "chat-stream",
                        appId = "app-1",
                        role = ChatRole.AI.name,
                        payloadJson =
                            """
                            {
                              "markdown": "填写信息",
                              "reasoning": "",
                              "eventPayloads": [
                                {
                                  "fields": [
                                    {"label": "Topic"},
                                    {"name": "details"}
                                  ],
                                  "responseValueId": "response-2"
                                }
                              ]
                            }
                            """.trimIndent(),
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                )

                val restored = fixture.repository.observeMessages("app-1", "chat-stream").first()

                val interactiveCard = restored.single().interactiveCard
                assertNotNull(interactiveCard)
                assertEquals(InteractiveCardKind.CollectionForm, interactiveCard?.kind)
                assertEquals(listOf("Topic", "details"), interactiveCard?.fields?.map { it.label })
                assertEquals("response-2", interactiveCard?.responseValueId)
            }
        }

    @Test
    fun submitInteractiveResponseCreatesHumanReplyAndClearsDraft() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.database.conversationDao().upsert(
                    ConversationEntity(
                        chatId = "chat-existing",
                        appId = "app-1",
                        title = "Existing",
                        customTitle = null,
                        isPinned = false,
                        source = "online",
                        updateTime = 1L,
                        lastMessagePreview = null,
                        hasDraft = false,
                        isDeleted = false,
                        isArchived = false,
                    ),
                )
                fixture.database.interactiveDraftDao().upsert(
                    com.lifuyue.kora.core.database.entity.InteractiveDraftEntity(
                        chatId = "chat-existing",
                        messageDataId = "assistant-1",
                        responseValueId = "response-1",
                        rawPayloadJson = """{"type":"userSelect","options":[{"label":"Alpha"}]}""",
                        draftPayloadJson = null,
                        updatedAt = 1L,
                    ),
                )
                fixture.database.messageDao().upsert(
                    com.lifuyue.kora.core.database.entity.MessageEntity(
                        dataId = "assistant-1",
                        chatId = "chat-existing",
                        appId = "app-1",
                        role = ChatRole.AI.name,
                        payloadJson =
                            """
                            {
                              "markdown": "请选择",
                              "eventPayloads": [
                                {"type":"userSelect","responseValueId":"response-1","options":[{"label":"Alpha"}]}
                              ]
                            }
                            """.trimIndent(),
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                )

                val chatId =
                    fixture.repository.submitInteractiveResponse(
                        appId = "app-1",
                        chatId = "chat-existing",
                        card =
                            InteractiveCardUiModel(
                                kind = InteractiveCardKind.UserSelect,
                                messageDataId = "assistant-1",
                                responseValueId = "response-1",
                                options = listOf("Alpha"),
                            ),
                        value = "Alpha",
                    )
                advanceUntilIdle()

                assertEquals("chat-existing", chatId)
                val restored = fixture.repository.observeMessages("app-1", "chat-existing").first()
                assertEquals(3, restored.size)
                assertEquals(InteractiveCardStatus.Resolved, restored.first().interactiveCard?.status)
                assertEquals(ChatRole.Human, restored[1].role)
                assertEquals("Alpha", restored[1].markdown)
                assertEquals(null, fixture.database.interactiveDraftDao().getByChatId("chat-existing"))
            }
        }

    @Test
    fun observeMessagesRestoresSubmittingCollectionFormDraft() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            newFixture().use { fixture ->
                fixture.database.conversationDao().upsert(
                    ConversationEntity(
                        chatId = "chat-form",
                        appId = "app-1",
                        title = "Form",
                        customTitle = null,
                        isPinned = false,
                        source = "online",
                        updateTime = 1L,
                        lastMessagePreview = null,
                        hasDraft = false,
                        isDeleted = false,
                        isArchived = false,
                    ),
                )
                fixture.database.messageDao().upsert(
                    com.lifuyue.kora.core.database.entity.MessageEntity(
                        dataId = "assistant-form",
                        chatId = "chat-form",
                        appId = "app-1",
                        role = ChatRole.AI.name,
                        payloadJson =
                            """
                            {
                              "markdown": "填写信息",
                              "eventPayloads": [
                                {
                                  "type": "collectionForm",
                                  "fields": [
                                    {"name": "topic", "label": "Topic"},
                                    {"name": "details", "label": "Details"}
                                  ],
                                  "responseValueId": "response-form"
                                }
                              ]
                            }
                            """.trimIndent(),
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                )
                fixture.database.interactiveDraftDao().upsert(
                    com.lifuyue.kora.core.database.entity.InteractiveDraftEntity(
                        chatId = "chat-form",
                        messageDataId = "assistant-form",
                        responseValueId = "response-form",
                        rawPayloadJson =
                            """
                            {"type":"collectionForm","fields":[{"name":"topic","label":"Topic"},{"name":"details","label":"Details"}]}
                            """.trimIndent(),
                        draftPayloadJson =
                            """
                            {"status":"Submitting","fieldValues":{"topic":"Kotlin","details":"Flow"}}
                            """.trimIndent(),
                        updatedAt = 1L,
                    ),
                )

                val restored = fixture.repository.observeMessages("app-1", "chat-form").first()

                val card = restored.single().interactiveCard
                assertNotNull(card)
                assertEquals(InteractiveCardStatus.Submitting, card?.status)
                assertEquals("Kotlin", card?.fields?.firstOrNull { it.id == "topic" }?.value)
                assertEquals("Flow", card?.fields?.firstOrNull { it.id == "details" }?.value)
            }
        }
}

private fun jsonContent(text: String) =
    JsonArray(
        listOf(
            JsonObject(
                mapOf(
                    "text" to JsonObject(mapOf("content" to JsonPrimitive(text))),
                ),
            ),
        ),
    )

private class Fixture(
    val database: KoraDatabase,
    val repository: RoomBackedChatRepository,
    val api: FakeFastGptApi,
) : AutoCloseable {
    override fun close() {
        database.close()
    }
}

private fun RoomBackedChatRepositoryTest.newFixture(): Fixture {
    val database =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KoraDatabase::class.java,
        ).allowMainThreadQueries().build()
    val api = FakeFastGptApi()
    val repository =
        RoomBackedChatRepository(
            conversationDao = database.conversationDao(),
            conversationFolderDao = database.conversationFolderDao(),
            conversationTagDao = database.conversationTagDao(),
            interactiveDraftDao = database.interactiveDraftDao(),
            messageDao = database.messageDao(),
            api = api,
            context = ApplicationProvider.getApplicationContext(),
            sseStreamClient =
                SseStreamClient(
                    okHttpClient = OkHttpClient(),
                    baseUrlProvider = StaticBaseUrlProvider("https://placeholder.invalid/"),
                ),
            ioDispatcher = mainDispatcherRule.dispatcher,
            responsePlanner = AssistantResponsePlanner { emptyList() },
        )
    return Fixture(database, repository, api)
}

private class FakeFastGptApi : FastGptApi {
    var nextInitChatId: String? = null
    val initChatCalls = mutableListOf<String>()
    var histories: List<ChatHistoryItemDto> = emptyList()
    val recordsByChat = linkedMapOf<String, List<ChatRecordItemDto>>()

    override suspend fun listApps(body: JsonObject): ResponseEnvelope<List<AppListItemDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun initChat(
        appId: String,
        chatId: String?,
    ): ResponseEnvelope<ChatInitData> =
        ResponseEnvelope(
            code = 200,
            data =
                ChatInitData(
                    chatId =
                        nextInitChatId
                            ?.also { initChatCalls += "$appId:$chatId" }
                            ?: chatId.also { initChatCalls += "$appId:$chatId" },
                    appId = appId,
                ),
        )

    override suspend fun getHistories(request: ChatHistoriesRequest): ResponseEnvelope<ChatHistoriesResponseData> =
        ResponseEnvelope(code = 200, data = ChatHistoriesResponseData(list = histories, total = histories.size))

    override suspend fun getPaginationRecords(request: PaginationRecordsRequest): ResponseEnvelope<PaginationRecordsResponseData> =
        ResponseEnvelope(
            code = 200,
            data =
                PaginationRecordsResponseData(
                    list = recordsByChat[request.chatId].orEmpty(),
                    total = recordsByChat[request.chatId].orEmpty().size,
                ),
        )

    override suspend fun updateHistory(request: UpdateHistoryRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun deleteHistory(
        appId: String,
        chatId: String,
    ): ResponseEnvelope<JsonObject> = ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun clearHistories(appId: String): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun getResData(
        appId: String,
        dataId: String,
        chatId: String?,
    ): ResponseEnvelope<JsonArray> = ResponseEnvelope(code = 200, data = JsonArray(emptyList()))

    override suspend fun createQuestionGuide(request: QuestionGuideRequest): ResponseEnvelope<List<String>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun uploadChatAttachment(
        file: okhttp3.MultipartBody.Part,
        appId: okhttp3.RequestBody,
        chatId: okhttp3.RequestBody?,
    ): ResponseEnvelope<UploadedAssetRef> =
        ResponseEnvelope(
            code = 200,
            data =
                UploadedAssetRef(
                    name = "upload.bin",
                    url = "https://example.com/upload.bin",
                    key = "chat/upload.bin",
                    mimeType = "application/octet-stream",
                    size = 1L,
                ),
        )

    override suspend fun listDatasets(request: DatasetListRequest): ResponseEnvelope<List<DatasetSummaryDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun createDataset(request: DatasetCreateRequest): ResponseEnvelope<DatasetSummaryDto> =
        ResponseEnvelope(code = 200, data = null)

    override suspend fun deleteDataset(request: DatasetDeleteRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun listCollections(request: CollectionListRequest): ResponseEnvelope<List<CollectionSummaryDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun createTextCollection(request: TextCollectionCreateRequest): ResponseEnvelope<CollectionSummaryDto> =
        ResponseEnvelope(code = 200, data = null)

    override suspend fun createLinkCollection(request: LinkCollectionCreateRequest): ResponseEnvelope<List<CollectionSummaryDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun createLocalFileCollection(
        file: okhttp3.MultipartBody.Part,
        datasetId: okhttp3.RequestBody,
        parentId: okhttp3.RequestBody?,
        trainingType: okhttp3.RequestBody,
    ): ResponseEnvelope<CollectionSummaryDto> = ResponseEnvelope(code = 200, data = null)

    override suspend fun listChunkData(request: ChunkListRequest): ResponseEnvelope<ChunkListResponseDto> =
        ResponseEnvelope(code = 200, data = ChunkListResponseDto())

    override suspend fun updateChunkData(request: ChunkUpdateRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun deleteChunkData(request: ChunkDeleteRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun searchTest(request: SearchTestRequest): ResponseEnvelope<SearchTestResponseDto> =
        ResponseEnvelope(code = 200, data = SearchTestResponseDto())

    override suspend fun deleteChatItem(request: DeleteChatItemRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun updateUserFeedback(request: UpdateUserFeedbackRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun shareAuthInit(request: ShareAuthInitRequest): ResponseEnvelope<ShareAuthStateDto> =
        ResponseEnvelope(code = 200, data = ShareAuthStateDto(uid = request.token))

    override suspend fun shareAuthStart(request: ShareAuthStartRequest): ResponseEnvelope<ShareAuthStateDto> =
        ResponseEnvelope(code = 200, data = ShareAuthStateDto(uid = request.token))

    override suspend fun shareAuthFinish(request: ShareAuthFinishRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun initShareSession(
        shareId: String,
        outLinkUid: String,
        chatId: String?,
    ): ResponseEnvelope<ShareSessionBootstrapDto> =
        ResponseEnvelope(
            code = 200,
            data = ShareSessionBootstrapDto(chatId = chatId ?: "share-chat", appId = "app-1", title = "share", appName = "Kora"),
        )

    override suspend fun getAppAnalytics(
        appId: String,
        range: String?,
    ): ResponseEnvelope<AppAnalyticsSummaryDto> =
        ResponseEnvelope(code = 200, data = AppAnalyticsSummaryDto())

    override suspend fun chatCompletions(request: ChatCompletionRequest): ResponseEnvelope<ChatCompletionResponseDto> =
        ResponseEnvelope(code = 200, data = ChatCompletionResponseDto())
}
