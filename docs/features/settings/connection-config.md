---
status: implemented
priority: P0
phase: 1-mvp
---

# Connection Config

## Overview
连接配置页负责保存 Base URL 和 API key，执行连通测试，并驱动全局连接状态。

## Functional Requirements
- [ ] FR-1: 用户可输入、校验并保存 Base URL 与 API key。
- [ ] FR-2: 保存前可执行连接测试。
- [ ] FR-3: 支持清除当前凭证并回到未连接状态。

## Non-Functional Requirements
- [ ] NFR-1: API key 不在 UI 或日志中完整回显。
- [ ] NFR-2: 测试结果要区分 URL 错误、认证失败、网络错误、服务端错误。

## API Contract
依赖 [../../api/authentication.md](../../api/authentication.md) 和 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
表单包含 Base URL、API key、测试连接和保存按钮。API key 默认遮挡显示；测试结果以状态卡片展示，包括成功、401/403、网络不可达等。

## Data Model
- `ConnectionConfigState(baseUrl, apiKeyInput, validation, testResult, saveEnabled)`
- `ConnectionTestResult(status, serverInfo?, error)`

## Architecture Notes
连接状态由 session manager 管理，DataStore 只保存可公开的连接摘要；密钥走安全存储。保存成功后广播给网络层和导航层。

## Dependencies
- [../auth/authentication.md](../auth/authentication.md)
- [../../architecture/networking.md](../../architecture/networking.md)

## Acceptance Criteria
- 正常连接可保存并进入应用。
- 无效连接被正确阻止或提示。
