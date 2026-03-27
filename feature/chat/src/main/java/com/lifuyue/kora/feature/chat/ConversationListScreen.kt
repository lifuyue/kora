package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    uiState: ConversationListUiState,
    onQueryChanged: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onClearConversations: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("会话") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewConversation) {
                Text("新建")
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                label = { Text("搜索会话") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onClearConversations, enabled = uiState.items.isNotEmpty()) {
                Text("清空全部")
            }
            if (uiState.isEmpty) {
                Text(
                    text = "暂无会话，点击右下角开始新对话。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.items, key = { it.chatId }) { item ->
                        Card(onClick = { onOpenConversation(item.chatId) }) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(12.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                                    if (item.isPinned) {
                                        Text("置顶", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (item.preview.isNotBlank()) {
                                    Text(item.preview, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onTogglePin(item.chatId, !item.isPinned) }) {
                                        Text(if (item.isPinned) "取消置顶" else "置顶")
                                    }
                                    TextButton(onClick = { onRenameConversation(item.chatId, "${item.title}*") }) {
                                        Text("重命名")
                                    }
                                    TextButton(onClick = { onDeleteConversation(item.chatId) }) {
                                        Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
