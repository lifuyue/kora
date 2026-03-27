---
status: draft
priority: P0
phase: 1-mvp
---

# Conversation CRUD

## Overview
支持新建、重命名、删除和清空会话，是聊天历史管理的基础能力。

## Functional Requirements
- [ ] FR-1: 用户可创建新会话并立即进入聊天页。
- [ ] FR-2: 用户可重命名和删除单个会话。
- [ ] FR-3: 用户可清空某应用下的历史会话。

## Non-Functional Requirements
- [ ] NFR-1: 删除和清空操作必须有确认步骤。
- [ ] NFR-2: 本地与远端删除状态要保持一致或可恢复。

## API Contract
依赖 [../../api/chat-history.md](../../api/chat-history.md) 与 [../../api/chat-completions.md](../../api/chat-completions.md)。

## UI Description
列表项支持更多菜单；新建会话可通过浮动按钮或输入框启动；重命名使用对话框；删除和清空显示危险操作确认。

## Data Model
- `ConversationEditState`
- `ConversationDeleteAction`

## Architecture Notes
会话创建和消息发送共享会话初始化逻辑；删除后需同步清理本地消息表和扩展元数据。

## Dependencies
- 会话列表
- 历史接口
- 本地数据库

## Acceptance Criteria
- 新建、重命名、删除和清空路径都可验证。
- 删除后的会话不会在本地残留可见记录。

