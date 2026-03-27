package com.lifuyue.kora.feature.chat

import androidx.lifecycle.SavedStateHandle
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendUsesRepositoryAndClearsComposer() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                ChatViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to null)),
                    chatRepository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("你好")
            viewModel.send()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.input)
            assertEquals("chat-1", state.chatId)
            assertEquals(2, state.messages.size)
            assertEquals("你好", repository.sentText)
            collectJob.cancel()
        }

    @Test
    fun sendFailureShowsInlineError() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository(shouldFailSend = true)
            val viewModel =
                ChatViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to null)),
                    chatRepository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.updateInput("失败案例")
            viewModel.send()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("发送失败", state.errorMessage)
            assertEquals("失败案例", state.input)
            collectJob.cancel()
        }

    @Test
    fun regenerateStopAndFeedbackDelegateToRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                ChatViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to "chat-1")),
                    chatRepository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            val message =
                ChatMessageUiModel(
                    messageId = "assistant-1",
                    chatId = "chat-1",
                    appId = "app-1",
                    role = ChatRole.AI,
                    markdown = "hello",
                )
            repository.emitMessages("chat-1", listOf(message))

            viewModel.regenerate(message)
            viewModel.stopGeneration()
            viewModel.updateFeedback(message, MessageFeedback.Downvote)
            advanceUntilIdle()

            assertEquals("assistant-1", repository.regeneratedMessageId)
            assertEquals("chat-1", repository.stoppedChatId)
            assertEquals(MessageFeedback.Downvote, repository.feedback)
            collectJob.cancel()
        }

    @Test
    fun continueGenerationDelegatesToRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingChatRepository()
            val viewModel =
                ChatViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to "chat-1")),
                    chatRepository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.continueGeneration()
            advanceUntilIdle()

            assertEquals("chat-1", repository.continuedChatId)
            collectJob.cancel()
        }
}

private class RecordingChatRepository(
    private val shouldFailSend: Boolean = false,
) : ChatRepository {
    private val messagesByChat =
        MutableStateFlow<Map<String, List<ChatMessageUiModel>>>(emptyMap())

    var sentText: String? = null
    var regeneratedMessageId: String? = null
    var stoppedChatId: String? = null
    var continuedChatId: String? = null
    var feedback: MessageFeedback? = null

    override fun observeMessages(appId: String, chatId: String?): Flow<List<ChatMessageUiModel>> =
        messagesByChat.map { it[chatId].orEmpty() }

    override suspend fun bootstrapChat(appId: String): ChatBootstrap =
        ChatBootstrap(
            chatId = "chat-1",
            welcomeText = "欢迎语",
        )

    override suspend fun restoreMessages(appId: String, chatId: String) = Unit

    override suspend fun sendMessage(appId: String, chatId: String?, text: String): String {
        if (shouldFailSend) {
            error("发送失败")
        }
        sentText = text
        val resolvedChatId = chatId ?: "chat-1"
        emitMessages(
            resolvedChatId,
            listOf(
                ChatMessageUiModel(
                    messageId = "user-1",
                    chatId = resolvedChatId,
                    appId = appId,
                    role = ChatRole.Human,
                    markdown = text,
                ),
                ChatMessageUiModel(
                    messageId = "assistant-1",
                    chatId = resolvedChatId,
                    appId = appId,
                    role = ChatRole.AI,
                    markdown = "已收到",
                ),
            ),
        )
        return resolvedChatId
    }

    override suspend fun stopStreaming(appId: String, chatId: String) {
        stoppedChatId = chatId
    }

    override suspend fun continueGeneration(appId: String, chatId: String): String {
        continuedChatId = chatId
        return chatId
    }

    override suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String {
        regeneratedMessageId = messageId
        return chatId
    }

    override suspend fun setFeedback(appId: String, chatId: String, messageId: String, feedback: MessageFeedback) {
        this.feedback = feedback
    }

    fun emitMessages(chatId: String, messages: List<ChatMessageUiModel>) {
        messagesByChat.value =
            messagesByChat.value.toMutableMap().apply {
                put(chatId, messages)
            }
    }
}
