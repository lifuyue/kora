# Local Storage

## Room Entities

### `ConversationEntity`
- `chatId: String` primary key
- `appId: String`
- `title: String`
- `customTitle: String?`
- `isPinned: Boolean`
- `source: String`
- `updateTime: Long`
- `lastMessagePreview: String?`
- `hasDraft: Boolean`
- `isDeleted: Boolean`

### `MessageEntity`
- `dataId: String` primary key
- `chatId: String`
- `appId: String`
- `role: String`
- `payloadJson: String`
- `createdAt: Long`
- `isStreaming: Boolean`
- `sendStatus: String`
- `errorCode: Int?`

### `CachedDatasetEntity`
- `datasetId: String` primary key
- `name: String`
- `type: String`
- `status: String`
- `updateTime: Long`
- `summaryJson: String`

## DataStore Keys
- `server_base_url`
- `server_api_key_present`
- `selected_app_id`
- `theme_mode`
- `dynamic_color_enabled`
- `language_tag`
- `tts_enabled`
- `stt_auto_send`
- `chat_markdown_enabled`
- `chat_show_citations`
- `storage_last_cleanup_at`
- `onboarding_completed`

## Persistence Rules
- API key secret is stored outside DataStore in encrypted storage; DataStore only stores presence and profile metadata.
- Share-session state uses a separate namespace from primary auth state.
- Pending interactive drafts must survive process death.

## Cache Ownership
- Room stores user continuity.
- DataStore stores app preferences and connection metadata.
- Media/file cache stores previews and temporary uploads.

## Related Specs
- [data-flow.md](data-flow.md)
- [features/settings/storage-cache.md](../features/settings/storage-cache.md)
