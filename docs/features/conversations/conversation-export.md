---
status: draft
priority: P2
phase: 3-advanced
---

# Conversation Export

## Overview
支持将会话导出为 JSON、TXT 或 PDF，满足备份、归档和外部审阅需求。

## Functional Requirements
- [ ] FR-1: 用户可选择导出格式和导出范围。
- [ ] FR-2: 导出文件包含基本会话元数据与消息内容。
- [ ] FR-3: 导出完成后可打开、分享或保存到指定位置。

## Non-Functional Requirements
- [ ] NFR-1: 大会话导出需在后台线程执行。
- [ ] NFR-2: 导出失败要保留明确错误原因和重试入口。

## API Contract
会话内容来自 [../../api/chat-records.md](../../api/chat-records.md) 或本地缓存；导出本身不依赖额外 FastGPT 端点。

## UI Description
用户在会话菜单中进入导出面板，选择 `JSON/TXT/PDF` 和导出范围；处理中显示进度，成功后提供“分享”与“打开文件夹”操作。

## Data Model
- `ConversationExportRequest`
- `ConversationExportFile`

## Architecture Notes
导出能力应独立于聊天页主状态机，避免长时 IO 影响消息滚动；PDF 可作为后续增强实现。

## Dependencies
- 会话记录
- 本地文件写入
- PDF 生成能力

## Acceptance Criteria
- JSON 和 TXT 导出完整可用。
- 导出后的文件能被系统或外部应用读取。

