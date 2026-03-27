package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ConnectionValidationError
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionValidatorTest {
    @Test
    fun normalizeServerBaseUrlRemovesDuplicateApiSuffix() {
        assertEquals("https://api.fastgpt.in/", ConnectionValidator.normalizeServerBaseUrl("https://api.fastgpt.in/api"))
        assertEquals("https://api.fastgpt.in/", ConnectionValidator.normalizeServerBaseUrl("https://api.fastgpt.in/"))
        assertEquals("https://api.fastgpt.in/", ConnectionValidator.normalizeServerBaseUrl("https://api.fastgpt.in"))
    }

    @Test
    fun validateServerBaseUrlRejectsUnsupportedValues() {
        assertEquals(ConnectionValidationError.EMPTY_SERVER_URL, ConnectionValidator.validateServerBaseUrl(" "))
        assertEquals(ConnectionValidationError.INVALID_SERVER_URL, ConnectionValidator.validateServerBaseUrl("ftp://example.com"))
    }

    @Test
    fun validateApiKeyEnforcesFastGptPrefix() {
        assertEquals(ConnectionValidationError.EMPTY_API_KEY, ConnectionValidator.validateApiKey(""))
        assertEquals(ConnectionValidationError.INVALID_API_KEY, ConnectionValidator.validateApiKey("sk-test"))
        assertEquals(null, ConnectionValidator.validateApiKey("fastgpt-secret"))
    }
}
