package com.lifuyue.kora.feature.chat

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
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
    fun assistantMessageShowsActionsAndCodeCopyButton() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        input = "hello",
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "assistant-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "```kotlin\nprintln(\"hi\")\n```",
                                ),
                            ),
                    ),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onAllNodesWithText("复制", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithText("重新生成", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithText("复制代码", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun streamingStateShowsStopButtonAndInlineStatus() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "assistant-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "正在思考",
                                    isStreaming = true,
                                    deliveryState = MessageDeliveryState.Streaming,
                                ),
                            ),
                    ),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithText("停止生成").assertIsDisplayed()
        composeRule.onNodeWithText("生成中").assertIsDisplayed()
    }

    @Test
    fun stoppedAssistantShowsContinueGenerationAction() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "assistant-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "partial",
                                    isStreaming = false,
                                    deliveryState = MessageDeliveryState.Stopped,
                                    errorMessage = "已停止生成",
                                ),
                            ),
                    ),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithText("继续生成").assertIsDisplayed()
    }

    @Test
    fun citationSummaryOpensBottomSheetAndShowsCitationContent() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "assistant-1",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "answer",
                                    citations =
                                        listOf(
                                            CitationItemUiModel(
                                                datasetId = "dataset-1",
                                                collectionId = "collection-1",
                                                dataId = "data-1",
                                                title = "来源文档",
                                                snippet = "命中片段",
                                            ),
                                        ),
                                ),
                            ),
                    ),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.citationSummary("assistant-1")).performClick()
        composeRule.onNodeWithTag(ChatTestTags.citationPanel).assertIsDisplayed()
        composeRule.onNodeWithText("来源文档").assertIsDisplayed()
        composeRule.onNodeWithText("命中片段").assertIsDisplayed()
    }
}
