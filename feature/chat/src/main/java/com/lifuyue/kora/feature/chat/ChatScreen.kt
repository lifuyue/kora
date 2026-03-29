package com.lifuyue.kora.feature.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifuyue.kora.core.common.ChatRole
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    appSelectorUiState: AppSelectorUiState = AppSelectorUiState(),
    conversationBrowserUiState: ConversationListUiState = ConversationListUiState(),
    showAppSelector: Boolean = false,
    showConversationBrowser: Boolean = false,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onStartSpeechInput: () -> Unit = {},
    onStopSpeechInput: () -> Unit = {},
    onCancelSpeechInput: () -> Unit = {},
    onSend: () -> Unit,
    onPickImage: () -> Unit = {},
    onPickFile: () -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    onRetryAttachment: (String) -> Unit = {},
    onCancelAttachmentUpload: (String) -> Unit = {},
    onStopGenerating: () -> Unit,
    onContinueGeneration: () -> Unit,
    onFeedback: (ChatMessageUiModel, MessageFeedback) -> Unit,
    onRegenerate: (ChatMessageUiModel) -> Unit,
    onPlayMessage: (String, String) -> Unit = { _, _ -> },
    onPausePlayback: () -> Unit = {},
    onStopPlayback: () -> Unit = {},
    onOpenAppSelector: () -> Unit = {},
    onDismissAppSelector: () -> Unit = {},
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
    onSwitchApp: (String) -> Unit = {},
    onOpenAppDetail: () -> Unit = {},
    onSuggestedQuestion: (String) -> Unit = {},
    onOpenCitation: (CitationItemUiModel) -> Unit = {},
    onUpdateInteractiveDraft: (ChatMessageUiModel, String) -> Unit = { _, _ -> },
    onSubmitInteractiveResponse: (ChatMessageUiModel, String) -> Unit = { _, _ -> },
) {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pageBackground = MaterialTheme.colorScheme.background
    val errorContainer = if (isLightTheme) Color(0xFFFFE2DE) else Color(0xFF351A1A)
    val errorText = if (isLightTheme) Color(0xFF8C2E27) else Color(0xFFFFC9C5)
    var activeCitationMessage by remember { mutableStateOf<ChatMessageUiModel?>(null) }
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

    if (showAppSelector) {
        ModalBottomSheet(onDismissRequest = onDismissAppSelector) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.chat_switch_app_title), style = MaterialTheme.typography.titleLarge)
                appSelectorUiState.items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                if (item.intro.isNotBlank()) {
                                    Text(item.intro, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            TextButton(onClick = { onSwitchApp(item.appId) }) {
                                Text(
                                    stringResource(
                                        if (appSelectorUiState.currentAppId == item.appId) {
                                            R.string.chat_switch_app_current
                                        } else {
                                            R.string.chat_switch_app_action
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onOpenAppDetail, enabled = appSelectorUiState.currentAppId != null) {
                    Text(stringResource(R.string.chat_open_app_detail))
                }
                appSelectorUiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
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
                            TextButton(onClick = { onOpenCitation(citation) }) {
                                Text(stringResource(R.string.chat_open_citation))
                            }
                        }
                    }
                }
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = pageBackground,
        topBar = {
            ChatGeminiTopBar(
                onOpenDrawer = onOpenDrawer,
                onOpenAppSelector = onOpenAppSelector,
            )
        },
        bottomBar = {
            ChatGeminiComposer(
                uiState = uiState,
                onInputChanged = onInputChanged,
                onSend = onSend,
                onStartSpeechInput = onStartSpeechInput,
                onStopSpeechInput = onStopSpeechInput,
                onPickImage = onPickImage,
                onPickFile = onPickFile,
                onOpenQuickSettings = onOpenQuickSettings,
                onToggleAttachmentActions = {
                    if (uiState.attachmentConfig.hasAnySelectionType) {
                        onPickFile()
                    }
                },
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
                            onFeedback = { feedback -> onFeedback(message, feedback) },
                            ttsPlaybackState = uiState.ttsPlaybackState,
                            onPlayMessage = onPlayMessage,
                            onPausePlayback = onPausePlayback,
                            onStopPlayback = onStopPlayback,
                            onSuggestedQuestion = onSuggestedQuestion,
                            onOpenCitation = onOpenCitation,
                            onUpdateInteractiveDraft = onUpdateInteractiveDraft,
                            onSubmitInteractiveResponse = onSubmitInteractiveResponse,
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
                    onPickImage = onPickImage,
                    onPickFile = onPickFile,
                    onRemoveAttachment = onRemoveAttachment,
                    onRetryAttachment = onRetryAttachment,
                    onCancelAttachmentUpload = onCancelAttachmentUpload,
                )
            }
            if (
                uiState.speechInputState.status != SpeechInputStatus.Idle ||
                uiState.speechInputState.transcript.isNotBlank() ||
                !uiState.speechInputState.errorMessage.isNullOrBlank()
            ) {
                SpeechInputComposer(
                    state = uiState.speechInputState,
                    onStart = onStartSpeechInput,
                    onStop = onStopSpeechInput,
                    onCancel = onCancelSpeechInput,
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
    onOpenAppSelector: () -> Unit,
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
        GeminiProfileAvatar(onClick = onOpenAppSelector)
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
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.chat_switch_app_title),
            tint = if (isLightTheme) Color(0xFF5B616C) else Color(0xFFE1E1E1),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ChatGeminiEmptyState(
    modifier: Modifier = Modifier,
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val suggestionItems: List<Pair<ImageVector, String>> =
        listOf(
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_image),
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_music),
            Icons.Filled.Add to stringResource(R.string.chat_suggestion_video),
            Icons.Filled.Person to stringResource(R.string.chat_suggestion_study),
            Icons.Filled.Search to stringResource(R.string.chat_suggestion_energy),
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.wrapContentWidth(),
        color = if (isLightTheme) Color.White else Color(0xFF1A1A1C),
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
                tint = if (isLightTheme) Color(0xFF6A717D) else Color(0xFFC7C7C7),
                modifier = Modifier.size(18.dp),
            )
            Text(
                label,
                color = if (isLightTheme) Color(0xFF2A2D33) else Color(0xFFD5D5D5),
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
    onStartSpeechInput: () -> Unit,
    onStopSpeechInput: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onToggleAttachmentActions: () -> Unit,
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Surface(
        color = if (isLightTheme) Color(0xFFFCFCFD) else Color(0xFF252628),
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
            BasicTextField(
                value = uiState.input,
                onValueChange = onInputChanged,
                modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.CHAT_INPUT),
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
                    contentDescription = stringResource(R.string.chat_composer_add_attachment),
                    onClick = onToggleAttachmentActions,
                    modifier = Modifier.testTag(ChatTestTags.CHAT_ATTACHMENT_TRIGGER_BUTTON),
                )
                GeminiComposerIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.chat_quick_settings),
                    onClick = onOpenQuickSettings,
                    modifier = Modifier.testTag(ChatTestTags.CHAT_QUICK_SETTINGS_BUTTON),
                )
                Spacer(modifier = Modifier.weight(1f))
                GeminiComposerPainterButton(
                    painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
                    contentDescription = stringResource(R.string.chat_composer_voice),
                    onClick = {
                        if (uiState.speechInputState.status == SpeechInputStatus.Recording ||
                            uiState.speechInputState.status == SpeechInputStatus.Recognizing
                        ) {
                            onStopSpeechInput()
                        } else {
                            onStartSpeechInput()
                        }
                    },
                    modifier = Modifier.testTag(ChatTestTags.CHAT_MIC_BUTTON),
                )
            }
        }
    }
}

