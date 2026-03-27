package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_tags",
    indices = [Index(value = ["appId", "name"], unique = true)],
)
data class ConversationTagEntity(
    @PrimaryKey val tagId: String,
    val appId: String,
    val name: String,
    val colorToken: String,
    val sortOrder: Long,
)
