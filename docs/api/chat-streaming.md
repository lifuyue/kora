# Chat Streaming

## 范围
聊天流式返回通过 SSE 传递，客户端需要处理至少以下 9 类事件：
- `answer`
- `toolCall`
- `toolResult`
- `flowResponses`
- `moduleStatus`
- `interactive`
- `citation`
- `error`
- `finish`

## 解析要求
- 按事件边界顺序消费，不能假设所有 payload 都是文本。
- `answer` 事件用于增量拼接可见回复。
- `toolCall`、`toolResult` 和 `flowResponses` 用于展示工作流与工具执行过程。
- `interactive` 事件需转交 [chat-interactive.md](chat-interactive.md) 处理。
- `finish` 到达后统一落盘与收尾。

## 健壮性要求
- 断流时要区分可恢复网络错误与业务失败。
- 解析异常不能直接崩溃 UI，应记录原始片段以便排查。

