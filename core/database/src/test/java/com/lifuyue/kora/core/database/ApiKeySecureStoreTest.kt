package com.lifuyue.kora.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.database.store.ApiKeySecureStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiKeySecureStoreTest {
    @Test
    fun saveReadAndClearApiKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ApiKeySecureStore(context)

        store.clear()
        assertNull(store.get())

        store.save("fastgpt-secret")
        assertEquals("fastgpt-secret", store.get())

        store.clear()
        assertNull(store.get())
    }
}
