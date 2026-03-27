---
status: draft
priority: P2
phase: 3-advanced
---

# Speech To Text

## Overview
支持语音输入，将用户语音转为文本并送入聊天输入框，降低移动端输入成本。

## Functional Requirements
- [ ] FR-1: 支持按住说话或点击开始录音。
- [ ] FR-2: 支持系统 `SpeechRecognizer` 或后续接入 Whisper。
- [ ] FR-3: 识别结果可编辑后再发送。

## Non-Functional Requirements
- [ ] NFR-1: 麦克风权限拒绝与识别失败要有清晰提示。
- [ ] NFR-2: 录音与识别过程要控制功耗和资源释放。

## API Contract
默认优先使用本地或系统能力；若接入远端识别服务，再补充对应 API 文档。

## UI Description
输入框旁展示麦克风按钮；录音中显示波形或计时；识别完成后将文本填入输入框，用户可修改再发送。

## Data Model
- `VoiceInputState`
- `SpeechRecognitionResult`

## Architecture Notes
语音输入属于聊天输入层扩展，应与消息发送解耦；识别引擎选择由设置模块提供。

## Dependencies
- Android 麦克风权限
- SpeechRecognizer 或 Whisper

## Acceptance Criteria
- 用户可完成录音、识别和编辑后发送。
- 权限拒绝和识别失败路径可恢复。

