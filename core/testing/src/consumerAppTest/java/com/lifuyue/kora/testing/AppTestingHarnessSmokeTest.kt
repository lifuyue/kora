package com.lifuyue.kora.testing

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.core.testing.DataStoreTestFactory
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AppTestingHarnessSmokeTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun appModuleCanConsumeSharedTestingHarness() =
        runTest {
            val store = DataStoreTestFactory.createPreferencesDataStore("app-consumer-smoke")
            val key = stringPreferencesKey("app-key")

            store.edit { preferences ->
                preferences[key] = "ready"
            }

            assertEquals("ready", store.data.first()[key])
        }
}
