# Module Structure

## 规划模块
- `:app`: Application、导航装配、全局主题与入口 Activity。
- `:core:common`: 通用工具、Result 包装、日志、时间与资源适配。
- `:core:network`: Retrofit、OkHttp、SSE、DTO 与认证。
- `:core:database`: Room、DAO、Entity、迁移。
- `:core:model`: 领域模型与枚举。
- `:feature:chat`: 聊天、消息渲染、引用、交互式节点。
- `:feature:knowledge`: 知识库、Collection、Chunk 管理。
- `:feature:settings`: 连接配置、主题、缓存与语言。

## 依赖方向
- `app -> feature -> core`
- `feature` 之间避免直接依赖，公共能力下沉到 `core`。
- `core:network` 与 `core:database` 不互相感知，由 repository 在 feature 层或 data 层桥接。

## 拆分原则
- 以业务域为边界，而不是按页面随意切分。
- 可测试逻辑优先放入 use case/repository，而不是 Compose 层。

## 上游实现模式参考
- Open WebUI 的 `src/lib` 与 `src/routes` 分层说明“共享能力先下沉、页面负责组装”更适合长期扩展，因此 Kora 把消息渲染、引用、附件等能力放在 feature 内共享层而不是分散进 screen。
- FastGPT 的 `projects` 与 `packages` 分层说明产品主链和共享服务要分开，因此 Kora 继续坚持 `feature -> core` 方向，不把 FastGPT 的产品复杂度直接泄漏到 `app` 模块。
- 详见 [../reference/open-webui-implementation-patterns.md](../reference/open-webui-implementation-patterns.md) 和 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md)。
