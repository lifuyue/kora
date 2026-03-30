package com.lifuyue.kora.feature.chat

object ChatTestTags {
    const val CONVERSATION_LIST = "conversation_list"
    const val CONVERSATION_SEARCH = "conversation_search"
    const val CONVERSATION_FAB = "conversation_fab"
    const val CONVERSATION_CLEAR_ALL = "conversation_clear_all"
    const val CONVERSATION_FOLDER_FILTER = "conversation_folder_filter"
    const val CONVERSATION_TAG_FILTER = "conversation_tag_filter"
    const val CONVERSATION_ACTIONS_SHEET = "conversation_actions_sheet"
    const val CONVERSATION_ACTION_RENAME = "conversation_action_rename"
    const val CONVERSATION_ACTION_TOGGLE_PIN = "conversation_action_toggle_pin"
    const val CONVERSATION_ACTION_ARCHIVE = "conversation_action_archive"
    const val CONVERSATION_ACTION_DELETE = "conversation_action_delete"
    const val CONVERSATION_ACTION_MOVE_FOLDER = "conversation_action_move_folder"
    const val CONVERSATION_ACTION_EDIT_TAGS = "conversation_action_edit_tags"
    const val CONVERSATION_FOLDER_SHEET = "conversation_folder_sheet"
    const val CONVERSATION_TAG_SHEET = "conversation_tag_sheet"
    const val CONVERSATION_BROWSER_SHEET = "conversation_browser_sheet"
    const val CONVERSATION_BROWSER_CLOSE = "conversation_browser_close"
    const val RENAME_CONVERSATION_INPUT = "rename_conversation_input"
    const val CONVERSATION_ITEM_PREFIX = "conversation_item_"
    const val CHAT_INPUT = "chat_input"
    const val CHAT_MENU_BUTTON = "chat_menu_button"
    const val CHAT_QUICK_SETTINGS_BUTTON = "chat_quick_settings_button"
    const val CHAT_ATTACHMENT_TRIGGER_BUTTON = "chat_attachment_trigger_button"
    const val CHAT_PRIMARY_ACTION_BUTTON = "chat_primary_action_button"
    const val CHAT_DRAWER = "chat_drawer"
    const val CHAT_DRAWER_NEW_CHAT = "chat_drawer_new_chat"
    const val CHAT_DRAWER_KNOWLEDGE = "chat_drawer_knowledge"
    const val CHAT_DRAWER_SETTINGS = "chat_drawer_settings"
    const val CHAT_DRAWER_SEARCH = "chat_drawer_search"
    const val CHAT_SUGGESTION_PREFIX = "chat_suggestion_"
    const val CHAT_ATTACHMENT_IMAGE_PICK = "chat_attachment_pick_image"
    const val CHAT_ATTACHMENT_FILE_PICK = "chat_attachment_pick_file"
    const val CHAT_ATTACHMENT_LIST = "chat_attachment_list"
    const val CHAT_ATTACHMENT_ITEM_PREFIX = "chat_attachment_item_"
    const val CHAT_SPEECH_STATUS = "chat-speech-status"
    const val CHAT_MIC_BUTTON = "chat-mic-button"
    const val CHAT_SPEECH_STOP = "chat-speech-stop"
    const val CHAT_SPEECH_CANCEL = "chat-speech-cancel"
    const val CHAT_LIST = "chat_list"
    const val CHAT_SKELETON = "chat_skeleton"
    const val AUTO_SCROLL_RESUME = "auto_scroll_resume"
    const val CITATION_PANEL = "citation_panel"
    const val CITATION_SUMMARY_PREFIX = "citation_summary_"
    const val MERMAID_BLOCK_PREFIX = "mermaid_block_"

    fun messageCard(messageId: String): String = "message_card_$messageId"

    fun messageCopyAction(messageId: String): String = "message_copy_$messageId"

    fun messageRegenerateAction(messageId: String): String = "message_regenerate_$messageId"

    fun messageUpvoteAction(messageId: String): String = "message_upvote_$messageId"

    fun messageDownvoteAction(messageId: String): String = "message_downvote_$messageId"

    fun messageTtsAction(messageId: String): String = "message_tts_$messageId"

    fun messageTtsPauseAction(messageId: String): String = "message_tts_pause_$messageId"

    fun messageTtsStopAction(messageId: String): String = "message_tts_stop_$messageId"

    fun messageError(messageId: String): String = "message_error_$messageId"

    fun citationSummary(messageId: String): String = "${CITATION_SUMMARY_PREFIX}$messageId"

    fun interactiveCard(messageId: String): String = "interactive_card_$messageId"

    fun interactiveOption(
        messageId: String,
        option: String,
    ): String = "interactive_option_${messageId}_${option.hashCode()}"

    fun interactiveFieldInput(
        messageId: String,
        fieldId: String,
    ): String = "interactive_field_${messageId}_$fieldId"

    fun interactiveSubmit(messageId: String): String = "interactive_submit_$messageId"

    fun attachmentItem(localUri: String): String = "${CHAT_ATTACHMENT_ITEM_PREFIX}$localUri"
}
