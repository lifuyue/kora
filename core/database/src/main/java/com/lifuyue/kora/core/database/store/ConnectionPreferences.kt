package com.lifuyue.kora.core.database.store

import com.lifuyue.kora.core.common.ThemeMode

data class ConnectionPreferences(
    val serverBaseUrl: String? = null,
    val apiKeyPresent: Boolean = false,
    val selectedAppId: String? = null,
    val onboardingCompleted: Boolean = false,
    val streamEnabled: Boolean = true,
    val autoScroll: Boolean = true,
    val fontSizeScale: Float = 1f,
    val showCitationsByDefault: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledEnabled: Boolean = false,
    val languageTag: String? = null,
)
