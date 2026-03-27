# Kora

安卓原生 Kotlin 知识库检索与 AI Chatbot 应用，交互参考 Open WebUI，能力对齐 FastGPT API。

## Documentation
- Agent 入口: [CLAUDE.md](CLAUDE.md)
- 全局索引: [docs/INDEX.md](docs/INDEX.md)
- 术语表: [docs/GLOSSARY.md](docs/GLOSSARY.md)
- 约定: [docs/CONVENTIONS.md](docs/CONVENTIONS.md)

## Planned Stack
- Kotlin 2.x
- Jetpack Compose
- Hilt
- Retrofit + OkHttp
- Room
- DataStore
- Markwon
- Coil

## Dev Workflow
- 本地提交前先执行 `./scripts/ktlint-check.sh`，或者将 `.githooks/` 设为 `git config core.hooksPath .githooks`
- M1 当前的最小回归链路是 `./gradlew testDebugUnitTest assembleDebug`
