# Module Structure

## Planned Modules
- `:app`
  Android application, activity, nav host, top-level dependency wiring.
- `:core:common`
  Result types, error mapping, shared enums, date/time helpers, resource-safe models.
- `:core:network`
  Retrofit services, OkHttp client, auth interceptors, SSE parser, DTOs.
- `:core:database`
  Room database, DAOs, entities, migrations.
- `:feature:chat`
  Chat screen, message list, markdown, citations, message actions, conversation entry.
- `:feature:knowledge`
  Dataset list/detail, collection browser, upload, chunk editor, search test.
- `:feature:settings`
  Onboarding, connection config, theme, language, storage, audio, about.

## Public Boundaries
- `:core:common`
  Exposes domain-safe types such as `AppResult`, `ConnectionProfile`, `SseEvent`, `ChatMessageUiModel`.
- `:core:network`
  Exposes repository-facing service interfaces and streaming event adapters.
- `:core:database`
  Exposes DAO interfaces and transaction helpers, not raw SQLite APIs.
- `:feature:*`
  Expose navigation entry functions and top-level screen composables only.

## Forbidden Dependencies
- Feature-to-feature direct imports.
- `:core:network -> :core:database`
- `:core:* -> :feature:*`
- Screen composables importing Retrofit/Room directly.

## Ownership Rules
- Transport-specific models stay in `:core:network`.
- Storage-specific models stay in `:core:database`.
- Screen state and reducers stay inside the owning feature module.

## Related Specs
- [overview.md](overview.md)
- [navigation.md](navigation.md)
