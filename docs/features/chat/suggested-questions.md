---
status: implemented
priority: P2
phase: 2-knowledge
---

# Suggested Questions

## Overview
推荐追问依赖 FastGPT `createQuestionGuide`，展示时机参考 Open WebUI 的消息后置操作区。它属于 assistant 消息的附属阅读/继续动作，而不是全局页面模块。

## Functional Requirements
- [ ] FR-1: assistant 完成后按需请求推荐问题列表。
- [ ] FR-2: 每条 assistant 消息可附带一组推荐问题 chips。
- [ ] FR-3: 点击推荐问题后直接触发下一轮发送并把该问题作为新的用户消息。

## Non-Functional Requirements
- [ ] NFR-1: 推荐问题的请求与主回答链路解耦，不得阻塞消息完成展示。
- [ ] NFR-2: 失败或空结果时不占据多余布局。

## API Contract
依赖 [../../api/question-guide.md](../../api/question-guide.md) 和 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
推荐问题展示在 assistant 消息尾部，使用横向可滚动 chips。加载中显示小型 skeleton；空结果直接隐藏；用户点击后 chips 消失并进入新的发送状态。

## Data Model
- `QuestionGuideState(messageDataId, items, status)`
- `SuggestedQuestionItem(text)`

## Architecture Notes
问题推荐绑定到生成完成的 assistant `dataId`。Repository 根据 app 的 `questionGuide.open` 决定是否请求。不要把推荐问题缓存成会话级共享列表。

## Dependencies
- [streaming-chat.md](streaming-chat.md)
- [../../ui/component-catalog.md](../../ui/component-catalog.md)

## Acceptance Criteria
- 回答完成后可正确展示或隐藏推荐问题。
- 点击推荐问题能直接形成下一轮对话。
