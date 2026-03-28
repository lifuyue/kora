---
status: implemented
priority: P3
phase: 1-mvp
---

# About

## Overview
关于页展示版本、构建号、许可证、仓库链接和诊断信息复制入口。

## Functional Requirements
- [ ] FR-1: 展示应用版本和构建信息。
- [ ] FR-2: 展示开源许可证与仓库链接。
- [ ] FR-3: 支持复制诊断信息。

## Non-Functional Requirements
- [ ] NFR-1: 版本信息必须与当前构建一致。
- [ ] NFR-2: 外链跳转清晰可控。

## API Contract
不依赖远端接口。

## UI Description
页面顶部显示应用图标和版本号，下方为许可证、仓库和诊断信息卡片。

## Data Model
- `AboutInfo(versionName, versionCode, commitSha?, license)`
- `DiagnosticInfo(baseUrlPresent, appVersion, deviceInfo)`

## Architecture Notes
内容来自 BuildConfig 与静态资源，不应依赖网络。

## Dependencies
- [../../ui/iconography.md](../../ui/iconography.md)

## Acceptance Criteria
- 版本、许可证和链接准确可用。
- 诊断信息复制成功。
