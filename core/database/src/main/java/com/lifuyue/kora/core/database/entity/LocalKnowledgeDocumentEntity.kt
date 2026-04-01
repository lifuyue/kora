package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_knowledge_documents")
data class LocalKnowledgeDocumentEntity(
    @PrimaryKey val documentId: String,
    val title: String,
    val normalizedTitle: String,
    val sourceLabel: String,
    val rawText: String,
    val previewText: String,
    val chunkCount: Int,
    val isEnabled: Boolean,
    val indexStatus: String,
    val indexErrorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
