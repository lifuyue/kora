package com.lifuyue.kora.feature.settings

import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestResult
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
        serverBaseUrl: String,
        apiKey: String,
    ): ConnectionTestResult

    suspend fun saveConnection(
        serverBaseUrl: String,
        apiKey: String,
        selectedAppId: String,
        onboardingCompleted: Boolean,
    )

    suspend fun updateSelectedAppId(selectedAppId: String)

    suspend fun clearConnection()

    suspend fun updateAppearance(
        themeMode: ThemeMode,
        dynamicColorEnabled: Boolean,
        oledEnabled: Boolean,
    )

    suspend fun updateChatPreferences(
        streamEnabled: Boolean,
        autoScroll: Boolean,
        fontSizeScale: Float,
        showCitationsByDefault: Boolean,
    )

    suspend fun updateLanguageTag(languageTag: String?)
}

@Singleton
class ConnectionRepositorySettingsFacade
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
    ) : SettingsConnectionFacade {
        override val snapshot: StateFlow<ConnectionSnapshot> = connectionRepository.snapshot

        override suspend fun testConnection(
            serverBaseUrl: String,
            apiKey: String,
        ): ConnectionTestResult = connectionRepository.testConnection(serverBaseUrl, apiKey)

        override suspend fun saveConnection(
            serverBaseUrl: String,
            apiKey: String,
            selectedAppId: String,
            onboardingCompleted: Boolean,
        ) {
            connectionRepository.saveConnection(
                serverBaseUrl = serverBaseUrl,
                apiKey = apiKey,
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
            dynamicColorEnabled: Boolean,
            oledEnabled: Boolean,
        ) {
            connectionRepository.updateAppearance(
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColorEnabled,
                oledEnabled = oledEnabled,
            )
        }

        override suspend fun updateChatPreferences(
            streamEnabled: Boolean,
            autoScroll: Boolean,
            fontSizeScale: Float,
            showCitationsByDefault: Boolean,
        ) {
            connectionRepository.updateChatPreferences(
                streamEnabled = streamEnabled,
                autoScroll = autoScroll,
                fontSizeScale = fontSizeScale,
                showCitationsByDefault = showCitationsByDefault,
            )
        }

        override suspend fun updateLanguageTag(languageTag: String?) {
            connectionRepository.updateLanguageTag(languageTag)
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