@Composable
private fun GeminiComposerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                        Color(0xFFF0F2F5)
                    } else {
                        Color.White.copy(alpha = 0.06f)
                    },
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun GeminiComposerPainterButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                        Color(0xFFF0F2F5)
                    } else {
                        Color.White.copy(alpha = 0.06f)
                    },
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun SpeechInputComposer(
    state: SpeechInputUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.CHAT_SPEECH_STATUS),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
                Text(
                    text =
                        when (state.status) {
                        SpeechInputStatus.Recording -> appString("chat_speech_recording")
                        SpeechInputStatus.Recognizing -> appString("chat_speech_recognizing")
                        SpeechInputStatus.Error -> state.errorMessage ?: appString("chat_error_speech_failed")
                        SpeechInputStatus.Idle -> appString("chat_speech_start")
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            if (state.transcript.isNotBlank()) {
                Text(text = state.transcript, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state.status) {
                    SpeechInputStatus.Recording,
                    SpeechInputStatus.Recognizing,
                    -> {
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.testTag(ChatTestTags.CHAT_SPEECH_STOP),
                        ) {
                            Text(appString("chat_speech_stop"))
                        }
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag(ChatTestTags.CHAT_SPEECH_CANCEL),
                        ) {
                            Text(appString("chat_speech_cancel"))
                        }
                    }
                    SpeechInputStatus.Error,
                    SpeechInputStatus.Idle,
                    -> {
                        OutlinedButton(
                            onClick = onStart,
                            modifier = Modifier.testTag(ChatTestTags.CHAT_MIC_BUTTON),
                        ) {
                            Text(
                                if (state.status == SpeechInputStatus.Error) {
                                    appString("chat_speech_retry")
                                } else {
                                    appString("chat_speech_start")
                                },
                            )
                        }
                    }
                }
            }
        }
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

