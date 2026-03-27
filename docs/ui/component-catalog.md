# Component Catalog

## Chat Components
| Component | Key props | Usage |
|---|---|---|
| `MessageBubble` | `role`, `content`, `status` | user/assistant/system shell |
| `RenderedMarkdown` | `markdown`, `policy` | assistant rich text |
| `ReasoningPanel` | `text`, `expanded` | thinking content |
| `ToolStatusCard` | `toolName`, `params`, `response`, `status` | tool call lifecycle |
| `CitationSummaryChip` | `count`, `onClick` | message citation entry |
| `SuggestedQuestionRow` | `items`, `onSelect` | assistant follow-up questions |
| `AttachmentDraftRow` | `attachments`, `onRemove`, `onRetry` | composer uploads |
| `InteractiveCard` | `model`, `onSubmit` | workflow interactive node |

## Conversation Components
| Component | Key props | Usage |
|---|---|---|
| `ConversationRow` | `item`, `onOpen`, `onMore` | conversation list |
| `ConversationFilterBar` | `query`, `filters` | search/filter UI |
| `ConversationActionSheet` | `actions`, `onAction` | rename/delete/pin/etc |

## Knowledge Components
| Component | Key props | Usage |
|---|---|---|
| `DatasetCard` | `dataset`, `onOpen` | dataset browser |
| `CollectionRow` | `collection`, `onOpen`, `onMore` | collection list |
| `ChunkRow` | `chunk`, `onOpen` | chunk viewer |
| `SearchResultCard` | `result`, `onOpenSource` | search test result |
| `ImportTaskCard` | `task`, `onRetry` | upload/import status |

## Settings Components
| Component | Key props | Usage |
|---|---|---|
| `SettingsRow` | `title`, `summary`, `onClick` | settings overview |
| `SettingsSwitchRow` | `checked`, `onCheckedChange` | boolean preference |
| `SettingsSliderRow` | `value`, `range`, `onChange` | font scale / speed |
| `ConnectionStatusCard` | `status`, `details` | connection result |

## Shared Components
| Component | Key props | Usage |
|---|---|---|
| `LoadingSkeleton` | `variant` | loading states |
| `EmptyStateCard` | `title`, `body`, `action` | empty states |
| `ErrorStateCard` | `title`, `message`, `retry` | recoverable failure |
| `ConfirmActionDialog` | `title`, `body`, `confirmKind` | dangerous actions |
