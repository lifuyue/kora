---
status: draft
priority: P2
phase: 2-knowledge
---

# App Detail

## Overview
展示应用详情，包括配置摘要、欢迎语和文件选择配置，帮助用户理解当前 App 的能力边界。

## Functional Requirements
- [ ] FR-1: 展示 App 基础信息与描述。
- [ ] FR-2: 展示欢迎语、建议输入或能力标签。
- [ ] FR-3: 展示文件选择相关配置和限制。

## Non-Functional Requirements
- [ ] NFR-1: 配置字段缺失时需要有合理占位。
- [ ] NFR-2: 详情页加载失败时不能影响当前 App 继续使用。

## API Contract
依赖 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
详情页包含头部概要、能力标签区、欢迎语区域和配置列表；支持从选择器或设置页进入。

## Data Model
- `AppDetail`
- `AppCapabilityTag`

## Architecture Notes
详情页只读展示为主，可直接复用 App repository，不引入额外状态写回流程。

## Dependencies
- App 详情接口
- 可复用信息卡片组件

## Acceptance Criteria
- 用户能看到当前 App 的主要能力说明。
- 字段缺失时页面仍可正常展示。

