package com.lifuyue.kora.core.database.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lifuyue.kora.core.common.ConnectionType
import com.lifuyue.kora.core.common.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

class ConnectionPreferencesStore internal constructor(
    val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<ConnectionPreferences> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                ConnectionPreferences(
                    connectionType =
                        preferences[Keys.CONNECTION_TYPE]
                            ?.let { storedType -> ConnectionType.entries.firstOrNull { it.name == storedType } }
                            ?: inferLegacyConnectionType(preferences),
                    serverBaseUrl = preferences[Keys.SERVER_BASE_URL],
                    apiKeyPresent = preferences[Keys.SERVER_API_KEY_PRESENT] ?: false,
                    model = preferences[Keys.MODEL],
                    selectedAppId = preferences[Keys.SELECTED_APP_ID],
                    onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
                    themeMode =
                        preferences[Keys.THEME_MODE]
                            ?.let { storedMode -> ThemeMode.entries.firstOrNull { it.name == storedMode } }
                            ?: ThemeMode.DARK,
                    languageInitialized = preferences[Keys.LANGUAGE_INITIALIZED] ?: false,
                    languageTag = preferences[Keys.LANGUAGE_TAG],
                )
            }

    suspend fun updateServerBaseUrl(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.SERVER_BASE_URL)
            } else {
                preferences[Keys.SERVER_BASE_URL] = value
            }
        }
    }

    suspend fun updateConnectionType(value: ConnectionType) {
        dataStore.edit { preferences ->
            preferences[Keys.CONNECTION_TYPE] = value.name
        }
    }

    suspend fun updateApiKeyPresence(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SERVER_API_KEY_PRESENT] = value
        }
    }

    suspend fun updateModel(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.MODEL)
            } else {
                preferences[Keys.MODEL] = value
            }
        }
    }

    suspend fun updateSelectedAppId(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.SELECTED_APP_ID)
            } else {
                preferences[Keys.SELECTED_APP_ID] = value
            }
        }
    }

    suspend fun updateOnboardingCompleted(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = value
        }
    }

    suspend fun updateThemeMode(value: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = value.name
        }
    }

    suspend fun updateLanguageInitialized(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE_INITIALIZED] = value
        }
    }

    suspend fun updateLanguageTag(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.LANGUAGE_TAG)
            } else {
                preferences[Keys.LANGUAGE_TAG] = value
            }
        }
    }

    companion object {
        fun create(
            scope: CoroutineScope,
            produceFile: () -> File,
        ): ConnectionPreferencesStore =
            ConnectionPreferencesStore(
                dataStore =
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                        produceFile = {
                            produceFile().also { file ->
                                file.parentFile?.mkdirs()
                            }
                        },
                    ),
            )

        fun createForTest(
            scope: CoroutineScope,
            file: File,
        ): ConnectionPreferencesStore = create(scope = scope) { file }
    }

    object Keys {
        val CONNECTION_TYPE = stringPreferencesKey("connection_type")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val SERVER_API_KEY_PRESENT = booleanPreferencesKey("server_api_key_present")
        val MODEL = stringPreferencesKey("model")
        val SELECTED_APP_ID = stringPreferencesKey("selected_app_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_INITIALIZED = booleanPreferencesKey("language_initialized")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }

    private fun inferLegacyConnectionType(preferences: Preferences): ConnectionType =
        if (preferences[Keys.SELECTED_APP_ID].isNullOrBlank()) {
            ConnectionType.OPENAI_COMPATIBLE
        } else {
            ConnectionType.FAST_GPT
        }
}
