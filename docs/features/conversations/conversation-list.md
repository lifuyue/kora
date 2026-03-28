---
status: implemented
priority: P0
phase: 1-mvp
---

# Conversation List

## Overview
会话列表参考 Open WebUI `layout/Sidebar.svelte` 与 `Sidebar/ChatItem.svelte` 的“快速回到上下文”模式，但移动端改为单列列表页。数据源由 FastGPT `getHistories` 提供，客户端再叠加 pin/archive/tag/folder 等本地元数据。

## Functional Requirements
- [ ] FR-1: 首屏分页加载会话摘要，支持下拉刷新和上拉加载更多。
- [ ] FR-2: 支持标题关键词搜索和本地即时筛选。
- [ ] FR-3: 支持按 `top`、更新时间和本地 pin/archive 状态组合排序。

## Non-Functional Requirements
- [ ] NFR-1: 列表滚动稳定，刷新后不跳动。
- [ ] NFR-2: 搜索输入有防抖，且在弱网下保留上一屏结果。

## API Contract
依赖 [../../api/chat-history.md](../../api/chat-history.md)。

## UI Description
页面顶部为搜索框和过滤入口，下方为分组列表：置顶、本地固定分组、普通会话。列表项展示标题、更新时间、最近摘要和轻量状态标记。空列表显示新建会话 CTA；错误时保留上次成功列表并展示重试。

## Data Model
- `ConversationListUiState(items, query, loadingState, filter)`
- `ConversationListItem(chatId, title, customTitle, updateTime, source, preview, isPinned, isArchived, tags, folderId)`
- `ConversationFilter(showArchived, folderId?, tagId?)`

## Architecture Notes
Remote history 与 local extensions 通过 repository merge。列表页只消费合并结果，不直接知道 FastGPT / Room 差异。打开某条会话仅传递 `chatId` 与 `appId`。

## Dependencies
- [conversation-crud.md](conversation-crud.md)
- [../../architecture/local-storage.md](../../architecture/local-storage.md)

## Acceptance Criteria
- 列表加载、搜索、分页和打开会话四条路径都可验证。
- 排序与分组结果稳定一致。
