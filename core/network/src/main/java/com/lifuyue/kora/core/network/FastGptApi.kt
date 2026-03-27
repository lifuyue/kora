package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ResponseEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FastGptApi {
    @POST("api/core/app/list")
    suspend fun listApps(
        @Body body: JsonObject = JsonObject(emptyMap()),
    ): ResponseEnvelope<List<AppListItemDto>>

    suspend fun getAppList(): ResponseEnvelope<List<AppListItemDto>> = listApps()

    @GET("api/core/chat/init")
    suspend fun initChat(
        @Query("appId") appId: String,
        @Query("chatId") chatId: String? = null,
    ): ResponseEnvelope<ChatInitData>

    @POST("api/core/chat/getHistories")
    suspend fun getHistories(
        @Body request: ChatHistoriesRequest,
    ): ResponseEnvelope<ChatHistoriesResponseData>

    @POST("api/core/chat/getPaginationRecords")
    suspend fun getPaginationRecords(
        @Body request: PaginationRecordsRequest,
    ): ResponseEnvelope<PaginationRecordsResponseData>

    @POST("api/v1/chat/completions")
    suspend fun chatCompletions(
        @Body request: ChatCompletionRequest,
    ): ResponseEnvelope<ChatCompletionResponseDto>
}
