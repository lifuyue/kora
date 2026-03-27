# Architecture Overview

## Snapshot
- Kora is an Android-native client for FastGPT-style chat, knowledge, and app selection.
- Upstream product references:
  - FastGPT `v4.14.9.5`
  - Open WebUI `v0.8.12`

## C4 Narrative
- System
  Kora Android App talks to FastGPT over HTTPS and consumes JSON + SSE.
- Container
  `:app` hosts Android entrypoints, `:core:*` owns shared infrastructure, `:feature:*` owns domain behavior.
- Component
  Each feature contains screen, ViewModel, reducer/state, repository, and UI component sublayers.
- Code
  Compose renders immutable screen state produced from ViewModel + Flow streams.

## Dependency Direction
- `:app -> :feature:* -> :core:*`
- `:core:*` never depends on `:feature:*`
- `:feature:*` must not depend directly on sibling features
- Shared domain types can live in `:core:common` or a later `:core:model` extraction if needed

## Product Domains
- Chat
  Streaming completions, records, citations, interactive nodes, message actions.
- Knowledge
  Dataset, collection, chunk, upload, search test.
- Settings/Auth
  Connection profile, theme, language, storage, audio.
- App selection
  App list, app detail, app-scoped chat config bootstrap.

## Key Architecture Decisions
- SSE is parsed in `:core:network`, not in Compose.
- Chat screen state is derived from a single ordered message timeline containing text, reasoning, tools, plans, citations, and interactive nodes.
- Room stores local continuity and offline resume state; FastGPT remains the source of truth for server-owned history.

## Related Specs
- [module-structure.md](module-structure.md)
- [networking.md](networking.md)
- [data-flow.md](data-flow.md)
