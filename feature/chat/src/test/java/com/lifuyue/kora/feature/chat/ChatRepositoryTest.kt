package com.lifuyue.kora.feature.chat

import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendMessageStreamsAssistantAndUpdatesConversationPreview() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                InMemoryChatRepository(
                    scope = CoroutineScope(SupervisorJob() + mainDispatcherRule.dispatcher),
                    responsePlanner =
                        AssistantResponsePlanner {
                            listOf(
                                AssistantResponseStep(markdownDelta = "你好，", delayMillis = 100),
                                AssistantResponseStep(markdownDelta = "这里是代码：\n```kotlin\nprintln(\"hi\")\n```", delayMillis = 100),
                            )
                        },
                )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试")
            val initial = repository.observeMessages("app-1", chatId).first()
            assertEquals(2, initial.size)
            assertEquals(ChatRole.Human, initial.first().role)
            assertTrue(initial.last().isStreaming)

            advanceUntilIdle()

            val completed = repository.observeMessages("app-1", chatId).first()
            assertEquals(MessageDeliveryState.Sent, completed.last().deliveryState)
            assertTrue(completed.last().markdown.contains("println(\"hi\")"))

            val items = repository.observeConversations("app-1").first()
            assertEquals(1, items.size)
            assertTrue(items.first().preview.contains("println(\"hi\")"))
        }

    @Test
    fun stopStreamingPreservesPartialAnswer() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                InMemoryChatRepository(
                    scope = CoroutineScope(SupervisorJob() + mainDispatcherRule.dispatcher),
                    responsePlanner =
                        AssistantResponsePlanner {
                            listOf(
                                AssistantResponseStep(markdownDelta = "第一段"),
                                AssistantResponseStep(markdownDelta = "第二段", delayMillis = 100),
                            )
                        },
                )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "测试停止")
            runCurrent()

            repository.stopStreaming(appId = "app-1", chatId = chatId)
            advanceUntilIdle()

            val assistant = repository.observeMessages("app-1", chatId).first().last()
            assertTrue(assistant.markdown.isNotBlank())
            assertTrue(assistant.markdown.contains("第一段"))
            assertEquals(MessageDeliveryState.Stopped, assistant.deliveryState)
            assertEquals("已停止生成", assistant.errorMessage)
        }

    @Test
    fun regenerateAndFeedbackUpdateLatestAssistantMessage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository =
                InMemoryChatRepository(
                    scope = CoroutineScope(SupervisorJob() + mainDispatcherRule.dispatcher),
                    responsePlanner =
                        AssistantResponsePlanner { request ->
                            listOf(
                                AssistantResponseStep(
                                    markdownDelta = "第${request.attempt}次回答：${request.prompt}",
                                ),
                            )
                        },
                )

            val chatId = repository.sendMessage(appId = "app-1", chatId = null, text = "原问题")
            advanceUntilIdle()

            var assistantId = ""
            val collectJob =
                launch {
                    repository.observeMessages("app-1", chatId).collect { messages ->
                        assistantId = messages.last().messageId
                    }
                }
            advanceUntilIdle()

            repository.regenerateResponse("app-1", chatId, assistantId)
            advanceUntilIdle()

            val regenerated = repository.observeMessages("app-1", chatId).first().last()
            assertTrue(regenerated.markdown.contains("第2次回答"))
            repository.setFeedback("app-1", chatId, regenerated.messageId, MessageFeedback.Upvote)
            val updated = repository.observeMessages("app-1", chatId).first().last()
            assertEquals(MessageFeedback.Upvote, updated.feedback)
            collectJob.cancel()
        }
}
