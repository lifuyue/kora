---
status: draft
priority: P2
phase: 2-knowledge
---

# Chunk Viewer

## Overview
提供知识数据块查看器，支持浏览、搜索和内联编辑单个 Chunk。

## Functional Requirements
- [ ] FR-1: 列出某个 Collection 下的数据块。
- [ ] FR-2: 支持查看 chunk 内容、元信息和来源。
- [ ] FR-3: 支持内联编辑和删除单个 chunk。

## Non-Functional Requirements
- [ ] NFR-1: 大量 chunk 浏览要支持分页或懒加载。
- [ ] NFR-2: 编辑保存失败时必须保留原始内容与重试入口。

## API Contract
依赖 [../../api/dataset-data.md](../../api/dataset-data.md)。

## UI Description
Chunk 列表支持搜索和分页；点击项后展开内联编辑区或进入详情页；保存中和删除中有明确按钮状态反馈。

## Data Model
- `ChunkItem`
- `ChunkEditorState`

## Architecture Notes
Chunk 查看器属于数据集详情子模块，编辑能力通过 repository 与本地 UI state 解耦。

## Dependencies
- 数据块接口
- 列表分页

## Acceptance Criteria
- 能查看、编辑和删除 chunk。
- 编辑后的内容刷新后仍保持一致。

