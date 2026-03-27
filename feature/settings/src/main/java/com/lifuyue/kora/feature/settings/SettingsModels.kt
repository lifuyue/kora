package com.lifuyue.kora.feature.settings

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
    val connectionSummary: String = "未配置",
    val themeSummary: String = "跟随系统",
    val selectedAppSummary: String = "未选择",
)

data class ThemeAppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledEnabled: Boolean = false,
)
