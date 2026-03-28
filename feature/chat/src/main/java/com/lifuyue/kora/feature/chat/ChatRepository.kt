package com.lifuyue.kora.feature.chat

import android.content.Context
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.ConversationFolderDao
import com.lifuyue.kora.core.database.dao.ConversationTagDao
import com.lifuyue.kora.core.database.dao.InteractiveDraftDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.network.AppQuestionGuideConfigDto
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.SseStreamClient
import com.lifuyue.kora.core.network.UploadedAssetRef
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

interface ConversationRepository {
    fun observeConversations(appId: String): Flow<List<ConversationListItemUiModel>>

    fun observeFolders(appId: String): Flow<List<ConversationFolderUiModel>>

    fun observeTags(appId: String): Flow<List<ConversationTagUiModel>>

    suspend fun refreshConversations(appId: String)

    suspend fun renameConversation(
        appId: String,
        chatId: String,
        title: String,
    )

    suspend fun togglePinConversation(
        appId: String,
        chatId: String,
        pinned: Boolean,
    )

    suspend fun deleteConversation(
        appId: String,
        chatId: String,
    )

    suspend fun clearConversations(appId: String)

    suspend fun createFolder(
        appId: String,
        name: String,
    )

    suspend fun renameFolder(
        appId: String,
        folderId: String,
        name: String,
    )

    suspend fun deleteFolder(
        appId: String,
        folderId: String,
    )

    suspend fun createTag(
        appId: String,
        name: String,
    )

    suspend fun renameTag(
        appId: String,
        tagId: String,
        name: String,
    )

    suspend fun deleteTag(
        appId: String,
        tagId: String,
    )

    suspend fun moveConversationToFolder(
        appId: String,
        chatId: String,
        folderId: String?,
    )

    suspend fun setConversationTags(
        appId: String,
        chatId: String,
        tagIds: List<String>,
    )

    suspend fun setConversationArchived(
        appId: String,
        chatId: String,
        archived: Boolean,
    ) {
        throw UnsupportedOperationException("Conversation archive foundation is not implemented yet")
    }
}

interface ChatRepository {
    fun observeMessages(
        appId: String,
        chatId: String?,
    ): Flow<List<ChatMessageUiModel>>

    suspend fun bootstrapChat(appId: String): ChatBootstrap

    suspend fun restoreMessages(
        appId: String,
        chatId: String,
    )

    suspend fun sendMessage(
        appId: String,
        chatId: String?,
        text: String,
    ): String

    suspend fun stopStreaming(
        appId: String,
        chatId: String,
    )

    suspend fun continueGeneration(
        appId: String,
        chatId: String,
    ): String

    suspend fun regenerateResponse(
        appId: String,
        chatId: String,
        messageId: String,
    ): String

    suspend fun setFeedback(
        appId: String,
        chatId: String,
        messageId: String,
        feedback: MessageFeedback,
    )

    suspend fun uploadAttachment(
        appId: String,
        chatId: String?,
        attachment: AttachmentDraftUiModel,
    ): UploadedAssetRef {
        throw UnsupportedOperationException("Attachment upload foundation is not implemented yet")
    }

    suspend fun savePendingInteractiveDraft(
        appId: String,
        chatId: String,
        card: InteractiveCardUiModel,
        draftPayloadJson: String?,
    ) {
        throw UnsupportedOperationException("Interactive draft foundation is not implemented yet")
    }

    suspend fun clearPendingInteractiveDraft(
        appId: String,
        chatId: String,
    ) {
        throw UnsupportedOperationException("Interactive draft foundation is not implemented yet")
    }

    suspend fun submitInteractiveResponse(
        appId: String,
        chatId: String,
        card: InteractiveCardUiModel,
        value: String,
    ): String {
        throw UnsupportedOperationException("Interactive submission is not implemented yet")
    }

    suspend fun buildShareExportPreview(
        appId: String,
        chatId: String,
        selection: MessageRangeSelection?,
        format: ConversationExportFormat,
    ): String {
        throw UnsupportedOperationException("Share/export foundation is not implemented yet")
    }
}

data class ChatBootstrap(
    val chatId: String,
    val title: String = "",
    val welcomeText: String? = null,
    val questionGuide: AppQuestionGuideConfigDto? = null,
)

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
        conversationFolderDao: ConversationFolderDao,
        conversationTagDao: ConversationTagDao,
        interactiveDraftDao: InteractiveDraftDao,
        messageDao: MessageDao,
        @ApplicationContext context: Context,
    ): RoomChatRepository =
        RoomChatRepository(
            api = api,
            sseStreamClient = sseStreamClient,
            conversationDao = conversationDao,
            conversationFolderDao = conversationFolderDao,
            conversationTagDao = conversationTagDao,
            interactiveDraftDao = interactiveDraftDao,
            messageDao = messageDao,
            context = context,
        )

    @Provides
    fun provideChatRepository(repository: RoomChatRepository): ChatRepository = ChatTestOverrides.chatRepository ?: repository

    @Provides
    fun provideConversationRepository(repository: RoomChatRepository): ConversationRepository =
        ChatTestOverrides.conversationRepository ?: repository
}
