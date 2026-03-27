---
status: draft
priority: P2
phase: 3-advanced
---

# Share Link Auth

## Overview
分享链接认证处理外部 deep link、out-link 初始化和独立 session 生命周期。它与主 API key 连接配置完全隔离。

## Functional Requirements
- [ ] FR-1: 解析 `shareId`、`outLinkUid`、可选 `chatId`。
- [ ] FR-2: 通过 out-link init 完成分享态启动。
- [ ] FR-3: 分享态结束后安全回收 session，并返回主应用。

## Non-Functional Requirements
- [ ] NFR-1: 分享态错误不会污染主连接状态。
- [ ] NFR-2: 过期或无效链接有明确的错误页和回退入口。

## API Contract
依赖 [../../api/share-auth.md](../../api/share-auth.md) 和 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
应用被 deep link 拉起后，先进入加载态；验证成功则进入 share chat 界面；失败则显示错误、重试和回首页按钮。

## Data Model
- `ShareLinkPayload(shareId, outLinkUid, chatId?)`
- `ShareSessionState(initializing|ready|expired|error, appId?, chatId?)`

## Architecture Notes
建议使用独立 nav graph 和独立 share session store。退出 share graph 时清理所有 share-scoped state。

## Dependencies
- [../../architecture/navigation.md](../../architecture/navigation.md)
- [../chat/streaming-chat.md](../chat/streaming-chat.md)

## Acceptance Criteria
- 合法分享链接能进入聊天。
- 无效链接被安全拦截并可返回主应用。
