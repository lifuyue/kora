package com.lifuyue.kora.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "local_knowledge_postings",
    primaryKeys = ["term", "chunkId"],
)
data class LocalKnowledgePostingEntity(
    val term: String,
    val chunkId: String,
    val documentId: String,
    val termFrequency: Int,
    val isTitleTerm: Boolean,
)
