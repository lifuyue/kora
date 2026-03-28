package com.lifuyue.kora.navigation

import android.content.Intent
import android.net.Uri
import com.lifuyue.kora.core.database.store.ShareLinkPayload

fun parseShareLinkIntent(intent: Intent?): ShareLinkPayload? = parseShareLinkUri(intent?.data)

fun parseShareLinkUri(uri: Uri?): ShareLinkPayload? {
    if (uri == null || uri.scheme != "kora" || uri.host != "share") {
        return null
    }
    val shareId =
        when {
            uri.pathSegments.firstOrNull() == "chat" -> uri.getQueryParameter("shareId")
            uri.pathSegments.isNotEmpty() -> uri.pathSegments.firstOrNull()
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return null
    val outLinkUid = uri.getQueryParameter("outLinkUid")?.takeIf { it.isNotBlank() } ?: return null
    val chatId = uri.getQueryParameter("chatId")?.takeIf { it.isNotBlank() }
    return ShareLinkPayload(
        shareId = shareId,
        outLinkUid = outLinkUid,
        chatId = chatId,
    )
}
