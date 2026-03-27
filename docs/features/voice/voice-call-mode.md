---
status: draft
priority: P3
phase: 3-advanced
---

# Voice Call Mode

## Overview
提供更接近通话的全双工或近实时语音交互模式，串联 STT、LLM 与 TTS。

## Functional Requirements
- [ ] FR-1: 用户可进入专用语音通话界面并开始连续对话。
- [ ] FR-2: 支持 STT -> Chat -> TTS 的连续链路。
- [ ] FR-3: 支持中断模型朗读并继续说话。

## Non-Functional Requirements
- [ ] NFR-1: 音频焦点、回声控制和功耗管理必须明确。
- [ ] NFR-2: 网络抖动时要有降级策略和状态提示。

## API Contract
聊天依赖 [../../api/chat-completions.md](../../api/chat-completions.md)，语音侧依赖 [speech-to-text.md](speech-to-text.md) 与 [text-to-speech.md](text-to-speech.md) 的能力组合。

## UI Description
进入语音模式后展示全屏通话界面，包括收音状态、实时字幕、AI 回答字幕和结束按钮；错误状态提供回到文字聊天入口。

## Data Model
- `VoiceCallSessionState`
- `LiveTranscriptSegment`

## Architecture Notes
该功能跨越音频、聊天和状态编排，建议以独立 coordinator 组织，而不是把所有逻辑塞进单一聊天 ViewModel。

## Dependencies
- STT
- TTS
- 流式聊天
- 音频焦点管理

## Acceptance Criteria
- 用户能完成至少一轮语音往返对话。
- 打断、暂停和退出流程可预测且稳定。

