package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.ConversationFolderCrossRef
import com.lifuyue.kora.core.database.entity.ConversationFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(folder: ConversationFolderEntity)

    @Query(
        """
        SELECT * FROM conversation_folders
        WHERE appId = :appId
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC, folderId ASC
        """,
    )
    fun observeFolders(appId: String): Flow<List<ConversationFolderEntity>>

    @Query(
        """
        SELECT * FROM conversation_folders
        WHERE appId = :appId
        ORDER BY sortOrder DESC
        LIMIT 1
        """,
    )
    fun getLastFolder(appId: String): ConversationFolderEntity?

    @Query(
        """
        UPDATE conversation_folders
        SET name = :name
        WHERE folderId = :folderId
        """,
    )
    fun rename(
        folderId: String,
        name: String,
    )

    @Query("DELETE FROM conversation_folders WHERE folderId = :folderId")
    fun delete(folderId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAssignment(ref: ConversationFolderCrossRef)

    @Query("DELETE FROM conversation_folder_cross_refs WHERE chatId = :chatId")
    fun clearAssignment(chatId: String)

    @Query("DELETE FROM conversation_folder_cross_refs WHERE chatId IN (:chatIds)")
    fun clearAssignments(chatIds: List<String>)

    @Query(
        """
        SELECT conversation_folder_cross_refs.chatId AS chatId,
               conversation_folders.folderId AS folderId,
               conversation_folders.name AS folderName
        FROM conversation_folder_cross_refs
        INNER JOIN conversations ON conversations.chatId = conversation_folder_cross_refs.chatId
        INNER JOIN conversation_folders ON conversation_folders.folderId = conversation_folder_cross_refs.folderId
        WHERE conversations.appId = :appId AND conversations.isDeleted = 0
        """,
    )
    fun observeAssignments(appId: String): Flow<List<ConversationFolderAssignmentRow>>
}

data class ConversationFolderAssignmentRow(
    val chatId: String,
    val folderId: String,
    val folderName: String,
)
