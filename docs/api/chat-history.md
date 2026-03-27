# Chat History

## 关注接口
- `getHistories`
- `delHistory`
- `clearHistories`

## 目标
管理会话级摘要信息，包括列表展示、删除单个历史和清空某应用下的全部历史。

## 客户端映射
- 会话列表页读取 `getHistories`。
- 删除、清空操作要与本地 Room 同步。
- 置顶、标签、文件夹等客户端扩展字段由本地表维护，不直接依赖服务端。

