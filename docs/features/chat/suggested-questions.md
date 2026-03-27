---
status: draft
priority: P2
phase: 2-knowledge
---

# Suggested Questions

## Overview
在回答完成后展示推荐追问，帮助用户快速继续探索当前主题。

## Functional Requirements
- [ ] FR-1: 回答完成后可请求或展示推荐问题列表。
- [ ] FR-2: 点击推荐问题后直接填充并发送。
- [ ] FR-3: 支持用户关闭当前轮推荐问题。

## Non-Functional Requirements
- [ ] NFR-1: 推荐问题出现不能影响主回答渲染完成。
- [ ] NFR-2: 多轮连续使用时需要避免重复度过高。

## API Contract
依赖 [../../api/question-guide.md](../../api/question-guide.md) 与 [../../api/chat-completions.md](../../api/chat-completions.md)。

## UI Description
推荐问题以横向卡片或纵向 chips 展示在 assistant 消息尾部；加载中显示骨架，空结果则不占位；点击后触发一次新的用户消息发送。

## Data Model
- `SuggestedQuestionItem`
- `SuggestedQuestionState`

## Architecture Notes
推荐问题是聊天页附属能力，状态应绑定到消息而不是全局页面，避免滚动过程中错位。

## Dependencies
- 聊天消息模型
- 推荐问题接口

## Acceptance Criteria
- 回答成功后可看到推荐问题。
- 点击推荐问题后能正确进入下一轮对话。

