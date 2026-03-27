# Networking

## OkHttp Stack
Recommended interceptor order:
1. Base URL / environment resolver
2. Auth interceptor
3. Common headers interceptor
4. Logging interceptor in debug only
5. Retry policy for idempotent requests only

## Default Headers
- `Authorization: Bearer fastgpt-*`
- `Content-Type: application/json`
- Locale header may be added later for multilingual user-facing responses

## Timeout Policy
- REST connect timeout: `15s`
- REST read timeout: `30s`
- Upload timeout: `120s`
- SSE call timeout: disabled or effectively very large
- First-event watchdog in app layer: `60s` to match upstream web behavior

## SSE Implementation Decision
- Choose manual OkHttp streaming parser over Retrofit converters or browser-like EventSource wrappers.
- Reason:
  - FastGPT mixes `event:` and `data:` frames.
  - Kora must preserve event order and parse non-text payloads.
  - Stream termination includes `[DONE]` plus optional trailing `flowResponses`.

## REST / Stream Split
- Retrofit handles normal JSON APIs.
- Dedicated `SseStreamClient` handles `/api/v1/chat/completions` when `stream=true`.

## Error Propagation
- Convert JSON envelopes into domain errors in `:core:network`.
- Convert SSE `event=error` frames into the same domain error type.
- Preserve:
  - `code`
  - `statusText`
  - `message`

## Related Specs
- [chat-streaming.md](../api/chat-streaming.md)
- [error-handling.md](../api/error-handling.md)
