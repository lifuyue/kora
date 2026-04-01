# Testing Strategy

## Goal
围绕现有功能建立稳定、快速、可定位的自动化回归，把测试当成主开发接口，而不是发布前补救。

## Current Test Layers

### 1. Pure Unit
- 位置: `core/common`, `core/network`, `core/database` 的纯逻辑测试
- 目标: 校验数据模型、解析、拦截器、配置和轻量工具函数
- 特征: 快、定位准、不依赖 UI

### 2. Repository / Contract
- 位置: `feature/chat/*RepositoryTest`, `core/database/*RepositoryTest`
- 目标: 校验 API + Room + 状态归并等关键行为
- 当前优先级最高，因为最近的高成本回归主要出在这里

### 3. ViewModel / State
- 位置: `feature/*ViewModelTest`
- 目标: 校验导航参数、发送状态、错误处理、草稿与用户操作
- 要求: 避免只断言内部实现，优先断言可观察状态与仓库交互

### 4. Compose Screen
- 位置: `feature/*ScreenTest`, `app/navigation/*Test`
- 目标: 校验主要 UI 状态、关键 CTA、空态/错态/加载态
- 边界: 不替代 repository 契约测试

### 5. Acceptance Harness
- 位置: `app/*AcceptanceTest`, `core/testing/src/consumer*`
- 目标: 覆盖少量高价值端到端链路
- 原则: 数量少，但必须稳定
- 设备脚本链路: `make seed` 负责把测试知识库导入已安装 debug Kora；`make seed-e2e` 负责自动探测环境、执行导入并校验状态文件成功

## Current Coverage Shape
- `feature/chat` 测试最密集，是当前产品主链路
- `feature/settings` 有稳定基础覆盖
- `feature/knowledge` 覆盖偏薄，后续优先补 ViewModel 和关键屏幕状态
- `app` 层已有 acceptance harness，但需要继续收敛到少量稳定路径

## Testing Rules
- 新回归先补最靠近根因的一层测试，再修实现
- repository 回归必须覆盖输入归一化、流式事件合并、持久化状态
- ViewModel 回归必须覆盖导航参数和发送/恢复/错误路径
- 设备或模拟器复现只作为最后验收，不替代自动化测试
- flaky 测试视为缺陷，优先修而不是反复重跑

## Recommended Regression Chain
1. 目标模块定向单测
2. 相关 feature 全量 `testDebugUnitTest`
3. `assembleDebug`
4. 必要时再做一条模拟器链路确认
5. 涉及已安装 App 本地知识库导入时，执行 `make seed-e2e`

## Device Seed E2E
- 前提: 已连接单个设备或显式传 `SERIAL`，设备可安装 debug APK，且 `run-as com.lifuyue.kora` 可用
- `make seed`: 生成 fixture、安装 debug APK、推送 payload、触发 debug 导入并等待状态文件成功
- `make seed-e2e`: 在 `make seed` 前自动记录设备 serial、Android 版本、`pm path`、`run-as` 可用性、payload/status 路径、receiver/action，并将结果写入 `build/seed-e2e/last-run.json`
- 当前成功标准停留在脚本与状态文件层，不覆盖 UI 自动化；验收要求 `state=success`、`imported > 0`、`ready == imported`
- 排障优先级: 先看脚本 stderr，再看 `build/seed-e2e/last-run.json`，最后看设备内状态文件

## Immediate Improvement Targets
- 聊天发送与流式解析的 contract 测试继续前置
- 知识库链路补足 repository / ViewModel 层缺口
- 收敛 app 层 acceptance，用更少但更稳定的主链路替代大而松的回归
- 记录高成本回归到 `docs/memory/solutions.md` 或 `docs/memory/mistakes.md`
