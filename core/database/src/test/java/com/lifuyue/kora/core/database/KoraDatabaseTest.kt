package com.lifuyue.kora.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.common.ChatSource
import com.lifuyue.kora.core.database.entity.ConversationEntity
import com.lifuyue.kora.core.database.entity.ConversationFolderCrossRef
import com.lifuyue.kora.core.database.entity.ConversationFolderEntity
import com.lifuyue.kora.core.database.entity.ConversationTagCrossRef
import com.lifuyue.kora.core.database.entity.ConversationTagEntity
import com.lifuyue.kora.core.database.entity.InteractiveDraftEntity
import com.lifuyue.kora.core.database.entity.MessageEntity
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
class KoraDatabaseTest {
    private lateinit var database: KoraDatabase

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                KoraDatabase::class.java,
            ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun conversationDaoUpsertsAndOrdersByUpdateTime() =
        runBlocking {
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-1",
                    appId = "app-a",
                    title = "Older",
                    customTitle = null,
                    isPinned = false,
                    source = ChatSource.online.name,
                    updateTime = 100L,
                    lastMessagePreview = "old",
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-2",
                    appId = "app-a",
                    title = "Newer",
                    customTitle = "Pinned",
                    isPinned = true,
                    source = ChatSource.api.name,
                    updateTime = 200L,
                    lastMessagePreview = "new",
                    hasDraft = true,
                    isDeleted = false,
                    isArchived = true,
                ),
            )

            val results = database.conversationDao().getConversationsForApp(appId = "app-a", limit = 10, offset = 0)

            assertEquals(listOf("chat-2", "chat-1"), results.map { it.chatId })
            assertTrue(results.first().isPinned)
            assertTrue(results.first().isArchived)
        }

    @Test
    fun conversationDaoSoftDeletesAndClearsByApp() =
        runBlocking {
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-1",
                    appId = "app-a",
                    title = "Keep",
                    customTitle = null,
                    isPinned = false,
                    source = ChatSource.online.name,
                    updateTime = 100L,
                    lastMessagePreview = null,
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-2",
                    appId = "app-a",
                    title = "Delete",
                    customTitle = null,
                    isPinned = false,
                    source = ChatSource.online.name,
                    updateTime = 200L,
                    lastMessagePreview = null,
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )

            database.conversationDao().softDelete(chatId = "chat-2")
            assertTrue(database.conversationDao().getConversationByChatId("chat-2")!!.isDeleted)
            assertEquals(listOf("chat-1"), database.conversationDao().observeConversationsForApp("app-a").first().map { it.chatId })

            database.conversationDao().clearByAppId("app-a")

            assertTrue(database.conversationDao().getConversationByChatId("chat-1")!!.isDeleted)
            assertTrue(database.conversationDao().getConversationByChatId("chat-2")!!.isDeleted)
        }

    @Test
    fun conversationDaoMarksMissingChatsDeletedAndKeepsPinnedOrdering() =
        runBlocking {
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-1",
                    appId = "app-a",
                    title = "Pinned newer",
                    customTitle = null,
                    isPinned = true,
                    source = ChatSource.online.name,
                    updateTime = 200L,
                    lastMessagePreview = "preview-1",
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-2",
                    appId = "app-a",
                    title = "Pinned older",
                    customTitle = null,
                    isPinned = true,
                    source = ChatSource.online.name,
                    updateTime = 200L,
                    lastMessagePreview = "preview-2",
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-3",
                    appId = "app-a",
                    title = "Will disappear",
                    customTitle = null,
                    isPinned = false,
                    source = ChatSource.online.name,
                    updateTime = 50L,
                    lastMessagePreview = null,
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )

            database.conversationDao().markMissingAsDeleted("app-a", listOf("chat-1", "chat-2"))

            val visible = database.conversationDao().getConversationsForApp(appId = "app-a", limit = 10, offset = 0)
            assertEquals(listOf("chat-2", "chat-1"), visible.map { it.chatId })
            assertTrue(database.conversationDao().getConversationByChatId("chat-3")!!.isDeleted)
        }

    @Test
    fun conversationDaoUpdatesArchivedFlag() =
        runBlocking {
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-1",
                    appId = "app-a",
                    title = "Archive me",
                    customTitle = null,
                    isPinned = false,
                    source = ChatSource.online.name,
                    updateTime = 100L,
                    lastMessagePreview = null,
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )

            database.conversationDao().updateArchived(chatId = "chat-1", isArchived = true)

            assertTrue(database.conversationDao().getConversationByChatId("chat-1")!!.isArchived)
        }

    @Test
    fun messageDaoPersistsStreamingAndOrdersByCreatedAtThenDataId() =
        runBlocking {
            database.messageDao().upsertAll(
                listOf(
                    MessageEntity(
                        dataId = "b",
                        chatId = "chat-1",
                        appId = "app-a",
                        role = ChatRole.AI.name,
                        payloadJson = """{"text":"second"}""",
                        createdAt = 10L,
                        isStreaming = true,
                        sendStatus = "streaming",
                        errorCode = 500,
                    ),
                    MessageEntity(
                        dataId = "a",
                        chatId = "chat-1",
                        appId = "app-a",
                        role = ChatRole.Human.name,
                        payloadJson = """{"text":"first"}""",
                        createdAt = 10L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                ),
            )

            val messages = database.messageDao().getMessagesForChat("chat-1")

            assertEquals(listOf("a", "b"), messages.map { it.dataId })
            assertTrue(messages.last().isStreaming)
            assertEquals(500, messages.last().errorCode)
        }

    @Test
    fun messageDaoDeletesSingleMessageAndChatMessages() =
        runBlocking {
            database.messageDao().upsertAll(
                listOf(
                    MessageEntity(
                        dataId = "msg-1",
                        chatId = "chat-1",
                        appId = "app-a",
                        role = ChatRole.Human.name,
                        payloadJson = "{}",
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                    MessageEntity(
                        dataId = "msg-2",
                        chatId = "chat-1",
                        appId = "app-a",
                        role = ChatRole.AI.name,
                        payloadJson = "{}",
                        createdAt = 2L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                ),
            )

            database.messageDao().deleteMessage("msg-1")
            assertEquals(listOf("msg-2"), database.messageDao().getMessagesForChat("chat-1").map { it.dataId })

            database.messageDao().updateFeedback("msg-2", 1)
            assertEquals(1, database.messageDao().observeMessagesForChat("chat-1").first().single().feedbackType)

            database.messageDao().deleteMessagesForChat("chat-1")
            assertTrue(database.messageDao().getMessagesForChat("chat-1").isEmpty())
        }

    @Test
    fun messageDaoDeletesMessagesForEntireApp() =
        runBlocking {
            database.messageDao().upsertAll(
                listOf(
                    MessageEntity(
                        dataId = "msg-a1",
                        chatId = "chat-a",
                        appId = "app-a",
                        role = ChatRole.Human.name,
                        payloadJson = "{}",
                        createdAt = 1L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                    MessageEntity(
                        dataId = "msg-b1",
                        chatId = "chat-b",
                        appId = "app-b",
                        role = ChatRole.AI.name,
                        payloadJson = "{}",
                        createdAt = 2L,
                        isStreaming = false,
                        sendStatus = "sent",
                        errorCode = null,
                    ),
                ),
            )

            database.messageDao().deleteMessagesForApp("app-a")

            assertTrue(database.messageDao().getMessagesForChat("chat-a").isEmpty())
            assertEquals(listOf("msg-b1"), database.messageDao().getMessagesForChat("chat-b").map { it.dataId })
        }

    @Test
    fun folderAndTagDaosStoreAssignmentsPerApp() =
        runBlocking {
            database.conversationDao().upsert(
                ConversationEntity(
                    chatId = "chat-1",
                    appId = "app-a",
                    title = "Organized",
                    customTitle = null,
                    isPinned = false,
                    source = ChatSource.online.name,
                    updateTime = 100L,
                    lastMessagePreview = null,
                    hasDraft = false,
                    isDeleted = false,
                    isArchived = false,
                ),
            )
            database.conversationFolderDao().upsert(
                ConversationFolderEntity(
                    folderId = "folder-1",
                    appId = "app-a",
                    name = "工作",
                    sortOrder = 0L,
                ),
            )
            database.conversationTagDao().upsert(
                ConversationTagEntity(
                    tagId = "tag-1",
                    appId = "app-a",
                    name = "Kotlin",
                    colorToken = "sky",
                    sortOrder = 0L,
                ),
            )
            database.conversationFolderDao().upsertAssignment(
                ConversationFolderCrossRef(chatId = "chat-1", folderId = "folder-1"),
            )
            database.conversationTagDao().upsertAssignments(
                listOf(ConversationTagCrossRef(chatId = "chat-1", tagId = "tag-1")),
            )

            assertEquals(listOf("folder-1"), database.conversationFolderDao().observeAssignments("app-a").first().map { it.folderId })
            assertEquals(listOf("tag-1"), database.conversationTagDao().observeAssignments("app-a").first().map { it.tagId })
        }

    @Test
    fun interactiveDraftDaoPersistsAndDeletesLatestSnapshot() =
        runBlocking {
            database.interactiveDraftDao().upsert(
                InteractiveDraftEntity(
                    chatId = "chat-1",
                    messageDataId = "assistant-1",
                    responseValueId = "value-1",
                    rawPayloadJson = """{"kind":"userInput"}""",
                    draftPayloadJson = """{"field":"draft"}""",
                    updatedAt = 123L,
                ),
            )

            val stored = database.interactiveDraftDao().getByChatId("chat-1")

            assertEquals("assistant-1", stored?.messageDataId)
            assertEquals("value-1", stored?.responseValueId)
            assertEquals("""{"field":"draft"}""", stored?.draftPayloadJson)

            database.interactiveDraftDao().deleteByChatId("chat-1")

            assertEquals(null, database.interactiveDraftDao().getByChatId("chat-1"))
        }
}
