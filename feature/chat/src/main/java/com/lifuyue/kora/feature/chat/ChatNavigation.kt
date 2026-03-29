package com.lifuyue.kora.feature.chat

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.core.content.ContextCompat
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

object ChatRoutes {
    const val CONVERSATIONS = "chat/{appId}"
    const val THREAD = "chat/thread/{appId}?chatId={chatId}"
    const val APP_DETAIL = "chat/app/{appId}?chatId={chatId}"
    const val APP_ANALYTICS = "chat/app/{appId}/analytics"

    fun conversations(appId: String): String = "chat/$appId"

    fun thread(
        appId: String,
        chatId: String? = null,
    ): String =
        if (chatId == null) {
            "chat/thread/$appId?chatId="
        } else {
            "chat/thread/$appId?chatId=$chatId"
        }

    fun appDetail(
        appId: String,
        chatId: String? = null,
    ): String =
        if (chatId == null) {
            "chat/app/$appId?chatId="
        } else {
            "chat/app/$appId?chatId=$chatId"
        }

    fun appAnalytics(appId: String): String = "chat/app/$appId/analytics"
}

fun NavGraphBuilder.chatGraph(
    navController: NavController,
    onOpenQuickSettings: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    composable(
        route = ChatRoutes.CONVERSATIONS,
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
        route = ChatRoutes.THREAD,
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
        ChatRoute(
            navController = navController,
            onOpenQuickSettings = onOpenQuickSettings,
            onOpenDrawer = onOpenDrawer,
        )
    }
    composable(
        route = ChatRoutes.APP_DETAIL,
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
        AppDetailRoute(
            onBack = { navController.popBackStack() },
            onOpenAnalytics = { appId -> navController.navigate(ChatRoutes.appAnalytics(appId)) },
        )
    }
    composable(
        route = ChatRoutes.APP_ANALYTICS,
        arguments = listOf(navArgument("appId") { type = NavType.StringType }),
    ) {
        val viewModel: AppAnalyticsViewModel = hiltViewModel()
        val uiState = viewModel.uiState.collectAsStateWithLifecycle()
        AppAnalyticsScreen(
            uiState = uiState.value,
            onRangeChanged = viewModel::updateRange,
        )
    }
}

@Composable
private fun ConversationListRoute(
    onOpenConversation: (String, String) -> Unit,
    onNewConversation: (String) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val useDualPane = rememberChatDualPaneEnabled()
    if (useDualPane && uiState.value.items.isNotEmpty()) {
        AdaptiveChatScaffold(
            isExpanded = true,
            conversationPane = {
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
                    onSetArchived = viewModel::setArchived,
                    onClearConversations = viewModel::clearConversations,
                    onCreateFolder = viewModel::createFolder,
                    onRenameFolder = viewModel::renameFolder,
                    onDeleteFolder = viewModel::deleteFolder,
                    onCreateTag = viewModel::createTag,
                    onRenameTag = viewModel::renameTag,
                    onDeleteTag = viewModel::deleteTag,
                    onMoveConversationToFolder = viewModel::moveConversation,
                    onSetConversationTags = viewModel::setConversationTags,
                    onToggleShowArchived = viewModel::toggleShowArchived,
                )
            },
            detailPane = {
                PlaceholderDetailPane(
                    title = appString("adaptive_chat_placeholder_title"),
                    body = appString("adaptive_chat_placeholder_body"),
                )
            },
        )
    } else {
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
            onSetArchived = viewModel::setArchived,
            onClearConversations = viewModel::clearConversations,
            onCreateFolder = viewModel::createFolder,
            onRenameFolder = viewModel::renameFolder,
            onDeleteFolder = viewModel::deleteFolder,
            onCreateTag = viewModel::createTag,
            onRenameTag = viewModel::renameTag,
            onDeleteTag = viewModel::deleteTag,
            onMoveConversationToFolder = viewModel::moveConversation,
            onSetConversationTags = viewModel::setConversationTags,
            onToggleShowArchived = viewModel::toggleShowArchived,
        )
    }
}

