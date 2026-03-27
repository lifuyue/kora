# Authentication

## Snapshot
- Upstream: FastGPT `v4.14.9.5`
- Primary sources:
  - `.reference/FastGPT/projects/app/src/pages/api/support/openapi/create.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/v1/chat/completions.ts`
  - `.reference/FastGPT/packages/global/support/permission/chat.ts`

## Supported Auth Contexts
- `Authorization: Bearer fastgpt-...`
  Used for normal logged-in API access and OpenAPI chat access.
- `shareId + outLinkUid`
  Used for public share/out-link sessions.
- `teamId + teamToken`
  Used for team-space chat sessions.

## API Key Format
- FastGPT creates API keys as `${openapiPrefix || 'fastgpt'}-${nanoid}`.
- In the default deployment this means keys start with `fastgpt-`.
- Kora must reject obviously invalid keys before saving when the value does not start with `fastgpt-`.

## Key Types
- Global API key:
  Created under `/api/support/openapi/create` without `appId`; authorizes team-scoped OpenAPI access.
- App-level API key:
  Created with an `appId`; used to access a specific app or workflow tool.

## Request Conventions
- Kora always sends API keys through `Authorization: Bearer <apiKey>`.
- `shareId/outLinkUid` and `teamId/teamToken` are request parameters, not bearer tokens.
- Chat APIs can authenticate with either bearer API key or cookie/token auth; Kora targets bearer API key.

## Android Storage Rules
- Persist `baseUrl` and non-sensitive connection metadata in DataStore.
- Persist the raw API key in encrypted storage first; mirror only minimal connection state into DataStore.
- Store share-session credentials separately from the primary connection profile.

## Failure Semantics
- Invalid/missing token usually resolves to `403` with `statusText=unAuthorization`.
- Invalid API key can also map to `514 / unAuthApiKey` or `506000+` OpenAPI errors depending on route.
- Share-link auth failures map to out-link errors such as `outlinkUnAuthUser`, `outlinkUnExist`, `outlinkUnAuthLink`.

## Kora Decisions
- On save, validate:
  - URL is absolute HTTPS or trusted HTTP dev endpoint.
  - API key is non-empty and starts with `fastgpt-`.
- On any auth failure, move the connection profile into `invalid` state and surface a reconnect CTA.
- Share-link sessions never overwrite the user's primary API key profile.

## Related Specs
- [chat-completions.md](chat-completions.md)
- [share-auth.md](share-auth.md)
- [error-handling.md](error-handling.md)
