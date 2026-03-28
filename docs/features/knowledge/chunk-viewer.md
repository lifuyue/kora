---
status: implemented
priority: P1
phase: 2-knowledge
---

# Chunk Viewer

## Overview
Chunk 查看器展示 `DatasetDataSchemaType`，用于查看、编辑、禁用和回溯引用命中的知识片段。

## Functional Requirements
- [ ] FR-1: 支持分页浏览 chunk 列表。
- [ ] FR-2: 支持查看详情、编辑 `q/a`、删除或禁用 chunk。
- [ ] FR-3: 支持从聊天引用跳入对应 chunk。

## Non-Functional Requirements
- [ ] NFR-1: 编辑和删除操作应有回滚策略。
- [ ] NFR-2: 超长 chunk 详情采用分段或折叠显示，避免主线程卡顿。

## API Contract
依赖 [../../api/dataset-data.md](../../api/dataset-data.md) 和 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
列表项显示 `chunkIndex`、问题/正文摘要和状态。点击进入详情页，可查看全文、索引信息、历史版本与关联引用。编辑页支持保存和取消。

## Data Model
- `ChunkListItemUiModel(dataId, chunkIndex, preview, forbid, rebuilding)`
- `ChunkEditorState(q, a, indexes, dirty)`

## Architecture Notes
chunk viewer 既服务知识管理，也服务聊天引用回跳。跳入时通过 `datasetId/collectionId/dataId` 精确定位；普通浏览时走 collection 范围分页。

## Dependencies
- [collection-management.md](collection-management.md)
- [../chat/citations.md](../chat/citations.md)

## Acceptance Criteria
- 能从 collection 列表进入 chunk 浏览。
- 能从聊天引用跳入对应 chunk。
