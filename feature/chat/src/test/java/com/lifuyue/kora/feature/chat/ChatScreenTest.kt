package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun assistantMessageShowsActionsAndCodeCopyButton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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

        composeRule
            .onAllNodesWithText(context.getString(R.string.chat_copy), useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onAllNodesWithText(context.getString(R.string.chat_regenerate), useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onAllNodesWithText(context.getString(R.string.markdown_copy_code), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "en")
    fun markdownMessageLocalizesMermaidFallbackAndCopyCodeAction() {
        composeRule.setContent {
            MarkdownMessage(
                markdown =
                    """
                    ```mermaid
                    graph TD
                    A-->B
                    ```
                    """.trimIndent(),
                onCopyCode = {},
            )
        }

        composeRule.onNodeWithText("Copy code").assertIsDisplayed()
        composeRule.onNodeWithText("Mermaid diagrams are not rendered yet").assertIsDisplayed()
    }

    @Test
    fun streamingStateShowsStopButtonAndInlineStatus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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

        composeRule.onNodeWithText(context.getString(R.string.chat_stop_generation)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.chat_message_streaming)).assertIsDisplayed()
    }

    @Test
    fun stoppedAssistantShowsContinueGenerationAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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

        composeRule.onNodeWithText(context.getString(R.string.chat_continue_generation)).assertIsDisplayed()
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
                                                sourceName = "来源文档",
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
        composeRule.onNodeWithTag(ChatTestTags.CITATION_PANEL).assertIsDisplayed()
        composeRule.onNodeWithText("来源文档").assertIsDisplayed()
        composeRule.onNodeWithText("命中片段").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun citationSheetFormatsScoreAndUsesSourceNameFallbackInEnglish() {
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
                                                title = "",
                                                sourceName = "Knowledge Source",
                                                snippet = "Evidence snippet",
                                                scoreType = "semantic",
                                                score = 0.875,
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
        composeRule.onNodeWithText("Knowledge Source").assertIsDisplayed()
        composeRule.onNodeWithText("Evidence snippet").assertIsDisplayed()
        composeRule.onNodeWithText("semantic · 0.875").assertIsDisplayed()
    }
}
