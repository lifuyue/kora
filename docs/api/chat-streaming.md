# Chat Streaming

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/workflow/runtime/constants.ts`
  - `.reference/FastGPT/packages/service/common/response/index.ts`
  - `.reference/FastGPT/projects/app/src/web/common/api/fetch.ts`

## SSE Frame Format
FastGPT writes SSE frames as:
```text
event: <eventName>
data: <json-string>

```

## Transport Rules
- Kora must parse both `event:` and `data:` lines.
- Do not treat the stream as plain token text.
- `data: [DONE]` is a sentinel, not JSON.

## Canonical Event Enum
FastGPT defines 16 events in `SseResponseEventEnum`:
- `error`
- `workflowDuration`
- `answer`
- `fastAnswer`
- `flowNodeStatus`
- `flowNodeResponse`
- `toolCall`
- `toolParams`
- `toolResponse`
- `flowResponses`
- `updateVariables`
- `interactive`
- `plan`
- `stepTitle`
- `collectionForm`
- `topAgentConfig`

## Payload Shapes

### `answer`
- Incremental OpenAI-style delta payload.
- Typical fields:
  - `responseValueId?: string`
  - `stepId?: string`
  - `choices[0].delta.content?: string`
  - `choices[0].delta.reasoning_content?: string`
  - `choices[0].finish_reason?: string | null`

### `fastAnswer`
- Same logical payload family as `answer`.
- Web client treats it as non-animated direct text append.

### `flowNodeStatus`
- Small status payload emitted from `responseWriteNodeStatus`.
- Known shape:
```json
{ "status": "running", "name": "node display name" }
```

### `flowNodeResponse`
- Structured node response block for inspection/debug rendering.
- Forward directly into message-side structured response storage.

### `toolCall` / `toolParams` / `toolResponse`
- Payload includes a `tool` object matching `ToolModuleResponseItemType`:
  - `id`
  - `toolName`
  - `toolAvatar`
  - `params`
  - `response`
  - `functionName`

### `flowResponses`
- Final structured node-response array.
- In `detail=true` streaming mode this is the canonical source for citations and flow-result inspection.

### `updateVariables`
- Payload is the new variables object after workflow execution.

### `interactive`
- Payload carries interactive node data; see [chat-interactive.md](chat-interactive.md).

### `plan`
- Agent-plan payload used by assistant planning steps.

### `stepTitle`
- Payload matches `StepTitleItemType`:
  - `stepId`
  - `title`
  - optional `folded`

### `collectionForm`
- Helper-bot collection form payload; treat as an inline form request.

### `topAgentConfig`
- Helper-bot/top-agent form config payload.

### `workflowDuration`
- Duration metadata for the overall workflow run.

### `error`
- JSON error object.
- Common fields observed by web client:
  - `code`
  - `statusText`
  - `message`
  - `data`

## Completion Sequence
- `answer` / `fastAnswer` chunks may interleave with tool and plan events.
- On success, server emits:
  - final `answer` with `finish_reason=stop`
  - `[DONE]`
  - optional `flowResponses` if `detail=true`

## Android Parser Requirements
- Preserve order across all events.
- Separate visible answer text from structured side-channel data.
- Keep `responseValueId` and `stepId` for merging tool/plan/interactive fragments into one assistant message.
- Handle malformed JSON defensively and surface raw frame metadata to logs.

## Related Specs
- [chat-completions.md](chat-completions.md)
- [chat-interactive.md](chat-interactive.md)
- [error-handling.md](error-handling.md)
