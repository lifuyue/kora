package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_knowledge_chunks")
data class LocalKnowledgeChunkEntity(
    @PrimaryKey val chunkId: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
    val normalizedText: String,
    val tokenCount: Int,
)
