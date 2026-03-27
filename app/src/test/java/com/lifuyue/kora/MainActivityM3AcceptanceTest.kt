package com.lifuyue.kora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.testing.AcceptanceAppHarnessRule
import com.lifuyue.kora.testing.AcceptanceConnectionRouteOverride
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class, sdk = [35])
class MainActivityM3AcceptanceTest {
    private val harnessRule =
        AcceptanceAppHarnessRule(
            initialSnapshot = ConnectionSnapshot(),
            connectionRouteOverride = AcceptanceConnectionRouteOverride(),
        )
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(harnessRule).around(composeRule)

    @Test
    fun firstLaunchRunsOnboardingConnectionAndArrivesAtShell() {
        composeRule.onNodeWithText("欢迎使用 Kora").assertIsDisplayed()

        completeOnboardingAndSaveConnection()

        composeRule.onNodeWithText("会话").assertIsDisplayed()
        composeRule.onNodeWithText("聊天").assertIsDisplayed()
        composeRule.onNodeWithText("知识库").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun shellSupportsBottomNavigationAfterConnection() {
        completeOnboardingAndSaveConnection()

        composeRule.onNodeWithText("知识库").performClick()
        composeRule.onNodeWithText("知识库将在 M5 接入。").assertIsDisplayed()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("连接配置").assertIsDisplayed()

        composeRule.onNodeWithText("聊天").performClick()
        composeRule.onNodeWithText("暂无会话，点击右下角开始新对话。").assertIsDisplayed()
    }

    private fun completeOnboardingAndSaveConnection() {
        composeRule.onNodeWithText("下一步").performClick()
        composeRule.onNodeWithText("下一步").performClick()
        composeRule.onNodeWithText("进入连接配置").performClick()
        composeRule.onNodeWithText("测试连接").performClick()
        waitForText("测试连接成功")
        composeRule.onNodeWithText("保存").performClick()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
