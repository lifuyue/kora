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
                        isPinned = true,
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
            assertEquals(1, state.pinnedItems.size)
            assertTrue(state.otherItems.isEmpty())
            assertTrue(state.canClear)

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

    @Test
    fun pinnedItemsAreSeparatedFromRegularItems() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingConversationRepository()
            repository.emit(
                listOf(
                    ConversationListItemUiModel(
                        chatId = "chat-1",
                        appId = "app-1",
                        title = "Pinned",
                        preview = "Preview",
                        isPinned = true,
                    ),
                    ConversationListItemUiModel(
                        chatId = "chat-2",
                        appId = "app-1",
                        title = "Regular",
                        preview = "Preview",
                        isPinned = false,
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

            val state = viewModel.uiState.value
            assertEquals(listOf("chat-1"), state.pinnedItems.map { it.chatId })
            assertEquals(listOf("chat-2"), state.otherItems.map { it.chatId })
            collectJob.cancel()
        }

    @Test
    fun folderAndTagFiltersAreCombinedWithQuery() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = RecordingConversationRepository()
            repository.folders.value = listOf(ConversationFolderUiModel(folderId = "folder-1", name = "工作"))
            repository.tags.value = listOf(ConversationTagUiModel(tagId = "tag-1", name = "Kotlin", colorToken = "sky"))
            repository.emit(
                listOf(
                    ConversationListItemUiModel(
                        chatId = "chat-1",
                        appId = "app-1",
                        title = "Kotlin 方案",
                        preview = "实现细节",
                        folderId = "folder-1",
                        folderName = "工作",
                        tags = listOf(ConversationTagUiModel(tagId = "tag-1", name = "Kotlin", colorToken = "sky")),
                    ),
                    ConversationListItemUiModel(
                        chatId = "chat-2",
                        appId = "app-1",
                        title = "旅行计划",
                        preview = "路线",
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
            viewModel.selectFolder("folder-1")
            viewModel.selectTag("tag-1")
            viewModel.updateQuery("Kotlin")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf("chat-1"), state.items.map { it.chatId })
            assertEquals("工作", state.selectedFolderName)
            assertEquals("Kotlin", state.selectedTagName)
            collectJob.cancel()
        }
}

private class RecordingConversationRepository : ConversationRepository {
    private val items = MutableStateFlow<List<ConversationListItemUiModel>>(emptyList())
    val folders = MutableStateFlow<List<ConversationFolderUiModel>>(emptyList())
    val tags = MutableStateFlow<List<ConversationTagUiModel>>(emptyList())

    var renamedChatId: String? = null
    var pinned: Boolean = false
    var deletedChatId: String? = null
    var cleared: Boolean = false

    override fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>> = items

    override fun observeFolders(appId: String): Flow<List<ConversationFolderUiModel>> = folders

    override fun observeTags(appId: String): Flow<List<ConversationTagUiModel>> = tags

    override suspend fun refreshConversations(appId: String) = Unit

    override suspend fun renameConversation(
        appId: String,
        chatId: String,
        title: String,
    ) {
        renamedChatId = chatId
    }

    override suspend fun togglePinConversation(
        appId: String,
        chatId: String,
        pinned: Boolean,
    ) {
        this.pinned = pinned
    }

    override suspend fun deleteConversation(
        appId: String,
        chatId: String,
    ) {
        deletedChatId = chatId
    }

    override suspend fun clearConversations(appId: String) {
        cleared = true
    }

    override suspend fun createFolder(
        appId: String,
        name: String,
    ) = Unit

    override suspend fun renameFolder(
        appId: String,
        folderId: String,
        name: String,
    ) = Unit

    override suspend fun deleteFolder(
        appId: String,
        folderId: String,
    ) = Unit

    override suspend fun createTag(
        appId: String,
        name: String,
    ) = Unit

    override suspend fun renameTag(
        appId: String,
        tagId: String,
        name: String,
    ) = Unit

    override suspend fun deleteTag(
        appId: String,
        tagId: String,
    ) = Unit

    override suspend fun moveConversationToFolder(
        appId: String,
        chatId: String,
        folderId: String?,
    ) = Unit

    override suspend fun setConversationTags(
        appId: String,
        chatId: String,
        tagIds: List<String>,
    ) = Unit

    fun emit(value: List<ConversationListItemUiModel>) {
        items.value = value
    }
}
