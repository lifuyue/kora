# App Management

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/app/constants.ts`
  - `.reference/FastGPT/packages/global/core/app/type.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/app/list.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/init.ts`

## `AppTypeEnum`
- `folder`
- `toolFolder`
- `simple`
- `chatAgent`
- `advanced` mapped from `workflow`
- `plugin` mapped from `workflowTool`
- `toolSet` mapped from `mcpToolSet`
- `httpToolSet`
- `hidden`
- deprecated: `tool`, `httpPlugin`

## `AppChatConfigType`
```ts
type AppChatConfigType = {
  welcomeText?: string;
  variables?: VariableItemType[];
  autoExecute?: { open: boolean; defaultPrompt: string };
  questionGuide?: { open: boolean; model?: string; customPrompt?: string };
  ttsConfig?: { type: 'none' | 'web' | 'model'; model?: string; voice?: string; speed?: number };
  whisperConfig?: { open: boolean; autoSend: boolean; autoTTSResponse: boolean };
  scheduledTriggerConfig?: { cronString: string; timezone: string; defaultPrompt: string };
  chatInputGuide?: { open: boolean; customUrl: string };
  fileSelectConfig?: AppFileSelectConfigType;
  instruction?: string;
}
```

## `AppSchemaType`
Core fields:
- `_id`
- `parentId?`
- `teamId`
- `tmbId`
- `type`
- `version?: 'v1' | 'v2'`
- `name`
- `avatar`
- `intro`
- `templateId?`
- `updateTime`
- `modules`
- `edges`
- `pluginData?`
- `chatConfig`
- `scheduledTriggerConfig?`
- `scheduledTriggerNextTime?`
- `inheritPermission?`
- `favourite?`
- `quick?`
- `deleteTime?`

## App List Surface
`/api/core/app/list` returns `AppListItemType[]` with:
- `_id`
- `parentId`
- `tmbId`
- `name`
- `avatar`
- `intro`
- `type`
- `updateTime`
- `pluginData?`
- `permission`
- `inheritPermission?`
- `private?`
- `sourceMember`
- `hasInteractiveNode?`

## Android Requirements
- Cache the selected app id locally, but always refresh app detail before entering chat.
- Use `chatConfig` as the canonical source for welcome text, variable forms, file selection, TTS, whisper, and suggested-question behavior.
- Distinguish `chatAgent`, `simple`, and workflow-tool-like app types in the UI because they change entry behavior.

## Related Specs
- [chat-records.md](chat-records.md)
- [question-guide.md](question-guide.md)
