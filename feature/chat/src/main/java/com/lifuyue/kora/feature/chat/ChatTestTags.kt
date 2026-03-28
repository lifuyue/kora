package com.lifuyue.kora.feature.chat

object ChatTestTags {
    const val conversationSearch = "conversation_search"
    const val conversationFab = "conversation_fab"
    const val conversationClearAll = "conversation_clear_all"
    const val conversationFolderFilter = "conversation_folder_filter"
    const val conversationTagFilter = "conversation_tag_filter"
    const val conversationActionsSheet = "conversation_actions_sheet"
    const val conversationActionRename = "conversation_action_rename"
    const val conversationActionTogglePin = "conversation_action_toggle_pin"
    const val conversationActionDelete = "conversation_action_delete"
    const val conversationActionMoveFolder = "conversation_action_move_folder"
    const val conversationActionEditTags = "conversation_action_edit_tags"
    const val conversationFolderSheet = "conversation_folder_sheet"
    const val conversationTagSheet = "conversation_tag_sheet"
    const val renameConversationInput = "rename_conversation_input"
    const val conversationItemPrefix = "conversation_item_"
    const val chatInput = "chat_input"
    const val citationPanel = "citation_panel"
    const val citationSummaryPrefix = "citation_summary_"

    fun messageCard(messageId: String): String = "message_card_$messageId"

    fun messageCopyAction(messageId: String): String = "message_copy_$messageId"

    fun messageRegenerateAction(messageId: String): String = "message_regenerate_$messageId"

    fun messageUpvoteAction(messageId: String): String = "message_upvote_$messageId"

    fun messageDownvoteAction(messageId: String): String = "message_downvote_$messageId"

    fun messageError(messageId: String): String = "message_error_$messageId"

    fun citationSummary(messageId: String): String = "citation_summary_$messageId"
}
