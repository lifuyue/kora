package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.AudioPreferences
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun connectionConfigViewModel_enablesSaveOnlyAfterSuccessfulTest() =
        runTest {
            val facade = FakeSettingsConnectionFacade()
            val viewModel = ConnectionConfigViewModel(facade)

            viewModel.onBaseUrlChanged("https://fastgpt.example.com")
            viewModel.onApiKeyChanged("fastgpt-secret")
            viewModel.testConnection()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.testResult is ConnectionTestResult.Success)
            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun connectionConfigViewModel_savePersistsFirstAppAndMarksOnboardingCompleted() =
        runTest {
            val facade =
                FakeSettingsConnectionFacade(
                    testConnectionResult =
                        ConnectionTestResult.Success(
                            normalizedBaseUrl = "https://fastgpt.example.com/",
                            apps =
                                listOf(
                                    ConnectionTestApp(id = "app-primary", name = "Primary"),
                                    ConnectionTestApp(id = "app-secondary", name = "Secondary"),
                                ),
                            latencyMs = 88,
                        ),
                )
            val viewModel = ConnectionConfigViewModel(facade)

            viewModel.onBaseUrlChanged("https://fastgpt.example.com")
            viewModel.onApiKeyChanged("fastgpt-secret")
            viewModel.testConnection()
            advanceUntilIdle()

            var saved = false
            viewModel.saveConnection { saved = true }
            advanceUntilIdle()

            assertTrue(saved)
            assertEquals("https://fastgpt.example.com", facade.savedBaseUrl)
            assertEquals("fastgpt-secret", facade.savedApiKey)
            assertEquals("app-primary", facade.savedSelectedAppId)
            assertTrue(facade.savedOnboardingCompleted)
        }

    @Test
    fun settingsOverviewViewModel_mapsSnapshotToSummaries() =
        runTest {
            val facade =
                FakeSettingsConnectionFacade(
                    initialSnapshot =
                        ConnectionSnapshot(
                            serverBaseUrl = "https://fastgpt.example.com/",
                            apiKey = "fastgpt-secret",
                            selectedAppId = "app-42",
                            onboardingCompleted = true,
                            appearancePreferences =
                                AppearancePreferences(
                                    themeMode = ThemeMode.OLED_DARK,
                                    dynamicColorEnabled = false,
                                    oledEnabled = true,
                                ),
                        ),
                )

            val viewModel = SettingsOverviewViewModel(facade)
            advanceUntilIdle()

            assertEquals("https://fastgpt.example.com/", viewModel.uiState.value.serverBaseUrl)
            assertEquals("app-42", viewModel.uiState.value.selectedAppId)
            assertEquals(ThemeMode.OLED_DARK, viewModel.uiState.value.themeMode)
        }

    @Test
    fun themeAppearanceViewModel_updatesAppearanceThroughFacade() =
        runTest {
            val facade = FakeSettingsConnectionFacade()
            val viewModel = ThemeAppearanceViewModel(facade)

            viewModel.updateThemeMode(ThemeMode.DARK)
            viewModel.updateDynamicColorEnabled(false)
            viewModel.updateOledEnabled(true)
            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, facade.snapshot.value.appearancePreferences.themeMode)
            assertFalse(facade.snapshot.value.appearancePreferences.dynamicColorEnabled)
            assertTrue(facade.snapshot.value.appearancePreferences.oledEnabled)
        }

    @Test
    fun connectionConfigViewModel_clearRestoresDefaultServerUrl() =
        runTest {
            val facade = FakeSettingsConnectionFacade()
            val viewModel = ConnectionConfigViewModel(facade)

            viewModel.onBaseUrlChanged("https://example.com")
            viewModel.clearConnection()
            advanceUntilIdle()

            assertEquals("https://api.fastgpt.in/api", viewModel.uiState.value.serverUrl)
            assertEquals("", viewModel.uiState.value.apiKey)
        }

    @Test
    fun chatPreferencesViewModel_updatesStoredPreferences() =
        runTest {
            val facade =
                FakeSettingsConnectionFacade(
                    initialSnapshot =
                        ConnectionSnapshot(
                            appearancePreferences =
                                AppearancePreferences(
                                    streamEnabled = true,
                                    autoScroll = true,
                                    fontSizeScale = 1f,
                                    showCitationsByDefault = true,
                                ),
                        ),
                )
            val viewModel = ChatPreferencesViewModel(facade)

            viewModel.updateStreamEnabled(false)
            viewModel.updateAutoScroll(false)
            viewModel.updateFontSizeScale(1.25f)
            viewModel.updateShowCitationsByDefault(false)
            advanceUntilIdle()

            assertFalse(facade.snapshot.value.appearancePreferences.streamEnabled)
            assertFalse(facade.snapshot.value.appearancePreferences.autoScroll)
            assertEquals(1.25f, facade.snapshot.value.appearancePreferences.fontSizeScale)
            assertFalse(facade.snapshot.value.appearancePreferences.showCitationsByDefault)
        }

    @Test
    fun audioSettingsViewModel_updatesStoredPreferences() =
        runTest {
            val facade =
                FakeSettingsConnectionFacade(
                    initialSnapshot =
                        ConnectionSnapshot(
                            audioPreferences =
                                AudioPreferences(
                                    speechToTextEngine = SpeechToTextEngine.System,
                                    autoSendTranscripts = false,
                                    textToSpeechEngine = TextToSpeechEngine.System,
                                    speechRate = 1f,
                                    defaultVoiceName = null,
                                ),
                        ),
                )
            val viewModel = AudioSettingsViewModel(facade)

            viewModel.updateSpeechToTextEngine(SpeechToTextEngine.WhisperApp)
            viewModel.updateAutoSendTranscripts(true)
            viewModel.updateTextToSpeechEngine(TextToSpeechEngine.AppManaged)
            viewModel.updateSpeechRate(2.5f)
            viewModel.updateDefaultVoiceName("  Voice A  ")
            advanceUntilIdle()

            val preferences = facade.snapshot.value.audioPreferences
            assertEquals(SpeechToTextEngine.WhisperApp, preferences.speechToTextEngine)
            assertTrue(preferences.autoSendTranscripts)
            assertEquals(TextToSpeechEngine.AppManaged, preferences.textToSpeechEngine)
            assertEquals(2.0f, preferences.speechRate)
            assertEquals("Voice A", preferences.defaultVoiceName)
        }

    @Test
    fun languageSettingsViewModel_updatesStoredLanguageTag() =
        runTest {
            val facade = FakeSettingsConnectionFacade()
            val viewModel = LanguageSettingsViewModel(facade)

            viewModel.updateLanguageTag("en")
            advanceUntilIdle()

            assertEquals("en", facade.snapshot.value.appearancePreferences.languageTag)
        }

    @Test
    fun cacheSettingsViewModel_loadsAndClearsCache() =
        runTest {
            val cacheManager =
                FakeSettingsCacheManager(
                    storageBuckets =
                        mapOf(
                            StorageBucket.DATABASE to 2_048L,
                            StorageBucket.PREFERENCES to 512L,
                            StorageBucket.TEMP_CACHE to 1_536L,
                        ),
                )
            val viewModel = CacheSettingsViewModel(cacheManager)
            advanceUntilIdle()

            assertEquals(2_048L, viewModel.uiState.value.storageBuckets[StorageBucket.DATABASE])
            assertEquals(512L, viewModel.uiState.value.storageBuckets[StorageBucket.PREFERENCES])
            assertEquals(1_536L, viewModel.uiState.value.storageBuckets[StorageBucket.TEMP_CACHE])

            viewModel.clearCache()
            advanceUntilIdle()

            assertTrue(cacheManager.clearRequested)
            assertEquals(0L, viewModel.uiState.value.storageBuckets[StorageBucket.TEMP_CACHE])
            assertEquals(2_048L, viewModel.uiState.value.storageBuckets[StorageBucket.DATABASE])
            assertEquals(512L, viewModel.uiState.value.storageBuckets[StorageBucket.PREFERENCES])
        }

    @Test
    fun aboutViewModel_exposesAppMetadata() =
        runTest {
            val viewModel =
                AboutViewModel(
                    appInfoProvider =
                        FakeAppInfoProvider(
                            versionName = "1.2.3",
                            feedbackUrl = "https://github.com/lifuyue/kora/issues",
                            licensesUrl = "https://example.com/licenses",
                        ),
                )

            assertEquals("1.2.3", viewModel.uiState.value.versionName)
            assertEquals("https://github.com/lifuyue/kora/issues", viewModel.uiState.value.feedbackUrl)
            assertEquals("https://example.com/licenses", viewModel.uiState.value.licensesUrl)
        }
}

