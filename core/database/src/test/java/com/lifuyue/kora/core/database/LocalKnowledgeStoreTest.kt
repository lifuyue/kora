package com.lifuyue.kora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

            val documents = store.observeDocuments().first()
            val chunks = store.observeChunks(documents.single().documentId).first()

            assertEquals(1, documents.size)
            assertEquals("Kora Notes", documents.single().title)
            assertTrue(chunks.isNotEmpty())
        }

    @Test
    fun searchReturnsEnabledMatchingSnippet() =
        runBlocking {
            store.importText(
                title = "Android Notes",
                text = "Jetpack Compose works well for Android chat surfaces.",
                sourceLabel = "manual",
                now = 200L,
            )

            val hit = store.search("android chat").first()

            assertEquals("Android Notes", hit.title)
            assertTrue(hit.snippet.contains("Android", ignoreCase = true))
        }
}
