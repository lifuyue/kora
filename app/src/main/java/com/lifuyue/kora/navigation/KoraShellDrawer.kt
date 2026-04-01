package com.lifuyue.kora.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifuyue.kora.feature.chat.ChatDrawerContent
import com.lifuyue.kora.feature.chat.ConversationListUiState
import com.lifuyue.kora.feature.chat.ConversationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
internal fun KoraShellDrawer(
    currentAppId: String?,
    onNewConversation: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenKnowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = rememberConversationRepository(LocalContext.current)
    var query by remember(currentAppId) { mutableStateOf("") }
    val conversations by remember(currentAppId, repository) {
        currentAppId?.let(repository::observeConversations) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(currentAppId, repository) {
        currentAppId?.let { appId ->
            runCatching { repository.refreshConversations(appId) }
        }
    }

    val filteredItems =
        remember(conversations, query) {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) {
                conversations
            } else {
                conversations.filter { item ->
                    item.title.contains(trimmedQuery, ignoreCase = true) ||
                        item.preview.contains(trimmedQuery, ignoreCase = true)
                }
            }
        }

    ChatDrawerContent(
        uiState = ConversationListUiState(query = query, items = filteredItems),
        onQueryChanged = { query = it },
        onNewConversation = onNewConversation,
        onOpenConversation = onOpenConversation,
        onOpenKnowledge = onOpenKnowledge,
        modifier = modifier,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ShellDrawerEntryPoint {
    fun conversationRepository(): ConversationRepository
}

@Composable
private fun rememberConversationRepository(context: Context): ConversationRepository =
    remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ShellDrawerEntryPoint::class.java,
        ).conversationRepository()
    }
