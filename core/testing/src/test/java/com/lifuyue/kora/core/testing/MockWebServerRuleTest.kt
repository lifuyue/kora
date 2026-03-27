package com.lifuyue.kora.core.testing

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MockWebServerRuleTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val client = OkHttpClient()

    @Test
    fun servesJsonResponsesAndCapturesRequests() {
        serverRule.enqueueJson("""{"ok":true}""")

        client
            .newCall(
                Request
                    .Builder()
                    .url(serverRule.url("/health"))
                    .build(),
            ).execute()
            .use { response ->
                assertEquals(200, response.code)
                assertEquals("""{"ok":true}""", response.body!!.string())
            }

        val request = serverRule.takeRequest()
        assertEquals("/health", request.path)
        assertEquals("GET", request.method)
    }
}
