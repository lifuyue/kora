package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
@OptIn(ExperimentalTestApi::class)
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
    @Config(qualifiers = "zh-rCN")
    fun emptyChatShowsGeminiLandingLayout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_MENU_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag("chat_workspace_header").assertIsDisplayed()
        composeRule.onNodeWithText("Kora").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_greeting)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_knowledge_search)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_knowledge_summary)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_knowledge_answer)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_explain_concept)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_daily_reply)).assertExists()
        composeRule.onNodeWithText("富悦，你好").assertDoesNotExist()
        composeRule.onNodeWithText("制作图片").assertDoesNotExist()
        composeRule.onNodeWithText("创作音乐").assertDoesNotExist()
        composeRule.onNodeWithText("创作视频").assertDoesNotExist()
        composeRule.onNodeWithTag("${ChatTestTags.CHAT_SUGGESTION_PREFIX}0").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun emptyChatUsesUpdatedEnglishGreetingAndSuggestions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.chat_greeting)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_knowledge_search)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_knowledge_summary)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_knowledge_answer)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_explain_concept)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.chat_suggestion_daily_reply)).assertExists()
        composeRule.onNodeWithText("Hi, Fuyue").assertDoesNotExist()
        composeRule.onNodeWithText("Create images").assertDoesNotExist()
        composeRule.onNodeWithText("Make music").assertDoesNotExist()
        composeRule.onNodeWithText("Create video").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "zh-rCN")
    fun emptyChatSuggestionClickDispatchesKnowledgePrompt() {
        var suggestedQuestion: String? = null
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
                onSuggestedQuestion = { suggestedQuestion = it },
            )
        }

        val label = context.getString(R.string.chat_suggestion_knowledge_search)
        composeRule.onNodeWithText(label).performClick()
        composeRule.runOnIdle {
            assertEquals(label, suggestedQuestion)
        }
    }

    @Test
    @Config(qualifiers = "zh-rCN")
    fun emptyChatSuggestionClickDispatchesGeneralPrompt() {
        var suggestedQuestion: String? = null
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
                onSuggestedQuestion = { suggestedQuestion = it },
            )
        }

        val label = context.getString(R.string.chat_suggestion_explain_concept)
        composeRule.onNodeWithText(label).performClick()
        composeRule.runOnIdle {
            assertEquals(label, suggestedQuestion)
        }
    }

    @Test
    fun menuButtonTriggersCallback() {
        var openedDrawer = false

        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
                onOpenDrawer = { openedDrawer = true },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_MENU_BUTTON).performClick()
        composeRule.runOnIdle { assertTrue(openedDrawer) }
    }

    @Test
    fun settingsAvatarTriggersCallback() {
        var openedSettings = false

        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
                onOpenQuickSettings = { openedSettings = true },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_SETTINGS_BUTTON).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_SETTINGS_BUTTON).performClick()
        composeRule.runOnIdle { assertTrue(openedSettings) }
    }

    @Test
    fun geminiComposerUsesAttachmentAndVoiceControls() {
        val context = ApplicationProvider.getApplicationContext<Context>()

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

        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_TRIGGER_BUTTON).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_SETTINGS_BUTTON).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_MIC_BUTTON).assertExists()
        composeRule.onNodeWithText(context.chatString("chat_mode_fast")).assertDoesNotExist()

        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_TRIGGER_BUTTON).performClick()

        composeRule.onNodeWithContentDescription(context.chatString("chat_composer_voice")).assertExists()
    }

    @Test
    fun inputDraftStillRendersInsideGeminiComposer() {
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1", input = "继续整理发布说明"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_INPUT).assertTextContains("继续整理发布说明")
    }

    @Test
    fun draftInputShowsSendActionAndTriggersSendCallback() {
        composeRule.setContent {
            ChatScreen(
                uiState = ChatUiState(appId = "app-1", chatId = "chat-1", input = "你好"),
                onInputChanged = {},
                onSend = {},
                onBack = {},
                onStopGenerating = {},
                onContinueGeneration = {},
                onFeedback = { _, _ -> },
                onRegenerate = { _ -> },
            )
        }

        composeRule.onNodeWithTag(ChatTestTags.CHAT_PRIMARY_ACTION_BUTTON).assertExists().assertIsEnabled()
    }

    @Test
    fun hardwareEnterShortcutHelperOnlyTriggersForSendableEnterKeyUp() {
        assertTrue(shouldSubmitFromHardwareEnter(Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyUp, canSend = true))
        assertTrue(shouldSubmitFromHardwareEnter(Key.NumPadEnter, androidx.compose.ui.input.key.KeyEventType.KeyUp, canSend = true))
        assertFalse(shouldSubmitFromHardwareEnter(Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyDown, canSend = true))
        assertFalse(shouldSubmitFromHardwareEnter(Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyUp, canSend = false))
        assertFalse(shouldSubmitFromHardwareEnter(Key.Spacebar, androidx.compose.ui.input.key.KeyEventType.KeyUp, canSend = true))
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

        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_TRIGGER_BUTTON).performClick()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_ATTACHMENT_LIST).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.attachmentItem("content://local/picture.png")).assertExists()
        composeRule.onNodeWithTag(ChatTestTags.attachmentItem("content://local/uploading.pdf")).assertExists()
        composeRule.onNodeWithText("failed.txt").assertExists()
        composeRule.onNodeWithText(context.chatString("chat_attachment_cancel"), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(context.chatString("chat_attachment_retry"), useUnmergedTree = true).assertExists()
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
        composeRule.onNodeWithText(context.chatString("chat_message_streaming_placeholder")).assertExists()
    }

    @Test
    fun streamingAssistantWithoutVisibleAnswerShowsThinkingPlaceholder() {
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
                                    markdown = "",
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

        composeRule.onNodeWithText(context.chatString("chat_message_streaming_placeholder")).assertIsDisplayed()
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

private val sampleConversationItems =
    listOf(
        ConversationListItemUiModel(
            chatId = "chat-1",
            appId = "app-1",
            title = "架构讨论",
            preview = "最新内容",
            folderId = "folder-1",
            folderName = "工作",
            tags = listOf(ConversationTagUiModel(tagId = "tag-1", name = "Kotlin", colorToken = "sky")),
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
