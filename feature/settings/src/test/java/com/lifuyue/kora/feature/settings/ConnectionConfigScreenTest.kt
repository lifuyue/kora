package com.lifuyue.kora.feature.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionType
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ConnectionConfigScreen(
                state =
                    ConnectionConfigUiState(
                        connectionType = ConnectionType.FAST_GPT,
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
                onConnectionTypeChange = {},
                onBaseUrlChange = {},
                onApiKeyChange = {},
                onModelChange = {},
                onTestConnection = {},
                onSave = {},
                onClear = {},
            )
        }

        composeRule.onAllNodesWithTag("connection-result").assertCountEquals(1)
        composeRule.onNodeWithText(context.getString(R.string.settings_connection_save)).assertIsEnabled()
    }
}
