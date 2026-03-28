package com.lifuyue.kora.feature.knowledge

import android.content.Context
import android.net.Uri
import com.lifuyue.kora.core.database.dao.CachedCollectionDao
import com.lifuyue.kora.core.database.dao.CachedDatasetDao
import com.lifuyue.kora.core.database.dao.ImportTaskDao
import com.lifuyue.kora.core.database.entity.CachedCollectionEntity
import com.lifuyue.kora.core.database.entity.CachedDatasetEntity
import com.lifuyue.kora.core.database.entity.ImportTaskEntity
import com.lifuyue.kora.core.network.ChunkDeleteRequest
import com.lifuyue.kora.core.network.ChunkListRequest
import com.lifuyue.kora.core.network.ChunkSummaryDto
import com.lifuyue.kora.core.network.ChunkUpdateRequest
import com.lifuyue.kora.core.network.CollectionListRequest
import com.lifuyue.kora.core.network.CollectionSummaryDto
import com.lifuyue.kora.core.network.DatasetCreateRequest
import com.lifuyue.kora.core.network.DatasetDeleteRequest
import com.lifuyue.kora.core.network.DatasetListRequest
import com.lifuyue.kora.core.network.DatasetSummaryDto
import com.lifuyue.kora.core.network.FastGptApi
import com.lifuyue.kora.core.network.LinkCollectionCreateItemRequest
import com.lifuyue.kora.core.network.LinkCollectionCreateRequest
import com.lifuyue.kora.core.network.SearchTestRequest
import com.lifuyue.kora.core.network.SearchTestResponseDto
import com.lifuyue.kora.core.network.TextCollectionCreateRequest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

interface KnowledgeRepository {
    fun observeDatasets(): Flow<List<DatasetListItemUiModel>>

    fun observeDataset(datasetId: String): Flow<DatasetListItemUiModel?>

    suspend fun refreshDatasets(query: String = "")

    suspend fun createDataset(name: String)

    suspend fun deleteDataset(datasetId: String)

    fun observeCollections(datasetId: String): Flow<List<CollectionListItemUiModel>>

    fun observeImportTasks(datasetId: String): Flow<List<ImportTaskUiModel>>

    suspend fun refreshCollections(datasetId: String)

    suspend fun importText(
        datasetId: String,
        name: String,
        text: String,
        trainingType: String,
    )

    suspend fun importLinks(
        datasetId: String,
        urls: List<String>,
        selector: String?,
        trainingType: String,
    )

    suspend fun importDocument(
        datasetId: String,
        uri: Uri,
        displayName: String,
        trainingType: String,
    )

    suspend fun listChunks(
        datasetId: String,
        collectionId: String,
        offset: Int = 0,
        pageSize: Int = 20,
    ): List<ChunkItemUiModel>

    suspend fun updateChunk(
        dataId: String,
        question: String,
        answer: String,
        forbid: Boolean,
    )

    suspend fun deleteChunk(dataId: String)

    suspend fun search(
        datasetId: String,
        query: String,
        searchMode: String,
        similarity: Double?,
        embeddingWeight: Double?,
        usingReRank: Boolean,
    ): Triple<String, String, List<SearchResultUiModel>>
}

