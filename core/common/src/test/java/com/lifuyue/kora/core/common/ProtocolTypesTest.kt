package com.lifuyue.kora.core.common

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtocolTypesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun responseEnvelopeDeserializesWithStatusText() {
        val envelope =
            json.decodeFromString<ResponseEnvelope<String>>(
                """
                {
                  "code": 403,
                  "statusText": "unAuthorization",
                  "message": "denied",
                  "data": "payload"
                }
                """.trimIndent(),
            )

        assertEquals(403, envelope.code)
        assertEquals("unAuthorization", envelope.statusText)
        assertEquals("denied", envelope.message)
        assertEquals("payload", envelope.data)
    }

    @Test
    fun networkErrorMapsFromEnvelope() {
        val envelope =
            ResponseEnvelope(
                code = 500,
                statusText = "error",
                message = "boom",
                data = JsonNull,
            )

        val error = envelope.toNetworkError()

        assertEquals(500, error.code)
        assertEquals("error", error.statusText)
        assertEquals("boom", error.message)
        assertEquals(JsonNull, error.data)
    }

    @Test
    fun networkErrorDefaultsDataToNull() {
        val error =
            NetworkError(
                code = 400,
                statusText = "badRequest",
                message = "invalid",
            )

        assertNull(error.data)
    }

    @Test
    fun chatSourceMatchesFastGptValues() {
        assertEquals(
            listOf(
                "test",
                "online",
                "share",
                "api",
                "cronJob",
                "team",
                "feishu",
                "official_account",
                "wecom",
                "wechat",
                "mcp",
            ),
            ChatSource.entries.map { it.name },
        )
    }

    @Test
    fun sseEventMatchesFastGptValues() {
        assertEquals(
            listOf(
                "error",
                "workflowDuration",
                "answer",
                "fastAnswer",
                "flowNodeStatus",
                "flowNodeResponse",
                "toolCall",
                "toolParams",
                "toolResponse",
                "flowResponses",
                "updateVariables",
                "interactive",
                "plan",
                "stepTitle",
                "collectionForm",
                "topAgentConfig",
            ),
            SseEvent.entries.map { it.name },
        )
    }

    @Test
    fun chatRoleAndFileTypeSerializeUsingProtocolNames() {
        assertEquals("\"System\"", json.encodeToString(ChatRole.System))
        assertEquals("\"Human\"", json.encodeToString(ChatRole.Human))
        assertEquals("\"AI\"", json.encodeToString(ChatRole.AI))
        assertEquals("\"image\"", json.encodeToString(ChatFileType.image))
        assertEquals("\"file\"", json.encodeToString(ChatFileType.file))
    }

    @Test
    fun sseEventFindsKnownWireName() {
        assertEquals(SseEvent.flowNodeResponse, sseEventFromWireName("flowNodeResponse"))
        assertEquals(null, sseEventFromWireName("unknown"))
    }

    @Test
    fun connectionSnapshotReflectsConnectionState() {
        val disconnected = ConnectionSnapshot()
        val connected =
            ConnectionSnapshot(
                connectionType = ConnectionType.FAST_GPT,
                serverBaseUrl = "https://api.fastgpt.in/",
                apiKey = "fastgpt-secret",
                selectedAppId = "app-1",
            )
        val openAiConnected =
            ConnectionSnapshot(
                connectionType = ConnectionType.OPENAI_COMPATIBLE,
                serverBaseUrl = "https://api.openai.com/v1",
                apiKey = "openai-secret",
                model = "gpt-4o-mini",
            )

        assertEquals(false, disconnected.hasValidConnection)
        assertEquals(true, connected.hasApiKey)
        assertEquals(true, connected.hasValidConnection)
        assertEquals(true, openAiConnected.hasValidConnection)
    }

    @Test
    fun appearancePreferencesDefaultsMatchM3Behavior() {
        val preferences = AppearancePreferences()

        assertEquals(ThemeMode.SYSTEM, preferences.themeMode)
        assertEquals(false, preferences.dynamicColorEnabled)
        assertEquals(false, preferences.oledEnabled)
        assertNull(preferences.languageTag)
    }
}
