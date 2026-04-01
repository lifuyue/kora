package com.lifuyue.kora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalKnowledgeStoreTest {
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
    fun importTextCreatesSearchableChunks() =
        runBlocking {
            store.importText(
                title = "Kora Notes",
                text = "Kora is a native Android AI workspace.\n\nIt supports OpenAI compatible chat and local retrieval.",
                sourceLabel = "manual",
                now = 100L,
            )
            waitUntilIndexed()

            val documents = store.observeDocuments().first()
            val chunks = store.observeChunks(documents.single().documentId).first()

            assertEquals(1, documents.size)
            assertEquals("Kora Notes", documents.single().title)
            assertTrue(chunks.isNotEmpty())
        }

    @Test
    fun searchReturnsEnabledMatchingSnippetForChineseMixedQuery() =
        runBlocking {
            store.importText(
                title = "OpenRouter 配置说明",
                text = "Jetpack Compose works well for Android chat surfaces. OpenRouter 的模型配置也可以写在这里。",
                sourceLabel = "manual",
                now = 200L,
            )
            waitUntilIndexed()

            val hit = store.search("openrouter 配置").first()

            assertEquals("OpenRouter 配置说明", hit.title)
            assertTrue(hit.snippet.contains("OpenRouter", ignoreCase = true))
        }

    @Test
    fun titleMatchesRankAboveBodyOnlyMatches() =
        runBlocking {
            store.importText(
                title = "Android 权限说明",
                text = "这里只提到通知栏样式。",
                sourceLabel = "manual",
                now = 300L,
            )
            store.importText(
                title = "杂项记录",
                text = "Android 权限说明放在这段正文里，但标题不相关。",
                sourceLabel = "manual",
                now = 301L,
            )
            waitUntilIndexed(expectedReady = 2)

            val hits = store.search("android 权限").take(2)
            assertEquals("Android 权限说明", hits.first().title)
        }

    private suspend fun waitUntilIndexed(expectedReady: Int = 1) {
        repeat(50) {
            val readyCount = store.observeDocuments().first().count { it.indexStatus == com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus.Ready }
            if (readyCount >= expectedReady) {
                return
            }
            delay(20)
        }
        throw AssertionError("Local knowledge indexing did not finish in time")
    }
}
