package com.lifuyue.kora.feature.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.SpeechToTextEngine
import com.lifuyue.kora.core.common.TextToSpeechEngine
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.common.ui.KoraMetricRow
import com.lifuyue.kora.core.common.ui.KoraSectionCard

@Composable
fun ConnectionConfigScreen(
    state: ConnectionConfigUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_connection_title),
        onBack = onBack,
        modifier = modifier.semantics { testTag = "settings-overview-scroll" },
    ) {
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onBaseUrlChange,
            label = { Text(stringResource(R.string.settings_connection_server_url_label)) },
            modifier = Modifier.fillMaxWidth().semantics { testTag = "server-url" },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChange,
            label = { Text(stringResource(R.string.settings_connection_api_key_label)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().semantics { testTag = "api-key" },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onTestConnection,
                enabled = !state.isTesting,
                modifier = Modifier.semantics { testTag = "test-connection" },
            ) {
                Text(
                    stringResource(
                        if (state.isTesting) {
                            R.string.settings_connection_testing
                        } else {
                            R.string.settings_connection_test
                        },
                    ),
                )
            }
            Button(
                onClick = onSave,
                enabled = state.canSave && !state.isSaving,
                modifier = Modifier.semantics { testTag = "save-connection" },
            ) {
                Text(
                    stringResource(
                        if (state.isSaving) {
                            R.string.settings_connection_saving
                        } else {
                            R.string.settings_connection_save
                        },
                    ),
                )
            }
            Button(
                onClick = onClear,
                modifier = Modifier.semantics { testTag = "clear-connection" },
            ) {
                Text(stringResource(R.string.settings_connection_clear))
            }
        }
        state.testResult?.let {
            ConnectionTestResultCard(result = it)
        }
        if (state.apiKeyMaskedSummary.isNotBlank()) {
            Text(
                text = stringResource(R.string.settings_connection_api_key_summary, state.apiKeyMaskedSummary),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ConnectionTestResultCard(result: ConnectionTestResult) {
    val description =
        when (result) {
            is ConnectionTestResult.Success ->
                pluralStringResource(
                    R.plurals.settings_connection_success,
                    result.apps.size,
                    result.apps.size,
                    result.latencyMs,
                )
            is ConnectionTestResult.ValidationError ->
                stringResource(R.string.settings_connection_validation_error, result.reason)
            is ConnectionTestResult.AuthError ->
                stringResource(R.string.settings_connection_auth_error, result.error.message)
            is ConnectionTestResult.NetworkFailure ->
                stringResource(R.string.settings_connection_network_error, result.message)
            is ConnectionTestResult.ServerError ->
                stringResource(R.string.settings_connection_server_error, result.error.message)
        }

    Card(modifier = Modifier.fillMaxWidth().semantics { testTag = "connection-result" }) {
        Text(
            text = description,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun SettingsOverviewScreen(
    state: SettingsOverviewUiState,
    onOpenConnection: () -> Unit,
    onOpenCurrentApp: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenChatPreferences: () -> Unit,
    onOpenAudio: () -> Unit = {},
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .semantics { testTag = "settings-overview-scroll" },
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.settings_overview_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            KoraSectionCard(
                tag = "settings_status_card",
            ) {
                Text(
                    text = stringResource(R.string.settings_overview_status_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.settings_overview_status_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KoraMetricRow(
                    label = stringResource(R.string.settings_overview_status_connection),
                    value = state.serverBaseUrl ?: stringResource(R.string.settings_summary_not_configured),
                )
                KoraMetricRow(
                    label = stringResource(R.string.settings_overview_status_theme),
                    value = themeModeLabel(state.themeMode),
                )
                KoraMetricRow(
                    label = stringResource(R.string.settings_overview_status_app),
                    value = state.selectedAppId ?: stringResource(R.string.settings_summary_not_selected),
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(R.string.settings_overview_section_connection),
                content = {
                    SettingsEntry(
                        title = stringResource(R.string.settings_connection_title),
                        summary = state.serverBaseUrl ?: stringResource(R.string.settings_summary_not_configured),
                        onClick = onOpenConnection,
                        testTag = "settings-connection",
                    )
                    SettingsEntry(
                        title = stringResource(R.string.settings_current_app_title),
                        summary = state.selectedAppId ?: stringResource(R.string.settings_summary_not_selected),
                        onClick = onOpenCurrentApp,
                        testTag = "settings-current-app",
                    )
                },
            )
        }
        item {
            SettingsSection(
                title = stringResource(R.string.settings_overview_section_appearance),
                content = {
                    SettingsEntry(
                        title = stringResource(R.string.settings_theme_title),
                        summary = themeModeLabel(state.themeMode),
                        onClick = onOpenTheme,
                        testTag = "settings-theme",
                    )
                },
            )
        }
        item {
            SettingsSection(
                title = stringResource(R.string.settings_overview_section_common),
                content = {
                    SettingsEntry(
                        title = stringResource(R.string.settings_chat_preferences_title),
                        summary = stringResource(R.string.settings_chat_preferences_summary),
                        onClick = onOpenChatPreferences,
                        testTag = "settings-chat-preferences",
                    )
                    SettingsEntry(
                        title = appString("settings_audio_title"),
                        summary = appString("settings_audio_summary"),
                        onClick = onOpenAudio,
                        testTag = "settings-audio",
                    )
                    SettingsEntry(
                        title = stringResource(R.string.settings_language_title),
                        summary = languageLabel(state.selectedLanguageTag),
                        onClick = onOpenLanguage,
                        testTag = "settings-language",
                    )
                    SettingsEntry(
                        title = stringResource(R.string.settings_storage_title),
                        summary = stringResource(R.string.settings_storage_summary),
                        onClick = onOpenCache,
                        testTag = "settings-cache",
                    )
                    SettingsEntry(
                        title = stringResource(R.string.settings_about_title),
                        summary = stringResource(R.string.settings_about_summary),
                        onClick = onOpenAbout,
                        testTag = "settings-about",
                    )
                },
            )
        }
    }
}

@Composable
private fun languageLabel(languageTag: String?): String =
    when (languageTag) {
        null -> stringResource(R.string.settings_language_follow_system)
        "zh-CN" -> stringResource(R.string.settings_language_simplified_chinese)
        "en" -> stringResource(R.string.settings_language_english)
        else -> languageTag
    }

@Composable
internal fun SettingsAdaptivePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(appString("adaptive_settings_placeholder_title"), style = MaterialTheme.typography.headlineSmall)
        Text(appString("adaptive_settings_placeholder_body"), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun SettingsEntry(
    title: String,
    summary: String,
    onClick: (() -> Unit)?,
    testTag: String,
) {
    val titleModifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(onClick = onClick)
        }

    Card(
        modifier = Modifier.fillMaxWidth().semantics { this.testTag = testTag },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, modifier = titleModifier, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ThemeAppearanceScreen(
    state: ThemeAppearanceUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOledChange: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_theme_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = state.themeMode == mode, onClick = { onThemeModeChange(mode) }),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(themeModeLabel(mode))
                Text(if (state.themeMode == mode) stringResource(R.string.settings_selected) else "")
            }
            HorizontalDivider()
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_theme_dynamic_color))
            Switch(
                checked = state.dynamicColorEnabled,
                onCheckedChange = onDynamicColorChange,
                modifier = Modifier.semantics { testTag = "dynamic-color" },
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_theme_oled))
            Switch(
                checked = state.oledEnabled,
                onCheckedChange = onOledChange,
                modifier = Modifier.semantics { testTag = "oled-toggle" },
            )
        }
    }
}

@Composable
fun ChatPreferencesScreen(
    state: ChatPreferencesUiState,
    onStreamEnabledChange: (Boolean) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onShowCitationsChange: (Boolean) -> Unit,
    onFontSizeScaleChange: (Float) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_chat_preferences_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_chat_preferences_stream))
            Switch(
                checked = state.streamEnabled,
                onCheckedChange = onStreamEnabledChange,
                modifier = Modifier.semantics { testTag = "chat-pref-stream" },
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_chat_preferences_auto_scroll))
            Switch(
                checked = state.autoScroll,
                onCheckedChange = onAutoScrollChange,
                modifier = Modifier.semantics { testTag = "chat-pref-auto-scroll" },
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_chat_preferences_citations))
            Switch(
                checked = state.showCitationsByDefault,
                onCheckedChange = onShowCitationsChange,
                modifier = Modifier.semantics { testTag = "chat-pref-citations" },
            )
        }
        Text(stringResource(R.string.settings_chat_preferences_font_scale, (state.fontSizeScale * 100).toInt()))
        Slider(
            value = state.fontSizeScale,
            onValueChange = onFontSizeScaleChange,
            valueRange = 0.8f..1.4f,
            steps = 5,
        )
    }
}

