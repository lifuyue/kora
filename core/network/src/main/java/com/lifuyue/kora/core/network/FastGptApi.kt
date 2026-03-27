package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ResponseEnvelope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @POST("api/core/ai/agent/v2/createQuestionGuide")
    suspend fun createQuestionGuide(
        @Body request: QuestionGuideRequest,
    ): ResponseEnvelope<List<String>>

    @POST("api/core/dataset/list")
    suspend fun listDatasets(
        @Body request: DatasetListRequest = DatasetListRequest(),
    ): ResponseEnvelope<List<DatasetSummaryDto>>

    @POST("api/core/dataset/create")
    suspend fun createDataset(
        @Body request: DatasetCreateRequest,
    ): ResponseEnvelope<DatasetSummaryDto>

    @POST("api/core/dataset/delete")
    suspend fun deleteDataset(
        @Body request: DatasetDeleteRequest,
    ): ResponseEnvelope<JsonObject>

    @POST("api/core/dataset/collection/list")
    suspend fun listCollections(
        @Body request: CollectionListRequest,
    ): ResponseEnvelope<List<CollectionSummaryDto>>

    @POST("api/core/dataset/collection/create/text")
    suspend fun createTextCollection(
        @Body request: TextCollectionCreateRequest,
    ): ResponseEnvelope<CollectionSummaryDto>

    @POST("api/core/dataset/collection/create/link")
    suspend fun createLinkCollection(
        @Body request: LinkCollectionCreateRequest,
    ): ResponseEnvelope<List<CollectionSummaryDto>>

    @Multipart
    @POST("api/core/dataset/collection/create/localFile")
    suspend fun createLocalFileCollection(
        @Part file: MultipartBody.Part,
        @Part("datasetId") datasetId: RequestBody,
        @Part("parentId") parentId: RequestBody? = null,
        @Part("trainingType") trainingType: RequestBody,
    ): ResponseEnvelope<CollectionSummaryDto>

    @POST("api/core/dataset/data/list")
    suspend fun listChunkData(
        @Body request: ChunkListRequest,
    ): ResponseEnvelope<ChunkListResponseDto>

    @PUT("api/core/dataset/data/update")
    suspend fun updateChunkData(
        @Body request: ChunkUpdateRequest,
    ): ResponseEnvelope<JsonObject>

    @POST("api/core/dataset/data/delete")
    suspend fun deleteChunkData(
        @Body request: ChunkDeleteRequest,
    ): ResponseEnvelope<JsonObject>

    @POST("api/core/dataset/searchTest")
    suspend fun searchTest(
        @Body request: SearchTestRequest,
    ): ResponseEnvelope<SearchTestResponseDto>

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
