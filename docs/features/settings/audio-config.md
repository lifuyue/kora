---
status: draft
priority: P2
phase: 3-advanced
---

# Audio Config

## Overview
用于配置 STT 与 TTS 引擎、语速和音频相关偏好，为语音能力提供统一入口。

## Functional Requirements
- [ ] FR-1: 用户可选择 STT 引擎。
- [ ] FR-2: 用户可选择 TTS 引擎与语速。
- [ ] FR-3: 用户可测试当前音频配置是否可用。

## Non-Functional Requirements
- [ ] NFR-1: 引擎不可用时要给出明确降级说明。
- [ ] NFR-2: 切换引擎后应释放旧资源，避免泄漏。

## API Contract
本功能主要依赖本地音频能力，不直接依赖 FastGPT API。

## UI Description
页面展示 STT、TTS 选择器和测试按钮；当前不可用的引擎项标记为 disabled；测试区可直接播放示例音频或开始短录音。

## Data Model
- `AudioConfigState`
- `SttEngineType`
- `TtsEngineType`

## Architecture Notes
音频设置由 `:feature:settings` 编辑，具体引擎能力由音频 service 层消费和管理。

## Dependencies
- SpeechRecognizer
- TTS
- DataStore

## Acceptance Criteria
- 引擎切换、参数保存和测试能力可用。
- 不可用引擎不会导致崩溃或卡死。

