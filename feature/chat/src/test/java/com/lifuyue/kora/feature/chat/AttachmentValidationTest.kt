package com.lifuyue.kora.feature.chat

import com.lifuyue.kora.core.network.UploadedAssetRef
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentValidationTest {
    @Test
    fun buildChatCompletionContentSerializesImagesAsFileUrlParts() {
        val content =
            buildChatCompletionContent(
                text = "hello",
                attachments =
                    listOf(
                        AttachmentDraftUiModel(
                            displayName = "photo.png",
                            localUri = "content://photo",
                            mimeType = "image/png",
                            kind = AttachmentKind.Image,
                            uploadStatus = AttachmentUploadStatus.Uploaded,
                            uploadedRef =
                                UploadedAssetRef(
                                    name = "photo.png",
                                    url = "https://example.com/photo.png",
                                    key = "chat/photo.png",
                                    mimeType = "image/png",
                                    size = 42L,
                                ),
                        ),
                    ),
            )

        assertTrue(content is JsonArray)
        assertEquals("file_url", (content as JsonArray).last().jsonObject["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun canLaunchAttachmentPickerBlocksWhenMaxFilesReachedOrTypeDisallowed() {
        val config =
            ChatAttachmentConfig(
                maxFiles = 1,
                canSelectImg = true,
                canSelectFile = false,
            )

        assertFalse(canLaunchAttachmentPicker(config, currentCount = 1, kind = AttachmentKind.Image))
        assertFalse(canLaunchAttachmentPicker(config, currentCount = 0, kind = AttachmentKind.File))
        assertTrue(canLaunchAttachmentPicker(config, currentCount = 0, kind = AttachmentKind.Image))
    }
}
