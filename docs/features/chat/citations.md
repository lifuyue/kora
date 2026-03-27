---
status: draft
priority: P1
phase: 2-knowledge
---

# Citations

## Overview
展示 RAG 回答引用来源，帮助用户理解答案依据并快速回看命中文档片段。

## Functional Requirements
- [ ] FR-1: 当响应包含引用时，消息展示引用入口和数量。
- [ ] FR-2: 用户可展开引用面板查看来源标题、片段和元信息。
- [ ] FR-3: 支持跳转到对应知识库或文档上下文。

## Non-Functional Requirements
- [ ] NFR-1: 引用面板展开和收起要流畅，不阻塞主列表。
- [ ] NFR-2: 缺失引用详情时需有明确降级展示。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md) 与 [../../api/chat-records.md](../../api/chat-records.md)；请求时默认启用 `detail=true`。

## UI Description
assistant 消息底部出现“引用来源”入口；点击后弹出底部面板或内联展开区域，列出来源文档、命中片段和相似度信息；无引用时不展示入口。

## Data Model
- `CitationItem`
- `CitationPanelState`

## Architecture Notes
引用信息由聊天响应直接进入消息模型，不额外请求时也能渲染；复杂详情可通过记录补拉接口补足。

## Dependencies
- 流式聊天或消息详情
- 知识库实体映射

## Acceptance Criteria
- 有引用的回答可以展开查看来源。
- 无引用的回答不会出现误导性 UI。

