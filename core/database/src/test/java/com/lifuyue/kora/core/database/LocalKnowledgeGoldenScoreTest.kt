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
    fun goldenRetrievalScoreStaysAboveUsableThreshold() =
        runBlocking {
            store.seedBenchmarkData(benchmarkData, nowBase = 1_000L)
            waitUntilIndexed(expectedReady = benchmarkData.documents.size)

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
            val score = weightedPoints / totalWeight
            val top3Rate =
                benchmarkData.queries.count { query ->
                    val expectedTitle = checkNotNull(titleByDocumentId[query.expectedDocumentId])
                    store.search(query.query, limit = 4).take(3).map(LocalKnowledgeHit::title).contains(expectedTitle)
                }.toDouble() / benchmarkData.queries.size

            assertTrue("golden retrieval score=$score", score >= 7.5)
            assertTrue("golden top3 rate=$top3Rate", top3Rate >= 0.8)
        }

    private suspend fun waitUntilIndexed(expectedReady: Int) {
        repeat(500) {
            val readyCount = store.observeDocuments().first().count { it.indexStatus == LocalKnowledgeIndexStatus.Ready }
            if (readyCount >= expectedReady) {
                return
            }
            delay(20)
        }
        throw AssertionError("Local knowledge indexing did not finish in time")
    }
}