private class FakeSettingsConnectionFacade(
    initialSnapshot: ConnectionSnapshot = ConnectionSnapshot(),
    private val testConnectionResult: ConnectionTestResult =
        ConnectionTestResult.Success(
            normalizedBaseUrl = "https://fastgpt.example.com/",
            apps = listOf(ConnectionTestApp(id = "app-1", name = "Kora")),
            latencyMs = 42,
        ),
) : SettingsConnectionFacade {
    private val mutableSnapshot = MutableStateFlow(initialSnapshot)

    override val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot

    var savedBaseUrl: String? = null
        private set
    var savedApiKey: String? = null
        private set
    var savedSelectedAppId: String? = null
        private set
    var savedOnboardingCompleted: Boolean = false
        private set

    override suspend fun testConnection(
        serverBaseUrl: String,
        apiKey: String,
    ): ConnectionTestResult = testConnectionResult

    override suspend fun saveConnection(
        serverBaseUrl: String,
        apiKey: String,
        selectedAppId: String,
        onboardingCompleted: Boolean,
    ) {
        savedBaseUrl = serverBaseUrl
        savedApiKey = apiKey
        savedSelectedAppId = selectedAppId
        savedOnboardingCompleted = onboardingCompleted
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                serverBaseUrl = serverBaseUrl,
                apiKey = apiKey,
                selectedAppId = selectedAppId,
                onboardingCompleted = onboardingCompleted,
            )
    }

    override suspend fun clearConnection() {
        mutableSnapshot.value = ConnectionSnapshot()
    }

    override suspend fun updateSelectedAppId(selectedAppId: String) {
        savedSelectedAppId = selectedAppId
        mutableSnapshot.value = mutableSnapshot.value.copy(selectedAppId = selectedAppId)
    }

    override suspend fun updateAppearance(
        themeMode: ThemeMode,
        dynamicColorEnabled: Boolean,
        oledEnabled: Boolean,
    ) {
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                appearancePreferences =
                    AppearancePreferences(
                        themeMode = themeMode,
                        dynamicColorEnabled = dynamicColorEnabled,
                        oledEnabled = oledEnabled,
                        streamEnabled = mutableSnapshot.value.appearancePreferences.streamEnabled,
                        autoScroll = mutableSnapshot.value.appearancePreferences.autoScroll,
                        fontSizeScale = mutableSnapshot.value.appearancePreferences.fontSizeScale,
                        showCitationsByDefault = mutableSnapshot.value.appearancePreferences.showCitationsByDefault,
                    ),
            )
    }

    override suspend fun updateChatPreferences(
        streamEnabled: Boolean,
        autoScroll: Boolean,
        fontSizeScale: Float,
        showCitationsByDefault: Boolean,
    ) {
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                appearancePreferences =
                    mutableSnapshot.value.appearancePreferences.copy(
                        streamEnabled = streamEnabled,
                        autoScroll = autoScroll,
                        fontSizeScale = fontSizeScale,
                        showCitationsByDefault = showCitationsByDefault,
                    ),
            )
    }

    override suspend fun updateLanguageTag(languageTag: String?) {
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                appearancePreferences = mutableSnapshot.value.appearancePreferences.copy(languageTag = languageTag),
            )
    }

    override suspend fun updateAudioPreferences(
        speechToTextEngine: SpeechToTextEngine,
        autoSendTranscripts: Boolean,
        textToSpeechEngine: TextToSpeechEngine,
        speechRate: Float,
        defaultVoiceName: String?,
    ) {
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                audioPreferences =
                    mutableSnapshot.value.audioPreferences.copy(
                        speechToTextEngine = speechToTextEngine,
                        autoSendTranscripts = autoSendTranscripts,
                        textToSpeechEngine = textToSpeechEngine,
                        speechRate = speechRate,
                        defaultVoiceName = defaultVoiceName,
                    ),
            )
    }
}

private class FakeSettingsCacheManager(
    storageBuckets: Map<StorageBucket, Long>,
) : SettingsCacheManager {
    private var currentStorageBuckets = storageBuckets.toMap()

    var clearRequested: Boolean = false
        private set

    override suspend fun getStorageBuckets(): Map<StorageBucket, Long> = currentStorageBuckets

    override suspend fun clearCache() {
        clearRequested = true
        currentStorageBuckets =
            currentStorageBuckets.toMutableMap().apply {
                this[StorageBucket.TEMP_CACHE] = 0L
            }
    }
}

private class FakeAppInfoProvider(
    private val versionName: String,
    private val feedbackUrl: String,
    private val licensesUrl: String,
) : AppInfoProvider {
    override fun versionName(): String = versionName

    override fun feedbackUrl(): String = feedbackUrl

    override fun licensesUrl(): String = licensesUrl
}
