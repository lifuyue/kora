---
status: draft
priority: P1
phase: 2-knowledge
---

# App Selector

## Overview
App 选择器决定当前聊天上下文、变量表单、文件选择策略和推荐问题配置。它不是普通筛选器，而是全局会话上下文切换器。

## Functional Requirements
- [ ] FR-1: 展示可访问 App 列表和当前选中项。
- [ ] FR-2: 支持切换默认 App，并刷新聊天入口依赖的 app 配置。
- [ ] FR-3: 切换后新建聊天自动使用新 app 上下文。

## Non-Functional Requirements
- [ ] NFR-1: 切换 app 时不得污染旧会话的变量或输入草稿。
- [ ] NFR-2: 列表加载失败时保留当前选择并可重试。

## API Contract
依赖 [../../api/app-management.md](../../api/app-management.md) 和 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
选择器可作为独立页面或聊天页顶部 sheet。列表项展示头像、名称、简介、类型和是否含交互节点。当前选中项有明显勾选状态。

## Data Model
- `AppSelectorState(items, selectedAppId, loading, error)`
- `AppSummaryUiModel(appId, name, avatar, intro, type, hasInteractiveNode)`

## Architecture Notes
当前 appId 写入 DataStore，并通过全局 observable state 分发给 chat/knowledge 模块。切换仅影响新会话和新的 app bootstrap，不 retroactively 改写已有 chatId。

## Dependencies
- [app-detail.md](app-detail.md)
- [../../architecture/navigation.md](../../architecture/navigation.md)

## Acceptance Criteria
- 可切换当前 App 并持久化。
- 新会话使用正确 app 上下文。
