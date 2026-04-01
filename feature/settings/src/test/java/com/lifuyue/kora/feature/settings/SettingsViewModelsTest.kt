package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelsTest {
    @Test
    fun themeAppearanceViewModelReflectsSnapshotAndUpdatesTheme() =
        runTest {
            val facade = FakeSettingsConnectionFacade()
            facade.mutableSnapshot.value =
                ConnectionSnapshot(
                    appearancePreferences = AppearancePreferences(themeMode = ThemeMode.LIGHT, languageTag = "en"),
                )

            val viewModel = ThemeAppearanceViewModel(facade)

            assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

            viewModel.updateThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, facade.snapshot.value.appearancePreferences.themeMode)
        }

    @Test
    fun settingsOverviewViewModelExposesOnlyKeptSummaryFields() =
        runTest {
            val facade = FakeSettingsConnectionFacade()
            facade.mutableSnapshot.value =
                ConnectionSnapshot(
                    connectionType = ConnectionType.FAST_GPT,
                    serverBaseUrl = "https://api.fastgpt.in/",
                    selectedAppId = "app-1",
                    appearancePreferences = AppearancePreferences(themeMode = ThemeMode.DARK, languageTag = null),
                )

            val viewModel = SettingsOverviewViewModel(facade)

            assertEquals(ConnectionType.FAST_GPT, viewModel.uiState.value.connectionType)
            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
            assertNull(viewModel.uiState.value.model)
            assertEquals("app-1", viewModel.uiState.value.selectedAppId)
        }
}

private class FakeSettingsConnectionFacade : SettingsConnectionFacade {
    val mutableSnapshot = MutableStateFlow(ConnectionSnapshot())

    override val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot

    override suspend fun testConnection(
        connectionType: ConnectionType,
        serverBaseUrl: String,
        apiKey: String,
        model: String?,
    ): ConnectionTestResult = ConnectionTestResult.NetworkFailure("unused")

    override suspend fun saveConnection(
        connectionType: ConnectionType,
        serverBaseUrl: String,
        apiKey: String,
        model: String?,
        selectedAppId: String?,
        onboardingCompleted: Boolean,
    ) = Unit

    override suspend fun updateSelectedAppId(selectedAppId: String) = Unit

    override suspend fun clearConnection() = Unit

    override suspend fun updateAppearance(themeMode: ThemeMode) {
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                appearancePreferences =
                    mutableSnapshot.value.appearancePreferences.copy(themeMode = themeMode),
            )
    }

    override suspend fun updateLanguageTag(languageTag: String?) {
        mutableSnapshot.value =
            mutableSnapshot.value.copy(
                appearancePreferences =
                    mutableSnapshot.value.appearancePreferences.copy(languageTag = languageTag),
            )
    }
}
