---
status: implemented
priority: P2
phase: 2-knowledge
---

# App Detail

## Overview
App 详情页展示 app 的基础信息和对聊天有直接影响的 `chatConfig` 摘要，帮助用户理解欢迎语、变量、文件选择和推荐问题配置。

## Functional Requirements
- [ ] FR-1: 展示 app 基础信息、类型和简介。
- [ ] FR-2: 展示欢迎语、变量字段、文件选择限制和推荐问题摘要。
- [ ] FR-3: 支持从详情页进入聊天或检索相关子页面。

## Non-Functional Requirements
- [ ] NFR-1: 配置字段缺失时有稳定降级显示。
- [ ] NFR-2: 详情页失败不影响当前 app 继续使用。

## API Contract
依赖 [../../api/app-management.md](../../api/app-management.md) 和 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
页面顶部显示头像、名称和简介；下方分 section 展示 `welcomeText`、变量表单、附件支持和推荐问题配置。只显示对客户端有实际影响的字段。

## Data Model
- `AppDetailUiState(app, resolvedCapabilities)`
- `AppCapabilitySection(title, items)`

## Architecture Notes
详情页以 `chat/init` 返回的 app 概览为主要来源，必要时补充 app 列表中的元信息。不要泄露 FastGPT 内部 workflow 节点细节到 UI。

## Dependencies
- [app-selector.md](app-selector.md)

## Acceptance Criteria
- 用户可读懂当前 app 的主要能力边界。
- 详情页中的能力摘要与实际聊天行为一致。
