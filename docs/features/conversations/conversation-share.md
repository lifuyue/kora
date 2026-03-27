---
status: draft
priority: P2
phase: 3-advanced
---

# Conversation Share

## Overview
会话分享是 Android 本地分享能力，不等同于 FastGPT out-link 分享。它将当前会话内容导出为文本或文件并调用系统 Share Sheet。

## Functional Requirements
- [ ] FR-1: 支持分享整段会话或选中消息片段。
- [ ] FR-2: 支持生成文本分享内容和导出文件后分享。
- [ ] FR-3: 分享前提供预览和敏感内容确认。

## Non-Functional Requirements
- [ ] NFR-1: 默认不泄露未选中的消息。
- [ ] NFR-2: 文件生成在后台执行，避免阻塞 UI。

## API Contract
消息数据来自 [../../api/chat-records.md](../../api/chat-records.md) 或本地缓存。

## UI Description
聊天页/会话详情页提供“分享”操作。进入分享面板后选择范围和格式，确认后调起系统 Share Sheet。若生成文件较慢，先展示进度。

## Data Model
- `ConversationShareSelection(messageIds, format)`
- `ConversationSharePayload(title, content, fileUri?)`

## Architecture Notes
分享逻辑位于 feature 层，文件创建和 `FileProvider` 适配可下沉到 common。

## Dependencies
- [conversation-export.md](conversation-export.md)

## Acceptance Criteria
- 文本分享和文件分享至少各有一种实现路径可用。
