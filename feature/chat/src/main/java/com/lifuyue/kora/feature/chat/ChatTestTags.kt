package com.lifuyue.kora.feature.chat

object ChatTestTags {
    const val CONVERSATION_SEARCH = "conversation_search"
    const val CONVERSATION_FAB = "conversation_fab"
    const val CONVERSATION_CLEAR_ALL = "conversation_clear_all"
    const val CONVERSATION_FOLDER_FILTER = "conversation_folder_filter"
    const val CONVERSATION_TAG_FILTER = "conversation_tag_filter"
    const val CONVERSATION_ACTIONS_SHEET = "conversation_actions_sheet"
    const val CONVERSATION_ACTION_RENAME = "conversation_action_rename"
    const val CONVERSATION_ACTION_TOGGLE_PIN = "conversation_action_toggle_pin"
    const val CONVERSATION_ACTION_DELETE = "conversation_action_delete"
    const val CONVERSATION_ACTION_MOVE_FOLDER = "conversation_action_move_folder"
    const val CONVERSATION_ACTION_EDIT_TAGS = "conversation_action_edit_tags"
    const val CONVERSATION_FOLDER_SHEET = "conversation_folder_sheet"
    const val CONVERSATION_TAG_SHEET = "conversation_tag_sheet"
    const val RENAME_CONVERSATION_INPUT = "rename_conversation_input"
    const val CONVERSATION_ITEM_PREFIX = "conversation_item_"
    const val CHAT_INPUT = "chat_input"
    const val CITATION_PANEL = "citation_panel"
    const val CITATION_SUMMARY_PREFIX = "citation_summary_"

    fun messageCard(messageId: String): String = "message_card_$messageId"

    fun messageCopyAction(messageId: String): String = "message_copy_$messageId"

    fun messageRegenerateAction(messageId: String): String = "message_regenerate_$messageId"

    fun messageUpvoteAction(messageId: String): String = "message_upvote_$messageId"

    fun messageDownvoteAction(messageId: String): String = "message_downvote_$messageId"

    fun messageError(messageId: String): String = "message_error_$messageId"

    fun citationSummary(messageId: String): String = "${CITATION_SUMMARY_PREFIX}$messageId"
}
