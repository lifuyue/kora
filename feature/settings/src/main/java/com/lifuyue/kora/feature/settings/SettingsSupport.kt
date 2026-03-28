package com.lifuyue.kora.feature.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsCacheManager {
    suspend fun getStorageBuckets(): Map<StorageBucket, Long>

    suspend fun clearCache()
}

@Singleton
class AndroidSettingsCacheManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SettingsCacheManager {
        override suspend fun getStorageBuckets(): Map<StorageBucket, Long> =
            mapOf(
                StorageBucket.DATABASE to databaseStorageSize(),
                StorageBucket.PREFERENCES to preferencesStorageSize(),
                StorageBucket.TEMP_CACHE to directorySize(context.cacheDir),
            )

        override suspend fun clearCache() {
            context.cacheDir.listFiles().orEmpty().forEach { file ->
                file.deleteRecursively()
            }
        }

        private fun databaseStorageSize(): Long {
            val databaseFile = context.getDatabasePath("kora.db")
            val databaseDir = databaseFile.parentFile ?: return fileSize(databaseFile)
            return databaseDir
                .listFiles()
                .orEmpty()
                .filter { file ->
                    file.name == databaseFile.name || file.name.startsWith("${databaseFile.name}-")
                }.sumOf(::fileSize)
        }

        private fun preferencesStorageSize(): Long {
            val dataStoreFile = context.preferencesDataStoreFile("connection.preferences_pb")
            val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            val sharedPrefsSize =
                sharedPrefsDir
                    .listFiles()
                    .orEmpty()
                    .filter { file ->
                        file.name == "kora_secure_connection.xml" || file.name == "kora_secure_connection_fallback.xml"
                    }.sumOf(::fileSize)
            return fileSize(dataStoreFile) + sharedPrefsSize
        }

        private fun directorySize(root: File): Long =
            if (!root.exists()) {
                0L
            } else {
                root.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            }

        private fun fileSize(file: File): Long = if (file.exists() && file.isFile) file.length() else 0L
    }

interface AppInfoProvider {
    fun versionName(): String

    fun feedbackUrl(): String

    fun licensesUrl(): String
}

@Singleton
class AndroidAppInfoProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AppInfoProvider {
        override fun versionName(): String {
            return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "dev"
        }

        override fun feedbackUrl(): String = "mailto:kora@example.com"

        override fun licensesUrl(): String = "https://github.com/lifuyue/kora"
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsSupportModule {
    @Binds
    abstract fun bindSettingsCacheManager(manager: AndroidSettingsCacheManager): SettingsCacheManager

    @Binds
    abstract fun bindAppInfoProvider(provider: AndroidAppInfoProvider): AppInfoProvider

    companion object {
        @Provides
        fun provideSettingsFeedbackUrl(): String = "mailto:kora@example.com"
    }
}
