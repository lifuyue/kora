# Share Auth

## 目标
支持分享链接三步认证流程，使外部打开的分享内容能够在客户端完成校验与展示。

## 建议三步
1. 解析深层链接中的分享参数。
2. 请求分享鉴权或访问令牌。
3. 用临时凭证拉取分享详情或进入分享会话。

## 客户端要求
- 分享态与常规登录态隔离，避免污染主账号会话。
- 认证失败时提供返回首页或重新打开链接的出口。

## 上游实现模式参考
- FastGPT 的分享能力属于独立入口和临时认证上下文，因此 Kora 不应把分享令牌写回主连接配置。
- 详见 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md)。
