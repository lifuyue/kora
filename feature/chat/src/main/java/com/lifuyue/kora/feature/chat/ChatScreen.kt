package com.lifuyue.kora.feature.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.KoraFeedbackPhase
import com.lifuyue.kora.core.common.ui.KoraFeedbackLabel
import com.lifuyue.kora.core.common.ui.KoraInlineFeedbackCard
import kotlinx.coroutines.launch
import java.text.NumberFormat

internal enum class ChatComposerPrimaryAction {
    Send,
    Stop,
}

internal fun chatComposerPrimaryAction(uiState: ChatUiState): ChatComposerPrimaryAction =
    when {
        uiState.canStopGeneration -> ChatComposerPrimaryAction.Stop
        else -> ChatComposerPrimaryAction.Send
    }

internal fun shouldSubmitFromHardwareEnter(
    key: Key,
    type: KeyEventType,
    canSend: Boolean,
): Boolean = canSend && type == KeyEventType.KeyUp && (key == Key.Enter || key == Key.NumPadEnter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    conversationBrowserUiState: ConversationListUiState = ConversationListUiState(),
    showConversationBrowser: Boolean = false,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRemoveAttachment: (String) -> Unit = {},
    onRetryAttachment: (String) -> Unit = {},
    onCancelAttachmentUpload: (String) -> Unit = {},
    onStopGenerating: () -> Unit,
    onContinueGeneration: () -> Unit,
    onRegenerate: (ChatMessageUiModel) -> Unit,
    onOpenKnowledgePicker: () -> Unit = {},
    onDismissKnowledgePicker: () -> Unit = {},
    onBackToKnowledgeDatasets: () -> Unit = {},
    onSelectKnowledgeDataset: (String) -> Unit = {},
    onSelectKnowledgeCollection: (String) -> Unit = {},
    onKnowledgePickerQueryChanged: (String) -> Unit = {},
    onClearKnowledgeReference: () -> Unit = {},
    onOpenKnowledgeManager: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenConversationBrowser: () -> Unit = {},
    onDismissConversationBrowser: () -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onNewConversation: () -> Unit = {},
    onConversationQueryChanged: (String) -> Unit = {},
    onSelectConversationFolderFilter: (String?) -> Unit = {},
    onSelectConversationTagFilter: (String?) -> Unit = {},
    onToggleShowArchivedConversations: (Boolean) -> Unit = {},
    onDeleteConversation: (String) -> Unit = {},
    onRenameConversation: (String, String) -> Unit = { _, _ -> },
    onTogglePinConversation: (String, Boolean) -> Unit = { _, _ -> },
    onSetConversationArchived: (String, Boolean) -> Unit = { _, _ -> },
    onClearConversations: () -> Unit = {},
    onCreateConversationFolder: (String) -> Unit = {},
    onRenameConversationFolder: (String, String) -> Unit = { _, _ -> },
    onDeleteConversationFolder: (String) -> Unit = {},
    onCreateConversationTag: (String) -> Unit = {},
    onRenameConversationTag: (String, String) -> Unit = { _, _ -> },
    onDeleteConversationTag: (String) -> Unit = {},
    onMoveConversationToFolder: (String, String?) -> Unit = { _, _ -> },
    onSetConversationTags: (String, List<String>) -> Unit = { _, _ -> },
    onOpenQuickSettings: () -> Unit = {},
    onSuggestedQuestion: (String) -> Unit = {},
    onOpenCitation: (CitationItemUiModel) -> Unit = {},
    onUpdateInteractiveDraft: (ChatMessageUiModel, String) -> Unit = { _, _ -> },
    onSubmitInteractiveResponse: (ChatMessageUiModel, String) -> Unit = { _, _ -> },
    onToggleReasoning: (String) -> Unit = {},
) {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pageBackground = MaterialTheme.colorScheme.background
    val errorContainer = if (isLightTheme) Color(0xFFFFE2DE) else Color(0xFF351A1A)
    val errorText = if (isLightTheme) Color(0xFF8C2E27) else Color(0xFFFFC9C5)
    var activeCitationMessage by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var activeCitationPreview by remember { mutableStateOf<CitationItemUiModel?>(null) }
    var autoScrollPaused by rememberSaveable { mutableStateOf(false) }
    var hasInitializedScroll by rememberSaveable(uiState.chatId) { mutableStateOf(false) }

    LaunchedEffect(uiState.chatId, uiState.messages.isEmpty()) {
        if (uiState.messages.isEmpty()) {
            autoScrollPaused = false
            hasInitializedScroll = false
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.autoScrollEnabled, autoScrollPaused) {
        if (!uiState.autoScrollEnabled || uiState.messages.isEmpty() || autoScrollPaused) {
            return@LaunchedEffect
        }
        listState.scrollToItem(uiState.messages.lastIndex)
        hasInitializedScroll = true
    }

    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.layoutInfo.totalItemsCount,
        uiState.autoScrollEnabled,
    ) {
        if (!uiState.autoScrollEnabled || !hasInitializedScroll || uiState.messages.isEmpty()) {
            autoScrollPaused = false
            return@LaunchedEffect
        }
        autoScrollPaused = !isNearListBottom(listState, uiState.messages.lastIndex)
    }

    if (showConversationBrowser) {
        ModalBottomSheet(
            onDismissRequest = onDismissConversationBrowser,
            modifier = Modifier.testTag(ChatTestTags.CONVERSATION_BROWSER_SHEET),
        ) {
            ConversationListScreen(
                uiState = conversationBrowserUiState,
                onQueryChanged = onConversationQueryChanged,
                onSelectFolderFilter = onSelectConversationFolderFilter,
                onSelectTagFilter = onSelectConversationTagFilter,
                onToggleShowArchived = onToggleShowArchivedConversations,
                onOpenConversation = onOpenConversation,
                onNewConversation = onNewConversation,
                onDeleteConversation = onDeleteConversation,
                onRenameConversation = onRenameConversation,
                onTogglePin = onTogglePinConversation,
                onSetArchived = onSetConversationArchived,
                onClearConversations = onClearConversations,
                onCreateFolder = onCreateConversationFolder,
                onRenameFolder = onRenameConversationFolder,
                onDeleteFolder = onDeleteConversationFolder,
                onCreateTag = onCreateConversationTag,
                onRenameTag = onRenameConversationTag,
                onDeleteTag = onDeleteConversationTag,
                onMoveConversationToFolder = onMoveConversationToFolder,
                onSetConversationTags = onSetConversationTags,
                embeddedMode = true,
                onCloseEmbedded = onDismissConversationBrowser,
            )
        }
    }
    if (uiState.knowledgePickerState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissKnowledgePicker,
            modifier = Modifier.testTag(ChatTestTags.KNOWLEDGE_PICKER_SHEET),
        ) {
            ChatKnowledgePickerSheet(
                state = uiState.knowledgePickerState,
                onBack = onBackToKnowledgeDatasets,
                onSelectDataset = onSelectKnowledgeDataset,
                onSelectCollection = onSelectKnowledgeCollection,
                onQueryChanged = onKnowledgePickerQueryChanged,
                onOpenKnowledgeManager = onOpenKnowledgeManager,
            )
        }
    }
    activeCitationMessage?.let { message ->
        ModalBottomSheet(
            onDismissRequest = { activeCitationMessage = null },
            modifier = Modifier.testTag(ChatTestTags.CITATION_PANEL),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.chat_citations_title, message.citations.size),
                    style = MaterialTheme.typography.titleLarge,
                )
                message.citations.forEach { citation ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                citationDisplayTitle(citation),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (citation.snippet.isNotBlank()) {
                                Text(citation.snippet, style = MaterialTheme.typography.bodyMedium)
                            }
                            citationScoreLabel(citation)?.let { scoreLabel ->
                                Text(scoreLabel, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(onClick = { activeCitationPreview = citation }) {
                                Text(stringResource(R.string.chat_open_citation))
                            }
                        }
                    }
                }
            }
        }
    }
    activeCitationPreview?.let { citation ->
        ModalBottomSheet(
            onDismissRequest = { activeCitationPreview = null },
            modifier = Modifier.testTag(ChatTestTags.CITATION_PREVIEW_SHEET),
        ) {
            CitationPreviewSheet(
                citation = citation,
                onOpenCitation = {
                    onOpenCitation(citation)
                    activeCitationPreview = null
                },
            )
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = pageBackground,
        topBar = {
            ChatGeminiTopBar(
                onOpenDrawer = onOpenDrawer,
                onOpenQuickSettings = onOpenQuickSettings,
            )
        },
        bottomBar = {
            ChatGeminiComposer(
                uiState = uiState,
                onInputChanged = onInputChanged,
                onSend = onSend,
                onStopGenerating = onStopGenerating,
                onOpenKnowledgePicker = onOpenKnowledgePicker,
                onClearKnowledgeReference = onClearKnowledgeReference,
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(pageBackground)
                    .padding(horizontal = 20.dp),
        ) {
            if (uiState.messages.isEmpty()) {
                ChatGeminiEmptyState(
                    modifier = Modifier.padding(top = 48.dp).testTag("chat_workspace_header"),
                    onSuggestionClick = onSuggestedQuestion,
                )
            }
            if (uiState.errorMessage != null) {
                Surface(
                    color = errorContainer,
                    contentColor = errorText,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                    )
                }
            }
            if (uiState.isInitialLoading) {
                ChatLoadingSkeleton(modifier = Modifier.weight(1f).testTag(ChatTestTags.CHAT_SKELETON))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).testTag(ChatTestTags.CHAT_LIST),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.messages, key = { it.messageId }) { message ->
                        MessageCard(
                            message = message,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(message.markdown))
                            },
                            onCopyCode = { code ->
                                clipboardManager.setText(AnnotatedString(code))
                            },
                            onContinueGeneration = onContinueGeneration,
                            onRegenerate = { onRegenerate(message) },
                            onSuggestedQuestion = onSuggestedQuestion,
                            onOpenCitationList = { activeCitationMessage = it },
                            onOpenCitation = { citation -> activeCitationPreview = citation },
                            onUpdateInteractiveDraft = onUpdateInteractiveDraft,
                            onSubmitInteractiveResponse = onSubmitInteractiveResponse,
                            onToggleReasoning = onToggleReasoning,
                            showReasoningEntry = uiState.showReasoningEntry,
                        )
                    }
                }
            }
            if (uiState.autoScrollEnabled && autoScrollPaused) {
                AssistChip(
                    onClick = {
                        scope.launch {
                            if (uiState.messages.isNotEmpty()) {
                                listState.scrollToItem(uiState.messages.lastIndex)
                            }
                            hasInitializedScroll = true
                            autoScrollPaused = false
                        }
                    },
                    label = { Text(stringResource(R.string.chat_resume_auto_scroll)) },
                    modifier = Modifier.testTag(ChatTestTags.AUTO_SCROLL_RESUME),
                )
            }
            if (uiState.attachments.isNotEmpty()) {
                AttachmentComposer(
                    attachments = uiState.attachments,
                    showSelectionActions = false,
                    canPickImage = uiState.attachmentConfig.canSelectImg,
                    canPickFile = uiState.attachmentConfig.canSelectFile ||
                        uiState.attachmentConfig.canSelectVideo ||
                        uiState.attachmentConfig.canSelectAudio ||
                        uiState.attachmentConfig.canSelectCustomFileExtension,
                    onPickImage = {},
                    onPickFile = {},
                    onRemoveAttachment = onRemoveAttachment,
                    onRetryAttachment = onRetryAttachment,
                    onCancelAttachmentUpload = onCancelAttachmentUpload,
                )
            }
            if (uiState.canStopGeneration) {
                OutlinedButton(onClick = onStopGenerating) {
                    Text(stringResource(R.string.chat_stop_generation))
                }
            }
        }
    }
}

