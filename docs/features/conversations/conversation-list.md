---
status: draft
priority: P0
phase: 1-mvp
---

# Conversation List

## Overview
提供会话列表浏览入口，支持分页、搜索和排序，让用户快速找到已有对话。

## Functional Requirements
- [ ] FR-1: 支持会话分页加载与下拉刷新。
- [ ] FR-2: 支持按标题关键词搜索。
- [ ] FR-3: 支持按最近更新时间排序。

## Non-Functional Requirements
- [ ] NFR-1: 长列表滚动要稳定，避免频繁重组。
- [ ] NFR-2: 搜索输入应有合理防抖，减少多余请求。

## API Contract
依赖 [../../api/chat-history.md](../../api/chat-history.md)。

## UI Description
会话首页展示列表、搜索框和排序入口；初次加载显示骨架，空状态提供新建会话 CTA，错误状态支持重试；分页到底部自动加载更多。

## Data Model
- `ConversationSummary`
- `ConversationListState`
- `ConversationSearchQuery`

## Architecture Notes
列表数据由 `:feature:chat` 或独立 conversations 子域承载，Room 可缓存最近结果并叠加本地扩展字段。
Open WebUI 的会话组织方式说明列表的首要任务是帮助用户快速回到上下文，而不是承担复杂工作台职责；因此 Kora 的列表优先保留摘要、搜索、排序和快速恢复，扩展组织能力再用本地元数据叠加。参考 [../../reference/open-webui-implementation-patterns.md](../../reference/open-webui-implementation-patterns.md)。

## Dependencies
- 会话历史接口
- Room 缓存
- Compose paging 或自实现分页

## Acceptance Criteria
- 用户可稳定浏览、搜索和打开会话。
- 列表刷新和分页不会打乱已显示顺序。
