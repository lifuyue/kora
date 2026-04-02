package com.lifuyue.kora.feature.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.common.ui.KoraGeminiTopBar
import com.lifuyue.kora.core.common.ui.KoraMetricRow
import com.lifuyue.kora.core.common.ui.KoraSectionCard

@Composable
fun ConnectionConfigScreen(
    state: ConnectionConfigUiState,
    onConnectionTypeChange: (ConnectionType) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_connection_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier.semantics { testTag = "settings-overview-scroll" },
    ) {
        ConnectionType.entries.forEach { type ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.connectionType == type,
                            onClick = { onConnectionTypeChange(type) },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.connectionType == type, onClick = { onConnectionTypeChange(type) })
                Text(
                    text =
                        stringResource(
                            if (type == ConnectionType.OPENAI_COMPATIBLE) {
                                R.string.settings_connection_type_openai
                            } else {
                                R.string.settings_connection_type_fastgpt
                            },
                        ),
                )
            }
        }
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
        if (state.connectionType == ConnectionType.OPENAI_COMPATIBLE) {
            OutlinedTextField(
                value = state.model,
                onValueChange = onModelChange,
                label = { Text(stringResource(R.string.settings_connection_model_label)) },
                modifier = Modifier.fillMaxWidth().semantics { testTag = "model" },
                singleLine = true,
            )
        }
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
                if (result.apps.isEmpty()) {
                    stringResource(R.string.settings_connection_success_openai, result.latencyMs)
                } else {
                    pluralStringResource(
                        R.plurals.settings_connection_success,
                        result.apps.size,
                        result.apps.size,
                        result.latencyMs,
                    )
                }
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
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenChatPreferences: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val connectionTypeLabel =
        stringResource(
            if (state.connectionType == ConnectionType.OPENAI_COMPATIBLE) {
                R.string.settings_connection_type_openai
            } else {
                R.string.settings_connection_type_fastgpt
            },
        )
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { testTag = "settings-overview-scroll" },
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            KoraGeminiTopBar(
                title = stringResource(R.string.settings_overview_title),
                onOpenDrawer = onOpenDrawer,
            )
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
                    value =
                        buildString {
                            append(connectionTypeLabel)
                            state.serverBaseUrl?.let {
                                append(" · ")
                                append(it)
                            }
                        }.ifBlank { stringResource(R.string.settings_summary_not_configured) },
                )
                KoraMetricRow(
                    label = stringResource(R.string.settings_overview_status_theme),
                    value = themeModeLabel(state.themeMode),
                )
                KoraMetricRow(
                    label = stringResource(R.string.settings_overview_status_app),
                    value =
                        if (state.connectionType == ConnectionType.OPENAI_COMPATIBLE) {
                            state.model ?: stringResource(R.string.settings_summary_not_selected)
                        } else {
                            state.selectedAppId ?: stringResource(R.string.settings_summary_not_selected)
                        },
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
                    SettingsEntry(
                        title = stringResource(R.string.settings_chat_preferences_title),
                        summary =
                            stringResource(
                                R.string.settings_chat_preferences_summary,
                                if (state.showReasoningEntry) {
                                    stringResource(R.string.settings_chat_preferences_reasoning_on)
                                } else {
                                    stringResource(R.string.settings_chat_preferences_reasoning_off)
                                },
                                if (state.streamResponses) {
                                    stringResource(R.string.settings_chat_preferences_stream_on)
                                } else {
                                    stringResource(R.string.settings_chat_preferences_stream_off)
                                },
                            ),
                        onClick = onOpenChatPreferences,
                        testTag = "settings-chat-preferences",
                    )
                },
            )
        }
        item {
            SettingsSection(
                title = stringResource(R.string.settings_overview_section_common),
                content = {
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
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_theme_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    ) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
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
    }
}

@Composable
fun ChatSettingsSheetContent(
    state: SettingsOverviewUiState,
    onOpenConnection: () -> Unit,
    onOpenCurrentApp: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenChatPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surfaceContainerHigh
    val cardColor = colorScheme.surfaceContainer
    val primaryText = colorScheme.onSurface
    val secondaryText = colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = primaryText,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxWidth().testTag("chat_settings_sheet"),
            contentPadding =
                PaddingValues(
                    start = 18.dp,
                    top = 20.dp,
                    end = 18.dp,
                    bottom = 24.dp + bottomInset,
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ChatSettingsAccountHeader(
                    titleColor = primaryText,
                    subtitleColor = secondaryText,
                    avatarBackgroundColor = colorScheme.surface,
                    buttonContainerColor = colorScheme.surface,
                    buttonContentColor = primaryText,
                    onOpenConnection = onOpenConnection,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_sheet_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = secondaryText,
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_connection_title),
                    summary = state.serverBaseUrl ?: stringResource(R.string.settings_summary_not_configured),
                    onClick = onOpenConnection,
                    containerColor = cardColor,
                    testTag = "chat-settings-connection",
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_current_app_title),
                    summary =
                        if (state.connectionType == ConnectionType.OPENAI_COMPATIBLE) {
                            state.model ?: stringResource(R.string.settings_summary_not_selected)
                        } else {
                            state.selectedAppId ?: stringResource(R.string.settings_summary_not_selected)
                        },
                    onClick = onOpenCurrentApp,
                    containerColor = cardColor,
                    testTag = "chat-settings-current-app",
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_theme_title),
                    summary = themeModeLabel(state.themeMode),
                    onClick = onOpenTheme,
                    containerColor = cardColor,
                    testTag = "chat-settings-theme",
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_language_title),
                    summary = languageLabel(state.selectedLanguageTag),
                    onClick = onOpenLanguage,
                    containerColor = cardColor,
                    testTag = "chat-settings-language",
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_chat_preferences_title),
                    summary =
                        stringResource(
                            R.string.settings_chat_preferences_summary,
                            if (state.showReasoningEntry) {
                                stringResource(R.string.settings_chat_preferences_reasoning_on)
                            } else {
                                stringResource(R.string.settings_chat_preferences_reasoning_off)
                            },
                            if (state.streamResponses) {
                                stringResource(R.string.settings_chat_preferences_stream_on)
                            } else {
                                stringResource(R.string.settings_chat_preferences_stream_off)
                            },
                        ),
                    onClick = onOpenChatPreferences,
                    containerColor = cardColor,
                    testTag = "chat-settings-chat-preferences",
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_storage_title),
                    summary = stringResource(R.string.settings_storage_summary),
                    onClick = onOpenCache,
                    containerColor = cardColor,
                    testTag = "chat-settings-cache",
                )
            }
            item {
                ChatSettingsSheetEntry(
                    title = stringResource(R.string.settings_about_title),
                    summary = stringResource(R.string.settings_about_summary),
                    onClick = onOpenAbout,
                    containerColor = cardColor,
                    testTag = "chat-settings-about",
                )
            }
        }
    }
}

