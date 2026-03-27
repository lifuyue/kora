package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.CachedDatasetEntity

@Dao
interface CachedDatasetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: CachedDatasetEntity)

    @Query("SELECT * FROM cached_datasets WHERE datasetId = :datasetId LIMIT 1")
    fun getDatasetById(datasetId: String): CachedDatasetEntity?

    @Query("SELECT * FROM cached_datasets ORDER BY updateTime DESC, datasetId DESC")
    fun getDatasets(): List<CachedDatasetEntity>
}
