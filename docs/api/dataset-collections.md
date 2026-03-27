# Dataset Collections

## 范围
Collection 是知识导入单元，支持以下来源：
- `text`
- `link`
- `file`
- `api`

## 典型能力
- 创建 Collection
- 列表与详情
- 删除
- 训练状态查询

## 客户端要求
- 不同来源的入参结构不同，UI 表单需按来源动态切换。
- 训练状态需要轮询或刷新，向用户明确展示处理中、成功、失败。

## 上游实现模式参考
- FastGPT 的知识链路是 `Dataset -> Collection -> Chunk`，Collection 只是导入单元，不是最终知识本体，因此 Kora 也要把来源、训练状态和 chunk 查看拆开建模。
- Open WebUI 的经验说明，知识导入最终要服务聊天引用与上下文增强，不能把导入页面设计成孤立后台。
- 详见 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md) 和 [../reference/open-webui-implementation-patterns.md](../reference/open-webui-implementation-patterns.md)。
