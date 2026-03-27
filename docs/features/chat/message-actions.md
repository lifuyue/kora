---
status: draft
priority: P1
phase: 1-mvp
---

# Message Actions

## Overview
为消息提供复制、编辑重发、重新生成和继续生成操作，降低用户修正输入与迭代追问的成本。

## Functional Requirements
- [ ] FR-1: 用户可复制任意消息纯文本内容。
- [ ] FR-2: 用户可将历史用户消息载回输入框并再次发送。
- [ ] FR-3: assistant 消息支持重新生成和继续生成。

## Non-Functional Requirements
- [ ] NFR-1: 重发和重新生成必须保持所属会话上下文正确。
- [ ] NFR-2: 长按菜单与底部操作不能影响消息列表滚动性能。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md) 与 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
长按消息或点击操作按钮弹出菜单；用户消息展示复制与编辑重发，assistant 消息展示复制、重新生成和继续生成；网络进行中时禁用冲突操作。

## Data Model
- `MessageAction`
- `RegenerateRequest`
- `ContinueGenerationContext`

## Architecture Notes
操作入口属于 `:feature:chat` UI，具体行为通过 ViewModel 转为意图并复用聊天发送链路，避免出现多条并行发送状态。

## Dependencies
- 聊天会话状态
- 剪贴板
- 流式聊天能力

## Acceptance Criteria
- 用户可稳定复制、编辑重发和重新生成。
- 继续生成不会破坏已有消息顺序和本地历史。

