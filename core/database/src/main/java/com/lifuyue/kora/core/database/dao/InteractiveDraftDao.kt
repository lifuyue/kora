package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.InteractiveDraftEntity

@Dao
interface InteractiveDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InteractiveDraftEntity)

    @Query("SELECT * FROM interactive_drafts WHERE chatId = :chatId LIMIT 1")
    fun getByChatId(chatId: String): InteractiveDraftEntity?

    @Query("DELETE FROM interactive_drafts WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: String)
}
