# Navigation

## 顶层路由
- `onboarding`
- `auth`
- `chat/home`
- `chat/detail/{chatId}`
- `knowledge`
- `apps`
- `settings`

## 导航原则
- 顶层使用 Compose Navigation 管理，参数通过类型安全路由对象封装。
- 深层链接主要用于分享链接认证与打开指定会话。
- 平板与折叠屏允许列表-详情双栏布局，但路由语义保持一致。

## 状态恢复
- 导航参数只承载最小必要数据，详情内容通过 `SavedStateHandle` + repository 恢复。
- 对于会话、知识库与应用详情，优先使用 ID 导航，而非传递大对象。

