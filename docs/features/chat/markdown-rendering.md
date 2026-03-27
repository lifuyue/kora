---
status: draft
priority: P1
phase: 1-mvp
---

# Markdown Rendering

## Overview
让模型回答中的 Markdown、代码块、LaTeX 和 Mermaid 在 Android 端可读、可复制、可滚动，保持技术问答场景的可用性。

## Functional Requirements
- [ ] FR-1: 支持标题、列表、表格、引用、链接和行内代码。
- [ ] FR-2: 支持 fenced code block 的语法高亮和复制。
- [ ] FR-3: 支持 LaTeX 和 Mermaid 的降级渲染或占位展示。

## Non-Functional Requirements
- [ ] NFR-1: 长消息渲染不能明显阻塞主线程。
- [ ] NFR-2: 对恶意超长 Markdown 输入有安全防护和渲染上限。

## API Contract
消息内容来自 [../../api/chat-completions.md](../../api/chat-completions.md) 与 [../../api/chat-records.md](../../api/chat-records.md)。

## UI Description
消息气泡内部按块级内容渲染；代码块需要横向滚动和复制按钮；公式与 Mermaid 在不支持原生交互时提供清晰占位；错误状态下回退为纯文本展示。

## Data Model
- `RenderedMessageContent`
- `CodeBlockUiModel`
- `RichContentFallbackMode`

## Architecture Notes
渲染层位于 `:feature:chat` UI 子层，解析器可下沉到 `:core:common` 以便复用，避免将富文本解析逻辑放入 ViewModel。

## Dependencies
- Markwon
- Compose text/layout
- 剪贴板能力

## Acceptance Criteria
- 常见 Markdown 内容在手机竖屏下可稳定阅读。
- 代码块复制、链接点击和超长内容滚动行为正常。

