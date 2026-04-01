---
status: implemented
priority: P1
phase: 1-mvp
---

# Theme Appearance

## Overview
主题设置只保留亮色和暗色两种模式，并驱动全局 Material 3 token。

## Functional Requirements
- [ ] FR-1: 支持亮色。
- [ ] FR-2: 支持暗色。
- [ ] FR-3: 切换后全局立即生效。

## UI Description
页面仅显示浅色和深色两个可选项，不再提供跟随系统、OLED 深色或动态取色开关。

## Data Model
- `AppearancePreferences(themeMode, languageTag)`

## Architecture Notes
设置写入 DataStore，由 `:app` 主题层消费；不保留额外主题增强开关。

## Acceptance Criteria
- 主题切换后全局 UI 同步刷新。
- 代码库内不再存在 OLED 或动态取色的设置入口。
