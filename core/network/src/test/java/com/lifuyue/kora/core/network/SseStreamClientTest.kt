package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.SseEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SseStreamClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: SseStreamClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            SseStreamClient(
                okHttpClient =
                    NetworkFactory.createOkHttpClient(
                        apiKeyProvider = { "fastgpt-secret" },
                    ),
                baseUrlProvider = StaticBaseUrlProvider(server.url("/").toString()),
            )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun streamsEventsInOrderAndAllowsFlowResponsesAfterDone() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(
                        """
                        event: answer
                        data: {"choices":[{"delta":{"content":"Hi"}}]}

                        event: toolCall
                        data: {"id":"tool-1"}

                        data: [DONE]

                        event: flowResponses
                        data: {"items":[{"id":"node-1"}]}

                        """.trimIndent(),
                    ),
            )

            val events =
                client.streamChatCompletions(
                    request =
                        ChatCompletionRequest(
                            appId = "app-1",
                            messages = listOf(ChatCompletionMessageParam(role = "user")),
                        ),
                ).toList()

            assertEquals(
                listOf(SseEvent.answer, SseEvent.toolCall, SseEvent.flowResponses),
                events.mapNotNull { if (it.isDone) null else it.event },
            )
            assertEquals("Bearer fastgpt-secret", server.takeRequest().getHeader("Authorization"))
        }

    @Test
    fun emitsKnownErrorEventInsteadOfCrashing() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(
                        """
                        event: error
                        data: {"code":514,"statusText":"unAuthApiKey","message":"invalid"}

                        """.trimIndent(),
                    ),
            )

            val events =
                client.streamChatCompletions(
                    request =
                        ChatCompletionRequest(
                            appId = "app-1",
                            messages = listOf(ChatCompletionMessageParam(role = "user")),
                        ),
                ).toList()

            assertTrue(events.any { it.event == SseEvent.error })
        }
}
