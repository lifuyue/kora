package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interactive_drafts")
data class InteractiveDraftEntity(
    @PrimaryKey val chatId: String,
    val messageDataId: String,
    val responseValueId: String? = null,
    val rawPayloadJson: String,
    val draftPayloadJson: String? = null,
    val updatedAt: Long,
)
