package com.lifuyue.kora.debug

import android.content.Context
import java.io.File

internal const val ACTION_SEED_LOCAL_KNOWLEDGE = "com.lifuyue.kora.debug.SEED_LOCAL_KNOWLEDGE"
internal const val EXTRA_PAYLOAD_PATH = "payload_path"
internal const val EXTRA_REPLACE_EXISTING = "replace_existing"
internal const val EXTRA_BATCH_SIZE = "batch_size"
internal const val EXTRA_SEED_TOKEN = "seed_token"
internal const val DEFAULT_BATCH_SIZE = 20
internal const val DEFAULT_PAYLOAD_RELATIVE_PATH = "debug/seed/local_knowledge_benchmark.json"
internal const val DEFAULT_STATUS_RELATIVE_PATH = "debug/seed/seed-status.txt"
internal const val DEFAULT_TOKEN_RELATIVE_PATH = "debug/seed/seed-token.txt"
internal const val SEED_TIMEOUT_MS = 120_000L

internal fun resolveSeedPath(
    context: Context,
    rawPath: String?,
): File {
    val candidate = rawPath?.trim().orEmpty()
    if (candidate.isBlank()) {
        return File(context.filesDir, DEFAULT_PAYLOAD_RELATIVE_PATH)
    }
    val file = File(candidate)
    if (file.isAbsolute) {
        return file
    }
    return if (candidate.startsWith("files/")) {
        File(requireNotNull(context.filesDir.parentFile), candidate)
    } else {
        File(context.filesDir, candidate)
    }
}

internal fun resolveStatusFile(context: Context): File = File(context.filesDir, DEFAULT_STATUS_RELATIVE_PATH)

private fun resolveTokenFile(context: Context): File = File(context.filesDir, DEFAULT_TOKEN_RELATIVE_PATH)

internal fun writeStatus(
    file: File,
    state: String,
    imported: Int,
    ready: Int,
    elapsedMs: Long,
    message: String,
) {
    file.parentFile?.mkdirs()
    file.writeText(
        buildString {
            appendLine("state=$state")
            appendLine("imported=$imported")
            appendLine("ready=$ready")
            appendLine("elapsedMs=$elapsedMs")
            appendLine("message=${message.replace('\n', ' ').replace('\r', ' ')}")
        },
        Charsets.UTF_8,
    )
}

internal fun consumeSeedToken(
    context: Context,
    providedToken: String?,
): Boolean {
    val expectedToken =
        runCatching { resolveTokenFile(context).readText(Charsets.UTF_8).trim() }
            .getOrNull()
            .orEmpty()
    val normalizedProvidedToken = providedToken?.trim().orEmpty()
    if (expectedToken.isBlank() || normalizedProvidedToken.isBlank() || expectedToken != normalizedProvidedToken) {
        return false
    }
    resolveTokenFile(context).delete()
    return true
}
