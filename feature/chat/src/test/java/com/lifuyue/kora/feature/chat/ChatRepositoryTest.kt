package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.network.NetworkFactory
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.StaticApiKeyProvider
import com.lifuyue.kora.core.network.StaticBaseUrlProvider
import com.lifuyue.kora.core.testing.MockWebServerRule
import com.lifuyue.kora.core.testing.RoomTestFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChatRepositoryTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private lateinit var database: KoraDatabase
    private lateinit var repository: RoomBackedChatRepository

    @Before
    fun setUp() {
        database = RoomTestFactory.inMemoryDatabase(ApplicationProvider.getApplicationContext())
        val baseUrl = serverRule.baseUrl
        repository =
            RoomBackedChatRepository(
                api =
                    NetworkFactory.createApi(
                        baseUrl = baseUrl,
                        apiKeyProvider = StaticApiKeyProvider("fastgpt-secret"),
                    ),
                sseStreamClient =
                    SseStreamClient(
                        okHttpClient =
                            NetworkFactory.createOkHttpClient(
                                apiKeyProvider = StaticApiKeyProvider("fastgpt-secret"),
                                baseUrlProvider = StaticBaseUrlProvider(baseUrl),
                            ),
                        baseUrlProvider = StaticBaseUrlProvider(baseUrl),
                    ),
                conversationDao = database.conversationDao(),
                conversationFolderDao = database.conversationFolderDao(),
                conversationTagDao = database.conversationTagDao(),
                interactiveDraftDao = database.interactiveDraftDao(),
                messageDao = database.messageDao(),
                context = ApplicationProvider.getApplicationContext(),
            )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun sendMessageInsertsHumanAndAssistantPlaceholderThenMergesStreamIntoOneAssistantMessage() =
        runBlocking {
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-1","appId":"app-1","title":"新会话"}}
                """,
            )
            serverRule.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"你好","reasoning_content":"思考中"}}]}

                event: fastAnswer
                data: {"choices":[{"delta":{"content":"，世界"}}]}

                event: toolCall
                data: {"tool":{"id":"tool-1","toolName":"search"}}

                event: plan
                data: {"steps":[{"title":"step-1"}]}

                data: [DONE]

                event: flowResponses
                data: {"items":[{"id":"node-1"}]}
                """,
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试问题")

            val initial = repository.observeMessages("app-1", chatId).first()
            assertEquals(2, initial.size)
            assertEquals(ChatRole.Human, initial.first().role)
            assertEquals(ChatRole.AI, initial.last().role)

            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "sent"
            }

            val completed = repository.observeMessages("app-1", chatId).first()
            val assistant = completed.last()
            assertEquals(2, completed.size)
            assertEquals(MessageDeliveryState.Sent, assistant.deliveryState)
            assertEquals("你好，世界", assistant.markdown)
            assertEquals("思考中", assistant.reasoning)
            assertTrue(database.messageDao().getMessagesForChat(chatId).last().payloadJson.contains("tool-1"))
            assertTrue(database.messageDao().getMessagesForChat(chatId).last().payloadJson.contains("node-1"))
            assertEquals("你好，世界", database.conversationDao().getConversationByChatId(chatId)?.lastMessagePreview)

            val initRequest = serverRule.takeRequest()
            val request = serverRule.takeRequest()
            assertEquals("/api/core/chat/init?appId=app-1", initRequest.path)
            assertEquals("/api/v1/chat/completions", request.path)
            assertTrue(request.body.readUtf8().contains(assistant.messageId))
        }

    @Test
    fun errorEventPreservesPartialAnswerAndMarksAssistantFailed() =
        runBlocking {
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-2","appId":"app-1"}}
                """,
            )
            serverRule.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"partial"}}]}

                event: error
                data: {"code":514,"statusText":"unAuthApiKey","message":"invalid"}

                """,
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试错误")

            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "failed"
            }

            val assistant = repository.observeMessages("app-1", chatId).first().last()
            assertEquals(MessageDeliveryState.Failed, assistant.deliveryState)
            assertEquals("partial", assistant.markdown)
            assertEquals("invalid", assistant.errorMessage)
        }

    @Test
    fun stopAndContinueGenerationKeepStoppedMessageAndAppendNewAssistant() =
        runBlocking {
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-stop","appId":"app-1"}}
                """,
            )
            var plannerInvocation = 0
            val stopRepository =
                RoomBackedChatRepository(
                    api =
                        NetworkFactory.createApi(
                            baseUrl = serverRule.baseUrl,
                            apiKeyProvider = StaticApiKeyProvider("fastgpt-secret"),
                        ),
                    sseStreamClient =
                        SseStreamClient(
                            okHttpClient =
                                NetworkFactory.createOkHttpClient(
                                    apiKeyProvider = StaticApiKeyProvider("fastgpt-secret"),
                                    baseUrlProvider = StaticBaseUrlProvider(serverRule.baseUrl),
                                ),
                            baseUrlProvider = StaticBaseUrlProvider(serverRule.baseUrl),
                        ),
                    conversationDao = database.conversationDao(),
                    conversationFolderDao = database.conversationFolderDao(),
                    conversationTagDao = database.conversationTagDao(),
                    interactiveDraftDao = database.interactiveDraftDao(),
                    messageDao = database.messageDao(),
                    context = ApplicationProvider.getApplicationContext(),
                    responsePlanner =
                        AssistantResponsePlanner {
                            plannerInvocation += 1
                            if (plannerInvocation == 1) {
                                listOf(
                                    AssistantResponseStep(markdownDelta = "先到这里"),
                                    AssistantResponseStep(markdownDelta = "后续内容", delayMillis = 2_000),
                                )
                            } else {
                                listOf(AssistantResponseStep(markdownDelta = "继续完成"))
                            }
                        },
                )

            val chatId = stopRepository.sendMessage(appId = "app-1", chatId = null, text = "测试停止继续")

            waitUntil {
                val messages = database.messageDao().getMessagesForChat(chatId)
                messages.size == 2 && messages.last().isStreaming
            }
            stopRepository.stopStreaming(appId = "app-1", chatId = chatId)
            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "stopped"
            }

            stopRepository.continueGeneration(appId = "app-1", chatId = chatId)

            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "sent" &&
                    database.messageDao().getMessagesForChat(chatId).size == 3
            }

            val messages = stopRepository.observeMessages("app-1", chatId).first()
            assertEquals(3, messages.size)
            assertEquals(MessageDeliveryState.Stopped, messages[1].deliveryState)
            assertEquals(MessageDeliveryState.Sent, messages[2].deliveryState)
            assertEquals("继续完成", messages[2].markdown)
        }

    @Test
    fun regenerateDeletesPreviousAssistantThenResendsLastUserMessage() =
        runBlocking {
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-3","appId":"app-1"}}
                """,
            )
            serverRule.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"第一次回答"}}]}

                data: [DONE]
                """,
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "原问题")

            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "sent"
            }

            val firstAssistantId = repository.observeMessages("app-1", chatId).first().last().messageId

            serverRule.enqueueJson("""{"code":200,"statusText":"","message":"","data":{}}""")
            serverRule.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"第二次回答"}}]}

                data: [DONE]
                """,
            )

            repository.regenerateResponse(appId = "app-1", chatId = chatId, messageId = firstAssistantId)

            waitUntil {
                val messages = database.messageDao().getMessagesForChat(chatId)
                messages.size == 2 && messages.last().sendStatus == "sent"
            }

            val messages = repository.observeMessages("app-1", chatId).first()
            assertEquals(2, messages.size)
            assertNotEquals(firstAssistantId, messages.last().messageId)
            assertEquals("第二次回答", messages.last().markdown)

            val initRequest = serverRule.takeRequest()
            val firstSend = serverRule.takeRequest()
            val deleteRequest = serverRule.takeRequest()
            val secondSend = serverRule.takeRequest()
            assertEquals("/api/core/chat/init?appId=app-1", initRequest.path)
            assertEquals("/api/v1/chat/completions", firstSend.path)
            assertEquals("/api/core/chat/item/delete", deleteRequest.path)
            assertTrue(deleteRequest.body.readUtf8().contains(firstAssistantId))
            assertEquals("/api/v1/chat/completions", secondSend.path)
            assertTrue(secondSend.body.readUtf8().contains("原问题"))
        }

    @Test
    fun feedbackUpdatesLocalStateAndCallsFeedbackEndpoint() =
        runBlocking {
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-4","appId":"app-1"}}
                """,
            )
            serverRule.enqueueSse(
                """
                event: answer
                data: {"choices":[{"delta":{"content":"可反馈答案"}}]}

                data: [DONE]
                """,
            )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "需要反馈")

            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "sent"
            }

            val assistantId = repository.observeMessages("app-1", chatId).first().last().messageId
            serverRule.enqueueJson("""{"code":200,"statusText":"","message":"","data":{}}""")

            repository.setFeedback(
                appId = "app-1",
                chatId = chatId,
                messageId = assistantId,
                feedback = MessageFeedback.Upvote,
            )

            val assistant = repository.observeMessages("app-1", chatId).first().last()
            assertEquals(MessageFeedback.Upvote, assistant.feedback)

            val initRequest = serverRule.takeRequest()
            val chatRequest = serverRule.takeRequest()
            val feedbackRequest = serverRule.takeRequest()
            assertEquals("/api/core/chat/init?appId=app-1", initRequest.path)
            assertEquals("/api/v1/chat/completions", chatRequest.path)
            assertEquals("/api/core/chat/feedback/updateUserFeedback", feedbackRequest.path)
            assertTrue(feedbackRequest.body.readUtf8().contains(assistantId))
        }

    @Test
    fun sendMessageAssemblesAttachmentPartsIntoCompletionRequest() =
        runBlocking {
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"chatId":"chat-init-attachments","appId":"app-1","title":"附件会话"}}
                """,
            )
            serverRule.enqueueSse(
                """
                data: [DONE]
                """,
            )

            val chatId =
                repository.sendMessage(
                    appId = "app-1",
                    chatId = null,
                    text = "see attachments",
                    attachments =
                        listOf(
                            AttachmentDraftUiModel(
                                displayName = "photo.png",
                                localUri = "content://local/photo.png",
                                mimeType = "image/png",
                                kind = AttachmentKind.Image,
                                uploadStatus = AttachmentUploadStatus.Uploaded,
                                uploadedRef =
                                    com.lifuyue.kora.core.network.UploadedAssetRef(
                                        name = "photo.png",
                                        url = "https://example.com/photo.png",
                                        key = "chat/photo.png",
                                        mimeType = "image/png",
                                        size = 1L,
                                    ),
                            ),
                            AttachmentDraftUiModel(
                                displayName = "notes.pdf",
                                localUri = "content://local/notes.pdf",
                                mimeType = "application/pdf",
                                kind = AttachmentKind.File,
                                uploadStatus = AttachmentUploadStatus.Uploaded,
                                uploadedRef =
                                    com.lifuyue.kora.core.network.UploadedAssetRef(
                                        name = "notes.pdf",
                                        url = "https://example.com/notes.pdf",
                                        key = "chat/notes.pdf",
                                        mimeType = "application/pdf",
                                        size = 1L,
                                    ),
                            ),
                        ),
                )

            waitUntil {
                database.messageDao().getMessagesForChat(chatId).last().sendStatus == "sent"
            }

            val initRequest = serverRule.takeRequest()
            val chatRequest = serverRule.takeRequest()
            val body = chatRequest.body.readUtf8()
            assertEquals("/api/core/chat/init?appId=app-1", initRequest.path)
            assertEquals("/api/v1/chat/completions", chatRequest.path)
            assertTrue(body.contains("\"file_url\""))
            assertTrue(body.contains("https://example.com/photo.png"))
            assertTrue(body.contains("https://example.com/notes.pdf"))
        }

    @Test
    fun uploadAttachmentUsesDedicatedChatEndpointAndReportsProgress() =
        runBlocking {
            val tempFile = File.createTempFile("chat-attachment-", ".txt", ApplicationProvider.getApplicationContext<Context>().cacheDir).apply {
                writeText("hello upload")
            }
            serverRule.enqueueJson(
                """
                {"code":200,"statusText":"","message":"","data":{"name":"hello.txt","url":"https://example.com/hello.txt","key":"chat/hello.txt","mimeType":"text/plain","size":12}}
                """,
            )

            var reportedProgress = 0f
            val uploaded =
                repository.uploadAttachment(
                    appId = "app-1",
                    chatId = "chat-1",
                    attachment =
                        AttachmentDraftUiModel(
                            displayName = "hello.txt",
                            localUri = tempFile.toURI().toString(),
                            mimeType = "text/plain",
                            sizeBytes = tempFile.length(),
                            kind = AttachmentKind.File,
                        ),
                ) { progress ->
                    reportedProgress = progress
                }

            assertEquals("hello.txt", uploaded.name)
            assertTrue(reportedProgress >= 1f)
            assertEquals("/api/core/chat/attachment/upload", serverRule.takeRequest().path)
        }

    private fun waitUntil(
        timeoutMs: Long = 3_000,
        condition: () -> Boolean,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(20)
        }
        throw AssertionError("Condition not satisfied within ${timeoutMs}ms")
    }
}
