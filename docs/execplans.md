# Plan: Kora 前端与交互设计重构执行主文档

## Context

Kora 项目目前状态：
- **文档体系已就位**：73 个 spec 文件已基本充实到可实现精度，仍有部分 status 元数据待回写
- **参考仓库已克隆**：`.reference/FastGPT`（v4.14.9.5）和 `.reference/open-webui`（v0.8.12）
- **Android 工程已落地**：多模块应用、核心网络/数据库/公共模块、设置、聊天 MVP 与知识库主线均已实现
- **当前集成状态**：M0-M5 已完成，功能链路基本可运行，但前端层仍停留在“Material 3 默认骨架 + 局部功能实现”的水位

现在需要：把文档和实现重心切到“前端设计 + 交互设计 + 页面验收”，先完成深色工作台重构，再继续推进后续高级功能。

---

## Frontend Refactor Priority

### 目标
- 用深色工作台视觉语言统一聊天、知识库、设置三大域。
- 让导航、状态摘要、筛选、空态、错误态、反馈动效使用同一套交互语法。
- 建立可量化的 UI/UX 评分体系，作为里程碑验收标准。

### 第二轮交互重构重点
- 聊天 tab 固定从“新对话工作台”进入，历史会话降级为聊天内浏览器，而不是独立一级入口。
- 会话列表重点服务“快速回到上下文”，优先保留草稿、筛选、滚动和当前 app 上下文。
- 知识库页面的主 CTA 改成“挂接到当前对话 / 返回当前对话”，后台 CRUD 降到次级操作。
- 常用设置提供任务内快速入口，避免用户为了流式输出、引用、主题或语言频繁跳出聊天。
- 平板双栏本轮只保兼容，不单独设计一套新的页面逻辑。

### 当前轮次执行顺序
1. 主题 token 与共享工作台组件
2. 壳层导航与全局状态摘要
3. 聊天与会话列表
4. 知识库首页与浏览器
5. 设置首页与控制面板
6. 文档与评分体系回写

### 验收基线
- 聊天与会话页 `>= 90/100`
- 知识与设置页 `>= 85/100`
- 平均分 `>= 88/100`
- 评分方法见 `docs/ui/quality-scorecard.md`

## 总体里程碑

```
M0  文档充实      → 所有 spec 充实到 agent 可直接实现的精度
M1  工程脚手架    → Android 多模块项目可编译运行空壳
M2  核心基础设施  → :core:network, :core:database, :core:common 可用
M3  设置 & 引导   → 首次启动 → 配置服务器 → 连通测试 完成
M4  聊天 MVP      → 流式聊天 + Markdown 渲染 + 会话 CRUD
M5  知识库        → Dataset/Collection/Chunk 管理 + 引用 + 多 App
M6  高级功能      → 语音、多模态、交互节点、分享、导出、平板适配
```

依赖链：`M0 → M1 → M2 → M3 → M4 → M5 → M6`（M3/M4 可部分并行）

当前里程碑状态（2026-03-28）：
- `M0` 文档充实：已完成
- `M1` 工程脚手架：已完成
- `M2` 核心基础设施：已完成
- `M3` 设置与引导：已完成
- `M4` 聊天 MVP：已完成
- `M5` 知识库：已完成
- `M6` 高级功能：未开始

---

## M0: 文档充实（Codex 任务 0.1 ~ 0.5）

**目标**：将所有 skeleton spec 充实为包含精确类型定义、字段名、枚举值、源码路径的完整规格，使 agent 可不依赖 .reference/ 直接实现。

### Task 0.1: 充实 API 规格（16 个文件）

从 `.reference/FastGPT/packages/global/` 提取精确类型并回填到 `docs/api/`：

