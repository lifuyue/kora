package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    uiState: AppDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAnalytics: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App 能力") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = onRefresh) { Text("刷新") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(uiState.appName.ifBlank { uiState.appId }, style = MaterialTheme.typography.headlineSmall)
                    if (uiState.type.isNotBlank()) {
                        Text(uiState.type, style = MaterialTheme.typography.labelLarge)
                    }
                    if (uiState.intro.isNotBlank()) {
                        Text(uiState.intro, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        uiState.welcomeText ?: "当前 App 未提供欢迎语",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (uiState.showAnalyticsEntry) {
                        TextButton(onClick = onOpenAnalytics) {
                            Text("查看统计")
                        }
                    }
                }
            }
            when {
                uiState.isLoading -> {
                    Text("加载中...", style = MaterialTheme.typography.bodyMedium)
                }
                uiState.errorMessage != null -> {
                    Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                }
                uiState.sections.isEmpty() -> {
                    Text("当前 App 未暴露额外客户端配置。", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    uiState.sections.forEach { section ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(section.title, style = MaterialTheme.typography.titleMedium)
                                section.items.forEach { item ->
                                    Text(item, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
