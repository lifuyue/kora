package com.lifuyue.kora.core.database

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.database.store.ConnectionPreferences
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ConnectionPreferencesStoreTest {
    @Test
    fun storePersistsNonSensitivePreferences() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store =
                ConnectionPreferencesStore.createForTest(
                    scope = this,
                    file = File(context.filesDir, "connection-preferences.preferences_pb"),
                )

            store.updateConnectionType(ConnectionType.OPENAI_COMPATIBLE)
            store.updateServerBaseUrl("https://api.fastgpt.in/api")
            store.updateApiKeyPresence(true)
            store.updateModel("gpt-4o-mini")
            store.updateSelectedAppId("app-1")
            store.updateOnboardingCompleted(true)
            store.updateStreamEnabled(false)
            store.updateAutoScroll(false)
            store.updateThemeMode(ThemeMode.DARK)
            store.updateDynamicColorEnabled(false)
            store.updateOledEnabled(true)
            store.updateLanguageInitialized(true)
            store.updateLanguageTag("zh-CN")
            store.updateSpeechToTextEngine(SpeechToTextEngine.WhisperApp)
            store.updateAutoSendTranscripts(true)
            store.updateTextToSpeechEngine(TextToSpeechEngine.AppManaged)
            store.updateSpeechRate(1.2f)
            store.updateDefaultVoiceName("alloy")

            val preferences = store.preferences.first()

            assertEquals(ConnectionType.OPENAI_COMPATIBLE, preferences.connectionType)
            assertEquals("https://api.fastgpt.in/api", preferences.serverBaseUrl)
            assertTrue(preferences.apiKeyPresent)
            assertEquals("gpt-4o-mini", preferences.model)
            assertEquals("app-1", preferences.selectedAppId)
            assertTrue(preferences.onboardingCompleted)
            assertFalse(preferences.streamEnabled)
            assertFalse(preferences.autoScroll)
            assertEquals(ThemeMode.DARK, preferences.themeMode)
            assertFalse(preferences.dynamicColorEnabled)
            assertTrue(preferences.oledEnabled)
            assertTrue(preferences.languageInitialized)
            assertEquals("zh-CN", preferences.languageTag)
            assertEquals(SpeechToTextEngine.WhisperApp, preferences.speechToTextEngine)
            assertTrue(preferences.autoSendTranscripts)
            assertEquals(TextToSpeechEngine.AppManaged, preferences.textToSpeechEngine)
            assertEquals(1.2f, preferences.speechRate)
            assertEquals("alloy", preferences.defaultVoiceName)
        }

    @Test
    fun defaultsAreSafeAndDoNotStoreRawApiKey() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val backingFile = File(context.filesDir, "connection-defaults.preferences_pb")
            val store = ConnectionPreferencesStore.createForTest(scope = this, file = backingFile)

            val preferences = store.preferences.first()

            assertEquals(ConnectionPreferences(), preferences)
            assertFalse(store.dataStore.data.first().asMap().containsKey(stringPreferencesKey("api_key")))
            assertFalse(store.dataStore.data.first().asMap().containsKey(stringPreferencesKey("server_api_key")))
            assertEquals(emptyPreferences(), emptyPreferences())
            assertNull(preferences.selectedAppId)
        }
}
