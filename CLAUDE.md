# Kora Agent Guide

## 项目简介
Kora 是一个 Android 原生 Kotlin 应用，目标是提供面向知识库检索与 AI Chatbot 的移动端体验，交互参考 Open WebUI，能力对齐 FastGPT API 与工作流能力。

## 快速导航
- 全局地图: [docs/INDEX.md](docs/INDEX.md)
- 术语表: [docs/GLOSSARY.md](docs/GLOSSARY.md)
- 约定: [docs/CONVENTIONS.md](docs/CONVENTIONS.md)
- 项目记忆: `docs/memory/working-rules.md`、`docs/memory/mistakes.md`、`docs/memory/solutions.md`

## 技术栈速览
- Kotlin 2.x
- Jetpack Compose
- Hilt
- Retrofit + OkHttp
- Room
- DataStore
- Markwon
- Coil

## 源码模块结构
- `:app`
- `:core:network`
- `:core:database`
- `:core:common`
- `:core:testing`
- `:feature:chat`
- `:feature:knowledge`
- `:feature:settings`

## 核心开发约定摘要
- 架构采用 MVVM + UDF，状态单向流动。
- 异步能力优先使用协程、Flow 与结构化并发。
- Kotlin 代码风格以 `ktlint` 为基线。
- 面向用户的文案必须资源化，避免硬编码字符串。
- 项目处于快速迭代阶段，允许激进重构与实现替换。
- 除非任务明确要求兼容，否则不需要兼容旧数据和历史持久化格式。

## Agent 查找指南
- 要实现功能规格: 先读 `docs/features/`
- 要对接接口: 先读 `docs/api/`
- 要理解架构: 先读 `docs/architecture/`
- 要确认视觉与交互: 先读 `docs/ui/`
- 要理解上游来源与实现模式: 先读 `docs/reference/open-webui-implementation-patterns.md` 和 `docs/reference/fastgpt-implementation-patterns.md`
- 要看当前里程碑与测试收敛目标: 先读 `docs/roadmap/milestones.md`（原 phase-1/2/3 功能阶段文档已不再维护）
- 要避免重复踩坑或复用已有方案: 先读 `docs/memory/`

## Agent 工作流
1. 阅读本文件，确认项目边界与约定。
2. 阅读 [docs/INDEX.md](docs/INDEX.md) 定位目标文档。
3. 阅读对应功能 spec。
4. 阅读相关 API spec。
5. 阅读架构与数据流文档。
6. 如涉及历史坑点、重构决策或实现策略，补读 `docs/memory/`。
7. 再开始实现、测试与补充文档。
8. 如果出现高成本错误、稳定规则或可复用解法，追加到对应的 memory 文档中，保持条目精简。

## 关键注意事项
- FastGPT API Key 需要识别并校验 `fastgpt-` 前缀。
- 聊天流式返回依赖 SSE，自定义事件解析不可省略。
- 需要 `detail=true` 才能稳定拿到引用与更完整的响应附加信息。
- 只有 `chatId` 非空时才应持久化历史，避免产生孤立会话。
