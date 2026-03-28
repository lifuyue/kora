package com.lifuyue.kora.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ThemeMode

@Composable
fun ConnectionConfigScreen(
    state: ConnectionConfigUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .semantics { testTag = "settings-overview-scroll" }
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("连接配置", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth().semantics { testTag = "server-url" },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
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
                Text(if (state.isTesting) "测试中..." else "测试连接")
            }
            Button(
                onClick = onSave,
                enabled = state.canSave && !state.isSaving,
                modifier = Modifier.semantics { testTag = "save-connection" },
            ) {
                Text(if (state.isSaving) "保存中..." else "保存")
            }
            Button(
                onClick = onClear,
                modifier = Modifier.semantics { testTag = "clear-connection" },
            ) {
                Text("清除")
            }
        }
        state.testResult?.let {
            ConnectionTestResultCard(result = it)
        }
        if (state.apiKeyMaskedSummary.isNotBlank()) {
            Text(
                text = "当前密钥摘要：${state.apiKeyMaskedSummary}",
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
                "连接成功，发现 ${result.apps.size} 个 App，耗时 ${result.latencyMs}ms"
            is ConnectionTestResult.ValidationError -> "校验失败：${result.reason}"
            is ConnectionTestResult.AuthError -> "认证失败：${result.error.message}"
            is ConnectionTestResult.NetworkFailure -> "网络错误：${result.message}"
            is ConnectionTestResult.ServerError -> "服务端错误：${result.error.message}"
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
    onOpenLanguage: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().semantics { testTag = "settings-overview-scroll" },
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("设置", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            SettingsSection(
                title = "连接与账号",
                content = {
                    SettingsEntry(
                        title = "连接配置",
                        summary = state.connectionSummary,
                        onClick = onOpenConnection,
                        testTag = "settings-connection",
                    )
                    SettingsEntry(
                        title = "当前 App",
                        summary = state.selectedAppSummary,
                        onClick = onOpenCurrentApp,
                        testTag = "settings-current-app",
                    )
                },
            )
        }
        item {
            SettingsSection(
                title = "外观与主题",
                content = {
                    SettingsEntry(
                        title = "主题外观",
                        summary = state.themeSummary,
                        onClick = onOpenTheme,
                        testTag = "settings-theme",
                    )
                },
            )
        }
        item {
            SettingsSection(
                title = "常用与信息",
                content = {
                    SettingsEntry(
                        title = "聊天偏好",
                        summary = "流式、滚动、字号与引用",
                        onClick = onOpenChatPreferences,
                        testTag = "settings-chat-preferences",
                    )
                    SettingsEntry(
                        title = "语言",
                        summary = "跟随系统",
                        onClick = onOpenLanguage,
                        testTag = "settings-language",
                    )
                    SettingsEntry(
                        title = "缓存",
                        summary = "查看并清理临时缓存",
                        onClick = onOpenCache,
                        testTag = "settings-cache",
                    )
                    SettingsEntry(
                        title = "关于",
                        summary = "Kora Android Client",
                        onClick = onOpenAbout,
                        testTag = "settings-about",
                    )
                },
            )
        }
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("主题外观", style = MaterialTheme.typography.headlineMedium)
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = state.themeMode == mode, onClick = { onThemeModeChange(mode) }),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(mode.label())
                Text(if (state.themeMode == mode) "已选中" else "")
            }
            HorizontalDivider()
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("动态取色")
            Switch(
                checked = state.dynamicColorEnabled,
                onCheckedChange = onDynamicColorChange,
                modifier = Modifier.semantics { testTag = "dynamic-color" },
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("OLED 深色增强")
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("聊天偏好", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("启用流式输出")
            Switch(
                checked = state.streamEnabled,
                onCheckedChange = onStreamEnabledChange,
                modifier = Modifier.semantics { testTag = "chat-pref-stream" },
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("自动滚动到底部")
            Switch(
                checked = state.autoScroll,
                onCheckedChange = onAutoScrollChange,
                modifier = Modifier.semantics { testTag = "chat-pref-auto-scroll" },
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("默认显示引用")
            Switch(
                checked = state.showCitationsByDefault,
                onCheckedChange = onShowCitationsChange,
                modifier = Modifier.semantics { testTag = "chat-pref-citations" },
            )
        }
        Text("${"%.0f".format(state.fontSizeScale * 100)}%")
        Slider(
            value = state.fontSizeScale,
            onValueChange = onFontSizeScaleChange,
            valueRange = 0.8f..1.4f,
            steps = 5,
        )
    }
}

@Composable
fun LanguageSettingsScreen(
    state: LanguageSettingsUiState,
    onLanguageTagChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("语言", style = MaterialTheme.typography.headlineMedium)
        LanguageOption(
            tag = "language-option-system",
            label = "跟随系统",
            selected = state.selectedLanguageTag == null,
            onClick = { onLanguageTagChange(null) },
        )
        LanguageOption(
            tag = "language-option-zh-CN",
            label = "简体中文",
            selected = state.selectedLanguageTag == "zh-CN",
            onClick = { onLanguageTagChange("zh-CN") },
        )
        LanguageOption(
            tag = "language-option-en",
            label = "English",
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
            Text(if (selected) "已选中" else "")
        }
    }
}

@Composable
fun CacheSettingsScreen(
    state: CacheSettingsUiState,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("缓存", style = MaterialTheme.typography.headlineMedium)
        Text("当前缓存大小")
        Text(state.cacheSizeLabel, style = MaterialTheme.typography.headlineSmall)
        Button(
            onClick = onClearCache,
            enabled = !state.isClearing,
            modifier = Modifier.semantics { testTag = "clear-cache" },
        ) {
            Text(if (state.isClearing) "清理中..." else "清理缓存")
        }
    }
}

@Composable
fun AboutScreen(
    state: AboutUiState,
    onOpenFeedback: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("关于", style = MaterialTheme.typography.headlineMedium)
        Text("Kora Android Client", style = MaterialTheme.typography.titleLarge)
        Text(state.versionName, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = onOpenFeedback,
            modifier = Modifier.semantics { testTag = "about-feedback" },
        ) {
            Text("反馈")
        }
        OutlinedButton(
            onClick = onOpenLicenses,
            modifier = Modifier.semantics { testTag = "about-licenses" },
        ) {
            Text("开源许可")
        }
    }
}

private fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.OLED_DARK -> "OLED 深色"
    }
