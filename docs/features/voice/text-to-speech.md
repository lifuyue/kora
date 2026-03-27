---
status: draft
priority: P2
phase: 3-advanced
---

# Text To Speech

## Overview
支持将回答朗读出来，并提供播放队列与基础控制能力。

## Functional Requirements
- [ ] FR-1: 用户可对单条 assistant 消息发起朗读。
- [ ] FR-2: 支持播放、暂停、停止和切换下一条。
- [ ] FR-3: 长消息支持分段朗读与进度恢复。

## Non-Functional Requirements
- [ ] NFR-1: TTS 队列切换时不能出现明显音频重叠。
- [ ] NFR-2: 后台播放与前台恢复要保持状态一致。

## API Contract
默认使用系统 TTS，本功能不强依赖 FastGPT API；消息内容来源于 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
assistant 消息操作区提供朗读按钮；播放中的消息显示状态与进度；用户可在全局迷你播放器中查看当前队列。

## Data Model
- `TtsQueueItem`
- `TtsPlaybackState`

## Architecture Notes
TTS 能力建议封装为独立 manager，由 `:feature:chat` 调用，避免 ViewModel 直接管理复杂音频生命周期。

## Dependencies
- Android TTS
- 消息记录

## Acceptance Criteria
- 单条消息朗读和全局停止都可用。
- 队列切换与后台恢复状态正确。