@Composable
private fun ChatRoute(
    navController: NavController,
    onOpenQuickSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    appSelectorViewModel: AppSelectorViewModel = hiltViewModel(),
    conversationListViewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val appSelectorUiState = appSelectorViewModel.uiState.collectAsStateWithLifecycle()
    val conversationBrowserUiState = conversationListViewModel.uiState.collectAsStateWithLifecycle()
    var showAppSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recordAudioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.startSpeechInput()
            } else {
                viewModel.onSpeechPermissionDenied()
            }
        }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri>? ->
            if (!uris.isNullOrEmpty()) {
                viewModel.addAttachments(uris)
            }
        }
    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri>? ->
            if (!uris.isNullOrEmpty()) {
                viewModel.addAttachments(uris)
            }
        }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    viewModel.onHostStopped()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val handleBack = {
        if (!navController.popBackStack()) {
            Unit
        }
    }
    BackHandler(onBack = handleBack)
    ChatScreen(
        uiState = uiState.value,
        appSelectorUiState = appSelectorUiState.value,
        conversationBrowserUiState = conversationBrowserUiState.value,
        showAppSelector = showAppSelector,
        onBack = handleBack,
        onInputChanged = viewModel::updateInput,
        onStartSpeechInput = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startSpeechInput()
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onStopSpeechInput = viewModel::stopSpeechInput,
        onCancelSpeechInput = viewModel::cancelSpeechInput,
        onSend = viewModel::send,
        onPickImage = {
            val state = uiState.value
            val mimeTypes = state.attachmentConfig.allowedMimeTypes(AttachmentKind.Image)
            if (canLaunchAttachmentPicker(state.attachmentConfig, state.attachments.size, AttachmentKind.Image)) {
                imagePickerLauncher.launch(mimeTypes)
            }
        },
        onPickFile = {
            val state = uiState.value
            val mimeTypes = state.attachmentConfig.allowedMimeTypes(AttachmentKind.File)
            if (canLaunchAttachmentPicker(state.attachmentConfig, state.attachments.size, AttachmentKind.File)) {
                filePickerLauncher.launch(mimeTypes)
            }
        },
        onRemoveAttachment = viewModel::removeAttachment,
        onRetryAttachment = viewModel::retryAttachment,
        onCancelAttachmentUpload = viewModel::cancelAttachmentUpload,
        onStopGenerating = viewModel::stopGeneration,
        onContinueGeneration = viewModel::continueGeneration,
        onFeedback = viewModel::updateFeedback,
        onRegenerate = viewModel::regenerate,
        onPlayMessage = viewModel::playMessage,
        onPausePlayback = viewModel::pausePlayback,
        onStopPlayback = viewModel::stopPlayback,
        onOpenAppSelector = { showAppSelector = true },
        onDismissAppSelector = { showAppSelector = false },
        onOpenDrawer = onOpenDrawer,
        onOpenQuickSettings = onOpenQuickSettings,
        onSwitchApp = { appId ->
            showAppSelector = false
            appSelectorViewModel.switchApp(appId) { selected ->
                navController.navigate(ChatRoutes.thread(selected)) {
                    popUpTo(ChatRoutes.CONVERSATIONS) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        onOpenAppDetail = {
            navController.navigate(ChatRoutes.appDetail(uiState.value.appId, uiState.value.chatId))
        },
        onSuggestedQuestion = {
            viewModel.updateInput(it)
            viewModel.send()
        },
        onUpdateInteractiveDraft = viewModel::updateInteractiveDraft,
        onSubmitInteractiveResponse = viewModel::submitInteractiveResponse,
        onOpenCitation = { citation ->
            if (!citation.datasetId.isNullOrBlank() && !citation.collectionId.isNullOrBlank()) {
                navController.navigate(
                    knowledgeChunkRoute(
                        datasetId = citation.datasetId,
                        collectionId = citation.collectionId,
                        dataId = citation.dataId,
                    ),
                )
            }
        },
    )
}

@Composable
private fun AppDetailRoute(
    onBack: () -> Unit,
    onOpenAnalytics: (String) -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    AppDetailScreen(
        uiState = uiState.value,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenAnalytics = { onOpenAnalytics(uiState.value.appId) },
    )
}

private fun knowledgeChunkRoute(
    datasetId: String,
    collectionId: String,
    dataId: String?,
): String =
    if (dataId.isNullOrBlank()) {
        "knowledge/datasets/$datasetId/collections/$collectionId?dataId="
    } else {
        "knowledge/datasets/$datasetId/collections/$collectionId?dataId=$dataId"
    }
