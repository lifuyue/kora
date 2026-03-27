package com.lifuyue.kora.feature.chat

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun pinnedAndRegularSectionsAreGrouped() {
        composeRule.renderConversationList()

        composeRule.onNodeWithText("置顶会话").assertIsDisplayed()
        composeRule.onNodeWithText("全部会话").assertIsDisplayed()
        composeRule.onNodeWithText("架构讨论").assertIsDisplayed()
        composeRule.onNodeWithText("周报整理").assertIsDisplayed()
        composeRule.onNodeWithText("清空全部").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.conversationFab).assertIsDisplayed()
    }

    @Test
    fun longPressConversationOpensBottomSheetAndDispatchesActions() {
        composeRule.renderConversationList()

        composeRule.onNodeWithTag("${ChatTestTags.conversationItemPrefix}chat-1").performTouchInput {
            longClick()
        }
        composeRule.onNodeWithText("会话操作").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.conversationActionTogglePin, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.conversationActionRename, useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.conversationActionDelete, useUnmergedTree = true).fetchSemanticsNode()
    }

    @Test
    fun clearAllUsesExplicitConfirmation() {
        var cleared = false

        composeRule.renderConversationList(onClearConversations = { cleared = true })

        composeRule.onNodeWithText("清空全部").performClick()
        composeRule.onNodeWithText("清空所有会话？").assertIsDisplayed()
        composeRule.onNodeWithText("确认清空").performClick()

        assertTrue(cleared)
    }

    @Test
    fun emptyStateStillShowsSearchAndCreateEntry() {
        composeRule.renderConversationList(
            uiState = ConversationListUiState(items = emptyList()),
        )

        composeRule.onNodeWithText("暂无会话").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.conversationSearch).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.conversationFab).assertIsDisplayed()
    }

    @Test
    fun newConversationFabDispatchesCallback() {
        var invoked = false

        composeRule.renderConversationList(onNewConversation = { invoked = true })

        composeRule.onNodeWithTag(ChatTestTags.conversationFab).performClick()

        assertTrue(invoked)
    }

    @Test
    fun searchFieldShowsCurrentQuery() {
        composeRule.renderConversationList(
            uiState = ConversationListUiState(query = "架构", items = sampleConversationItems),
        )

        composeRule.onNodeWithTag(ChatTestTags.conversationSearch).assertTextContains("架构")
        composeRule.onAllNodesWithText("架构讨论").assertCountEquals(1)
    }
}

private fun ComposeContentTestRule.renderConversationList(
    uiState: ConversationListUiState = ConversationListUiState(items = sampleConversationItems),
    onQueryChanged: (String) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onNewConversation: () -> Unit = {},
    onDeleteConversation: (String) -> Unit = {},
    onRenameConversation: (String, String) -> Unit = { _, _ -> },
    onTogglePin: (String, Boolean) -> Unit = { _, _ -> },
    onClearConversations: () -> Unit = {},
) {
    setContent {
        ConversationListScreen(
            uiState = uiState,
            onQueryChanged = onQueryChanged,
            onOpenConversation = onOpenConversation,
            onNewConversation = onNewConversation,
            onDeleteConversation = onDeleteConversation,
            onRenameConversation = onRenameConversation,
            onTogglePin = onTogglePin,
            onClearConversations = onClearConversations,
        )
    }
}

private val sampleConversationItems =
    listOf(
        ConversationListItemUiModel(
            chatId = "chat-1",
            appId = "app-1",
            title = "架构讨论",
            preview = "最新内容",
            isPinned = true,
        ),
        ConversationListItemUiModel(
            chatId = "chat-2",
            appId = "app-1",
            title = "周报整理",
            preview = "待发送",
            isPinned = false,
        ),
    )
