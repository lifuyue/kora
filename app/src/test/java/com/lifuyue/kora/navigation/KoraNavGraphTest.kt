package com.lifuyue.kora.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KoraNavGraphTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingIsShownWhenNotCompleted() {
        composeRule.setContent {
            KoraNavGraph(snapshot = ConnectionSnapshot())
        }

        composeRule.onNodeWithText("欢迎使用 Kora").assertIsDisplayed()
        composeRule.onNodeWithText("1/3").assertIsDisplayed()
    }

    @Test
    fun finishingOnboardingTriggersCompletionCallback() {
        var completed = false

        composeRule.setContent {
            KoraNavGraph(
                snapshot = ConnectionSnapshot(),
                onOnboardingCompleted = { completed = true },
                connectionRoute = {
                    Text("Fake Connection")
                },
            )
        }

        composeRule.onNodeWithText("下一步").performClick()
        composeRule.onNodeWithText("下一步").performClick()
        composeRule.onNodeWithText("进入连接配置").performClick()

        composeRule.onNodeWithText("Fake Connection").assertIsDisplayed()
        assertTrue(completed)
    }

    @Test
    fun connectionScreenIsShownWhenOnboardingCompleteButConnectionMissing() {
        composeRule.setContent {
            KoraNavGraph(
                snapshot =
                    ConnectionSnapshot(
                        onboardingCompleted = true,
                    ),
                connectionRoute = {
                    Text("Fake Connection")
                },
            )
        }

        composeRule.onNodeWithText("Fake Connection").assertIsDisplayed()
    }

    @Test
    fun shellIsShownWhenConnectionReady() {
        composeRule.setContent {
            KoraNavGraph(
                snapshot =
                    ConnectionSnapshot(
                        serverBaseUrl = "https://api.fastgpt.in/",
                        apiKey = "fastgpt-secret",
                        selectedAppId = "app-1",
                        onboardingCompleted = true,
                        appearancePreferences =
                            AppearancePreferences(
                                themeMode = ThemeMode.DARK,
                            ),
                    ),
                shellRoute = { shellSnapshot ->
                    Text("Fake Shell ${shellSnapshot.selectedAppId}")
                },
            )
        }

        composeRule.onNodeWithText("Fake Shell app-1").assertIsDisplayed()
    }
}
