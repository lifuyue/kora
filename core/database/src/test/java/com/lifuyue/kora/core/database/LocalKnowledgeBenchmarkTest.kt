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
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class LocalKnowledgeBenchmarkTest {
    private lateinit var database: KoraDatabase
    private lateinit var store: LocalKnowledgeStore
    private lateinit var benchmarkData: LocalKnowledgeBenchmarkData

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
        benchmarkData = loadLocalKnowledgeBenchmarkData()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun benchmarkFixtureKeepsRetrievalQualityUsable() =
        runBlocking {
            seedStore()

            val report = evaluateBenchmarkCases()

            assertTrue("benchmark retrieval score=${report.score}", report.score >= 7.5)
            assertTrue("benchmark top3 rate=${report.top3Rate}", report.top3Rate >= 0.8)
        }

    @Test
    fun benchmarkFixtureStaysWithinReasonableIndexAndSearchBudget() =
        runBlocking {
            val indexingMs =
                measureTimeMillis {
                    benchmarkData.documents.forEachIndexed { index, document ->
                        store.importText(
                            title = document.title,
                            text = document.body,
                            sourceLabel = document.sourceLabel,
                            now = 20_000L + index,
                        )
                    }
                    waitUntilIndexed(expectedReady = benchmarkData.documents.size)
                }
            val searchMs =
                measureTimeMillis {
                    benchmarkData.queries.forEach { query ->
                        store.search(query.query, limit = 4)
                    }
                }
            val totalChunks = store.observeDocuments().first().sumOf { it.chunkCount }

            println(
                "LocalKnowledgeBenchmarkTest indexingMs=$indexingMs searchMs=$searchMs " +
                    "documents=${benchmarkData.documents.size} queries=${benchmarkData.queries.size} totalChunks=$totalChunks",
            )

            assertTrue("benchmark indexingMs=$indexingMs", indexingMs < 12_000)
            assertTrue("benchmark searchMs=$searchMs", searchMs < 2_500)
            assertTrue("benchmark totalChunks=$totalChunks", totalChunks <= 260)
        }

    private suspend fun seedStore() {
        store.seedBenchmarkData(benchmarkData)
        waitUntilIndexed(expectedReady = benchmarkData.documents.size)
    }

    private suspend fun waitUntilIndexed(expectedReady: Int) {
        repeat(500) {
            val readyCount = store.observeDocuments().first().count { it.indexStatus == LocalKnowledgeIndexStatus.Ready }
            if (readyCount >= expectedReady) {
                return
            }
            delay(20)
        }
        throw AssertionError("Local knowledge benchmark indexing did not finish in time")
    }

    private fun evaluateBenchmarkCases(): BenchmarkReport {
        val titleByDocumentId = benchmarkData.documents.associate { it.id to it.title }
        val totalWeight = benchmarkData.queries.sumOf(LocalKnowledgeBenchmarkQuery::weight)
        val weightedPoints =
            benchmarkData.queries.sumOf { query ->
                val expectedTitle = checkNotNull(titleByDocumentId[query.expectedDocumentId])
                val hits = store.search(query.query, limit = 4).map(LocalKnowledgeHit::title)
                val rank = hits.indexOf(expectedTitle) + 1
                val points =
                    when {
                        rank == 1 -> 10.0
                        rank in 2..query.minimumRank -> 8.5
                        rank in 1..3 -> 7.0
                        rank in 1..4 -> 5.0
                        else -> 0.0
                    }
                points * query.weight
            }
        val top3Rate =
            benchmarkData.queries.count { query ->
                val expectedTitle = checkNotNull(titleByDocumentId[query.expectedDocumentId])
                store.search(query.query, limit = 4).take(3).map(LocalKnowledgeHit::title).contains(expectedTitle)
            }.toDouble() / benchmarkData.queries.size
        return BenchmarkReport(
            score = weightedPoints / totalWeight,
            top3Rate = top3Rate,
        )
    }
}

private data class BenchmarkReport(
    val score: Double,
    val top3Rate: Double,
)
