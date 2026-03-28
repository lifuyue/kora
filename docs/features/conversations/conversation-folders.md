---
status: implemented
priority: P2
phase: 2-knowledge
---

# Conversation Folders

## Overview
文件夹是客户端本地组织能力，用于按项目/主题聚合会话，不改变 FastGPT 服务端历史结构。

## Functional Requirements
- [ ] FR-1: 用户可创建、重命名、删除文件夹。
- [ ] FR-2: 会话可移动到某个文件夹，或移出文件夹。
- [ ] FR-3: 会话列表支持按文件夹过滤。

## Non-Functional Requirements
- [ ] NFR-1: 文件夹删除前必须提示是否保留其中会话。
- [ ] NFR-2: 批量移动操作不卡顿且可回滚。

## API Contract
无专属远端接口；基础会话数据仍来自 [../../api/chat-history.md](../../api/chat-history.md)。

## UI Description
会话列表顶部过滤器支持切换文件夹。编辑文件夹和移动会话使用底部 sheet。删除文件夹时可选“仅删除文件夹”或“连同本地组织关系一并清除”。

## Data Model
- `ConversationFolderEntity(folderId, name, order)`
- `ConversationFolderCrossRef(chatId, folderId)`

## Architecture Notes
文件夹关系完全本地化，由 Room 管理；Repository 在列表 merge 时把 folder 信息挂入 `ConversationListItem`。

## Dependencies
- [conversation-list.md](conversation-list.md)
- [../../architecture/local-storage.md](../../architecture/local-storage.md)

## Acceptance Criteria
- 文件夹 CRUD、移动会话、按文件夹过滤都可验证。
