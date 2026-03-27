---
status: draft
priority: P2
phase: 2-knowledge
---

# Conversation Pin

## Overview
FastGPT 自带 `top` 字段，Kora 需要同时支持服务端 `top` 和本地 pin 体验。默认策略是尊重服务端 `top`，本地 pin 作为客户端扩展排序层。

## Functional Requirements
- [ ] FR-1: 用户可本地 pin / unpin 会话。
- [ ] FR-2: 服务端 `top=true` 的会话天然进入顶部组。
- [ ] FR-3: 当本地 pin 与服务端 top 同时存在时，统一归入顶部组并按最近更新时间排序。

## Non-Functional Requirements
- [ ] NFR-1: pin 状态切换即时生效。
- [ ] NFR-2: 不修改服务端原始更新时间语义。

## API Contract
基础数据依赖 [../../api/chat-history.md](../../api/chat-history.md) 的 `top` 字段；本地 pin 不回写服务端。

## UI Description
列表项菜单展示“置顶到本机/取消置顶”。顶部组标题统一为“重要会话”。服务端 top 可用小图标区分“服务端置顶”和“本地置顶”。

## Data Model
- `PinnedConversationEntity(chatId, pinnedAt)`
- `ConversationTopState(serverTop, localPinned)`

## Architecture Notes
Repository 在 merge 阶段把服务端 `top` 和本地 pin 组装成同一个排序字段，UI 不区分数据来源，只在详情面板展示来源说明。

## Dependencies
- [conversation-list.md](conversation-list.md)

## Acceptance Criteria
- 本地 pin 与服务端 top 都能正确排序。
- 重启后本地 pin 状态保持。