@Composable
fun AudioSettingsScreen(
    state: AudioSettingsUiState,
    onSpeechToTextEngineChange: (SpeechToTextEngine) -> Unit,
    onAutoSendTranscriptsChange: (Boolean) -> Unit,
    onTextToSpeechEngineChange: (TextToSpeechEngine) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onDefaultVoiceNameChange: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = appString("settings_audio_title"),
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsSection(
            title = appString("settings_audio_section_stt"),
            content = {
                Text(appString("settings_audio_stt_engine"), style = MaterialTheme.typography.titleMedium)
                AudioEngineOption(
                    tag = "audio-stt-system",
                    label = appString("settings_audio_stt_engine_system"),
                    selected = state.speechToTextEngine == SpeechToTextEngine.System,
                    onClick = { onSpeechToTextEngineChange(SpeechToTextEngine.System) },
                )
                AudioEngineOption(
                    tag = "audio-stt-whisper",
                    label = appString("settings_audio_stt_engine_whisper"),
                    selected = state.speechToTextEngine == SpeechToTextEngine.WhisperApp,
                    onClick = { onSpeechToTextEngineChange(SpeechToTextEngine.WhisperApp) },
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(appString("settings_audio_auto_send_transcripts"))
                    Switch(
                        checked = state.autoSendTranscripts,
                        onCheckedChange = onAutoSendTranscriptsChange,
                        modifier = Modifier.semantics { testTag = "audio-auto-send" },
                    )
                }
            },
        )
        SettingsSection(
            title = appString("settings_audio_section_tts"),
            content = {
                Text(appString("settings_audio_tts_engine"), style = MaterialTheme.typography.titleMedium)
                AudioEngineOption(
                    tag = "audio-tts-system",
                    label = appString("settings_audio_tts_engine_system"),
                    selected = state.textToSpeechEngine == TextToSpeechEngine.System,
                    onClick = { onTextToSpeechEngineChange(TextToSpeechEngine.System) },
                )
                AudioEngineOption(
                    tag = "audio-tts-app-managed",
                    label = appString("settings_audio_tts_engine_app_managed"),
                    selected = state.textToSpeechEngine == TextToSpeechEngine.AppManaged,
                    onClick = { onTextToSpeechEngineChange(TextToSpeechEngine.AppManaged) },
                )
                Text(appString("settings_audio_speech_rate", state.speechRate), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = state.speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.5f..2.0f,
                    steps = 9,
                    modifier = Modifier.semantics { testTag = "audio-speech-rate" },
                )
                OutlinedTextField(
                    value = state.defaultVoiceName.orEmpty(),
                    onValueChange = onDefaultVoiceNameChange,
                    label = { Text(appString("settings_audio_default_voice")) },
                    supportingText = { Text(appString("settings_audio_default_voice_hint")) },
                    modifier = Modifier.fillMaxWidth().semantics { testTag = "audio-default-voice" },
                    singleLine = true,
                )
            },
        )
    }
}

