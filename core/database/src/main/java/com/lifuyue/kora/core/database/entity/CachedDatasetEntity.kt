package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_datasets")
data class CachedDatasetEntity(
    @PrimaryKey val datasetId: String,
    val name: String,
    val type: String,
    val status: String,
    val updateTime: Long,
    val summaryJson: String,
)
