---
status: implemented
priority: P1
phase: 2-knowledge
---

# Document Upload

## Overview
文档上传是创建 file-type collection 的主入口，直接对应 `create/localFile` 与 `create/fileId`。

## Functional Requirements
- [ ] FR-1: 支持本地文件选择、上传、导入为 collection。
- [ ] FR-2: 展示上传进度、导入结果和失败重试。
- [ ] FR-3: 支持重复导入前提示或复用已有 `fileId`。

## Non-Functional Requirements
- [ ] NFR-1: 大文件上传可取消且不阻塞页面其他操作。
- [ ] NFR-2: 文件类型和大小在客户端预校验。

## API Contract
依赖 [../../api/file-upload.md](../../api/file-upload.md) 和 [../../api/dataset-collections.md](../../api/dataset-collections.md)。

## UI Description
上传页显示选择文件按钮、当前上传队列和结果卡片。上传完成后自动刷新 collection 列表；失败项显示明确原因和重试按钮。

## Data Model
- `DocumentUploadDraft(uri, filename, size, status, progress, fileId?)`
- `DocumentImportResult(collectionId, resultSummary)`

## Architecture Notes
上传与导入需要拆成两个状态：对象存储上传成功并不等于 collection 创建成功。Repository 应分别暴露 `uploadStatus` 和 `importStatus`。

## Dependencies
- [collection-management.md](collection-management.md)
- [../../architecture/networking.md](../../architecture/networking.md)

## Acceptance Criteria
- 选中文档后可完成上传和导入。
- 失败和取消流程不会留下僵尸任务。
