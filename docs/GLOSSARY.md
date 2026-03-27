# Glossary

## 产品与平台
- Kora: 本项目，Android 原生知识库检索与 AI Chatbot 客户端。
- FastGPT: 提供聊天、知识库、工作流与应用管理能力的后端平台。
- Open WebUI: 作为交互体验与功能拆解的重要参考。

## Android 领域
- Compose: Android 声明式 UI 框架。
- Hilt: Android 官方推荐依赖注入方案。
- Room: SQLite 的类型安全抽象层。
- DataStore: 轻量键值与配置存储。
- ViewModel: 承载界面状态与业务调度的生命周期组件。

## AI 与检索
- RAG: Retrieval-Augmented Generation，先检索再生成。
- SSE: Server-Sent Events，聊天流式返回主要传输方式。
- Embedding Search: 基于向量相似度的检索。
- Full-text Search: 基于文本倒排的检索。
- Re-rank: 对候选检索结果做二次排序。
- Citation: 模型回答引用的来源片段或文档。

## FastGPT 概念
- App: 可被调用的聊天应用，包含模型、知识库与工作流配置。
- Dataset: 知识库容器。
- Collection: 数据导入单元，来源可为文本、链接、文件或 API。
- Chunk: 经过切分和处理后的知识片段。
- History: 会话级历史元数据。
- Record: 会话中的具体消息记录。
- Interactive Node: 需要用户继续输入或选择的工作流节点。

