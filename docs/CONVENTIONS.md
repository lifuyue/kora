# Conventions

## 命名约定
- 文档文件统一使用 `kebab-case`。
- 一个文件只描述一个关注点。
- 文档目录深度控制在 3 层内，便于 agent 搜索与 glob。

## Markdown 约定
- 标题层级从 `#` 开始，避免跳级。
- 功能规格文档统一复用 [SPEC-TEMPLATE.md](SPEC-TEMPLATE.md) 的 section 名称。
- 相对路径交叉引用，避免使用仓库外链接描述本仓库文档。

## 工程约定
- Kotlin 默认 4 空格缩进，XML 默认 2 空格。
- UI 状态统一覆盖 loading、empty、error、success。
- 文案进入字符串资源，业务错误消息需映射为用户可理解描述。
- 优先离线友好的读写策略，本地缓存与远端状态同步需要明示。

## Agent 约定
- 新功能先补 spec，再补 API 约束，再落实现。
- 变更前先核对相关 `docs/features/`、`docs/api/` 与 `docs/architecture/`。
- 需要新增术语时，同步更新 [GLOSSARY.md](GLOSSARY.md)。
- 需要新增功能时，同步更新 [INDEX.md](INDEX.md) 与相应路线图文档。

