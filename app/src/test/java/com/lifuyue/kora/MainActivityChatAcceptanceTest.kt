package com.lifuyue.kora

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.feature.chat.ChatTestTags
import com.lifuyue.kora.testing.AcceptanceAppHarnessRule
import com.lifuyue.kora.testing.AcceptanceChatRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class, sdk = [35])
class MainActivityChatAcceptanceTest {
    private val repository = AcceptanceChatRepository()
    private val harnessRule =
        AcceptanceAppHarnessRule(
            initialSnapshot =
                ConnectionSnapshot(
                    serverBaseUrl = "https://api.fastgpt.in/",
                    apiKey = "fastgpt-secret",
                    selectedAppId = "app-1",
                    onboardingCompleted = true,
                ),
            chatRepositoryOverride = repository,
        )
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(harnessRule).around(composeRule)

    @Test
    fun chatLoopStreamsReopensAndSupportsActions() {
        composeRule.onNodeWithText("会话").assertIsDisplayed()
        composeRule.onNodeWithText("暂无会话").assertIsDisplayed()
        waitUntil { repository.hasRefreshed() }

        composeRule.onNodeWithTag(ChatTestTags.conversationFab).performClick()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).performTextInput("测试 M4 主流程")
        composeRule.onNodeWithText("发送").assertIsEnabled()
        composeRule.onNodeWithText("发送").performClick()

        waitUntil { repository.hasSentMessage() }
        val chatId = waitForConversationId("app-1")
        val assistant = waitForCompletedAssistant(chatId)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("复制").performClick()

        val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
        val copiedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        assertTrue(copiedText.isNotBlank())
        assertTrue(copiedText.contains("测试 M4 主流程"))

        composeRule.onNodeWithText("返回").performClick()
        waitUntilText("测试 M4 主流程")
        composeRule.onNodeWithText("测试 M4 主流程").assertIsDisplayed()
        composeRule.onNodeWithText("测试 M4 主流程").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).assertIsDisplayed()
    }

    @Test
    fun streamingErrorIsVisibleAndAppDoesNotCrash() {
        composeRule.onNodeWithText("会话").assertIsDisplayed()
        waitUntil { repository.hasRefreshed() }

        composeRule.onNodeWithTag(ChatTestTags.conversationFab).performClick()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).performTextInput("触发错误")
        composeRule.onNodeWithText("发送").assertIsEnabled()
        composeRule.onNodeWithText("发送").performClick()

        waitUntil { repository.hasSentMessage() }
        val chatId = waitForConversationId("app-1")
        val failedAssistant = waitForFailedAssistant(chatId)
        composeRule.waitForIdle()
        assertEquals("模拟网络错误", failedAssistant.lastErrorMessage)
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("模拟网络错误").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.conversationFab).assertIsDisplayed()
    }

    private fun waitForConversationId(appId: String): String {
        waitUntil { repository.latestConversationId(appId) != null }
        return checkNotNull(repository.latestConversationId(appId))
    }

    private fun waitForCompletedAssistant(chatId: String): AcceptanceChatRepository.AcceptanceProbe {
        waitUntil {
            repository.probe(chatId)?.let { probe ->
                probe.assistantCompleted && !probe.lastAssistantId.isNullOrBlank()
            } == true
        }
        return checkNotNull(repository.probe(chatId))
    }

    private fun waitForFailedAssistant(chatId: String): AcceptanceChatRepository.AcceptanceProbe {
        waitUntil {
            repository.probe(chatId)?.let { probe ->
                probe.errorVisible && !probe.lastAssistantId.isNullOrBlank()
            } == true
        }
        return checkNotNull(repository.probe(chatId))
    }

    private fun waitUntilText(text: String) {
        waitUntil {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000, condition = condition)
    }
}
