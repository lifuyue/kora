package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.ConversationTagCrossRef
import com.lifuyue.kora.core.database.entity.ConversationTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(tag: ConversationTagEntity)

    @Query(
        """
        SELECT * FROM conversation_tags
        WHERE appId = :appId
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC, tagId ASC
        """,
    )
    fun observeTags(appId: String): Flow<List<ConversationTagEntity>>

    @Query(
        """
        SELECT * FROM conversation_tags
        WHERE appId = :appId
        ORDER BY sortOrder DESC
        LIMIT 1
        """,
    )
    fun getLastTag(appId: String): ConversationTagEntity?

    @Query(
        """
        UPDATE conversation_tags
        SET name = :name
        WHERE tagId = :tagId
        """,
    )
    fun rename(tagId: String, name: String)

    @Query("DELETE FROM conversation_tags WHERE tagId = :tagId")
    fun delete(tagId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAssignments(refs: List<ConversationTagCrossRef>)

    @Query("DELETE FROM conversation_tag_cross_refs WHERE chatId = :chatId")
    fun clearAssignments(chatId: String)

    @Query("DELETE FROM conversation_tag_cross_refs WHERE chatId IN (:chatIds)")
    fun clearAssignments(chatIds: List<String>)

    @Query(
        """
        SELECT conversation_tag_cross_refs.chatId AS chatId,
               conversation_tags.tagId AS tagId,
               conversation_tags.name AS tagName,
               conversation_tags.colorToken AS colorToken
        FROM conversation_tag_cross_refs
        INNER JOIN conversations ON conversations.chatId = conversation_tag_cross_refs.chatId
        INNER JOIN conversation_tags ON conversation_tags.tagId = conversation_tag_cross_refs.tagId
        WHERE conversations.appId = :appId AND conversations.isDeleted = 0
        ORDER BY conversation_tags.sortOrder ASC, conversation_tags.name COLLATE NOCASE ASC, conversation_tags.tagId ASC
        """,
    )
    fun observeAssignments(appId: String): Flow<List<ConversationTagAssignmentRow>>
}

data class ConversationTagAssignmentRow(
    val chatId: String,
    val tagId: String,
    val tagName: String,
    val colorToken: String,
)
