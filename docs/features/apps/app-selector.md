---
status: draft
priority: P1
phase: 2-knowledge
---

# App Selector

## Overview
支持用户在多个 FastGPT App 之间切换，决定当前聊天、知识库引用和工作流执行所使用的应用上下文。

## Functional Requirements
- [ ] FR-1: 展示可用 App 列表及基本信息。
- [ ] FR-2: 支持选择当前默认 App。
- [ ] FR-3: 切换后聊天入口和新会话自动使用新 App。

## Non-Functional Requirements
- [ ] NFR-1: 切换 App 必须避免污染旧会话上下文。
- [ ] NFR-2: 列表加载失败时要允许重试且保留当前选择。

## API Contract
依赖 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
应用选择器可以是设置页入口或聊天页顶部切换器；列表项展示名称、简介和当前状态；选择后给出轻量成功反馈。

## Data Model
- `AppSummary`
- `SelectedAppState`

## Architecture Notes
当前 App 选择应作为全局可观察状态，持久化在 DataStore，并由聊天和知识库模块消费。
FastGPT 的产品结构说明 App 不是普通过滤器，而是聊天、知识库和工作流的上层上下文，因此 Kora 要把当前 App 作为全局会话上下文对待，并在切换时明确清理旧上下文。参考 [../../reference/fastgpt-implementation-patterns.md](../../reference/fastgpt-implementation-patterns.md)。

## Dependencies
- App 管理接口
- DataStore

## Acceptance Criteria
- 用户可切换并持久化当前 App。
- 新开的聊天会话使用正确的 App 上下文。
