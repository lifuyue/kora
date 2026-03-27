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

