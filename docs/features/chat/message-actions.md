---
status: draft
priority: P1
phase: 1-mvp
---

# Message Actions

## Overview
消息操作参考 Open WebUI `Messages/ResponseMessage.svelte` 的消息菜单，但移动端改为长按菜单和底部 action sheet。目标是支持复制、重发、重新生成、继续生成和辅助查看操作。

## Functional Requirements
- [ ] FR-1: 用户消息支持复制和“编辑后重发”。
- [ ] FR-2: assistant 消息支持复制、重新生成、继续生成。
- [ ] FR-3: 当消息存在引用、完整节点响应或语音时，菜单展示相应入口。

## Non-Functional Requirements
- [ ] NFR-1: 菜单打开和关闭不打断列表滚动位置。
- [ ] NFR-2: 冲突操作在生成中被正确禁用，例如同一会话已有活跃流时不允许再次重新生成。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md) 和 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
长按消息或点击消息尾部更多按钮，弹出底部菜单。用户消息显示“复制”“编辑重发”；assistant 消息显示“复制”“重新生成”“继续生成”“查看引用”“查看完整响应”。危险或不可用操作显示 disabled 说明。

## Data Model
- `MessageActionSheetState(targetMessageId, actions)`
- `ReplayMessageDraft(originalMessageId, text, attachments)`
- `RegenerateContext(chatId, appId, anchorAssistantId)`

## Architecture Notes
重新生成和继续生成不新增独立网络协议，仍复用 chat completions，只是由 ViewModel 重组 `messages` 和 `chatId`。动作入口在 UI，动作执行在 ViewModel / Repository。

## Dependencies
- [../../ui/component-catalog.md](../../ui/component-catalog.md)
- [streaming-chat.md](streaming-chat.md)

## Acceptance Criteria
- 复制、编辑重发、重新生成、继续生成四个主动作都可验证。
- 生成中不会触发冲突请求。
