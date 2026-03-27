package com.lifuyue.kora.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        ChatRoute(navController = navController, onBack = { navController.popBackStack() })
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
        onSelectFolderFilter = viewModel::selectFolder,
        onSelectTagFilter = viewModel::selectTag,
        onOpenConversation = { chatId -> onOpenConversation(viewModel.appId, chatId) },
        onNewConversation = { onNewConversation(viewModel.appId) },
        onDeleteConversation = viewModel::deleteConversation,
        onRenameConversation = viewModel::renameConversation,
        onTogglePin = viewModel::togglePin,
        onClearConversations = viewModel::clearConversations,
        onCreateFolder = viewModel::createFolder,
        onRenameFolder = viewModel::renameFolder,
        onDeleteFolder = viewModel::deleteFolder,
        onCreateTag = viewModel::createTag,
        onRenameTag = viewModel::renameTag,
        onDeleteTag = viewModel::deleteTag,
        onMoveConversationToFolder = viewModel::moveConversation,
        onSetConversationTags = viewModel::setConversationTags,
    )
}

@Composable
private fun ChatRoute(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    appSelectorViewModel: AppSelectorViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val appSelectorUiState = appSelectorViewModel.uiState.collectAsStateWithLifecycle()
    var showAppSelector by remember { mutableStateOf(false) }
    ChatScreen(
        uiState = uiState.value,
        appSelectorUiState = appSelectorUiState.value,
        showAppSelector = showAppSelector,
        onBack = onBack,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::send,
        onStopGenerating = viewModel::stopGeneration,
        onContinueGeneration = viewModel::continueGeneration,
        onFeedback = viewModel::updateFeedback,
        onRegenerate = viewModel::regenerate,
        onOpenAppSelector = { showAppSelector = true },
        onDismissAppSelector = { showAppSelector = false },
        onSwitchApp = { appId ->
            showAppSelector = false
            appSelectorViewModel.switchApp(appId) { selected ->
                navController.navigate(ChatRoutes.conversations(selected)) {
                    popUpTo(ChatRoutes.conversations) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        onSuggestedQuestion = {
            viewModel.updateInput(it)
            viewModel.send()
        },
        onOpenCitation = { citation ->
            if (!citation.datasetId.isNullOrBlank() && !citation.collectionId.isNullOrBlank()) {
                navController.navigate("knowledge/datasets/${citation.datasetId}/collections/${citation.collectionId}")
            }
        },
    )
}
