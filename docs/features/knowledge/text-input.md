---
status: draft
priority: P2
phase: 2-knowledge
---

# Text Input

## Overview
文本录入是最轻量的知识导入方式，对应 `create/text` 或后续 chunk 数据批量插入。

## Functional Requirements
- [ ] FR-1: 支持直接输入长文本并创建 collection。
- [ ] FR-2: 支持 QA 样式输入，映射到 `q/a` 数据结构。
- [ ] FR-3: 支持本地草稿恢复和基础字符统计。

## Non-Functional Requirements
- [ ] NFR-1: 超长文本在提交前提示切分或限制。
- [ ] NFR-2: 表单切换模式时不丢草稿。

## API Contract
依赖 [../../api/dataset-collections.md](../../api/dataset-collections.md) 和 [../../api/dataset-data.md](../../api/dataset-data.md)。

## UI Description
页面提供“纯文本导入”和“QA 导入”切换。纯文本模式使用大文本框；QA 模式使用成对输入项列表。提交后跳回 collection 列表并定位到新条目。

## Data Model
- `ManualTextDraft(name, text, mode)`
- `QaDraftItem(q, a)`

## Architecture Notes
文本录入属于 collection 创建链路的一种来源，提交成功后不再停留在编辑页，而是回到 collection 管理页查看处理结果。

## Dependencies
- [collection-management.md](collection-management.md)

## Acceptance Criteria
- 纯文本和 QA 两种模式都可完整走通。
- 草稿在切后台后可恢复。
