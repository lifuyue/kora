---
status: draft
priority: P2
phase: 3-advanced
---

# Conversation Share

## Overview
允许用户通过 Android Share Sheet 分享会话内容，支持复制摘要或导出后的文件分享。

## Functional Requirements
- [ ] FR-1: 用户可分享当前会话的文本摘要。
- [ ] FR-2: 用户可选择分享部分消息或整段会话。
- [ ] FR-3: 分享前可生成简短标题和预览文本。

## Non-Functional Requirements
- [ ] NFR-1: 分享过程不能泄露未选择的敏感内容。
- [ ] NFR-2: 大会话导出前要有明确进度反馈。

## API Contract
会话内容主要来自 [../../api/chat-records.md](../../api/chat-records.md)，分享动作本身依赖 Android 系统能力。

## UI Description
聊天页和会话详情页提供分享入口；点击后展示分享范围选择；确认后调起系统 Share Sheet 或生成临时文本文件。

## Data Model
- `ConversationSharePayload`
- `ShareSelectionRange`

## Architecture Notes
分享逻辑由 `:feature:chat` 组织，真正的文件生成与 `FileProvider` 适配可下沉到 `:core:common`。

## Dependencies
- 会话记录
- Android Share Sheet
- 可选导出能力

## Acceptance Criteria
- 可将选定会话内容成功分享给外部应用。
- 分享取消或失败不会影响原始会话数据。

