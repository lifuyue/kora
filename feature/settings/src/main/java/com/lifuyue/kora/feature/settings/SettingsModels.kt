package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.KoraFeedbackPhase
import com.lifuyue.kora.core.common.ThemeMode

enum class ConnectionFeedbackSource {
    Test,
    Save,
    Clear,
}

data class ConnectionInlineFeedbackUiState(
    val phase: KoraFeedbackPhase = KoraFeedbackPhase.Idle,
    val source: ConnectionFeedbackSource = ConnectionFeedbackSource.Test,
    val saveErrorMessage: String? = null,
)

data class ConnectionConfigUiState(
    val connectionType: ConnectionType = ConnectionType.OPENAI_COMPATIBLE,
    val serverUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "",
    val apiKeyMaskedSummary: String = "",
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val validationResult: ConnectionTestResult? = null,
    val feedback: ConnectionInlineFeedbackUiState = ConnectionInlineFeedbackUiState(),
)

data class SettingsEntryUiModel(
    val title: String,
    val summary: String,
    val badge: String? = null,
)

data class SettingsOverviewUiState(
    val connectionType: ConnectionType = ConnectionType.OPENAI_COMPATIBLE,
    val serverBaseUrl: String? = null,
    val model: String? = null,
    val selectedAppId: String? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val selectedLanguageTag: String? = "zh-CN",
    val showReasoningEntry: Boolean = true,
    val streamResponses: Boolean = true,
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
    val themeMode: ThemeMode = ThemeMode.DARK,
)

data class ChatPreferencesUiState(
    val showReasoningEntry: Boolean = true,
    val streamResponses: Boolean = true,
)

data class CurrentAppSettingsUiState(
    val connectionType: ConnectionType = ConnectionType.OPENAI_COMPATIBLE,
    val selectedAppId: String? = null,
    val selectedAppName: String = "",
    val model: String? = null,
    val serverBaseUrl: String? = null,
    val items: List<SettingsCurrentAppItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class SettingsCurrentAppItemUiModel(
    val appId: String,
    val name: String,
    val intro: String,
    val isSelected: Boolean,
)

enum class StorageBucket {
    DATABASE,
    PREFERENCES,
    TEMP_CACHE,
}
