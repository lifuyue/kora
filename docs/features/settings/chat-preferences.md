---
status: implemented
priority: P1
phase: 1-mvp
---

# Chat Preferences

## Overview
聊天偏好控制流式显示、自动滚动、字体大小和引用显示等本地体验开关。

## Functional Requirements
- [ ] FR-1: 支持启用/禁用流式显示。
- [ ] FR-2: 支持自动滚动开关。
- [ ] FR-3: 支持聊天字体缩放和引用默认展开策略。

## Non-Functional Requirements
- [ ] NFR-1: 设置修改后在聊天页即时可见。
- [ ] NFR-2: 默认值对首次用户合理，不要求复杂解释。

## API Contract
流式开关会影响 [../../api/chat-completions.md](../../api/chat-completions.md) 的 `stream` 参数，其余为本地配置。

## UI Description
使用开关和滑块组成的列表页，并提供小型实时预览。关闭流式后，聊天页等待完整结果再插入 assistant 内容。

## Data Model
- `ChatPreferences(streamEnabled, autoScroll, fontScale, showCitationsByDefault)`

## Architecture Notes
设置页写入 DataStore，聊天 ViewModel 订阅变化。不要在每次发送时重新解析复杂对象，偏好应当预归一化。

## Dependencies
- [../chat/streaming-chat.md](../chat/streaming-chat.md)
- [../chat/citations.md](../chat/citations.md)

## Acceptance Criteria
- 三类偏好都能保存且聊天页立即生效。
