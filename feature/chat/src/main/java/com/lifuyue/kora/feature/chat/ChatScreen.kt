package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ChatRole
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    appSelectorUiState: AppSelectorUiState = AppSelectorUiState(),
    showAppSelector: Boolean = false,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    onContinueGeneration: () -> Unit,
    onFeedback: (ChatMessageUiModel, MessageFeedback) -> Unit,
    onRegenerate: (ChatMessageUiModel) -> Unit,
    onOpenAppSelector: () -> Unit = {},
    onDismissAppSelector: () -> Unit = {},
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.chat_title))
                        TextButton(onClick = onOpenAppSelector) {
                            Text(appSelectorUiState.currentAppName.ifBlank { uiState.appId })
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.chat_back))
                    }
                },
                actions = {
                    TextButton(onClick = onOpenAppDetail) {
                        Text(stringResource(R.string.chat_capabilities))
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
                    .padding(16.dp),
        ) {
            if (uiState.messages.isEmpty()) {
                Text(
                    text = uiState.welcomeText ?: stringResource(R.string.chat_empty_welcome),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            if (uiState.isInitialLoading) {
                ChatLoadingSkeleton(modifier = Modifier.weight(1f).testTag(ChatTestTags.CHAT_SKELETON))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).testTag(ChatTestTags.CHAT_LIST),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            onSuggestedQuestion = onSuggestedQuestion,
                            onOpenCitation = {
                                activeCitationMessage = message
                            },
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
            OutlinedTextField(
                value = uiState.input,
                onValueChange = onInputChanged,
                label = { Text(stringResource(R.string.chat_input_label)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth().testTag(ChatTestTags.CHAT_INPUT),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.canStopGeneration) {
                    OutlinedButton(onClick = onStopGenerating) {
                        Text(stringResource(R.string.chat_stop_generation))
                    }
                }
                Button(
                    onClick = onSend,
                    enabled = uiState.input.isNotBlank() && !uiState.isSending && !uiState.canStopGeneration,
                ) {
                    Text(
                        stringResource(
                            if (uiState.isSending) {
                                R.string.chat_sending
                            } else {
                                R.string.chat_send
                            },
                        ),
                    )
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
