package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<ConversationEntity>)

    @Query(
        """
        SELECT * FROM conversations
        WHERE appId = :appId
        ORDER BY isPinned DESC, updateTime DESC, chatId DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun getConversationsForAppIncludingDeleted(
        appId: String,
        limit: Int,
        offset: Int,
    ): List<ConversationEntity>

    @Query(
        """
        SELECT * FROM conversations
        WHERE appId = :appId AND isDeleted = 0
        ORDER BY isPinned DESC, updateTime DESC, chatId DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun getConversationsForApp(
        appId: String,
        limit: Int,
        offset: Int,
    ): List<ConversationEntity>

    @Query(
        """
        SELECT * FROM conversations
        WHERE appId = :appId AND isDeleted = 0
        ORDER BY isPinned DESC, updateTime DESC, chatId DESC
        """,
    )
    fun observeConversationsForApp(appId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE chatId = :chatId LIMIT 1")
    fun getConversationByChatId(chatId: String): ConversationEntity?

    @Query(
        """
        UPDATE conversations
        SET isDeleted = CASE WHEN chatId IN (:activeChatIds) THEN 0 ELSE 1 END
        WHERE appId = :appId
        """,
    )
    fun markMissingAsDeleted(
        appId: String,
        activeChatIds: List<String>,
    )

    @Query(
        """
        UPDATE conversations
        SET
            title = COALESCE(:title, title),
            customTitle = :customTitle,
            isPinned = COALESCE(:isPinned, isPinned),
            updateTime = :updateTime,
            lastMessagePreview = COALESCE(:lastMessagePreview, lastMessagePreview)
        WHERE chatId = :chatId
        """,
    )
    fun updateConversation(
        chatId: String,
        title: String?,
        customTitle: String?,
        isPinned: Boolean?,
        updateTime: Long,
        lastMessagePreview: String?,
    )

    @Query("UPDATE conversations SET isDeleted = 1 WHERE chatId = :chatId")
    fun softDelete(chatId: String)

    @Query("UPDATE conversations SET isDeleted = 1 WHERE appId = :appId")
    fun clearByAppId(appId: String)

    @Query("UPDATE conversations SET isArchived = :isArchived WHERE chatId = :chatId")
    fun updateArchived(chatId: String, isArchived: Boolean)
}
