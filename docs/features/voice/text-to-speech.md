---
status: draft
priority: P2
phase: 3-advanced
---

# Text To Speech

## Overview
TTS 把 assistant 消息朗读出来，配置来源受 app `ttsConfig` 和用户本地音频偏好共同影响。

## Functional Requirements
- [ ] FR-1: assistant 消息支持开始朗读、暂停、停止。
- [ ] FR-2: 支持单条消息朗读和顺序播放队列。
- [ ] FR-3: 支持根据 app `ttsConfig` 选择 web/model/system TTS 路径或降级到系统 TTS。

## Non-Functional Requirements
- [ ] NFR-1: 播放状态和当前消息绑定稳定，不发生重叠播报。
- [ ] NFR-2: 切后台后能安全停止或维持受控播放状态。

## API Contract
文本来源于 [../../api/chat-records.md](../../api/chat-records.md)，配置来源于 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
assistant 消息尾部显示朗读按钮；播放中的消息显示活动状态；可选全局迷你播放器显示当前播报进度。

## Data Model
- `TtsPlaybackState(messageId?, status, progress)`
- `TtsQueueItem(messageId, text, voiceConfig)`

## Architecture Notes
TTS 由独立 manager 管理，feature 只发播放意图和订阅状态，避免把音频生命周期塞进消息列表 reducer。

## Dependencies
- [../settings/audio-config.md](../settings/audio-config.md)

## Acceptance Criteria
- 单条朗读和停止可稳定工作。
- 连续朗读不会出现重叠播报。
