package com.lifuyue.kora.feature.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ResponseEnvelope
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.network.AppListItemDto
import com.lifuyue.kora.core.network.CollectionListRequest
import com.lifuyue.kora.core.network.CollectionSummaryDto
import com.lifuyue.kora.core.network.ChatCompletionRequest
import com.lifuyue.kora.core.network.ChatCompletionResponseDto
import com.lifuyue.kora.core.network.ChatHistoriesRequest
import com.lifuyue.kora.core.network.ChatHistoriesResponseData
import com.lifuyue.kora.core.network.ChatHistoryItemDto
import com.lifuyue.kora.core.network.ChatInitData
import com.lifuyue.kora.core.network.ChunkDeleteRequest
import com.lifuyue.kora.core.network.ChunkListRequest
import com.lifuyue.kora.core.network.ChunkListResponseDto
import com.lifuyue.kora.core.network.ChunkUpdateRequest
import com.lifuyue.kora.core.network.DatasetCreateRequest
import com.lifuyue.kora.core.network.DatasetDeleteRequest
import com.lifuyue.kora.core.network.DatasetListRequest
import com.lifuyue.kora.core.network.DatasetSummaryDto
import com.lifuyue.kora.core.network.DeleteChatItemRequest
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.LinkCollectionCreateRequest
import com.lifuyue.kora.core.network.PaginationRecordsRequest
import com.lifuyue.kora.core.network.PaginationRecordsResponseData
import com.lifuyue.kora.core.network.ChatRecordItemDto
import com.lifuyue.kora.core.network.QuestionGuideRequest
import com.lifuyue.kora.core.network.SearchTestRequest
import com.lifuyue.kora.core.network.SearchTestResponseDto
import com.lifuyue.kora.core.network.UpdateHistoryRequest
import com.lifuyue.kora.core.network.UpdateUserFeedbackRequest
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.StaticBaseUrlProvider
import com.lifuyue.kora.core.network.TextCollectionCreateRequest
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
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
            messageDao = database.messageDao(),
            api = api,
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

    override suspend fun initChat(appId: String, chatId: String?): ResponseEnvelope<ChatInitData> =
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

    override suspend fun deleteHistory(appId: String, chatId: String): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun clearHistories(appId: String): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun getResData(appId: String, dataId: String, chatId: String?): ResponseEnvelope<JsonArray> =
        ResponseEnvelope(code = 200, data = JsonArray(emptyList()))

    override suspend fun createQuestionGuide(request: QuestionGuideRequest): ResponseEnvelope<List<String>> =
        ResponseEnvelope(code = 200, data = emptyList())

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

    override suspend fun chatCompletions(request: ChatCompletionRequest): ResponseEnvelope<ChatCompletionResponseDto> =
        ResponseEnvelope(code = 200, data = ChatCompletionResponseDto())
}
