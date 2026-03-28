package com.lifuyue.kora.navigation

object ShareRoutes {
    const val CHAT = "share/chat?shareId={shareId}&outLinkUid={outLinkUid}&chatId={chatId}"

    fun chat(
        shareId: String,
        outLinkUid: String,
        chatId: String? = null,
    ): String =
        buildString {
            append("share/chat?shareId=")
            append(shareId)
            append("&outLinkUid=")
            append(outLinkUid)
            append("&chatId=")
            append(chatId.orEmpty())
        }
}
