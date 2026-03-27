---
status: draft
priority: P2
phase: 2-knowledge
---

# Conversation Pin

## Overview
支持用户置顶重要会话，便于长期项目或常用机器人快速访问。

## Functional Requirements
- [ ] FR-1: 用户可置顶和取消置顶会话。
- [ ] FR-2: 置顶会话在列表顶部独立排序。
- [ ] FR-3: 置顶状态在重启应用后保持。

## Non-Functional Requirements
- [ ] NFR-1: 置顶不应改变远端原始更新时间语义。
- [ ] NFR-2: 大量置顶项时列表仍需可用。

## API Contract
当前为客户端扩展能力，基础会话数据来自 [../../api/chat-history.md](../../api/chat-history.md)。

## UI Description
列表项更多菜单提供“置顶/取消置顶”；置顶组显示在普通会话上方，可有独立标题；空置顶组时不展示分组头。

## Data Model
- `PinnedConversationEntity`
- `PinOrder`

## Architecture Notes
置顶仅存本地 Room，不写回服务端；展示层将本地置顶信息与远端会话摘要做合并排序。

## Dependencies
- 会话列表
- Room

## Acceptance Criteria
- 置顶和取消置顶后列表顺序即时更新。
- 重新打开应用后置顶状态仍然正确。

