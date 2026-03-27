package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "conversation_tag_cross_refs",
    primaryKeys = ["chatId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ConversationTagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class ConversationTagCrossRef(
    val chatId: String,
    val tagId: String,
)
