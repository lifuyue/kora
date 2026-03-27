# API Overview

## Scope
- Authentication and connection bootstrap
- Chat completions and SSE
- Chat history and message records
- Dataset / collection / chunk management
- Search test
- App discovery and chat config bootstrap
- Upload and share-link auth

## Endpoint Table
| Area | Method | Endpoint | Spec |
|---|---|---|---|
| Auth | Header | `Authorization: Bearer fastgpt-*` | [authentication.md](authentication.md) |
| Chat | `POST` | `/api/v1/chat/completions` | [chat-completions.md](chat-completions.md) |
| Chat init | `GET` | `/api/core/chat/init` | [chat-records.md](chat-records.md) |
| Chat history | `POST` | `/api/core/chat/history/getHistories` | [chat-history.md](chat-history.md) |
| Chat history | `PUT` | `/api/core/chat/history/updateHistory` | [chat-history.md](chat-history.md) |
| Chat history | `DELETE` | `/api/core/chat/history/delHistory` | [chat-history.md](chat-history.md) |
| Chat history | `DELETE` | `/api/core/chat/history/clearHistories` | [chat-history.md](chat-history.md) |
| Chat records | `POST` | `/api/core/chat/getPaginationRecords` | [chat-records.md](chat-records.md) |
| Chat records | `GET` | `/api/core/chat/getResData` | [chat-records.md](chat-records.md) |
| Chat item | `POST` | `/api/core/chat/item/delete` | [chat-records.md](chat-records.md) |
| Feedback | `POST` | `/api/core/chat/feedback/updateUserFeedback` | [chat-records.md](chat-records.md) |
| Question guide | `POST` | `/api/core/ai/agent/v2/createQuestionGuide` | [question-guide.md](question-guide.md) |
| Share init | `GET` | `/api/core/chat/outLink/init` | [share-auth.md](share-auth.md) |
| App list | `POST` | `/api/core/app/list` | [app-management.md](app-management.md) |
| Dataset search test | `POST` | `/api/core/dataset/searchTest` | [dataset-search.md](dataset-search.md) |
| Collection create | `POST` | `/api/core/dataset/collection/create/text` | [dataset-collections.md](dataset-collections.md) |
| Collection create | `POST` | `/api/core/dataset/collection/create/link` | [dataset-collections.md](dataset-collections.md) |
| Collection create | `POST` | `/api/core/dataset/collection/create/localFile` | [file-upload.md](file-upload.md) |
| Collection create | `POST` | `/api/core/dataset/collection/create/fileId` | [dataset-collections.md](dataset-collections.md) |
| Collection create | `POST` | `/api/core/dataset/collection/create/apiCollection` | [dataset-collections.md](dataset-collections.md) |
| Collection create | `POST` | `/api/core/dataset/collection/create/apiCollectionV2` | [dataset-collections.md](dataset-collections.md) |

## Kora Defaults
- All chat calls use `detail=true`.
- Chat sends use `stream=true` except explicit fallback or tests.
- All requests preserve raw `code` and `statusText` for error mapping.

## Related Specs
- [chat-streaming.md](chat-streaming.md)
- [error-handling.md](error-handling.md)
