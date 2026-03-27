package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId", "createdAt", "dataId"]),
    ],
)
data class MessageEntity(
    @PrimaryKey val dataId: String,
    val chatId: String,
    val appId: String,
    val role: String,
    val payloadJson: String,
    val createdAt: Long,
    val isStreaming: Boolean,
    val sendStatus: String,
    val errorCode: Int?,
)
