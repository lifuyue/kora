---
status: draft
priority: P1
phase: 1-mvp
---

# Theme Appearance

## Overview
提供亮色、暗色、OLED 和 Material You 动态取色等主题设置，让用户获得符合设备和偏好的视觉体验。

## Functional Requirements
- [ ] FR-1: 支持亮色、暗色和跟随系统。
- [ ] FR-2: 支持 OLED 深色主题。
- [ ] FR-3: 支持 Material You 动态取色开关。

## Non-Functional Requirements
- [ ] NFR-1: 主题切换应尽量即时生效。
- [ ] NFR-2: 颜色对比度需满足可访问性要求。

## API Contract
本功能主要依赖本地配置，不依赖 FastGPT API。

## UI Description
设置页展示主题选项卡、动态取色开关和预览区；切换后全局主题即时刷新，无法支持的设备显示说明。

## Data Model
- `ThemeMode`
- `AppearancePreferences`

## Architecture Notes
主题配置在 `:app` 层消费，在 `:feature:settings` 中编辑并写入 DataStore。

## Dependencies
- Material 3
- DataStore

## Acceptance Criteria
- 主题模式切换后整个应用样式同步更新。
- 不支持动态取色的设备能优雅降级。