@Singleton
class RoomBackedKnowledgeRepository
    @Inject
    constructor(
        private val api: FastGptApi,
        private val cachedDatasetDao: CachedDatasetDao,
        private val cachedCollectionDao: CachedCollectionDao,
        private val importTaskDao: ImportTaskDao,
        @ApplicationContext private val context: Context,
    ) : KnowledgeRepository {
        override fun observeDatasets(): Flow<List<DatasetListItemUiModel>> =
            cachedDatasetDao.observeDatasets().map { items ->
                items.map { entity ->
                    DatasetListItemUiModel(
                        datasetId = entity.datasetId,
                        name = entity.name,
                        intro = entity.summaryJson.extractField("intro"),
                        type = entity.type,
                        vectorModel = entity.summaryJson.extractField("vectorModel"),
                        updateTimeLabel = entity.updateTime.toString(),
                    )
                }
            }

        override fun observeDataset(datasetId: String): Flow<DatasetListItemUiModel?> =
            observeDatasets().map { items -> items.firstOrNull { it.datasetId == datasetId } }

        override suspend fun refreshDatasets(query: String) {
            val datasets = api.listDatasets(DatasetListRequest(searchKey = query.takeIf { it.isNotBlank() })).data.orEmpty()
            cachedDatasetDao.clearAll()
            cachedDatasetDao.upsertAll(datasets.map { it.toEntity() })
        }

        override suspend fun createDataset(name: String) {
            val created = api.createDataset(DatasetCreateRequest(name = name.trim())).data ?: return
            cachedDatasetDao.upsert(created.toEntity())
        }

        override suspend fun deleteDataset(datasetId: String) {
            api.deleteDataset(DatasetDeleteRequest(id = datasetId))
            refreshDatasets()
        }

        override fun observeCollections(datasetId: String): Flow<List<CollectionListItemUiModel>> =
            cachedCollectionDao.observeCollectionsForDataset(
                datasetId,
            ).combine(importTaskDao.observeTasksForDataset(datasetId)) { items, tasks ->
                val importingNames = tasks.filter { it.status == "running" }.map { it.displayName }.toSet()
                items.map { entity ->
                    CollectionListItemUiModel(
                        collectionId = entity.collectionId,
                        datasetId = entity.datasetId,
                        name = entity.name,
                        type = entity.type,
                        trainingType = entity.trainingType,
                        status = if (importingNames.contains(entity.name)) "syncing" else entity.status,
                        sourceName = entity.sourceName,
                        updateTimeLabel = entity.updateTime.toString(),
                    )
                }
            }

        override fun observeImportTasks(datasetId: String): Flow<List<ImportTaskUiModel>> =
            importTaskDao.observeTasksForDataset(datasetId).map { items ->
                items.map {
                    ImportTaskUiModel(
                        taskId = it.taskId,
                        displayName = it.displayName,
                        sourceType = it.sourceType,
                        status = it.status,
                        progress = it.progress,
                        errorMessage = it.errorMessage,
                    )
                }
            }

        override suspend fun refreshCollections(datasetId: String) {
            val collections = api.listCollections(CollectionListRequest(datasetId = datasetId)).data.orEmpty()
            cachedCollectionDao.clearByDatasetId(datasetId)
            cachedCollectionDao.upsertAll(collections.map { it.toEntity() })
        }

        override suspend fun importText(
            datasetId: String,
            name: String,
            text: String,
            trainingType: String,
        ) {
            val taskId = "text-${System.currentTimeMillis()}"
            importTaskDao.upsert(task(taskId, datasetId, "text", name, "running", 15))
            try {
                api.createTextCollection(
                    TextCollectionCreateRequest(
                        datasetId = datasetId,
                        trainingType = trainingType,
                        text = text,
                        name = name,
                    ),
                )
                importTaskDao.upsert(task(taskId, datasetId, "text", name, "done", 100))
                refreshCollections(datasetId)
            } catch (error: Throwable) {
                importTaskDao.upsert(task(taskId, datasetId, "text", name, "error", 100, error.message))
                throw error
            }
        }

        override suspend fun importLinks(
            datasetId: String,
            urls: List<String>,
            selector: String?,
            trainingType: String,
        ) {
            val displayName = urls.firstOrNull().orEmpty()
            val taskId = "link-${System.currentTimeMillis()}"
            importTaskDao.upsert(task(taskId, datasetId, "link", displayName, "running", 10))
            try {
                api.createLinkCollection(
                    LinkCollectionCreateRequest(
                        datasetId = datasetId,
                        trainingType = trainingType,
                        links = urls.map { LinkCollectionCreateItemRequest(url = it, selector = selector?.takeIf(String::isNotBlank)) },
                    ),
                )
                importTaskDao.upsert(task(taskId, datasetId, "link", displayName, "done", 100))
                refreshCollections(datasetId)
            } catch (error: Throwable) {
                importTaskDao.upsert(task(taskId, datasetId, "link", displayName, "error", 100, error.message))
                throw error
            }
        }

        override suspend fun importDocument(
            datasetId: String,
            uri: Uri,
            displayName: String,
            trainingType: String,
        ) {
            val taskId = "file-${System.currentTimeMillis()}"
            importTaskDao.upsert(task(taskId, datasetId, "file", displayName, "running", 5))
            val tempFile = context.cacheDir.resolve("${System.currentTimeMillis()}-$displayName")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("无法读取所选文件")
                importTaskDao.upsert(task(taskId, datasetId, "file", displayName, "running", 50))
                val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaType())
                api.createLocalFileCollection(
                    file = MultipartBody.Part.createFormData("file", displayName, requestBody),
                    datasetId = datasetId.toRequestBody("text/plain".toMediaType()),
                    trainingType = trainingType.toRequestBody("text/plain".toMediaType()),
                )
                importTaskDao.upsert(task(taskId, datasetId, "file", displayName, "done", 100))
                refreshCollections(datasetId)
            } catch (error: Throwable) {
                importTaskDao.upsert(task(taskId, datasetId, "file", displayName, "error", 100, error.message))
                throw error
            } finally {
                tempFile.delete()
            }
        }

        override suspend fun listChunks(
            datasetId: String,
            collectionId: String,
            offset: Int,
            pageSize: Int,
        ): List<ChunkItemUiModel> =
            api.listChunkData(
                ChunkListRequest(
                    datasetId = datasetId,
                    collectionId = collectionId,
                    offset = offset,
                    pageSize = pageSize,
                ),
            ).data?.list.orEmpty()
                .map { it.toUiModel() }

        override suspend fun updateChunk(
            dataId: String,
            question: String,
            answer: String,
            forbid: Boolean,
        ) {
            api.updateChunkData(ChunkUpdateRequest(id = dataId, q = question, a = answer, forbid = forbid))
        }

        override suspend fun deleteChunk(dataId: String) {
            api.deleteChunkData(ChunkDeleteRequest(id = dataId))
        }

        override suspend fun search(
            datasetId: String,
            query: String,
            searchMode: String,
            similarity: Double?,
            embeddingWeight: Double?,
            usingReRank: Boolean,
        ): Triple<String, String, List<SearchResultUiModel>> {
            val response =
                api.searchTest(
                    SearchTestRequest(
                        datasetId = datasetId,
                        text = query,
                        searchMode = searchMode,
                        similarity = similarity,
                        embeddingWeight = embeddingWeight,
                        usingReRank = usingReRank,
                    ),
                ).data ?: SearchTestResponseDto()
            return Triple(
                response.duration,
                response.queryExtensionModel.orEmpty(),
                response.list.map {
                    SearchResultUiModel(
                        datasetId = it.datasetId,
                        collectionId = it.collectionId,
                        dataId = it.dataId,
                        title = it.sourceName ?: it.q.orEmpty().take(18).ifBlank { "命中片段" },
                        snippet = listOfNotNull(it.q, it.a).joinToString("\n").ifBlank { "无预览" },
                        scoreLabel =
                            listOfNotNull(
                                it.scoreType,
                                it.score?.let { score -> String.format("%.3f", score) },
                            ).joinToString(" · "),
                    )
                },
            )
        }

        private fun DatasetSummaryDto.toEntity(): CachedDatasetEntity =
            CachedDatasetEntity(
                datasetId = id,
                name = name,
                type = type,
                status = "active",
                updateTime = updateTime.toEpochMillis(),
                summaryJson = """{"intro":${intro.quote()},"vectorModel":${vectorModel.quote()}}""",
            )

        private fun CollectionSummaryDto.toEntity(): CachedCollectionEntity =
            CachedCollectionEntity(
                collectionId = id,
                datasetId = datasetId,
                name = name,
                type = type,
                status = status ?: trainingStatus ?: "active",
                trainingType = trainingType,
                sourceName = rawLink ?: name,
                updateTime = updateTime.toEpochMillis(),
                summaryJson = """{"sourceName":${(rawLink ?: name).quote()}}""",
            )

        private fun ChunkSummaryDto.toUiModel(): ChunkItemUiModel =
            ChunkItemUiModel(
                dataId = id,
                chunkIndex = chunkIndex,
                question = q,
                answer = a.orEmpty(),
                status =
                    when {
                        rebuilding == true -> "rebuilding"
                        forbid == true -> "disabled"
                        else -> "active"
                    },
                isDisabled = forbid == true,
                isRebuilding = rebuilding == true,
            )

        private fun task(
            taskId: String,
            datasetId: String,
            sourceType: String,
            displayName: String,
            status: String,
            progress: Int,
            errorMessage: String? = null,
        ) = ImportTaskEntity(
            taskId = taskId,
            datasetId = datasetId,
            sourceType = sourceType,
            displayName = displayName,
            status = status,
            progress = progress,
            errorMessage = errorMessage,
            updateTime = System.currentTimeMillis(),
        )
    }

@Module
@InstallIn(SingletonComponent::class)
object KnowledgeRepositoryModule {
    @Provides
    @Singleton
    fun provideKnowledgeRepository(
        api: FastGptApi,
        cachedDatasetDao: CachedDatasetDao,
        cachedCollectionDao: CachedCollectionDao,
        importTaskDao: ImportTaskDao,
        @ApplicationContext context: Context,
    ): KnowledgeRepository =
        RoomBackedKnowledgeRepository(
            api = api,
            cachedDatasetDao = cachedDatasetDao,
            cachedCollectionDao = cachedCollectionDao,
            importTaskDao = importTaskDao,
            context = context,
        )
}

private fun String.toEpochMillis(): Long = java.time.Instant.parse(this).toEpochMilli()

private fun String.quote(): String = "\"${replace("\"", "\\\"")}\""

private fun String.extractField(name: String): String = Regex("\"$name\":\"(.*?)\"").find(this)?.groupValues?.getOrNull(1).orEmpty()
