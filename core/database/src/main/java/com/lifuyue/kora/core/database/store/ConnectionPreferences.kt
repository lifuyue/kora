package com.lifuyue.kora.core.database.store

import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ThemeMode

data class ConnectionPreferences(
    val connectionType: ConnectionType = ConnectionType.OPENAI_COMPATIBLE,
    val serverBaseUrl: String? = null,
    val apiKeyPresent: Boolean = false,
    val model: String? = null,
    val selectedAppId: String? = null,
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val languageInitialized: Boolean = false,
    val languageTag: String? = null,
)
