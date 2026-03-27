---
status: draft
priority: P0
phase: 1-mvp
---

# Streaming Chat

## Overview
提供 Kora 最核心的流式聊天体验，让用户在移动端以低等待感接收模型增量回复，并在必要时继续生成、取消或重试。

## Functional Requirements
- [ ] FR-1: 用户发送消息后，界面立即展示本地占位的 assistant 气泡。
- [ ] FR-2: 客户端按 SSE 增量更新回复文本、工具状态和结束状态。
- [ ] FR-3: 失败后允许重新生成或从中断位置继续生成。

## Non-Functional Requirements
- [ ] NFR-1: 首个可见 token 应尽快出现，避免长时间空白等待。
- [ ] NFR-2: 流式解析异常不能导致页面崩溃或已收内容丢失。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md) 和 [../../api/chat-streaming.md](../../api/chat-streaming.md)。

## UI Description
聊天页底部输入框发送后，消息列表追加用户消息与 assistant 占位消息；占位消息持续增长，工具调用与错误状态以内联状态条展示；加载、空会话、断流错误和成功完成状态都需可区分。

## Data Model
- `ChatSession`
- `MessageItem`
- `StreamingMessageState`
- `PendingSendDraft`

## Architecture Notes
落在 `:feature:chat`，由 ViewModel 收集流式事件，repository 协调网络与本地落盘，遵循 [../../architecture/data-flow.md](../../architecture/data-flow.md)。

## Dependencies
- Compose LazyColumn
- Coroutines / Flow
- OkHttp SSE
- 本地消息缓存

## Acceptance Criteria
- 可成功发送新消息并流式看到回复。
- 中断、超时和服务端错误能被用户识别并重试。

