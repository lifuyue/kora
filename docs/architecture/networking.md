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

## 上游实现模式参考
- FastGPT 的聊天接口实际上是产品级事件入口，SSE 里既有文本也有流程、工具和交互节点，因此 Kora 需要单独的事件解析层，而不是把流式响应当普通字符串流。
- Open WebUI 的服务端聚合方式说明客户端应尽量消费稳定的聚合结果，不在 UI 层重新拼接网络语义。
- 详见 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md) 和 [../reference/open-webui-implementation-patterns.md](../reference/open-webui-implementation-patterns.md)。
