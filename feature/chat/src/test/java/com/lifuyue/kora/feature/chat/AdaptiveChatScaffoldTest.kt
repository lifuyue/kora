package com.lifuyue.kora.feature.chat

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AdaptiveChatScaffoldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedWidthShowsConversationListAndChatPaneTogether() {
        composeRule.setContent {
            AdaptiveChatScaffold(
                isExpanded = true,
                conversationPane = { Text("Conversations") },
                detailPane = { Text("Chat Detail") },
            )
        }

        composeRule.onNodeWithText("Conversations").assertIsDisplayed()
        composeRule.onNodeWithText("Chat Detail").assertIsDisplayed()
    }

    @Test
    fun compactWidthShowsOnlyChatPane() {
        composeRule.setContent {
            AdaptiveChatScaffold(
                isExpanded = false,
                conversationPane = { Text("Conversations") },
                detailPane = { Text("Chat Detail") },
            )
        }

        composeRule.onAllNodesWithText("Conversations").assertCountEquals(0)
        composeRule.onNodeWithText("Chat Detail").assertIsDisplayed()
    }
}
