package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

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

    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY createdAt ASC, dataId ASC
        """,
    )
    fun observeMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query(
        """
        UPDATE messages
        SET
            payloadJson = :payloadJson,
            isStreaming = :isStreaming,
            sendStatus = :sendStatus,
            errorCode = :errorCode
        WHERE dataId = :dataId
        """,
    )
    fun updateMessageState(
        dataId: String,
        payloadJson: String,
        isStreaming: Boolean,
        sendStatus: String,
        errorCode: Int?,
    )

    @Query("UPDATE messages SET feedbackType = :feedbackType WHERE dataId = :dataId")
    fun updateFeedback(dataId: String, feedbackType: Int?)

    @Query("DELETE FROM messages WHERE dataId = :dataId")
    fun deleteMessage(dataId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    fun deleteMessagesForChat(chatId: String)
}