@Composable
fun ChatPreferencesScreen(
    state: ChatPreferencesUiState,
    onShowReasoningEntryChange: (Boolean) -> Unit,
    onStreamResponsesChange: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_chat_preferences_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.settings_chat_preferences_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToggleSettingsCard(
            title = stringResource(R.string.settings_chat_preferences_reasoning_title),
            summary = stringResource(R.string.settings_chat_preferences_reasoning_description),
            checked = state.showReasoningEntry,
            onCheckedChange = onShowReasoningEntryChange,
            testTag = "chat-preferences-reasoning",
        )
        ToggleSettingsCard(
            title = stringResource(R.string.settings_chat_preferences_stream_title),
            summary = stringResource(R.string.settings_chat_preferences_stream_description),
            checked = state.streamResponses,
            onCheckedChange = onStreamResponsesChange,
            testTag = "chat-preferences-stream",
        )
    }
}

@Composable
fun CurrentAppSettingsScreen(
    state: CurrentAppSettingsUiState,
    onSwitchApp: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenCurrentApp: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_current_app_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    ) {
        if (state.connectionType == ConnectionType.OPENAI_COMPATIBLE) {
            Card(modifier = Modifier.fillMaxWidth().testTag("settings-current-app-summary")) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.model ?: stringResource(R.string.settings_summary_not_selected),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_current_app_openai_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.testTag("settings-current-app-refresh")) {
                Text(stringResource(R.string.settings_current_app_refresh))
            }
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("settings-current-app-error"),
                )
            }
            if (state.isLoading) {
                Text(stringResource(R.string.settings_current_app_loading))
            }
            if (state.selectedAppId != null) {
                OutlinedButton(onClick = onOpenCurrentApp, modifier = Modifier.testTag("settings-current-app-detail")) {
                    Text(stringResource(R.string.settings_current_app_open_detail))
                }
            }
            state.items.forEach { item ->
                Card(
                    onClick = { onSwitchApp(item.appId) },
                    modifier = Modifier.fillMaxWidth().testTag("settings-current-app-${item.appId}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = item.intro,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(if (item.isSelected) stringResource(R.string.settings_selected) else "")
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSettingsScreen(
    state: LanguageSettingsUiState,
    onLanguageTagChange: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_language_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
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
private fun ChatSettingsAccountHeader(
    titleColor: Color,
    subtitleColor: Color,
    avatarBackgroundColor: Color,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    onOpenConnection: () -> Unit,
) {
    val gradientColors =
        listOf(
            Color(0xFF4285F4),
            Color(0xFFEA4335),
            Color(0xFFFBBC05),
            Color(0xFF34A853),
        )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_sheet_account_email),
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
        )
        Box(
            modifier =
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(3.dp, Brush.sweepGradient(gradientColors), CircleShape)
                    .background(avatarBackgroundColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_sheet_account_avatar_initials),
                style = MaterialTheme.typography.headlineMedium,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_sheet_account_name),
                style = MaterialTheme.typography.headlineSmall,
                color = titleColor,
            )
            Text(
                text = stringResource(R.string.settings_sheet_account_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
            )
        }
        Card(
            onClick = onOpenConnection,
            colors = CardDefaults.cardColors(containerColor = buttonContainerColor),
        ) {
            Text(
                text = stringResource(R.string.settings_sheet_account_manage),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                color = buttonContentColor,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ChatSettingsSheetEntry(
    title: String,
    summary: String,
    onClick: () -> Unit,
    containerColor: Color,
    testTag: String,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.settings_sheet_item_chevron),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleSettingsCard(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { this.testTag = testTag },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun CacheSettingsScreen(
    state: CacheSettingsUiState,
    onClearCache: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SettingsPageColumn(
        title = stringResource(R.string.settings_storage_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
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
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsPageColumn(
        title = stringResource(R.string.settings_about_title),
        onBack = onBack,
        onOpenDrawer = onOpenDrawer,
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
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    verticalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            KoraGeminiTopBar(title = title, onOpenDrawer = onOpenDrawer)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            SettingsPageHeader(onBack = onBack)
            content()
        }
    }
}

@Composable
private fun SettingsPageHeader(
    onBack: (() -> Unit)?,
) {
    if (onBack != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back))
            }
        }
    }
}
