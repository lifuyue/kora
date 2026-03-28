package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_collections")
data class CachedCollectionEntity(
    @PrimaryKey val collectionId: String,
    val datasetId: String,
    val name: String,
    val type: String,
    val status: String,
    val trainingType: String,
    val sourceName: String,
    val updateTime: Long,
    val summaryJson: String,
)
