# FastGPT Implementation Patterns

## Snapshot
- 上游仓库: [labring/FastGPT](https://github.com/labring/FastGPT)
- 参考版本: [`v4.14.9.5`](https://github.com/labring/FastGPT/tree/v4.14.9.5)
- 参考日期: 2026-03-25
- 目标: 提取 Kora 对接 FastGPT 时必须理解的产品级实现模式，而不是只罗列 OpenAPI 端点。

## Module/Directory Map
- `projects/`: 主产品工程，`app` 是核心应用，旁边还有 marketplace、sandbox、mcp_server 等配套工程。[源码](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects)
- `projects/app/`: FastGPT 主应用入口，承载聊天、工作流、知识库和运营界面。[源码](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects/app)
- `packages/`: 多端复用依赖包，分为 `global`、`service`、`web` 三类。[源码](https://github.com/labring/FastGPT/tree/v4.14.9.5/packages)
- `sdk/`: 日志、可观测性、存储等基础 SDK。[源码](https://github.com/labring/FastGPT/tree/v4.14.9.5/sdk)
- `plugins/`: 模型和 webcrawler 等辅助子项目，属于服务端扩展背景而不是移动端直接实现目标。[源码](https://github.com/labring/FastGPT/tree/v4.14.9.5/plugins)

## Key Patterns

### Pattern 1: `projects/app` 承载产品能力主链
- 上游模块/目录: `projects`, `projects/app`
- 职责: 把聊天、工作流、知识库、运营和分享等能力整合到单一产品壳中。
- 关键实现模式: FastGPT 的接口设计不是独立微功能清单，而是围绕“应用、对话、知识库、运营”几条主链组织。
- Kora 直接可复用的决策: Android 侧也按主链拆 feature，而不是按每个端点单独建页面；聊天、知识库和设置仍然是第一层业务域。
- Kora 明确不复制的部分: 不复制管理后台、运营后台和大量桌面端工作台入口。
- 关联现有文档: [../architecture/module-structure.md](../architecture/module-structure.md), [../features/apps/app-selector.md](../features/apps/app-selector.md)
- 源码来源: [projects](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects), [projects/app](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects/app)

### Pattern 2: `packages/global|service|web` 区分领域模型、服务编排和前端适配
- 上游模块/目录: `packages`
- 职责: 用共享包沉淀全局模型、服务能力和 Web 端适配层。
- 关键实现模式: 产品逻辑不会全部堆在页面里，而是通过共享包把领域模型、调用层和端特定表现层拆开。
- Kora 直接可复用的决策: Kora 要把 DTO、领域模型、repository 和 UI 表现分层，不让 FastGPT 的产品复杂度直接泄漏到 Compose 页面。
- Kora 明确不复制的部分: 不复制 Web 专属适配层和 Next.js/Node 运行时假设。
- 关联现有文档: [../architecture/overview.md](../architecture/overview.md), [../architecture/data-flow.md](../architecture/data-flow.md)
- 源码来源: [packages](https://github.com/labring/FastGPT/tree/v4.14.9.5/packages)

### Pattern 3: 聊天链路把流式回答、工具、流程和交互节点揉在同一协议中
- 上游模块/目录: `projects/app`, `packages/service`
- 职责: 在聊天主链中承载 LLM 回复、工具调用、流程节点、引用和用户交互。
- 关键实现模式: `chat/completions` 不是“只返回文本”的简单接口，而是产品级事件总线入口。
- Kora 直接可复用的决策: 把 SSE 事件解析抽成独立流式层；消息模型要能容纳文本、流程、工具、交互节点和引用，而不是只放 `content: String`。
- Kora 明确不复制的部分: 不在移动端尝试复刻服务端工作流执行器，只消费事件结果。
- 关联现有文档: [../api/chat-completions.md](../api/chat-completions.md), [../api/chat-streaming.md](../api/chat-streaming.md), [../features/chat/streaming-chat.md](../features/chat/streaming-chat.md)
- 源码来源: [projects/app](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects/app), [OpenAPI docs](https://doc.fastgpt.io/docs/introduction/development/openapi/)

### Pattern 4: 交互节点是工作流产品能力，不是临时 UI 例外
- 上游模块/目录: `projects/app`, `packages/service`
- 职责: 让对话工作流可以中断，等待用户通过按钮或表单继续。
- 关键实现模式: `userSelect`、`userInput` 这类节点与普通消息共处于同一会话上下文，必须可恢复、可追溯。
- Kora 直接可复用的决策: Android 要把交互节点视为消息流的一等公民，绑定消息 ID、流程上下文和恢复状态。
- Kora 明确不复制的部分: 不复制桌面端复杂工作流调试 UI。
- 关联现有文档: [../api/chat-interactive.md](../api/chat-interactive.md), [../features/chat/interactive-nodes.md](../features/chat/interactive-nodes.md)
- 源码来源: [projects/app](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects/app), [README feature list](https://github.com/labring/FastGPT/tree/v4.14.9.5#readme)

### Pattern 5: 知识库导入围绕 Dataset -> Collection -> Chunk 链路组织
- 上游模块/目录: `projects/app`, `packages/service`
- 职责: 从数据集、导入源、训练状态到 chunk 编辑，形成清晰的知识处理链。
- 关键实现模式: Collection 来源、训练状态、Chunk 编辑、检索测试、引用回溯是同一知识链路的不同视角。
- Kora 直接可复用的决策: Android 的知识库模块必须显式建模 `Dataset`、`Collection`、`Chunk` 三层，而不是直接把“上传文档”视为单一步骤。
- Kora 明确不复制的部分: 不复制桌面后台式的高密度数据表和复杂批量管理交互。
- 关联现有文档: [../api/dataset-collections.md](../api/dataset-collections.md), [../features/knowledge/document-upload.md](../features/knowledge/document-upload.md), [../features/knowledge/chunk-viewer.md](../features/knowledge/chunk-viewer.md)
- 源码来源: [projects/app](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects/app), [README knowledge features](https://github.com/labring/FastGPT/tree/v4.14.9.5#readme)

### Pattern 6: 检索测试和引用反馈属于可调试产品闭环
- 上游模块/目录: `projects/app`, `packages/service`
- 职责: 让用户验证知识召回质量，并把引用展示回接到聊天结果。
- 关键实现模式: 检索测试、引用来源和结果解释在 FastGPT 中是面向产品使用者的调试闭环，而不是后端内部工具。
- Kora 直接可复用的决策: Kora 要保留“检索测试 + 聊天引用”这两个互相印证的入口，便于移动端验证知识质量。
- Kora 明确不复制的部分: 不复制完整应用评测和节点日志后台。
- 关联现有文档: [../features/knowledge/search-test.md](../features/knowledge/search-test.md), [../features/chat/citations.md](../features/chat/citations.md)
- 源码来源: [README debugging and knowledge features](https://github.com/labring/FastGPT/tree/v4.14.9.5#readme), [OpenAPI docs](https://doc.fastgpt.io/docs/introduction/development/openapi/)

### Pattern 7: `sdk` 和 `plugins` 是服务端基础设施，不是客户端功能需求
- 上游模块/目录: `sdk`, `plugins`
- 职责: `sdk` 提供 logger、otel、storage 等基础设施，`plugins` 提供模型和 webcrawler 等扩展能力。
- 关键实现模式: 产品能力背后有完整服务端基础设施层，但移动端只需要知道这些能力会影响接口行为和数据形态。
- Kora 直接可复用的决策: Kora 只为这些能力预留字段和错误处理，不直接实现 OTEL、对象存储或 crawler。
- Kora 明确不复制的部分: 服务端日志、观测、对象存储和私有模型插件实现。
- 关联现有文档: [../api/error-handling.md](../api/error-handling.md), [../architecture/networking.md](../architecture/networking.md)
- 源码来源: [sdk](https://github.com/labring/FastGPT/tree/v4.14.9.5/sdk), [plugins](https://github.com/labring/FastGPT/tree/v4.14.9.5/plugins)

### Pattern 8: 分享和运营属于独立会话入口
- 上游模块/目录: `projects/app`
- 职责: 支持免登录分享窗口、统一查看记录和运营日志等运营能力。
- 关键实现模式: 分享态不是普通登录态的别名，而是独立的访问入口和临时认证上下文。
- Kora 直接可复用的决策: Android 的分享链接流程必须和主连接配置隔离，使用独立 session 和深层链接入口。
- Kora 明确不复制的部分: 不复制完整运营日志后台和标注平台。
- 关联现有文档: [../api/share-auth.md](../api/share-auth.md), [../features/auth/share-link-auth.md](../features/auth/share-link-auth.md)
- 源码来源: [README operations features](https://github.com/labring/FastGPT/tree/v4.14.9.5#readme), [OpenAPI docs](https://doc.fastgpt.io/docs/introduction/development/openapi/)

## Kora Adaptation Decisions
- 对接 FastGPT 时，以“聊天主链、知识库主链、应用主链、分享主链”四条产品链组织文档和模块，而不是按接口名拆散。
- 聊天消息模型必须支持文本、工具、流程、交互节点、引用和最终收尾事件。
- 知识库模块必须显式建模 Dataset、Collection、Chunk 和 Search Test，才能和引用、上传、训练状态形成闭环。
- 分享态与主认证态隔离，避免污染全局连接配置。

## Not To Copy
- 不复制 FastGPT 的服务端工作流执行、沙盒、日志与对象存储实现。
- 不复制桌面后台和运营后台的大量管理页面。
- 不把 `sdk` / `plugins` 误当成 Android 侧必须实现的功能模块。
- 不把 OpenAPI 当成唯一真相；Kora 还需要理解它背后的产品链和状态语义。

## Source Trace
- GitHub Repo: [labring/FastGPT](https://github.com/labring/FastGPT)
- Tag Snapshot: [`v4.14.9.5`](https://github.com/labring/FastGPT/tree/v4.14.9.5)
- Root Tree: [repo tree](https://github.com/labring/FastGPT/tree/v4.14.9.5)
- Projects: [projects](https://github.com/labring/FastGPT/tree/v4.14.9.5/projects)
- Packages: [packages](https://github.com/labring/FastGPT/tree/v4.14.9.5/packages)
- SDK: [sdk](https://github.com/labring/FastGPT/tree/v4.14.9.5/sdk)
- Plugins: [plugins](https://github.com/labring/FastGPT/tree/v4.14.9.5/plugins)
- Official Docs: [doc.fastgpt.io](https://doc.fastgpt.io/)
- OpenAPI Docs: [FastGPT OpenAPI](https://doc.fastgpt.io/docs/introduction/development/openapi/)

