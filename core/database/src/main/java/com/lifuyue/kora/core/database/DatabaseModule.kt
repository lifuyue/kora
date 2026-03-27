package com.lifuyue.kora.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lifuyue.kora.core.database.dao.CachedDatasetDao
import com.lifuyue.kora.core.database.dao.ConversationDao
import com.lifuyue.kora.core.database.dao.MessageDao
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
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
        return Room.databaseBuilder(context, KoraDatabase::class.java, "kora.db").build()
    }

    @Provides
    fun provideConversationDao(database: KoraDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: KoraDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideCachedDatasetDao(database: KoraDatabase): CachedDatasetDao = database.cachedDatasetDao()

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
}
