---
status: draft
priority: P2
phase: 2-knowledge
---

# Web Link Import

## Overview
允许用户通过网页链接导入内容到知识库，适合快速采集公开网页资料。

## Functional Requirements
- [ ] FR-1: 用户可输入一个或多个 URL 创建链接型 Collection。
- [ ] FR-2: 系统展示抓取与训练状态。
- [ ] FR-3: 失败链接可单独重试或编辑。

## Non-Functional Requirements
- [ ] NFR-1: URL 校验要在客户端预处理，减少无效请求。
- [ ] NFR-2: 批量导入时需要控制并发和错误提示密度。

## API Contract
依赖 [../../api/dataset-collections.md](../../api/dataset-collections.md)。

## UI Description
页面包含 URL 输入区、批量列表和状态反馈；处理中显示进度或状态标签；失败项允许局部修改和重试。

## Data Model
- `LinkImportDraft`
- `LinkImportItem`

## Architecture Notes
链接导入与文件导入共享 Collection 创建流程，但前置输入校验和结果展示模型不同。

## Dependencies
- Collection 接口
- URL 校验器

## Acceptance Criteria
- 单链接和多链接导入均可执行。
- 失败项不会阻塞成功项完成。

