package com.lifuyue.kora.feature.chat

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object ChatRoutes {
    const val conversations = "chat/{appId}"
    const val thread = "chat/thread/{appId}?chatId={chatId}"

    fun conversations(appId: String): String = "chat/$appId"

    fun thread(appId: String, chatId: String? = null): String =
        if (chatId == null) {
            "chat/thread/$appId?chatId="
        } else {
            "chat/thread/$appId?chatId=$chatId"
        }
}

fun NavGraphBuilder.chatGraph(navController: NavController) {
    composable(
        route = ChatRoutes.conversations,
        arguments = listOf(navArgument("appId") { type = NavType.StringType }),
    ) {
        ConversationListRoute(
            onOpenConversation = { appId, chatId ->
                navController.navigate(ChatRoutes.thread(appId, chatId))
            },
            onNewConversation = { appId ->
                navController.navigate(ChatRoutes.thread(appId))
            },
        )
    }
    composable(
        route = ChatRoutes.thread,
        arguments =
            listOf(
                navArgument("appId") { type = NavType.StringType },
                navArgument("chatId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        ChatRoute(onBack = { navController.popBackStack() })
    }
}

@Composable
private fun ConversationListRoute(
    onOpenConversation: (String, String) -> Unit,
    onNewConversation: (String) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    ConversationListScreen(
        uiState = uiState.value,
        onQueryChanged = viewModel::updateQuery,
        onOpenConversation = { chatId -> onOpenConversation(viewModel.appId, chatId) },
        onNewConversation = { onNewConversation(viewModel.appId) },
        onDeleteConversation = viewModel::deleteConversation,
        onRenameConversation = viewModel::renameConversation,
        onTogglePin = viewModel::togglePin,
        onClearConversations = viewModel::clearConversations,
    )
}

@Composable
private fun ChatRoute(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        uiState = uiState.value,
        onBack = onBack,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::send,
        onStopGenerating = viewModel::stopGeneration,
        onFeedback = viewModel::updateFeedback,
        onRegenerate = viewModel::regenerate,
    )
}
