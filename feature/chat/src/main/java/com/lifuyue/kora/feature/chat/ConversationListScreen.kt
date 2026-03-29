package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ui.KoraWorkspaceHeroCard

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
    onToggleShowArchived: (Boolean) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onSetArchived: (String, Boolean) -> Unit,
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
                title = { Text(stringResource(R.string.conversation_list_title)) },
                actions = {
                    TextButton(
                        onClick = { showClearAllDialog = true },
                        enabled = uiState.canClear,
                        modifier = Modifier.testTag(ChatTestTags.CONVERSATION_CLEAR_ALL),
                    ) {
                        Text(stringResource(R.string.conversation_list_clear_all))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewConversation,
                modifier = Modifier.testTag(ChatTestTags.CONVERSATION_FAB),
            ) {
                Text(stringResource(R.string.conversation_list_new_conversation))
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
            KoraWorkspaceHeroCard(
                title = stringResource(R.string.conversation_list_workspace_title),
                subtitle =
                    stringResource(
                        R.string.conversation_list_workspace_summary,
                        uiState.items.size,
                        uiState.pinnedItems.size,
                    ),
                eyebrow = stringResource(R.string.conversation_list_workspace_eyebrow),
                meta = stringResource(R.string.conversation_list_workspace_meta),
                modifier = Modifier.testTag("conversation_workspace_summary"),
            )
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.conversation_list_search_label)) },
                placeholder = { Text(stringResource(R.string.conversation_list_search_placeholder)) },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(ChatTestTags.CONVERSATION_SEARCH),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedActionChip(
                    label = uiState.selectedFolderName ?: stringResource(R.string.conversation_list_all_folders),
                    modifier = Modifier.weight(1f).testTag(ChatTestTags.CONVERSATION_FOLDER_FILTER),
                    onClick = { activeSheet = ConversationOrganizerSheet.FolderFilter },
                )
                OutlinedActionChip(
                    label = uiState.selectedTagName ?: stringResource(R.string.conversation_list_all_tags),
                    modifier = Modifier.weight(1f).testTag(ChatTestTags.CONVERSATION_TAG_FILTER),
                    onClick = { activeSheet = ConversationOrganizerSheet.TagFilter },
                )
            }
            OutlinedActionChip(
                label =
                    appString(
                        if (uiState.showArchived) {
                            "conversation_list_filter_archived"
                        } else {
                            "conversation_list_filter_active"
                        },
                    ),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onToggleShowArchived(!uiState.showArchived) },
            )
            if (uiState.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyConversationState(modifier = Modifier.fillMaxWidth())
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.CONVERSATION_LIST),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    if (uiState.pinnedItems.isNotEmpty()) {
                        item(key = "pinned_header") {
                            ConversationSectionHeader(title = stringResource(R.string.conversation_list_pinned_section))
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
                            ConversationSectionHeader(title = stringResource(R.string.conversation_list_all_section))
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
            modifier = Modifier.testTag(ChatTestTags.CONVERSATION_ACTIONS_SHEET),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.conversation_list_action_sheet_title),
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
                    label = stringResource(R.string.conversation_list_action_rename),
                    testTag = ChatTestTags.CONVERSATION_ACTION_RENAME,
                    onClick = {
                        renameChatId = selectedConversation.chatId
                        renameDraft = selectedConversation.title
                        selectedChatId = null
                    },
                )
                SheetAction(
                    label = stringResource(R.string.conversation_list_action_move_to_folder),
                    testTag = ChatTestTags.CONVERSATION_ACTION_MOVE_FOLDER,
                    onClick = {
                        organizerChatId = selectedConversation.chatId
                        selectedChatId = null
                        activeSheet = ConversationOrganizerSheet.MoveFolder
                    },
                )
                SheetAction(
                    label = stringResource(R.string.conversation_list_action_edit_tags),
                    testTag = ChatTestTags.CONVERSATION_ACTION_EDIT_TAGS,
                    onClick = {
                        organizerChatId = selectedConversation.chatId
                        editingTagIds = selectedConversation.tags.map { it.tagId }
                        tagSearchQuery = ""
                        selectedChatId = null
                        activeSheet = ConversationOrganizerSheet.EditTags
                    },
                )
                SheetAction(
                    label =
                        stringResource(
                            if (selectedConversation.isPinned) {
                                R.string.conversation_list_action_unpin
                            } else {
                                R.string.conversation_list_action_pin
                            },
                        ),
                    testTag = ChatTestTags.CONVERSATION_ACTION_TOGGLE_PIN,
                    onClick = {
                        onTogglePin(selectedConversation.chatId, !selectedConversation.isPinned)
                        selectedChatId = null
                    },
                )
                SheetAction(
                    label =
                        appString(
                            if (selectedConversation.isArchived) {
                                "conversation_list_action_unarchive"
                            } else {
                                "conversation_list_action_archive"
                            },
                        ),
                    testTag = ChatTestTags.CONVERSATION_ACTION_ARCHIVE,
                    onClick = {
                        onSetArchived(selectedConversation.chatId, !selectedConversation.isArchived)
                        selectedChatId = null
                    },
                )
                SheetAction(
                    label = stringResource(R.string.conversation_list_action_delete),
                    testTag = ChatTestTags.CONVERSATION_ACTION_DELETE,
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
            ->
                ModalBottomSheet(
                    onDismissRequest = {
                        activeSheet = null
                        organizerChatId = null
                    },
                    modifier = Modifier.testTag(ChatTestTags.CONVERSATION_FOLDER_SHEET),
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
            ->
                ModalBottomSheet(
                    onDismissRequest = {
                        activeSheet = null
                        organizerChatId = null
                    },
                    modifier = Modifier.testTag(ChatTestTags.CONVERSATION_TAG_SHEET),
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
            title = { Text(stringResource(R.string.conversation_list_dialog_rename_conversation_title)) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text(stringResource(R.string.conversation_list_dialog_conversation_title_label)) },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(ChatTestTags.RENAME_CONVERSATION_INPUT),
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
                    Text(stringResource(R.string.conversation_list_action_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameChatId = null }) {
                    Text(stringResource(R.string.conversation_list_cancel))
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
                Text(
                    stringResource(
                        if (renameTargetType == EditTarget.Folder) {
                            R.string.conversation_list_dialog_rename_folder_title
                        } else {
                            R.string.conversation_list_dialog_rename_tag_title
                        },
                    ),
                )
            },
            text = {
                OutlinedTextField(
                    value = renameTargetDraft,
                    onValueChange = { renameTargetDraft = it },
                    label = {
                        Text(
                            stringResource(
                                if (renameTargetType == EditTarget.Folder) {
                                    R.string.conversation_list_dialog_folder_name_label
                                } else {
                                    R.string.conversation_list_dialog_tag_name_label
                                },
                            ),
                        )
                    },
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
                    Text(stringResource(R.string.conversation_list_action_rename))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameTargetId = null
                        renameTargetType = null
                    },
                ) {
                    Text(stringResource(R.string.conversation_list_cancel))
                }
            },
        )
    }

    if (deleteFolderId != null) {
        AlertDialog(
            onDismissRequest = { deleteFolderId = null },
            title = { Text(stringResource(R.string.conversation_list_dialog_delete_folder_title)) },
            text = { Text(stringResource(R.string.conversation_list_dialog_delete_folder_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(checkNotNull(deleteFolderId))
                        deleteFolderId = null
                    },
                ) {
                    Text(stringResource(R.string.conversation_list_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderId = null }) {
                    Text(stringResource(R.string.conversation_list_cancel))
                }
            },
        )
    }

    if (deleteTagId != null) {
        AlertDialog(
            onDismissRequest = { deleteTagId = null },
            title = { Text(stringResource(R.string.conversation_list_dialog_delete_tag_title)) },
            text = { Text(stringResource(R.string.conversation_list_dialog_delete_tag_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag(checkNotNull(deleteTagId))
                        deleteTagId = null
                    },
                ) {
                    Text(stringResource(R.string.conversation_list_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTagId = null }) {
                    Text(stringResource(R.string.conversation_list_cancel))
                }
            },
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.conversation_list_dialog_clear_all_title)) },
            text = { Text(stringResource(R.string.conversation_list_dialog_clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearConversations()
                        showClearAllDialog = false
                    },
                ) {
                    Text(stringResource(R.string.conversation_list_confirm_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.conversation_list_cancel))
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
            stringResource(
                if (activeSheet == ConversationOrganizerSheet.MoveFolder) {
                    R.string.conversation_list_move_folder_sheet_title
                } else {
                    R.string.conversation_list_folder_sheet_title
                },
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = createDraft,
                onValueChange = onCreateDraftChanged,
                label = { Text(stringResource(R.string.conversation_list_new_folder_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onCreateFolder, enabled = createDraft.trim().isNotEmpty()) {
                Text(stringResource(R.string.conversation_list_add))
            }
        }
        SheetActionRow(
            title =
                stringResource(
                    if (activeSheet == ConversationOrganizerSheet.MoveFolder) {
                        R.string.conversation_list_remove_from_folder
                    } else {
                        R.string.conversation_list_all_folders
                    },
                ),
            subtitle = "",
            selected =
                if (activeSheet == ConversationOrganizerSheet.MoveFolder) {
                    currentFolderId == null
                } else {
                    uiState.selectedFolderId == null
                },
            onPrimaryAction = { onSelectFolder(null) },
            primaryLabel =
                stringResource(
                    if (activeSheet == ConversationOrganizerSheet.MoveFolder) {
                        R.string.conversation_list_remove
                    } else {
                        R.string.conversation_list_view
                    },
                ),
        )
        uiState.folders.forEach { folder ->
            SheetActionRow(
                title = folder.name,
                subtitle = "",
                selected =
                    if (activeSheet == ConversationOrganizerSheet.MoveFolder) {
                        currentFolderId == folder.folderId
                    } else {
                        uiState.selectedFolderId == folder.folderId
                    },
                onPrimaryAction = { onSelectFolder(folder.folderId) },
                primaryLabel =
                    stringResource(
                        if (activeSheet == ConversationOrganizerSheet.MoveFolder) {
                            R.string.conversation_list_move
                        } else {
                            R.string.conversation_list_view
                        },
                    ),
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
            stringResource(
                if (activeSheet == ConversationOrganizerSheet.EditTags) {
                    R.string.conversation_list_edit_tags_title
                } else {
                    R.string.conversation_list_tag_sheet_title
                },
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = createDraft,
                onValueChange = onCreateDraftChanged,
                label = { Text(stringResource(R.string.conversation_list_new_tag_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onCreateTag, enabled = createDraft.trim().isNotEmpty()) {
                Text(stringResource(R.string.conversation_list_add))
            }
        }
        OutlinedTextField(
            value = tagSearchQuery,
            onValueChange = onTagSearchQueryChanged,
            label = { Text(stringResource(R.string.conversation_list_search_tags_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (activeSheet == ConversationOrganizerSheet.TagFilter) {
            SheetActionRow(
                title = stringResource(R.string.conversation_list_all_tags),
                subtitle = "",
                selected = uiState.selectedTagId == null,
                onPrimaryAction = { onSelectFilterTag(null) },
                primaryLabel = stringResource(R.string.conversation_list_view),
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
                    primaryLabel = stringResource(R.string.conversation_list_view),
                    onRename = { onRenameTag(tag) },
                    onDelete = { onDeleteTag(tag.tagId) },
                )
            }
        }
        if (activeSheet == ConversationOrganizerSheet.EditTags) {
            Button(onClick = onSaveConversationTags, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.conversation_list_save_tags))
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
                text = stringResource(R.string.conversation_list_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.conversation_list_empty_body),
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
                .testTag("${ChatTestTags.CONVERSATION_ITEM_PREFIX}${item.chatId}")
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
                        text = stringResource(R.string.conversation_list_pinned_badge),
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
        TextButton(onClick = onRename) { Text(stringResource(R.string.conversation_list_action_rename)) }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.conversation_list_action_delete)) }
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
                Text(
                    stringResource(R.string.conversation_list_current_badge),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(onClick = onPrimaryAction) { Text(primaryLabel) }
            onRename?.let { TextButton(onClick = it) { Text(stringResource(R.string.conversation_list_action_rename)) } }
            onDelete?.let { TextButton(onClick = it) { Text(stringResource(R.string.conversation_list_action_delete)) } }
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
