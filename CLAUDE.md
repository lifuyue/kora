# Kora Agent Guide

## 项目简介
Kora 是一个 Android 原生 Kotlin 应用，目标是提供面向知识库检索与 AI Chatbot 的移动端体验，交互参考 Open WebUI，能力对齐 FastGPT API 与工作流能力。

## 快速导航
- 全局地图: [docs/INDEX.md](docs/INDEX.md)
- 术语表: [docs/GLOSSARY.md](docs/GLOSSARY.md)
- 约定: [docs/CONVENTIONS.md](docs/CONVENTIONS.md)

## 技术栈速览
- Kotlin 2.x
- Jetpack Compose
- Hilt
- Retrofit + OkHttp
- Room
- DataStore
- Markwon
- Coil

## 规划中的源码目录结构
- `:app`
- `:core:network`
- `:core:database`
- `:core:common`
- `:feature:chat`
- `:feature:knowledge`
- `:feature:settings`

## 核心开发约定摘要
- 架构采用 MVVM + UDF，状态单向流动。
- 异步能力优先使用协程、Flow 与结构化并发。
- Kotlin 代码风格以 `ktlint` 为基线。
- 面向用户的文案必须资源化，避免硬编码字符串。

## Agent 查找指南
- 要实现功能规格: 先读 `docs/features/`
- 要对接接口: 先读 `docs/api/`
- 要理解架构: 先读 `docs/architecture/`
- 要确认视觉与交互: 先读 `docs/ui/`
- 要理解上游来源与实现模式: 先读 `docs/reference/open-webui-implementation-patterns.md` 和 `docs/reference/fastgpt-implementation-patterns.md`
- 要看阶段计划: 先读 `docs/roadmap/`

## Agent 工作流
1. 阅读本文件，确认项目边界与约定。
2. 阅读 [docs/INDEX.md](docs/INDEX.md) 定位目标文档。
3. 阅读对应功能 spec。
4. 阅读相关 API spec。
5. 阅读架构与数据流文档。
6. 再开始实现、测试与补充文档。

## 关键注意事项
- FastGPT API Key 需要识别并校验 `fastgpt-` 前缀。
- 聊天流式返回依赖 SSE，自定义事件解析不可省略。
- 需要 `detail=true` 才能稳定拿到引用与更完整的响应附加信息。
- 只有 `chatId` 非空时才应持久化历史，避免产生孤立会话。
