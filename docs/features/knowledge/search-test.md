---
status: draft
priority: P1
phase: 2-knowledge
---

# Search Test

## Overview
为知识库管理员提供检索测试工具，直接验证不同检索模式与 re-rank 的实际效果。

## Functional Requirements
- [ ] FR-1: 用户可输入查询并选择 `embedding/fullText/mixed`。
- [ ] FR-2: 用户可切换 `reRank` 并查看结果变化。
- [ ] FR-3: 结果列表展示相似度、来源和命中文本。

## Non-Functional Requirements
- [ ] NFR-1: 多次测试请求要支持取消过期查询。
- [ ] NFR-2: 相似度数值和排序需要稳定、可比较。

## API Contract
依赖 [../../api/dataset-search.md](../../api/dataset-search.md)。

## UI Description
页面由查询输入框、模式选择器、re-rank 开关和结果列表组成；加载中显示骨架，空结果提示优化数据集或检索参数。

## Data Model
- `SearchTestQuery`
- `SearchTestResult`
- `SearchMode`

## Architecture Notes
检索测试属于工具型页面，不直接影响聊天主流程，但应与引用数据模型保持字段一致，便于对照验证。

## Dependencies
- 检索测试接口
- 表单状态管理

## Acceptance Criteria
- 三种检索模式都能发起并展示结果。
- re-rank 开关对结果变化可见。

