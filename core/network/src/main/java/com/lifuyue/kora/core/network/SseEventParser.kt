package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.SseEvent
import com.lifuyue.kora.core.common.sseEventFromWireName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class SseEventData(
    val event: SseEvent?,
    val rawEventName: String?,
    val payload: JsonElement?,
    val rawData: String,
    val isDone: Boolean = false,
    val isMalformed: Boolean = false,
    val parseExceptionMessage: String? = null,
) {
    val done: Boolean
        get() = isDone

    val rawPayload: String
        get() = rawData

    val unknownEvent: String?
        get() = if (event == null && !isDone) rawEventName else null

    val parseError: String?
        get() = parseExceptionMessage
}

class SseEventParser(
    private val json: Json = NetworkJson.default,
) {
    private var currentEventName: String? = null
    private val dataLines = mutableListOf<String>()

    fun appendLine(line: String): SseEventData? {
        return when {
            line.isBlank() -> flush()
            line.startsWith("event:") -> {
                currentEventName = line.substringAfter("event:").trim()
                null
            }
            line.startsWith("data:") -> {
                dataLines += line.substringAfter("data:").trimStart()
                null
            }
            dataLines.isNotEmpty() -> {
                dataLines[dataLines.lastIndex] = dataLines.last() + "\n" + line
                null
            }
            else -> null
        }
    }

    fun parseLine(line: String): SseEventData? = appendLine(line)

    fun flush(): SseEventData? {
        if (currentEventName == null && dataLines.isEmpty()) {
            return null
        }

        val rawData = dataLines.joinToString(separator = "\n")
        val eventName = currentEventName
        currentEventName = null
        dataLines.clear()

        if (rawData == "[DONE]") {
            return SseEventData(
                event = eventName?.let(::sseEventFromWireName),
                rawEventName = eventName,
                payload = null,
                rawData = rawData,
                isDone = true,
            )
        }

        val parsed = runCatching { json.parseToJsonElement(rawData) }
        return SseEventData(
            event = eventName?.let(::sseEventFromWireName),
            rawEventName = eventName,
            payload = parsed.getOrNull(),
            rawData = rawData,
            isMalformed = parsed.isFailure,
            parseExceptionMessage = parsed.exceptionOrNull()?.message,
        )
    }
}
