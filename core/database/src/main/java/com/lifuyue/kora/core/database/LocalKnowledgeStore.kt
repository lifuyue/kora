package com.lifuyue.kora.core.database

import com.lifuyue.kora.core.database.dao.LocalKnowledgeChunkDao
import com.lifuyue.kora.core.database.dao.LocalKnowledgeDocumentDao
import com.lifuyue.kora.core.database.entity.LocalKnowledgeChunkEntity
import com.lifuyue.kora.core.database.entity.LocalKnowledgeDocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class LocalKnowledgeDocument(
    val documentId: String,
    val title: String,
    val sourceLabel: String,
    val previewText: String,
    val chunkCount: Int,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LocalKnowledgeChunk(
    val chunkId: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
)

data class LocalKnowledgeHit(
    val documentId: String,
    val chunkId: String,
    val title: String,
    val sourceLabel: String,
    val snippet: String,
    val score: Double,
)

@Singleton
class LocalKnowledgeStore
    @Inject
    constructor(
        private val documentDao: LocalKnowledgeDocumentDao,
        private val chunkDao: LocalKnowledgeChunkDao,
    ) {
        fun observeDocuments(): Flow<List<LocalKnowledgeDocument>> =
            documentDao.observeDocuments().map { items -> items.map(LocalKnowledgeDocumentEntity::toModel) }

        fun observeDocument(documentId: String): Flow<LocalKnowledgeDocument?> =
            documentDao.observeDocument(documentId).map { it?.toModel() }

        fun observeChunks(documentId: String): Flow<List<LocalKnowledgeChunk>> =
            chunkDao.observeChunks(documentId).map { items -> items.map(LocalKnowledgeChunkEntity::toModel) }

        fun observeDocumentWithChunks(documentId: String): Flow<Pair<LocalKnowledgeDocument?, List<LocalKnowledgeChunk>>> =
            combine(observeDocument(documentId), observeChunks(documentId)) { document, chunks -> document to chunks }

        fun importText(
            title: String,
            text: String,
            sourceLabel: String,
            enabled: Boolean = true,
            now: Long = System.currentTimeMillis(),
        ) {
            val trimmedTitle = title.trim()
            val normalizedText = text.trim()
            require(trimmedTitle.isNotBlank()) { "标题不能为空" }
            require(normalizedText.isNotBlank()) { "内容不能为空" }
            val documentId = "local-doc-$now"
            val chunks =
                normalizedText
                    .chunkForLocalSearch()
                    .mapIndexed { index, chunk ->
                        LocalKnowledgeChunkEntity(
                            chunkId = "$documentId-chunk-$index",
                            documentId = documentId,
                            chunkIndex = index,
                            text = chunk,
                            normalizedText = chunk.normalizeForSearch(),
                            tokenCount = chunk.extractSearchTokens().size,
                        )
                    }
            documentDao.upsert(
                LocalKnowledgeDocumentEntity(
                    documentId = documentId,
                    title = trimmedTitle,
                    sourceLabel = sourceLabel.trim().ifBlank { "本地资料" },
                    rawText = normalizedText,
                    previewText = chunks.firstOrNull()?.text.orEmpty().take(160),
                    chunkCount = chunks.size,
                    isEnabled = enabled,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            chunkDao.upsertAll(chunks)
        }

        fun setEnabled(
            documentId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ) {
            val current = documentDao.getDocument(documentId) ?: return
            documentDao.upsert(current.copy(isEnabled = enabled, updatedAt = now))
        }

        fun deleteDocument(documentId: String) {
            chunkDao.deleteByDocumentId(documentId)
            documentDao.delete(documentId)
        }

        fun search(
            query: String,
            limit: Int = 4,
        ): List<LocalKnowledgeHit> {
            val queryTokens = query.extractSearchTokens()
            if (queryTokens.isEmpty()) {
                return emptyList()
            }
            val enabledDocuments = documentDao.getDocuments().filter { it.isEnabled }.associateBy { it.documentId }
            return chunkDao.getAllChunks()
                .mapNotNull { chunk ->
                    val document = enabledDocuments[chunk.documentId] ?: return@mapNotNull null
                    val score = scoreChunk(query = query, queryTokens = queryTokens, chunk = chunk)
                    if (score <= 0.0) {
                        null
                    } else {
                        LocalKnowledgeHit(
                            documentId = document.documentId,
                            chunkId = chunk.chunkId,
                            title = document.title,
                            sourceLabel = document.sourceLabel,
                            snippet = chunk.text,
                            score = score,
                        )
                    }
                }.sortedByDescending { it.score }
                .take(limit)
        }

        private fun scoreChunk(
            query: String,
            queryTokens: Set<String>,
            chunk: LocalKnowledgeChunkEntity,
        ): Double {
            val overlap = queryTokens.count { token -> chunk.normalizedText.contains(token) }
            if (overlap == 0) {
                return 0.0
            }
            val exactBoost = if (chunk.normalizedText.contains(query.normalizeForSearch())) 0.75 else 0.0
            return overlap.toDouble() + exactBoost + (1.0 / (chunk.chunkIndex + 1))
        }
    }

private fun LocalKnowledgeDocumentEntity.toModel(): LocalKnowledgeDocument =
    LocalKnowledgeDocument(
        documentId = documentId,
        title = title,
        sourceLabel = sourceLabel,
        previewText = previewText,
        chunkCount = chunkCount,
        isEnabled = isEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun LocalKnowledgeChunkEntity.toModel(): LocalKnowledgeChunk =
    LocalKnowledgeChunk(
        chunkId = chunkId,
        documentId = documentId,
        chunkIndex = chunkIndex,
        text = text,
    )

private fun String.chunkForLocalSearch(maxChunkLength: Int = 420): List<String> {
    val normalized = replace("\r\n", "\n").trim()
    if (normalized.isBlank()) {
        return emptyList()
    }
    val paragraphs = normalized.split("\n\n").map(String::trim).filter(String::isNotBlank)
    val result = mutableListOf<String>()
    var buffer = StringBuilder()
    paragraphs.forEach { paragraph ->
        if (buffer.isNotEmpty() && buffer.length + paragraph.length + 2 > maxChunkLength) {
            result += buffer.toString().trim()
            buffer = StringBuilder()
        }
        if (paragraph.length > maxChunkLength) {
            paragraph.chunked(maxChunkLength).forEach { piece ->
                if (buffer.isNotEmpty()) {
                    result += buffer.toString().trim()
                    buffer = StringBuilder()
                }
                result += piece.trim()
            }
        } else {
            if (buffer.isNotEmpty()) {
                buffer.append("\n\n")
            }
            buffer.append(paragraph)
        }
    }
    if (buffer.isNotEmpty()) {
        result += buffer.toString().trim()
    }
    return result.filter { it.isNotBlank() }
}

private fun String.normalizeForSearch(): String =
    lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").replace(Regex("\\s+"), " ").trim()

private fun String.extractSearchTokens(): Set<String> =
    normalizeForSearch()
        .split(" ")
        .map(String::trim)
        .filter { token -> token.length >= 2 }
        .toSet()
