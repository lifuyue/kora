package com.lifuyue.kora.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionConfigTest {
    @Test
    fun normalizeBaseUrlAddsSchemeAndStripsTrailingApi() {
        assertEquals(
            "https://api.fastgpt.in/",
            ConnectionConfig.normalizeBaseUrl("api.fastgpt.in/api"),
        )
    }

    @Test
    fun normalizeBaseUrlPreservesNestedPath() {
        assertEquals(
            "https://example.com/proxy/",
            ConnectionConfig.normalizeBaseUrl("https://example.com/proxy/api"),
        )
    }

    @Test
    fun normalizeOpenAiCompatibleBaseUrlStripsTrailingV1() {
        assertEquals(
            "https://api.siliconflow.cn/",
            ConnectionConfig.normalizeOpenAiCompatibleBaseUrl("https://api.siliconflow.cn/v1"),
        )
    }

    @Test
    fun normalizeOpenAiCompatibleBaseUrlPreservesNestedProxyPrefix() {
        assertEquals(
            "https://example.com/openai/",
            ConnectionConfig.normalizeOpenAiCompatibleBaseUrl("https://example.com/openai/v1"),
        )
    }

    @Test
    fun apiKeyValidationRequiresFastGptPrefix() {
        assertTrue(ConnectionConfig.isValidApiKey("fastgpt-secret"))
        assertFalse(ConnectionConfig.isValidApiKey("sk-secret"))
    }

    @Test
    fun redactApiKeyHidesMiddleCharacters() {
        assertEquals("fastgpt-***7890", ConnectionConfig.redactApiKey("fastgpt-1234567890"))
    }
}
