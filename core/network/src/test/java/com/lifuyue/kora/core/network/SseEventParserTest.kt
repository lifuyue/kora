package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.SseEvent
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseEventParserTest {
    private val parser = SseEventParser()

    @Test
    fun parsesSingleFrame() {
        assertNull(parser.parseLine("event: answer"))
        assertNull(parser.parseLine("""data: {"choices":[{"delta":{"content":"hi"}}]}"""))

        val event = parser.parseLine("")

        assertEquals(SseEvent.answer, event?.event)
        assertEquals(
            "hi",
            event?.payload?.jsonObject?.get("choices")?.jsonArray?.first()?.jsonObject
                ?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun joinsMultiLineData() {
        assertNull(parser.parseLine("event: plan"))
        assertNull(parser.parseLine("""data: {"title":"one","body":"line1"""))
        assertNull(parser.parseLine("""data: line2"}"""))

        val event = parser.parseLine("")

        assertEquals(SseEvent.plan, event?.event)
        assertEquals("{\"title\":\"one\",\"body\":\"line1\nline2\"}", event?.rawPayload)
    }

    @Test
    fun marksDoneSentinel() {
        assertNull(parser.parseLine("data: [DONE]"))

        val event = parser.parseLine("")

        assertTrue(event?.done == true)
        assertNull(event?.event)
    }

    @Test
    fun keepsUnknownEventAndPayload() {
        assertNull(parser.parseLine("event: customEvent"))
        assertNull(parser.parseLine("""data: {"value":1}"""))

        val event = parser.parseLine("")

        assertEquals("customEvent", event?.unknownEvent)
        assertEquals(1, event?.payload?.jsonObject?.get("value")?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun reportsInvalidJsonWithoutCrashing() {
        assertNull(parser.parseLine("event: answer"))
        assertNull(parser.parseLine("""data: {"broken":"""))

        val event = parser.parseLine("")

        assertEquals(SseEvent.answer, event?.event)
        assertNotNull(event?.parseError)
        assertEquals("""{"broken":""", event?.rawPayload)
    }

    @Test
    fun continuesParsingAfterDoneForFlowResponses() {
        assertNull(parser.parseLine("data: [DONE]"))
        val done = parser.parseLine("")

        assertTrue(done?.done == true)

        assertNull(parser.parseLine("event: flowResponses"))
        assertNull(parser.parseLine("""data: {"items":[1,2]}"""))
        val flowResponses = parser.parseLine("")

        assertEquals(SseEvent.flowResponses, flowResponses?.event)
        assertEquals("""{"items":[1,2]}""", flowResponses?.rawPayload)
    }
}
