---
status: draft
priority: P1
phase: 2-knowledge
---

# Search Test

## Overview
检索测试是 FastGPT 的调试闭环能力，允许用户验证召回、重排和扩展查询质量。

## Functional Requirements
- [ ] FR-1: 用户可输入测试问题并选择 `embedding/fullTextRecall/mixedRecall`。
- [ ] FR-2: 支持 similarity、embeddingWeight、re-rank、extension-query 等高级参数。
- [ ] FR-3: 展示结果列表、耗时、得分和扩展查询信息。

## Non-Functional Requirements
- [ ] NFR-1: 测试请求和常规聊天状态分离，互不影响。
- [ ] NFR-2: 结果列表可读且在手机端不拥挤。

## API Contract
依赖 [../../api/dataset-search.md](../../api/dataset-search.md)。

## UI Description
页面分为输入区和结果区。高级参数折叠在“更多选项”；结果区展示每条命中的标题、片段、得分标签和数据来源。失败时显示错误而保留上一次输入。

## Data Model
- `SearchTestState(input, params, results, status)`
- `SearchResultUiModel(title, snippet, scoreType, score, collectionId, dataId)`

## Architecture Notes
Search test 是知识 feature 独立屏，不与聊天页共享 ViewModel，但可复用同一引用展示组件。

## Dependencies
- [../../api/error-handling.md](../../api/error-handling.md)
- [../chat/citations.md](../chat/citations.md)

## Acceptance Criteria
- 基础检索和开启 re-rank 的结果都可展示。
- 结果与参数切换逻辑正确。
