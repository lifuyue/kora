---
status: planned
priority: P2
phase: 3-advanced
---

# Multimodal Input

## Overview
输入区能力参考 Open WebUI `MessageInput/FilesOverlay`、`VoiceRecording` 等组件，但针对 Android 采用系统 picker + 本地预览。目标是把图片、文件和后续语音都统一挂到同一个发送草稿模型上。

## Functional Requirements
- [ ] FR-1: 支持从系统文件选择器、图片选择器和相机入口添加附件。
- [ ] FR-2: 发送前展示附件预览、删除、失败重试和上传进度。
- [ ] FR-3: 上传成功后把附件映射为 `ChatCompletionContentPartFile` 或 app 允许的 `file_url` 内容。
- [ ] FR-4: 当 app `fileSelectConfig` 禁止某类文件时，在 picker 前和选后双重校验。

## Non-Functional Requirements
- [ ] NFR-1: 大文件上传可取消，不得阻塞文本输入。
- [ ] NFR-2: URI 权限、相机权限和格式错误都要有清晰回退。

## API Contract
依赖 [../../api/file-upload.md](../../api/file-upload.md)、[../../api/chat-completions.md](../../api/chat-completions.md) 和 [../../api/app-management.md](../../api/app-management.md)。

## UI Description
输入框左侧或上方提供附件入口。用户选中文件后显示为胶囊卡片，含缩略图、名称、大小、上传状态和删除按钮。上传完成后可和文本一起发送；上传失败项支持单独重试。

## Data Model
- `AttachmentDraft(localUri, mimeType, kind, uploadStatus, uploadedRef?)`
- `ChatComposerState(text, attachments, isRecording, canSend)`
- `UploadedAssetRef(name, url, key)`

## Architecture Notes
上传链路与发送链路解耦：先把本地 URI 变成远端引用，再组装消息数组。`ChatViewModel` 只关心草稿状态，实际上传由 repository / upload manager 处理。

## Dependencies
- [../../features/voice/speech-to-text.md](../voice/speech-to-text.md)
- [../../ui/component-catalog.md](../../ui/component-catalog.md)

## Acceptance Criteria
- 至少一种图片和一种文件类型能从选择到上传到发送完整走通。
- 失败、取消、权限拒绝路径不会残留脏草稿。
