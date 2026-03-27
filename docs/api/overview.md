# API Overview

## 范围
Kora 当前面向 FastGPT 的聊天、会话历史、消息记录、知识库、应用管理、文件上传、分享认证与推荐问题能力。

## 端点分组速查
- 认证: [authentication.md](authentication.md)
- 核心聊天: [chat-completions.md](chat-completions.md)
- 历史与记录: [chat-history.md](chat-history.md), [chat-records.md](chat-records.md)
- 流式事件: [chat-streaming.md](chat-streaming.md), [chat-interactive.md](chat-interactive.md)
- 知识库: [dataset-management.md](dataset-management.md), [dataset-collections.md](dataset-collections.md), [dataset-data.md](dataset-data.md), [dataset-search.md](dataset-search.md)
- 应用: [app-management.md](app-management.md)
- 文件与辅助: [file-upload.md](file-upload.md), [question-guide.md](question-guide.md), [share-auth.md](share-auth.md)
- 错误: [error-handling.md](error-handling.md)

## 客户端约束
- 所有需要鉴权的请求统一走 Bearer Token。
- 聊天相关请求优先支持 `stream=true`。
- 需要引用和附加信息时默认带 `detail=true`。

