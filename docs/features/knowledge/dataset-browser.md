---
status: draft
priority: P1
phase: 2-knowledge
---

# Dataset Browser

## Overview
数据集浏览页负责展示 `DatasetListItemType` / `DatasetItemType`，让用户快速查看名称、类型、更新时间、状态和权限，再进入 collection 或搜索测试。

## Functional Requirements
- [ ] FR-1: 支持列表展示、搜索和下拉刷新数据集。
- [ ] FR-2: 支持按 `DatasetTypeEnum` 和状态筛选。
- [ ] FR-3: 点击数据集进入 collection 管理或详情页。

## Non-Functional Requirements
- [ ] NFR-1: 列表项信息密度在手机端可读，不引入桌面式复杂列。
- [ ] NFR-2: 搜索和筛选切换后滚动位置与结果稳定。

## API Contract
依赖 [../../api/dataset-management.md](../../api/dataset-management.md)。

## UI Description
页面顶部提供搜索和筛选，列表项显示头像、名称、类型、更新时间、状态与轻量描述。不同类型的数据集用不同 icon 识别；状态异常时显示 error tag。

## Data Model
- `DatasetBrowserState(items, query, typeFilter, statusFilter, paging)`
- `DatasetListItemUiModel(datasetId, name, type, status, intro, updateTime, permission)`

## Architecture Notes
浏览页优先消费远端列表，Room 仅用于最近访问和离线恢复。权限不足项默认不进入列表；若存在已缓存旧数据，显示只读降级状态。

## Dependencies
- [collection-management.md](collection-management.md)
- [../../ui/component-catalog.md](../../ui/component-catalog.md)

## Acceptance Criteria
- 可稳定浏览、搜索和打开数据集。
- 类型与状态标识和服务端一致。
