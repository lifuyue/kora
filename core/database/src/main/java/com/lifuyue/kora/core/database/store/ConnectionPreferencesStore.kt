package com.lifuyue.kora.core.database.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
                    serverBaseUrl = preferences[Keys.SERVER_BASE_URL],
                    apiKeyPresent = preferences[Keys.SERVER_API_KEY_PRESENT] ?: false,
                    selectedAppId = preferences[Keys.SELECTED_APP_ID],
                    onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
                    streamEnabled = preferences[Keys.STREAM_ENABLED] ?: true,
                    autoScroll = preferences[Keys.AUTO_SCROLL] ?: true,
                    fontSizeScale = preferences[Keys.FONT_SIZE_SCALE] ?: 1f,
                    showCitationsByDefault = preferences[Keys.SHOW_CITATIONS_BY_DEFAULT] ?: true,
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

    suspend fun updateApiKeyPresence(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SERVER_API_KEY_PRESENT] = value
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

    suspend fun updateStreamEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STREAM_ENABLED] = value
        }
    }

    suspend fun updateAutoScroll(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_SCROLL] = value
        }
    }

    suspend fun updateFontSizeScale(value: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.FONT_SIZE_SCALE] = value
        }
    }

    suspend fun updateShowCitationsByDefault(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_CITATIONS_BY_DEFAULT] = value
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
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val SERVER_API_KEY_PRESENT = booleanPreferencesKey("server_api_key_present")
        val SELECTED_APP_ID = stringPreferencesKey("selected_app_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val SHOW_CITATIONS_BY_DEFAULT = booleanPreferencesKey("show_citations_by_default")
    }
}
