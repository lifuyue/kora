# Chat History

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/openapi/core/chat/history/api.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/history/getHistories.ts`
  - `.reference/FastGPT/projects/app/src/web/core/chat/history/api.ts`

## Endpoints
- `POST /api/core/chat/history/getHistories`
- `PUT /api/core/chat/history/updateHistory`
- `DELETE /api/core/chat/history/delHistory`
- `DELETE /api/core/chat/history/clearHistories`

## `getHistories`
Request body:
```ts
type GetHistoriesBody = {
  offset?: number;
  pageSize?: number;
  appId?: string;
  shareId?: string;
  outLinkUid?: string;
  teamId?: string;
  teamToken?: string;
  source?: ChatSourceEnum;
  startCreateTime?: string;
  endCreateTime?: string;
  startUpdateTime?: string;
  endUpdateTime?: string;
}
```

Response:
```ts
type GetHistoriesResponse = {
  list: Array<{
    chatId: string;
    updateTime: Date;
    appId: string;
    customTitle?: string;
    title: string;
    top?: boolean;
  }>;
  total: number;
}
```

Server behavior:
- Share mode filters by `shareId + parsed outLinkUid` and only returns roughly the last month.
- Team-space mode filters by `appId + outLinkUid + source=team`.
- Normal token/API-key mode filters by `appId + tmbId`, optionally by `source`.
- Sorting is fixed: `top DESC`, then `updateTime DESC`.

## `updateHistory`
Request body:
```ts
type UpdateHistoryBody = {
  appId: string;
  chatId: string;
  shareId?: string;
  outLinkUid?: string;
  title?: string;
  customTitle?: string;
  top?: boolean;
}
```

Semantics:
- Mutates `title`, `customTitle`, `top`.
- Always refreshes `updateTime`.

## `delHistory`
Query params:
```ts
type DelChatHistory = {
  appId: string;
  chatId: string;
  shareId?: string;
  outLinkUid?: string;
}
```

Semantics:
- Soft delete only.
- Sets `deleteTime`.

## `clearHistories`
Query params:
```ts
type ClearChatHistories = {
  appId: string;
  shareId?: string;
  outLinkUid?: string;
}
```

Semantics:
- Share mode clears chats for `appId + outLinkUid`.
- Team-space mode clears chats for `appId + outLinkUid`.
- Token mode clears `appId + tmbId + source=online`.
- API-key mode clears `appId + source=api`.

## Android Requirements
- Use server pagination as source of truth.
- Keep local-only metadata such as archive/tag/folder separate from server history fields.
- On delete/clear, mark local rows soft-deleted immediately and reconcile after network success.

## Related Specs
- [chat-records.md](chat-records.md)
- [authentication.md](authentication.md)
