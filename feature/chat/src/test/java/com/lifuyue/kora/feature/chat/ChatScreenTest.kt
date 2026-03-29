package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            .onAllNodesWithText(context.chatString("chat_copy"), useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onAllNodesWithText(context.chatString("chat_regenerate"), useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onAllNodesWithText(context.getString(R.string.markdown_copy_code), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun speechInputStateShowsComposerBackfillAndControls() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        input = "",
                        speechInputState =
                            SpeechInputUiState(
                                status = SpeechInputStatus.Recording,
                                transcript = "Backfilled transcript",
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

        composeRule.onNodeWithTag("chat-speech-status").assertExists()
        composeRule.onNodeWithTag("chat-mic-button").assertExists()
        composeRule.onNodeWithText("Backfilled transcript").assertExists()
    }

    @Test
    fun assistantMessageShowsPlaybackActionsForActiveMessage() {
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
                                    markdown = "Read me",
                                ),
                            ),
                        ttsPlaybackState =
                            TtsPlaybackUiState(
                                messageId = "assistant-1",
                                status = TtsPlaybackStatus.Playing,
                            ),
                    ),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
                onPlayMessage = { _, _ -> },
                onPausePlayback = {},
                onStopPlayback = {},
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.messageTtsAction("assistant-1")).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.messageTtsPauseAction("assistant-1")).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.messageTtsStopAction("assistant-1")).assertExists()
    }

    @Test
    fun composerShowsAttachmentActionsAndPreviewControls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var pickedImage = false
        var pickedFile = false
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        attachmentConfig =
                            ChatAttachmentConfig(
                                canSelectImg = true,
                                canSelectFile = true,
                            ),
                        attachments =
                            listOf(
                                AttachmentDraftUiModel(
                                    displayName = "picture.png",
                                    localUri = "content://local/picture.png",
                                    mimeType = "image/png",
                                    kind = AttachmentKind.Image,
                                    uploadStatus = AttachmentUploadStatus.Uploaded,
                                ),
                                AttachmentDraftUiModel(
                                    displayName = "uploading.pdf",
                                    localUri = "content://local/uploading.pdf",
                                    mimeType = "application/pdf",
                                    kind = AttachmentKind.File,
                                    uploadStatus = AttachmentUploadStatus.Uploading,
                                    progress = 0.5f,
                                ),
                                AttachmentDraftUiModel(
                                    displayName = "failed.txt",
                                    localUri = "content://local/failed.txt",
                                    mimeType = "text/plain",
                                    kind = AttachmentKind.File,
                                    uploadStatus = AttachmentUploadStatus.Failed,
                                    errorMessage = "上传失败",
                                ),
                            ),
                    ),
                onInputChanged = {},
                onSend = {},
                onPickImage = { pickedImage = true },
                onPickFile = { pickedFile = true },
                onRemoveAttachment = {},
                onRetryAttachment = {},
                onCancelAttachmentUpload = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_IMAGE_PICK).performClick()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_FILE_PICK).performClick()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("picture.png").assertIsDisplayed()
        composeRule.onNodeWithText("uploading.pdf").fetchSemanticsNode()
        composeRule.onNodeWithText("failed.txt").assertExists()
        composeRule.onNodeWithText(context.chatString("chat_attachment_cancel"), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(context.chatString("chat_attachment_retry"), useUnmergedTree = true).assertExists()
        composeRule.runOnIdle {
            assertTrue(pickedImage)
            assertTrue(pickedFile)
        }
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

        composeRule.onNodeWithText(context.chatString("chat_stop_generation")).assertIsDisplayed()
        composeRule.onNodeWithText(context.chatString("chat_message_streaming")).assertExists()
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

        composeRule.onNodeWithText(context.chatString("chat_continue_generation")).assertExists()
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

        composeRule.onNodeWithTag(ChatTestTags.CHAT_SKELETON).assertExists()
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
        var openedCitation: CitationItemUiModel? = null
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
                onOpenCitation = { openedCitation = it },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.citationSummary("assistant-1")).performClick()
        composeRule.runOnIdle {
            assertEquals("来源文档", openedCitation?.title)
            assertEquals("命中片段", openedCitation?.snippet)
        }
    }

    @Test
    @Config(qualifiers = "en")
    fun citationSheetFormatsScoreAndUsesSourceNameFallbackInEnglish() {
        var openedCitation: CitationItemUiModel? = null
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
                onOpenCitation = { openedCitation = it },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.citationSummary("assistant-1")).performClick()
        composeRule.runOnIdle {
            assertEquals("Knowledge Source", openedCitation?.sourceName)
            assertEquals("Evidence snippet", openedCitation?.snippet)
            assertEquals(0.875, openedCitation?.score ?: 0.0, 0.0)
        }
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

        composeRule.onNodeWithTag(ChatTestTags.interactiveCard("assistant-1")).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.interactiveOption("assistant-1", "Alpha")).performClick()
        composeRule.runOnIdle {
            assertEquals("Alpha", submittedValue)
        }
    }

    @Test
    fun collectionFormRendersMultipleInputsAndRequiresAllValuesBeforeSubmit() {
        composeRule.setContent {
            ChatScreen(
                uiState =
                    ChatUiState(
                        appId = "app-1",
                        chatId = "chat-1",
                        messages =
                            listOf(
                                ChatMessageUiModel(
                                    messageId = "assistant-2",
                                    chatId = "chat-1",
                                    appId = "app-1",
                                    role = ChatRole.AI,
                                    markdown = "填写信息",
                                    interactiveCard =
                                        InteractiveCardUiModel(
                                            kind = InteractiveCardKind.CollectionForm,
                                            messageDataId = "assistant-2",
                                            responseValueId = "response-2",
                                            fields =
                                                listOf(
                                                    InteractiveFieldUiModel(id = "topic", label = "Topic"),
                                                    InteractiveFieldUiModel(id = "details", label = "Details"),
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

        composeRule.onNodeWithTag(ChatTestTags.interactiveFieldInput("assistant-2", "topic"), useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.interactiveFieldInput("assistant-2", "details"), useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag(ChatTestTags.interactiveSubmit("assistant-2")).assertIsNotEnabled()

        composeRule.onNodeWithTag(ChatTestTags.interactiveFieldInput("assistant-2", "topic")).performTextInput("Kotlin")
        composeRule.onNodeWithTag(ChatTestTags.interactiveSubmit("assistant-2")).assertIsNotEnabled()
        composeRule.onNodeWithTag(ChatTestTags.interactiveFieldInput("assistant-2", "details")).performTextInput("Flow")
        composeRule.onNodeWithTag(ChatTestTags.interactiveSubmit("assistant-2")).assertIsEnabled()
    }
}
