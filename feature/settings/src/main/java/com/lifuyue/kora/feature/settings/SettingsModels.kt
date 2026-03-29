package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode

data class ConnectionConfigUiState(
    val serverUrl: String = "https://api.fastgpt.in/api",
    val apiKey: String = "",
    val apiKeyMaskedSummary: String = "",
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val testResult: ConnectionTestResult? = null,
)

data class SettingsEntryUiModel(
    val title: String,
    val summary: String,
    val badge: String? = null,
)

data class SettingsOverviewUiState(
    val serverBaseUrl: String? = null,
    val selectedAppId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedLanguageTag: String? = "zh-CN",
)

data class ChatPreferencesUiState(
    val streamEnabled: Boolean = true,
    val autoScroll: Boolean = true,
    val fontSizeScale: Float = 1f,
    val showCitationsByDefault: Boolean = true,
)

data class LanguageSettingsUiState(
    val selectedLanguageTag: String? = null,
)

data class CacheSettingsUiState(
    val storageBuckets: Map<StorageBucket, Long> = StorageBucket.entries.associateWith { 0L },
    val isClearing: Boolean = false,
)

data class AboutUiState(
    val versionName: String = "",
    val feedbackUrl: String = "",
    val licensesUrl: String = "",
)

data class ThemeAppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledEnabled: Boolean = false,
)

data class AudioSettingsUiState(
    val speechToTextEngine: SpeechToTextEngine = SpeechToTextEngine.System,
    val autoSendTranscripts: Boolean = false,
    val textToSpeechEngine: TextToSpeechEngine = TextToSpeechEngine.System,
    val speechRate: Float = 1f,
    val defaultVoiceName: String? = null,
)

enum class StorageBucket {
    DATABASE,
    PREFERENCES,
    TEMP_CACHE,
}
