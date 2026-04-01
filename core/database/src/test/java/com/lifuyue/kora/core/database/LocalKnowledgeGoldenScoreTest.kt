package com.lifuyue.kora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalKnowledgeGoldenScoreTest {
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
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun goldenRetrievalScoreStaysAboveUsableThreshold() =
        runBlocking {
            goldenDocuments.forEachIndexed { index, document ->
                store.importText(
                    title = document.title,
                    text = document.body,
                    sourceLabel = "golden",
                    now = 1_000L + index,
                )
            }
            waitUntilIndexed(expectedReady = goldenDocuments.size)

            val report =
                evaluateGoldenCases(
                    cases = goldenCases,
                    search = { query -> store.search(query, limit = 4).map(LocalKnowledgeHit::title) },
                )

            assertTrue("golden retrieval score=${report.score}", report.score >= 8.0)
            assertTrue("golden top3 rate=${report.top3Rate}", report.top3Rate >= 0.85)
        }

    private suspend fun waitUntilIndexed(expectedReady: Int) {
        repeat(50) {
            val readyCount = store.observeDocuments().first().count { it.indexStatus == LocalKnowledgeIndexStatus.Ready }
            if (readyCount >= expectedReady) {
                return
            }
            delay(20)
        }
        throw AssertionError("Local knowledge indexing did not finish in time")
    }
}

private data class GoldenDocument(
    val title: String,
    val body: String,
)

private data class GoldenRetrievalCase(
    val query: String,
    val expectedTitle: String,
    val weight: Double,
    val minimumRank: Int,
)

private data class GoldenRetrievalReport(
    val score: Double,
    val top3Rate: Double,
)

private val goldenDocuments =
    listOf(
        GoldenDocument(
            title = "OpenRouter 配置说明",
            body = "接入 OpenRouter 时需要提供 base url、api key 和模型名。推荐先验证 qwen3.6 的 chat/completions 配置。",
        ),
        GoldenDocument(
            title = "Android 权限说明",
            body = "这里只介绍通知栏样式和视觉规范，不讨论权限接入细节。",
        ),
        GoldenDocument(
            title = "杂项记录",
            body = "Android 权限说明这几个字只在正文里被顺手提到一次，标题与主题都不相关。",
        ),
        GoldenDocument(
            title = "部署巡检：模型路由 offline cache",
            body =
                buildString {
                    append("这里记录部署巡检的常规操作。".repeat(20))
                    append("关键规则：编号 de-04-k21 使用 offline cache，服务于模型路由，并保留首条摘要。")
                },
        ),
        GoldenDocument(
            title = "部署治理：连接探活 delta stream",
            body =
                buildString {
                    append("背景说明".repeat(90))
                    append("边界规则：de-07-k24 在边界附近也需要命中。")
                    append("这里补充部署治理的后续监控说明。".repeat(12))
                },
        ),
    )

private val goldenCases =
    listOf(
        GoldenRetrievalCase(
            query = "openrouter 配置",
            expectedTitle = "OpenRouter 配置说明",
            weight = 1.2,
            minimumRank = 1,
        ),
        GoldenRetrievalCase(
            query = "android 权限",
            expectedTitle = "Android 权限说明",
            weight = 1.2,
            minimumRank = 1,
        ),
        GoldenRetrievalCase(
            query = "de-04-k21 模型路由",
            expectedTitle = "部署巡检：模型路由 offline cache",
            weight = 1.0,
            minimumRank = 1,
        ),
        GoldenRetrievalCase(
            query = "offline cache",
            expectedTitle = "部署巡检：模型路由 offline cache",
            weight = 1.0,
            minimumRank = 3,
        ),
        GoldenRetrievalCase(
            query = "de-07-k24 边界附近",
            expectedTitle = "部署治理：连接探活 delta stream",
            weight = 1.1,
            minimumRank = 1,
        ),
        GoldenRetrievalCase(
            query = "部署 连接探活 delta stream",
            expectedTitle = "部署治理：连接探活 delta stream",
            weight = 0.9,
            minimumRank = 3,
        ),
    )

private fun evaluateGoldenCases(
    cases: List<GoldenRetrievalCase>,
    search: (String) -> List<String>,
): GoldenRetrievalReport {
    val totalWeight = cases.sumOf(GoldenRetrievalCase::weight)
    val weightedPoints =
        cases.sumOf { testCase ->
            val hits = search(testCase.query)
            val rank = hits.indexOf(testCase.expectedTitle) + 1
            val points =
                when {
                    rank == 1 -> 10.0
                    rank in 2..testCase.minimumRank -> 8.5
                    rank in 1..3 -> 7.0
                    rank in 1..4 -> 5.0
                    else -> 0.0
                }
            points * testCase.weight
        }
    val top3Rate =
        cases.count { testCase ->
            search(testCase.query).take(3).contains(testCase.expectedTitle)
        }.toDouble() / cases.size
    return GoldenRetrievalReport(
        score = weightedPoints / totalWeight,
        top3Rate = top3Rate,
    )
}
