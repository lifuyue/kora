package com.lifuyue.kora.feature.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private val documentMimeTypes =
    listOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/markdown",
        "text/html",
        "text/csv",
        "application/csv",
    )

fun JsonObject?.toChatAttachmentConfig(): ChatAttachmentConfig {
    if (this == null) {
        return ChatAttachmentConfig()
    }
    return ChatAttachmentConfig(
        maxFiles = this["maxFiles"]?.jsonPrimitive?.intOrNull ?: 10,
        canSelectFile = this["canSelectFile"]?.jsonPrimitive?.booleanOrNull ?: false,
        canSelectImg = this["canSelectImg"]?.jsonPrimitive?.booleanOrNull ?: false,
        canSelectVideo = this["canSelectVideo"]?.jsonPrimitive?.booleanOrNull ?: false,
        canSelectAudio = this["canSelectAudio"]?.jsonPrimitive?.booleanOrNull ?: false,
        canSelectCustomFileExtension = this["canSelectCustomFileExtension"]?.jsonPrimitive?.booleanOrNull ?: false,
        customFileExtensionList =
            this["customFileExtensionList"]?.let { element ->
                element as? JsonArray
            }?.mapNotNull { item ->
                item.jsonPrimitive.content.takeIf(String::isNotBlank)?.let { normalizeFileExtension(it) }
            }.orEmpty(),
    )
}

fun ChatAttachmentConfig.allowedMimeTypes(kind: AttachmentKind): Array<String> =
    when (kind) {
        AttachmentKind.Image -> if (canSelectImg) arrayOf("image/*") else emptyArray()
        AttachmentKind.File ->
            buildList {
                if (canSelectFile) {
                    addAll(documentMimeTypes)
                }
                if (canSelectVideo) {
                    add("video/*")
                }
                if (canSelectAudio) {
                    add("audio/*")
                }
                if (canSelectCustomFileExtension) {
                    add("*/*")
                }
            }.distinct().toTypedArray()
    }

fun ChatAttachmentConfig.canAcceptSelection(
    mimeType: String?,
    displayName: String?,
    kindHint: AttachmentKind? = null,
): Boolean {
    if (kindHint == AttachmentKind.Image) {
        return canSelectImg && mimeType?.startsWith("image/") == true
    }
    if (kindHint == AttachmentKind.File && mimeType?.startsWith("image/") == true) {
        return false
    }
    return when {
        mimeType?.startsWith("image/") == true -> canSelectImg
        mimeType?.startsWith("video/") == true -> canSelectVideo
        mimeType?.startsWith("audio/") == true -> canSelectAudio
        mimeType != null && mimeType in documentMimeTypes -> canSelectFile
        canSelectCustomFileExtension && displayName != null ->
            normalizeFileExtension(displayName.substringAfterLast('.', missingDelimiterValue = ""))
                ?.let { extension -> customFileExtensionList.contains(extension) }
                ?: false
        else -> false
    }
}

fun ChatAttachmentConfig.canAddMore(currentCount: Int): Boolean = currentCount < maxFiles

fun canLaunchAttachmentPicker(
    config: ChatAttachmentConfig,
    currentCount: Int,
    kind: AttachmentKind,
): Boolean = config.canAddMore(currentCount) && config.allowedMimeTypes(kind).isNotEmpty()

fun resolveAttachmentKind(
    mimeType: String,
    displayName: String,
): AttachmentKind =
    when {
        mimeType.startsWith("image/") -> AttachmentKind.Image
        displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase() in imageExtensions ->
            AttachmentKind.Image
        else -> AttachmentKind.File
    }

fun resolveAttachmentMetadata(
    context: Context,
    uri: Uri,
): AttachmentSourceMetadata {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri).orEmpty()
    var displayName = uri.lastPathSegment?.takeIf(String::isNotBlank).orEmpty()
    var sizeBytes: Long? = null

    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex).orEmpty().ifBlank { displayName }
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

    if (displayName.isBlank()) {
        displayName = "attachment"
    }

    return AttachmentSourceMetadata(
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
    )
}

fun buildChatCompletionContent(
    text: String,
    attachments: List<AttachmentDraftUiModel>,
): JsonElement {
    val parts =
        buildList<JsonElement> {
            if (text.isNotBlank()) {
                add(
                    buildJsonObject {
                        put("type", JsonPrimitive("text"))
                        put("text", JsonPrimitive(text))
                    },
                )
            }
            attachments.forEach { attachment ->
                val uploaded = attachment.uploadedRef ?: return@forEach
                add(
                    when (attachment.kind) {
                        AttachmentKind.Image ->
                            buildJsonObject {
                                put("type", JsonPrimitive("file_url"))
                                put("name", JsonPrimitive(uploaded.name))
                                put("url", JsonPrimitive(uploaded.url))
                                uploaded.key?.takeIf(String::isNotBlank)?.let { put("key", JsonPrimitive(it)) }
                            }
                        AttachmentKind.File ->
                            buildJsonObject {
                                put("type", JsonPrimitive("file_url"))
                                put("name", JsonPrimitive(uploaded.name))
                                put("url", JsonPrimitive(uploaded.url))
                                uploaded.key?.takeIf(String::isNotBlank)?.let { put("key", JsonPrimitive(it)) }
                            }
                    },
                )
            }
        }

    return when {
        parts.isEmpty() -> JsonPrimitive(text)
        parts.size == 1 && attachments.isEmpty() && text.isNotBlank() ->
            JsonPrimitive(text)
        else -> JsonArray(parts)
    }
}

data class AttachmentSourceMetadata(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
)

private fun normalizeFileExtension(value: String): String? {
    val trimmed = value.trim().trimStart('.').lowercase()
    return trimmed.takeIf(String::isNotBlank)?.let { ".$it" }
}

private val imageExtensions =
    setOf(
        "jpg",
        "jpeg",
        "png",
        "gif",
        "bmp",
        "webp",
        "svg",
    )
