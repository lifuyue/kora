package com.lifuyue.kora.core.common

import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {
    @Test
    fun successWrapsData() {
        val result: AppResult<String> = AppResult.Success("kora")

        assertTrue(result is AppResult.Success)
        assertEquals("kora", (result as AppResult.Success).data)
    }

    @Test
    fun errorWrapsNetworkError() {
        val error =
            NetworkError(
                code = 514,
                statusText = "unAuthApiKey",
                message = "API key invalid",
                data = JsonNull,
            )

        val result: AppResult<Nothing> = AppResult.Error(error)

        assertTrue(result is AppResult.Error)
        assertSame(error, (result as AppResult.Error).error)
    }

    @Test
    fun loadingIsSingleton() {
        assertSame(AppResult.Loading, AppResult.Loading)
    }
}
