package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ResponseEnvelope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @PUT("api/core/chat/updateHistory")
    suspend fun updateHistory(
        @Body request: UpdateHistoryRequest,
    ): ResponseEnvelope<JsonObject>

    @DELETE("api/core/chat/delHistory")
    suspend fun deleteHistory(
        @Query("appId") appId: String,
        @Query("chatId") chatId: String,
    ): ResponseEnvelope<JsonObject>

    @DELETE("api/core/chat/clearHistories")
    suspend fun clearHistories(
        @Query("appId") appId: String,
    ): ResponseEnvelope<JsonObject>

    @GET("api/core/chat/getResData")
    suspend fun getResData(
        @Query("appId") appId: String,
        @Query("dataId") dataId: String,
        @Query("chatId") chatId: String? = null,
    ): ResponseEnvelope<JsonArray>

    @POST("api/core/chat/item/delete")
    suspend fun deleteChatItem(
        @Body request: DeleteChatItemRequest,
    ): ResponseEnvelope<JsonObject>

    @POST("api/core/chat/feedback/updateUserFeedback")
    suspend fun updateUserFeedback(
        @Body request: UpdateUserFeedbackRequest,
    ): ResponseEnvelope<JsonObject>

    @POST("api/v1/chat/completions")
    suspend fun chatCompletions(
        @Body request: ChatCompletionRequest,
    ): ResponseEnvelope<ChatCompletionResponseDto>
}
