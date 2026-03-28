package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.createRetrofit
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomChatRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var database: KoraDatabase
    private lateinit var repository: RoomChatRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, KoraDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        server = MockWebServer()
        server.start()

        val connectionProvider =
            MutableConnectionProvider().apply {
                update(
                    getSnapshot().copy(
                        serverBaseUrl = server.url("/").toString(),
                        apiKey = "fastgpt-secret",
                    ),
                )
            }
        val okHttpClient = OkHttpClient()
        val api = createRetrofit(server.url("/").toString(), okHttpClient).create(FastGptApi::class.java)
        val sseClient = SseStreamClient(okHttpClient = okHttpClient, baseUrlProvider = connectionProvider)
        repository =
            RoomChatRepository(
                api = api,
                sseStreamClient = sseClient,
                conversationDao = database.conversationDao(),
                conversationFolderDao = database.conversationFolderDao(),
                conversationTagDao = database.conversationTagDao(),
                messageDao = database.messageDao(),
                json = NetworkJson.default,
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
    }

    @After
    fun tearDown() {
        if (::server.isInitialized) {
            server.shutdown()
        }
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun sendMessageStreamsIntoSingleAssistantMessageAndUpdatesPreview() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            server.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-1","appId":"app-1","title":"新会话"}}
                """.trimIndent(),
            )
            server.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"你好，"}}]}

                event: fastAnswer
                data: {"choices":[{"delta":{"content":"这是 FastGPT。"}}]}

                data: [DONE]

                event: flowResponses
                data: {"items":[{"id":"node-1"}]}
                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试真实流")
            advanceUntilIdle()

            val messages = repository.observeMessages("app-1", chatId).first()
            assertEquals(2, messages.size)
            assertEquals(ChatRole.Human, messages.first().role)
            assertEquals(ChatRole.AI, messages.last().role)
            assertEquals(MessageDeliveryState.Sent, messages.last().deliveryState)
            assertTrue(messages.last().markdown.contains("你好，这是 FastGPT。"))

            val conversations = repository.observeConversations("app-1").first()
            assertEquals(1, conversations.size)
            assertTrue(conversations.single().preview.contains("FastGPT"))
        }

    @Test
    fun streamErrorPreservesPartialAssistantAndMarksFailure() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            server.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-2","appId":"app-1"}}
                """.trimIndent(),
            )
            server.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"partial"}}]}

                event: error
                data: {"code":429,"statusText":"tooManyRequest","message":"slow down"}

                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试失败")
            advanceUntilIdle()

            val assistant = repository.observeMessages("app-1", chatId).first().last()
            assertEquals(MessageDeliveryState.Failed, assistant.deliveryState)
            assertTrue(assistant.markdown.contains("partial"))
            assertEquals("slow down", assistant.errorMessage)
        }

    @Test
    fun refreshConversationsAndRestoreRecordsFromRemoteApis() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            server.enqueueJson(
                """
                {
                  "code": 200,
                  "statusText": "",
                  "message": "",
                  "data": {
                    "list": [
                      {
                        "chatId": "chat-remote",
                        "updateTime": "2026-03-27T00:00:00Z",
                        "appId": "app-1",
                        "customTitle": "Pinned",
                        "title": "远端会话",
                        "top": true
                      }
                    ],
                    "total": 1
                  }
                }
                """.trimIndent(),
            )
            server.enqueueJson(
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
                      },
                      {
                        "dataId": "msg-2",
                        "obj": "AI",
                        "value": [{"text":{"content":"world"}}]
                      }
                    ],
                    "total": 2
                  }
                }
                """.trimIndent(),
            )

            repository.refreshConversations("app-1")
            repository.restoreMessages(appId = "app-1", chatId = "chat-remote")
            advanceUntilIdle()

            val conversations = repository.observeConversations("app-1").first()
            assertEquals("chat-remote", conversations.single().chatId)
            assertTrue(conversations.single().isPinned)

            val messages = repository.observeMessages("app-1", "chat-remote").first()
            assertEquals(listOf("hello", "world"), messages.map { it.markdown })
        }

    @Test
    fun regenerateDeletesTargetAssistantAndCreatesReplacement() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            server.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-3","appId":"app-1"}}
                """.trimIndent(),
            )
            server.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"第一次"}}]}
                """.trimIndent(),
            )
            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "原问题")
            advanceUntilIdle()
            val originalAssistantId = repository.observeMessages("app-1", chatId).first().last().messageId

            server.enqueueJson("""{"code":200,"statusText":"","message":"","data":{}}""")
            server.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"第二次"}}]}
                """.trimIndent(),
            )

            repository.regenerateResponse(appId = "app-1", chatId = chatId, messageId = originalAssistantId)
            advanceUntilIdle()

            val messages = repository.observeMessages("app-1", chatId).first()
            assertEquals(2, messages.size)
            assertEquals("原问题", messages.first().markdown)
            assertEquals("第二次", messages.last().markdown)
            assertTrue(messages.none { it.messageId == originalAssistantId })
        }
}

private fun MockWebServer.enqueueSse(body: String) {
    enqueue(
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(body),
    )
}

private fun MockWebServer.enqueueJson(body: String) {
    enqueue(
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body),
    )
}
