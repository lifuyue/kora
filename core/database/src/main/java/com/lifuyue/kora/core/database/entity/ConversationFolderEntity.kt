package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_folders",
    indices = [Index(value = ["appId", "name"], unique = true)],
)
data class ConversationFolderEntity(
    @PrimaryKey val folderId: String,
    val appId: String,
    val name: String,
    val sortOrder: Long,
)
