---
status: draft
priority: P1
phase: 2-knowledge
---

# Interactive Nodes

## Overview
支持 FastGPT 工作流中的交互节点，让用户在聊天流内通过按钮选择或表单输入继续流程。

## Functional Requirements
- [ ] FR-1: 支持 `userSelect` 节点的按钮选项渲染和回传。
- [ ] FR-2: 支持 `userInput` 节点的表单或文本输入。
- [ ] FR-3: 未完成交互节点可在页面恢复时继续。

## Non-Functional Requirements
- [ ] NFR-1: 交互状态必须与所属消息严格绑定，避免串会话。
- [ ] NFR-2: 节点恢复要兼容旋转、进程回收和后台切换。

## API Contract
依赖 [../../api/chat-interactive.md](../../api/chat-interactive.md) 和 [../../api/chat-streaming.md](../../api/chat-streaming.md)。

## UI Description
交互节点以内联卡片出现在消息流中；按钮选择走单击确认，表单输入支持校验；提交后节点变为只读历史状态，并继续显示后续流式消息。

## Data Model
- `InteractiveNodeUiModel`
- `InteractiveSubmission`
- `PendingInteractiveContext`

## Architecture Notes
节点解析由流式层产出统一模型，`:feature:chat` 渲染并提交，避免 UI 直接依赖原始 SSE payload。

## Dependencies
- 流式聊天
- 表单状态管理
- 本地恢复能力

## Acceptance Criteria
- 两类交互节点都可完成提交流程。
- 提交后聊天可继续推进且历史可追溯。

