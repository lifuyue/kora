package com.lifuyue.kora.core.database.store

import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode

data class ConnectionPreferences(
    val connectionType: ConnectionType = ConnectionType.OPENAI_COMPATIBLE,
    val serverBaseUrl: String? = null,
    val apiKeyPresent: Boolean = false,
    val model: String? = null,
    val selectedAppId: String? = null,
    val onboardingCompleted: Boolean = false,
    val streamEnabled: Boolean = true,
    val autoScroll: Boolean = true,
    val fontSizeScale: Float = 1f,
    val showCitationsByDefault: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledEnabled: Boolean = false,
    val languageInitialized: Boolean = false,
    val languageTag: String? = null,
    val speechToTextEngine: SpeechToTextEngine = SpeechToTextEngine.System,
    val autoSendTranscripts: Boolean = false,
    val textToSpeechEngine: TextToSpeechEngine = TextToSpeechEngine.System,
    val speechRate: Float = 1f,
    val defaultVoiceName: String? = null,
)
