# Chat Records

## 关注接口
- `init`
- `getPaginationRecords`
- `getResData`

## 目标
获取具体会话消息记录、分页加载旧消息，并在必要时读取结构化响应附加数据。

## 客户端策略
- 首次进入会话详情时调用 `init` 获取页面初始化数据。
- 上滑分页采用 `getPaginationRecords`，避免一次性拉全量消息。
- 当消息需要查看引用、工作流结果或调试数据时，通过 `getResData` 补齐。

