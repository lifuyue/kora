---
status: draft
priority: P2
phase: 2-knowledge
---

# Conversation Archive

## Overview
支持归档不常用会话，在不删除历史的前提下降低主列表噪音。

## Functional Requirements
- [ ] FR-1: 用户可归档和取消归档会话。
- [ ] FR-2: 主列表默认隐藏归档项。
- [ ] FR-3: 提供单独的归档视图或筛选入口。

## Non-Functional Requirements
- [ ] NFR-1: 归档操作应为轻量本地更新。
- [ ] NFR-2: 归档状态切换后列表刷新要稳定。

## API Contract
基础会话数据来自 [../../api/chat-history.md](../../api/chat-history.md)，归档为本地扩展状态。

## UI Description
列表项菜单支持归档；归档后项目从主列表消失；用户可在过滤器中打开归档视图并恢复会话。

## Data Model
- `ArchivedConversationEntity`

## Architecture Notes
归档状态保存在本地，与置顶、标签、文件夹一起作为列表聚合层的一部分。

## Dependencies
- 会话列表
- Room

## Acceptance Criteria
- 归档和取消归档立即影响列表展示。
- 归档数据在重启后保持。