| 文件 | 需要回填的内容 | 源码位置 |
|------|--------------|---------|
| `api/authentication.md` | API Key 格式 `fastgpt-`、Bearer header、两种 key 类型 | `packages/global/support/permission/` |
| `api/chat-completions.md` | 完整 request body（chatId, stream, detail, responseChatItemId, variables, messages）、response 四种模式（stream×detail）、`ChatCompletionMessageParam` 扩展字段（file_url, reasoning_content）| `packages/global/core/ai/type.ts`, `projects/app/src/pages/api/v1/chat/completions.ts` |
| `api/chat-streaming.md` | 16 种 SSE 事件枚举（`SseResponseEventEnum`）替换当前不准确的 9 种、每种事件的 payload 结构 | `packages/global/core/workflow/runtime/constants.ts`, `packages/service/common/response/index.ts` |
| `api/chat-history.md` | `getHistories`（offset/pageSize/source）、`updateHistory`（title/top）、`delHistory`、`clearHistories` 精确参数 | `projects/app/src/pages/api/core/chat/` |
| `api/chat-records.md` | `init`（返回 chatConfig/chatModels/welcomeText）、`getPaginationRecords`、`getResData`、`item/delete`、`updateUserFeedback` | 同上 |
| `api/chat-interactive.md` | `userSelect`/`userInput` 类型定义、如何通过 messages 恢复交互 | `packages/global/core/chat/type.ts` (AIChatItemValueItemType.interactive) |
| `api/dataset-management.md` | `DatasetSchemaType` 全字段、`DatasetTypeEnum` 枚举、CRUD 端点参数 | `packages/global/core/dataset/type.ts`, `packages/global/core/dataset/constants.ts` |
| `api/dataset-collections.md` | `DatasetCollectionSchemaType`、6 种创建端点、`TrainingModeEnum`、`DatasetCollectionTypeEnum` | 同上 |
| `api/dataset-data.md` | `DatasetDataSchemaType`、batch push（最大 200）、index 类型 | 同上 |
| `api/dataset-search.md` | `DatasetSearchModeEnum`、`SearchScoreTypeEnum`、searchTest 参数 | 同上 |
| `api/app-management.md` | `AppSchemaType`、`AppTypeEnum`、`AppChatConfigType`（welcomeText/variables/fileSelectConfig/ttsConfig 等） | `packages/global/core/app/type.ts`, `packages/global/core/app/constants.ts` |
| `api/error-handling.md` | 完整错误码表（400-504 + 500000-509000 各域错误码）、`ResponseType<T>` 结构 | `packages/global/common/error/errorCode.ts`, `packages/global/common/error/code/*.ts` |
| `api/file-upload.md` | multipart 上传到 collection 的流程、支持文件类型 | `projects/app/src/pages/api/core/dataset/collection/create/` |
| `api/question-guide.md` | `createQuestionGuide`（appId, chatId, config） | `projects/app/src/pages/api/core/chat/` |
| `api/share-auth.md` | 三步认证：init → start → finish、authToken/shareId 参数 | `projects/app/src/pages/api/support/outLink/` |
| `api/overview.md` | 全端点速查表更新 | 汇总以上所有 |

### Task 0.2: 充实 Architecture 规格（7 个文件）

| 文件 | 需要补充 |
|------|---------|
| `architecture/overview.md` | 完整 C4 层次图文字描述、模块依赖方向 |
| `architecture/tech-stack.md` | 每个依赖的确切 artifact + 版本（Compose BOM 2025.x、Hilt 2.51、Retrofit 2.11、OkHttp 4.12、Room 2.6、kotlinx-serialization 1.7 等） |
| `architecture/module-structure.md` | 每个模块的公开 API 边界、禁止的依赖方向（feature 不能互相依赖、core 不能依赖 feature） |
| `architecture/navigation.md` | 完整 route 表：`onboarding`, `settings/*`, `chat/{chatId?}`, `conversations`, `knowledge/*`, `app-selector` |
| `architecture/data-flow.md` | 具体 Flow 链路示例（ChatRepository → MessageFlow → ChatViewModel → ChatScreen） |
| `architecture/networking.md` | OkHttp interceptor chain 顺序、SSE 用 OkHttp EventSource vs 手动 BufferedReader 的决策、timeout 值 |
| `architecture/local-storage.md` | Room entities 初步 schema（ConversationEntity, MessageEntity, CachedDatasetEntity）、DataStore preference keys |

