---
status: draft
priority: P3
phase: 3-advanced
---

# App Analytics

## Overview
展示应用使用统计，帮助用户理解调用量、活跃度或资源消耗趋势。

## Functional Requirements
- [ ] FR-1: 展示基础统计指标，如请求数、会话数或最近活跃度。
- [ ] FR-2: 支持按时间范围查看统计变化。
- [ ] FR-3: 支持空数据和权限不足状态。

## Non-Functional Requirements
- [ ] NFR-1: 图表展示必须在手机端保持可读。
- [ ] NFR-2: 统计刷新不应频繁打断主交互。

## API Contract
依赖 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
页面顶部展示关键数字卡片，下方展示趋势图或列表；无统计权限时提示当前限制；加载中显示骨架。

## Data Model
- `AppAnalyticsSummary`
- `AnalyticsTimeRange`

## Architecture Notes
统计页可作为独立 screen，避免与聊天主链路耦合；数据更新采用显式刷新而不是高频轮询。

## Dependencies
- App 统计接口
- 图表组件

## Acceptance Criteria
- 统计数据可展示且时间范围切换正确。
- 空数据和权限受限状态可区分。

