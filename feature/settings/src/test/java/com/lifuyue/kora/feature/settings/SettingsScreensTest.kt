package com.lifuyue.kora.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectionScreenShowsSuccessResult() {
        composeRule.setContent {
            ConnectionConfigScreen(
                state =
                    ConnectionConfigUiState(
                        serverUrl = "https://api.fastgpt.in/api",
                        apiKey = "fastgpt-secret",
                        canSave = true,
                        testResult =
                            ConnectionTestResult.Success(
                                normalizedBaseUrl = "https://api.fastgpt.in/",
                                apps = listOf(ConnectionTestApp(id = "app-1", name = "Kora")),
                                latencyMs = 120,
                            ),
                    ),
                onBaseUrlChange = {},
                onApiKeyChange = {},
                onTestConnection = {},
                onSave = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("server-url").assertIsDisplayed()
        composeRule.onNodeWithTag("api-key").assertIsDisplayed()
        composeRule.onNodeWithTag("connection-result").assertIsDisplayed()
        composeRule.onNodeWithText("连接成功，发现 1 个 App，耗时 120ms").assertIsDisplayed()
    }

    @Test
    fun settingsOverviewShowsSummaries() {
        composeRule.setContent {
            SettingsOverviewScreen(
                state =
                    SettingsOverviewUiState(
                        connectionSummary = "已连接到 FastGPT",
                        themeSummary = "OLED 深色",
                        selectedAppSummary = "Kora App",
                    ),
                onOpenConnection = {},
                onOpenTheme = {},
            )
        }

        composeRule.onNodeWithText("连接配置").assertIsDisplayed()
        composeRule.onNodeWithText("已连接到 FastGPT").assertIsDisplayed()
        composeRule.onNodeWithText("OLED 深色").assertIsDisplayed()
        composeRule.onNodeWithText("Kora App").assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodesWithText("聊天偏好").fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodesWithText("语言").fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodesWithText("缓存").fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodesWithText("关于").fetchSemanticsNodes().size)
    }

    @Test
    fun themeScreenDisplaysSwitchesAndSelection() {
        composeRule.setContent {
            ThemeAppearanceScreen(
                state =
                    ThemeAppearanceUiState(
                        themeMode = ThemeMode.OLED_DARK,
                        dynamicColorEnabled = false,
                        oledEnabled = true,
                    ),
                onThemeModeChange = {},
                onDynamicColorChange = {},
                onOledChange = {},
            )
        }

        composeRule.onNodeWithText("OLED 深色").assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic-color").assertIsDisplayed()
    }
}
