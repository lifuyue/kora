package com.lifuyue.kora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifuyue.kora.core.database.entity.CachedCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedCollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<CachedCollectionEntity>)

    @Query(
        """
        SELECT * FROM cached_collections
        WHERE datasetId = :datasetId
        ORDER BY updateTime DESC, collectionId DESC
        """,
    )
    fun observeCollectionsForDataset(datasetId: String): Flow<List<CachedCollectionEntity>>

    @Query("DELETE FROM cached_collections WHERE datasetId = :datasetId")
    fun clearByDatasetId(datasetId: String)
}
