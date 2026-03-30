package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.LocalKnowledgeDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalKnowledgeDocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: LocalKnowledgeDocumentEntity)

    @Query("SELECT * FROM local_knowledge_documents ORDER BY updatedAt DESC, documentId DESC")
    fun observeDocuments(): Flow<List<LocalKnowledgeDocumentEntity>>

    @Query("SELECT * FROM local_knowledge_documents ORDER BY updatedAt DESC, documentId DESC")
    fun getDocuments(): List<LocalKnowledgeDocumentEntity>

    @Query("SELECT * FROM local_knowledge_documents WHERE documentId = :documentId LIMIT 1")
    fun observeDocument(documentId: String): Flow<LocalKnowledgeDocumentEntity?>

    @Query("SELECT * FROM local_knowledge_documents WHERE documentId = :documentId LIMIT 1")
    fun getDocument(documentId: String): LocalKnowledgeDocumentEntity?

    @Query("DELETE FROM local_knowledge_documents WHERE documentId = :documentId")
    fun delete(documentId: String)
}
