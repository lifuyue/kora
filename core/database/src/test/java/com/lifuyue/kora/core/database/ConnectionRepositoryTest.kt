package com.lifuyue.kora.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkJson
import com.lifuyue.kora.core.network.OpenAiCompatibleApiFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ConnectionRepositoryTest {
    @Test
    fun freshRepositoryDefaultsToChineseLocale() =
        runBlocking {
            val repository =
                ConnectionRepository(
                    preferencesStore = testPreferencesStore(),
                    apiKeySecureStore = testApiKeyStore(),
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            assertEquals("zh-CN", repository.snapshot.first().appearancePreferences.languageTag)
        }

    @Test
    fun explicitFollowSystemSelectionIsPreservedAfterInitialization() =
        runBlocking {
            val store = testPreferencesStore()
            val repository =
                ConnectionRepository(
                    preferencesStore = store,
                    apiKeySecureStore = testApiKeyStore(),
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            repository.updateLanguageTag(null)

            assertNull(repository.snapshot.first().appearancePreferences.languageTag)
            assertTrue(store.preferences.first().languageInitialized)
            assertNull(store.preferences.first().languageTag)
        }

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
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            repository.saveConnection(
                connectionType = ConnectionType.FAST_GPT,
                serverBaseUrl = "https://api.fastgpt.in/api",
                apiKey = "fastgpt-secret",
                model = null,
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
            assertEquals("zh-CN", snapshot.appearancePreferences.languageTag)
            assertEquals("https://api.fastgpt.in/", prefs.serverBaseUrl)
            assertTrue(prefs.apiKeyPresent)
            assertTrue(prefs.languageInitialized)
        }

    @Test
    fun saveConnectionStripsTrailingV1ForOpenAiCompatibleBaseUrl() =
        runBlocking {
            val repository =
                ConnectionRepository(
                    preferencesStore = testPreferencesStore(),
                    apiKeySecureStore = testApiKeyStore(),
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            repository.saveConnection(
                connectionType = ConnectionType.OPENAI_COMPATIBLE,
                serverBaseUrl = "https://api.siliconflow.cn/v1",
                apiKey = "sk-test",
                model = "Qwen/Qwen3.5-4B",
                onboardingCompleted = true,
            )

            val snapshot = repository.snapshot.first()
            assertEquals("https://api.siliconflow.cn/", snapshot.serverBaseUrl)
            assertEquals("Qwen/Qwen3.5-4B", snapshot.model)
            assertEquals(ConnectionType.OPENAI_COMPATIBLE, snapshot.connectionType)
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
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            val result =
                repository.testConnection(
                    connectionType = ConnectionType.FAST_GPT,
                    serverBaseUrl = server.url("/api").toString(),
                    apiKey = "fastgpt-secret",
                )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/core/app/list", request.path)
            assertTrue(result is ConnectionTestResult.Success)
            result as ConnectionTestResult.Success
            assertEquals(listOf("Alpha", "Beta"), result.apps.map { it.name })

            server.shutdown()
        }

    @Test
    fun testConnectionUsesSingleV1PrefixForOpenAiCompatibleEndpoints() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "data": [
                            { "id": "Qwen/Qwen3.5-4B", "owned_by": "siliconflow" }
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
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            val result =
                repository.testConnection(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = server.url("/v1").toString(),
                    apiKey = "sk-test",
                    model = "Qwen/Qwen3.5-4B",
                )

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/v1/models", request.path)
            assertTrue(result is ConnectionTestResult.Success)

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
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            repository.updateOnboardingCompleted(true)

            assertTrue(repository.snapshot.first().onboardingCompleted)
            assertTrue(store.preferences.first().onboardingCompleted)
        }

    @Test
    fun updateAudioPreferencesPersistsSnapshotAndPreferences() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store =
                ConnectionPreferencesStore.createForTest(
                    scope = this,
                    file = File(context.filesDir, "connection-audio.preferences_pb"),
                )
            val secureStore = ApiKeySecureStore(context, "connection-audio-secure")
            val repository =
                ConnectionRepository(
                    preferencesStore = store,
                    apiKeySecureStore = secureStore,
                    connectionProvider = MutableConnectionProvider(),
                    apiFactory = FastGptApiFactory(NetworkJson.default),
                    openAiApiFactory = OpenAiCompatibleApiFactory(NetworkJson.default),
                )

            repository.updateAudioPreferences(
                speechToTextEngine = SpeechToTextEngine.WhisperApp,
                autoSendTranscripts = true,
                textToSpeechEngine = TextToSpeechEngine.AppManaged,
                speechRate = 1.15f,
                defaultVoiceName = "verse",
            )

            val snapshot = repository.snapshot.first()
            val preferences = store.preferences.first()

            assertEquals(SpeechToTextEngine.WhisperApp, snapshot.audioPreferences.speechToTextEngine)
            assertTrue(snapshot.audioPreferences.autoSendTranscripts)
            assertEquals(TextToSpeechEngine.AppManaged, snapshot.audioPreferences.textToSpeechEngine)
            assertEquals(1.15f, snapshot.audioPreferences.speechRate)
            assertEquals("verse", snapshot.audioPreferences.defaultVoiceName)
            assertEquals(SpeechToTextEngine.WhisperApp, preferences.speechToTextEngine)
            assertTrue(preferences.autoSendTranscripts)
            assertEquals(TextToSpeechEngine.AppManaged, preferences.textToSpeechEngine)
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
