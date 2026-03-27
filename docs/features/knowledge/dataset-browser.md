---
status: draft
priority: P1
phase: 2-knowledge
---

# Dataset Browser

## Overview
提供知识库浏览器，让用户创建、删除并查看数据集详情，是知识库域的一级入口。

## Functional Requirements
- [ ] FR-1: 展示数据集列表、数量与基础状态。
- [ ] FR-2: 支持创建和删除数据集。
- [ ] FR-3: 支持进入数据集详情页。

## Non-Functional Requirements
- [ ] NFR-1: 删除操作必须明确提示影响范围。
- [ ] NFR-2: 列表刷新时要避免状态闪烁。

## API Contract
依赖 [../../api/dataset-management.md](../../api/dataset-management.md)。

## UI Description
页面顶部提供创建入口，主体为数据集列表；空状态引导首次创建；删除使用危险确认弹窗；进入详情后展示 Collection 与检索能力入口。

## Data Model
- `DatasetListItem`
- `CreateDatasetFormState`

## Architecture Notes
数据集浏览器是 `:feature:knowledge` 的入口 screen，列表与创建逻辑可共用同一 ViewModel 的 intent 模型。

## Dependencies
- 数据集管理接口
- 表单验证

## Acceptance Criteria
- 新建、删除和进入详情路径均可用。
- 列表刷新后能反映远端最新状态。

