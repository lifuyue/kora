package com.lifuyue.kora.core.database

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
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

            store.updateServerBaseUrl("https://api.fastgpt.in/api")
            store.updateApiKeyPresence(true)
            store.updateSelectedAppId("app-1")
            store.updateOnboardingCompleted(true)
            store.updateStreamEnabled(false)
            store.updateAutoScroll(false)

            val preferences = store.preferences.first()

            assertEquals("https://api.fastgpt.in/api", preferences.serverBaseUrl)
            assertTrue(preferences.apiKeyPresent)
            assertEquals("app-1", preferences.selectedAppId)
            assertTrue(preferences.onboardingCompleted)
            assertFalse(preferences.streamEnabled)
            assertFalse(preferences.autoScroll)
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
