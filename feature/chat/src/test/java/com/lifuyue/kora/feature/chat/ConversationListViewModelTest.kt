package com.lifuyue.kora.feature.chat

import androidx.lifecycle.SavedStateHandle
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun queryFiltersItemsAndActionsDelegate() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingConversationRepository()
            repository.emit(
                listOf(
                    ConversationListItemUiModel(
                        chatId = "chat-1",
                        appId = "app-1",
                        title = "安卓调试",
                        preview = "代码块",
                    ),
                    ConversationListItemUiModel(
                        chatId = "chat-2",
                        appId = "app-1",
                        title = "旅行计划",
                        preview = "周末",
                    ),
                ),
            )
            val viewModel =
                ConversationListViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1")),
                    conversationRepository = repository,
                )
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.updateQuery("安卓")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertEquals("chat-1", state.items.first().chatId)

            viewModel.renameConversation("chat-1", "安卓调试*")
            viewModel.togglePin("chat-1", true)
            viewModel.deleteConversation("chat-2")
            viewModel.clearConversations()
            advanceUntilIdle()

            assertEquals("chat-1", repository.renamedChatId)
            assertTrue(repository.pinned)
            assertEquals("chat-2", repository.deletedChatId)
            assertTrue(repository.cleared)
            collectJob.cancel()
        }
}

private class RecordingConversationRepository : ConversationRepository {
    private val items = MutableStateFlow<List<ConversationListItemUiModel>>(emptyList())

    var renamedChatId: String? = null
    var pinned: Boolean = false
    var deletedChatId: String? = null
    var cleared: Boolean = false

    override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> = items

    override suspend fun refreshConversations(appId: String) = Unit

    override suspend fun renameConversation(appId: String, chatId: String, title: String) {
        renamedChatId = chatId
    }

    override suspend fun togglePinConversation(appId: String, chatId: String, pinned: Boolean) {
        this.pinned = pinned
    }

    override suspend fun deleteConversation(appId: String, chatId: String) {
        deletedChatId = chatId
    }

    override suspend fun clearConversations(appId: String) {
        cleared = true
    }

    fun emit(value: List<ConversationListItemUiModel>) {
        items.value = value
    }
}
