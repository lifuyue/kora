package com.lifuyue.kora

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.feature.chat.ChatTestTags
import com.lifuyue.kora.testing.AcceptanceAppHarnessRule
import com.lifuyue.kora.testing.AcceptanceChatRouteOverride
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class, sdk = [35])
class MainActivityChatAcceptanceTest {
    private val harnessRule =
        AcceptanceAppHarnessRule(
            initialSnapshot =
                ConnectionSnapshot(
                    serverBaseUrl = "https://api.fastgpt.in/",
                    apiKey = "fastgpt-secret",
                    selectedAppId = "app-1",
                    onboardingCompleted = true,
                ),
            shellRouteOverride = AcceptanceChatRouteOverride(),
        )
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(harnessRule).around(composeRule)

    @Test
    fun chatLoopStreamsReopensAndSupportsActions() {
        composeRule.onNodeWithText("新建").performClick()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).performTextInput("测试 M4 主流程")
        composeRule.onNodeWithText("发送").performClick()

        waitForText("生成中")
        waitForText("重新生成")

        composeRule.onNodeWithText("测试 M4 主流程").assertIsDisplayed()
        composeRule.onNodeWithText("点赞").performClick()
        composeRule.onNodeWithText("取消赞").assertIsDisplayed()
        composeRule.onNodeWithText("复制").performClick()

        val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
        assertEquals(
            "已收到：测试 M4 主流程\n\n```kotlin\nprintln(\"测试 M4 主流程\")\n```",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString(),
        )

        composeRule.onNodeWithText("重新生成").performClick()
        waitForText("第 2 次生成")

        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("测试 M4 主流程").assertIsDisplayed()
        composeRule.onNodeWithText("测试 M4 主流程").performClick()
        composeRule.onNodeWithText("取消赞").assertIsDisplayed()
        composeRule.onNodeWithText("第 2 次生成").assertIsDisplayed()
        composeRule.onNodeWithText("复制代码").assertIsDisplayed()
    }

    @Test
    fun streamingErrorIsVisibleAndAppDoesNotCrash() {
        composeRule.onNodeWithText("新建").performClick()
        composeRule.onNodeWithTag(ChatTestTags.chatInput).performTextInput("触发错误")
        composeRule.onNodeWithText("发送").performClick()

        waitForText("生成中")
        waitForText("模拟网络错误")

        composeRule.onNodeWithText("模拟网络错误").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("触发错误").assertIsDisplayed()
        composeRule.onNodeWithText("新建").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
