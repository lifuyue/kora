---
status: draft
priority: P0
phase: 1-mvp
---

# Authentication

## Overview
Kora 的认证不是传统账号登录，而是围绕连接配置、API key 校验和全局 session 状态展开。

## Functional Requirements
- [ ] FR-1: 在首次连接成功后建立已认证状态。
- [ ] FR-2: 认证失败时阻止进入依赖远端的主功能。
- [ ] FR-3: 用户可主动清除凭证并退出到未认证状态。

## Non-Functional Requirements
- [ ] NFR-1: 凭证不以明文出现在 UI、日志和非加密存储。
- [ ] NFR-2: 认证状态变化应立即传递到聊天、知识和 app 选择模块。

## API Contract
依赖 [../../api/authentication.md](../../api/authentication.md) 和 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
认证本身可由连接配置页承载，不单独新增复杂登录页。已认证时显示当前连接摘要；失效时显示重新连接提示。

## Data Model
- `AuthState(unconfigured|validating|authenticated|invalid)`
- `CredentialSummary(baseUrl, keyMasked, lastValidatedAt)`

## Architecture Notes
session manager 是全局单例状态容器，负责连接摘要、认证状态和 share session 隔离。

## Dependencies
- [../settings/connection-config.md](../settings/connection-config.md)

## Acceptance Criteria
- 有效连接进入 authenticated。
- 清除凭证后全局状态恢复到 unconfigured。
