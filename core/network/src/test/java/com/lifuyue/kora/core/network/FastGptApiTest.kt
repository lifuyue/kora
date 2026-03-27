package com.lifuyue.kora.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FastGptApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: FastGptApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            NetworkFactory.createApi(
                baseUrl = server.url("/").toString(),
                apiKeyProvider = { "fastgpt-secret" },
            )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun appListUsesRetrofitAndAuthInterceptor() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 200,
                          "statusText": "",
                          "message": "",
                          "data": [
                            {
                              "_id": "app-1",
                              "name": "Kora",
                              "avatar": "bot",
                              "intro": "test",
                              "type": "simple",
                              "updateTime": "2026-03-27T00:00:00Z"
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
            )

            val response = api.listApps()
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/api/core/app/list", request.path)
            assertEquals("Bearer fastgpt-secret", request.getHeader("Authorization"))
            assertEquals("app-1", response.data!!.first().id)
        }

    @Test
    fun chatCompletionsSerializesDetailTrue() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "code": 200,
                          "statusText": "",
                          "message": "",
                          "data": {
                            "id": "chat-1",
                            "choices": [
                              {
                                "index": 0,
                                "finish_reason": "stop",
                                "message": {
                                  "role": "assistant",
                                  "content": "done"
                                }
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val response =
                api.chatCompletions(
                    ChatCompletionRequest(
                        appId = "app-1",
                        messages =
                            listOf(
                                ChatCompletionMessageParam(
                                    role = "user",
                                    content = JsonPrimitive("hello"),
                                ),
                            ),
                    ),
                )
            val request = server.takeRequest()

            assertEquals("/api/v1/chat/completions", request.path)
            assertEquals(true, request.body.readUtf8().replace(" ", "").contains(""""detail":true"""))
            assertEquals("done", response.data!!.choices.first().message.content)
        }
}
