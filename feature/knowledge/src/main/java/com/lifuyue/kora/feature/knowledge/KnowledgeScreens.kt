package com.lifuyue.kora.feature.knowledge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeOverviewScreen(
    state: KnowledgeOverviewUiState,
    onOpenDatasets: () -> Unit,
    onOpenRecentDataset: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("知识库") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("当前 App：${state.selectedAppId ?: "未选择"}", style = MaterialTheme.typography.bodyMedium)
            Text("数据集 ${state.datasetCount} 个", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onOpenDatasets) { Text("进入数据集") }
            state.recentDatasets.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenRecentDataset(item.datasetId) },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.intro.ifBlank { item.type }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
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
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("数据集") },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text("搜索数据集") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedTypeFilter == null,
                    onClick = { onTypeFilterSelected(null) },
                    label = { Text("全部类型") },
                )
                state.availableTypes.forEach { type ->
                    FilterChip(
                        selected = state.selectedTypeFilter == type,
                        onClick = { onTypeFilterSelected(type) },
                        label = { Text(type) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedStatusFilter == null,
                    onClick = { onStatusFilterSelected(null) },
                    label = { Text("全部状态") },
                )
                state.availableStatuses.forEach { status ->
                    FilterChip(
                        selected = state.selectedStatusFilter == status,
                        onClick = { onStatusFilterSelected(status) },
                        label = { Text(status) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.createName,
                    onValueChange = onCreateNameChanged,
                    label = { Text("新数据集名称") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onCreateDataset) { Text("创建") }
                Button(onClick = onRefresh) { Text(if (state.isRefreshing) "刷新中" else "刷新") }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when (state.status) {
                KnowledgeLoadState.Empty -> Text("当前筛选下没有数据集。", style = MaterialTheme.typography.bodyMedium)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.items, key = { it.datasetId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(item.intro.ifBlank { item.type }, style = MaterialTheme.typography.bodyMedium)
                                Text("向量模型：${item.vectorModel.ifBlank { "未标注" }}", style = MaterialTheme.typography.bodySmall)
                                Text("更新时间：${item.updateTimeLabel}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onOpenDataset(item.datasetId) }) { Text("Collections") }
                                    TextButton(onClick = { onOpenSearch(item.datasetId) }) { Text("检索测试") }
                                    TextButton(onClick = { onDeleteDataset(item.datasetId) }) { Text("删除") }
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
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(state.datasetName.ifBlank { "Collections" }) },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            actions = { TextButton(onClick = onRefresh) { Text("刷新") } },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionCreateMode.entries.forEach { mode ->
                    Text(
                        text = mode.name,
                        modifier = Modifier.selectable(selected = state.createMode == mode, onClick = { onModeChanged(mode) }),
                        color = if (state.createMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            when (state.createMode) {
                CollectionCreateMode.FILE -> {
                    Button(onClick = onPickDocument) { Text("选择文件") }
                    if (state.selectedDocumentName.isNotBlank()) {
                        Text("已选择：${state.selectedDocumentName}")
                    }
                }
                CollectionCreateMode.LINK -> {
                    OutlinedTextField(
                        value = state.linkDraftValue,
                        onValueChange = onLinkValueChanged,
                        label = { Text("URL，每行一个") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.linkSelector,
                        onValueChange = onLinkSelectorChanged,
                        label = { Text("Selector（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CollectionCreateMode.TEXT,
                CollectionCreateMode.QA,
                -> {
                    OutlinedTextField(
                        value = state.textDraftName,
                        onValueChange = onTextNameChanged,
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.textDraftValue,
                        onValueChange = onTextValueChanged,
                        label = { Text(if (state.createMode == CollectionCreateMode.QA) "Q/A 内容" else "文本内容") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Button(onClick = onSubmit, enabled = !state.isSubmitting) {
                Text(if (state.isSubmitting) "提交中..." else "创建导入")
            }
            TextButton(onClick = onOpenSearch) { Text("进入检索测试") }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.tasks.isNotEmpty()) {
                Text("导入任务", style = MaterialTheme.typography.titleMedium)
                state.tasks.forEach { task ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(task.displayName, style = MaterialTheme.typography.titleSmall)
                            Text("${task.sourceType} · ${task.status} · ${task.progress}%")
                            task.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            Text("Collection 列表", style = MaterialTheme.typography.titleMedium)
            if (state.status == KnowledgeLoadState.Empty) {
                Text("当前数据集还没有 collection。", style = MaterialTheme.typography.bodyMedium)
            } else {
                state.items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCollection(item.collectionId) },
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text("${item.type} · ${item.trainingType} · ${item.status}")
                            Text(item.sourceName, style = MaterialTheme.typography.bodySmall)
                            Text(item.updateTimeLabel, style = MaterialTheme.typography.labelSmall)
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
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Chunk Viewer") },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.editingChunkId != null) {
                OutlinedTextField(
                    value = state.editingQuestion,
                    onValueChange = onQuestionChanged,
                    label = { Text("问题") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.editingAnswer,
                    onValueChange = onAnswerChanged,
                    label = { Text("答案") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("禁用该 Chunk")
                    Switch(checked = state.editingDisabled, onCheckedChange = onDisabledChanged)
                }
                Button(onClick = onSave) { Text("保存") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.dataId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("#${item.chunkIndex} ${item.status}", style = MaterialTheme.typography.labelLarge)
                            if (state.highlightedDataId == item.dataId) {
                                Text("当前引用命中", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(item.question, style = MaterialTheme.typography.bodyLarge)
                            if (item.answer.isNotBlank()) {
                                Text(item.answer, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onStartEditing(item) }) { Text("编辑") }
                                TextButton(onClick = { onDelete(item.dataId) }) { Text("删除") }
                            }
                        }
                    }
                }
                if (state.canLoadMore) {
                    item(key = "load_more") {
                        Button(onClick = onLoadMore, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.isLoading) "加载中..." else "加载更多")
                        }
                    }
                }
            }
        }
    }
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
    onOpenResult: (SearchResultUiModel) -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("检索测试") },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text("问题") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.searchMode,
                onValueChange = onSearchModeChanged,
                label = { Text("Search Mode") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.similarity,
                onValueChange = onSimilarityChanged,
                label = { Text("Similarity") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.embeddingWeight,
                onValueChange = onEmbeddingWeightChanged,
                label = { Text("Embedding Weight") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("使用 ReRank")
                Switch(checked = state.useReRank, onCheckedChange = onUseReRankChanged)
            }
            Button(onClick = onSearch, enabled = !state.isSearching) {
                Text(if (state.isSearching) "检索中..." else "开始检索")
            }
            if (state.duration.isNotBlank()) {
                Text("耗时：${state.duration}")
            }
            if (state.extensionInfo.isNotBlank()) {
                Text("扩展查询模型：${state.extensionInfo}", style = MaterialTheme.typography.bodySmall)
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.status == KnowledgeLoadState.Empty && !state.isSearching) {
                Text("未命中结果。", style = MaterialTheme.typography.bodyMedium)
            }
            state.results.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenResult(item) }) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(item.snippet, style = MaterialTheme.typography.bodyMedium)
                        if (item.scoreLabel.isNotBlank()) {
                            Text(item.scoreLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
