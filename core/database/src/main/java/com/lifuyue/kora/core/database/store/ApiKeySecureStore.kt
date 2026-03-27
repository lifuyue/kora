package com.lifuyue.kora.core.database.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeySecureStore(
    context: Context,
    private val preferencesName: String = DEFAULT_PREFERENCES_NAME,
) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences by lazy { createSharedPreferences() }

    fun save(apiKey: String) {
        preferences.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun get(): String? = preferences.getString(KEY_API_KEY, null)

    fun clear() {
        preferences.edit().remove(KEY_API_KEY).apply()
    }

    private fun createSharedPreferences(): SharedPreferences =
        try {
            val masterKey =
                MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            EncryptedSharedPreferences.create(
                appContext,
                preferencesName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            appContext.getSharedPreferences("${preferencesName}_fallback", Context.MODE_PRIVATE)
        }

    private companion object {
        const val DEFAULT_PREFERENCES_NAME = "kora_secure_connection"
        const val KEY_API_KEY = "api_key"
    }
}
