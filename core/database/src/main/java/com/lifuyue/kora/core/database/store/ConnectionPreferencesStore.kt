package com.lifuyue.kora.core.database.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

class ConnectionPreferencesStore internal constructor(
    val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<ConnectionPreferences> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                ConnectionPreferences(
                    serverBaseUrl = preferences[Keys.SERVER_BASE_URL],
                    apiKeyPresent = preferences[Keys.SERVER_API_KEY_PRESENT] ?: false,
                    selectedAppId = preferences[Keys.SELECTED_APP_ID],
                    onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
                    streamEnabled = preferences[Keys.STREAM_ENABLED] ?: true,
                    autoScroll = preferences[Keys.AUTO_SCROLL] ?: true,
                    fontSizeScale = preferences[Keys.FONT_SIZE_SCALE] ?: 1f,
                    showCitationsByDefault = preferences[Keys.SHOW_CITATIONS_BY_DEFAULT] ?: true,
                    themeMode =
                        preferences[Keys.THEME_MODE]
                            ?.let { storedMode -> ThemeMode.entries.firstOrNull { it.name == storedMode } }
                            ?: ThemeMode.SYSTEM,
                    dynamicColorEnabled = preferences[Keys.DYNAMIC_COLOR_ENABLED] ?: true,
                    oledEnabled = preferences[Keys.OLED_ENABLED] ?: false,
                    languageTag = preferences[Keys.LANGUAGE_TAG],
                    speechToTextEngine =
                        preferences[Keys.SPEECH_TO_TEXT_ENGINE]
                            ?.let { storedEngine -> SpeechToTextEngine.entries.firstOrNull { it.name == storedEngine } }
                            ?: SpeechToTextEngine.System,
                    autoSendTranscripts = preferences[Keys.AUTO_SEND_TRANSCRIPTS] ?: false,
                    textToSpeechEngine =
                        preferences[Keys.TEXT_TO_SPEECH_ENGINE]
                            ?.let { storedEngine -> TextToSpeechEngine.entries.firstOrNull { it.name == storedEngine } }
                            ?: TextToSpeechEngine.System,
                    speechRate = preferences[Keys.SPEECH_RATE] ?: 1f,
                    defaultVoiceName = preferences[Keys.DEFAULT_VOICE_NAME],
                )
            }

    suspend fun updateServerBaseUrl(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.SERVER_BASE_URL)
            } else {
                preferences[Keys.SERVER_BASE_URL] = value
            }
        }
    }

    suspend fun updateApiKeyPresence(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SERVER_API_KEY_PRESENT] = value
        }
    }

    suspend fun updateSelectedAppId(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.SELECTED_APP_ID)
            } else {
                preferences[Keys.SELECTED_APP_ID] = value
            }
        }
    }

    suspend fun updateOnboardingCompleted(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = value
        }
    }

    suspend fun updateStreamEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STREAM_ENABLED] = value
        }
    }

    suspend fun updateAutoScroll(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_SCROLL] = value
        }
    }

    suspend fun updateFontSizeScale(value: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.FONT_SIZE_SCALE] = value
        }
    }

    suspend fun updateShowCitationsByDefault(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_CITATIONS_BY_DEFAULT] = value
        }
    }

    suspend fun updateThemeMode(value: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = value.name
        }
    }

    suspend fun updateDynamicColorEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DYNAMIC_COLOR_ENABLED] = value
        }
    }

    suspend fun updateOledEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.OLED_ENABLED] = value
        }
    }

    suspend fun updateLanguageTag(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.LANGUAGE_TAG)
            } else {
                preferences[Keys.LANGUAGE_TAG] = value
            }
        }
    }

    suspend fun updateSpeechToTextEngine(value: SpeechToTextEngine) {
        dataStore.edit { preferences ->
            preferences[Keys.SPEECH_TO_TEXT_ENGINE] = value.name
        }
    }

    suspend fun updateAutoSendTranscripts(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_SEND_TRANSCRIPTS] = value
        }
    }

    suspend fun updateTextToSpeechEngine(value: TextToSpeechEngine) {
        dataStore.edit { preferences ->
            preferences[Keys.TEXT_TO_SPEECH_ENGINE] = value.name
        }
    }

    suspend fun updateSpeechRate(value: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.SPEECH_RATE] = value
        }
    }

    suspend fun updateDefaultVoiceName(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.DEFAULT_VOICE_NAME)
            } else {
                preferences[Keys.DEFAULT_VOICE_NAME] = value
            }
        }
    }

    companion object {
        fun create(
            scope: CoroutineScope,
            produceFile: () -> File,
        ): ConnectionPreferencesStore =
            ConnectionPreferencesStore(
                dataStore =
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                        produceFile = {
                            produceFile().also { file ->
                                file.parentFile?.mkdirs()
                            }
                        },
                    ),
            )

        fun createForTest(
            scope: CoroutineScope,
            file: File,
        ): ConnectionPreferencesStore = create(scope = scope) { file }
    }

    object Keys {
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val SERVER_API_KEY_PRESENT = booleanPreferencesKey("server_api_key_present")
        val SELECTED_APP_ID = stringPreferencesKey("selected_app_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val SHOW_CITATIONS_BY_DEFAULT = booleanPreferencesKey("show_citations_by_default")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val OLED_ENABLED = booleanPreferencesKey("oled_enabled")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val SPEECH_TO_TEXT_ENGINE = stringPreferencesKey("speech_to_text_engine")
        val AUTO_SEND_TRANSCRIPTS = booleanPreferencesKey("auto_send_transcripts")
        val TEXT_TO_SPEECH_ENGINE = stringPreferencesKey("text_to_speech_engine")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val DEFAULT_VOICE_NAME = stringPreferencesKey("default_voice_name")
    }
}
