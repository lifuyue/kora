package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.ImportTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ImportTaskEntity)

    @Query(
        """
        SELECT * FROM import_tasks
        WHERE datasetId = :datasetId
        ORDER BY updateTime DESC, taskId DESC
        """,
    )
    fun observeTasksForDataset(datasetId: String): Flow<List<ImportTaskEntity>>

    @Query("DELETE FROM import_tasks WHERE taskId = :taskId")
    fun delete(taskId: String)
}
