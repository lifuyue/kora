# Chat Interactive

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/chat/type.ts`
  - `.reference/FastGPT/packages/global/core/ai/type.ts`
  - `.reference/FastGPT/projects/app/src/web/common/api/fetch.ts`

## Purpose
- Represent workflow checkpoints that require user action before the server can continue.

## Where Interactive State Appears
- Assistant chat values:
  `AIChatItemValueItemType.interactive`
- Streaming SSE:
  `event=interactive`
- Assistant OpenAI-like messages:
  `ChatCompletionMessageParam.interactive`

## Canonical Client Model
Kora should normalize interactive payloads into:
```ts
type InteractiveMessageState = {
  chatId: string;
  messageDataId: string;
  responseValueId?: string;
  stepId?: string;
  kind: 'userSelect' | 'userInput' | 'collectionForm';
  raw: JsonObject;
  status: 'pending' | 'submitting' | 'resolved' | 'expired';
}
```

## `userSelect`
- Server provides a finite set of options.
- Mobile UI should render inline chips or a bottom sheet action list.
- Submission must preserve the owning `chatId`, assistant `dataId`, and current variable context.

## `userInput`
- Server requests free text or structured form input.
- Kora should present a focused input affordance near the assistant message, not a detached page.

## Recovery Rules
- Interactive nodes are part of chat history, not ephemeral UI.
- After process death or app restart, rebuild pending interactive state from:
  - paginated chat records
  - assistant `value[].interactive`
  - latest unresolved local submission state

## Kora Decisions
- Keep one unresolved interactive node active per assistant message block.
- Disable normal "send new unrelated prompt" CTA only when the active app flow explicitly requires the user to continue the interaction.
- Persist pending form drafts locally until success or explicit cancel.

## Related Specs
- [chat-completions.md](chat-completions.md)
- [chat-streaming.md](chat-streaming.md)
