package com.lifuyue.kora.feature.settings

import android.content.Context
import android.text.format.Formatter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        composeRule
            .onNodeWithText(context.resources.getQuantityString(R.plurals.settings_connection_success, 1, 1, 120))
            .assertIsDisplayed()
    }

    @Test
    fun settingsOverviewShowsSummaries() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            SettingsOverviewScreen(
                state =
                    SettingsOverviewUiState(
                        serverBaseUrl = "https://fastgpt.example.com",
                        themeMode = ThemeMode.OLED_DARK,
                        selectedAppId = "app-42",
                        selectedLanguageTag = "zh-CN",
                    ),
                onOpenConnection = {},
                onOpenCurrentApp = {},
                onOpenTheme = {},
                onOpenChatPreferences = {},
                onOpenLanguage = {},
                onOpenCache = {},
                onOpenAbout = {},
            )
        }

        composeRule.onNodeWithTag("settings_status_card").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_overview_section_connection)).assertExists()
        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasText(context.getString(R.string.settings_overview_section_appearance)))
        composeRule.onNodeWithText(context.getString(R.string.settings_overview_section_appearance)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.settings_connection_title)).assertIsDisplayed()
        composeRule.onNodeWithText("https://fastgpt.example.com").assertIsDisplayed()
        composeRule.onNodeWithText("app-42").assertIsDisplayed()
        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasText(context.getString(R.string.settings_theme_mode_oled_dark)))
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_mode_oled_dark)).assertIsDisplayed()
        composeRule.onNodeWithTag("settings-current-app").performClick()
        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasTestTag("settings-chat-preferences"))
        composeRule.onNodeWithText(context.getString(R.string.settings_overview_section_common)).assertIsDisplayed()
        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasTestTag("settings-language"))
        composeRule.onNodeWithText(context.getString(R.string.settings_language_simplified_chinese)).assertIsDisplayed()
        assertEquals(
            1,
            composeRule.onAllNodesWithText(context.getString(R.string.settings_chat_preferences_title)).fetchSemanticsNodes().size,
        )
        assertEquals(
            1,
            composeRule.onAllNodesWithText(context.getString(R.string.settings_language_title)).fetchSemanticsNodes().size,
        )
        assertEquals(
            1,
            composeRule.onAllNodesWithText(context.getString(R.string.settings_storage_title)).fetchSemanticsNodes().size,
        )
        assertEquals(
            1,
            composeRule.onAllNodesWithText(context.getString(R.string.settings_about_title)).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun settingsOverviewIncludesAudioEntry() {
        var openedAudio = false
        composeRule.setContent {
            SettingsOverviewScreen(
                state = SettingsOverviewUiState(),
                onOpenConnection = {},
                onOpenCurrentApp = {},
                onOpenTheme = {},
                onOpenChatPreferences = {},
                onOpenAudio = { openedAudio = true },
                onOpenLanguage = {},
                onOpenCache = {},
                onOpenAbout = {},
            )
        }

        composeRule
            .onNodeWithTag("settings-overview-scroll")
            .performScrollToNode(hasTestTag("settings-audio"))
        composeRule.onNodeWithTag("settings-audio").assertExists()
        composeRule.onNodeWithText(ApplicationProvider.getApplicationContext<Context>().appString("settings_audio_title")).performClick()
        composeRule.runOnIdle {
            assertTrue(openedAudio)
        }
    }

    @Test
    fun themeScreenDisplaysSwitchesAndSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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

        composeRule.onNodeWithText(context.getString(R.string.settings_theme_mode_oled_dark)).assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic-color").assertIsDisplayed()
    }

    @Test
    fun chatPreferencesScreenShowsPersistedTogglesAndSlider() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            ChatPreferencesScreen(
                state =
                    ChatPreferencesUiState(
                        streamEnabled = false,
                        autoScroll = true,
                        showCitationsByDefault = false,
                        fontSizeScale = 1.2f,
                    ),
                onStreamEnabledChange = {},
                onAutoScrollChange = {},
                onShowCitationsChange = {},
                onFontSizeScaleChange = {},
            )
        }

        composeRule.onNodeWithTag("chat-pref-stream").assertIsOff()
        composeRule.onNodeWithTag("chat-pref-auto-scroll").assertIsOn()
        composeRule.onNodeWithTag("chat-pref-citations").assertIsOff()
        composeRule
            .onNodeWithText(context.getString(R.string.settings_chat_preferences_font_scale, 120))
            .assertIsDisplayed()
    }

    @Test
    fun audioSettingsScreenShowsPersistedControls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            AudioSettingsScreen(
                state =
                    AudioSettingsUiState(
                        speechToTextEngine = SpeechToTextEngine.WhisperApp,
                        autoSendTranscripts = true,
                        textToSpeechEngine = TextToSpeechEngine.AppManaged,
                        speechRate = 1.5f,
                        defaultVoiceName = "Voice A",
                    ),
                onSpeechToTextEngineChange = {},
                onAutoSendTranscriptsChange = {},
                onTextToSpeechEngineChange = {},
                onSpeechRateChange = {},
                onDefaultVoiceNameChange = {},
            )
        }

        composeRule.onNodeWithText(context.appString("settings_audio_title")).assertExists()
        composeRule.onNodeWithTag("audio-stt-whisper").assertExists()
        composeRule.onNodeWithTag("audio-auto-send").assertIsOn()
        composeRule.onNodeWithTag("audio-tts-app-managed").assertExists()
        composeRule.onNodeWithTag("audio-speech-rate").assertExists()
        composeRule.onNodeWithTag("audio-default-voice").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun chatPreferencesScreenUsesLocalizedFontScaleLabel() {
        composeRule.setContent {
            ChatPreferencesScreen(
                state =
                    ChatPreferencesUiState(
                        streamEnabled = true,
                        autoScroll = true,
                        showCitationsByDefault = true,
                        fontSizeScale = 1.2f,
                    ),
                onStreamEnabledChange = {},
                onAutoScrollChange = {},
                onShowCitationsChange = {},
                onFontSizeScaleChange = {},
            )
        }

        composeRule.onNodeWithText("Text size 120%").assertIsDisplayed()
    }

    @Test
    fun languageScreenShowsSelectedOption() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            LanguageSettingsScreen(
                state =
                    LanguageSettingsUiState(
                        selectedLanguageTag = "en",
                    ),
                onLanguageTagChange = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_language_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("language-option-en").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_language_english)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun languageScreenLocalizesOptionLabelsInEnglish() {
        composeRule.setContent {
            LanguageSettingsScreen(
                state = LanguageSettingsUiState(selectedLanguageTag = null),
                onLanguageTagChange = {},
            )
        }

        composeRule.onNodeWithText("Follow system").assertIsDisplayed()
        composeRule.onNodeWithText("Simplified Chinese").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun cacheScreenShowsSizeAndAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            CacheSettingsScreen(
                state =
                    CacheSettingsUiState(
                        storageBuckets =
                            mapOf(
                                StorageBucket.DATABASE to 2_048L,
                                StorageBucket.PREFERENCES to 512L,
                                StorageBucket.TEMP_CACHE to 1_536L,
                            ),
                    ),
                onClearCache = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_storage_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_storage_bucket_database)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_storage_bucket_preferences)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_storage_bucket_temp_cache)).assertIsDisplayed()
        composeRule.onNodeWithText(Formatter.formatShortFileSize(context, 2_048L)).assertIsDisplayed()
        composeRule.onNodeWithText(Formatter.formatShortFileSize(context, 512L)).assertIsDisplayed()
        composeRule.onNodeWithText(Formatter.formatShortFileSize(context, 1_536L)).assertIsDisplayed()
        composeRule.onNodeWithTag("clear-cache").assertIsDisplayed()
    }

    @Test
    fun cacheScreen_onlyTempCacheShowsClearAction() {
        composeRule.setContent {
            CacheSettingsScreen(
                state =
                    CacheSettingsUiState(
                        storageBuckets =
                            mapOf(
                                StorageBucket.DATABASE to 256L,
                                StorageBucket.PREFERENCES to 128L,
                                StorageBucket.TEMP_CACHE to 64L,
                            ),
                    ),
                onClearCache = {},
            )
        }

        assertEquals(0, composeRule.onAllNodesWithTag("clear-cache-database").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("clear-cache-preferences").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("clear-cache").assertIsDisplayed()
    }

    @Test
    fun aboutScreenShowsVersionAndLinks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            AboutScreen(
                state =
                    AboutUiState(
                        versionName = "1.2.3",
                        feedbackUrl = "mailto:kora@example.com",
                        licensesUrl = "https://example.com/licenses",
                    ),
                onOpenFeedback = {},
                onOpenLicenses = {},
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_about_title)).assertIsDisplayed()
        composeRule.onNodeWithText("1.2.3").assertIsDisplayed()
        composeRule.onNodeWithTag("about-feedback").assertIsDisplayed()
        composeRule.onNodeWithTag("about-licenses").assertIsDisplayed()
    }
}
