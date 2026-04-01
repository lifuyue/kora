package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.LocalKnowledgeChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalKnowledgeChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<LocalKnowledgeChunkEntity>)

    @Query("SELECT * FROM local_knowledge_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    fun observeChunks(documentId: String): Flow<List<LocalKnowledgeChunkEntity>>

    @Query("SELECT * FROM local_knowledge_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    fun getChunks(documentId: String): List<LocalKnowledgeChunkEntity>

    @Query("DELETE FROM local_knowledge_chunks WHERE documentId = :documentId")
    fun deleteByDocumentId(documentId: String)

    @Query("DELETE FROM local_knowledge_chunks")
    fun clearAll()

    @Query("SELECT * FROM local_knowledge_chunks")
    fun getAllChunks(): List<LocalKnowledgeChunkEntity>

    @Query("SELECT * FROM local_knowledge_chunks WHERE chunkId IN (:chunkIds)")
    fun getChunksByIds(chunkIds: List<String>): List<LocalKnowledgeChunkEntity>

    @Query("SELECT AVG(tokenCount) FROM local_knowledge_chunks WHERE documentId IN (:documentIds)")
    fun getAverageTokenCountForDocuments(documentIds: List<String>): Double?
}
