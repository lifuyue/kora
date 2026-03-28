---
status: implemented
priority: P2
phase: 3-advanced
---

# Language I18n

## Overview
语言设置控制应用 locale 和本地化资源切换，并为远端错误映射提供本地文案容器。

## Functional Requirements
- [ ] FR-1: 支持跟随系统和手动语言选择。
- [ ] FR-2: 时间、数字和文件大小格式按 locale 显示。
- [ ] FR-3: 切换后主要页面自动刷新。

## Non-Functional Requirements
- [ ] NFR-1: 禁止新增硬编码文案。
- [ ] NFR-2: 切换语言后不丢关键页面状态。

## API Contract
本地能力为主；远端错误码映射参考 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
列表页展示可选语言、当前语言和“跟随系统”入口。切换后弹出轻提示说明部分页面会重建。

## Data Model
- `LanguagePreferenceState(mode, selectedTag)`

## Architecture Notes
locale 由 app 层统一应用。feature 层仅消费资源，不自行缓存翻译字符串。

## Dependencies
- [../../ui/accessibility.md](../../ui/accessibility.md)

## Acceptance Criteria
- 语言切换后核心页面文案正确更新。
- 格式化输出符合 locale。
