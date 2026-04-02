package com.lifuyue.kora.core.database.connection

import com.lifuyue.kora.core.common.AppearancePreferences
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionTestApp
import com.lifuyue.kora.core.common.ConnectionTestResult
import com.lifuyue.kora.core.common.ConnectionValidationError
import com.lifuyue.kora.core.common.DIRECT_OPENAI_APP_ID
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
import com.lifuyue.kora.core.network.OpenAiCompatibleApiFactory
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
        private val openAiApiFactory: OpenAiCompatibleApiFactory,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutableSnapshot =
            MutableStateFlow(
                loadSnapshot(
                    ConnectionPreferences(
                        languageInitialized = true,
                        languageTag = DEFAULT_LANGUAGE_TAG,
                    ),
                ),
            )

        val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot.asStateFlow()

        init {
            connectionProvider.update(mutableSnapshot.value)
            scope.launch {
                preferencesStore.preferences.collectLatest { preferences ->
                    ensureLanguageInitialized(preferences)
                    val snapshot = loadSnapshot(preferences)
                    mutableSnapshot.value = snapshot
                    connectionProvider.update(snapshot)
                }
            }
        }

        suspend fun saveConnection(
            connectionType: ConnectionType,
            serverBaseUrl: String,
            apiKey: String,
            model: String? = null,
            selectedAppId: String? = null,
            onboardingCompleted: Boolean,
            themeMode: ThemeMode = mutableSnapshot.value.appearancePreferences.themeMode,
            languageTag: String? = mutableSnapshot.value.appearancePreferences.languageTag,
            showReasoningEntry: Boolean = mutableSnapshot.value.appearancePreferences.showReasoningEntry,
            streamResponses: Boolean = mutableSnapshot.value.appearancePreferences.streamResponses,
        ) {
            val normalizedBaseUrl = normalizeBaseUrl(connectionType, serverBaseUrl)
            val trimmedApiKey = apiKey.trim()
            val normalizedModel = model?.trim()?.ifBlank { null }
            val resolvedSelectedAppId =
                when (connectionType) {
                    ConnectionType.OPENAI_COMPATIBLE -> DIRECT_OPENAI_APP_ID
                    ConnectionType.FAST_GPT -> selectedAppId
                }

            apiKeySecureStore.save(trimmedApiKey)
            preferencesStore.updateConnectionType(connectionType)
            preferencesStore.updateServerBaseUrl(normalizedBaseUrl)
            preferencesStore.updateApiKeyPresence(true)
            preferencesStore.updateModel(normalizedModel)
            preferencesStore.updateSelectedAppId(resolvedSelectedAppId)
            preferencesStore.updateOnboardingCompleted(onboardingCompleted)
            preferencesStore.updateThemeMode(themeMode)
            preferencesStore.updateLanguageInitialized(true)
            preferencesStore.updateLanguageTag(languageTag)
            preferencesStore.updateShowReasoningEntry(showReasoningEntry)
            preferencesStore.updateStreamResponses(streamResponses)

            publishSnapshot(
                ConnectionSnapshot(
                    connectionType = connectionType,
                    serverBaseUrl = normalizedBaseUrl,
                    apiKey = trimmedApiKey,
                    model = normalizedModel,
                    selectedAppId = resolvedSelectedAppId,
                    onboardingCompleted = onboardingCompleted,
                    appearancePreferences =
                        AppearancePreferences(
                            themeMode = themeMode,
                            languageTag = languageTag,
                            showReasoningEntry = showReasoningEntry,
                            streamResponses = streamResponses,
                        ),
                ),
            )
        }

        suspend fun updateAppearance(
            themeMode: ThemeMode,
            languageTag: String? = mutableSnapshot.value.appearancePreferences.languageTag,
        ) {
            preferencesStore.updateThemeMode(themeMode)
            preferencesStore.updateLanguageInitialized(true)
            preferencesStore.updateLanguageTag(languageTag)
        }

        suspend fun updateLanguageTag(languageTag: String?) {
            preferencesStore.updateLanguageInitialized(true)
            preferencesStore.updateLanguageTag(languageTag)
        }

        suspend fun updateChatPreferences(
            showReasoningEntry: Boolean = mutableSnapshot.value.appearancePreferences.showReasoningEntry,
            streamResponses: Boolean = mutableSnapshot.value.appearancePreferences.streamResponses,
        ) {
            preferencesStore.updateShowReasoningEntry(showReasoningEntry)
            preferencesStore.updateStreamResponses(streamResponses)
            publishSnapshot(
                mutableSnapshot.value.copy(
                    appearancePreferences =
                        mutableSnapshot.value.appearancePreferences.copy(
                            showReasoningEntry = showReasoningEntry,
                            streamResponses = streamResponses,
                        ),
                ),
            )
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
            preferencesStore.updateConnectionType(ConnectionType.OPENAI_COMPATIBLE)
            preferencesStore.updateServerBaseUrl(null)
            preferencesStore.updateApiKeyPresence(false)
            preferencesStore.updateModel(null)
            preferencesStore.updateSelectedAppId(null)
            publishSnapshot(
                mutableSnapshot.value.copy(
                    connectionType = ConnectionType.OPENAI_COMPATIBLE,
                    serverBaseUrl = null,
                    apiKey = null,
                    model = null,
                    selectedAppId = null,
                ),
            )
        }

        suspend fun testConnection(
            connectionType: ConnectionType,
            serverBaseUrl: String,
            apiKey: String,
            model: String? = null,
        ): ConnectionTestResult {
            ConnectionValidator.validateServerBaseUrl(serverBaseUrl)?.let {
                return ConnectionTestResult.ValidationError(it)
            }
            ConnectionValidator.validateApiKey(connectionType, apiKey)?.let {
                return ConnectionTestResult.ValidationError(it)
            }
            if (connectionType == ConnectionType.OPENAI_COMPATIBLE && model.isNullOrBlank()) {
                return ConnectionTestResult.ValidationError(ConnectionValidationError.EMPTY_MODEL)
            }

            val normalizedBaseUrl = normalizeBaseUrl(connectionType, serverBaseUrl)
            var result: ConnectionTestResult = ConnectionTestResult.ValidationError(ConnectionValidationError.INVALID_SERVER_URL)
            val latencyMs =
                measureTimeMillis {
                    result = performConnectionTest(connectionType, normalizedBaseUrl, apiKey.trim(), model?.trim())
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
            connectionType: ConnectionType,
            normalizedBaseUrl: String,
            apiKey: String,
            model: String?,
        ): ConnectionTestResult {
            return try {
                when (connectionType) {
                    ConnectionType.FAST_GPT -> {
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
                    }
                    ConnectionType.OPENAI_COMPATIBLE -> {
                        val response = openAiApiFactory.create(normalizedBaseUrl, apiKey, enableDebugLogging = true).listModels()
                        val resolvedModel = model.orEmpty()
                        val modelExists = response.data.any { it.id == resolvedModel }
                        if (!modelExists) {
                            ConnectionTestResult.ServerError(
                                error =
                                    NetworkError(
                                        code = 404,
                                        statusText = "modelNotFound",
                                        message = "Model $resolvedModel is not available on this endpoint",
                                    ),
                                latencyMs = 0L,
                            )
                        } else {
                            ConnectionTestResult.Success(
                                normalizedBaseUrl = normalizedBaseUrl,
                                apps = emptyList(),
                                latencyMs = 0L,
                            )
                        }
                    }
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
                connectionType = preferences.connectionType,
                serverBaseUrl =
                    preferences.serverBaseUrl
                        ?.takeIf { it.isNotBlank() }
                        ?.let { normalizeBaseUrl(preferences.connectionType, it) },
                apiKey = apiKeySecureStore.get(),
                model = preferences.model,
                selectedAppId = preferences.selectedAppId,
                onboardingCompleted = preferences.onboardingCompleted,
                appearancePreferences =
                    AppearancePreferences(
                        themeMode = preferences.themeMode,
                        languageTag = resolvedLanguageTag(preferences),
                        showReasoningEntry = preferences.showReasoningEntry,
                        streamResponses = preferences.streamResponses,
                    ),
            )

        private fun publishSnapshot(snapshot: ConnectionSnapshot) {
            mutableSnapshot.value = snapshot
            connectionProvider.update(snapshot)
        }

        private suspend fun ensureLanguageInitialized(preferences: ConnectionPreferences) {
            if (preferences.languageInitialized) {
                return
            }
            preferencesStore.updateLanguageTag(DEFAULT_LANGUAGE_TAG)
            preferencesStore.updateLanguageInitialized(true)
        }

        private fun resolvedLanguageTag(preferences: ConnectionPreferences): String? =
            if (preferences.languageInitialized) {
                preferences.languageTag
            } else {
                DEFAULT_LANGUAGE_TAG
            }

        private fun normalizeBaseUrl(
            connectionType: ConnectionType,
            serverBaseUrl: String,
        ): String =
            when (connectionType) {
                ConnectionType.OPENAI_COMPATIBLE -> ConnectionConfig.normalizeOpenAiCompatibleBaseUrl(serverBaseUrl)
                ConnectionType.FAST_GPT -> ConnectionValidator.normalizeServerBaseUrl(serverBaseUrl)
            }

        companion object {
            private const val DEFAULT_LANGUAGE_TAG = "zh-CN"

            fun redactApiKey(apiKey: String?): String = ConnectionConfig.redactApiKey(apiKey)
        }
    }
