package com.lifuyue.kora.core.testing

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class TestFactoriesTest {
    @Test
    fun createsPreferenceDataStore() =
        runTest {
            val store = DataStoreTestFactory.createPreferencesDataStore("core-testing-preferences")
            val key = stringPreferencesKey("value")

            store.edit { preferences ->
                preferences[key] = "stored"
            }

            assertEquals("stored", store.data.first()[key])
        }

    @Test
    fun createsInMemoryRoomDatabase() {
        val database =
            RoomTestFactory.inMemoryDatabase<KoraDatabase>(
                ApplicationProvider.getApplicationContext(),
            )

        database.conversationDao().upsert(
            ConversationEntity(
                chatId = "room-smoke",
                appId = "app",
                title = "Room Smoke",
                customTitle = null,
                isPinned = false,
                source = "test",
                updateTime = 1L,
                lastMessagePreview = null,
                hasDraft = false,
                isDeleted = false,
                isArchived = false,
            ),
        )

        assertTrue(database.isOpen)
        assertEquals("Room Smoke", database.conversationDao().getConversationByChatId("room-smoke")?.title)
        database.close()
    }

    @Test
    fun advancesFixedClockDeterministically() {
        val clock = FixedClock(Instant.parse("2026-03-27T00:00:00Z"), ZoneId.of("UTC"))

        clock.advanceBy(Duration.ofMinutes(5))

        assertEquals(Instant.parse("2026-03-27T00:05:00Z"), clock.instant())
    }

    @Test
    fun generatesSequentialIds() {
        val generator = SequentialIdGenerator(prefix = "msg", seed = 41)

        assertEquals("msg-42", generator.nextId())
        assertEquals("msg-43", generator.nextId())
    }
}
