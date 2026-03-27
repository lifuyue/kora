---
status: draft
priority: P1
phase: 2-knowledge
---

# Citations

## Overview
Kora 需要把 FastGPT 返回的 `flowResponses` / `responseData` 中的引用信息，以 Open WebUI `Messages/Citations.svelte` 风格映射成消息内引用入口与底部详情面板。

## Functional Requirements
- [ ] FR-1: 当 assistant 响应含引用时，在消息尾部显示引用入口和数量摘要。
- [ ] FR-2: 用户可展开查看文档标题、命中片段、来源 collection、得分类型等元数据。
- [ ] FR-3: 用户可从引用面板跳转到知识库上下文，如 collection 或 chunk 查看器。

## Non-Functional Requirements
- [ ] NFR-1: 引用详情加载或展开不能阻塞主消息列表滚动。
- [ ] NFR-2: `showCite=false` 或 out-link 脱敏模式下必须正确隐藏/裁剪详情。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md)、[../../api/chat-records.md](../../api/chat-records.md) 和 [../../api/dataset-search.md](../../api/dataset-search.md)。

## UI Description
assistant 消息底部显示“引用 n 条”入口。点击后打开底部面板，列出来源卡片；卡片包含标题、摘要片段、得分标签和“查看原文”动作。无引用时不预留空白。被分享态或权限裁剪的引用显示降级说明。

## Data Model
- `CitationSummary(count, hiddenByPolicy)`
- `CitationItem(datasetId?, collectionId?, dataId?, title, snippet, scoreType, score)`
- `CitationPanelState(messageDataId, status, items)`

## Architecture Notes
主消息列表持有 `CitationSummary`，完整详情按需由 `getResData` 或已有 `flowResponses` 补齐。引用是消息的一部分，但跳转到 chunk 查看器由知识库 feature 处理。

## Dependencies
- [../../features/knowledge/chunk-viewer.md](../knowledge/chunk-viewer.md)
- [../../features/knowledge/search-test.md](../knowledge/search-test.md)

## Acceptance Criteria
- 有引用的回答一定能看到入口。
- 展开面板后能看到来源详情并进行跳转或降级提示。
