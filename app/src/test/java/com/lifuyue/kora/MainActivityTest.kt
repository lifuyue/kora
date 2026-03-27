package com.lifuyue.kora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsOnboardingScreen() {
        composeRule.onNodeWithText("欢迎使用 Kora").assertIsDisplayed()
        composeRule.onNodeWithText(
            "在手机上配置 FastGPT 连接，进入流式聊天与会话管理。",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("下一步").assertIsDisplayed()
    }
}
