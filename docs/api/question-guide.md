# Question Guide

## Endpoint
- `POST /api/core/ai/agent/v2/createQuestionGuide`

## Snapshot
- Primary sources:
  - `.reference/FastGPT/projects/app/src/pages/api/core/ai/agent/v2/createQuestionGuide.ts`
  - `.reference/FastGPT/packages/service/core/ai/functions/createQuestionGuide.ts`
  - `.reference/FastGPT/packages/global/core/app/type.ts`

## Request Body
```ts
type CreateQuestionGuideParams = {
  appId: string;
  chatId: string;
  shareId?: string;
  outLinkUid?: string;
  questionGuide?: {
    open: boolean;
    model?: string;
    customPrompt?: string;
  };
}
```

## Server Behavior
- Auths the current chat context.
- Uses explicit `questionGuide` from request when provided.
- Otherwise resolves `chatConfig.questionGuide` from the app's latest version.
- Loads the latest `6` history items and converts them into GPT messages.
- Uses `questionGuide.model` or the default LLM model.
- Returns `string[]`.

## Android Requirements
- Display question guides only after the assistant answer reaches a terminal state.
- Cache them as message-affiliated UI data, not as standalone conversation state.
- Hide the section when the app config sets `questionGuide.open=false`.

## Related Specs
- [app-management.md](app-management.md)
- [chat-completions.md](chat-completions.md)
