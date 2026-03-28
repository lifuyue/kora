---
status: implemented
priority: P2
phase: 2-knowledge
---

# Message Feedback

## Overview
允许用户对 assistant 消息点赞或点踩，并调用 FastGPT `updateUserFeedback`。表现参考 Open WebUI 的反馈交互，但移动端以 icon toggle + 轻量提示实现。

## Functional Requirements
- [ ] FR-1: assistant 消息展示点赞、点踩两个反馈入口。
- [ ] FR-2: 用户可设置、覆盖或取消反馈。
- [ ] FR-3: 页面重进后反馈状态由消息记录恢复。

## Non-Functional Requirements
- [ ] NFR-1: 反馈提交不阻塞消息阅读或滚动。
- [ ] NFR-2: 失败时回退到上一个稳定状态并提供重试提示。

## API Contract
依赖 [../../api/chat-records.md](../../api/chat-records.md) 中的 `updateUserFeedback`。

## UI Description
反馈按钮位于 assistant 消息尾部操作区。已选中状态要明显可见；二次点击取消当前反馈；用户在弱网下点击后先本地 optimistic 更新，失败时回滚并 toast。

## Data Model
- `MessageFeedbackUiState(dataId, goodText?, badText?, pending)`
- `FeedbackIntent(type=good|bad|clearGood|clearBad)`

## Architecture Notes
反馈是消息级附属状态，必须挂在 `dataId` 之上，不允许只按列表位置索引。Repository 负责把 optimistic state 与远端响应对齐。

## Dependencies
- [../../api/error-handling.md](../../api/error-handling.md)
- [message-actions.md](message-actions.md)

## Acceptance Criteria
- 点赞、点踩、取消都能成功提交并恢复。
- 刷新记录后状态一致。
