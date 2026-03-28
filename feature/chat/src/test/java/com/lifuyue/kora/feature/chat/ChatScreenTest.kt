package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import org.junit.Assert.assertEquals
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
    fun markdownMessageShowsMermaidContainerAndCopyCodeAction() {
        val source =
            """
            graph TD
            A-->B
            """.trimIndent()

        composeRule.setContent {
            MarkdownMessage(
                markdown =
                    """
                    ```mermaid
                    $source
                    ```
                    """.trimIndent(),
                onCopyCode = {},
            )
        }

        composeRule.onNodeWithText("Copy code").assertIsDisplayed()
        composeRule.onNodeWithTag("${ChatTestTags.MERMAID_BLOCK_PREFIX}${source.hashCode()}").assertIsDisplayed()
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
    fun loadingStateShowsSkeletonCards() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        isInitialLoading = true,
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

        composeRule.onNodeWithTag(ChatTestTags.CHAT_SKELETON).assertIsDisplayed()
    }

    @Test
    fun scrollingUpPausesAutoScrollUntilResumeTapped() {
        var state by
            mutableStateOf(
                ChatUiState(
                    appId = "app-1",
                    chatId = "chat-1",
                    autoScrollEnabled = true,
                    messages =
                        (1..30).map { index ->
                            ChatMessageUiModel(
                                messageId = "assistant-$index",
                                chatId = "chat-1",
                                appId = "app-1",
                                role = ChatRole.AI,
                                markdown = "message-$index",
                            )
                        },
                ),
            )

        composeRule.setContent {
            ChatScreen(
                uiState = state,
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_LIST).performScrollToIndex(0)
        composeRule.runOnIdle {
            state =
                state.copy(
                    messages =
                        state.messages +
                            ChatMessageUiModel(
                                messageId = "assistant-31",
                                chatId = "chat-1",
                                appId = "app-1",
                                role = ChatRole.AI,
                                markdown = "message-31",
                            ),
                )
        }

        composeRule.onNodeWithTag(ChatTestTags.AUTO_SCROLL_RESUME).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.AUTO_SCROLL_RESUME).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(ChatTestTags.AUTO_SCROLL_RESUME).assertCountEquals(0)
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

    @Test
    fun assistantMessageShowsInlineInteractiveOptions() {
        var submittedValue: String? = null
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
                                    markdown = "请选择一个方案",
                                    interactiveCard =
                                        InteractiveCardUiModel(
                                            kind = InteractiveCardKind.UserSelect,
                                            messageDataId = "assistant-1",
                                            responseValueId = "response-1",
                                            options = listOf("Alpha", "Beta"),
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
                onSubmitInteractiveResponse = { _, value -> submittedValue = value },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.interactiveCard("assistant-1")).assertIsDisplayed()
        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onNodeWithText("Beta").assertIsDisplayed()
        composeRule.onNodeWithText("Alpha").performClick()
        composeRule.runOnIdle {
            assertEquals("Alpha", submittedValue)
        }
    }
}
