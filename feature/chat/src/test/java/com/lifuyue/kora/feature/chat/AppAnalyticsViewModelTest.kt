package com.lifuyue.kora.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ResponseEnvelope
import com.lifuyue.kora.core.network.AppAnalyticsSummaryDto
import com.lifuyue.kora.core.network.AppListItemDto
import com.lifuyue.kora.core.network.ChatCompletionRequest
import com.lifuyue.kora.core.network.ChatCompletionResponseDto
import com.lifuyue.kora.core.network.ChatHistoriesRequest
import com.lifuyue.kora.core.network.ChatHistoriesResponseData
import com.lifuyue.kora.core.network.ChatInitData
import com.lifuyue.kora.core.network.ChunkDeleteRequest
import com.lifuyue.kora.core.network.ChunkListRequest
import com.lifuyue.kora.core.network.ChunkListResponseDto
import com.lifuyue.kora.core.network.ChunkUpdateRequest
import com.lifuyue.kora.core.network.CollectionListRequest
import com.lifuyue.kora.core.network.CollectionSummaryDto
import com.lifuyue.kora.core.network.DatasetCreateRequest
import com.lifuyue.kora.core.network.DatasetDeleteRequest
import com.lifuyue.kora.core.network.DatasetListRequest
import com.lifuyue.kora.core.network.DatasetSummaryDto
import com.lifuyue.kora.core.network.DeleteChatItemRequest
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.LinkCollectionCreateRequest
import com.lifuyue.kora.core.network.PaginationRecordsRequest
import com.lifuyue.kora.core.network.PaginationRecordsResponseData
import com.lifuyue.kora.core.network.QuestionGuideRequest
import com.lifuyue.kora.core.network.SearchTestRequest
import com.lifuyue.kora.core.network.SearchTestResponseDto
import com.lifuyue.kora.core.network.ShareAuthFinishRequest
import com.lifuyue.kora.core.network.ShareAuthInitRequest
import com.lifuyue.kora.core.network.ShareAuthStartRequest
import com.lifuyue.kora.core.network.ShareAuthStateDto
import com.lifuyue.kora.core.network.ShareSessionBootstrapDto
import com.lifuyue.kora.core.network.TextCollectionCreateRequest
import com.lifuyue.kora.core.network.UpdateHistoryRequest
import com.lifuyue.kora.core.network.UpdateUserFeedbackRequest
import com.lifuyue.kora.core.network.UploadedAssetRef
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppAnalyticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun appDetailHidesAnalyticsEntryWhenProbeFails() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel =
                AppDetailViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1", "chatId" to null)),
                    api = FakeAnalyticsFastGptApi(analyticsThrows = true),
                    strings = FakeAnalyticsChatStrings(),
                )

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showAnalyticsEntry)
        }

    @Test
    fun analyticsViewModelLoadsSummary() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel =
                AppAnalyticsViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("appId" to "app-1")),
                    api = FakeAnalyticsFastGptApi(),
                )

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(20, state.requestCount)
            assertEquals(5, state.conversationCount)
            assertEquals(1000L, state.inputTokens)
            assertEquals(2000L, state.outputTokens)
        }
}

private class FakeAnalyticsChatStrings : ChatStrings(ApplicationProvider.getApplicationContext()) {
    override fun loadAppDetailFailed(): String = "load app detail failed"
}

