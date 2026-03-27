---
status: draft
priority: P2
phase: 2-knowledge
---

# Text Input

## Overview
支持手动输入纯文本或问答对并导入知识库，适合补充零散知识片段。

## Functional Requirements
- [ ] FR-1: 支持纯文本与 QA 两种录入模式。
- [ ] FR-2: 支持草稿保留与再次编辑。
- [ ] FR-3: 提交后自动创建文本型 Collection 或数据块。

## Non-Functional Requirements
- [ ] NFR-1: 长文本编辑要避免输入丢失。
- [ ] NFR-2: 表单校验必须覆盖空内容和超长输入。

## API Contract
依赖 [../../api/dataset-collections.md](../../api/dataset-collections.md) 和 [../../api/dataset-data.md](../../api/dataset-data.md)。

## UI Description
页面顶部切换文本模式与 QA 模式；正文区支持多行输入；提交后展示处理结果和进入查看器入口。

## Data Model
- `ManualTextDraft`
- `QaInputPair`

## Architecture Notes
文本录入是知识导入最轻量的入口，建议与 Collection 创建流程共用表单状态和提交 use case。

## Dependencies
- Collection / data 接口
- 本地草稿缓存

## Acceptance Criteria
- 文本和 QA 两种模式都能成功导入。
- 草稿恢复和输入校验行为正确。

