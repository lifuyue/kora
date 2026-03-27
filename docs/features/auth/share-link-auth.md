---
status: draft
priority: P2
phase: 3-advanced
---

# Share Link Auth

## Overview
处理分享链接深层链接、临时认证和分享内容加载，让用户能从外部入口安全进入共享内容。

## Functional Requirements
- [ ] FR-1: 应用能解析分享深层链接参数。
- [ ] FR-2: 客户端可完成分享态认证并加载内容。
- [ ] FR-3: 分享态结束后可安全退出回常规状态。

## Non-Functional Requirements
- [ ] NFR-1: 分享凭证必须与主应用凭证隔离。
- [ ] NFR-2: 链接无效或过期时要提供清晰回退路径。

## API Contract
依赖 [../../api/share-auth.md](../../api/share-auth.md) 和 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
从系统深链进入后显示分享加载页；认证成功则进入分享内容详情，失败则展示错误与返回首页按钮。

## Data Model
- `ShareLinkPayload`
- `ShareAuthState`

## Architecture Notes
分享态建议使用独立 session 容器，避免覆盖主连接配置；导航上可采用单独 graph 管理。

## Dependencies
- 深层链接
- 分享认证接口
- 全局 session 管理

## Acceptance Criteria
- 合法分享链接可成功打开内容。
- 过期或无效链接能被安全拦截并提示。