### Task 0.3: 充实 Feature Specs — Chat & Conversations（16 个文件）

参考 Open WebUI 源码填充 UI 交互细节：

| 文件 | 源码参考 |
|------|---------|
| `features/chat/streaming-chat.md` | `open-webui/src/lib/components/chat/Chat.svelte` (chatCompletionEventHandler)、`open-webui/src/lib/apis/streaming/index.ts` |
| `features/chat/markdown-rendering.md` | `open-webui/src/lib/components/chat/Messages/Markdown.svelte`、`MarkdownTokens.svelte`、`KatexRenderer.svelte`、`CodeBlock.svelte` |
| `features/chat/message-actions.md` | `open-webui/src/lib/components/chat/Messages/ResponseMessage.svelte` (lines 855-1400+) |
| `features/chat/message-feedback.md` | `ResponseMessage.svelte` feedbackHandler、`updateUserFeedback` API |
| `features/chat/interactive-nodes.md` | FastGPT `AIChatItemValueItemType.interactive`、`SseResponseEventEnum.interactive` |
| `features/chat/suggested-questions.md` | FastGPT `createQuestionGuide` API |
| `features/chat/citations.md` | `open-webui/src/lib/components/chat/Messages/Citations.svelte`、FastGPT `quoteList` in `responseData` |
| `features/chat/multimodal-input.md` | `open-webui/src/lib/components/chat/MessageInput/` (FilesOverlay, VoiceRecording) |
| `features/conversations/conversation-list.md` | `open-webui/src/lib/components/layout/Sidebar.svelte`、FastGPT `getHistories` |
| `features/conversations/conversation-crud.md` | `Sidebar/ChatItem.svelte`、FastGPT `updateHistory`/`delHistory` |
| 其余 6 个 conversation specs | Open WebUI sidebar 交互模式适配 |

### Task 0.4: 充实 Feature Specs — Knowledge, Apps, Voice, Settings, Auth（17 个文件）

