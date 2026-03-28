---
status: planned
priority: P3
phase: 3-advanced
---

# App Analytics

## Overview
App analytics 展示 app 级使用概览，如请求量、会话量或活跃趋势。它是补充视图，不参与主聊天闭环。

## Functional Requirements
- [ ] FR-1: 展示基础统计卡片。
- [ ] FR-2: 支持切换时间范围。
- [ ] FR-3: 处理无权限或无数据状态。

## Non-Functional Requirements
- [ ] NFR-1: 图表在手机端保持可读。
- [ ] NFR-2: 刷新节奏可控，不做高频轮询。

## API Contract
依赖 [../../api/app-management.md](../../api/app-management.md)。若当前部署未开放统计端点，则该页面在产品层通过 feature flag 隐藏，不向用户暴露空壳入口。

## UI Description
页面顶部为关键指标，下方为趋势图或分布列表。无权限时展示只读说明和返回入口。

## Data Model
- `AppAnalyticsState(summary, series, range, status)`

## Architecture Notes
analytics 单独建屏，避免耦合到 app detail。实现前若后端端点仍不稳定，可保留受控 feature flag。

## Dependencies
- [app-detail.md](app-detail.md)

## Acceptance Criteria
- 有数据时展示完整统计。
- 无数据/无权限时界面清晰可理解。
