---
status: draft
priority: P1
phase: 1-mvp
---

# Markdown Rendering

## Overview
Kora 需要把 assistant 文本块渲染为可读、可复制、可滚动的富文本。交互模式参考 Open WebUI `Messages/Markdown.svelte`、`MarkdownTokens.svelte`、`KatexRenderer.svelte`、`CodeBlock.svelte`，但实现基于 Compose + Markwon。

## Functional Requirements
- [ ] FR-1: 支持标题、段落、列表、引用、表格、分隔线、链接、行内代码和 fenced code block。
- [ ] FR-2: 代码块提供语言标签、复制按钮、横向滚动和等宽字体。
- [ ] FR-3: 数学公式优先渲染 LaTeX，失败时回退为源码文本。
- [ ] FR-4: Mermaid 先以源码块或占位卡片降级，不阻塞 MVP。

## Non-Functional Requirements
- [ ] NFR-1: 长 Markdown 渲染必须运行在后台解析后再进入 Compose，避免主线程卡顿。
- [ ] NFR-2: 单条消息的异常标记不会导致整页崩溃，失败时回退为纯文本。

## API Contract
消息正文来自 [../../api/chat-completions.md](../../api/chat-completions.md) 和 [../../api/chat-records.md](../../api/chat-records.md) 中的 assistant `text` / `reasoning` 内容。

## UI Description
Markdown 渲染在消息气泡内部进行。正文块与推理块使用不同背景或层级；链接点击走系统浏览器确认；代码块使用固定高度预估并支持横向滚动；表格在窄屏时允许水平滚动容器；公式失败时展示源码和轻量提示。

## Data Model
- `RenderedRichText(blocks, parseState, rawMarkdown)`
- `CodeBlockUiModel(language, code, isCopied)`
- `MarkdownRenderPolicy(enableTables, enableLatex, enableMermaidFallback)`

## Architecture Notes
解析器与样式策略可下沉到 `:feature:chat` 的共享渲染层。消息列表只消费已经分块的 `RenderedRichText`。不要在 ViewModel 中拼 Compose AnnotatedString 细节；ViewModel 只持有原始文本和渲染状态。

## Dependencies
- [../../ui/design-system.md](../../ui/design-system.md)
- [../../ui/accessibility.md](../../ui/accessibility.md)

## Acceptance Criteria
- 常见技术问答 Markdown 在手机竖屏下可读且可复制。
- 代码块、表格、LaTeX 均有可用表现或明确降级。
