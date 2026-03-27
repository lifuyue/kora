package com.lifuyue.kora.core.network

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.BufferedSource

class ChatStreamCollector(
    private val parser: SseEventParser = SseEventParser(),
) {
    suspend fun collect(
        source: BufferedSource,
        onFrame: suspend (SseEventData) -> Unit,
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val line = source.readUtf8Line() ?: break
            val frame = parser.appendLine(line)
            if (frame != null) {
                onFrame(frame)
            }
        }
        val remaining = parser.flush()
        if (remaining != null) {
            onFrame(remaining)
        }
    }
}
