package com.lifuyue.kora.feature.chat

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
}
