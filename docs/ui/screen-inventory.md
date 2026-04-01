# Screen Inventory

## Primary Screens
| Screen | Route | Feature |
|---|---|---|
| `OnboardingScreen` | `onboarding` | auth/settings |
| `AppSelectorScreen` | `app-selector` | apps |
| `ChatScreen` | `chat/{appId}/{chatId?}` | chat |
| `ConversationListScreen` | `conversations/{appId}` | conversations |
| `KnowledgeHomeScreen` | `knowledge` | knowledge |
| `DatasetBrowserScreen` | `knowledge/datasets` | knowledge |
| `CollectionScreen` | `knowledge/datasets/{datasetId}/collections` | knowledge |
| `ChunkViewerScreen` | `knowledge/datasets/{datasetId}/collections/{collectionId}` | knowledge |
| `SearchTestScreen` | `knowledge/datasets/{datasetId}/search-test` | knowledge |
| `SettingsOverviewScreen` | `settings` | settings |

## Secondary / Modal Screens
| Screen | Form |
|---|---|
| `ConnectionConfigScreen` | full screen |
| `ThemeAppearanceScreen` | full screen |
| `LanguageScreen` | full screen |
| `StorageCacheScreen` | full screen |
| `AboutScreen` | full screen |
| `CitationSheet` | bottom sheet |
| `MessageActionSheet` | bottom sheet |
| `ConversationActionSheet` | bottom sheet |
| `InteractiveInputSheet` | bottom sheet or inline form |

## Share Flow
| Screen | Route | Notes |
|---|---|---|
| `ShareLoadingScreen` | `share/chat?...` | bootstraps share auth |
| `ShareChatScreen` | `share/chat?...` | isolated session graph |

## ViewModel Ownership
- `ChatViewModel`
- `ConversationListViewModel`
- `KnowledgeOverviewViewModel`
- `DatasetBrowserViewModel`
- `CollectionViewModel`
- `ChunkViewerViewModel`
- `SettingsOverviewViewModel`
- `ConnectionConfigViewModel`
