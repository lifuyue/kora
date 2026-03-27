# Data Flow

## Canonical Chat Flow
```text
ChatScreen
  -> ChatViewModel.onSend()
  -> ChatRepository.startCompletion()
  -> FastGptApi / SseStreamClient
  -> SseEventParser
  -> ChatRepository merges events into MessageFlow
  -> ChatViewModel reduces MessageFlow into ChatUiState
  -> ChatScreen recomposes
```

## Read Flow
- UI requests screen state.
- ViewModel subscribes to repository flow.
- Repository first emits local cache if available.
- Repository refreshes remote source and merges into DB / memory cache.
- ViewModel emits `loading -> success/error` transitions without dropping existing visible content.

## Write Flow
- UI emits intent.
- ViewModel validates and creates optimistic local state.
- Repository performs network write.
- On success, repository commits canonical server-owned ids and timestamps.
- On failure, optimistic state becomes recoverable error state.

## Streaming Chat Merge Rules
- `answer` / `fastAnswer` update visible text.
- `tool*`, `plan`, `stepTitle`, `interactive`, `collectionForm`, `topAgentConfig` update structured assistant sub-blocks.
- `flowResponses` finalizes citations and node-response inspection data.
- `updateVariables` updates app/session variables after the response.

## Offline Strategy
- Conversations and records are cache-first for resume.
- New sends are network-first.
- Pending sends and unresolved interactive forms are persisted locally until resolved or discarded.

## Related Specs
- [networking.md](networking.md)
- [local-storage.md](local-storage.md)
