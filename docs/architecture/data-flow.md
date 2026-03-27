# Data Flow

## 单向数据流
- UI 触发 `Event`
- ViewModel 处理意图并调用 use case / repository
- Repository 返回 `Flow<Result<T>>`
- ViewModel 归并为单一 `UiState`
- Compose 根据 `UiState` 渲染

## 离线策略
- 会话列表、消息记录、设置和最近打开的知识库优先读本地缓存。
- 聊天发送、知识库写操作采用远端优先，成功后回写本地。
- 流式聊天在本地维护临时消息草稿，流结束后再转持久化状态。

## 同步策略
- 进入页面时拉取远端最新摘要并合并本地。
- 出错时保留本地可展示内容，向 UI 提供局部降级状态。

## 上游实现模式参考
- Open WebUI 倾向把聊天中的引用、附件、工具状态和消息本体一起组织，因此 Kora 也将这些附属状态绑定到消息流，而不是拆成多个页面级状态机。
- FastGPT 的聊天链路同时承载回答、工具、流程和交互节点，说明 Kora 的 `UiState` 不能只建模纯文本消息，必须允许事件型与结构化消息共存。
- 详见 [../reference/open-webui-implementation-patterns.md](../reference/open-webui-implementation-patterns.md) 和 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md)。
