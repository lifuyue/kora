package com.lifuyue.kora.feature.chat

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.SseStreamClient

interface ConversationRepository {
    fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>>

    suspend fun refreshConversations(appId: String)

    suspend fun renameConversation(appId: String, chatId: String, title: String)

    suspend fun togglePinConversation(appId: String, chatId: String, pinned: Boolean)

    suspend fun deleteConversation(appId: String, chatId: String)

    suspend fun clearConversations(appId: String)
}

interface ChatRepository {
    fun observeMessages(appId: String, chatId: String?): Flow<List<ChatMessageUiModel>>

    suspend fun restoreMessages(appId: String, chatId: String)

    suspend fun sendMessage(appId: String, chatId: String?, text: String): String

    suspend fun stopStreaming(appId: String, chatId: String)

    suspend fun continueGeneration(appId: String, chatId: String): String

    suspend fun regenerateResponse(appId: String, chatId: String, messageId: String): String

    suspend fun setFeedback(
        appId: String,
        chatId: String,
        messageId: String,
        feedback: MessageFeedback,
    )
}

data class AssistantResponseRequest(
    val appId: String,
    val chatId: String,
    val prompt: String,
    val attempt: Int,
)

data class AssistantResponseStep(
    val markdownDelta: String = "",
    val reasoningDelta: String = "",
    val delayMillis: Long = 0L,
    val terminalError: String? = null,
)

fun interface AssistantResponsePlanner {
    fun plan(request: AssistantResponseRequest): List<AssistantResponseStep>
}

@Module
@InstallIn(SingletonComponent::class)
object ChatRepositoryModule {
    @Provides
    @Singleton
    fun provideRoomChatRepository(
        api: FastGptApi,
        sseStreamClient: SseStreamClient,
        conversationDao: ConversationDao,
        messageDao: MessageDao,
    ): RoomChatRepository =
        RoomChatRepository(
            api = api,
            sseStreamClient = sseStreamClient,
            conversationDao = conversationDao,
            messageDao = messageDao,
        )

    @Provides
    fun provideChatRepository(repository: RoomChatRepository): ChatRepository =
        ChatTestOverrides.chatRepository ?: repository

    @Provides
    fun provideConversationRepository(repository: RoomChatRepository): ConversationRepository =
        ChatTestOverrides.conversationRepository ?: repository
}
