---
status: draft
priority: P3
phase: 1-mvp
---

# About

## Overview
提供应用版本、开源说明、许可证和外部链接，帮助用户理解 Kora 的来源与当前构建信息。

## Functional Requirements
- [ ] FR-1: 展示应用版本号和构建号。
- [ ] FR-2: 展示项目简介、许可证和仓库链接。
- [ ] FR-3: 支持复制诊断信息。

## Non-Functional Requirements
- [ ] NFR-1: 关于页信息必须和实际构建配置一致。
- [ ] NFR-2: 外链跳转需要明确离开应用的行为。

## API Contract
本功能不依赖 FastGPT API。

## UI Description
页面展示应用 Logo、版本信息、许可证说明和外部链接按钮；诊断信息复制后有即时反馈。

## Data Model
- `AboutInfo`
- `DiagnosticInfo`

## Architecture Notes
关于页主要读取本地构建常量与静态资源，避免引入不必要的远端依赖。

## Dependencies
- BuildConfig
- 剪贴板
- 外链跳转

## Acceptance Criteria
- 版本、许可证和仓库链接显示正确。
- 诊断信息复制可被用户成功使用。

