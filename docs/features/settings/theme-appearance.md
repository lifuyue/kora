---
status: implemented
priority: P1
phase: 1-mvp
---

# Theme Appearance

## Overview
主题设置定义亮色、暗色、跟随系统、OLED 深色和动态取色开关，并驱动全局 Material 3 token。

## Functional Requirements
- [ ] FR-1: 支持亮色、暗色、跟随系统。
- [ ] FR-2: 支持 OLED 深色增强模式。
- [ ] FR-3: 支持动态取色开关。

## Non-Functional Requirements
- [ ] NFR-1: 切换主题时即时生效。
- [ ] NFR-2: 颜色对比满足可访问性约束。

## API Contract
本功能不依赖远端接口。

## UI Description
页面显示主题模式选项卡、OLED 开关和动态取色开关，附带迷你预览卡片。

## Data Model
- `AppearancePreferences(themeMode, oledEnabled, dynamicColorEnabled)`

## Architecture Notes
设置写入 DataStore，由 `:app` 主题层消费；不要在 feature 层单独持有临时主题状态超过页面生命周期。

## Dependencies
- [../../ui/design-system.md](../../ui/design-system.md)

## Acceptance Criteria
- 主题切换后全局 UI 同步刷新。
- 不支持动态取色的设备可优雅降级。
