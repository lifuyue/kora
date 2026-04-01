# Kora

Kora 是一个基于 Kotlin + Jetpack Compose 的 Android AI 工作台，当前主线能力已经覆盖：
- 聊天与流式回复
- 会话列表、归档、标签、导出
- 知识库浏览、导入、检索测试
- 设置、连接配置、语言、主题、音频偏好

项目现阶段的重点不再是继续铺更多阶段性规划，而是围绕已有功能收敛实现、补齐测试、降低回归成本。

## 文档入口
- 仓库协作约束: [AGENTS.md](AGENTS.md)
- Agent 入口: [CLAUDE.md](CLAUDE.md)
- 全局索引: [docs/INDEX.md](docs/INDEX.md)
- 架构概览: [docs/architecture/overview.md](docs/architecture/overview.md)
- 测试策略: [docs/architecture/testing-strategy.md](docs/architecture/testing-strategy.md)
- 测试优化路线图: [docs/roadmap/testing-optimization.md](docs/roadmap/testing-optimization.md)

## 当前技术栈
- Kotlin 2.x
- Jetpack Compose
- Hilt
- Retrofit + OkHttp
- Room
- DataStore
- Coil

## 开发与回归
- 提交前最小链路: `./gradlew testDebugUnitTest assembleDebug`
- 仅聊天模块回归: `./gradlew :feature:chat:testDebugUnitTest`
- 安装到当前模拟器: `./gradlew installDebug`
- 代码风格检查: `./gradlew ktlintCheck`
- 生成本地检索基准数据: `./scripts/generate_local_knowledge_benchmark.py`
- Git Hook: `git config core.hooksPath .githooks`

## 当前方向
- 优先修复真实回归和状态不一致问题
- 优先补 repository / viewmodel / screen 测试，而不是追加新的阶段性 spec
- 设备联调作为最后验收，不替代自动化回归
