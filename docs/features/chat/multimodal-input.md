---
status: draft
priority: P2
phase: 3-advanced
---

# Multimodal Input

## Overview
支持图片和文件作为聊天输入的一部分，扩展用户在问图、问文档和上传素材场景下的表达能力。

## Functional Requirements
- [ ] FR-1: 支持从相机、图库和文件选择器添加附件。
- [ ] FR-2: 发送前展示附件预览、删除和上传进度。
- [ ] FR-3: 聊天请求可关联上传后的文件标识。

## Non-Functional Requirements
- [ ] NFR-1: 大文件上传要可取消并反馈进度。
- [ ] NFR-2: 权限拒绝、格式不支持和超限场景要有明确提示。

## API Contract
依赖 [../../api/file-upload.md](../../api/file-upload.md) 与 [../../api/chat-completions.md](../../api/chat-completions.md)。

## UI Description
输入框旁提供附件入口；用户选择资源后在输入区上方显示预览条；上传中展示进度，失败可重试，成功后随消息一起发送。

## Data Model
- `AttachmentDraft`
- `UploadedAssetRef`
- `AttachmentPickerSource`

## Architecture Notes
附件上传与消息发送解耦，先上传拿到引用，再组装聊天请求；文件缓存与临时 URI 管理由 `:core:common` 或 `:core:network` 协调。

## Dependencies
- Android 权限与 picker
- 文件上传接口
- 图片加载

## Acceptance Criteria
- 图片和文件附件都可完成选择、上传和发送。
- 失败和取消路径不会遗留脏状态。

