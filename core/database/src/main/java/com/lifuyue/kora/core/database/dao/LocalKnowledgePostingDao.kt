package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.LocalKnowledgePostingEntity

@Dao
interface LocalKnowledgePostingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<LocalKnowledgePostingEntity>)

    @Query("DELETE FROM local_knowledge_postings WHERE documentId = :documentId")
    fun deleteByDocumentId(documentId: String)

    @Query("SELECT * FROM local_knowledge_postings WHERE term IN (:terms)")
    fun getPostingsForTerms(terms: List<String>): List<LocalKnowledgePostingEntity>
}
