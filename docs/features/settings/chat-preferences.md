---
status: draft
priority: P1
phase: 1-mvp
---

# Chat Preferences

## Overview
允许用户配置聊天相关偏好，例如流式开关、自动滚动和字体大小。

## Functional Requirements
- [ ] FR-1: 支持开启或关闭流式显示。
- [ ] FR-2: 支持自动滚动开关。
- [ ] FR-3: 支持聊天字体大小调整。

## Non-Functional Requirements
- [ ] NFR-1: 偏好修改后要尽快体现在聊天页。
- [ ] NFR-2: 默认值应对大多数首次使用者合理。

## API Contract
流式开关影响 [../../api/chat-completions.md](../../api/chat-completions.md) 请求参数，其余偏好主要为本地配置。

## UI Description
页面由开关和滑块组成，并提供即时预览；当关闭流式时，聊天页改为等待完整回复再展示。

## Data Model
- `ChatPreferences`
- `FontScaleOption`

## Architecture Notes
聊天偏好保存在 DataStore，由聊天模块订阅；配置读取必须轻量，避免每次发送时重新解析复杂对象。

## Dependencies
- DataStore
- 聊天模块

## Acceptance Criteria
- 三类偏好都能保存并在聊天页生效。
- 应用重启后配置仍然保持。