| 分组 | 源码参考 |
|------|---------|
| knowledge/* | FastGPT `packages/global/core/dataset/` 类型 + `projects/app/src/pages/api/core/dataset/` 端点 |
| apps/* | FastGPT `AppSchemaType`、`AppChatConfigType`、`chat/init` 返回 |
| voice/* | Open WebUI `CallOverlay.svelte`、`VoiceRecording.svelte` 交互模式 |
| settings/* | Open WebUI `Settings/*.svelte` 面板结构 |
| auth/* | FastGPT share-auth 三步流 + API key 校验 |

### Task 0.5: 充实 Reference & UI 规格（12 个文件）

| 文件 | 内容 |
|------|------|
| `reference/open-webui-feature-map.md` | 100+ 功能逐条映射：功能名 → Open WebUI 组件路径 → Kora 对应 spec → 优先级 → 阶段 |
| `reference/fastgpt-api-map.md` | 全端点映射：端点 → 方法 → Kora spec → 实现状态 |
| `ui/design-system.md` | Material 3 token 定义（color scheme, typography scale, shape） |
| `ui/component-catalog.md` | 可复用组件清单与 props |
| `ui/screen-inventory.md` | 全页面清单与 route |
| 其余 ui/* | 响应式、无障碍、动画、图标 |

---

## M1: Android 工程脚手架（Codex 任务 1.1 ~ 1.5）

**目标**：创建可编译运行的多模块 Android 项目空壳

### Task 1.1: 创建 Gradle 多模块项目

```
kora/
├── build.gradle.kts              (root, plugins + kotlin version)
├── settings.gradle.kts           (include all modules)
├── gradle.properties             (android.useAndroidX=true, kotlin.code.style=official)
├── gradle/
│   ├── wrapper/                  (gradle-wrapper.jar + properties)
│   └── libs.versions.toml        (version catalog: compose-bom, hilt, retrofit, okhttp, room, etc.)
├── gradlew + gradlew.bat
├── app/
│   ├── build.gradle.kts          (com.android.application, hilt, compose)
│   └── src/main/
│       ├── AndroidManifest.xml   (INTERNET permission, Application class)
│       ├── java/com/lifuyue/kora/
│       │   ├── KoraApplication.kt  (@HiltAndroidApp)
│       │   └── MainActivity.kt     (@AndroidEntryPoint, setContent with NavHost)
│       └── res/ (values/strings.xml, themes.xml, colors.xml)
├── core/
│   ├── network/build.gradle.kts  (android library, hilt, retrofit, okhttp, kotlinx-serialization)
│   ├── database/build.gradle.kts (android library, room)
│   └── common/build.gradle.kts   (android library, common types)
├── feature/
│   ├── chat/build.gradle.kts     (android library, compose, hilt)
│   ├── knowledge/build.gradle.kts
│   └── settings/build.gradle.kts
```

- 参考: `docs/architecture/module-structure.md`, `docs/architecture/tech-stack.md`
- 验证: `./gradlew assembleDebug` 通过

### Task 1.2: 配置 Version Catalog (libs.versions.toml)

```toml
[versions]
kotlin = "2.1.0"
compose-bom = "2025.01.01"
hilt = "2.51.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.6.1"
kotlinx-serialization = "1.7.3"
markwon = "4.6.2"
coil = "2.7.0"
datastore = "1.1.1"
navigation-compose = "2.8.5"
```

### Task 1.3: 基础 Application 与 MainActivity

- `KoraApplication.kt`: `@HiltAndroidApp` 空壳
- `MainActivity.kt`: `@AndroidEntryPoint`、`setContent { KoraNavGraph() }`
- `KoraNavGraph.kt`: Compose Navigation 骨架，初始 route 为 `onboarding`

### Task 1.4: 配置 ktlint + Git hooks

- 添加 ktlint Gradle 插件
- pre-commit hook 运行 `ktlintCheck`

### Task 1.5: 配置 CI（可选）

- GitHub Actions workflow: build + ktlintCheck on PR

**M1 验收**: `./gradlew assembleDebug` 成功，app 启动显示空白 Compose 页面

---

## M2: 核心基础设施（Codex 任务 2.1 ~ 2.6）

**目标**：`:core:network`、`:core:database`、`:core:common` 可被 feature 模块使用

### Task 2.1: :core:common — 基础类型

```kotlin
// Result wrapper
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val code: Int, val message: String) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()
}

// Chat enums (from FastGPT)
enum class ChatRole { System, Human, AI }
enum class ChatFileType { image, file }
enum class ChatSource { test, online, share, api }

// SSE event types (from FastGPT SseResponseEventEnum)
enum class SseEvent {
    error, answer, fastAnswer, flowNodeStatus, flowNodeResponse,
    toolCall, toolParams, toolResponse, flowResponses,
    updateVariables, interactive, plan, stepTitle,
    workflowDuration, collectionForm, topAgentConfig
}
```

- 参考: `docs/api/chat-streaming.md` (充实后), FastGPT `packages/global/core/chat/constants.ts`

### Task 2.2: :core:network — Retrofit + OkHttp 配置

```kotlin
// AuthInterceptor: 注入 Bearer fastgpt-xxx header
// FastGptApi interface:
//   POST /api/v1/chat/completions → ChatCompletionRequest → ChatCompletionResponse | ResponseBody (streaming)
//   POST /api/core/chat/getHistories → ChatHistoryRequest → ChatHistoryResponse
//   POST /api/core/chat/init → ChatInitRequest → ChatInitResponse
//   GET  /api/core/app/list → AppListResponse
//   ... (all endpoints from docs/api/)

// SSE parser: OkHttp streaming + custom event parser
// ResponseEnvelope<T>: { code, statusText, message, data: T }
```

- 参考: `docs/architecture/networking.md`, `docs/api/overview.md`
- 关键: SSE 解析器必须处理 `event:` 字段分发，不能只按 `data:` 行解析

### Task 2.3: :core:network — SSE 流式解析器

```kotlin
class SseEventParser {
    fun parse(line: String): SseEventData?
    // 处理: event: answer\ndata: {...}\n\n
    // 返回: SseEventData(event: SseEvent, payload: JsonElement)
}

class ChatStreamCollector(
    private val onEvent: (SseEvent, JsonElement) -> Unit,
    private val onDone: () -> Unit,
    private val onError: (Throwable) -> Unit
)
```

- 参考: FastGPT `packages/service/common/response/index.ts` (responseWrite 格式)
- 参考: Open WebUI `src/lib/apis/streaming/index.ts` (EventSourceParserStream 模式)

### Task 2.4: :core:database — Room Schema

```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val chatId: String,
    val appId: String,
    val title: String,
    val customTitle: String?,
    val isPinned: Boolean = false,
    val source: String, // ChatSource
    val createdAt: Long,
    val updatedAt: Long,
    val folderId: String? = null,
    val isArchived: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val dataId: String,
    val chatId: String,
    val role: String, // ChatRole
    val contentJson: String, // serialized list of content items (text, file, tool, interactive, reasoning)
    val feedbackType: Int? = null, // 1=good, -1=bad
    val timestamp: Long
)

@Entity(tableName = "cached_datasets")
data class CachedDatasetEntity(...)
```

- 参考: `docs/architecture/local-storage.md`
- 参考: FastGPT `ChatSchemaType`, `ChatItemSchemaType`

### Task 2.5: :core:database — DataStore Preferences

```kotlin
object PreferenceKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val API_KEY = stringPreferencesKey("api_key") // encrypted
    val CURRENT_APP_ID = stringPreferencesKey("current_app_id")
    val THEME_MODE = stringPreferencesKey("theme_mode") // light/dark/system
    val LANGUAGE = stringPreferencesKey("language")
    val STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
    val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
    val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}
```

### Task 2.6: :core:network — API Key 加密存储

- 使用 `EncryptedSharedPreferences` 或 AndroidKeyStore 加密 API Key
- `fastgpt-` 前缀校验

**M2 验收**: 可在测试中构造 Retrofit client → 发出请求 → 解析 SSE 事件 → 写入 Room → 读回

---

## M3: 设置与引导（Codex 任务 3.1 ~ 3.5）

**目标**：用户首次启动 → 引导 → 配置服务器 → 连通测试 → 进入主界面

### Task 3.1: Onboarding Flow

- 3 步引导页（Compose HorizontalPager）：欢迎 → 功能介绍 → 配置入口
- 完成后设置 `ONBOARDING_COMPLETED = true`
- 参考: `docs/features/auth/onboarding.md`

### Task 3.2: Connection Config Screen

- 输入 Server URL（默认 `https://api.fastgpt.in/api`）
- 输入 API Key（校验 `fastgpt-` 前缀）
- "测试连接"按钮 → `GET /api/core/app/list` 确认连通
- 成功后保存到 DataStore/EncryptedPrefs
- 参考: `docs/features/settings/connection-config.md`, Open WebUI `Settings/Connections.svelte`

### Task 3.3: Settings Overview Screen

- 列表式设置页：连接配置 / 主题 / 聊天偏好 / 语言 / 缓存 / 关于
- Material 3 List + 分组 header
- 参考: `docs/features/settings/settings-overview.md`

### Task 3.4: Theme & Appearance

- Light / Dark / System / OLED Dark
- Material You 动态取色（Android 12+）
- 参考: `docs/features/settings/theme-appearance.md`, Open WebUI `Settings/General.svelte` (theme selector)

### Task 3.5: Main Shell (底部导航)

- BottomNavigation: 聊天 / 知识库 / 设置
- 3 个 NavGraph (chatGraph, knowledgeGraph, settingsGraph)
- 参考: `docs/architecture/navigation.md`

**M3 验收**: 首次启动 → 引导 → 配置 API → 连通成功 → 进入带底部导航的主界面

---

## M4: 聊天 MVP（Codex 任务 4.1 ~ 4.12）

**目标**：完整的流式聊天体验 + Markdown 渲染 + 会话管理

### Task 4.1: ChatRepository

```kotlin
class ChatRepository @Inject constructor(
    private val api: FastGptApi,
    private val db: KoraDatabase,
    private val prefs: DataStore<Preferences>
) {
    fun sendMessage(chatId: String?, appId: String, messages: List<ChatMessage>, variables: Map<String, Any>): Flow<SseEventData>
    suspend fun getHistories(appId: String, offset: Int, pageSize: Int): AppResult<List<ChatHistory>>
    suspend fun initChat(appId: String, chatId: String): AppResult<ChatInitData>
    suspend fun updateHistory(appId: String, chatId: String, title: String?, top: Boolean?): AppResult<Unit>
    suspend fun deleteHistory(appId: String, chatId: String): AppResult<Unit>
    // ... + local cache sync
}
```

### Task 4.2: ChatViewModel

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val uiState: StateFlow<ChatUiState>  // messages, isStreaming, error, chatConfig
    fun send(text: String, files: List<ChatFile>?)
    fun stopGeneration()
    fun regenerate(messageId: String)
    fun continueGeneration()
}
```

- 参考: Open WebUI `Chat.svelte` 状态管理模式

### Task 4.3: Chat Screen — Message List (LazyColumn)

- `LazyColumn` 渲染消息列表
- 用户消息气泡（右侧）/ AI 消息气泡（左侧）
- 流式文本实时追加到最后一条 AI 消息
- 自动滚动到底部（可手动暂停）
- 加载骨架屏（Skeleton）
- 参考: Open WebUI `Messages.svelte`, `UserMessage.svelte`, `ResponseMessage.svelte`, `Skeleton.svelte`

### Task 4.4: Chat Screen — Message Input Bar

- 多行输入框 + 发送按钮
- 流式响应中显示停止按钮
- 附件入口（Phase 2+ 补充具体文件类型）
- 参考: Open WebUI `MessageInput.svelte`

### Task 4.5: Markdown 渲染 (Markwon)

配置 Markwon 插件链：
- 核心 Markdown（headings, bold, italic, lists, blockquotes, tables, hr）
- 代码块语法高亮（Prism4j 或 highlight.js WebView 备选）
- 行内代码
- 链接（可点击）
- 图片（Coil 加载）
- 参考: Open WebUI `Markdown.svelte` 插件链（marked + KaTeX + highlight.js + Mermaid）
- 参考: `docs/features/chat/markdown-rendering.md`

### Task 4.6: LaTeX 渲染

- Markwon + KaTeX（WebView 内嵌）或 MathJax
- 内联 `$...$` 和块级 `$$...$$`
- 参考: Open WebUI `KatexRenderer.svelte`

### Task 4.7: Code Block 组件

- 语法高亮 + 语言标签
- 复制代码按钮
- 参考: Open WebUI `CodeBlock.svelte`

### Task 4.8: Conversation List Screen

- LazyColumn 显示会话历史
- 搜索框（本地过滤 + API 搜索）
- 置顶会话排在最前
- 新建会话 FAB
- 点击进入聊天
- 参考: Open WebUI `Sidebar.svelte`, `ChatItem.svelte`
- API: FastGPT `getHistories`

### Task 4.9: Conversation CRUD

- 新建：点击 FAB → 直接进入空聊天页，首次发消息时生成 chatId
- 重命名：长按 → 底部 Sheet → 编辑标题 → `updateHistory`
- 删除：滑动删除或长按 → 确认 → `delHistory`
- 清空全部：设置入口 → `clearHistories`
- 参考: `docs/features/conversations/conversation-crud.md`, Open WebUI `ChatMenu.svelte`

### Task 4.10: Conversation Pin

- 长按 → 置顶/取消置顶 → `updateHistory(top=true/false)`
- 置顶会话 section 分组
- 参考: `docs/features/conversations/conversation-pin.md`

### Task 4.11: Message Actions（复制 + 重新生成）

- AI 消息底部操作栏：复制 / 重新生成
- 复制：`ClipboardManager.setPrimaryClip()`
- 重新生成：删除最后 AI 回复 → 重发用户消息
- 参考: Open WebUI `ResponseMessage.svelte` (copy, regenerate)

### Task 4.12: Message Feedback（点赞/点踩）

- AI 消息操作栏：👍 / 👎 按钮
- 调用 `updateUserFeedback` API
- 本地持久化反馈状态
- 参考: Open WebUI `ResponseMessage.svelte` (feedbackHandler), `docs/api/chat-records.md`

**M4 验收**:
- 新建会话 → 发送消息 → 流式看到 AI 回复（Markdown 正确渲染）
- 返回会话列表 → 看到历史 → 重新进入 → 恢复上下文
- 复制消息、重新生成、点赞/点踩 正常工作
- 网络断开/API 错误有明确提示且不崩溃

---

## M5: 知识库与高级聊天（Codex 任务 5.1 ~ 5.12）

### Task 5.1: App Selector

- 底部 Sheet 或 Dropdown 选择 FastGPT App
- `GET /api/core/app/list` 获取列表
- 切换 App 后重置聊天上下文
- 参考: `docs/features/apps/app-selector.md`, Open WebUI `ModelSelector/Selector.svelte`

### Task 5.2: App Detail / Chat Config

- 从 `chat/init` 获取 app 配置：welcomeText, chatModels, fileSelectConfig, variables
- 新会话显示欢迎语
- 参考: `docs/features/apps/app-detail.md`

### Task 5.3: Dataset Browser

- 列表展示知识库（名称、简介、向量模型、更新时间）
- 创建 / 删除知识库
- API: `GET /api/core/dataset/list`, `POST /api/core/dataset/create`, `DELETE /api/core/dataset/delete`
- 参考: `docs/features/knowledge/dataset-browser.md`

### Task 5.4: Collection Management

- 知识库内的 Collection 列表
- 显示训练状态指示器（active/syncing/waiting/error）
- API: `POST /api/core/dataset/collection/list`
- 参考: `docs/features/knowledge/collection-management.md`

### Task 5.5: Document Upload

- 文件选择器（PDF, DOCX, TXT, MD, CSV, HTML）
- 上传进度指示
- 训练配置（chunk size, training type: chunk/qa）
- API: `POST /api/core/dataset/collection/create/localFile` (multipart)
- 参考: `docs/features/knowledge/document-upload.md`

### Task 5.6: Web Link Import

- 输入 URL → 创建 Collection
- API: `POST /api/core/dataset/collection/create/link`
- 参考: `docs/features/knowledge/web-link-import.md`

### Task 5.7: Manual Text/QA Input

- 文本输入 → push data
- API: `POST /api/core/dataset/collection/create/text`, `POST /api/core/dataset/data/pushData`
- 参考: `docs/features/knowledge/text-input.md`

### Task 5.8: Chunk Viewer

- Collection 内的数据块列表
- 可编辑 Q/A 内容
- API: `POST /api/core/dataset/data/list`, `PUT /api/core/dataset/data/update`
- 参考: `docs/features/knowledge/chunk-viewer.md`

### Task 5.9: Search Test

- 输入查询 → 调用 searchTest → 显示匹配结果 + 相似度分数
- 搜索模式选择（embedding/fullText/mixed）
- API: `POST /api/core/dataset/searchTest`
- 参考: `docs/features/knowledge/search-test.md`

### Task 5.10: Citations in Chat

- `detail=true` 响应中解析 `quoteList`
- 引用 chip（来源文件名 + 相似度）
- 点击展开引用内容面板
- 参考: `docs/features/chat/citations.md`, Open WebUI `Citations.svelte`

### Task 5.11: Suggested Questions

- AI 回复后显示推荐问题 chip
- API: `POST /api/core/chat/createQuestionGuide`
- 参考: `docs/features/chat/suggested-questions.md`

### Task 5.12: Conversation Folders & Tags

- 本地 Room 实现文件夹分组
- 标签管理
- 参考: `docs/features/conversations/conversation-folders.md`, `conversation-tags.md`

**M5 验收**:
- 切换 App → 看到欢迎语 → 聊天中看到引用 chip → 展开查看来源
- 创建知识库 → 上传文档 → 查看训练状态 → 检索测试返回结果
- 推荐问题可点击继续对话

---

## M6: 高级功能（Codex 任务 6.1 ~ 6.10）

### Task 6.1: Interactive Workflow Nodes

- `userSelect`: 渲染选项按钮组，选择后发送到 chat completions 继续
- `userInput`: 渲染表单字段，填写后提交继续
- 参考: `docs/features/chat/interactive-nodes.md`

### Task 6.2: Multimodal Input（图片/文件）

- 相机拍照 / 图库选择 → base64 或上传后 URL → `image_url` / `file_url` content type
- 参考: `docs/features/chat/multimodal-input.md`

### Task 6.3: Speech-to-Text

- 麦克风按钮 → Android SpeechRecognizer / Whisper API
- 转录结果填入输入框
- 参考: `docs/features/voice/speech-to-text.md`, Open WebUI `VoiceRecording.svelte`

### Task 6.4: Text-to-Speech

- AI 消息朗读按钮 → Android TTS / 自定义 TTS API
- 播放/暂停/停止控制
- 参考: `docs/features/voice/text-to-speech.md`

### Task 6.5: Conversation Share

- Android Share Sheet → 导出对话为文本
- 参考: `docs/features/conversations/conversation-share.md`

### Task 6.6: Conversation Export

- 导出为 JSON / TXT / PDF
- 参考: `docs/features/conversations/conversation-export.md`

### Task 6.7: Conversation Archive

- 归档/取消归档
- 归档列表独立查看
- 参考: `docs/features/conversations/conversation-archive.md`

### Task 6.8: Share Link Auth

- Deep link `kora://share/{shareId}` → 三步认证 → 打开分享聊天
- 参考: `docs/features/auth/share-link-auth.md`

### Task 6.9: App Analytics

- 使用统计页面（调用量、token 用量）
- API: `GET /api/core/app/logs/getTotalData`
- 参考: `docs/features/apps/app-analytics.md`

### Task 6.10: Responsive Layout (Tablet/Foldable)

- 手机: 单栏
- 平板/折叠屏: 双栏（conversation list | chat detail）
- WindowSizeClass 适配
- 参考: `docs/ui/responsive-layout.md`

**M6 验收**: 所有高级功能可用，平板适配正常

---

## Codex 任务执行规范

每个 Task 提交给 Codex 时应包含：

```
## 任务描述
[一句话说明目标]

## 参考文档
- 读: docs/features/xxx.md (功能规格)
- 读: docs/api/xxx.md (API 合约)
- 读: docs/architecture/xxx.md (架构约束)

## 参考源码 (.reference/)
- 读: .reference/open-webui/src/lib/components/xxx (交互模式)
- 读: .reference/FastGPT/packages/global/core/xxx (类型定义)

## 实现范围
- 创建/修改哪些文件
- 不要修改哪些文件

## 验收标准
- [ ] 可编译
- [ ] 功能可用
- [ ] 错误状态有处理
```

---

## Verification

- M0: 所有 73 个 spec 文件包含精确的类型定义、字段名、枚举值（不再是 skeleton）
- M1: `./gradlew assembleDebug` 成功
- M2: 可在 instrumented test 中走通 API → SSE 解析 → Room 存储 → 读回
- M3: 首次启动引导 → 配置 → 连通测试 → 主界面
- M4: 完整聊天循环（发送 → 流式回复 → Markdown → 会话管理）
- M5: 知识库 CRUD + 引用展示 + 多 App 切换
- M6: 全部高级功能可用
