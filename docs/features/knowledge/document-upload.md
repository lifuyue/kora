---
status: draft
priority: P1
phase: 2-knowledge
---

# Document Upload

## Overview
支持通过文件选择器上传文档到知识库，并配置基础训练参数。

## Functional Requirements
- [ ] FR-1: 用户可选择本地文件并看到上传进度。
- [ ] FR-2: 用户可在上传前填写或调整训练配置。
- [ ] FR-3: 上传成功后自动创建或关联 Collection。

## Non-Functional Requirements
- [ ] NFR-1: 大文件上传要可取消、可重试。
- [ ] NFR-2: 文件格式校验必须在提交前完成。

## API Contract
依赖 [../../api/file-upload.md](../../api/file-upload.md) 和 [../../api/dataset-collections.md](../../api/dataset-collections.md)。

## UI Description
导入页包含文件选择区、训练配置表单和进度区域；上传中展示百分比，失败给出具体错误；成功后跳转到 Collection 详情或列表刷新。

## Data Model
- `DocumentUploadDraft`
- `TrainingConfig`
- `UploadProgressState`

## Architecture Notes
文件上传与 Collection 创建应拆为两个步骤，便于复用和错误恢复；临时文件 URI 由平台层统一管理。

## Dependencies
- 文件上传接口
- Android 文档选择器
- Collection 管理

## Acceptance Criteria
- 常见文档格式可完成上传与导入。
- 失败重试不会重复生成脏 Collection。

