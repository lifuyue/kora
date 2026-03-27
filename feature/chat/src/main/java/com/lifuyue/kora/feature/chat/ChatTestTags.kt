package com.lifuyue.kora.feature.chat

object ChatTestTags {
    const val conversationSearch = "conversation_search"
    const val conversationFab = "conversation_fab"
    const val conversationClearAll = "conversation_clear_all"
    const val conversationActionsSheet = "conversation_actions_sheet"
    const val conversationActionRename = "conversation_action_rename"
    const val conversationActionTogglePin = "conversation_action_toggle_pin"
    const val conversationActionDelete = "conversation_action_delete"
    const val renameConversationInput = "rename_conversation_input"
    const val conversationItemPrefix = "conversation_item_"
    const val chatInput = "chat_input"

    fun messageCard(messageId: String): String = "message_card_$messageId"

    fun messageCopyAction(messageId: String): String = "message_copy_$messageId"

    fun messageRegenerateAction(messageId: String): String = "message_regenerate_$messageId"

    fun messageUpvoteAction(messageId: String): String = "message_upvote_$messageId"

    fun messageDownvoteAction(messageId: String): String = "message_downvote_$messageId"

    fun messageError(messageId: String): String = "message_error_$messageId"
}
