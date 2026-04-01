# Repository Guidelines

## 项目结构与模块组织
Kora 是一个基于 Kotlin 与 Jetpack Compose 的多模块 Android 应用。`app/` 存放应用入口、导航、主题和 Android 资源。共享能力位于 `core/`：`core/common` 放基础模型与工具，`core/network` 负责 Retrofit/OkHttp 网络栈，`core/database` 负责 Room/DataStore 持久化，`core/testing` 提供通用测试支撑。面向用户的功能模块位于 `feature/chat`、`feature/settings` 和 `feature/knowledge`。产品与架构文档位于 `docs/`；改动功能前先检查 `docs/features/`、`docs/api/` 与 `docs/architecture/`。

## 迭代阶段约束
项目仍处于快速迭代阶段，默认优先交付清晰、可维护的实现，而不是保留历史包袱。允许激进重构、模块重组和实现替换；除非任务明确要求兼容，否则不为旧数据结构、旧持久化格式或临时过渡层背负兼容成本。面对复杂兼容逻辑时，优先选择删除、重建和收敛方案，而不是叠加补丁。

## 构建、测试与开发命令
所有命令都从仓库根目录通过 Gradle Wrapper 执行。

- `./gradlew assembleDebug`：构建调试版应用。
- `./gradlew testDebugUnitTest`：运行主回归单元测试集。
- `./gradlew ktlintCheck`：对所有模块执行 Kotlin 格式与静态检查。
- `./scripts/ktlint-check.sh`：Git Hook 使用的 `ktlint` 包装脚本。
- `git config core.hooksPath .githooks`：启用仓库内置的 pre-commit Hook。

README 中定义的最小提交前回归链路是 `./gradlew testDebugUnitTest assembleDebug`。

Makefile 定义了以下快捷命令（`make ad-run` / `make ad-debug` 需要连接模拟器或真机）：

- `make ad-run`：构建调试包并安装运行（普通模式）。
- `make ad-debug`：构建调试包并安装运行（debug-ui 模式）。
- `make seed`：生成本地知识库基准测试数据（运行 `scripts/generate_local_knowledge_benchmark.py`）。

## 代码风格与命名约定
Kotlin 使用 4 空格缩进，XML 使用 2 空格。包名保持在 `com.lifuyue.kora.*` 下，文件名应与主要类型或页面一致，例如 `ChatScreen.kt`、`ConnectionRepository.kt`。Compose UI 状态必须显式覆盖 loading、empty、error、success。用户可见文案统一放入 `app/src/main/res/values/strings.xml`，不要在代码中硬编码。文档文件名使用 `kebab-case`，并尽量将 `docs/` 目录深度控制在 3 层以内。

## 项目记忆文档
`docs/memory/` 是供 agent 和贡献者使用的项目级记忆区，只在本文件和 `CLAUDE.md` 中暴露，不进入主文档索引。开始任务前，如问题涉及重构策略、历史坑点或已有解法，先检查对应文档：

- `docs/memory/working-rules.md`：稳定有效的项目执行规则。
- `docs/memory/mistakes.md`：高成本、易重复踩坑的错误记录。
- `docs/memory/solutions.md`：已验证、可复用的解决方案。

只有高价值记忆才应写入这里：会重复发生、代价高、能直接减少后续决策成本的事项。不要记录琐碎过程、临时讨论或流水账。出现新的坑或形成复用方案后，用短条目追加，保持可搜索、可执行。

## 测试规范
测试代码按模块放在各自的 `src/test/java/...` 下，复用的 consumer 测试支撑由 `core/testing` 接入；写测试前先复用该模块的工具类：`MainDispatcherRule`（协程调度器）、`MockWebServerRule`（MockWebServer JUnit Rule）、`RoomTestFactory`（Room 内存数据库）、`DataStoreTestFactory`、`FixedClock`、`SequentialIdGenerator`。项目当前使用 JUnit 4、Robolectric、Compose UI Test、MockWebServer 和 Turbine。测试类命名应直接对应被测对象，例如 `ChatViewModelTest`、`ConnectionValidatorTest`。涉及网络、持久化或 Compose 导航的行为变更时，必须同步补充或更新测试。

## 提交与 Pull Request 规范
最近的提交历史同时出现了约定式前缀和简短中文摘要，例如 `Feat/...`、`chore/docs`。建议使用简洁的祈使句提交信息，并在合适时加作用域，例如 `feat(chat): persist draft messages`。Pull Request 应说明行为变更、列出影响模块、关联相关 issue 或文档；若涉及 UI 改动，附上截图或录屏。
