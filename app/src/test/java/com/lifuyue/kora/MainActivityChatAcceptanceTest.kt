package com.lifuyue.kora

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.feature.chat.ChatTestTags
import com.lifuyue.kora.testing.AcceptanceAppHarnessRule
import com.lifuyue.kora.testing.AcceptanceChatRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
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

    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun chatLoopStreamsReopensAndSupportsActions() {
        val context = composeRule.activity
        val emptyTitle = context.getString(com.lifuyue.kora.feature.chat.R.string.conversation_list_empty_title)
        val sendLabel = context.getString(com.lifuyue.kora.feature.chat.R.string.chat_send)
        val backLabel = context.getString(com.lifuyue.kora.feature.chat.R.string.chat_back)

        composeRule.onNodeWithText(context.getString(R.string.nav_chat)).assertIsDisplayed()
        waitUntil { repository.hasRefreshed() }
        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()

        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).performClick()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_INPUT).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_INPUT).performTextInput("测试 M4 主流程")
        waitUntil {
            runCatching {
                composeRule.onNodeWithText(sendLabel).assertIsEnabled()
            }.isSuccess
        }
        composeRule.onNodeWithText(sendLabel).performClick()

        waitUntil { repository.hasSentMessage() }
        val chatId = waitForConversationId("app-1")
        val assistant = waitForCompletedAssistant(chatId)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(ChatTestTags.messageCopyAction(checkNotNull(assistant.lastAssistantId)))
            .performClick()

        composeRule.onNodeWithText(backLabel).performClick()
        waitUntil {
            runCatching {
                composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ITEM_PREFIX + chatId).assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_ITEM_PREFIX + chatId).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_INPUT).assertIsDisplayed()
    }

    @Test
    fun streamingErrorIsVisibleAndAppDoesNotCrash() {
        val context = composeRule.activity
        val sendLabel = context.getString(com.lifuyue.kora.feature.chat.R.string.chat_send)
        val backLabel = context.getString(com.lifuyue.kora.feature.chat.R.string.chat_back)

        composeRule.onNodeWithText(context.getString(R.string.nav_chat)).assertIsDisplayed()
        waitUntil { repository.hasRefreshed() }

        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).performClick()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_INPUT).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CHAT_INPUT).performTextInput("触发错误")
        waitUntil {
            runCatching {
                composeRule.onNodeWithText(sendLabel).assertIsEnabled()
            }.isSuccess
        }
        composeRule.onNodeWithText(sendLabel).performClick()

        waitUntil { repository.hasSentMessage() }
        val chatId = waitForConversationId("app-1")
        val failedAssistant = waitForFailedAssistant(chatId)
        composeRule.waitForIdle()
        assertTrue(failedAssistant.lastErrorMessage?.contains("模拟网络错误") == true)
        composeRule.onNodeWithText(backLabel).performClick()
        waitUntilText("模拟网络错误")
        composeRule.onNodeWithText("模拟网络错误").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.CONVERSATION_FAB).assertIsDisplayed()
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
