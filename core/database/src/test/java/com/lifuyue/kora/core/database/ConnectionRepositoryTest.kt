package com.lifuyue.kora.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ConnectionRepositoryTest {
    @Test
    fun saveConnectionPersistsSnapshotAndAppearance() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store =
                ConnectionPreferencesStore.createForTest(
                    scope = this,
                    file = File(context.filesDir, "connection-repository.preferences_pb"),
                )
            val secureStore = ApiKeySecureStore(context, "connection-repository-secure")
            secureStore.clear()
            val repository =
                ConnectionRepository(
                    preferencesStore = store,
                    apiKeySecureStore = secureStore,
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                )

            repository.saveConnection(
                serverBaseUrl = "https://api.fastgpt.in/api",
                apiKey = "fastgpt-secret",
                selectedAppId = "app-1",
                onboardingCompleted = true,
                themeMode = ThemeMode.OLED_DARK,
                dynamicColorEnabled = false,
                oledEnabled = true,
            )

            val snapshot = repository.snapshot.first()
            val prefs = store.preferences.first()

            assertEquals("https://api.fastgpt.in/", snapshot.serverBaseUrl)
            assertEquals("fastgpt-secret", snapshot.apiKey)
            assertEquals("app-1", snapshot.selectedAppId)
            assertTrue(snapshot.onboardingCompleted)
            assertEquals(ThemeMode.OLED_DARK, snapshot.appearancePreferences.themeMode)
            assertFalse(snapshot.appearancePreferences.dynamicColorEnabled)
            assertTrue(snapshot.appearancePreferences.oledEnabled)
            assertEquals("https://api.fastgpt.in/", prefs.serverBaseUrl)
            assertTrue(prefs.apiKeyPresent)
        }

    @Test
    fun testConnectionReturnsAvailableAppsUsingPostAppList() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 200,
                          "statusText": "",
                          "message": "",
                          "data": [
                            { "_id": "app-1", "name": "Alpha" },
                            { "_id": "app-2", "name": "Beta" }
                          ]
                        }
                        """.trimIndent(),
                    ),
            )
            server.start()

            val repository =
                ConnectionRepository(
                    preferencesStore = testPreferencesStore(),
                    apiKeySecureStore = testApiKeyStore(),
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                )

            val result = repository.testConnection(server.url("/api").toString(), "fastgpt-secret")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/core/app/list", request.path)
            assertTrue(result is ConnectionTestResult.Success)
            result as ConnectionTestResult.Success
            assertEquals(listOf("Alpha", "Beta"), result.apps.map { it.name })

            server.shutdown()
        }

    @Test
    fun updateOnboardingCompletedUpdatesSnapshotAndPreferences() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store =
                ConnectionPreferencesStore.createForTest(
                    scope = this,
                    file = File(context.filesDir, "connection-onboarding.preferences_pb"),
                )
            val secureStore = ApiKeySecureStore(context, "connection-onboarding-secure")
            val repository =
                ConnectionRepository(
                    preferencesStore = store,
                    apiKeySecureStore = secureStore,
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                )

            repository.updateOnboardingCompleted(true)

            assertTrue(repository.snapshot.first().onboardingCompleted)
            assertTrue(store.preferences.first().onboardingCompleted)
        }
}

private fun testPreferencesStore(): ConnectionPreferencesStore {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return ConnectionPreferencesStore.createForTest(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
        file = File(context.filesDir, "connection-repository-${System.nanoTime()}.preferences_pb"),
    )
}

private fun testApiKeyStore(): ApiKeySecureStore {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return ApiKeySecureStore(context, "connection-repository-${System.nanoTime()}")
}
