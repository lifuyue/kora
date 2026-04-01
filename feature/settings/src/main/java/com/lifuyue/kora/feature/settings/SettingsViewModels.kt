package com.lifuyue.kora.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifuyue.kora.core.common.DIRECT_OPENAI_APP_ID
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionConfigViewModel
    @Inject
    constructor(
        private val connectionFacade: SettingsConnectionFacade,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ConnectionConfigUiState())
        val uiState: StateFlow<ConnectionConfigUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                connectionFacade.snapshot.collect { snapshot ->
                    mutableState.value =
                        mutableState.value.copy(
                            connectionType = snapshot.connectionType,
                            serverUrl = snapshot.serverBaseUrl ?: mutableState.value.serverUrl,
                            model = snapshot.model ?: mutableState.value.model,
                            apiKeyMaskedSummary = redactApiKey(snapshot.apiKey),
                        )
                }
            }
        }

        fun onConnectionTypeChanged(value: ConnectionType) {
            mutableState.value =
                mutableState.value.copy(
                    connectionType = value,
                    serverUrl =
                        if (value == ConnectionType.OPENAI_COMPATIBLE) {
                            "https://api.openai.com/v1"
                        } else {
                            "https://api.fastgpt.in/api"
                        },
                    canSave = false,
                    testResult = null,
                )
        }

        fun onBaseUrlChanged(value: String) {
            mutableState.value = mutableState.value.copy(serverUrl = value, canSave = false, testResult = null)
        }

        fun onApiKeyChanged(value: String) {
            mutableState.value = mutableState.value.copy(apiKey = value, canSave = false, testResult = null)
        }

        fun onModelChanged(value: String) {
            mutableState.value = mutableState.value.copy(model = value, canSave = false, testResult = null)
        }

        fun testConnection() {
            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(isTesting = true, canSave = false)
                val result =
                    connectionFacade.testConnection(
                        connectionType = mutableState.value.connectionType,
                        serverBaseUrl = mutableState.value.serverUrl,
                        apiKey = mutableState.value.apiKey,
                        model = mutableState.value.model,
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
            val selectedAppId =
                when (mutableState.value.connectionType) {
                    ConnectionType.OPENAI_COMPATIBLE -> DIRECT_OPENAI_APP_ID
                    ConnectionType.FAST_GPT -> result.apps.firstOrNull()?.id ?: return
                }

            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(isSaving = true)
                connectionFacade.saveConnection(
                    connectionType = mutableState.value.connectionType,
                    serverBaseUrl = mutableState.value.serverUrl,
                    apiKey = mutableState.value.apiKey,
                    model = mutableState.value.model.ifBlank { null },
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
                        connectionType = ConnectionType.OPENAI_COMPATIBLE,
                        serverUrl = ConnectionConfigUiState().serverUrl,
                        apiKey = "",
                        model = "",
                        apiKeyMaskedSummary = "",
                    )
            }
        }
    }

@HiltViewModel
class SettingsOverviewViewModel
    @Inject
    constructor(
        connectionFacade: SettingsConnectionFacade,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsOverviewUiState> =
            connectionFacade.snapshot
                .map { snapshot ->
                    SettingsOverviewUiState(
                        connectionType = snapshot.connectionType,
                        serverBaseUrl = snapshot.serverBaseUrl,
                        model = snapshot.model,
                        selectedAppId = snapshot.selectedAppId,
                        themeMode = snapshot.appearancePreferences.themeMode,
                        selectedLanguageTag = snapshot.appearancePreferences.languageTag,
                    )
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsOverviewUiState())
    }

@HiltViewModel
class ThemeAppearanceViewModel
    @Inject
    constructor(
        private val connectionFacade: SettingsConnectionFacade,
    ) : ViewModel() {
        val uiState: StateFlow<ThemeAppearanceUiState> =
            connectionFacade.snapshot
                .map { snapshot ->
                    ThemeAppearanceUiState(
                        themeMode = snapshot.appearancePreferences.themeMode,
                    )
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeAppearanceUiState())

        fun updateThemeMode(mode: ThemeMode) {
            viewModelScope.launch {
                connectionFacade.updateAppearance(
                    themeMode = mode,
                )
            }
        }
    }

@HiltViewModel
class LanguageSettingsViewModel
    @Inject
    constructor(
        private val connectionFacade: SettingsConnectionFacade,
    ) : ViewModel() {
        val uiState: StateFlow<LanguageSettingsUiState> =
            connectionFacade.snapshot
                .map { snapshot ->
                    LanguageSettingsUiState(
                        selectedLanguageTag = snapshot.appearancePreferences.languageTag,
                    )
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, LanguageSettingsUiState())

        fun updateLanguageTag(languageTag: String?) {
            viewModelScope.launch {
                connectionFacade.updateLanguageTag(languageTag)
            }
        }
    }

@HiltViewModel
class CacheSettingsViewModel
    @Inject
    constructor(
        private val cacheManager: SettingsCacheManager,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(CacheSettingsUiState())
        val uiState: StateFlow<CacheSettingsUiState> = mutableState.asStateFlow()

        init {
            refreshCacheSize()
        }

        fun clearCache() {
            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(isClearing = true)
                cacheManager.clearCache()
                mutableState.value =
                    mutableState.value.copy(
                        isClearing = false,
                        storageBuckets = cacheManager.getStorageBuckets(),
                    )
            }
        }

        private fun refreshCacheSize() {
            viewModelScope.launch {
                mutableState.value =
                    mutableState.value.copy(
                        storageBuckets = cacheManager.getStorageBuckets(),
                    )
            }
        }
    }

@HiltViewModel
class AboutViewModel
    @Inject
    constructor(
        appInfoProvider: AppInfoProvider,
    ) : ViewModel() {
        val uiState =
            MutableStateFlow(
                AboutUiState(
                    versionName = appInfoProvider.versionName(),
                    feedbackUrl = appInfoProvider.feedbackUrl(),
                    licensesUrl = appInfoProvider.licensesUrl(),
                ),
            ).asStateFlow()
    }
