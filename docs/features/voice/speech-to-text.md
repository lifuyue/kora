---
status: draft
priority: P2
phase: 3-advanced
---

# Speech To Text

## Overview
STT 是聊天输入层扩展，可使用系统语音识别或后续接入 FastGPT/Whisper 能力，但最终都回落到文本输入草稿。

## Functional Requirements
- [ ] FR-1: 支持开始录音、停止录音、取消录音。
- [ ] FR-2: 识别结果自动回填输入框，允许编辑后再发送。
- [ ] FR-3: 支持 `autoSend` 偏好，在配置开启时识别完成可直接发送。

## Non-Functional Requirements
- [ ] NFR-1: 麦克风权限被拒绝时有明确说明。
- [ ] NFR-2: 录音资源在退出页面或切后台后及时释放。

## API Contract
客户端优先使用本地/系统 STT；若接入 app `whisperConfig`，需要遵循 [../../api/app-management.md](../../api/app-management.md) 的配置摘要。

## UI Description
输入框旁显示麦克风按钮。录音中显示波形或计时态；识别完成后文本写回输入框；错误时提示并允许重新录音。

## Data Model
- `SpeechInputState(idle|recording|recognizing|error, transcript)`
- `SpeechEngineConfig(autoSend, engineType)`

## Architecture Notes
STT 不直接操作消息列表，只通过 `ChatComposerState` 影响输入区。引擎管理独立于 ChatViewModel。

## Dependencies
- [../settings/audio-config.md](../settings/audio-config.md)
- [../chat/multimodal-input.md](../chat/multimodal-input.md)

## Acceptance Criteria
- 可录音、识别、回填文本并发送。
- 权限拒绝和识别失败可恢复。
