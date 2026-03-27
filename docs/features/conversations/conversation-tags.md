---
status: draft
priority: P2
phase: 2-knowledge
---

# Conversation Tags

## Overview
为会话提供轻量标签分类，适合跨文件夹的主题检索与筛选。

## Functional Requirements
- [ ] FR-1: 用户可创建标签并绑定到会话。
- [ ] FR-2: 支持按单个标签筛选会话。
- [ ] FR-3: 会话详情和列表项可展示已绑定标签。

## Non-Functional Requirements
- [ ] NFR-1: 标签选择器要适配大量标签场景。
- [ ] NFR-2: 标签颜色和名称需要满足可访问性要求。

## API Contract
基础会话数据来自 [../../api/chat-history.md](../../api/chat-history.md)，标签为本地扩展元数据。

## UI Description
标签通过 chips 形式展示；列表提供标签筛选器；编辑标签时支持搜索已有标签和创建新标签。

## Data Model
- `ConversationTagEntity`
- `ConversationTagJoin`

## Architecture Notes
标签与文件夹类似，属于客户端组织能力，存储在本地数据库并与会话摘要联表查询。

## Dependencies
- 会话列表
- Room
- 设计系统 chips

## Acceptance Criteria
- 标签创建、绑定、解绑和筛选均可工作。
- 列表和详情的标签显示一致。

