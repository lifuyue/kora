---
status: planned
priority: P2
phase: 3-advanced
---

# Conversation Export

## Overview
导出用于备份或离线审阅。MVP 交付 `JSON` 和 `TXT` 两种格式；`PDF` 不进入当前实现范围。

## Functional Requirements
- [ ] FR-1: 用户可导出整段会话为 `JSON` 或 `TXT`。
- [ ] FR-2: 导出内容包含会话元数据、消息顺序、时间和角色。
- [ ] FR-3: 导出完成后支持打开或分享文件。

## Non-Functional Requirements
- [ ] NFR-1: 大会话导出在后台线程执行。
- [ ] NFR-2: 导出失败时保留明确错误和重试入口。

## API Contract
数据来源是 [../../api/chat-records.md](../../api/chat-records.md) 与本地缓存，不依赖额外远端导出端点。

## UI Description
导出面板允许选择格式和范围。执行中显示进度条；完成后展示结果卡片，含“打开”“分享”“重新导出”。

## Data Model
- `ConversationExportRequest(chatId, format, range)`
- `ConversationExportResult(fileUri, bytes, createdAt)`

## Architecture Notes
导出与分享分层：导出先生产文件，分享再消费文件。消息富内容在导出时需要展开为稳定文本或结构化 JSON。

## Dependencies
- [conversation-share.md](conversation-share.md)

## Acceptance Criteria
- JSON 和 TXT 导出可读且顺序正确。
- 导出后文件可被系统打开。
