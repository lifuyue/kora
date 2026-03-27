package com.lifuyue.kora.feature.settings.testing

import com.lifuyue.kora.core.testing.MockWebServerRule
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsTestingHarnessSmokeTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val client = OkHttpClient()

    @Test
    fun settingsModuleCanConsumeSharedTestingHarness() {
        serverRule.enqueueJson("""{"status":"ok"}""")

        client
            .newCall(
                Request
                    .Builder()
                    .url(serverRule.url("/settings/ping"))
                    .build(),
            ).execute()
            .use { response ->
                assertEquals(200, response.code)
            }

        assertEquals("/settings/ping", serverRule.takeRequest().path)
    }
}
