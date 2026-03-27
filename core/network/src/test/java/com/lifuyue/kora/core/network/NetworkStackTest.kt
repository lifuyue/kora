package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.SseEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStackTest {
    private val json = NetworkJson.default

    @Test
    fun retrofitParsesSuccessEnvelope() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 200,
                          "statusText": "",
                          "message": "ok",
                          "data": [
                            {
                              "_id": "app-1",
                              "name": "Kora",
                              "avatar": "avatar",
                              "intro": "intro",
                              "type": "simple",
                              "updateTime": "2026-03-27T00:00:00Z",
                              "tmbId": "tmb-1",
                              "parentId": null,
                              "permission": {
                                "hasReadPer": true
                              },
                              "sourceMember": {}
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
            )
            server.start()

            val api = createRetrofit(server.url("/").toString(), OkHttpClient()).create(FastGptApi::class.java)

            val response = api.listApps()

            assertEquals(200, response.code)
            assertEquals("Kora", response.data?.first()?.name)

            server.shutdown()
        }

    @Test
    fun streamingClientPreservesEventOrderAndDoneBoundary() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(
                        """
                        event: answer
                        data: {"choices":[{"delta":{"content":"Hi"}}]}

                        event: toolCall
                        data: {"toolName":"search"}

                        data: [DONE]

                        event: flowResponses
                        data: {"items":[{"id":"node-1"}]}

                        """.trimIndent(),
                    ),
            )
            server.start()

            val client =
                SseStreamClient(
                    okHttpClient = OkHttpClient(),
                    json = json,
                    baseUrlProvider = StaticBaseUrlProvider(server.url("/").toString()),
                )

            val events =
                client.streamChatCompletions(
                    ChatCompletionRequest(
                        appId = "app-1",
                        chatId = null,
                        messages =
                            listOf(
                                ChatCompletionMessageParam(
                                    role = "user",
                                    content = Json.parseToJsonElement("\"hello\""),
                                ),
                            ),
                        responseChatItemId = "resp-1",
                        stream = true,
                        detail = true,
                    ),
                ).toList()

            assertEquals(listOf(SseEvent.answer, SseEvent.toolCall, null, SseEvent.flowResponses), events.map { it.event })
            assertTrue(events[2].isDone)
            assertEquals("search", events[1].payload?.jsonObject?.get("toolName")?.jsonPrimitive?.content)
            assertEquals(
                "node-1",
                events[3].payload?.jsonObject?.get("items").toString().contains("node-1").let { if (it) "node-1" else "" },
            )

            server.shutdown()
        }

    @Test
    fun streamingClientMapsErrorResponseEnvelope() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 514,
                          "statusText": "unAuthApiKey",
                          "message": "invalid key",
                          "data": null
                        }
                        """.trimIndent(),
                    ),
            )
            server.start()

            val client =
                SseStreamClient(
                    okHttpClient = OkHttpClient(),
                    json = json,
                    baseUrlProvider = StaticBaseUrlProvider(server.url("/").toString()),
                )

            try {
                client.streamChatCompletions(
                    ChatCompletionRequest(
                        appId = "app-1",
                        messages =
                            listOf(
                                ChatCompletionMessageParam(
                                    role = "user",
                                    content = Json.parseToJsonElement("\"hello\""),
                                ),
                            ),
                        stream = true,
                        detail = true,
                    ),
                ).toList()
            } catch (error: NetworkException) {
                assertEquals(514, error.networkError.code)
                assertEquals("unAuthApiKey", error.networkError.statusText)
                server.shutdown()
                return@runBlocking
            }

            server.shutdown()
            throw AssertionError("Expected NetworkException")
        }
}
