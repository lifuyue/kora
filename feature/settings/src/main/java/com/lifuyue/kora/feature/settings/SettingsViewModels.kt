package com.lifuyue.kora.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectionConfigViewModel @Inject constructor(
    private val connectionFacade: SettingsConnectionFacade,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConnectionConfigUiState())
    val uiState: StateFlow<ConnectionConfigUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            connectionFacade.snapshot.collect { snapshot ->
                mutableState.value =
                    mutableState.value.copy(
                        serverUrl = snapshot.serverBaseUrl ?: mutableState.value.serverUrl,
                        apiKeyMaskedSummary = redactApiKey(snapshot.apiKey),
                    )
            }
        }
    }

    fun onBaseUrlChanged(value: String) {
        mutableState.value = mutableState.value.copy(serverUrl = value, canSave = false, testResult = null)
    }

    fun onApiKeyChanged(value: String) {
        mutableState.value = mutableState.value.copy(apiKey = value, canSave = false, testResult = null)
    }

    fun testConnection() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isTesting = true, canSave = false)
            val result =
                connectionFacade.testConnection(
                    serverBaseUrl = mutableState.value.serverUrl,
                    apiKey = mutableState.value.apiKey,
                )
            mutableState.value =
                mutableState.value.copy(
                    isTesting = false,
                    testResult = result,
                    canSave = result is ConnectionTestResult.Success,
                )
        }
    }

    fun saveConnection(onSaved: () -> Unit) {
        val result = mutableState.value.testResult as? ConnectionTestResult.Success ?: return
        val selectedAppId = result.apps.firstOrNull()?.id ?: return

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true)
            connectionFacade.saveConnection(
                serverBaseUrl = mutableState.value.serverUrl,
                apiKey = mutableState.value.apiKey,
                selectedAppId = selectedAppId,
                onboardingCompleted = true,
            )
            mutableState.value = mutableState.value.copy(isSaving = false)
            onSaved()
        }
    }

    fun clearConnection() {
        viewModelScope.launch {
            connectionFacade.clearConnection()
            mutableState.value =
                ConnectionConfigUiState(
                    serverUrl = ConnectionConfigUiState().serverUrl,
                    apiKey = "",
                    apiKeyMaskedSummary = "",
                )
        }
    }
}

@HiltViewModel
class SettingsOverviewViewModel @Inject constructor(
    connectionFacade: SettingsConnectionFacade,
) : ViewModel() {
    val uiState: StateFlow<SettingsOverviewUiState> =
        connectionFacade.snapshot
            .map { snapshot ->
                SettingsOverviewUiState(
                    connectionSummary = snapshot.serverBaseUrl ?: "未配置",
                    selectedAppSummary = snapshot.selectedAppId ?: "未选择",
                    themeSummary =
                        when (snapshot.appearancePreferences.themeMode) {
                            ThemeMode.LIGHT -> "浅色"
                            ThemeMode.DARK -> "深色"
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.OLED_DARK -> "OLED 深色"
                        },
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsOverviewUiState())
}

@HiltViewModel
class ThemeAppearanceViewModel @Inject constructor(
    private val connectionFacade: SettingsConnectionFacade,
) : ViewModel() {
    val uiState: StateFlow<ThemeAppearanceUiState> =
        connectionFacade.snapshot
            .map { snapshot ->
                ThemeAppearanceUiState(
                    themeMode = snapshot.appearancePreferences.themeMode,
                    dynamicColorEnabled = snapshot.appearancePreferences.dynamicColorEnabled,
                    oledEnabled = snapshot.appearancePreferences.oledEnabled,
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeAppearanceUiState())

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val current = connectionFacade.snapshot.value.appearancePreferences
            connectionFacade.updateAppearance(
                themeMode = mode,
                dynamicColorEnabled = current.dynamicColorEnabled,
                oledEnabled = current.oledEnabled,
            )
        }
    }

    fun updateDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = connectionFacade.snapshot.value.appearancePreferences
            connectionFacade.updateAppearance(
                themeMode = current.themeMode,
                dynamicColorEnabled = enabled,
                oledEnabled = current.oledEnabled,
            )
        }
    }

    fun updateOledEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = connectionFacade.snapshot.value.appearancePreferences
            connectionFacade.updateAppearance(
                themeMode = current.themeMode,
                dynamicColorEnabled = current.dynamicColorEnabled,
                oledEnabled = enabled,
            )
        }
    }
}
