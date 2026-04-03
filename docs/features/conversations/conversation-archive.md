---
status: implemented
priority: P2
phase: 2-knowledge
---

# Conversation Archive

## Overview
归档用于把低频会话从主列表移出，但不删除远端历史。

## Functional Requirements
- [ ] FR-1: 用户可归档和取消归档会话。
- [ ] FR-2: 主列表默认不显示归档项。
- [ ] FR-3: 提供单独归档过滤视图。

## Non-Functional Requirements
- [ ] NFR-1: 归档应为本地即时操作。
- [ ] NFR-2: 归档状态重启后保持。

## API Contract
无专属远端接口；基础会话依赖 [../../api/chat-history.md](../../api/chat-history.md)。

## UI Description
会话项菜单提供“归档/取消归档”。过滤器可切换到归档视图。归档会话默认不参与主列表搜索结果，除非显式进入归档视图。

## Data Model
- `ArchivedConversationEntity(chatId, archivedAt)`

## Architecture Notes
归档与删除完全分离。Repository 只在列表 merge 层过滤，不影响消息数据存在。

## Dependencies
- [conversation-list.md](conversation-list.md)

## Acceptance Criteria
- 归档与取消归档均可验证，且不会误删会话数据。