@Composable
fun LanguageSettingsScreen(
    state: LanguageSettingsUiState,
    onLanguageTagChange: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_language_title),
        onBack = onBack,
        modifier = modifier,
        verticalSpacing = 12.dp,
    ) {
        LanguageOption(
            tag = "language-option-system",
            label = stringResource(R.string.settings_language_follow_system),
            selected = state.selectedLanguageTag == null,
            onClick = { onLanguageTagChange(null) },
        )
        LanguageOption(
            tag = "language-option-zh-CN",
            label = stringResource(R.string.settings_language_simplified_chinese),
            selected = state.selectedLanguageTag == "zh-CN",
            onClick = { onLanguageTagChange("zh-CN") },
        )
        LanguageOption(
            tag = "language-option-en",
            label = stringResource(R.string.settings_language_english),
            selected = state.selectedLanguageTag == "en",
            onClick = { onLanguageTagChange("en") },
        )
    }
}

@Composable
private fun LanguageOption(
    tag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { testTag = tag },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text(if (selected) stringResource(R.string.settings_selected) else "")
        }
    }
}

@Composable
private fun AudioEngineOption(
    tag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { testTag = tag },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Text(if (selected) stringResource(R.string.settings_selected) else "")
            }
        }
    }
}

@Composable
fun CacheSettingsScreen(
    state: CacheSettingsUiState,
    onClearCache: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SettingsPageColumn(
        title = stringResource(R.string.settings_storage_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        StorageBucket.entries.forEach { bucket ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(storageBucketLabel(bucket), style = MaterialTheme.typography.titleMedium)
                        Text(
                            Formatter.formatShortFileSize(context, state.storageBuckets[bucket] ?: 0L),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (bucket == StorageBucket.TEMP_CACHE) {
                        Button(
                            onClick = onClearCache,
                            enabled = !state.isClearing,
                            modifier = Modifier.semantics { testTag = "clear-cache" },
                        ) {
                            Text(
                                stringResource(
                                    if (state.isClearing) {
                                        R.string.settings_storage_clearing
                                    } else {
                                        R.string.settings_storage_clear
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    state: AboutUiState,
    onOpenFeedback: () -> Unit,
    onOpenLicenses: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_about_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.settings_about_summary), style = MaterialTheme.typography.titleLarge)
        Text(state.versionName, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = onOpenFeedback,
            modifier = Modifier.semantics { testTag = "about-feedback" },
        ) {
            Text(stringResource(R.string.settings_about_feedback))
        }
        OutlinedButton(
            onClick = onOpenLicenses,
            modifier = Modifier.semantics { testTag = "about-licenses" },
        ) {
            Text(stringResource(R.string.settings_about_licenses))
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String =
    stringResource(
        when (mode) {
            ThemeMode.LIGHT -> R.string.settings_theme_mode_light
            ThemeMode.DARK -> R.string.settings_theme_mode_dark
            ThemeMode.SYSTEM -> R.string.settings_theme_mode_system
            ThemeMode.OLED_DARK -> R.string.settings_theme_mode_oled_dark
        },
    )

@Composable
private fun storageBucketLabel(bucket: StorageBucket): String =
    stringResource(
        when (bucket) {
            StorageBucket.DATABASE -> R.string.settings_storage_bucket_database
            StorageBucket.PREFERENCES -> R.string.settings_storage_bucket_preferences
            StorageBucket.TEMP_CACHE -> R.string.settings_storage_bucket_temp_cache
        },
    )

@Composable
private fun SettingsPageColumn(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    verticalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        SettingsPageHeader(title = title, onBack = onBack)
        content()
    }
}

@Composable
private fun SettingsPageHeader(
    title: String,
    onBack: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back))
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
