package com.lifuyue.kora.feature.chat

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExportConversationMessage(
    val messageId: String,
    val role: com.lifuyue.kora.core.common.ChatRole,
    val markdown: String,
    val createdAt: Long,
)

data class ConversationExportArtifact(
    val filePath: String,
    val mimeType: String,
    val bytes: Long,
)

interface ConversationExportManager {
    suspend fun export(
        conversationTitle: String,
        format: ConversationExportFormat,
        messages: List<ExportConversationMessage>,
    ): ConversationExportArtifact
}

@Singleton
class AndroidConversationExportManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConversationExportManager {
        override suspend fun export(
            conversationTitle: String,
            format: ConversationExportFormat,
            messages: List<ExportConversationMessage>,
        ): ConversationExportArtifact {
            val exportDir = File(context.cacheDir, "share_exports").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val safeTitle = conversationTitle.ifBlank { "conversation" }.replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            return when (format) {
                ConversationExportFormat.Txt -> {
                    val file = File(exportDir, "${safeTitle}_$timestamp.txt")
                    file.writeText(
                        messages.joinToString("\n\n") { "${it.role.name}: ${it.markdown}" },
                    )
                    ConversationExportArtifact(file.absolutePath, "text/plain", file.length())
                }
                ConversationExportFormat.Json -> {
                    val file = File(exportDir, "${safeTitle}_$timestamp.json")
                    file.writeText(Json.encodeToString(messages.map { it.toSerializable() }))
                    ConversationExportArtifact(file.absolutePath, "application/json", file.length())
                }
                ConversationExportFormat.Pdf -> {
                    val file = File(exportDir, "${safeTitle}_$timestamp.pdf")
                    writePdf(file, conversationTitle, messages)
                    ConversationExportArtifact(file.absolutePath, "application/pdf", file.length())
                }
            }
        }

        private fun writePdf(
            file: File,
            conversationTitle: String,
            messages: List<ExportConversationMessage>,
        ) {
            val content =
                buildString {
                    appendLine(conversationTitle.ifBlank { "Conversation Export" })
                    appendLine()
                    messages.forEach { message ->
                        appendLine("${message.role.name}: ${message.markdown}")
                    }
                }.escapePdfText()
            val stream = "BT /F1 12 Tf 40 780 Td ($content) Tj ET"
            val pdf =
                """
                %PDF-1.4
                1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
                2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
                3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj
                4 0 obj << /Length ${stream.toByteArray().size} >> stream
                $stream
                endstream
                endobj
                5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
                trailer << /Root 1 0 R >>
                %%EOF
                """.trimIndent()
            file.writeText(pdf)
        }
    }

@kotlinx.serialization.Serializable
private data class ExportConversationMessageDto(
    val messageId: String,
    val role: String,
    val markdown: String,
    val createdAt: Long,
)

private fun ExportConversationMessage.toSerializable() =
    ExportConversationMessageDto(
        messageId = messageId,
        role = role.name,
        markdown = markdown,
        createdAt = createdAt,
    )

private fun String.escapePdfText(): String =
    replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("\n", "\\n")

@Module
@InstallIn(SingletonComponent::class)
abstract class ConversationExportModule {
    @Binds
    abstract fun bindConversationExportManager(manager: AndroidConversationExportManager): ConversationExportManager
}
