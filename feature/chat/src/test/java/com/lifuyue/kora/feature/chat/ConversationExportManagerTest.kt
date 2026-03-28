package com.lifuyue.kora.feature.chat

import androidx.test.core.app.ApplicationProvider
import com.lifuyue.kora.core.common.ChatRole
import com.lifuyue.kora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConversationExportManagerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exportCreatesTxtJsonAndPdfFiles() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val manager = AndroidConversationExportManager(context)
            val messages =
                listOf(
                    ExportConversationMessage(
                        messageId = "user-1",
                        role = ChatRole.Human,
                        markdown = "Hello",
                        createdAt = 1L,
                    ),
                    ExportConversationMessage(
                        messageId = "assistant-1",
                        role = ChatRole.AI,
                        markdown = "Hi there",
                        createdAt = 2L,
                    ),
                )

            val txt = manager.export("Trip Plan", ConversationExportFormat.Txt, messages)
            val json = manager.export("Trip Plan", ConversationExportFormat.Json, messages)
            val pdf = manager.export("Trip Plan", ConversationExportFormat.Pdf, messages)

            assertTrue(File(txt.filePath).exists())
            assertTrue(File(json.filePath).exists())
            assertTrue(File(pdf.filePath).exists())
            assertTrue(txt.filePath.endsWith(".txt"))
            assertTrue(json.filePath.endsWith(".json"))
            assertTrue(pdf.filePath.endsWith(".pdf"))
            assertTrue(txt.bytes > 0)
            assertTrue(json.bytes > 0)
            assertTrue(pdf.bytes > 0)
        }
}
