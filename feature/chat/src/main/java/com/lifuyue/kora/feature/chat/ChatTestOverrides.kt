package com.lifuyue.kora.feature.chat

object ChatTestOverrides {
    var chatRepository: ChatRepository? = null
    var conversationRepository: ConversationRepository? = null

    fun reset() {
        chatRepository = null
        conversationRepository = null
    }
}
