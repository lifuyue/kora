package com.lifuyue.kora.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import com.lifuyue.kora.core.database.dao.CachedCollectionDao
import com.lifuyue.kora.core.database.dao.CachedDatasetDao
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.ConversationFolderDao
import com.lifuyue.kora.core.database.dao.ConversationTagDao
import com.lifuyue.kora.core.database.dao.ImportTaskDao
import com.lifuyue.kora.core.database.dao.InteractiveDraftDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): KoraDatabase {
        return Room.databaseBuilder(context, KoraDatabase::class.java, "kora.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideConversationDao(database: KoraDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideConversationFolderDao(database: KoraDatabase): ConversationFolderDao = database.conversationFolderDao()

    @Provides
    fun provideConversationTagDao(database: KoraDatabase): ConversationTagDao = database.conversationTagDao()

    @Provides
    fun provideMessageDao(database: KoraDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideCachedDatasetDao(database: KoraDatabase): CachedDatasetDao = database.cachedDatasetDao()

    @Provides
    fun provideCachedCollectionDao(database: KoraDatabase): CachedCollectionDao = database.cachedCollectionDao()

    @Provides
    fun provideImportTaskDao(database: KoraDatabase): ImportTaskDao = database.importTaskDao()

    @Provides
    fun provideInteractiveDraftDao(database: KoraDatabase): InteractiveDraftDao = database.interactiveDraftDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("connection.preferences_pb") },
        )
    }

    @Provides
    @Singleton
    fun provideConnectionPreferencesStore(dataStore: DataStore<Preferences>): ConnectionPreferencesStore =
        ConnectionPreferencesStore(dataStore)

    @Provides
    @Singleton
    fun provideApiKeySecureStore(
        @ApplicationContext context: Context,
    ): ApiKeySecureStore = ApiKeySecureStore(context)

    @Provides
    @Singleton
    fun provideConnectionRepository(
        connectionPreferencesStore: ConnectionPreferencesStore,
        apiKeySecureStore: ApiKeySecureStore,
        connectionProvider: MutableConnectionProvider,
        apiFactory: FastGptApiFactory,
    ): ConnectionRepository =
        ConnectionRepository(
            preferencesStore = connectionPreferencesStore,
            apiKeySecureStore = apiKeySecureStore,
            connectionProvider = connectionProvider,
            apiFactory = apiFactory,
        )
}
