package com.lifuyue.kora

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.feature.chat.ChatMessageUiModel
import com.lifuyue.kora.feature.chat.ChatScreen
import com.lifuyue.kora.feature.chat.MessageDeliveryState
import com.lifuyue.kora.feature.chat.MessageFeedback
import com.lifuyue.kora.testing.KoraTestOverrides
import kotlinx.coroutines.flow.MutableStateFlow

private const val EXTRA_OPEN_DEMO_CHAT = "com.lifuyue.kora.extra.OPEN_DEMO_CHAT"
private const val EXTRA_OPEN_DEBUG_SHELL = "com.lifuyue.kora.extra.OPEN_DEBUG_SHELL"
private const val DEMO_APP_ID = "demo-app"
private const val DEMO_CHAT_ID = "demo-chat"

internal fun installDebugDemoOverrides(intent: Intent?) {
    when {
        intent?.getBooleanExtra(EXTRA_OPEN_DEBUG_SHELL, false) == true -> {
            KoraTestOverrides.snapshotOverride = MutableStateFlow(createDemoSnapshot())
            KoraTestOverrides.shellRouteOverride = null
        }
        intent?.getBooleanExtra(EXTRA_OPEN_DEMO_CHAT, false) == true -> {
            KoraTestOverrides.snapshotOverride = MutableStateFlow(createDemoSnapshot())
            KoraTestOverrides.shellRouteOverride =
                object : KoraTestOverrides.ShellRouteOverride {
                    @Composable
                    override fun Render(snapshot: ConnectionSnapshot) {
                        DemoChatScreen()
                    }
                }
        }
        else -> {
            return
        }
    }
}

private fun createDemoSnapshot(): ConnectionSnapshot {
    return ConnectionSnapshot(
        serverBaseUrl = "https://demo.fastgpt.local/api/",
        apiKey = "fastgpt-demo-key",
        selectedAppId = DEMO_APP_ID,
        onboardingCompleted = true,
    )
}

@Composable
private fun DemoChatScreen() {
    val stopGeneratingMessage = stringResource(R.string.debug_demo_stop_generating)
    val continueGenerationMessage = stringResource(R.string.debug_demo_continue_generation)
    val regeneratedReplyTitle = stringResource(R.string.debug_demo_regenerated_reply)
    val receivedTemplate = stringResource(R.string.debug_demo_received_template)
    val messages =
        remember {
            mutableStateListOf(
                ChatMessageUiModel(
                    messageId = "demo-user-1",
                    chatId = DEMO_CHAT_ID,
                    appId = DEMO_APP_ID,
                    role = ChatRole.Human,
                    markdown = "帮我写一个 Kotlin data class，并展示代码块效果。",
                ),
                ChatMessageUiModel(
                    messageId = "demo-ai-1",
                    chatId = DEMO_CHAT_ID,
                    appId = DEMO_APP_ID,
                    role = ChatRole.AI,
                    markdown =
                        """
                        ## Kotlin 示例

                        这是一个带代码块的聊天页面示例：

                        ```kotlin
                        data class UserProfile(
                            val id: String,
                            val nickname: String,
                            val isPro: Boolean,
                        )
                        ```

                        你也可以继续输入消息，看发送区和操作条的样子。
                        """.trimIndent(),
                ),
            )
        }
    var input by remember { mutableStateOf("给我再来一个协程示例") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ChatScreen(
        uiState =
            com.lifuyue.kora.feature.chat.ChatUiState(
                appId = DEMO_APP_ID,
                chatId = DEMO_CHAT_ID,
                input = input,
                errorMessage = errorMessage,
                messages = messages.toList(),
            ),
        onBack = {},
        onInputChanged = { input = it },
        onSend = {
            val prompt = input.trim()
            if (prompt.isEmpty()) return@ChatScreen
            messages +=
                ChatMessageUiModel(
                    messageId = "demo-user-${messages.size + 1}",
                    chatId = DEMO_CHAT_ID,
                    appId = DEMO_APP_ID,
                    role = ChatRole.Human,
                    markdown = prompt,
                )
            messages +=
                ChatMessageUiModel(
                    messageId = "demo-ai-${messages.size + 1}",
                    chatId = DEMO_CHAT_ID,
                    appId = DEMO_APP_ID,
                    role = ChatRole.AI,
                    markdown =
                        """
                        ${receivedTemplate.format(prompt)}

                        ```kotlin
                        suspend fun loadAnswer(): String {
                            return "Kora 演示回复"
                        }
                        ```
                        """.trimIndent(),
                    deliveryState = MessageDeliveryState.Sent,
                )
            input = ""
            errorMessage = null
        },
        onStopGenerating = {
            errorMessage = stopGeneratingMessage
        },
        onContinueGeneration = {
            errorMessage = continueGenerationMessage
        },
        onFeedback = { message, feedback ->
            val index = messages.indexOfFirst { it.messageId == message.messageId }
            if (index >= 0) {
                messages[index] = messages[index].copy(feedback = feedback)
            }
        },
        onRegenerate = { message ->
            val index = messages.indexOfFirst { it.messageId == message.messageId }
            if (index >= 0) {
                messages[index] =
                    messages[index].copy(
                        markdown =
                            """
                            ## $regeneratedReplyTitle

                            ```kotlin
                            class DemoRegeneratedReply
                            ```
                            """.trimIndent(),
                        feedback = MessageFeedback.None,
                    )
            }
            errorMessage = null
        },
    )
}
