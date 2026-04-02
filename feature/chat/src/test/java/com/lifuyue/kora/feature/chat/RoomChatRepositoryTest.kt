package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.DIRECT_OPENAI_APP_ID
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.LocalKnowledgeStore
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.createRetrofit
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomChatRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var database: KoraDatabase
    private lateinit var repository: RoomChatRepository
    private lateinit var connectionProvider: MutableConnectionProvider
    private lateinit var localKnowledgeStore: LocalKnowledgeStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, KoraDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        localKnowledgeStore =
            LocalKnowledgeStore(
                documentDao = database.localKnowledgeDocumentDao(),
                chunkDao = database.localKnowledgeChunkDao(),
                postingDao = database.localKnowledgePostingDao(),
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
        server = MockWebServer()
        server.start()

        connectionProvider =
            MutableConnectionProvider().apply {
                update(
                    getSnapshot().copy(
                        connectionType = ConnectionType.FAST_GPT,
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
                interactiveDraftDao = database.interactiveDraftDao(),
                messageDao = database.messageDao(),
                context = context,
                json = NetworkJson.default,
                ioDispatcher = mainDispatcherRule.dispatcher,
                connectionSnapshotProvider = connectionProvider,
                localKnowledgeStore = localKnowledgeStore,
            )
    }

    @After
    fun tearDown() {
        if (::repository.isInitialized) {
            repository.cancelBackgroundWork()
        }
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
    fun openAiModeStreamsWithoutFastGptBootstrapAndUsesModelRequest() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            localKnowledgeStore.importText(
                title = "API Notes",
                text = "The OpenAI mode can use local references to answer questions about setup.",
                sourceLabel = "manual",
                now = 1L,
            )
            waitUntilLocalKnowledgeReady()
            connectionProvider.update(
                connectionProvider.getSnapshot().copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/").toString(),
                    apiKey = "openai-secret",
                    model = "gpt-4o-mini",
                ),
            )
            server.enqueueSse(
                """
                data: {"choices":[{"delta":{"content":"OpenAI 模式"}}]}

                data: [DONE]
                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = DIRECT_OPENAI_APP_ID, chatId = null, text = "测试 OpenAI")
            advanceUntilIdle()

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/v1/chat/completions", request.path)
            val requestBody = request.body.readUtf8()
            assertTrue(requestBody.contains("\"model\":\"gpt-4o-mini\""))
            assertTrue(requestBody.contains("\"role\":\"system\""))

            val messages = repository.observeMessages(DIRECT_OPENAI_APP_ID, chatId).first()
            assertEquals(2, messages.size)
            assertEquals("OpenAI 模式", messages.last().markdown)
            assertTrue(messages.last().citations.isNotEmpty())
        }

    @Test
    fun responsePlannerReceivesTrimmedDedupedLocalReferences() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            localKnowledgeStore.importText(
                title = "部署巡检：模型路由 offline cache",
                text = "这里记录部署巡检的通用前言。".repeat(30) + "关键规则：de-04-k21 使用 offline cache 做模型路由，并保留首条摘要。",
                sourceLabel = "manual",
                now = 2L,
            )
            waitUntilLocalKnowledgeReady()
            connectionProvider.update(
                connectionProvider.getSnapshot().copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/").toString(),
                    apiKey = "openai-secret",
                    model = "gpt-4o-mini",
                ),
            )

            val capturedRequests = mutableListOf<AssistantResponseRequest>()
            val plannedRepository =
                RoomChatRepository(
                    api = createRetrofit(server.url("/").toString(), OkHttpClient()).create(FastGptApi::class.java),
                    sseStreamClient = SseStreamClient(okHttpClient = OkHttpClient(), baseUrlProvider = connectionProvider),
                    conversationDao = database.conversationDao(),
                    conversationFolderDao = database.conversationFolderDao(),
                    conversationTagDao = database.conversationTagDao(),
                    interactiveDraftDao = database.interactiveDraftDao(),
                    messageDao = database.messageDao(),
                    context = ApplicationProvider.getApplicationContext(),
                    json = NetworkJson.default,
                    ioDispatcher = mainDispatcherRule.dispatcher,
                    responsePlanner =
                        AssistantResponsePlanner { request ->
                            capturedRequests += request
                            listOf(
                                AssistantResponseStep(
                                    markdownDelta = request.localReferences.firstOrNull()?.title ?: "missing",
                                ),
                            )
                        },
                    connectionSnapshotProvider = connectionProvider,
                    localKnowledgeStore = localKnowledgeStore,
                )

            val chatId = plannedRepository.sendMessage(
                appId = DIRECT_OPENAI_APP_ID,
                chatId = null,
                text = "de-04-k21 offline cache",
            )
            advanceUntilIdle()

            val request = capturedRequests.single()
            assertEquals(1, request.localReferences.size)
            assertEquals("部署巡检：模型路由 offline cache", request.localReferences.single().title)
            assertTrue(request.localReferences.single().snippet.length <= 223)

            val messages = plannedRepository.observeMessages(DIRECT_OPENAI_APP_ID, chatId).first()
            assertEquals("部署巡检：模型路由 offline cache", messages.last().markdown)

            plannedRepository.cancelBackgroundWork()
        }

    private suspend fun waitUntilLocalKnowledgeReady() {
        repeat(50) {
            val ready = localKnowledgeStore.observeDocuments().first().all {
                it.indexStatus == com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus.Ready
            }
            if (ready) {
                return
            }
            delay(20)
        }
        throw AssertionError("Local knowledge indexing did not finish in time")
    }

    @Test
    fun openAiStreamIgnoresJsonNullContentAndPreservesReasoning() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            connectionProvider.update(
                connectionProvider.getSnapshot().copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/").toString(),
                    apiKey = "openai-secret",
                    model = "gpt-4o-mini",
                ),
            )
            server.enqueueSse(
                """
                data: {"choices":[{"delta":{"content":null,"reasoning_content":"思考中"}}]}

                data: {"choices":[{"delta":{"content":"null","reasoning_content":"null"}}]}

                data: {"choices":[{"delta":{"content":"你好","reasoning_content":null}}]}

                data: [DONE]
                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = DIRECT_OPENAI_APP_ID, chatId = null, text = "kora")
            advanceUntilIdle()

            val messages = repository.observeMessages(DIRECT_OPENAI_APP_ID, chatId).first()
            assertEquals(2, messages.size)
            assertEquals("你好", messages.last().markdown)
            assertEquals("思考中", messages.last().reasoning)
        }

    @Test
    fun openAiStreamCoalescesCumulativeRepeatedChunks() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            connectionProvider.update(
                connectionProvider.getSnapshot().copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/").toString(),
                    apiKey = "openai-secret",
                    model = "gpt-4o-mini",
                ),
            )
            server.enqueueSse(
                """
                data: {"choices":[{"delta":{"content":"你好"}}]}

                data: {"choices":[{"delta":{"content":"你好，"}}]}

                data: {"choices":[{"delta":{"content":"你好，世界"}}]}

                data: {"choices":[{"delta":{"content":"你好，世界"}}]}

                data: {"choices":[{"delta":{"content":"你好，世界！"}}]}

                data: [DONE]
                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = DIRECT_OPENAI_APP_ID, chatId = null, text = "kora")
            advanceUntilIdle()

            val messages = repository.observeMessages(DIRECT_OPENAI_APP_ID, chatId).first()
            assertEquals(2, messages.size)
            assertEquals("你好，世界！", messages.last().markdown)
        }

    @Test
    fun blankChatIdInOpenAiModeIsNormalizedToGeneratedConversationId() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            connectionProvider.update(
                connectionProvider.getSnapshot().copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/").toString(),
                    apiKey = "openai-secret",
                    model = "gpt-4o-mini",
                ),
            )
            server.enqueueSse(
                """
                data: {"choices":[{"delta":{"content":"空 chatId 也应显示"}}]}

                data: [DONE]
                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = DIRECT_OPENAI_APP_ID, chatId = "", text = "kora")
            advanceUntilIdle()

            assertTrue(chatId.isNotBlank())
            val messages = repository.observeMessages(DIRECT_OPENAI_APP_ID, chatId).first()
            assertEquals(2, messages.size)
            assertEquals("kora", messages.first().markdown)
            assertEquals("空 chatId 也应显示", messages.last().markdown)
        }

    @Test
    fun nonDirectAppKeepsFastGptChatFlowEvenWhenConnectionTypeIsOpenAiCompatible() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            connectionProvider.update(
                connectionProvider.getSnapshot().copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/").toString(),
                    apiKey = "openai-secret",
                    model = "gpt-4o-mini",
                ),
            )
            server.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-openai-compat","appId":"app-1","title":"兼容应用"}}
                """.trimIndent(),
            )
            server.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"兼容模式应用回复"}}]}

                data: [DONE]
                """.trimIndent(),
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试兼容模式应用")
            advanceUntilIdle()

            val initRequest = server.takeRequest(2, TimeUnit.SECONDS)
            val completionRequest = server.takeRequest(2, TimeUnit.SECONDS)
            assertNotNull(initRequest)
            assertNotNull(completionRequest)
            val recordedInitRequest = requireNotNull(initRequest)
            val recordedCompletionRequest = requireNotNull(completionRequest)
            assertEquals("/api/core/chat/init?appId=app-1", recordedInitRequest.path)
            assertEquals("/api/v1/chat/completions", recordedCompletionRequest.path)

            val messages = repository.observeMessages("app-1", chatId).first()
            assertEquals(2, messages.size)
            assertEquals("兼容模式应用回复", messages.last().markdown)
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

    @Test
    fun observeMessagesCanBeCollectedFromMainThreadWithoutRoomViolation() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val strictDatabase =
                Room.inMemoryDatabaseBuilder(context, KoraDatabase::class.java)
                    .build()
            val strictRepository =
                RoomChatRepository(
                    api = createRetrofit(server.url("/").toString(), OkHttpClient()).create(FastGptApi::class.java),
                    sseStreamClient = SseStreamClient(okHttpClient = OkHttpClient(), baseUrlProvider = MutableConnectionProvider()),
                    conversationDao = strictDatabase.conversationDao(),
                    conversationFolderDao = strictDatabase.conversationFolderDao(),
                    conversationTagDao = strictDatabase.conversationTagDao(),
                    interactiveDraftDao = strictDatabase.interactiveDraftDao(),
                    messageDao = strictDatabase.messageDao(),
                context = context,
                json = NetworkJson.default,
                connectionSnapshotProvider = MutableConnectionProvider(),
            )

            try {
                val messages =
                    withContext(Dispatchers.Main) {
                        strictRepository.observeMessages(appId = "app-1", chatId = "chat-main-thread").first()
                    }

                assertTrue(messages.isEmpty())
            } finally {
                strictRepository.cancelBackgroundWork()
                strictDatabase.close()
            }
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
