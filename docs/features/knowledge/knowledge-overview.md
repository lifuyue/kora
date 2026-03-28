---
status: implemented
priority: P1
phase: 2-knowledge
---

# Knowledge Overview

## Overview
知识库域围绕 FastGPT 的 `Dataset -> Collection -> Chunk` 三层模型构建，目标是让用户在移动端完成浏览、导入、检索测试和引用回看，而不是复刻桌面后台的高密度管理台。

## Functional Requirements
- [ ] FR-1: 提供知识库总览入口，展示数据集数量、最近活动和快速操作。
- [ ] FR-2: 支持进入数据集浏览、文档导入、文本导入、检索测试等子功能。
- [ ] FR-3: 能从聊天引用跳回对应知识上下文。

## Non-Functional Requirements
- [ ] NFR-1: 移动端优先保留高价值主链，避免后台式信息过载。
- [ ] NFR-2: 知识相关长任务状态要可恢复。

## API Contract
依赖 [../../api/dataset-management.md](../../api/dataset-management.md)、[../../api/dataset-collections.md](../../api/dataset-collections.md)、[../../api/dataset-data.md](../../api/dataset-data.md) 和 [../../api/dataset-search.md](../../api/dataset-search.md)。

## UI Description
知识首页展示最近使用的数据集卡片、导入入口、搜索测试入口和空状态说明。页面强调“进入已有知识”和“新增内容”两条主路径；高频操作位于首屏。

## Data Model
- `KnowledgeOverviewState(recentDatasets, shortcuts, lastSyncTime)`
- `KnowledgeShortcut(type=openDataset|upload|addText|searchTest)`

## Architecture Notes
知识首页是 `:feature:knowledge` 的路由入口，不直接承载复杂表格逻辑，只编排子页面导航。引用回跳由 chat feature 传递 `datasetId/collectionId/dataId`。

## Dependencies
- [dataset-browser.md](dataset-browser.md)
- [search-test.md](search-test.md)

## Acceptance Criteria
- 用户可从首页进入所有主要知识链路。
- 空状态和已有数据状态都合理可用。
