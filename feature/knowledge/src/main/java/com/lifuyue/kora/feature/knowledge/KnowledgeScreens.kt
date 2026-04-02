package com.lifuyue.kora.feature.knowledge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ui.KoraGeminiTopBar
import com.lifuyue.kora.core.common.ui.KoraSectionCard
import com.lifuyue.kora.core.common.ui.KoraWorkspaceHeroCard
import com.lifuyue.kora.core.common.ui.KoraWorkspaceSectionTitle
import com.lifuyue.kora.core.common.ui.WorkspaceMetaPill
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeOverviewScreen(
    state: KnowledgeOverviewUiState,
    onOpenDatasets: () -> Unit,
    onOpenRecentDataset: (String) -> Unit,
    onReturnToChat: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            KoraGeminiTopBar(
                title = stringResource(R.string.knowledge_overview_title),
                onOpenDrawer = onOpenDrawer,
            )
        },
    ) { innerPadding ->
        KnowledgePageContainer(innerPadding = innerPadding) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KoraWorkspaceHeroCard(
                    title = stringResource(R.string.knowledge_overview_workspace_title),
                    subtitle =
                        stringResource(
                            R.string.knowledge_overview_current_app,
                            state.selectedAppId ?: stringResource(R.string.knowledge_overview_no_app_selected),
                        ),
                    eyebrow = stringResource(R.string.knowledge_overview_workspace_eyebrow),
                    meta = stringResource(R.string.knowledge_overview_workspace_meta),
                    modifier = Modifier.testTag("knowledge_overview_summary_card"),
                )
                KoraSectionCard {
                    KoraWorkspaceSectionTitle(
                        title =
                            pluralStringResource(
                                R.plurals.knowledge_overview_dataset_count,
                                state.datasetCount,
                                state.datasetCount,
                            ),
                        supportingText = stringResource(R.string.knowledge_overview_open_datasets),
                    )
                    if (onReturnToChat != null) {
                        TextButton(onClick = onReturnToChat) {
                            Text(stringResource(R.string.knowledge_return_to_chat))
                        }
                    }
                    Button(onClick = onOpenDatasets) { Text(stringResource(R.string.knowledge_overview_open_datasets)) }
                }
                state.recentDatasets.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenRecentDataset(item.datasetId) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(item.intro.ifBlank { knowledgeTypeLabel(item.type) }, style = MaterialTheme.typography.bodyMedium)
                            Text(datasetSummaryLabel(item.type, item.status), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalKnowledgeOverviewScreen(
    state: KnowledgeOverviewUiState,
    onOpenLibrary: () -> Unit,
    onOpenRecentDocument: (String) -> Unit,
    onReturnToChat: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            KoraGeminiTopBar(
                title = stringResource(R.string.knowledge_local_overview_title),
                onOpenDrawer = onOpenDrawer,
            )
        },
    ) { innerPadding ->
        KnowledgePageContainer(innerPadding = innerPadding) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("knowledge_local_summary_card"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.knowledge_local_workspace_eyebrow).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.knowledge_local_workspace_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.knowledge_local_workspace_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WorkspaceMetaPill(text = stringResource(R.string.knowledge_local_workspace_meta, state.datasetCount))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KoraWorkspaceSectionTitle(
                        title = stringResource(R.string.knowledge_local_recent_title),
                        supportingText = stringResource(R.string.knowledge_local_recent_supporting),
                    )
                    if (onReturnToChat != null) {
                        TextButton(onClick = onReturnToChat) {
                            Text(stringResource(R.string.knowledge_return_to_chat))
                        }
                    }
                    Button(onClick = onOpenLibrary) { Text(stringResource(R.string.knowledge_local_open_library)) }
                    if (state.recentDatasets.isEmpty()) {
                        Text(
                            stringResource(R.string.knowledge_local_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.recentDatasets.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenRecentDocument(item.datasetId) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(item.intro, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.knowledge_local_recent_meta, datasetSummaryLabel(item.type, item.status)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalKnowledgeLibraryScreen(
    state: LocalKnowledgeLibraryUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onSourceChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onImport: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    var showImportSheet by rememberSaveable { mutableStateOf(false) }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            modifier = Modifier.testTag("knowledge_local_import_sheet"),
        ) {
            LocalKnowledgeImportSheet(
                state = state,
                onTitleChanged = onTitleChanged,
                onSourceChanged = onSourceChanged,
                onTextChanged = onTextChanged,
                onImport = {
                    onImport()
                    showImportSheet = false
                },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            KoraGeminiTopBar(
                title = stringResource(R.string.knowledge_local_library_title),
                onOpenDrawer = onOpenDrawer,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportSheet = true },
                modifier = Modifier.testTag("knowledge_local_import_fab"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.knowledge_local_import_action),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.knowledge_back)) }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.knowledge_local_query_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when (state.status) {
                KnowledgeLoadState.Empty ->
                    Text(stringResource(R.string.knowledge_local_empty), style = MaterialTheme.typography.bodyMedium)
                else ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.items, key = { it.documentId }) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                                            Text(item.sourceLabel, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Switch(
                                            checked = item.isEnabled,
                                            onCheckedChange = { enabled -> onToggleEnabled(item.documentId, enabled) },
                                        )
                                    }
                                    Text(item.previewText, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        localKnowledgeIndexStatusLabel(item),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        stringResource(R.string.knowledge_local_chunk_count, item.chunkCount),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    item.indexErrorMessage?.takeIf { item.indexStatus == com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus.Failed }?.let { error ->
                                        Text(
                                            error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onOpenDocument(item.documentId) }) {
                                            Text(stringResource(R.string.knowledge_local_view_chunks))
                                        }
                                        TextButton(onClick = { onDeleteDocument(item.documentId) }) {
                                            Text(stringResource(R.string.knowledge_delete))
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

@Composable
private fun LocalKnowledgeImportSheet(
    state: LocalKnowledgeLibraryUiState,
    onTitleChanged: (String) -> Unit,
    onSourceChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.knowledge_local_import_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.knowledge_local_recent_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.draftTitle,
            onValueChange = onTitleChanged,
            label = { Text(stringResource(R.string.knowledge_local_import_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.draftSource,
            onValueChange = onSourceChanged,
            label = { Text(stringResource(R.string.knowledge_local_import_source_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.draftText,
            onValueChange = onTextChanged,
            label = { Text(stringResource(R.string.knowledge_local_import_text_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
        )
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth().testTag("knowledge_local_import_submit"),
        ) {
            Text(stringResource(R.string.knowledge_local_import_action))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalKnowledgeDocumentScreen(
    state: LocalKnowledgeDocumentUiState,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            KoraGeminiTopBar(
                title = state.title.ifBlank { stringResource(R.string.knowledge_local_document_title) },
                onOpenDrawer = onOpenDrawer,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.knowledge_back)) }
            Text(state.sourceLabel, style = MaterialTheme.typography.bodySmall)
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.chunks.isEmpty()) {
                Text(stringResource(R.string.knowledge_local_document_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.chunks, key = { it.dataId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.question, style = MaterialTheme.typography.titleSmall)
                                Text(item.answer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun localKnowledgeIndexStatusLabel(item: LocalKnowledgeDocumentUiModel): String =
    when (item.indexStatus) {
        com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus.Indexing ->
            stringResource(R.string.knowledge_local_status_indexing)
        com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus.Ready ->
            stringResource(R.string.knowledge_local_status_ready)
        com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus.Failed ->
            stringResource(R.string.knowledge_local_status_failed)
    }

@Composable
internal fun KnowledgeAdaptivePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.knowledge_overview_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.knowledge_overview_open_datasets), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetBrowserScreen(
    state: DatasetBrowserUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onCreateNameChanged: (String) -> Unit,
    onTypeFilterSelected: (String?) -> Unit,
    onStatusFilterSelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onCreateDataset: () -> Unit,
    onDeleteDataset: (String) -> Unit,
    onOpenDataset: (String) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
        KoraGeminiTopBar(
            title = stringResource(R.string.knowledge_dataset_browser_title),
            onOpenDrawer = onOpenDrawer,
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.knowledge_back)) }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.knowledge_dataset_search_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedTypeFilter == null,
                    onClick = { onTypeFilterSelected(null) },
                    label = { Text(stringResource(R.string.knowledge_dataset_filter_all_types)) },
                )
                state.availableTypes.forEach { type ->
                    FilterChip(
                        selected = state.selectedTypeFilter == type,
                        onClick = { onTypeFilterSelected(type) },
                        label = { Text(knowledgeTypeLabel(type)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedStatusFilter == null,
                    onClick = { onStatusFilterSelected(null) },
                    label = { Text(stringResource(R.string.knowledge_dataset_filter_all_statuses)) },
                )
                state.availableStatuses.forEach { status ->
                    FilterChip(
                        selected = state.selectedStatusFilter == status,
                        onClick = { onStatusFilterSelected(status) },
                        label = { Text(knowledgeStatusLabel(status)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.createName,
                    onValueChange = onCreateNameChanged,
                    label = { Text(stringResource(R.string.knowledge_dataset_create_name_label)) },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onCreateDataset) { Text(stringResource(R.string.knowledge_create)) }
                Button(onClick = onRefresh) {
                    Text(
                        stringResource(
                            if (state.isRefreshing) R.string.knowledge_refreshing else R.string.knowledge_refresh,
                        ),
                    )
                }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when (state.status) {
                KnowledgeLoadState.Empty ->
                    Text(stringResource(R.string.knowledge_dataset_empty), style = MaterialTheme.typography.bodyMedium)
                else ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.items, key = { it.datasetId }) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(item.intro.ifBlank { knowledgeTypeLabel(item.type) }, style = MaterialTheme.typography.bodyMedium)
                                    Text(datasetSummaryLabel(item.type, item.status), style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        stringResource(
                                            R.string.knowledge_dataset_vector_model,
                                            item.vectorModel.ifBlank { stringResource(R.string.knowledge_unspecified) },
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        stringResource(R.string.knowledge_updated_at, formatKnowledgeDate(item.updateTime)),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onOpenDataset(item.datasetId) }) {
                                            Text(stringResource(R.string.knowledge_collections_title))
                                        }
                                        TextButton(onClick = { onOpenSearch(item.datasetId) }) {
                                            Text(stringResource(R.string.knowledge_search_test_title))
                                        }
                                        TextButton(onClick = { onDeleteDataset(item.datasetId) }) {
                                            Text(stringResource(R.string.knowledge_delete))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManagementScreen(
    state: CollectionManagementUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onModeChanged: (CollectionCreateMode) -> Unit,
    onTextNameChanged: (String) -> Unit,
    onTextValueChanged: (String) -> Unit,
    onLinkValueChanged: (String) -> Unit,
    onLinkSelectorChanged: (String) -> Unit,
    onPickDocument: () -> Unit,
    onSubmit: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
        KoraGeminiTopBar(
            title = state.datasetName.ifBlank { stringResource(R.string.knowledge_collections_title) },
            onOpenDrawer = onOpenDrawer,
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.knowledge_back)) }
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.knowledge_refresh)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionCreateMode.entries.forEach { mode ->
                    Text(
                        text = collectionCreateModeLabel(mode),
                        modifier = Modifier.selectable(selected = state.createMode == mode, onClick = { onModeChanged(mode) }),
                        color = if (state.createMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            when (state.createMode) {
                CollectionCreateMode.FILE -> {
                    Button(onClick = onPickDocument) { Text(stringResource(R.string.knowledge_pick_file)) }
                    if (state.selectedDocumentName.isNotBlank()) {
                        Text(stringResource(R.string.knowledge_selected_document, state.selectedDocumentName))
                    }
                }
                CollectionCreateMode.LINK -> {
                    OutlinedTextField(
                        value = state.linkDraftValue,
                        onValueChange = onLinkValueChanged,
                        label = { Text(stringResource(R.string.knowledge_link_urls_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.linkSelector,
                        onValueChange = onLinkSelectorChanged,
                        label = { Text(stringResource(R.string.knowledge_link_selector_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CollectionCreateMode.TEXT,
                CollectionCreateMode.QA,
                -> {
                    OutlinedTextField(
                        value = state.textDraftName,
                        onValueChange = onTextNameChanged,
                        label = { Text(stringResource(R.string.knowledge_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.textDraftValue,
                        onValueChange = onTextValueChanged,
                        label = {
                            Text(
                                stringResource(
                                    if (state.createMode == CollectionCreateMode.QA) {
                                        R.string.knowledge_qa_content
                                    } else {
                                        R.string.knowledge_text_content
                                    },
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Button(onClick = onSubmit, enabled = !state.isSubmitting) {
                Text(
                    stringResource(
                        if (state.isSubmitting) R.string.knowledge_submitting else R.string.knowledge_create_import,
                    ),
                )
            }
            TextButton(onClick = onOpenSearch) { Text(stringResource(R.string.knowledge_open_search_test)) }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.tasks.isNotEmpty()) {
                Text(stringResource(R.string.knowledge_import_tasks), style = MaterialTheme.typography.titleMedium)
                state.tasks.forEach { task ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(task.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    R.string.knowledge_import_task_summary,
                                    knowledgeTypeLabel(task.sourceType),
                                    knowledgeStatusLabel(task.status),
                                    task.progress,
                                ),
                            )
                            task.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            Text(stringResource(R.string.knowledge_collection_list), style = MaterialTheme.typography.titleMedium)
            if (state.status == KnowledgeLoadState.Empty) {
                Text(stringResource(R.string.knowledge_collection_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                state.items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCollection(item.collectionId) },
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    R.string.knowledge_collection_summary,
                                    knowledgeTypeLabel(item.type),
                                    knowledgeTrainingTypeLabel(item.trainingType),
                                    knowledgeStatusLabel(item.status),
                                ),
                            )
                            Text(item.sourceName, style = MaterialTheme.typography.bodySmall)
                            Text(formatKnowledgeDate(item.updateTime), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChunkViewerScreen(
    state: ChunkViewerUiState,
    onBack: () -> Unit,
    onStartEditing: (ChunkItemUiModel) -> Unit,
    onQuestionChanged: (String) -> Unit,
    onAnswerChanged: (String) -> Unit,
    onDisabledChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
        KoraGeminiTopBar(
            title = stringResource(R.string.knowledge_chunk_viewer_title),
            onOpenDrawer = onOpenDrawer,
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.knowledge_back)) }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.editingChunkId != null) {
                OutlinedTextField(
                    value = state.editingQuestion,
                    onValueChange = onQuestionChanged,
                    label = { Text(stringResource(R.string.knowledge_question)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.editingAnswer,
                    onValueChange = onAnswerChanged,
                    label = { Text(stringResource(R.string.knowledge_answer)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.knowledge_disable_chunk))
                    Switch(checked = state.editingDisabled, onCheckedChange = onDisabledChanged)
                }
                Button(onClick = onSave) { Text(stringResource(R.string.knowledge_save)) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.dataId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(
                                    R.string.knowledge_chunk_header,
                                    item.chunkIndex,
                                    knowledgeStatusLabel(item.status),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            if (state.highlightedDataId == item.dataId) {
                                Text(
                                    stringResource(R.string.knowledge_highlighted_hit),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(item.question, style = MaterialTheme.typography.bodyLarge)
                            if (item.answer.isNotBlank()) {
                                Text(item.answer, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onStartEditing(item) }) { Text(stringResource(R.string.knowledge_edit)) }
                                TextButton(onClick = { onDelete(item.dataId) }) { Text(stringResource(R.string.knowledge_delete)) }
                            }
                        }
                    }
                }
                if (state.canLoadMore) {
                    item(key = "load_more") {
                        Button(onClick = onLoadMore, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    if (state.isLoading) R.string.knowledge_loading else R.string.knowledge_load_more,
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
private fun collectionCreateModeLabel(mode: CollectionCreateMode): String =
    stringResource(
        when (mode) {
            CollectionCreateMode.FILE -> R.string.knowledge_collection_mode_file
            CollectionCreateMode.LINK -> R.string.knowledge_collection_mode_link
            CollectionCreateMode.TEXT -> R.string.knowledge_collection_mode_text
            CollectionCreateMode.QA -> R.string.knowledge_collection_mode_qa
        },
    )

@Composable
private fun knowledgeTypeLabel(value: String): String {
    val resId =
        when (value.lowercase()) {
            "file" -> R.string.knowledge_label_type_file
            "link" -> R.string.knowledge_label_type_link
            "text" -> R.string.knowledge_label_type_text
            "qa" -> R.string.knowledge_label_type_qa
            "url" -> R.string.knowledge_label_type_url
            else -> return value
        }
    return stringResource(resId)
}

@Composable
private fun knowledgeTrainingTypeLabel(value: String): String {
    val resId =
        when (value.lowercase()) {
            "chunk" -> R.string.knowledge_label_training_chunk
            "qa" -> R.string.knowledge_label_training_qa
            else -> return value
        }
    return stringResource(resId)
}

@Composable
private fun knowledgeStatusLabel(value: String): String {
    val resId =
        when (value.lowercase()) {
            "active" -> R.string.knowledge_label_status_active
            "running" -> R.string.knowledge_label_status_running
            "syncing" -> R.string.knowledge_label_status_syncing
            "done",
            "success",
            -> R.string.knowledge_label_status_success
            "error",
            "failed",
            -> R.string.knowledge_label_status_failed
            "pending" -> R.string.knowledge_label_status_pending
            "disabled" -> R.string.knowledge_label_status_disabled
            else -> return value
        }
    return stringResource(resId)
}

@Composable
private fun datasetSummaryLabel(
    type: String,
    status: String,
): String = stringResource(R.string.knowledge_dataset_summary, knowledgeTypeLabel(type), knowledgeStatusLabel(status))

@Composable
private fun formatKnowledgeDate(epochMillis: Long): String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(epochMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTestScreen(
    state: SearchTestUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchModeChanged: (String) -> Unit,
    onSimilarityChanged: (String) -> Unit,
    onEmbeddingWeightChanged: (String) -> Unit,
    onUseReRankChanged: (Boolean) -> Unit,
    onSearch: () -> Unit,
    previewResult: SearchResultUiModel? = null,
    onOpenResultPreview: (SearchResultUiModel) -> Unit,
    onDismissPreview: () -> Unit,
    onOpenResultContext: (SearchResultUiModel) -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    previewResult?.let { result ->
        ModalBottomSheet(
            onDismissRequest = onDismissPreview,
            modifier = Modifier.testTag("knowledge_search_result_preview_sheet"),
        ) {
            SearchResultPreviewSheet(
                result = result,
                onOpenContext = {
                    onDismissPreview()
                    onOpenResultContext(result)
                },
            )
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
        KoraGeminiTopBar(
            title = stringResource(R.string.knowledge_search_test_title),
            onOpenDrawer = onOpenDrawer,
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.knowledge_back)) }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.knowledge_question)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.searchMode,
                onValueChange = onSearchModeChanged,
                label = { Text(stringResource(R.string.knowledge_search_mode)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.similarity,
                onValueChange = onSimilarityChanged,
                label = { Text(stringResource(R.string.knowledge_similarity)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.embeddingWeight,
                onValueChange = onEmbeddingWeightChanged,
                label = { Text(stringResource(R.string.knowledge_embedding_weight)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.knowledge_use_rerank))
                Switch(checked = state.useReRank, onCheckedChange = onUseReRankChanged)
            }
            Button(onClick = onSearch, enabled = !state.isSearching) {
                Text(
                    stringResource(
                        if (state.isSearching) R.string.knowledge_searching else R.string.knowledge_start_search,
                    ),
                )
            }
            if (state.duration.isNotBlank()) {
                Text(stringResource(R.string.knowledge_duration, state.duration))
            }
            if (state.extensionInfo.isNotBlank()) {
                Text(
                    stringResource(R.string.knowledge_extension_model, state.extensionInfo),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.status == KnowledgeLoadState.Empty && !state.isSearching) {
                Text(stringResource(R.string.knowledge_search_empty), style = MaterialTheme.typography.bodyMedium)
            }
            state.results.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenResultPreview(item) }) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(searchResultTitle(item), style = MaterialTheme.typography.titleSmall)
                        Text(searchResultSnippet(item), style = MaterialTheme.typography.bodyMedium)
                        searchResultScoreLabel(item)?.let { scoreLabel ->
                            Text(scoreLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun searchResultTitle(item: SearchResultUiModel): String =
    item.sourceName.ifBlank {
        item.question.take(18).ifBlank { stringResource(R.string.knowledge_search_result_title_fallback) }
    }

@Composable
private fun searchResultSnippet(item: SearchResultUiModel): String =
    listOf(item.question, item.answer).filter { it.isNotBlank() }.joinToString("\n").ifBlank {
        stringResource(R.string.knowledge_search_result_snippet_fallback)
    }

@Composable
private fun searchResultScoreLabel(item: SearchResultUiModel): String? {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val formattedScore =
        item.score?.let { score ->
            NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = 3
                minimumFractionDigits = 0
            }.format(score)
        }
    return when {
        item.scoreType.isNullOrBlank() && formattedScore == null -> null
        item.scoreType.isNullOrBlank() -> formattedScore
        formattedScore == null -> item.scoreType
        else -> stringResource(R.string.knowledge_score_summary, item.scoreType, formattedScore)
    }
}

@Composable
internal fun SearchResultPreviewSheet(
    result: SearchResultUiModel,
    onOpenContext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("knowledge_search_result_preview_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.knowledge_reference_preview_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(searchResultTitle(result), style = MaterialTheme.typography.titleMedium)
        if (result.sourceName.isNotBlank()) {
            Text(
                stringResource(R.string.knowledge_reference_preview_source, result.sourceName),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(searchResultPreviewBody(result), style = MaterialTheme.typography.bodyMedium)
        searchResultScoreLabel(result)?.let { scoreLabel ->
            Text(scoreLabel, style = MaterialTheme.typography.labelMedium)
        }
        if (!result.datasetId.isNullOrBlank() && !result.collectionId.isNullOrBlank()) {
            Button(
                onClick = onOpenContext,
                modifier = Modifier.testTag("knowledge_search_result_preview_open_context"),
            ) {
                Text(stringResource(R.string.knowledge_open_chunk_context))
            }
        }
    }
}

@Composable
private fun searchResultPreviewBody(item: SearchResultUiModel): String =
    listOf(item.question, item.answer).filter { it.isNotBlank() }.joinToString("\n\n").ifBlank {
        stringResource(R.string.knowledge_reference_preview_empty)
    }

@Composable
private fun KnowledgePageContainer(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(knowledgePageBackgroundBrush()),
    ) {
        content()
    }
}

@Composable
private fun knowledgePageBackgroundBrush(): Brush =
    Brush.verticalGradient(
        colors =
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.035f),
                MaterialTheme.colorScheme.surface,
            ),
    )
