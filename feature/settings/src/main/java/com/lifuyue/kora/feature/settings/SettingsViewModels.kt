package com.lifuyue.kora.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class SettingsOverviewViewModel
    @Inject
    constructor(
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

@HiltViewModel
class ChatPreferencesViewModel
    @Inject
    constructor(
        private val connectionFacade: SettingsConnectionFacade,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ChatPreferencesUiState())
        val uiState: StateFlow<ChatPreferencesUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                connectionFacade.snapshot.collect { snapshot ->
                    mutableState.value =
                        ChatPreferencesUiState(
                            streamEnabled = snapshot.appearancePreferences.streamEnabled,
                            autoScroll = snapshot.appearancePreferences.autoScroll,
                            fontSizeScale = snapshot.appearancePreferences.fontSizeScale,
                            showCitationsByDefault = snapshot.appearancePreferences.showCitationsByDefault,
                        )
                }
            }
        }

        fun updateStreamEnabled(enabled: Boolean) {
            updatePreferences(mutableState.value.copy(streamEnabled = enabled))
        }

        fun updateAutoScroll(enabled: Boolean) {
            updatePreferences(mutableState.value.copy(autoScroll = enabled))
        }

        fun updateFontSizeScale(value: Float) {
            updatePreferences(mutableState.value.copy(fontSizeScale = value))
        }

        fun updateShowCitationsByDefault(enabled: Boolean) {
            updatePreferences(mutableState.value.copy(showCitationsByDefault = enabled))
        }

        private fun updatePreferences(nextState: ChatPreferencesUiState) {
            mutableState.value = nextState
            viewModelScope.launch {
                connectionFacade.updateChatPreferences(
                    streamEnabled = nextState.streamEnabled,
                    autoScroll = nextState.autoScroll,
                    fontSizeScale = nextState.fontSizeScale,
                    showCitationsByDefault = nextState.showCitationsByDefault,
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
                        cacheSizeLabel = formatBytes(cacheManager.getCacheSizeBytes()),
                    )
            }
        }

        private fun refreshCacheSize() {
            viewModelScope.launch {
                mutableState.value =
                    mutableState.value.copy(
                        cacheSizeLabel = formatBytes(cacheManager.getCacheSizeBytes()),
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

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / 1024f / 1024f)
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
