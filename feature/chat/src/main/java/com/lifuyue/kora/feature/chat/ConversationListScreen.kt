package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    val selectedConversation =
        remember(selectedChatId, uiState.items) {
            uiState.items.firstOrNull { it.chatId == selectedChatId }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("会话") },
                actions = {
                    TextButton(
                        onClick = { showClearAllDialog = true },
                        enabled = uiState.canClear,
                        modifier = Modifier.testTag(ChatTestTags.conversationClearAll),
                    ) {
                        Text("清空全部")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewConversation,
                modifier = Modifier.testTag(ChatTestTags.conversationFab),
            ) {
                Text("新建会话")
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                label = { Text("搜索会话") },
                placeholder = { Text("按标题或预览搜索") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(ChatTestTags.conversationSearch),
            )
            if (uiState.isEmpty) {
                EmptyConversationState(modifier = Modifier.fillMaxWidth())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    if (uiState.pinnedItems.isNotEmpty()) {
                        item(key = "pinned_header") {
                            ConversationSectionHeader(title = "置顶会话")
                        }
                        items(uiState.pinnedItems, key = { it.chatId }) { item ->
                            ConversationListCard(
                                item = item,
                                onClick = { onOpenConversation(item.chatId) },
                                onLongClick = { selectedChatId = item.chatId },
                            )
                        }
                    }
                    if (uiState.otherItems.isNotEmpty()) {
                        item(key = "all_header") {
                            ConversationSectionHeader(title = "全部会话")
                        }
                        items(uiState.otherItems, key = { it.chatId }) { item ->
                            ConversationListCard(
                                item = item,
                                onClick = { onOpenConversation(item.chatId) },
                                onLongClick = { selectedChatId = item.chatId },
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedConversation != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedChatId = null },
            modifier = Modifier.testTag(ChatTestTags.conversationActionsSheet),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "会话操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = selectedConversation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                SheetAction(
                    label = "重命名",
                    testTag = ChatTestTags.conversationActionRename,
                    onClick = {
                        renameChatId = selectedConversation.chatId
                        renameDraft = selectedConversation.title
                        selectedChatId = null
                    },
                )
                SheetAction(
                    label = if (selectedConversation.isPinned) "取消置顶" else "置顶",
                    testTag = ChatTestTags.conversationActionTogglePin,
                    onClick = {
                        onTogglePin(selectedConversation.chatId, !selectedConversation.isPinned)
                        selectedChatId = null
                    },
                )
                SheetAction(
                    label = "删除",
                    testTag = ChatTestTags.conversationActionDelete,
                    onClick = {
                        onDeleteConversation(selectedConversation.chatId)
                        selectedChatId = null
                    },
                    isDestructive = true,
                )
            }
        }
    }

    if (renameChatId != null) {
        AlertDialog(
            onDismissRequest = { renameChatId = null },
            title = { Text("重命名会话") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("会话标题") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(ChatTestTags.renameConversationInput),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chatId = renameChatId ?: return@TextButton
                        val trimmed = renameDraft.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenameConversation(chatId, trimmed)
                            renameChatId = null
                        }
                    },
                    enabled = renameDraft.trim().isNotEmpty(),
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameChatId = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清空所有会话？") },
            text = { Text("该操作会移除当前应用下的全部会话记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearConversations()
                        showClearAllDialog = false
                    },
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun EmptyConversationState(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "暂无会话",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "点击右下角的新建会话开始第一轮对话，历史记录会显示在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ConversationSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationListCard(
    item: ConversationListItemUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("${ChatTestTags.conversationItemPrefix}${item.chatId}")
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.isPinned) {
                    Text(
                        text = "置顶",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (item.preview.isNotBlank()) {
                Text(
                    text = item.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    testTag: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    ) {
        Text(
            text = label,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
