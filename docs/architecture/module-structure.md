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

