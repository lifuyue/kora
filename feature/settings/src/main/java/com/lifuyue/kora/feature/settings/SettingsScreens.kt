package com.lifuyue.kora.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
private fun ConnectionTestResultCard(
    result: ConnectionTestResult,
) {
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
    onOpenTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        SettingsSection(
            title = "连接与账号",
            content = {
                SettingsEntry(title = "连接配置", summary = state.connectionSummary, onClick = onOpenConnection)
                SettingsEntry(title = "当前 App", summary = state.selectedAppSummary, onClick = null)
            },
        )
        SettingsSection(
            title = "外观与主题",
            content = {
                SettingsEntry(title = "主题外观", summary = state.themeSummary, onClick = onOpenTheme)
            },
        )
        SettingsSection(
            title = "常用与信息",
            content = {
                SettingsEntry(title = "聊天偏好", summary = "将在后续里程碑接入", onClick = null)
                SettingsEntry(title = "语言", summary = "跟随系统", onClick = null)
                SettingsEntry(title = "缓存", summary = "清理入口待接入", onClick = null)
                SettingsEntry(title = "关于", summary = "Kora Android Client", onClick = null)
            },
        )
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
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.selectable(selected = false, onClick = onClick)
                    },
                ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
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

private fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.OLED_DARK -> "OLED 深色"
    }
