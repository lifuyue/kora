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
                    ),
            )
    }
}
