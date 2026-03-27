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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lifuyue.kora.core.common.ChatRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    onFeedback: (ChatMessageUiModel, MessageFeedback) -> Unit,
    onRegenerate: (ChatMessageUiModel) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
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
                    text = "开始一个新对话，消息会在这里以 Markdown 和代码块形式渲染。",
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
                        onRegenerate = { onRegenerate(message) },
                        onFeedback = { feedback -> onFeedback(message, feedback) },
                    )
                }
            }
            OutlinedTextField(
                value = uiState.input,
                onValueChange = onInputChanged,
                label = { Text("输入消息") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.canStopGeneration) {
                    OutlinedButton(onClick = onStopGenerating) {
                        Text("停止生成")
                    }
                }
                Button(
                    onClick = onSend,
                    enabled = uiState.input.isNotBlank() && !uiState.isSending && !uiState.canStopGeneration,
                ) {
                    Text(if (uiState.isSending) "发送中..." else "发送")
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
    onRegenerate: () -> Unit,
    onFeedback: (MessageFeedback) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = if (message.role == ChatRole.Human) "你" else "Kora",
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
                            MessageDeliveryState.Streaming -> "生成中"
                            MessageDeliveryState.Failed -> message.errorMessage ?: "生成失败"
                            MessageDeliveryState.Stopped -> message.errorMessage ?: "已停止生成"
                            MessageDeliveryState.Sent -> ""
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        when (message.deliveryState) {
                            MessageDeliveryState.Failed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopy) {
                    Text("复制")
                }
                if (message.role == ChatRole.AI) {
                    TextButton(onClick = onRegenerate) {
                        Text("重新生成")
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
                    ) {
                        Text(if (message.feedback == MessageFeedback.Upvote) "取消赞" else "点赞")
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
                    ) {
                        Text(if (message.feedback == MessageFeedback.Downvote) "取消踩" else "点踩")
                    }
                }
            }
        }
    }
}
