package com.lifuyue.kora.feature.chat

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ChatRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ChatScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun composerShowsSendAndNoVoiceEntry() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", input = "hello"),
                onBack = {},
                onInputChanged = {},
                onSend = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = {},
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_PRIMARY_ACTION_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.chatString("chat_send")).assertIsDisplayed()
        composeRule.onAllNodesWithText("语音输入").assertCountEquals(0)
    }

    @Test
    fun assistantMessageDoesNotRenderTtsActions() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "msg-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "Answer",
                                ),
                            ),
                    ),
                onBack = {},
                onInputChanged = {},
                onSend = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = {},
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.messageCopyAction("msg-1")).assertIsDisplayed()
        composeRule.onAllNodesWithText("朗读").assertCountEquals(0)
        composeRule.onAllNodesWithText("暂停朗读").assertCountEquals(0)
    }

    @Test
    fun streamingAssistantMessageShowsPartialMarkdownAndExpandableReasoning() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        showReasoningEntry = true,
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "msg-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "Partial answer",
                                    reasoning = "internal chain",
                                    isStreaming = true,
                                ),
                            ),
                    ),
                onBack = {},
                onInputChanged = {},
                onSend = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = {},
                onToggleReasoning = {},
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.messageCard("msg-1")).assertIsDisplayed()
        composeRule.onAllNodesWithText(context.getString(R.string.chat_message_streaming_placeholder)).assertCountEquals(0)
        composeRule.onNodeWithText(context.getString(R.string.chat_message_reasoning_expand)).assertIsDisplayed()
    }

    @Test
    fun expandedReasoningRendersBody() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        showReasoningEntry = true,
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "msg-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "Answer",
                                    reasoning = "internal chain",
                                    isReasoningExpanded = true,
                                ),
                            ),
                    ),
                onBack = {},
                onInputChanged = {},
                onSend = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = {},
                onToggleReasoning = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.chat_message_reasoning_title)).assertIsDisplayed()
        composeRule.onNodeWithText("internal chain").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.chat_message_reasoning_collapse)).assertIsDisplayed()
    }
}
