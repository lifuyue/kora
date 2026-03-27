package com.lifuyue.kora.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConversationListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val conversationRepository: ConversationRepository,
    ) : ViewModel() {
        val appId: String = checkNotNull(savedStateHandle["appId"])
        private val query = MutableStateFlow("")
        private val isRefreshing = MutableStateFlow(false)

        val uiState: StateFlow<ConversationListUiState> =
            combine(
                query,
                isRefreshing,
                conversationRepository.observeConversations(appId),
            ) { currentQuery, refreshing, items ->
                val filtered =
                    if (currentQuery.isBlank()) {
                        items
                    } else {
                        items.filter {
                            it.title.contains(currentQuery, ignoreCase = true) ||
                                it.preview.contains(currentQuery, ignoreCase = true)
                        }
                    }
                ConversationListUiState(
                    query = currentQuery,
                    items = filtered,
                    isRefreshing = refreshing,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationListUiState())

        init {
            refresh()
        }

        fun updateQuery(value: String) {
            query.value = value
        }

        fun refresh() {
            viewModelScope.launch {
                isRefreshing.value = true
                runCatching { conversationRepository.refreshConversations(appId) }
                isRefreshing.value = false
            }
        }

        fun deleteConversation(chatId: String) {
            viewModelScope.launch { conversationRepository.deleteConversation(appId, chatId) }
        }

        fun clearConversations() {
            viewModelScope.launch { conversationRepository.clearConversations(appId) }
        }

        fun renameConversation(chatId: String, title: String) {
            viewModelScope.launch { conversationRepository.renameConversation(appId, chatId, title.trim()) }
        }

        fun togglePin(chatId: String, pinned: Boolean) {
            viewModelScope.launch { conversationRepository.togglePinConversation(appId, chatId, pinned) }
        }
    }

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val chatRepository: ChatRepository,
    ) : ViewModel() {
        private val appId: String = checkNotNull(savedStateHandle["appId"])
        private val chatId = MutableStateFlow(savedStateHandle.get<String?>("chatId"))
        private val input = MutableStateFlow("")
        private val metaState = MutableStateFlow(ChatMetaState())

        val uiState: StateFlow<ChatUiState> =
            combine(
                input,
                metaState,
                observeMessages(),
            ) { currentInput, meta, messages ->
                ChatUiState(
                    appId = appId,
                    chatId = chatId.value,
                    input = currentInput,
                    isSending = meta.isSending,
                    errorMessage = meta.errorMessage,
                    messages = messages,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ChatUiState(appId = appId, chatId = chatId.value),
            )

        fun updateInput(value: String) {
            input.value = value
        }

        fun send() {
            val text = input.value.trim()
            if (text.isEmpty() || metaState.value.isSending || uiState.value.canStopGeneration) {
                return
            }

            viewModelScope.launch {
                metaState.update { it.copy(isSending = true, errorMessage = null) }
                runCatching {
                    chatRepository.sendMessage(
                        appId = appId,
                        chatId = chatId.value,
                        text = text,
                    )
                }.onSuccess { resolvedChatId ->
                    chatId.value = resolvedChatId
                    input.value = ""
                    metaState.update { it.copy(isSending = false) }
                }.onFailure { error ->
                    metaState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: "发送失败",
                        )
                    }
                }
            }
        }

        fun stopGeneration() {
            val resolvedChatId = chatId.value ?: return
            viewModelScope.launch {
                chatRepository.stopStreaming(appId, resolvedChatId)
            }
        }

        fun regenerate(message: ChatMessageUiModel) {
            val resolvedChatId = chatId.value ?: return
            viewModelScope.launch {
                metaState.update { it.copy(errorMessage = null) }
                runCatching {
                    chatRepository.regenerateResponse(appId, resolvedChatId, message.messageId)
                }.onFailure { error ->
                    metaState.update { it.copy(errorMessage = error.message ?: "重新生成失败") }
                }
            }
        }

        fun updateFeedback(message: ChatMessageUiModel, feedback: MessageFeedback) {
            viewModelScope.launch {
                chatRepository.setFeedback(
                    appId = appId,
                    chatId = message.chatId,
                    messageId = message.messageId,
                    feedback = feedback,
                )
            }
        }

        private fun observeMessages(): Flow<List<ChatMessageUiModel>> =
            chatId.flatMapLatest { resolvedChatId ->
                if (resolvedChatId == null) {
                    flowOf(emptyList())
                } else {
                    chatRepository.observeMessages(appId, resolvedChatId)
                }
            }
    }

private data class ChatMetaState(
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)
