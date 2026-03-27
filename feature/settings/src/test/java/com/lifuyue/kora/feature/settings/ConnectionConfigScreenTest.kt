package com.lifuyue.kora.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ConnectionConfigScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveEnabledStateAndStatusAreRendered() {
        composeRule.setContent {
            ConnectionConfigScreen(
                state =
                    ConnectionConfigUiState(
                        serverUrl = "https://api.fastgpt.in",
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

        composeRule.onNodeWithText("连接成功，发现 1 个 App，耗时 120ms").assertIsDisplayed()
        composeRule.onNodeWithText("保存").assertIsEnabled()
    }
}
