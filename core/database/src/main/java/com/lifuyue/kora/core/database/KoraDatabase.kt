package com.lifuyue.kora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifuyue.kora.core.database.dao.CachedCollectionDao
import com.lifuyue.kora.core.database.dao.CachedDatasetDao
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.ImportTaskDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.database.entity.CachedCollectionEntity
import com.lifuyue.kora.core.database.entity.CachedDatasetEntity
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.database.entity.ImportTaskEntity
import com.lifuyue.kora.core.database.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        CachedDatasetEntity::class,
        CachedCollectionEntity::class,
        ImportTaskEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(KoraTypeConverters::class)
abstract class KoraDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao

    abstract fun cachedDatasetDao(): CachedDatasetDao

    abstract fun cachedCollectionDao(): CachedCollectionDao

    abstract fun importTaskDao(): ImportTaskDao
}
