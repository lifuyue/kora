# Component Catalog

## Shared Workspace
| Component | Responsibility | Notes |
|---|---|---|
| `KoraWorkspaceHeroCard` | Screen summary, orientation, status entry | Used at the top of chat empty state, conversation list, knowledge, settings |
| `KoraSectionCard` | Standard workspace card container | Shared tonal layer and spacing |
| `KoraMetricRow` | Two-column summary row | Used for connection/theme/app summaries |
| `KoraWorkspaceSectionTitle` | Section title + supporting copy | Keeps section hierarchy consistent |

## Chat
| Component | Responsibility | Notes |
|---|---|---|
| `MessageCard` | Message shell for user/assistant/system content | Must preserve clear role distinction |
| `RenderedMarkdown` | Rich text rendering | Optimized for reading, copy, code, citations |
| `AttachmentComposer` | Upload draft surface | Treated as part of the main composer, not a separate page |

## Conversation
| Component | Responsibility | Notes |
|---|---|---|
| `ConversationListCard` | Conversation summary row/card | Long press opens action sheet |
| `OutlinedActionChip` | Search/filter toggles | Shared selection grammar across pages |
| `ConversationActionSheet` | Rename, pin, archive, delete | Action hierarchy must stay stable |

## Knowledge
| Component | Responsibility | Notes |
|---|---|---|
| `DatasetCard` | Dataset summary and entry point | Must expose name, type, status, recency |
| `CollectionCard` | Collection summary | Same tonal system as conversation cards |
| `ImportTaskCard` | Background import status | Error state requires recovery affordance |

## Settings
| Component | Responsibility | Notes |
|---|---|---|
| `SettingsStatusCard` | Global workspace summary | Connection, theme, selected app |
| `SettingsEntry` | Section entry row | Summary-first, action-second |
| `ConnectionTestResultCard` | Validation feedback | Success and failure must read clearly |
