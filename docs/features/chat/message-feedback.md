---
status: draft
priority: P2
phase: 2-knowledge
---

# Message Feedback

## Overview
允许用户对回答做点赞或点踩，用于记录满意度并为后续模型调优或分析留出扩展位。

## Functional Requirements
- [ ] FR-1: assistant 消息可标记点赞、点踩和取消反馈。
- [ ] FR-2: 已反馈状态在消息重新进入页面时可恢复。
- [ ] FR-3: 反馈失败时允许重试或回退为本地状态。

## Non-Functional Requirements
- [ ] NFR-1: 反馈操作应为轻量异步，不阻塞消息阅读。
- [ ] NFR-2: 本地与远端反馈状态冲突时需要可解释的合并策略。

## API Contract
当前以消息标识为基础，依赖 [../../api/chat-records.md](../../api/chat-records.md) 获取 messageId；若后端补充专用反馈接口，再扩展对接。

## UI Description
assistant 消息底部显示点赞和点踩图标；点击后即时高亮并提交；失败时展示轻量 toast 或 inline error，不打断主阅读流。

## Data Model
- `MessageFeedbackState`
- `FeedbackPendingAction`

## Architecture Notes
反馈状态可先由 `:feature:chat` + Room 托管，远端接口成熟后再通过 repository 同步。

## Dependencies
- 消息记录模型
- 本地持久化
- 可选统计能力

## Acceptance Criteria
- 点赞、点踩、取消反馈三种状态切换正确。
- 列表滚动回收后反馈显示保持一致。

