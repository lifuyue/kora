package com.lifuyue.kora.core.network

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class M6FoundationNetworkModelsTest {
    @Test
    fun uploadedAssetRefParsesAttachmentUploadPayload() {
        val payload =
            """
            {
              "name": "diagram.png",
              "url": "https://cdn.example.com/diagram.png",
              "key": "chat/diagram.png",
              "mimeType": "image/png",
              "size": 2048
            }
            """.trimIndent()

        val dto = NetworkJson.default.decodeFromString<UploadedAssetRef>(payload)

        assertEquals("diagram.png", dto.name)
        assertEquals("https://cdn.example.com/diagram.png", dto.url)
        assertEquals("chat/diagram.png", dto.key)
        assertEquals("image/png", dto.mimeType)
        assertEquals(2048L, dto.size)
    }

    @Test
    fun shareBootstrapDtoParsesOutLinkInitPayload() {
        val payload =
            """
            {
              "chatId": "share-chat-1",
              "appId": "app-1",
              "title": "Shared Session",
              "appName": "Kora",
              "userAvatar": "https://example.com/avatar.png"
            }
            """.trimIndent()

        val dto = NetworkJson.default.decodeFromString<ShareSessionBootstrapDto>(payload)

        assertEquals("share-chat-1", dto.chatId)
        assertEquals("app-1", dto.appId)
        assertEquals("Shared Session", dto.title)
        assertEquals("Kora", dto.appName)
    }

    @Test
    fun analyticsSummaryDtoParsesUsageTotals() {
        val payload =
            """
            {
              "requestCount": 12,
              "conversationCount": 4,
              "inputTokens": 128,
              "outputTokens": 256
            }
            """.trimIndent()

        val dto = NetworkJson.default.decodeFromString<AppAnalyticsSummaryDto>(payload)

        assertEquals(12, dto.requestCount)
        assertEquals(4, dto.conversationCount)
        assertEquals(128L, dto.inputTokens)
        assertEquals(256L, dto.outputTokens)
    }
}
