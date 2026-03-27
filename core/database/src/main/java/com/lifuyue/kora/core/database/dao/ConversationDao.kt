package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.ConversationEntity

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ConversationEntity)

    @Query(
        """
        SELECT * FROM conversations
        WHERE appId = :appId
        ORDER BY isPinned DESC, updateTime DESC, chatId DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun getConversationsForApp(
        appId: String,
        limit: Int,
        offset: Int,
    ): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE chatId = :chatId LIMIT 1")
    fun getConversationByChatId(chatId: String): ConversationEntity?

    @Query("UPDATE conversations SET isDeleted = 1 WHERE chatId = :chatId")
    fun softDelete(chatId: String)

    @Query("UPDATE conversations SET isDeleted = 1 WHERE appId = :appId")
    fun clearByAppId(appId: String)
}
