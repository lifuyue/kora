package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class ConversationOrganizerSheet {
    FolderFilter,
    TagFilter,
    MoveFolder,
    EditTags,
}

private enum class EditTarget {
    Folder,
    Tag,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationListScreen(
    uiState: ConversationListUiState,
    onQueryChanged: (String) -> Unit,
    onSelectFolderFilter: (String?) -> Unit,
    onSelectTagFilter: (String?) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onClearConversations: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onCreateTag: (String) -> Unit,
    onRenameTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onMoveConversationToFolder: (String, String?) -> Unit,
    onSetConversationTags: (String, List<String>) -> Unit,
) {
    var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var organizerChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var activeSheet by rememberSaveable { mutableStateOf<ConversationOrganizerSheet?>(null) }
    var createFolderDraft by rememberSaveable { mutableStateOf("") }
    var createTagDraft by rememberSaveable { mutableStateOf("") }
    var renameTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameTargetType by rememberSaveable { mutableStateOf<EditTarget?>(null) }
    var renameTargetDraft by rememberSaveable { mutableStateOf("") }
    var deleteFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTagId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTagIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var tagSearchQuery by rememberSaveable { mutableStateOf("") }
    val selectedConversation =
        remember(selectedChatId, uiState.items) {
            uiState.items.firstOrNull { it.chatId == selectedChatId }
        }
    val organizerConversation =
        remember(organizerChatId, uiState.items) {
            uiState.items.firstOrNull { it.chatId == organizerChatId }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedActionChip(
                    label = uiState.selectedFolderName,
                    modifier = Modifier.weight(1f).testTag(ChatTestTags.conversationFolderFilter),
                    onClick = { activeSheet = ConversationOrganizerSheet.FolderFilter },
                )
                OutlinedActionChip(
                    label = uiState.selectedTagName,
                    modifier = Modifier.weight(1f).testTag(ChatTestTags.conversationTagFilter),
                    onClick = { activeSheet = ConversationOrganizerSheet.TagFilter },
                )
            }
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
                    label = "移动到文件夹",
                    testTag = ChatTestTags.conversationActionMoveFolder,
                    onClick = {
                        organizerChatId = selectedConversation.chatId
                        selectedChatId = null
                        activeSheet = ConversationOrganizerSheet.MoveFolder
                    },
                )
                SheetAction(
                    label = "编辑标签",
                    testTag = ChatTestTags.conversationActionEditTags,
                    onClick = {
                        organizerChatId = selectedConversation.chatId
                        editingTagIds = selectedConversation.tags.map { it.tagId }
                        tagSearchQuery = ""
                        selectedChatId = null
                        activeSheet = ConversationOrganizerSheet.EditTags
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

    if (activeSheet != null) {
        when (activeSheet) {
            ConversationOrganizerSheet.FolderFilter,
            ConversationOrganizerSheet.MoveFolder,
            -> ModalBottomSheet(
                onDismissRequest = {
                    activeSheet = null
                    organizerChatId = null
                },
                modifier = Modifier.testTag(ChatTestTags.conversationFolderSheet),
            ) {
                FolderSheetContent(
                    uiState = uiState,
                    currentFolderId = organizerConversation?.folderId,
                    activeSheet = checkNotNull(activeSheet),
                    createDraft = createFolderDraft,
                    onCreateDraftChanged = { createFolderDraft = it },
                    onCreateFolder = {
                        onCreateFolder(createFolderDraft)
                        createFolderDraft = ""
                    },
                    onSelectFolder = { folderId ->
                        val conversation = organizerConversation
                        if (activeSheet == ConversationOrganizerSheet.MoveFolder && conversation != null) {
                            onMoveConversationToFolder(conversation.chatId, folderId)
                            organizerChatId = null
                        } else {
                            onSelectFolderFilter(folderId)
                        }
                        activeSheet = null
                    },
                    onRenameFolder = { folder ->
                        renameTargetId = folder.folderId
                        renameTargetType = EditTarget.Folder
                        renameTargetDraft = folder.name
                    },
                    onDeleteFolder = { folderId ->
                        deleteFolderId = folderId
                    },
                )
            }

            ConversationOrganizerSheet.TagFilter,
            ConversationOrganizerSheet.EditTags,
            -> ModalBottomSheet(
                onDismissRequest = {
                    activeSheet = null
                    organizerChatId = null
                },
                modifier = Modifier.testTag(ChatTestTags.conversationTagSheet),
            ) {
                TagSheetContent(
                    uiState = uiState,
                    activeSheet = checkNotNull(activeSheet),
                    createDraft = createTagDraft,
                    tagSearchQuery = tagSearchQuery,
                    selectedTagIds = editingTagIds,
                    onCreateDraftChanged = { createTagDraft = it },
                    onTagSearchQueryChanged = { tagSearchQuery = it },
                    onCreateTag = {
                        onCreateTag(createTagDraft)
                        createTagDraft = ""
                    },
                    onSelectFilterTag = { tagId ->
                        onSelectTagFilter(tagId)
                        activeSheet = null
                    },
                    onToggleConversationTag = { tagId ->
                        editingTagIds =
                            if (editingTagIds.contains(tagId)) {
                                editingTagIds - tagId
                            } else {
                                editingTagIds + tagId
                            }
                    },
                    onSaveConversationTags = {
                        val conversation = organizerConversation
                        if (conversation != null) {
                            onSetConversationTags(conversation.chatId, editingTagIds)
                        }
                        organizerChatId = null
                        activeSheet = null
                    },
                    onRenameTag = { tag ->
                        renameTargetId = tag.tagId
                        renameTargetType = EditTarget.Tag
                        renameTargetDraft = tag.name
                    },
                    onDeleteTag = { tagId ->
                        deleteTagId = tagId
                    },
                )
            }

            null -> Unit
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

    if (renameTargetId != null && renameTargetType != null) {
        AlertDialog(
            onDismissRequest = {
                renameTargetId = null
                renameTargetType = null
            },
            title = {
                Text(if (renameTargetType == EditTarget.Folder) "重命名文件夹" else "重命名标签")
            },
            text = {
                OutlinedTextField(
                    value = renameTargetDraft,
                    onValueChange = { renameTargetDraft = it },
                    label = { Text(if (renameTargetType == EditTarget.Folder) "文件夹名称" else "标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = renameTargetId ?: return@TextButton
                        val trimmed = renameTargetDraft.trim()
                        if (trimmed.isEmpty()) {
                            return@TextButton
                        }
                        if (renameTargetType == EditTarget.Folder) {
                            onRenameFolder(targetId, trimmed)
                        } else {
                            onRenameTag(targetId, trimmed)
                        }
                        renameTargetId = null
                        renameTargetType = null
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameTargetId = null
                        renameTargetType = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (deleteFolderId != null) {
        AlertDialog(
            onDismissRequest = { deleteFolderId = null },
            title = { Text("删除文件夹？") },
            text = { Text("会保留其中会话，只移除文件夹关系。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(checkNotNull(deleteFolderId))
                        deleteFolderId = null
                    },
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderId = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (deleteTagId != null) {
        AlertDialog(
            onDismissRequest = { deleteTagId = null },
            title = { Text("删除标签？") },
            text = { Text("会保留原会话，只移除该标签关系。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag(checkNotNull(deleteTagId))
                        deleteTagId = null
                    },
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTagId = null }) {
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
private fun FolderSheetContent(
    uiState: ConversationListUiState,
    currentFolderId: String?,
    activeSheet: ConversationOrganizerSheet,
    createDraft: String,
    onCreateDraftChanged: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onSelectFolder: (String?) -> Unit,
    onRenameFolder: (ConversationFolderUiModel) -> Unit,
    onDeleteFolder: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Text(
            if (activeSheet == ConversationOrganizerSheet.MoveFolder) "移动到文件夹" else "文件夹",
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = createDraft,
                onValueChange = onCreateDraftChanged,
                label = { Text("新建文件夹") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onCreateFolder, enabled = createDraft.trim().isNotEmpty()) {
                Text("添加")
            }
        }
        SheetActionRow(
            title = if (activeSheet == ConversationOrganizerSheet.MoveFolder) "移出文件夹" else "全部文件夹",
            subtitle = "",
            selected = if (activeSheet == ConversationOrganizerSheet.MoveFolder) currentFolderId == null else uiState.selectedFolderId == null,
            onPrimaryAction = { onSelectFolder(null) },
            primaryLabel = if (activeSheet == ConversationOrganizerSheet.MoveFolder) "移出" else "查看",
        )
        uiState.folders.forEach { folder ->
            SheetActionRow(
                title = folder.name,
                subtitle = "",
                selected = if (activeSheet == ConversationOrganizerSheet.MoveFolder) currentFolderId == folder.folderId else uiState.selectedFolderId == folder.folderId,
                onPrimaryAction = { onSelectFolder(folder.folderId) },
                primaryLabel = if (activeSheet == ConversationOrganizerSheet.MoveFolder) "移动" else "查看",
                onRename = { onRenameFolder(folder) },
                onDelete = { onDeleteFolder(folder.folderId) },
            )
        }
    }
}

@Composable
private fun TagSheetContent(
    uiState: ConversationListUiState,
    activeSheet: ConversationOrganizerSheet,
    createDraft: String,
    tagSearchQuery: String,
    selectedTagIds: List<String>,
    onCreateDraftChanged: (String) -> Unit,
    onTagSearchQueryChanged: (String) -> Unit,
    onCreateTag: () -> Unit,
    onSelectFilterTag: (String?) -> Unit,
    onToggleConversationTag: (String) -> Unit,
    onSaveConversationTags: () -> Unit,
    onRenameTag: (ConversationTagUiModel) -> Unit,
    onDeleteTag: (String) -> Unit,
) {
    val visibleTags =
        uiState.tags.filter { tag ->
            tagSearchQuery.isBlank() || tag.name.contains(tagSearchQuery, ignoreCase = true)
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Text(
            if (activeSheet == ConversationOrganizerSheet.EditTags) "编辑标签" else "标签",
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = createDraft,
                onValueChange = onCreateDraftChanged,
                label = { Text("新建标签") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onCreateTag, enabled = createDraft.trim().isNotEmpty()) {
                Text("添加")
            }
        }
        OutlinedTextField(
            value = tagSearchQuery,
            onValueChange = onTagSearchQueryChanged,
            label = { Text("搜索标签") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (activeSheet == ConversationOrganizerSheet.TagFilter) {
            SheetActionRow(
                title = "全部标签",
                subtitle = "",
                selected = uiState.selectedTagId == null,
                onPrimaryAction = { onSelectFilterTag(null) },
                primaryLabel = "查看",
            )
        }
        visibleTags.forEach { tag ->
            if (activeSheet == ConversationOrganizerSheet.EditTags) {
                TagSelectionRow(
                    tag = tag,
                    checked = selectedTagIds.contains(tag.tagId),
                    onCheckedChange = { onToggleConversationTag(tag.tagId) },
                    onRename = { onRenameTag(tag) },
                    onDelete = { onDeleteTag(tag.tagId) },
                )
            } else {
                SheetActionRow(
                    title = tag.name,
                    subtitle = tag.colorToken,
                    selected = uiState.selectedTagId == tag.tagId,
                    onPrimaryAction = { onSelectFilterTag(tag.tagId) },
                    primaryLabel = "查看",
                    onRename = { onRenameTag(tag) },
                    onDelete = { onDeleteTag(tag.tagId) },
                )
            }
        }
        if (activeSheet == ConversationOrganizerSheet.EditTags) {
            Button(onClick = onSaveConversationTags, modifier = Modifier.fillMaxWidth()) {
                Text("保存标签")
            }
        }
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
            if (!item.folderName.isNullOrBlank() || item.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item.folderName?.takeIf { it.isNotBlank() }?.let { folderName ->
                        MetaChip(
                            text = folderName,
                            background = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    item.tags.take(2).forEach { tag ->
                        MetaChip(
                            text = tag.name,
                            background = tag.backgroundColor(),
                            content = tag.contentColor(),
                        )
                    }
                    if (item.tags.size > 2) {
                        Text(
                            text = "+${item.tags.size - 2}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSelectionRow(
    tag: ConversationTagUiModel,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
            MetaChip(
                text = tag.name,
                background = tag.backgroundColor(),
                content = tag.contentColor(),
            )
        }
        TextButton(onClick = onRename) { Text("重命名") }
        TextButton(onClick = onDelete) { Text("删除") }
    }
}

@Composable
private fun SheetActionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onPrimaryAction: () -> Unit,
    primaryLabel: String,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            if (selected) {
                Text("当前", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onPrimaryAction) { Text(primaryLabel) }
            onRename?.let { TextButton(onClick = it) { Text("重命名") } }
            onDelete?.let { TextButton(onClick = it) { Text("删除") } }
        }
        HorizontalDivider()
    }
}

@Composable
private fun OutlinedActionChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MetaChip(
    text: String,
    background: Color,
    content: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier =
            Modifier
                .background(background, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ConversationTagUiModel.backgroundColor(): Color =
    when (colorToken) {
        "amber" -> MaterialTheme.colorScheme.tertiaryContainer
        "mint" -> MaterialTheme.colorScheme.secondaryContainer
        "sky" -> MaterialTheme.colorScheme.primaryContainer
        "coral" -> MaterialTheme.colorScheme.errorContainer
        "indigo" -> MaterialTheme.colorScheme.primaryContainer
        "rose" -> MaterialTheme.colorScheme.errorContainer
        "teal" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

@Composable
private fun ConversationTagUiModel.contentColor(): Color =
    when (colorToken) {
        "amber" -> MaterialTheme.colorScheme.onTertiaryContainer
        "mint" -> MaterialTheme.colorScheme.onSecondaryContainer
        "sky" -> MaterialTheme.colorScheme.onPrimaryContainer
        "coral" -> MaterialTheme.colorScheme.onErrorContainer
        "indigo" -> MaterialTheme.colorScheme.onPrimaryContainer
        "rose" -> MaterialTheme.colorScheme.onErrorContainer
        "teal" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
