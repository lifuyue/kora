package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ChatRole

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
) {
    val clipboardManager = LocalClipboardManager.current
    var activeCitationMessage by remember { mutableStateOf<ChatMessageUiModel?>(null) }
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
                                citation.title.ifBlank { citation.snippet.take(24) },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (citation.snippet.isNotBlank()) {
                                Text(citation.snippet, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (citation.scoreLabel.isNotBlank()) {
                                Text(citation.scoreLabel, style = MaterialTheme.typography.labelMedium)
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
            LazyColumn(
                modifier = Modifier.weight(1f),
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
                    )
                }
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
