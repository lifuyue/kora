# Chat Records

## Snapshot
- Primary sources:
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/init.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/record/getPaginationRecords.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/record/getResData.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/item/delete.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/feedback/updateUserFeedback.ts`

## Endpoints
- `GET /api/core/chat/init`
- `POST /api/core/chat/getPaginationRecords`
- `GET /api/core/chat/getResData`
- `POST /api/core/chat/item/delete`
- `POST /api/core/chat/feedback/updateUserFeedback`

## `init`
Query:
```ts
type InitChatQuery = {
  appId: string;
  chatId?: string;
}
```

Response shape:
```ts
{
  chatId?: string;
  appId: string;
  title?: string;
  userAvatar?: string;
  variables: Record<string, any>;
  app: {
    chatConfig: AppChatConfigType;
    chatModels?: string[];
    name: string;
    avatar: string;
    intro: string;
    type: AppTypeEnum;
    pluginInputs: FlowNodeInputItemType[];
  }
}
```

Semantics:
- Returns latest app chat config, resolved variables, app summary, and plugin inputs.
- Existing `chatId` also returns chat title and stored variables.

## `getPaginationRecords`
Request body:
```ts
type GetPaginationRecordsBody = {
  appId: string;
  chatId: string;
  offset?: number;
  pageSize?: number;
  loadCustomFeedbacks?: boolean;
  type?: 'normal' | 'outLink' | 'team' | 'home';
  shareId?: string;
  outLinkUid?: string;
  teamId?: string;
  teamToken?: string;
}
```

Response:
```ts
{
  list: ChatItemType[];
  total: number;
}
```

Server-side record shaping:
- Adds preview URLs to file values.
- Removes private node response fields in out-link mode.
- Optionally strips citations when `showCite=false`.
- Backfills a per-value `type` discriminator:
  - `text`
  - `file`
  - `tool`
  - `interactive`
  - `reasoning`

## `getResData`
Query:
```ts
type GetResDataQuery = {
  appId: string;
  chatId?: string;
  dataId: string;
  shareId?: string;
  outLinkUid?: string;
}
```

Response:
- `ChatHistoryItemResType[]`
- Empty array when the message is not an AI item or required params are missing.

Semantics:
- Returns full node-response data for one assistant message.
- Falls back from inline `responseData` to `MongoChatItemResponse` rows when needed.

## `item/delete`
Request body:
```ts
type DeleteChatItemProps = {
  appId: string;
  chatId: string;
  contentId: string;
}
```

Semantics:
- Soft deletes one chat item by mapping `contentId -> dataId`.

## `updateUserFeedback`
Request body:
```ts
type UpdateUserFeedbackBody = {
  appId: string;
  chatId: string;
  dataId: string;
  userGoodFeedback?: string | null;
  userBadFeedback?: string | null;
}
```

Response:
```ts
{}
```

Semantics:
- Sets or unsets good/bad feedback on one assistant message.
- Also updates chat-level counters and app analytics counters.

## Android Requirements
- Model assistant records as rich items, not plain text rows.
- Preserve `dataId` because it is reused by `getResData`, feedback, delete, citations, and UI reconciliation.
- Treat `init` as the canonical source of `chatConfig`, `chatModels`, and `welcomeText`-related display state.

## Related Specs
- [chat-completions.md](chat-completions.md)
- [chat-interactive.md](chat-interactive.md)
