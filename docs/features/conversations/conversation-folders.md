---
status: draft
priority: P2
phase: 2-knowledge
---

# Conversation Folders

## Overview
通过客户端 Room 实现文件夹分组，让用户能按项目或主题组织大量会话。

## Functional Requirements
- [ ] FR-1: 用户可创建、重命名和删除文件夹。
- [ ] FR-2: 会话可移动到指定文件夹。
- [ ] FR-3: 列表支持按文件夹筛选。

## Non-Functional Requirements
- [ ] NFR-1: 文件夹变更不能影响远端历史数据完整性。
- [ ] NFR-2: 批量移动会话时应避免明显卡顿。

## API Contract
基础会话数据来自 [../../api/chat-history.md](../../api/chat-history.md)，文件夹元数据为本地扩展。

## UI Description
会话列表可切换“全部/文件夹”视图；文件夹作为侧边栏或顶部筛选入口；移动会话使用底部弹层选择目标文件夹。

## Data Model
- `ConversationFolderEntity`
- `ConversationFolderJoin`

## Architecture Notes
文件夹能力完全本地实现，由 Room 维护多对一映射，不修改 FastGPT 历史接口。

## Dependencies
- 会话列表
- Room 关系表

## Acceptance Criteria
- 文件夹 CRUD 与会话归类流程完整可用。
- 切换筛选后列表结果正确。

