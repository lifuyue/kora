---
status: planned
priority: P1
phase: 2-knowledge
---

# Interactive Nodes

## Overview
FastGPT 的交互节点是工作流的一等公民。Kora 必须把 `interactive`、`collectionForm` 等事件渲染为消息内联交互卡，而不是跳到独立表单页。

## Functional Requirements
- [ ] FR-1: 支持按钮选择型交互节点渲染和提交。
- [ ] FR-2: 支持文本或表单型交互节点渲染、校验、提交。
- [ ] FR-3: 节点提交后，原卡片转为只读历史态并继续追加后续回复。
- [ ] FR-4: 未完成节点在进程重建后可恢复。

## Non-Functional Requirements
- [ ] NFR-1: 节点与所属 assistant 消息的绑定必须稳定，不能串到其他会话。
- [ ] NFR-2: 表单草稿和提交状态在切后台后保持。

## API Contract
依赖 [../../api/chat-interactive.md](../../api/chat-interactive.md) 和 [../../api/chat-streaming.md](../../api/chat-streaming.md)。

## UI Description
交互节点显示在 assistant 消息内部。按钮型交互使用 chips 或分组按钮；表单型交互使用消息内嵌表单卡或底部弹层。提交中锁定当前节点，完成后展示用户选择结果和后续流。

## Data Model
- `InteractiveCardUiModel(kind, fields, options, status, messageDataId, responseValueId)`
- `InteractiveDraft(fieldValues, validationErrors)`
- `InteractiveResumeSnapshot(chatId, messageDataId, rawPayload)`

## Architecture Notes
交互解析由 Repository 统一做 schema normalize，UI 只面向 `InteractiveCardUiModel`。与普通消息一样参与 Room 缓存，但草稿和 pending 状态是本地补充字段。

## Dependencies
- [streaming-chat.md](streaming-chat.md)
- [../../architecture/local-storage.md](../../architecture/local-storage.md)

## Acceptance Criteria
- 选择型和输入型交互都可完成一次闭环。
- 杀进程后重开仍能看到未完成交互并继续提交。
