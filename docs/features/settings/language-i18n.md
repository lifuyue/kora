---
status: draft
priority: P2
phase: 3-advanced
---

# Language I18n

## Overview
支持多语言界面与本地化资源管理，提升 Kora 的国际化可用性。

## Functional Requirements
- [ ] FR-1: 支持系统跟随与手动选择应用语言。
- [ ] FR-2: 字符串资源按 locale 切换。
- [ ] FR-3: 对时间、数字和文件大小做本地化格式化。

## Non-Functional Requirements
- [ ] NFR-1: 新增文案必须可追踪到资源文件，不允许硬编码。
- [ ] NFR-2: 切换语言后需在合理范围内刷新页面而不丢状态。

## API Contract
本功能不直接依赖 FastGPT API，但需要与远端错误码文案映射保持一致，可参考 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
设置页提供语言列表和当前语言摘要；切换语言后提示需要刷新或自动重建页面；未翻译字段需有回退策略。

## Data Model
- `AppLanguage`
- `LanguagePreferenceState`

## Architecture Notes
语言偏好属于全局配置，`app` 层负责 locale 应用，业务模块只消费资源结果，不自行拼接文案。

## Dependencies
- Android locale 支持
- DataStore
- 字符串资源管理

## Acceptance Criteria
- 应用语言切换后大部分核心页面文案正确更新。
- 新增功能可接入现有 i18n 流程。

