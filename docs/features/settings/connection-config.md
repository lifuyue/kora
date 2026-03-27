---
status: draft
priority: P0
phase: 1-mvp
---

# Connection Config

## Overview
提供服务器连接配置，包括 Base URL、API Key 和连通性测试，是应用可用性的前置条件。

## Functional Requirements
- [ ] FR-1: 用户可输入和保存服务器 URL 与 API Key。
- [ ] FR-2: 保存前可执行连通测试。
- [ ] FR-3: 对明显无效的 URL 与 Key 做客户端校验。

## Non-Functional Requirements
- [ ] NFR-1: API Key 必须以安全方式存储。
- [ ] NFR-2: 连通测试结果要明确区分网络、鉴权和服务端错误。

## API Contract
依赖 [../../api/authentication.md](../../api/authentication.md) 和 [../../api/error-handling.md](../../api/error-handling.md)。

## UI Description
页面包含 URL 输入、API Key 输入、测试连接按钮和保存按钮；测试中显示进度；成功展示服务器可用提示，失败显示可诊断错误信息。

## Data Model
- `ConnectionConfigState`
- `ConnectionTestResult`

## Architecture Notes
配置修改后通过 DataStore 广播给网络层；安全凭证的落盘与展示摘要需要分离，避免泄露完整 Key。

## Dependencies
- DataStore
- 安全存储
- 网络探测接口

## Acceptance Criteria
- 用户能保存有效连接配置。
- 无效 URL、无效 Key 和不可达服务器都能正确提示。

