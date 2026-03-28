package com.lifuyue.kora.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_tasks")
data class ImportTaskEntity(
    @PrimaryKey val taskId: String,
    val datasetId: String,
    val collectionId: String? = null,
    val sourceType: String,
    val displayName: String,
    val status: String,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val updateTime: Long,
)
