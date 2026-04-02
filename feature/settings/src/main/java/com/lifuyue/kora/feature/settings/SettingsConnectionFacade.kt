package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsConnectionFacade {
    val snapshot: StateFlow<ConnectionSnapshot>

    suspend fun testConnection(
        connectionType: ConnectionType,
        serverBaseUrl: String,
        apiKey: String,
        model: String? = null,
    ): ConnectionTestResult

    suspend fun saveConnection(
        connectionType: ConnectionType,
        serverBaseUrl: String,
        apiKey: String,
        model: String?,
        selectedAppId: String? = null,
        onboardingCompleted: Boolean,
    )

    suspend fun updateSelectedAppId(selectedAppId: String)

    suspend fun clearConnection()

    suspend fun updateAppearance(
        themeMode: ThemeMode,
    )

    suspend fun updateLanguageTag(languageTag: String?)

    suspend fun updateChatPreferences(
        showReasoningEntry: Boolean,
        streamResponses: Boolean,
    )
}

@Singleton
class ConnectionRepositorySettingsFacade
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
    ) : SettingsConnectionFacade {
        override val snapshot: StateFlow<ConnectionSnapshot> = connectionRepository.snapshot

        override suspend fun testConnection(
            connectionType: ConnectionType,
            serverBaseUrl: String,
            apiKey: String,
            model: String?,
        ): ConnectionTestResult = connectionRepository.testConnection(connectionType, serverBaseUrl, apiKey, model)

        override suspend fun saveConnection(
            connectionType: ConnectionType,
            serverBaseUrl: String,
            apiKey: String,
            model: String?,
            selectedAppId: String?,
            onboardingCompleted: Boolean,
        ) {
            connectionRepository.saveConnection(
                connectionType = connectionType,
                serverBaseUrl = serverBaseUrl,
                apiKey = apiKey,
                model = model,
                selectedAppId = selectedAppId,
                onboardingCompleted = onboardingCompleted,
            )
        }

        override suspend fun updateSelectedAppId(selectedAppId: String) {
            connectionRepository.updateSelectedAppId(selectedAppId)
        }

        override suspend fun clearConnection() {
            connectionRepository.clearConnection()
        }

        override suspend fun updateAppearance(
            themeMode: ThemeMode,
        ) {
            connectionRepository.updateAppearance(
                themeMode = themeMode,
            )
        }

        override suspend fun updateLanguageTag(languageTag: String?) {
            connectionRepository.updateLanguageTag(languageTag)
        }

        override suspend fun updateChatPreferences(
            showReasoningEntry: Boolean,
            streamResponses: Boolean,
        ) {
            connectionRepository.updateChatPreferences(
                showReasoningEntry = showReasoningEntry,
                streamResponses = streamResponses,
            )
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsFacadeModule {
    @Binds
    abstract fun bindSettingsConnectionFacade(facade: ConnectionRepositorySettingsFacade): SettingsConnectionFacade
}

internal fun redactApiKey(apiKey: String?): String {
    if (apiKey.isNullOrBlank()) {
        return ""
    }
    if (apiKey.length <= 8) {
        return "*".repeat(apiKey.length)
    }
    return "${apiKey.take(8)}...${apiKey.takeLast(4)}"
}
