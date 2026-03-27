---
status: draft
priority: P3
phase: 3-advanced
---

# Voice Call Mode

## Overview
Voice Call Mode 把 STT、流式聊天和 TTS 串成连续语音会话，交互参考 Open WebUI `MessageInput/CallOverlay.svelte`。

## Functional Requirements
- [ ] FR-1: 用户可进入独立语音会话界面并开始一轮或连续多轮对话。
- [ ] FR-2: 支持 STT -> chat completions -> TTS 的端到端链路。
- [ ] FR-3: 用户说话时可打断 AI 播放并进入下一轮。

## Non-Functional Requirements
- [ ] NFR-1: 音频焦点和资源切换明确，不能与普通 TTS/STT 冲突。
- [ ] NFR-2: 网络异常时可安全回退到文本聊天。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md)、[speech-to-text.md](speech-to-text.md) 和 [text-to-speech.md](text-to-speech.md)。

## UI Description
进入语音模式后显示全屏或半屏 overlay，包括当前收音状态、实时字幕、AI 字幕和结束按钮。错误时提示切回文字聊天。

## Data Model
- `VoiceCallState(listening|thinking|speaking|error|idle, transcript, lastAssistantText)`

## Architecture Notes
建议独立 coordinator 管理，不复用普通聊天页的每个细粒度 state，但底层仍复用相同 Repository。

## Dependencies
- [../../ui/animations.md](../../ui/animations.md)
- [../../architecture/data-flow.md](../../architecture/data-flow.md)

## Acceptance Criteria
- 至少能完成一轮语音问答闭环。
- 退出语音模式后普通聊天仍可正常使用。
