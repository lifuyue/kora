# FastGPT API Map

## Snapshot
- Upstream: FastGPT `v4.14.9.5`
- Purpose: map concrete FastGPT endpoints and types to Kora specs and implementation phases.

## Chat and Records
| FastGPT Endpoint / Type | Method | Kora Spec | Phase | Notes |
|---|---|---|---|---|
| `/api/v1/chat/completions` | `POST` | [../api/chat-completions.md](../api/chat-completions.md) | M1+ | Core chat/event-bus entrypoint |
| `SseResponseEventEnum` | type | [../api/chat-streaming.md](../api/chat-streaming.md) | M1+ | 16 event types |
| `/api/core/chat/init` | `GET` | [../api/chat-records.md](../api/chat-records.md) | M1+ | Bootstrap app/chat config |
| `/api/core/chat/getPaginationRecords` | `POST` | [../api/chat-records.md](../api/chat-records.md) | M1+ | Message pagination |
| `/api/core/chat/getResData` | `GET` | [../api/chat-records.md](../api/chat-records.md) | M1+ | Node-response detail |
| `/api/core/chat/item/delete` | `POST` | [../api/chat-records.md](../api/chat-records.md) | M2+ | Soft delete one message |
| `/api/core/chat/feedback/updateUserFeedback` | `POST` | [../api/chat-records.md](../api/chat-records.md) | M2+ | Good/bad feedback |
| `/api/core/chat/history/getHistories` | `POST` | [../api/chat-history.md](../api/chat-history.md) | M1+ | Conversation list |
| `/api/core/chat/history/updateHistory` | `PUT` | [../api/chat-history.md](../api/chat-history.md) | M1+ | Rename/top |
| `/api/core/chat/history/delHistory` | `DELETE` | [../api/chat-history.md](../api/chat-history.md) | M1+ | Soft delete conversation |
| `/api/core/chat/history/clearHistories` | `DELETE` | [../api/chat-history.md](../api/chat-history.md) | M1+ | Clear per app/auth scope |
| `/api/core/ai/agent/v2/createQuestionGuide` | `POST` | [../api/question-guide.md](../api/question-guide.md) | M2+ | Suggested follow-up questions |

## Dataset and Search
| FastGPT Endpoint / Type | Method | Kora Spec | Phase | Notes |
|---|---|---|---|---|
| `DatasetSchemaType` | type | [../api/dataset-management.md](../api/dataset-management.md) | M2+ | Dataset core model |
| `DatasetCollectionSchemaType` | type | [../api/dataset-collections.md](../api/dataset-collections.md) | M2+ | Collection core model |
| `DatasetDataSchemaType` | type | [../api/dataset-data.md](../api/dataset-data.md) | M2+ | Chunk/data model |
| `/api/core/dataset/list` | `POST` | [../api/dataset-management.md](../api/dataset-management.md) | M2+ | Dataset browser |
| `/api/core/dataset/create` | `POST` | [../api/dataset-management.md](../api/dataset-management.md) | M2+ | Dataset create |
| `/api/core/dataset/update` | `POST` | [../api/dataset-management.md](../api/dataset-management.md) | M2+ | Dataset update |
| `/api/core/dataset/delete` | `POST` | [../api/dataset-management.md](../api/dataset-management.md) | M2+ | Dataset delete |
| `/api/core/dataset/collection/create/text` | `POST` | [../api/dataset-collections.md](../api/dataset-collections.md) | M2+ | Text import |
| `/api/core/dataset/collection/create/link` | `POST` | [../api/dataset-collections.md](../api/dataset-collections.md) | M2+ | URL import |
| `/api/core/dataset/collection/create/localFile` | `POST` | [../api/file-upload.md](../api/file-upload.md) | M2+ | Multipart upload |
| `/api/core/dataset/collection/create/fileId` | `POST` | [../api/dataset-collections.md](../api/dataset-collections.md) | M2+ | Reuse uploaded file |
| `/api/core/dataset/collection/create/apiCollection` | `POST` | [../api/dataset-collections.md](../api/dataset-collections.md) | M3+ | API dataset import |
| `/api/core/dataset/collection/create/apiCollectionV2` | `POST` | [../api/dataset-collections.md](../api/dataset-collections.md) | M3+ | Recursive API import |
| `/api/core/dataset/searchTest` | `POST` | [../api/dataset-search.md](../api/dataset-search.md) | M2+ | Retrieval debugging |

## App and Share
| FastGPT Endpoint / Type | Method | Kora Spec | Phase | Notes |
|---|---|---|---|---|
| `AppSchemaType` | type | [../api/app-management.md](../api/app-management.md) | M1+ | App entity |
| `AppChatConfigType` | type | [../api/app-management.md](../api/app-management.md) | M1+ | Chat/UI config surface |
| `/api/core/app/list` | `POST` | [../api/app-management.md](../api/app-management.md) | M1+ | App selector |
| `/api/core/chat/outLink/init` | `GET` | [../api/share-auth.md](../api/share-auth.md) | M3+ | Share chat init |
| `/shareAuth/init` | external hook | [../api/share-auth.md](../api/share-auth.md) | M3+ | External share hook |
| `/shareAuth/start` | external hook | [../api/share-auth.md](../api/share-auth.md) | M3+ | External preflight auth |
| `/shareAuth/finish` | external hook | [../api/share-auth.md](../api/share-auth.md) | M3+ | External result callback |

## Error / Cross-Cutting
| FastGPT Endpoint / Type | Method | Kora Spec | Phase | Notes |
|---|---|---|---|---|
| `ResponseType<T>` | type | [../api/error-handling.md](../api/error-handling.md) | M1+ | JSON envelope |
| `ERROR_RESPONSE` | type map | [../api/error-handling.md](../api/error-handling.md) | M1+ | Domain error mapping |
| OpenAPI key create format | create logic | [../api/authentication.md](../api/authentication.md) | M1+ | `fastgpt-` prefix by default |

## Feature Coverage Map
- Chat: [../features/chat/streaming-chat.md](../features/chat/streaming-chat.md)
- Conversations: [../features/conversations/conversation-list.md](../features/conversations/conversation-list.md)
- Knowledge: [../features/knowledge/knowledge-overview.md](../features/knowledge/knowledge-overview.md)
- Apps: [../features/apps/app-selector.md](../features/apps/app-selector.md)
- Share/Auth: [../features/auth/share-link-auth.md](../features/auth/share-link-auth.md)
