---
status: planned
priority: P2
phase: 3-advanced
---

# Audio Config

## Overview
音频设置统一配置 STT/TTS 引擎、语速、自动发送等偏好，为语音功能提供单一来源。

## Functional Requirements
- [ ] FR-1: 选择 STT 引擎和自动发送策略。
- [ ] FR-2: 选择 TTS 引擎、语速和默认语音。
- [ ] FR-3: 提供引擎可用性测试。

## Non-Functional Requirements
- [ ] NFR-1: 切换引擎后旧资源及时释放。
- [ ] NFR-2: 不可用引擎以 disabled + 原因说明显示。

## API Contract
主要是本地配置；如 app `ttsConfig/whisperConfig` 存在，需在页面中显示“由当前 app 影响”的提示，参考 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
页面分为 STT 和 TTS 两个 section。每个 section 含引擎选择、附加参数和测试按钮。

## Data Model
- `AudioConfigState(sttEngine, ttsEngine, autoSend, speed, testStatus)`

## Architecture Notes
音频设置是本地偏好，不直接控制当前播放/录音 session；正在进行的 session 只在下一次启动时应用新配置。

## Dependencies
- [../voice/speech-to-text.md](../voice/speech-to-text.md)
- [../voice/text-to-speech.md](../voice/text-to-speech.md)

## Acceptance Criteria
- 引擎切换和测试路径可用。
- 不可用引擎不会导致崩溃。
