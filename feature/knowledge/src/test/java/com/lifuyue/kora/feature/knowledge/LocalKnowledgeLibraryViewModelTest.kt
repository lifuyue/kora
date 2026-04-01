package com.lifuyue.kora.feature.knowledge

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.LocalKnowledgeStore
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocalKnowledgeLibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: KoraDatabase
    private lateinit var store: LocalKnowledgeStore

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                KoraDatabase::class.java,
            ).allowMainThreadQueries().build()
        store =
            LocalKnowledgeStore(
                documentDao = database.localKnowledgeDocumentDao(),
                chunkDao = database.localKnowledgeChunkDao(),
                postingDao = database.localKnowledgePostingDao(),
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun queryUsesRetrievalPathInsteadOfPreviewContainsFilter() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            store.importText(
                title = "部署巡检手册",
                text = "这里是部署巡检的通用前言。".repeat(20) + "关键规则：de-04-k21 使用 offline cache 做模型路由。",
                sourceLabel = "manual",
                now = 10L,
            )
            store.importText(
                title = "杂项记录",
                text = "这里只有一些无关的背景信息。".repeat(30),
                sourceLabel = "manual",
                now = 11L,
            )
            waitUntilIndexed()

            val viewModel =
                LocalKnowledgeLibraryViewModel(
                    localKnowledgeStore = store,
                    strings = TestKnowledgeStrings(),
                    ioDispatcher = mainDispatcherRule.dispatcher,
                )
            val collector = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            assertEquals(
                listOf("部署巡检手册"),
                store.search("de-04-k21 模型路由", limit = 20).map { it.title }.distinct(),
            )

            val result =
                async {
                    viewModel.uiState
                        .drop(1)
                        .first { it.items.isNotEmpty() }
                }
            viewModel.updateQuery("de-04-k21 模型路由")
            advanceUntilIdle()

            assertEquals(listOf("部署巡检手册"), result.await().items.map { it.title })
            collector.cancel()
        }

    private suspend fun waitUntilIndexed(expectedReady: Int = 2) {
        repeat(50) {
            val readyCount = store.observeDocuments().first().count { it.indexStatus.name == "Ready" }
            if (readyCount >= expectedReady) {
                return
            }
            delay(20)
        }
        throw AssertionError("Local knowledge indexing did not finish in time")
    }
}

private class TestKnowledgeStrings : KnowledgeStrings(context = ContextWrapper(null)) {
    override fun localImportValidationFailed(): String = "标题和内容不能为空"

    override fun localManualImportSource(): String = "手动导入"

    override fun localImportFailed(): String = "导入失败"
}
