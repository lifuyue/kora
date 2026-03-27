# Authentication

## 认证方式
- 使用 Bearer Token / API Key。
- 客户端应校验 `fastgpt-` 前缀，减少明显无效输入。

## 存储策略
- API Key 持久化到安全存储层，再通过 DataStore 持有最小配置镜像。
- Base URL、环境标签、最近连通结果可存 DataStore。

## 客户端行为
- 未配置凭证时，阻止进入需要远端请求的功能。
- 401/403 统一触发鉴权失败状态，提示用户检查 URL 与 Key。

