package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
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

            assertEquals("https://fastgpt.example.com/", viewModel.uiState.value.connectionSummary)
            assertEquals("app-42", viewModel.uiState.value.selectedAppSummary)
            assertEquals("OLED 深色", viewModel.uiState.value.themeSummary)
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
            val cacheManager = FakeSettingsCacheManager(cacheSizeBytes = 1_536L)
            val viewModel = CacheSettingsViewModel(cacheManager)
            advanceUntilIdle()

            assertEquals("1.5 KB", viewModel.uiState.value.cacheSizeLabel)

            viewModel.clearCache()
            advanceUntilIdle()

            assertTrue(cacheManager.clearRequested)
            assertEquals("0 B", viewModel.uiState.value.cacheSizeLabel)
        }

    @Test
    fun aboutViewModel_exposesAppMetadata() =
        runTest {
            val viewModel =
                AboutViewModel(
                    appInfoProvider =
                        FakeAppInfoProvider(
                            versionName = "1.2.3",
                            feedbackUrl = "mailto:kora@example.com",
                            licensesUrl = "https://example.com/licenses",
                        ),
                )

            assertEquals("1.2.3", viewModel.uiState.value.versionName)
            assertEquals("mailto:kora@example.com", viewModel.uiState.value.feedbackUrl)
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
}

private class FakeSettingsCacheManager(
    cacheSizeBytes: Long,
) : SettingsCacheManager {
    private var currentSizeBytes = cacheSizeBytes

    var clearRequested: Boolean = false
        private set

    override suspend fun getCacheSizeBytes(): Long = currentSizeBytes

    override suspend fun clearCache() {
        clearRequested = true
        currentSizeBytes = 0L
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
