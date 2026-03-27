package com.lifuyue.kora.feature.chat.testing

import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.database.KoraDatabase
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.testing.RoomTestFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatTestingHarnessSmokeTest {
    @Test
    fun chatModuleCanConsumeSharedRoomFactory() {
        val database =
            RoomTestFactory.inMemoryDatabase<KoraDatabase>(
                ApplicationProvider.getApplicationContext(),
            )

        database.conversationDao().upsert(
            ConversationEntity(
                chatId = "chat-1",
                appId = "app-1",
                title = "Smoke",
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

        assertEquals("Smoke", database.conversationDao().getConversationByChatId("chat-1")?.title)
        database.close()
    }
}
