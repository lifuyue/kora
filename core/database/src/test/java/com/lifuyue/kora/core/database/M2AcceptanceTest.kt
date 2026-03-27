package com.lifuyue.kora.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ChatSource
import com.lifuyue.kora.core.common.SseEvent
import com.lifuyue.kora.core.network.ChatCompletionMessageParam
import com.lifuyue.kora.core.network.ChatCompletionRequest
import com.lifuyue.kora.core.network.ChatHistoriesRequest
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.PaginationRecordsRequest
import com.lifuyue.kora.core.network.SseEventData
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.StaticBaseUrlProvider
import com.lifuyue.kora.core.network.createRetrofit
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.lifuyue.kora.core.database.entity.ConversationEntity as DbConversationEntity
import com.lifuyue.kora.core.database.entity.MessageEntity as DbMessageEntity

@RunWith(RobolectricTestRunner::class)
class M2AcceptanceTest {
    private val json = NetworkJson.default
    private lateinit var database: KoraDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, KoraDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun streamEventsCanBePersistedAndReadBack() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(
                        """
                        event: answer
                        data: {"choices":[{"delta":{"content":"Hi"}}]}

                        event: toolCall
                        data: {"toolName":"search"}

                        event: interactive
                        data: {"type":"form"}

                        data: [DONE]

                        event: flowResponses
                        data: {"items":[{"id":"node-1"}]}

                        """.trimIndent(),
                    ),
            )
            server.start()

            val client =
                SseStreamClient(
                    okHttpClient = OkHttpClient(),
                    baseUrlProvider = StaticBaseUrlProvider(server.url("/").toString()),
                )
            val events =
                client.streamChatCompletions(
                    ChatCompletionRequest(
                        appId = "app-1",
                        chatId = "chat-1",
                        responseChatItemId = "resp-1",
                        messages =
                            listOf(
                                ChatCompletionMessageParam(
                                    role = "user",
                                    content = JsonPrimitive("hello"),
                                ),
                            ),
                    ),
                ).toList()

            database.messageDao().upsertAll(
                listOf(
                    DbMessageEntity(
                        dataId = "user-1",
                        chatId = "chat-1",
                        appId = "app-1",
                        role = ChatRole.Human.name,
                        payloadJson = """{"text":"hello"}""",
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                    DbMessageEntity(
                        dataId = "resp-1",
                        chatId = "chat-1",
                        appId = "app-1",
                        role = ChatRole.AI.name,
                        payloadJson = persistedPayload(events).toString(),
                        createdAt = 2L,
                        isStreaming = false,
                        sendStatus = "completed",
                        errorCode = null,
                    ),
                ),
            )

            val rows = database.messageDao().getMessagesForChat("chat-1")

            assertEquals(listOf("user-1", "resp-1"), rows.map { it.dataId })
            assertTrue(rows.last().payloadJson.contains("flowResponses"))
            assertTrue(rows.last().payloadJson.contains("[DONE]"))

            server.shutdown()
        }

    @Test
    fun sseErrorAfterPartialAnswerKeepsPartialTextAndErrorCode() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(
                        """
                        event: answer
                        data: {"choices":[{"delta":{"content":"partial"}}]}

                        event: error
                        data: {"code":429,"statusText":"tooManyRequest","message":"slow down"}

                        """.trimIndent(),
                    ),
            )
            server.start()

            val client =
                SseStreamClient(
                    okHttpClient = OkHttpClient(),
                    baseUrlProvider = StaticBaseUrlProvider(server.url("/").toString()),
                )
            val events =
                client.streamChatCompletions(
                    ChatCompletionRequest(
                        appId = "app-1",
                        chatId = "chat-2",
                        messages =
                            listOf(
                                ChatCompletionMessageParam(
                                    role = "user",
                                    content = JsonPrimitive("hello"),
                                ),
                            ),
                    ),
                ).toList()

            val errorEvent = events.last { it.event == SseEvent.error }
            val partialText =
                events.first().payload
                    ?.jsonObject
                    ?.get("choices")
                    ?.jsonArray
                    ?.first()
                    ?.jsonObject
                    ?.get("delta")
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonPrimitive
                    ?.content

            database.messageDao().upsertAll(
                listOf(
                    DbMessageEntity(
                        dataId = "resp-2",
                        chatId = "chat-2",
                        appId = "app-1",
                        role = ChatRole.AI.name,
                        payloadJson =
                            buildJsonObject {
                                put("partialText", JsonPrimitive(partialText))
                                put("events", persistedPayload(events))
                            }.toString(),
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "error",
                        errorCode =
                            errorEvent.payload
                                ?.jsonObject
                                ?.get("code")
                                ?.jsonPrimitive
                                ?.content
                                ?.toInt(),
                    ),
                ),
            )

            val row = database.messageDao().getMessagesForChat("chat-2").single()
            assertTrue(row.payloadJson.contains("partial"))
            assertEquals(429, row.errorCode)

            server.shutdown()
        }

    @Test
    fun historyAndRecordsResponsesCanBeCachedLocally() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 200,
                          "statusText": "",
                          "message": "",
                          "data": {
                            "list": [
                              {
                                "chatId": "chat-3",
                                "updateTime": "2026-03-27T00:00:00Z",
                                "appId": "app-1",
                                "customTitle": "Pinned",
                                "title": "History Title",
                                "top": true
                              }
                            ],
                            "total": 1
                          }
                        }
                        """.trimIndent(),
                    ),
            )
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 200,
                          "statusText": "",
                          "message": "",
                          "data": {
                            "list": [
                              {
                                "dataId": "msg-1",
                                "obj": "Human",
                                "value": [{"text":{"content":"hello"}}]
                              }
                            ],
                            "total": 1
                          }
                        }
                        """.trimIndent(),
                    ),
            )
            server.start()

            val api = createRetrofit(server.url("/").toString(), OkHttpClient()).create(FastGptApi::class.java)
            val histories = api.getHistories(ChatHistoriesRequest(appId = "app-1"))
            val records = api.getPaginationRecords(PaginationRecordsRequest(appId = "app-1", chatId = "chat-3"))

            val historyItem = histories.data!!.list.single()
            database.conversationDao().upsert(
                DbConversationEntity(
                    chatId = historyItem.chatId,
                    appId = historyItem.appId,
                    title = historyItem.title,
                    customTitle = historyItem.customTitle,
                    isPinned = historyItem.top == true,
                    source = ChatSource.online.name,
                    updateTime = 1L,
                    lastMessagePreview = null,
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )
            val recordItem = records.data!!.list.single()
            database.messageDao().upsertAll(
                listOf(
                    DbMessageEntity(
                        dataId = requireNotNull(recordItem.dataId),
                        chatId = "chat-3",
                        appId = "app-1",
                        role = recordItem.obj ?: ChatRole.Human.name,
                        payloadJson = recordItem.value?.toString() ?: JsonNull.toString(),
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "synced",
                        errorCode = null,
                    ),
                ),
            )

            val conversations = database.conversationDao().getConversationsForApp("app-1", limit = 10, offset = 0)
            val messages = database.messageDao().getMessagesForChat("chat-3")

            assertEquals("History Title", conversations.single().title)
            assertEquals("msg-1", messages.single().dataId)

            server.shutdown()
        }

    private fun persistedPayload(events: List<SseEventData>) =
        JsonArray(
            events.map { event ->
                buildJsonObject {
                    put("event", event.rawEventName?.let(::JsonPrimitive) ?: JsonNull)
                    put("data", JsonPrimitive(event.rawData))
                    put("done", JsonPrimitive(event.isDone))
                }
            },
        )
}
