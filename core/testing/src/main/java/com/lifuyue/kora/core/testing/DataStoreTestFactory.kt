package com.lifuyue.kora.core.testing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.database.store.ConnectionPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

object DataStoreTestFactory {
    fun createPreferencesDataStore(
        name: String = "test.preferences_pb",
        context: Context = ApplicationProvider.getApplicationContext(),
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                context.testFile(name.ensurePreferencesExtension()).also { file ->
                    file.parentFile?.mkdirs()
                }
            },
        )

    fun createConnectionPreferencesStore(
        name: String = "connection.preferences_pb",
        context: Context = ApplicationProvider.getApplicationContext(),
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ): ConnectionPreferencesStore =
        ConnectionPreferencesStore.createForTest(
            scope = scope,
            file = context.testFile(name.ensurePreferencesExtension()),
        )

    private fun Context.testFile(name: String): File = File(cacheDir, "core-testing/datastore/$name")

    private fun String.ensurePreferencesExtension(): String =
        if (endsWith(".preferences_pb")) {
            this
        } else {
            "$this.preferences_pb"
        }
}
