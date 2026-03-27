package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val chatId: String,
    val appId: String,
    val title: String,
    val customTitle: String?,
    val isPinned: Boolean,
    val source: String,
    val updateTime: Long,
    val lastMessagePreview: String?,
    val hasDraft: Boolean,
    val isDeleted: Boolean,
    val isArchived: Boolean,
)
