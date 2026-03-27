# Glossary

## Product and Platform
- Kora: Android-native FastGPT client built around chat, knowledge, and settings flows.
- FastGPT: backend platform providing chat, workflow, dataset, app, and share-link capabilities.
- Open WebUI: interaction reference for chat, sidebar, settings, markdown, and voice patterns.

## Android Terms
- Compose: declarative Android UI toolkit used for all screens.
- Hilt: dependency injection framework.
- Room: typed SQLite abstraction used for local continuity and cached metadata.
- DataStore: preference/config storage for non-sensitive local settings.
- ViewModel: lifecycle-aware state holder for screens.

## AI / Retrieval Terms
- RAG: retrieval-augmented generation.
- SSE: server-sent events stream used by FastGPT chat completions.
- Citation: reference/source information attached to an assistant answer.
- Re-rank: second-stage ranking of retrieved candidates.
- Extension Query: query expansion step used in dataset search test.

## FastGPT-Specific Terms
- App: chat-capable app/workflow/tool shell with `chatConfig`.
- AppChatConfig: app-scoped UI/runtime config including welcome text, variables, question guide, TTS, whisper, and file selection.
- Dataset: top-level knowledge container.
- Collection: imported source unit under a dataset.
- Chunk: processed knowledge fragment / dataset data row.
- History: conversation-level summary record.
- Record: message-level chat item.
- Interactive Node: workflow step requiring user choice or input.
- `responseChatItemId`: client-generated assistant message id used to merge one stream into one UI message.
- `flowResponses`: final structured node-response payload returned by FastGPT chat runs.
- `shareId`: identifier of an out-link/share publication.
- `outLinkUid`: share-session user token or normalized share-session identity handle.
