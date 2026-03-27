package com.lifuyue.kora.core.database.connection

import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionValidationError
import com.lifuyue.kora.core.common.NetworkError
import com.lifuyue.kora.core.common.ThemeMode
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import com.lifuyue.kora.core.database.store.ConnectionPreferences
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import com.lifuyue.kora.core.network.ConnectionConfig
import com.lifuyue.kora.core.network.ConnectionValidator
import com.lifuyue.kora.core.network.FastGptApiFactory
import com.lifuyue.kora.core.network.MutableConnectionProvider
import com.lifuyue.kora.core.network.NetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class ConnectionRepository
    @Inject
    constructor(
        private val preferencesStore: ConnectionPreferencesStore,
        private val apiKeySecureStore: ApiKeySecureStore,
        private val connectionProvider: MutableConnectionProvider,
        private val apiFactory: FastGptApiFactory,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutableSnapshot = MutableStateFlow(loadSnapshot(ConnectionPreferences()))

        val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot.asStateFlow()

        init {
            connectionProvider.update(mutableSnapshot.value)
            scope.launch {
                preferencesStore.preferences.collectLatest { preferences ->
                    val snapshot = loadSnapshot(preferences)
                    mutableSnapshot.value = snapshot
                    connectionProvider.update(snapshot)
                }
            }
        }

        suspend fun saveConnection(
            serverBaseUrl: String,
            apiKey: String,
            selectedAppId: String,
            onboardingCompleted: Boolean,
            themeMode: ThemeMode = mutableSnapshot.value.appearancePreferences.themeMode,
            dynamicColorEnabled: Boolean = mutableSnapshot.value.appearancePreferences.dynamicColorEnabled,
            oledEnabled: Boolean = mutableSnapshot.value.appearancePreferences.oledEnabled,
            languageTag: String? = mutableSnapshot.value.appearancePreferences.languageTag,
        ) {
            val normalizedBaseUrl = ConnectionValidator.normalizeServerBaseUrl(serverBaseUrl)
            val trimmedApiKey = apiKey.trim()

            apiKeySecureStore.save(trimmedApiKey)
            preferencesStore.updateServerBaseUrl(normalizedBaseUrl)
            preferencesStore.updateApiKeyPresence(true)
            preferencesStore.updateSelectedAppId(selectedAppId)
            preferencesStore.updateOnboardingCompleted(onboardingCompleted)
            preferencesStore.updateThemeMode(themeMode)
            preferencesStore.updateDynamicColorEnabled(dynamicColorEnabled)
            preferencesStore.updateOledEnabled(oledEnabled)
            preferencesStore.updateLanguageTag(languageTag)

            publishSnapshot(
                ConnectionSnapshot(
                    serverBaseUrl = normalizedBaseUrl,
                    apiKey = trimmedApiKey,
                    selectedAppId = selectedAppId,
                    onboardingCompleted = onboardingCompleted,
                    appearancePreferences =
                        AppearancePreferences(
                            themeMode = themeMode,
                            dynamicColorEnabled = dynamicColorEnabled,
                            oledEnabled = oledEnabled,
                            languageTag = languageTag,
                        ),
                ),
            )
        }

        suspend fun updateAppearance(
            themeMode: ThemeMode,
            dynamicColorEnabled: Boolean,
            oledEnabled: Boolean,
            languageTag: String? = mutableSnapshot.value.appearancePreferences.languageTag,
        ) {
            preferencesStore.updateThemeMode(themeMode)
            preferencesStore.updateDynamicColorEnabled(dynamicColorEnabled)
            preferencesStore.updateOledEnabled(oledEnabled)
            preferencesStore.updateLanguageTag(languageTag)
        }

        suspend fun updateOnboardingCompleted(onboardingCompleted: Boolean) {
            preferencesStore.updateOnboardingCompleted(onboardingCompleted)
            publishSnapshot(
                mutableSnapshot.value.copy(
                    onboardingCompleted = onboardingCompleted,
                ),
            )
        }

        suspend fun updateSelectedAppId(selectedAppId: String) {
            preferencesStore.updateSelectedAppId(selectedAppId)
            publishSnapshot(
                mutableSnapshot.value.copy(
                    selectedAppId = selectedAppId,
                ),
            )
        }

        suspend fun clearConnection() {
            apiKeySecureStore.clear()
            preferencesStore.updateServerBaseUrl(null)
            preferencesStore.updateApiKeyPresence(false)
            preferencesStore.updateSelectedAppId(null)
            publishSnapshot(
                mutableSnapshot.value.copy(
                    serverBaseUrl = null,
                    apiKey = null,
                    selectedAppId = null,
                ),
            )
        }

        suspend fun testConnection(
            serverBaseUrl: String,
            apiKey: String,
        ): ConnectionTestResult {
            ConnectionValidator.validateServerBaseUrl(serverBaseUrl)?.let {
                return ConnectionTestResult.ValidationError(it)
            }
            ConnectionValidator.validateApiKey(apiKey)?.let {
                return ConnectionTestResult.ValidationError(it)
            }

            val normalizedBaseUrl = ConnectionValidator.normalizeServerBaseUrl(serverBaseUrl)
            var result: ConnectionTestResult = ConnectionTestResult.ValidationError(ConnectionValidationError.INVALID_SERVER_URL)
            val latencyMs =
                measureTimeMillis {
                    result = performConnectionTest(normalizedBaseUrl, apiKey.trim())
                }

            return when (result) {
                is ConnectionTestResult.Success ->
                    (result as ConnectionTestResult.Success).copy(latencyMs = latencyMs)
                is ConnectionTestResult.AuthError ->
                    (result as ConnectionTestResult.AuthError).copy(latencyMs = latencyMs)
                is ConnectionTestResult.ServerError ->
                    (result as ConnectionTestResult.ServerError).copy(latencyMs = latencyMs)
                else -> result
            }
        }

        private suspend fun performConnectionTest(
            normalizedBaseUrl: String,
            apiKey: String,
        ): ConnectionTestResult {
            return try {
                val response = apiFactory.create(normalizedBaseUrl, apiKey, enableDebugLogging = true).listApps()
                if (response.code !in 200..299) {
                    return mapServerError(
                        NetworkError(
                            code = response.code,
                            statusText = response.statusText,
                            message = response.message,
                        ),
                    )
                }

                val apps = response.data.orEmpty().map { ConnectionTestApp(id = it.id, name = it.name) }
                if (apps.isEmpty()) {
                    ConnectionTestResult.ValidationError(ConnectionValidationError.NO_AVAILABLE_APPS)
                } else {
                    ConnectionTestResult.Success(
                        normalizedBaseUrl = normalizedBaseUrl,
                        apps = apps,
                        latencyMs = 0L,
                    )
                }
            } catch (error: NetworkException) {
                mapServerError(error.networkError)
            } catch (error: IOException) {
                ConnectionTestResult.NetworkFailure(error.message ?: "Network request failed")
            }
        }

        private fun mapServerError(error: NetworkError): ConnectionTestResult =
            if (error.code in listOf(401, 403, 514) || error.statusText == "unAuthApiKey") {
                ConnectionTestResult.AuthError(error = error, latencyMs = 0L)
            } else {
                ConnectionTestResult.ServerError(error = error, latencyMs = 0L)
            }

        private fun loadSnapshot(preferences: ConnectionPreferences): ConnectionSnapshot =
            ConnectionSnapshot(
                serverBaseUrl = preferences.serverBaseUrl,
                apiKey = apiKeySecureStore.get(),
                selectedAppId = preferences.selectedAppId,
                onboardingCompleted = preferences.onboardingCompleted,
                appearancePreferences =
                    AppearancePreferences(
                        themeMode = preferences.themeMode,
                        dynamicColorEnabled = preferences.dynamicColorEnabled,
                        oledEnabled = preferences.oledEnabled,
                        languageTag = preferences.languageTag,
                    ),
            )

        private fun publishSnapshot(snapshot: ConnectionSnapshot) {
            mutableSnapshot.value = snapshot
            connectionProvider.update(snapshot)
        }

        companion object {
            fun redactApiKey(apiKey: String?): String = ConnectionConfig.redactApiKey(apiKey)
        }
    }
