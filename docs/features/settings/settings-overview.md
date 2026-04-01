---
status: implemented
priority: P1
phase: 1-mvp
---

# Settings Overview

## Overview
设置页是连接配置、主题、语言、缓存和关于页的统一入口，负责展示当前摘要并进入对应详情页。

## Functional Requirements
- [ ] FR-1: 首页按“连接与账号”“外观与主题”“常用与信息”分组展示。
- [ ] FR-2: 每个条目展示当前值摘要并进入详情页。
- [ ] FR-3: 关键配置修改后能即时反馈到摘要。

## UI Description
设置总览采用 section list。每项展示标题、说明和当前摘要；不再承载聊天偏好或音频与语音入口。

## Data Model
- `SettingsOverviewState(connection, currentApp, theme, language, cache, about)`

## Dependencies
- [connection-config.md](connection-config.md)
- [theme-appearance.md](theme-appearance.md)

## Acceptance Criteria
- 用户可从设置首页进入保留的子设置。
- 摘要信息和实际配置保持一致。
