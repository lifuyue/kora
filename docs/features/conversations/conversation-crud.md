---
status: draft
priority: P0
phase: 1-mvp
---

# Conversation CRUD

## Overview
会话 CRUD 覆盖新建、重命名、删除和清空。服务端能力来自 `chat/completions`、`updateHistory`、`delHistory`、`clearHistories`，本地还要同步清理消息缓存和扩展元数据。

## Functional Requirements
- [ ] FR-1: 从列表页或聊天页创建新会话，并立即进入新会话上下文。
- [ ] FR-2: 支持修改 `title` / `customTitle`。
- [ ] FR-3: 支持删除单条会话和清空某 app 下的会话历史。

## Non-Functional Requirements
- [ ] NFR-1: 删除和清空必须二次确认。
- [ ] NFR-2: 删除成功后本地 Room 与当前 UI 同步清空，不出现幽灵会话。

## API Contract
依赖 [../../api/chat-history.md](../../api/chat-history.md) 和 [../../api/chat-completions.md](../../api/chat-completions.md)。

## UI Description
列表页提供 FAB 或顶部 CTA 新建聊天；列表项更多菜单中提供重命名和删除；清空历史位于 app 级菜单。危险操作使用底部确认弹层，成功后列表即时刷新。

## Data Model
- `ConversationEditDialogState(chatId?, currentTitle, pending)`
- `DeleteConversationRequest(chatId)` 
- `ClearHistoriesRequest(appId)`

## Architecture Notes
“新建会话”本质上是创建新的聊天上下文并在首次发送后拿到正式 `chatId`。删除/清空后同时清理 `MessageEntity`、`ConversationEntity` 及本地 tag/pin/folder/archive 关系。

## Dependencies
- [conversation-list.md](conversation-list.md)
- [../../api/chat-records.md](../../api/chat-records.md)

## Acceptance Criteria
- 新建、重命名、删除、清空都可稳定验证。
- 删除后的会话不会被本地缓存重新回显。
