---
status: draft
priority: P1
phase: 2-knowledge
---

# Collection Management

## Overview
管理某个数据集下的 Collection 列表与训练状态，帮助用户理解导入来源和处理进度。

## Functional Requirements
- [ ] FR-1: 列出 Collection 来源类型、创建时间和训练状态。
- [ ] FR-2: 支持删除 Collection。
- [ ] FR-3: 支持从列表进入对应内容详情。

## Non-Functional Requirements
- [ ] NFR-1: 训练状态刷新需要明确节流策略。
- [ ] NFR-2: 失败状态必须保留可诊断信息入口。

## API Contract
依赖 [../../api/dataset-collections.md](../../api/dataset-collections.md)。

## UI Description
Collection 列表按来源展示图标与标题，状态标签区分处理中、完成和失败；列表支持下拉刷新，删除前有确认步骤。

## Data Model
- `CollectionListItem`
- `CollectionTrainingState`

## Architecture Notes
Collection 管理位于 `:feature:knowledge` 数据集详情子页，与导入入口共用 repository。

## Dependencies
- Collection 接口
- 列表刷新能力

## Acceptance Criteria
- 用户可看到准确的 Collection 状态。
- 删除后列表和计数同步更新。

