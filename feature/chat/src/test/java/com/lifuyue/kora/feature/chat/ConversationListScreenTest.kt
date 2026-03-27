package com.lifuyue.kora.feature.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ConversationListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun conversationItemShowsActions() {
        composeRule.setContent {
            ConversationListScreen(
                uiState =
                    ConversationListUiState(
                        items =
                            listOf(
                                ConversationListItemUiModel(
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    title = "第一个会话",
                                    preview = "最新内容",
                                    isPinned = true,
                                ),
                            ),
                    ),
                onQueryChanged = {},
                onOpenConversation = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onRenameConversation = { _, _ -> },
                onTogglePin = { _, _ -> },
                onClearConversations = {},
            )
        }

        composeRule.onNodeWithText("置顶").assertIsDisplayed()
        composeRule.onNodeWithText("重命名").assertIsDisplayed()
        composeRule.onNodeWithText("删除").assertIsDisplayed()
    }
}