@Composable
private fun ChatGeminiTopBar(
    onOpenDrawer: () -> Unit,
    onOpenQuickSettings: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 8.dp, end = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenDrawer)
                    .testTag(ChatTestTags.CHAT_MENU_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.chat_history), tint = onSurface)
        }
        Text(
            text = stringResource(R.string.chat_brand_title),
            style = MaterialTheme.typography.titleLarge,
            color = onSurface,
        )
        GeminiProfileAvatar(onClick = onOpenQuickSettings)
    }
}

@Composable
private fun GeminiProfileAvatar(onClick: () -> Unit) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val gradientColors =
        listOf(
            Color(0xFF4285F4),
            Color(0xFFEA4335),
            Color(0xFFFBBC05),
            Color(0xFF34A853),
        )
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .border(2.dp, Brush.sweepGradient(gradientColors), CircleShape)
                .background(if (isLightTheme) Color.White else Color(0xFF121212))
                .clickable(onClick = onClick)
                .testTag(ChatTestTags.CHAT_SETTINGS_BUTTON),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.chat_quick_settings),
            tint = if (isLightTheme) Color(0xFF5B616C) else Color(0xFFE1E1E1),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ChatGeminiEmptyState(
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit = {},
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val suggestionItems: List<Pair<ImageVector, String>> =
        listOf(
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_knowledge_search),
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_knowledge_summary),
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_knowledge_answer),
            Icons.Filled.Person to stringResource(R.string.chat_suggestion_explain_concept),
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_daily_reply),
        )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.chat_greeting),
                style = MaterialTheme.typography.headlineMedium,
                color = secondaryText,
            )
            Text(
                text = stringResource(R.string.chat_empty_headline),
                style = MaterialTheme.typography.displaySmall,
                color = primaryText,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            suggestionItems.forEachIndexed { index, (icon, label) ->
                ChatSuggestionChip(
                    icon = icon,
                    label = label,
                    isLightTheme = isLightTheme,
                    onClick = { onSuggestionClick(label) },
                    modifier = Modifier.testTag("${ChatTestTags.CHAT_SUGGESTION_PREFIX}$index"),
                )
            }
        }
    }
}

