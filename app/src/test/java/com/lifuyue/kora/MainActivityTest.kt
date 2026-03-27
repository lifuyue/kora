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
            "这是 M1 阶段的 Compose + Hilt 应用入口壳，后续会接入设置、聊天与知识库模块。",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("继续").assertIsDisplayed()
    }
}