@Composable
private fun MessageCard(
    message: ChatMessageUiModel,
    onCopy: () -> Unit,
    onCopyCode: (String) -> Unit,
    onContinueGeneration: () -> Unit,
    onRegenerate: () -> Unit,
    onFeedback: (MessageFeedback) -> Unit,
    ttsPlaybackState: TtsPlaybackUiState,
    onPlayMessage: (String, String) -> Unit,
    onPausePlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSuggestedQuestion: (String) -> Unit,
    onOpenCitation: (CitationItemUiModel) -> Unit,
    onUpdateInteractiveDraft: (ChatMessageUiModel, String) -> Unit,
    onSubmitInteractiveResponse: (ChatMessageUiModel, String) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(ChatTestTags.messageCard(message.messageId)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text =
                    stringResource(
                        if (message.role == ChatRole.Human) {
                            R.string.chat_message_role_you
                        } else {
                            R.string.chat_message_role_kora
                        },
                    ),
                style = MaterialTheme.typography.titleSmall,
            )
            MarkdownMessage(
                markdown = message.markdown,
                onCopyCode = onCopyCode,
                modifier = Modifier.fillMaxWidth(),
            )
            if (message.reasoning.isNotBlank()) {
                Text(
                    text = message.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
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
            if (message.deliveryState != MessageDeliveryState.Sent) {
                Text(
                    text =
                        when (message.deliveryState) {
                            MessageDeliveryState.Streaming -> stringResource(R.string.chat_message_streaming)
                            MessageDeliveryState.Failed ->
                                message.errorMessage ?: stringResource(R.string.chat_message_failed)
                            MessageDeliveryState.Stopped ->
                                message.errorMessage ?: stringResource(R.string.chat_message_stopped)
                            MessageDeliveryState.Sent -> ""
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        when (message.deliveryState) {
                            MessageDeliveryState.Failed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        },
                    modifier = Modifier.testTag(ChatTestTags.messageError(message.messageId)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.testTag(ChatTestTags.messageCopyAction(message.messageId)),
                ) {
                    Text(stringResource(R.string.chat_copy))
                }
                if (message.role == ChatRole.AI) {
                    if (message.deliveryState == MessageDeliveryState.Stopped) {
                        TextButton(onClick = onContinueGeneration) {
                            Text(stringResource(R.string.chat_continue_generation))
                        }
                    }
                    TextButton(
                        onClick = onRegenerate,
                        modifier = Modifier.testTag(ChatTestTags.messageRegenerateAction(message.messageId)),
                    ) {
                        Text(stringResource(R.string.chat_regenerate))
                    }
                    TextButton(
                        onClick = {
                            onFeedback(
                                if (message.feedback == MessageFeedback.Upvote) {
                                    MessageFeedback.None
                                } else {
                                    MessageFeedback.Upvote
                                },
                            )
                        },
                        modifier = Modifier.testTag(ChatTestTags.messageUpvoteAction(message.messageId)),
                    ) {
                        Text(
                            stringResource(
                                if (message.feedback == MessageFeedback.Upvote) {
                                    R.string.chat_cancel_upvote
                                } else {
                                    R.string.chat_upvote
                                },
                            ),
                        )
                    }
                    TextButton(
                        onClick = {
                            onFeedback(
                                if (message.feedback == MessageFeedback.Downvote) {
                                    MessageFeedback.None
                                } else {
                                    MessageFeedback.Downvote
                                },
                            )
                        },
                        modifier = Modifier.testTag(ChatTestTags.messageDownvoteAction(message.messageId)),
                    ) {
                        Text(
                            stringResource(
                                if (message.feedback == MessageFeedback.Downvote) {
                                    R.string.chat_cancel_downvote
                                } else {
                                    R.string.chat_downvote
                                },
                            ),
                        )
                    }
                    TextButton(
                        onClick = { onPlayMessage(message.messageId, message.markdown) },
                        modifier = Modifier.testTag(ChatTestTags.messageTtsAction(message.messageId)),
                    ) {
                        Text(appString("chat_tts_play"))
                    }
                    if (ttsPlaybackState.messageId == message.messageId && ttsPlaybackState.status == TtsPlaybackStatus.Playing) {
                        TextButton(
                            onClick = onPausePlayback,
                            modifier = Modifier.testTag(ChatTestTags.messageTtsPauseAction(message.messageId)),
                        ) {
                            Text(appString("chat_tts_pause"))
                        }
                        TextButton(
                            onClick = onStopPlayback,
                            modifier = Modifier.testTag(ChatTestTags.messageTtsStopAction(message.messageId)),
                        ) {
                            Text(appString("chat_tts_stop"))
                        }
                    }
                }
            }
            if (message.citations.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onOpenCitation(message.citations.first()) },
                    modifier = Modifier.testTag(ChatTestTags.citationSummary(message.messageId)),
                ) {
                    Text(stringResource(R.string.chat_citations_title, message.citations.size))
                }
            }
            if (message.suggestedQuestions.isNotEmpty()) {
                Text(stringResource(R.string.chat_suggested_questions), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    message.suggestedQuestions.forEach { question ->
                        OutlinedButton(onClick = { onSuggestedQuestion(question) }) {
                            Text(question)
                        }
                    }
                }
            }
        }
    }
}

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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.testTag(ChatTestTags.interactiveCard(messageId)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                ) {
                        card.options.forEach { option ->
                            OutlinedButton(
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
                Button(
                    onClick = { onSubmit(message, interactiveFieldValuesToJson(fieldValues)) },
                    enabled = canSubmit,
                    modifier = Modifier.testTag(ChatTestTags.interactiveSubmit(messageId)),
                ) {
                    Text(stringResource(R.string.chat_send))
                }
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