@Composable
private fun ChatSuggestionChip(
    icon: ImageVector,
    label: String,
    isLightTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipBackground =
        if (isLightTheme) {
            Color.White
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val iconTint =
        if (isLightTheme) {
            Color(0xFF6A717D)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val textColor =
        if (isLightTheme) {
            Color(0xFF2A2D33)
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Surface(
        modifier = modifier.wrapContentWidth().clickable(onClick = onClick),
        color = chipBackground,
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
            Text(
                label,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ChatGeminiComposer(
    uiState: ChatUiState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    onOpenKnowledgePicker: () -> Unit,
    onClearKnowledgeReference: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isLightTheme = colorScheme.background.luminance() > 0.5f
    Surface(
        color = if (isLightTheme) Color(0xFFFCFCFD) else colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 34.dp, bottomEnd = 34.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            uiState.selectedKnowledgeReference?.let { reference ->
                SelectedKnowledgeReferencePill(
                    reference = reference,
                    onClear = onClearKnowledgeReference,
                    modifier = Modifier.testTag(ChatTestTags.CHAT_SELECTED_KNOWLEDGE_PILL),
                )
            }
            BasicTextField(
                value = uiState.input,
                onValueChange = onInputChanged,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(ChatTestTags.CHAT_INPUT)
                        .onPreviewKeyEvent { event ->
                            if (shouldSubmitFromHardwareEnter(event.key, event.type, uiState.canSend)) {
                                onSend()
                                true
                            } else {
                                false
                            }
                        },
                singleLine = false,
                maxLines = 4,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (uiState.canSend) onSend() }),
                decorationBox = { innerField ->
                    if (uiState.input.isBlank()) {
                        Text(
                            text = stringResource(R.string.chat_input_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerField()
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GeminiComposerIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.chat_composer_add_knowledge),
                    onClick = onOpenKnowledgePicker,
                    modifier = Modifier.testTag(ChatTestTags.CHAT_ATTACHMENT_TRIGGER_BUTTON),
                )
                Spacer(modifier = Modifier.weight(1f))
                when (chatComposerPrimaryAction(uiState)) {
                    ChatComposerPrimaryAction.Stop -> {
                        GeminiComposerIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.chat_stop_generation),
                            onClick = onStopGenerating,
                            modifier = Modifier,
                        )
                    }
                    ChatComposerPrimaryAction.Send -> {
                        GeminiComposerIconButton(
                            icon = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            onClick = onSend,
                            enabled = uiState.canSend,
                            modifier = Modifier.testTag(ChatTestTags.CHAT_PRIMARY_ACTION_BUTTON),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedKnowledgeReferencePill(
    reference: ChatKnowledgeReferenceUiModel,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_selected_knowledge_reference, reference.collectionName),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f, fill = false),
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp).testTag(ChatTestTags.CHAT_SELECTED_KNOWLEDGE_CLEAR),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.chat_clear_selected_knowledge),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatKnowledgePickerSheet(
    state: ChatKnowledgePickerUiState,
    onBack: () -> Unit,
    onSelectDataset: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onOpenKnowledgeManager: () -> Unit,
) {
    val isDatasetLevel = state.selectedDatasetId == null
    val filteredItemsEmpty =
        if (isDatasetLevel) {
            state.filteredDatasets.isEmpty()
        } else {
            state.filteredCollections.isEmpty()
        }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_knowledge_picker_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(ChatTestTags.KNOWLEDGE_PICKER_TITLE),
        )
        KnowledgePickerSearchField(
            value = state.query,
            placeholder =
                when {
                    state.usesLocalLibrary -> stringResource(R.string.chat_knowledge_picker_search_local_placeholder)
                    isDatasetLevel -> stringResource(R.string.chat_knowledge_picker_search_datasets_placeholder)
                    else -> stringResource(R.string.chat_knowledge_picker_search_collections_placeholder)
                },
            onValueChange = onQueryChanged,
            modifier = Modifier.testTag(ChatTestTags.KNOWLEDGE_PICKER_SEARCH),
        )
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (isDatasetLevel) {
            Text(
                text =
                    if (state.usesLocalLibrary) {
                        stringResource(R.string.chat_knowledge_picker_local_documents_title)
                    } else {
                        stringResource(R.string.chat_knowledge_picker_datasets_title)
                    },
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.isLoadingDatasets) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!state.isLoadingDatasets && state.datasets.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_knowledge_picker_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onOpenKnowledgeManager,
                    modifier = Modifier.testTag(ChatTestTags.KNOWLEDGE_PICKER_OPEN_MANAGER),
                ) {
                    Text(stringResource(R.string.chat_knowledge_picker_open_manager))
                }
            } else if (!state.isLoadingDatasets && filteredItemsEmpty) {
                Text(
                    text = stringResource(R.string.chat_knowledge_picker_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(state.filteredDatasets, key = { it.datasetId }) { dataset ->
                        KnowledgePickerListItem(
                            title = dataset.name,
                            summary = dataset.summary,
                            onClick = { onSelectDataset(dataset.datasetId) },
                            modifier = Modifier.testTag(ChatTestTags.knowledgeDataset(dataset.datasetId)),
                        )
                    }
                }
            }
        } else {
            TextButton(onClick = onBack, modifier = Modifier.testTag(ChatTestTags.KNOWLEDGE_PICKER_BACK)) {
                Text(stringResource(R.string.chat_knowledge_picker_back))
            }
            Text(
                text = stringResource(R.string.chat_knowledge_picker_collections_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.isLoadingCollections) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!state.isLoadingCollections && state.collections.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_knowledge_picker_collections_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (!state.isLoadingCollections && filteredItemsEmpty) {
                Text(
                    text = stringResource(R.string.chat_knowledge_picker_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(state.filteredCollections, key = { it.collectionId }) { collection ->
                        KnowledgePickerListItem(
                            title = collection.name,
                            summary = collection.summary,
                            onClick = { onSelectCollection(collection.collectionId) },
                            modifier = Modifier.testTag(ChatTestTags.knowledgeCollection(collection.collectionId)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgePickerSearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.testTag(ChatTestTags.KNOWLEDGE_PICKER_SEARCH_CLEAR),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.chat_knowledge_picker_search_clear),
                    )
                }
            }
        },
        placeholder = {
            Text(placeholder)
        },
    )
}

@Composable
private fun KnowledgePickerListItem(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (summary.isNotBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun GeminiComposerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isLightTheme = colorScheme.background.luminance() > 0.5f
    val containerColor =
        if (isLightTheme) {
            Color(0xFFF0F2F5)
        } else {
            colorScheme.surfaceContainerHigh
        }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(containerColor.copy(alpha = if (enabled) 1f else 0.6f)),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.55f),
            modifier = Modifier.size(21.dp),
        )
    }
}

private fun isNearListBottom(
    listState: androidx.compose.foundation.lazy.LazyListState,
    lastIndex: Int,
): Boolean {
    if (lastIndex < 0) {
        return true
    }
    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisible >= lastIndex - 1
}

@Composable
private fun ChatLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(3) { index ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(0.35f).height(14.dp),
                    ) {}
                    Surface(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(if (index == 1) 0.9f else 0.75f).height(16.dp),
                    ) {}
                    Surface(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(if (index == 2) 0.6f else 0.82f).height(16.dp),
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun AttachmentComposer(
    attachments: List<AttachmentDraftUiModel>,
    showSelectionActions: Boolean,
    canPickImage: Boolean,
    canPickFile: Boolean,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onRetryAttachment: (String) -> Unit,
    onCancelAttachmentUpload: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (showSelectionActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canPickImage) {
                    OutlinedButton(onClick = onPickImage, modifier = Modifier.testTag(ChatTestTags.CHAT_ATTACHMENT_IMAGE_PICK)) {
                        Text(chatString("chat_attachment_add_image"))
                    }
                }
                if (canPickFile) {
                    OutlinedButton(onClick = onPickFile, modifier = Modifier.testTag(ChatTestTags.CHAT_ATTACHMENT_FILE_PICK)) {
                        Text(chatString("chat_attachment_add_file"))
                    }
                }
            }
        }
        if (attachments.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.CHAT_ATTACHMENT_LIST),
            ) {
                attachments.forEach { attachment ->
                    AttachmentPreviewCard(
                        attachment = attachment,
                        onRemoveAttachment = onRemoveAttachment,
                        onRetryAttachment = onRetryAttachment,
                        onCancelAttachmentUpload = onCancelAttachmentUpload,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewCard(
    attachment: AttachmentDraftUiModel,
    onRemoveAttachment: (String) -> Unit,
    onRetryAttachment: (String) -> Unit,
    onCancelAttachmentUpload: (String) -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.attachmentItem(attachment.localUri)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (attachment.kind == AttachmentKind.Image) {
                    AsyncImage(
                        model = Uri.parse(attachment.localUri),
                        contentDescription = attachment.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(attachment.kind.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(attachment.displayName, style = MaterialTheme.typography.titleSmall)
                    attachment.sizeBytes?.let { sizeBytes ->
                        val locale = LocalContext.current.resources.configuration.locales[0]
                        Text(
                            chatString(
                                "chat_attachment_size_bytes",
                                NumberFormat.getIntegerInstance(locale).format(sizeBytes),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        text =
                            when (attachment.uploadStatus) {
                                AttachmentUploadStatus.Idle -> chatString("chat_attachment_uploading")
                                AttachmentUploadStatus.Uploading -> chatString("chat_attachment_uploading")
                                AttachmentUploadStatus.Uploaded -> chatString("chat_attachment_uploaded")
                                AttachmentUploadStatus.Failed -> attachment.errorMessage ?: chatString("chat_attachment_failed")
                                AttachmentUploadStatus.Cancelled -> chatString("chat_attachment_cancelled")
                            },
                        color =
                            if (attachment.uploadStatus == AttachmentUploadStatus.Failed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { onRemoveAttachment(attachment.localUri) }) {
                    Text(chatString("chat_attachment_remove"))
                }
            }
            if (attachment.uploadStatus == AttachmentUploadStatus.Uploading) {
                LinearProgressIndicator(
                    progress = { attachment.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onCancelAttachmentUpload(attachment.localUri) }) {
                    Text(chatString("chat_attachment_cancel"))
                }
            }
            if (attachment.uploadStatus == AttachmentUploadStatus.Failed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onRetryAttachment(attachment.localUri) }) {
                        Text(chatString("chat_attachment_retry"))
                    }
                }
            }
        }
    }
}

@Composable
private fun citationDisplayTitle(citation: CitationItemUiModel): String =
    citation.title.ifBlank {
        citation.sourceName.ifBlank {
            citation.snippet.take(24).ifBlank { stringResource(R.string.chat_citation_title_fallback) }
        }
    }

@Composable
private fun citationScoreLabel(citation: CitationItemUiModel): String? {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val formattedScore =
        citation.score?.let { score ->
            NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = 3
                minimumFractionDigits = 0
            }.format(score)
        }
    return when {
        citation.scoreType.isNullOrBlank() && formattedScore == null -> null
        citation.scoreType.isNullOrBlank() -> formattedScore
        formattedScore == null -> citation.scoreType
        else -> stringResource(R.string.chat_score_summary, citation.scoreType, formattedScore)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(
    message: ChatMessageUiModel,
    onCopy: () -> Unit,
    onCopyCode: (String) -> Unit,
    onContinueGeneration: () -> Unit,
    onRegenerate: () -> Unit,
    onSuggestedQuestion: (String) -> Unit,
    onOpenCitationList: (ChatMessageUiModel) -> Unit,
    onOpenCitation: (CitationItemUiModel) -> Unit,
    onUpdateInteractiveDraft: (ChatMessageUiModel, String) -> Unit,
    onSubmitInteractiveResponse: (ChatMessageUiModel, String) -> Unit,
    onToggleReasoning: (String) -> Unit,
    showReasoningEntry: Boolean,
) {
    val shouldShowWaitingPlaceholder =
        message.role == ChatRole.AI &&
            message.phase == KoraFeedbackPhase.InFlightFirstByte &&
            message.markdown.isBlank() &&
            message.reasoning.isBlank()
    val failedAssistantBody = failedAssistantFeedbackBody(message)
    val shouldSuppressAssistantMarkdown =
        message.role == ChatRole.AI &&
            message.phase == KoraFeedbackPhase.ErrorRecoverable &&
            failedAssistantBody != null &&
            shouldHideFailedMarkdown(message)
    if (message.role == ChatRole.Human) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(ChatTestTags.messageCard(message.messageId)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Card(
                modifier = Modifier.widthIn(max = UserMessageMaxWidth),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    MarkdownMessage(
                        markdown = message.markdown,
                        onCopyCode = onCopyCode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    messageStatusText(message)?.let { statusText ->
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = messageStatusColor(message),
                            modifier = Modifier.testTag(ChatTestTags.messageError(message.messageId)),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MessageActionIconButton(
                    onClick = onCopy,
                    painter = painterResource(R.drawable.ic_chat_copy),
                    contentDescription = stringResource(R.string.chat_copy),
                    modifier = Modifier.testTag(ChatTestTags.messageCopyAction(message.messageId)),
                )
            }
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(ChatTestTags.messageCard(message.messageId)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (shouldShowWaitingPlaceholder) {
                AssistantWaitingBubble(
                    text = stringResource(R.string.chat_message_waiting_label),
                    modifier = Modifier.testTag("chat_message_waiting_${message.messageId}"),
                )
            } else if (message.phase == KoraFeedbackPhase.ErrorRecoverable && failedAssistantBody != null) {
                KoraInlineFeedbackCard(
                    phase = KoraFeedbackPhase.ErrorRecoverable,
                    title = stringResource(R.string.chat_message_failed),
                    body = failedAssistantBody,
                    actionHint = stringResource(R.string.chat_message_failed_hint),
                    testTag = "chat_message_error_${message.messageId}",
                )
            } else if (message.markdown.isNotBlank() && !shouldSuppressAssistantMarkdown) {
                MarkdownMessage(
                    markdown = message.markdown,
                    onCopyCode = onCopyCode,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showReasoningEntry && message.hasReasoning) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onToggleReasoning(message.messageId) },
                        modifier = Modifier.testTag("message-reasoning-toggle-${message.messageId}"),
                    ) {
                        Text(
                            if (message.isReasoningExpanded) {
                                stringResource(R.string.chat_message_reasoning_collapse)
                            } else {
                                stringResource(R.string.chat_message_reasoning_expand)
                            },
                        )
                    }
                    if (message.isReasoningExpanded) {
                        Text(
                            text = stringResource(R.string.chat_message_reasoning_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = message.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            message.interactiveCard?.let { card ->
                InteractiveCard(
                    message = message,
                    messageId = message.messageId,
                    card = card,
                    onDraftChanged = onUpdateInteractiveDraft,
                    onSubmit = onSubmitInteractiveResponse,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            messageStatusText(message)?.let { statusText ->
                KoraFeedbackLabel(
                    phase = message.phase,
                    text = statusText,
                    testTag = ChatTestTags.messageError(message.messageId),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (message.deliveryState == MessageDeliveryState.Stopped) {
                TextButton(onClick = onContinueGeneration) {
                    Text(stringResource(R.string.chat_continue_generation))
                }
            }
            MessageActionIconButton(
                onClick = onRegenerate,
                painter = painterResource(R.drawable.ic_chat_regenerate),
                contentDescription = stringResource(R.string.chat_regenerate),
                modifier = Modifier.testTag(ChatTestTags.messageRegenerateAction(message.messageId)),
            )
            MessageActionIconButton(
                onClick = onCopy,
                painter = painterResource(R.drawable.ic_chat_copy),
                contentDescription = stringResource(R.string.chat_copy),
                modifier = Modifier.testTag(ChatTestTags.messageCopyAction(message.messageId)),
            )
        }
        if (message.citations.isNotEmpty()) {
            TextButton(
                onClick = {
                    if (message.citations.size == 1) {
                        onOpenCitation(message.citations.first())
                    } else {
                        onOpenCitationList(message)
                    }
                },
                modifier = Modifier.testTag(ChatTestTags.citationSummary(message.messageId)),
            ) {
                Text(stringResource(R.string.chat_citations_title, message.citations.size))
            }
        }
        if (message.suggestedQuestions.isNotEmpty()) {
            Text(
                stringResource(R.string.chat_suggested_questions),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                message.suggestedQuestions.forEach { question ->
                    TextButton(onClick = { onSuggestedQuestion(question) }) {
                        Text(question)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionIconButton(
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AssistantWaitingBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.widthIn(max = 220.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun messageStatusText(message: ChatMessageUiModel): String? =
    when (message.phase) {
        KoraFeedbackPhase.InFlightFirstByte -> null
        KoraFeedbackPhase.InFlightStreaming -> stringResource(R.string.chat_message_streaming_label)
        KoraFeedbackPhase.ErrorRecoverable -> message.errorMessage ?: stringResource(R.string.chat_message_failed)
        KoraFeedbackPhase.Stopped -> message.errorMessage ?: stringResource(R.string.chat_message_stopped_continue)
        else -> null
    }

@Composable
private fun messageStatusColor(message: ChatMessageUiModel) =
    when (message.phase) {
        KoraFeedbackPhase.ErrorRecoverable -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

private fun failedAssistantFeedbackBody(message: ChatMessageUiModel): String? {
    if (message.phase != KoraFeedbackPhase.ErrorRecoverable) {
        return null
    }
    return when {
        !message.errorMessage.isNullOrBlank() -> message.errorMessage
        looksLikeStructuredErrorPayload(message.markdown) -> extractStructuredErrorMessage(message.markdown)
        else -> null
    }
}

private fun shouldHideFailedMarkdown(message: ChatMessageUiModel): Boolean =
    message.errorMessage != null || looksLikeStructuredErrorPayload(message.markdown)

private fun looksLikeStructuredErrorPayload(markdown: String): Boolean {
    val trimmed = markdown.trim()
    return trimmed.startsWith("{") &&
        trimmed.contains("\"error\"") &&
        trimmed.contains("\"message\"")
}

private fun extractStructuredErrorMessage(payload: String): String {
    val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.getOrNull(1)
    val codeMatch = Regex("\"code\"\\s*:\\s*\"?([^\"]+?)\"?(,|})").find(payload)?.groupValues?.getOrNull(1)
    return buildString {
        append(messageMatch ?: payload.trim())
        if (!codeMatch.isNullOrBlank()) {
            append(" (")
            append(codeMatch)
            append(")")
        }
    }
}

@Composable
internal fun CitationPreviewSheet(
    citation: CitationItemUiModel,
    onOpenCitation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("citation_preview_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_citation_preview_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(citationDisplayTitle(citation), style = MaterialTheme.typography.titleMedium)
        if (citation.sourceName.isNotBlank()) {
            Text(
                stringResource(R.string.chat_citation_preview_source, citation.sourceName),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            text = citation.snippet.ifBlank { stringResource(R.string.chat_citation_preview_empty) },
            style = MaterialTheme.typography.bodyMedium,
        )
        citationScoreLabel(citation)?.let { scoreLabel ->
            Text(scoreLabel, style = MaterialTheme.typography.labelMedium)
        }
        if (!citation.datasetId.isNullOrBlank() && !citation.collectionId.isNullOrBlank()) {
            Button(
                onClick = onOpenCitation,
                modifier = Modifier.testTag("citation_preview_open_context"),
            ) {
                Text(stringResource(R.string.chat_open_citation))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveCard(
    message: ChatMessageUiModel,
    messageId: String,
    card: InteractiveCardUiModel,
    onDraftChanged: (ChatMessageUiModel, String) -> Unit,
    onSubmit: (ChatMessageUiModel, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValues by rememberSaveable(messageId, card.fields) {
        mutableStateOf(card.fields.associate { it.id to it.value })
    }
    val canEdit = card.status == InteractiveCardStatus.Pending
    val canSubmit =
        canEdit &&
            when (card.kind) {
                InteractiveCardKind.UserSelect -> true
                else -> card.fields.all { !it.required || !fieldValues[it.id].isNullOrBlank() }
            }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag(ChatTestTags.interactiveCard(messageId)),
    ) {
            Text(
                text = card.kind.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            card.selectedOption?.takeIf { it.isNotBlank() }?.let { selected ->
                Text(selected, style = MaterialTheme.typography.bodyMedium)
            }
            if (card.options.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    card.options.forEach { option ->
                        TextButton(
                            onClick = { onSubmit(message, option) },
                            enabled = canEdit,
                            modifier = Modifier.testTag(ChatTestTags.interactiveOption(messageId, option)),
                        ) {
                            Text(option)
                        }
                    }
                }
            }
            if (card.fields.isNotEmpty()) {
                card.fields.forEach { field ->
                    OutlinedTextField(
                        value = fieldValues[field.id].orEmpty(),
                        onValueChange = { value ->
                            fieldValues = fieldValues.toMutableMap().apply { put(field.id, value) }
                            onDraftChanged(message, interactiveFieldValuesToJson(fieldValues))
                        },
                        label = { Text(field.label) },
                        enabled = canEdit,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(ChatTestTags.interactiveFieldInput(messageId, field.id)),
                    )
                }
            } else if (card.kind != InteractiveCardKind.UserSelect) {
                Text(card.selectedOption.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
            if (card.kind != InteractiveCardKind.UserSelect) {
                TextButton(
                    onClick = { onSubmit(message, interactiveFieldValuesToJson(fieldValues)) },
                    enabled = canSubmit,
                    modifier = Modifier.testTag(ChatTestTags.interactiveSubmit(messageId)),
                ) {
                    Text(stringResource(R.string.chat_send))
                }
            }
    }
}

private fun interactiveFieldValuesToJson(values: Map<String, String>): String =
    kotlinx.serialization.json.JsonObject(
        mapOf(
            "fieldValues" to
                kotlinx.serialization.json.JsonObject(
                    values.mapValues { (_, value) -> kotlinx.serialization.json.JsonPrimitive(value) },
                ),
        ),
    ).toString()
private val UserMessageMaxWidth = 320.dp
