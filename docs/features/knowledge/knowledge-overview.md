---
status: draft
priority: P1
phase: 2-knowledge
---

# Knowledge Overview

## Overview
定义知识库功能在 Kora 中的整体目标、信息架构与 FastGPT 概念映射，作为后续各子功能 spec 的入口。

## Functional Requirements
- [ ] FR-1: 用户可浏览现有知识库并进入详情。
- [ ] FR-2: 用户可围绕 Dataset、Collection 和 Chunk 完成基础管理。
- [ ] FR-3: 用户可做检索测试并查看引用来源。

## Non-Functional Requirements
- [ ] NFR-1: 知识库列表与详情必须能承载较大数据量。
- [ ] NFR-2: 训练中与失败状态需要显式可见。

## API Contract
依赖 [../../api/dataset-management.md](../../api/dataset-management.md)、[../../api/dataset-collections.md](../../api/dataset-collections.md)、[../../api/dataset-data.md](../../api/dataset-data.md) 和 [../../api/dataset-search.md](../../api/dataset-search.md)。

## UI Description
知识库首页展示数据集列表，进入详情后分为 Collection、Chunk 和检索测试三个主要区域；加载、空状态、错误和操作成功状态都需可辨认。

## Data Model
- `DatasetSummary`
- `DatasetDetailState`
- `CollectionSummary`

## Architecture Notes
落在 `:feature:knowledge`，以数据集为聚合根组织子页面和仓库接口，并与聊天引用能力共享部分领域模型。

## Dependencies
- 知识库接口
- 本地缓存
- 设计系统列表与详情组件

## Acceptance Criteria
- 用户可从总览页进入各知识库子能力。
- Dataset、Collection 和 Chunk 术语在 UI 与文档中保持一致。

