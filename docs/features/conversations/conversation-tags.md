---
status: draft
priority: P2
phase: 2-knowledge
---

# Conversation Tags

## Overview
标签是跨文件夹的本地组织能力，用于轻量检索和筛选会话主题。

## Functional Requirements
- [ ] FR-1: 用户可创建、重命名、删除标签。
- [ ] FR-2: 会话可绑定多个标签。
- [ ] FR-3: 列表支持按标签过滤。

## Non-Functional Requirements
- [ ] NFR-1: 标签选择器需要支持大量标签搜索。
- [ ] NFR-2: 颜色与文本对比度满足可访问性要求。

## API Contract
无专属远端接口；基础会话仍来自 [../../api/chat-history.md](../../api/chat-history.md)。

## UI Description
标签以 chips 显示在会话行或详情页。编辑标签使用弹层选择器，支持搜索已有标签和快速创建。过滤时顶部展示当前激活标签 chip。

## Data Model
- `ConversationTagEntity(tagId, name, color)`
- `ConversationTagCrossRef(chatId, tagId)`

## Architecture Notes
标签与文件夹、归档、pin 一样属于本地扩展层。列表查询通过 Room 先过滤本地关系，再与远端摘要合并。

## Dependencies
- [conversation-list.md](conversation-list.md)

## Acceptance Criteria
- 标签 CRUD 和按标签过滤可正常工作。
- 删除标签不会删除原会话。
