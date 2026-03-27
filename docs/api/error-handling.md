# Error Handling

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/service/common/response/index.ts`
  - `.reference/FastGPT/packages/global/common/error/errorCode.ts`
  - `.reference/FastGPT/packages/global/common/error/code/*.ts`

## Success Envelope
FastGPT JSON responses are normalized as:
```ts
type ResponseType<T> = {
  code: number;
  message: string;
  data: T;
}
```

Actual HTTP JSON success emitted by `jsonRes` also includes:
```ts
{
  code: number;
  statusText: "";
  message: string;
  data: T | null;
}
```

## Error Envelope
```ts
{
  code: number;
  statusText: string;
  message: string;
  data: any | null;
  zodError?: any;
}
```

## Standard HTTP/transport codes
- `400`
- `401`
- `403`
- `404`
- `405`
- `406`
- `410`
- `422`
- `429`
- `500`
- `502`
- `503`
- `504`

## Built-in global status texts
- `unAuthorization` -> `403`
- `tooManyRequest` -> `429`
- `insufficientQuota` -> `510`
- `unAuthModel` -> `511`
- `unAuthFile` -> `513`
- `unAuthApiKey` -> `514`

## Domain Error Ranges
- Team: `500000+`
- Dataset: `501000+`
- App: `502000+`
- User: `503000+`
- Chat: `504000+`
- OutLink: `505000+`
- OpenAPI: `506000+`
- Common: `507000+`
- Plugin: `508000+`
- System/license: `509000+`

## Important Domain Errors
- Dataset:
  - `unExistDataset`
  - `unExistCollection`
  - `unAuthDataset`
  - `unAuthDatasetCollection`
  - `sameApiCollection`
- App:
  - `appUnExist`
  - `unAuthApp`
  - `invalidAppType`
- Chat:
  - `unAuthChat`
- OutLink:
  - `outlinkUnExist`
  - `unAuthLink`
  - `linkUnInvalid`
  - `unAuthUser`
- OpenAPI:
  - `openapiUnExist`
  - `openapiUnAuth`
  - `openapiExceedLimit`
- Team:
  - `aiPointsNotEnough`
  - `datasetSizeNotEnough`
  - `appAmountNotEnough`
  - `websiteSyncNotEnough`
  - `reRankNotEnough`

## SSE Error Rules
- `sseErrRes` sends `event: error`.
- Payload mirrors the same normalized error shape or a reduced `{ message }` object.
- Kora must preserve partial answer text when an SSE error arrives after prior chunks.

## Android Mapping Rules
- Keep raw `code` and `statusText` in domain errors for analytics and retry decisions.
- Convert user-visible text at the presentation layer.
- Retry only for idempotent reads and transport failures, never automatically for chat-send or dataset-write operations.

## Related Specs
- [authentication.md](authentication.md)
- [chat-streaming.md](chat-streaming.md)
