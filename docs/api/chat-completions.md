# Chat Completions

## Endpoint
- `POST /api/v1/chat/completions`

## 用途
核心聊天接口，支持普通消息发送、流式响应、引用信息返回、工作流交互以及多轮上下文续接。

## 关键请求字段
- `chatId`: 既有会话 ID；新会话可为空。
- `stream`: 是否使用流式输出。
- `detail`: 是否返回更完整的引用和调试信息，Kora 默认开启。
- `messages`: 当前轮输入与上下文。
- `variables`: 应用变量、知识库参数或工作流入参。

## 关键响应语义
- 非流式模式返回完整 assistant 响应。
- 流式模式结合 [chat-streaming.md](chat-streaming.md) 中的事件类型处理。
- 成功响应可能包含新的 `chatId`，用于后续历史持久化。

## 客户端注意事项
- 只有拿到非空 `chatId` 后才持久化历史。
- 消息发送期间要维护可取消、可重试的本地临时状态。
- 需要把响应视为“聊天主链入口”，允许承载引用、工具、流程和交互节点，而不是只映射成 `assistant.content`。

## 上游实现模式参考
- FastGPT 在产品层把聊天、工作流和交互节点揉进一条对话链，因此 Kora 的请求和消息模型必须为结构化事件预留字段。
- Open WebUI 的聊天页经验说明，这些附属信息最终仍应回到单一消息流渲染，而不是拆成多个不相关页面。
- 详见 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md) 和 [../reference/open-webui-implementation-patterns.md](../reference/open-webui-implementation-patterns.md)。
