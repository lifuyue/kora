---
status: implemented
priority: P2
phase: 2-knowledge
---

# Web Link Import

## Overview
网页导入对应 `create/link`，用于把 URL 转换为 link-type collection。

## Functional Requirements
- [ ] FR-1: 支持输入一个或多个 URL 并创建 collection。
- [ ] FR-2: 支持可选网页选择器输入。
- [ ] FR-3: 展示抓取/处理状态，并允许失败项重试。

## Non-Functional Requirements
- [ ] NFR-1: URL 在提交前必须完成格式校验与去重。
- [ ] NFR-2: 批量导入的成功与失败应分别反馈，不互相阻塞。

## API Contract
依赖 [../../api/dataset-collections.md](../../api/dataset-collections.md)。

## UI Description
输入区域提供 URL 列表编辑和可选高级参数如 selector。提交后页面显示每个链接的导入卡片和状态标签。

## Data Model
- `LinkImportDraft(url, selector?, status, error?)`
- `LinkImportBatchState(items, submitting)`

## Architecture Notes
Web link import 与文档上传共用 collection 刷新链路，但输入模型不同。UI 不直接暴露 FastGPT 内部抓取实现。

## Dependencies
- [collection-management.md](collection-management.md)

## Acceptance Criteria
- 单链接和多链接都可正常导入。
- 失败链接可单独重试。
