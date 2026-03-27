---
status: draft
priority: P1
phase: 2-knowledge
---

# Collection Management

## Overview
Collection 管理页围绕 `DatasetCollectionSchemaType` 展示导入单元、训练类型和处理状态，是数据集详情的主视图。

## Functional Requirements
- [ ] FR-1: 展示 collection 列表、类型、训练模式和更新时间。
- [ ] FR-2: 支持进入某个 collection 的 chunk 查看器。
- [ ] FR-3: 支持删除、重试导入、查看训练异常。

## Non-Functional Requirements
- [ ] NFR-1: 大量 collection 时列表仍可快速滚动和筛选。
- [ ] NFR-2: 状态更新不会导致整页闪烁。

## API Contract
依赖 [../../api/dataset-collections.md](../../api/dataset-collections.md) 和 [../../api/dataset-data.md](../../api/dataset-data.md)。

## UI Description
页面显示 collection 列表卡片，包含名称、来源类型、训练模式、更新时间和状态标签。顶部提供“上传文件”“导入网页”“录入文本”等新增入口。失败项允许重试或查看错误。

## Data Model
- `CollectionListState(items, filters, createEntryPoints)`
- `CollectionItemUiModel(collectionId, type, trainingType, status, sourceName, canRetry)`

## Architecture Notes
Collection 创建入口虽然分散，但最终都回到同一个列表刷新链路。Repository 负责把不同来源的 collection 统一成一个 UI 模型。

## Dependencies
- [document-upload.md](document-upload.md)
- [web-link-import.md](web-link-import.md)
- [text-input.md](text-input.md)

## Acceptance Criteria
- collection 列表、创建入口和查看详情闭环可用。
- 不同来源类型能正确显示。
