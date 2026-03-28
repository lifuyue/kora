package com.lifuyue.kora.core.testing

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

class MockWebServerRule : ExternalResource() {
    val server = MockWebServer()

    val baseUrl: String
        get() = server.url("/").toString()

    fun url(path: String = "/") = server.url(path)

    override fun before() {
        server.start()
    }

    override fun after() {
        server.shutdown()
    }

    fun enqueueJson(
        body: String,
        code: Int = 200,
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body.trimIndent()),
        )
    }

    fun enqueueSse(
        body: String,
        code: Int = 200,
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body.trimIndent()),
        )
    }

    fun takeRequest(
        timeout: Long = 1,
        unit: TimeUnit = TimeUnit.SECONDS,
    ): RecordedRequest =
        checkNotNull(server.takeRequest(timeout, unit)) {
            "Expected a recorded request within $timeout ${unit.name.lowercase()}, but none arrived."
        }
}
