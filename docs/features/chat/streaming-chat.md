---
status: draft
priority: P0
phase: 1-mvp
---

# Streaming Chat

## Overview
提供 Kora 的核心对话主链。聊天页必须在一个消息流中同时承载文本增量、推理文本、工具状态、流程节点、引用和交互节点，行为参考 Open WebUI `src/lib/components/chat/Chat.svelte` 的 `chatCompletionEventHandler`，协议真值来自 FastGPT `chat/completions` 与 SSE 事件枚举。

## Functional Requirements
- [ ] FR-1: 用户发送消息后立即插入本地 `Human` 消息和一个带 `responseChatItemId` 的占位 `AI` 消息。
- [ ] FR-2: `answer`/`fastAnswer` 增量更新同一条 assistant 消息的正文和 `reasoning` 子块。
- [ ] FR-3: `toolCall`、`toolParams`、`toolResponse`、`plan`、`stepTitle`、`interactive`、`collectionForm`、`topAgentConfig` 都合并到同一 assistant 消息的结构化子块。
- [ ] FR-4: `flowResponses` 到达后补齐引用和节点响应详情；`updateVariables` 到达后更新会话变量状态。
- [ ] FR-5: 流结束后将临时消息转为持久化消息；失败时保留已收到内容并暴露“重试/继续生成”操作。

## Non-Functional Requirements
- [ ] NFR-1: 首个可见 token 在网络正常时应尽快出现，首包等待超时由上层 watchdog 约束在 60s。
- [ ] NFR-2: 事件顺序必须严格保留，不能因 UI 动画或分页回收打乱。
- [ ] NFR-3: 流解析失败、断流、进程重建后都不能导致已收到内容丢失。

## API Contract
依赖 [../../api/chat-completions.md](../../api/chat-completions.md)、[../../api/chat-streaming.md](../../api/chat-streaming.md) 和 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
聊天页由顶部 app 标题栏、消息列表、底部输入区组成。发送后列表尾部追加用户气泡和 assistant 占位卡片；正文增长时自动滚动到最新位置，但用户手动上滑后暂停强制滚动。工具调用显示为消息内状态条；推理文本默认折叠；发生错误时保留气泡并展示 inline error；成功完成后在消息尾部展示推荐问题和引用入口。

## Data Model
- `ChatUiState(chatId, appId, messages, inputDraft, sendState, autoScrollEnabled, pendingInteractive, pendingSuggestions)`
- `ChatMessageUiModel(dataId?, responseValueId?, role, blocks, sendStatus, error)`
- `AssistantBlock(type=text|reasoning|tool|interactive|plan|stepTitle|citationSummary)`
- `PendingSendDraft(localId, responseChatItemId, startedAt, retryContext)`

## Architecture Notes
`ChatViewModel` 只处理 intent 和 state reduce；SSE 解析在 `:core:network`；消息合并逻辑在 `ChatRepository`。历史分页与流式尾插要共用同一消息排序键：`time + dataId`。Open WebUI 的 `Chat.svelte` 说明客户端必须把所有附属事件都归并到单一消息流中；FastGPT 的 `SseResponseEventEnum` 说明协议本身就是产品级事件总线。

## Dependencies
- [../../architecture/data-flow.md](../../architecture/data-flow.md)
- [../../architecture/networking.md](../../architecture/networking.md)
- [../../ui/component-catalog.md](../../ui/component-catalog.md)

## Acceptance Criteria
- 可在新会话和已有会话中稳定流式发送并看到增量回复。
- 工具、计划、交互和引用不会被渲染成独立页面或丢失。
- 断流后保留部分答案并可重试。
