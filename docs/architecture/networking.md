# Networking

## 基础配置
- 统一基于 OkHttpClient 注入超时、日志、认证与重试策略。
- Bearer Token 从安全存储读取并通过拦截器注入。
- Retrofit 处理常规 REST，SSE 使用自定义流式读取层。

## SSE 处理
- 不能只依赖 Retrofit converter，需要对事件边界、自定义 event 字段与增量 payload 做解析。
- 流式事件需映射为领域层 `StreamEvent`，交给聊天 ViewModel 顺序消费。

## 拦截器建议
- AuthInterceptor: 注入 API Key。
- CommonQueryInterceptor: 为需要引用信息的请求追加 `detail=true`。
- RetryInterceptor: 针对幂等请求做有限重试。