private class FakeAnalyticsFastGptApi(
    private val analyticsThrows: Boolean = false,
) : FastGptApi {
    override suspend fun listApps(body: JsonObject): ResponseEnvelope<List<AppListItemDto>> = ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun initChat(appId: String, chatId: String?): ResponseEnvelope<ChatInitData> = ResponseEnvelope(code = 200, data = ChatInitData(appId = appId))

    override suspend fun getHistories(request: ChatHistoriesRequest): ResponseEnvelope<ChatHistoriesResponseData> =
        ResponseEnvelope(code = 200, data = ChatHistoriesResponseData())

    override suspend fun getPaginationRecords(request: PaginationRecordsRequest): ResponseEnvelope<PaginationRecordsResponseData> =
        ResponseEnvelope(code = 200, data = PaginationRecordsResponseData())

    override suspend fun updateHistory(request: UpdateHistoryRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun deleteHistory(appId: String, chatId: String): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun clearHistories(appId: String): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun getResData(appId: String, dataId: String, chatId: String?): ResponseEnvelope<JsonArray> =
        ResponseEnvelope(code = 200, data = JsonArray(emptyList()))

    override suspend fun createQuestionGuide(request: QuestionGuideRequest): ResponseEnvelope<List<String>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun uploadChatAttachment(file: MultipartBody.Part, appId: RequestBody, chatId: RequestBody?): ResponseEnvelope<UploadedAssetRef> =
        ResponseEnvelope(code = 200, data = null)

    override suspend fun listDatasets(request: DatasetListRequest): ResponseEnvelope<List<DatasetSummaryDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun createDataset(request: DatasetCreateRequest): ResponseEnvelope<DatasetSummaryDto> =
        ResponseEnvelope(code = 200, data = null)

    override suspend fun deleteDataset(request: DatasetDeleteRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun listCollections(request: CollectionListRequest): ResponseEnvelope<List<CollectionSummaryDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun createTextCollection(request: TextCollectionCreateRequest): ResponseEnvelope<CollectionSummaryDto> =
        ResponseEnvelope(code = 200, data = null)

    override suspend fun createLinkCollection(request: LinkCollectionCreateRequest): ResponseEnvelope<List<CollectionSummaryDto>> =
        ResponseEnvelope(code = 200, data = emptyList())

    override suspend fun createLocalFileCollection(
        file: MultipartBody.Part,
        datasetId: RequestBody,
        parentId: RequestBody?,
        trainingType: RequestBody,
    ): ResponseEnvelope<CollectionSummaryDto> = ResponseEnvelope(code = 200, data = null)

    override suspend fun listChunkData(request: ChunkListRequest): ResponseEnvelope<ChunkListResponseDto> =
        ResponseEnvelope(code = 200, data = ChunkListResponseDto())

    override suspend fun updateChunkData(request: ChunkUpdateRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun deleteChunkData(request: ChunkDeleteRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun searchTest(request: SearchTestRequest): ResponseEnvelope<SearchTestResponseDto> =
        ResponseEnvelope(code = 200, data = SearchTestResponseDto())

    override suspend fun deleteChatItem(request: DeleteChatItemRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun updateUserFeedback(request: UpdateUserFeedbackRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun shareAuthInit(request: ShareAuthInitRequest): ResponseEnvelope<ShareAuthStateDto> =
        ResponseEnvelope(code = 200, data = ShareAuthStateDto(uid = request.token))

    override suspend fun shareAuthStart(request: ShareAuthStartRequest): ResponseEnvelope<ShareAuthStateDto> =
        ResponseEnvelope(code = 200, data = ShareAuthStateDto(uid = request.token))

    override suspend fun shareAuthFinish(request: ShareAuthFinishRequest): ResponseEnvelope<JsonObject> =
        ResponseEnvelope(code = 200, data = JsonObject(emptyMap()))

    override suspend fun initShareSession(shareId: String, outLinkUid: String, chatId: String?): ResponseEnvelope<ShareSessionBootstrapDto> =
        ResponseEnvelope(code = 200, data = ShareSessionBootstrapDto(chatId = chatId ?: "share-chat", appId = "app-share", title = "Share", appName = "Shared App"))

    override suspend fun getAppAnalytics(
        appId: String,
        range: String?,
    ): ResponseEnvelope<AppAnalyticsSummaryDto> {
        if (analyticsThrows) {
            throw IllegalStateException("403")
        }
        return ResponseEnvelope(
            code = 200,
            data = AppAnalyticsSummaryDto(requestCount = 20, conversationCount = 5, inputTokens = 1000L, outputTokens = 2000L),
        )
    }

    override suspend fun chatCompletions(request: ChatCompletionRequest): ResponseEnvelope<ChatCompletionResponseDto> =
        ResponseEnvelope(code = 200, data = ChatCompletionResponseDto())
}
