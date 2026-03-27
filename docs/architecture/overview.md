# Architecture Overview

## 目标
Kora 采用面向 Android 原生的分层架构，围绕聊天、知识库与设置三大域构建，强调可扩展模块边界、流式响应处理与离线优先的用户体验。

## C4 文字描述
- System: Kora Android App 通过 HTTPS 与 FastGPT API 通信。
- Container: `:app` 负责入口与导航，`core` 模块承载网络、数据库、公共能力，`feature` 模块承载业务域。
- Component: 每个 feature 由 screen、viewmodel、use case、repository 协同完成。
- Code: UI 层使用 Compose，状态层使用 ViewModel + StateFlow，数据层通过 repository 协调网络与本地缓存。

## 关键原则
- 单向数据流，UI 只消费 state 并发出 event。
- 网络、数据库、文件缓存彼此隔离，通过 repository 编排。
- 聊天与知识库能力按 feature 拆分，避免 `app` 模块承载业务细节。

