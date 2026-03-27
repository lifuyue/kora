# Chat Completions

## Endpoint
- `POST /api/v1/chat/completions`

## Snapshot
- Primary sources:
  - `.reference/FastGPT/projects/app/src/pages/api/v1/chat/completions.ts`
  - `.reference/FastGPT/packages/global/core/ai/type.ts`
  - `.reference/FastGPT/projects/app/src/web/common/api/fetch.ts`

## Purpose
- Single entrypoint for normal chat, workflow-tool execution, share chat, team-space chat, interactive continuation, citations, tool traces, and variable updates.

## Request Body
```ts
type Props = ChatCompletionCreateParams & {
  chatId?: string;
  appId?: string;
  customUid?: string;
  shareId?: string;
  outLinkUid?: string;
  teamId?: string;
  teamToken?: string;
  messages: ChatCompletionMessageParam[];
  responseChatItemId?: string;
  stream?: boolean;
  detail?: boolean;
  retainDatasetCite?: boolean;
  variables: Record<string, any>;
  metadata?: Record<string, any>;
}
```

## Key Request Semantics
- `chatId`
  - `undefined`: server derives history from request messages.
  - `""` or omitted in UI flows: start a new persisted chat.
  - non-empty string: continue an existing chat and load history from DB.
- `appId`
  Required for app-bound chat and workflow-tool execution.
- `messages`
  Must be an array of `ChatCompletionMessageParam`. Non-plugin chat requires at least one user message.
- `stream`
  `true` returns SSE.
- `detail`
  `true` returns structured event payloads and richer non-stream responses. Kora should default this to `true`.
- `responseChatItemId`
  Client-generated assistant message id. Defaults server-side to `getNanoid()`.
- `variables`
  Global variables or workflow-tool/plugin inputs. Persisted chat variables are merged first, request variables override them.
- `retainDatasetCite`
  Effective only when the auth context allows citations.

## `ChatCompletionMessageParam`
FastGPT extends OpenAI's message type:
```ts
type ChatCompletionContentPartFile = {
  type: 'file_url';
  name: string;
  url: string;
  key?: string;
}

type ChatCompletionMessageParam = OpenAIMessageLike & {
  reasoning_content?: string;
  dataId?: string;
  hideInUI?: boolean;
}
```

## Extended Message Capabilities
- User `content` may be:
  - plain string
  - array of content parts including `file_url`
- Assistant messages may carry:
  - `interactive`
  - `reasoning_content`
  - tool calls / function call data inherited from OpenAI SDK types
- Tool messages may include optional `name`

## Auth Modes
- Bearer API key
- Logged-in token/cookie
- `shareId + outLinkUid`
- `teamId + teamToken`

## Stream Response
- SSE events follow [chat-streaming.md](chat-streaming.md).
- Terminal flow:
  - server emits normal events
  - emits final `answer` event with `finish_reason=stop`
  - emits `data: [DONE]`
  - if `detail=true`, emits final `flowResponses` event after `[DONE]`

## Non-Stream Response Modes
There are four practical shapes determined by `stream x detail`:

### `stream=false`, `detail=false`
```json
{
  "error": null,
  "id": "chatId-or-empty",
  "model": "",
  "usage": { "prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 1 },
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "joined assistant text",
        "reasoning_content": "joined reasoning text"
      },
      "finish_reason": "stop",
      "index": 0
    }
  ]
}
```

### `stream=false`, `detail=true`
- Same outer shape as above.
- Adds:
  - `responseData`
  - `newVariables`
- `choices[0].message.content` may become an array of typed assistant value items instead of a single string.

### `stream=true`, `detail=false`
- SSE text stream optimized for answer text only.
- Kora still parses `event:` boundaries because answer chunks are OpenAI-like JSON payloads.

### `stream=true`, `detail=true`
- Full SSE event bus.
- Required for tool states, citations, flow node results, variables, interactive nodes, plans, helper-bot forms.

## Server-Side Persistence Semantics
- Server creates `saveChatId = chatId || getNanoid(24)`.
- Source is derived as:
  - `share`
  - `api`
  - `team`
  - `online`
- New title comes from latest user message for normal chat, or derived execution time for workflow tools.

## Client Requirements
- Use `detail=true` for all Kora chat traffic.
- Preserve `responseChatItemId` locally so streamed assistant state maps to one local message.
- Do not assume `choices[0].message.content` is always a string.
- Persist chat only after receiving a non-empty resolved `chatId`.
- Treat `newVariables` as authoritative app/session variable updates after completion.

## Related Specs
- [chat-streaming.md](chat-streaming.md)
- [chat-interactive.md](chat-interactive.md)
- [chat-records.md](chat-records.md)
