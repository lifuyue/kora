package com.lifuyue.kora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.testing.MockWebServerRule
import com.lifuyue.kora.feature.chat.ChatTestTags
import com.lifuyue.kora.testing.AcceptanceAppHarnessRule
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class, sdk = [35])
class MainActivityM3AcceptanceTest {
    private val serverRule = MockWebServerRule()
    private val harnessRule = AcceptanceAppHarnessRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(serverRule).around(harnessRule).around(composeRule)

    @Test
    fun firstLaunchRunsOnboardingRealConnectionAndArrivesAtShell() {
        enqueueConnectionSuccess()

        composeRule.onNodeWithText("欢迎使用 Kora").assertIsDisplayed()

        completeOnboardingAndSaveConnection()

        val request = serverRule.takeRequest()
        assertConnectionTestRequest(request)

        composeRule.onNodeWithText("会话").assertIsDisplayed()
        composeRule.onNodeWithText("聊天").assertIsDisplayed()
        composeRule.onNodeWithText("知识库").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun shellSupportsBottomNavigationAfterRealConnectionSave() {
        enqueueConnectionSuccess()

        completeOnboardingAndSaveConnection()

        composeRule.onNodeWithText("知识库").performClick()
        composeRule.onNodeWithText("知识库将在 M5 接入。").assertIsDisplayed()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("连接配置").assertIsDisplayed()
        composeRule.onNodeWithText("连接配置").performClick()
        waitForText("当前密钥摘要")

        composeRule.onNodeWithText("聊天").performClick()
        composeRule.onNodeWithText("暂无会话").assertIsDisplayed()
        composeRule.onNodeWithText("点击右下角的新建会话开始第一轮对话，历史记录会显示在这里。").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.conversationFab).assertIsDisplayed()
    }

    private fun completeOnboardingAndSaveConnection() {
        composeRule.onNodeWithText("下一步").performClick()
        composeRule.onNodeWithText("下一步").performClick()
        composeRule.onNodeWithText("进入连接配置").performClick()

        composeRule.onNodeWithTag("server-url").performTextClearance()
        composeRule.onNodeWithTag("server-url").performTextInput(serverRule.url("/api/").toString())
        composeRule.onNodeWithTag("api-key").performTextInput("fastgpt-secret")
        composeRule.onNodeWithTag("test-connection").performClick()

        waitForText("连接成功，发现 2 个 App")

        composeRule.onNodeWithTag("save-connection").performClick()
        waitForText("会话")
    }

    private fun enqueueConnectionSuccess() {
        serverRule.enqueueJson(
            """
            {
              "code": 200,
              "statusText": "success",
              "message": "",
              "data": [
                { "_id": "app-1", "name": "Alpha" },
                { "_id": "app-2", "name": "Beta" }
              ]
            }
            """.trimIndent(),
        )
    }

    private fun assertConnectionTestRequest(request: RecordedRequest) {
        assertEquals("POST", request.method)
        assertEquals("/api/core/app/list", request.path)
        assertEquals("Bearer fastgpt-secret", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("{}"))
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
