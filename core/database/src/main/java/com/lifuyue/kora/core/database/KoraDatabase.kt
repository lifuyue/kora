package com.lifuyue.kora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifuyue.kora.core.database.dao.CachedCollectionDao
import com.lifuyue.kora.core.database.dao.CachedDatasetDao
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.ConversationFolderDao
import com.lifuyue.kora.core.database.dao.ConversationTagDao
import com.lifuyue.kora.core.database.dao.ImportTaskDao
import com.lifuyue.kora.core.database.dao.InteractiveDraftDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.database.entity.CachedCollectionEntity
import com.lifuyue.kora.core.database.entity.CachedDatasetEntity
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.database.entity.ConversationFolderCrossRef
import com.lifuyue.kora.core.database.entity.ConversationFolderEntity
import com.lifuyue.kora.core.database.entity.ConversationTagCrossRef
import com.lifuyue.kora.core.database.entity.ConversationTagEntity
import com.lifuyue.kora.core.database.entity.ImportTaskEntity
import com.lifuyue.kora.core.database.entity.InteractiveDraftEntity
import com.lifuyue.kora.core.database.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        ConversationFolderEntity::class,
        ConversationTagEntity::class,
        ConversationFolderCrossRef::class,
        ConversationTagCrossRef::class,
        MessageEntity::class,
        CachedDatasetEntity::class,
        CachedCollectionEntity::class,
        ImportTaskEntity::class,
        InteractiveDraftEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(KoraTypeConverters::class)
abstract class KoraDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    abstract fun conversationFolderDao(): ConversationFolderDao

    abstract fun conversationTagDao(): ConversationTagDao

    abstract fun messageDao(): MessageDao

    abstract fun cachedDatasetDao(): CachedDatasetDao

    abstract fun cachedCollectionDao(): CachedCollectionDao

    abstract fun importTaskDao(): ImportTaskDao

    abstract fun interactiveDraftDao(): InteractiveDraftDao
}
