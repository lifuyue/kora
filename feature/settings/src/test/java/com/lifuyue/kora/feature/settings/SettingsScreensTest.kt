package com.lifuyue.kora.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ThemeMode
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
    fun settingsOverviewHidesRemovedEntries() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            SettingsOverviewScreen(
                state =
                    SettingsOverviewUiState(
                        connectionType = ConnectionType.OPENAI_COMPATIBLE,
                        serverBaseUrl = "https://api.openai.com/v1",
                        model = "gpt-4.1",
                        themeMode = ThemeMode.DARK,
                        selectedLanguageTag = "zh-CN",
                    ),
                onOpenConnection = {},
                onOpenCurrentApp = {},
                onOpenTheme = {},
                onOpenLanguage = {},
                onOpenCache = {},
                onOpenAbout = {},
                onOpenChatPreferences = {},
            )
        }

        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasTestTag("settings-chat-preferences"))
        composeRule.onNodeWithTag("settings-theme").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-chat-preferences").assertIsDisplayed()
        composeRule.onAllNodesWithText("音频与语音").assertCountEquals(0)
    }

    @Test
    fun themeAppearanceScreenShowsOnlyLightAndDark() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            ThemeAppearanceScreen(
                state = ThemeAppearanceUiState(themeMode = ThemeMode.DARK),
                onThemeModeChange = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_theme_mode_light)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_mode_dark)).assertIsDisplayed()
        composeRule.onAllNodesWithText("跟随系统").assertCountEquals(0)
        composeRule.onAllNodesWithText("OLED 深色").assertCountEquals(0)
        composeRule.onAllNodesWithText("动态取色").assertCountEquals(0)
    }

    @Test
    fun chatSettingsSheetScrollsToBottomEntriesOnSmallHeight() {
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ChatSettingsSheetContent(
                    state =
                        SettingsOverviewUiState(
                            serverBaseUrl = "https://fastgpt.example.com",
                            themeMode = ThemeMode.DARK,
                            selectedLanguageTag = "zh-CN",
                        ),
                    onOpenConnection = {},
                    onOpenCurrentApp = {},
                    onOpenTheme = {},
                    onOpenLanguage = {},
                    onOpenCache = {},
                    onOpenAbout = {},
                    onOpenChatPreferences = {},
                    modifier = Modifier.padding(top = 320.dp),
                )
            }
        }

        composeRule.onNodeWithText("local@kora.app").assertIsDisplayed()
        composeRule
            .onNodeWithTag("chat_settings_sheet")
            .performScrollToNode(hasTestTag("chat-settings-about"))
        composeRule.onNodeWithTag("chat-settings-about").assertIsDisplayed()
    }

    @Test
    fun chatPreferencesScreenShowsBothToggles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            ChatPreferencesScreen(
                state = ChatPreferencesUiState(showReasoningEntry = true, streamResponses = true),
                onShowReasoningEntryChange = {},
                onStreamResponsesChange = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_chat_preferences_reasoning_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_chat_preferences_stream_title)).assertIsDisplayed()
    }
}
