package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<MessageEntity>)

    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY createdAt ASC, dataId ASC
        """,
    )
    fun getMessagesForChat(chatId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE dataId = :dataId")
    fun deleteMessage(dataId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    fun deleteMessagesForChat(chatId: String)
}
