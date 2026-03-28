---
status: implemented
priority: P2
phase: 2-knowledge
---

# Storage Cache

## Overview
存储与缓存页展示本地数据库、图片缓存、附件缓存、导出文件和临时上传占用，并提供清理操作。

## Functional Requirements
- [ ] FR-1: 展示主要缓存类别占用大小。
- [ ] FR-2: 支持按类别清理缓存。
- [ ] FR-3: 展示最近清理时间和回收空间。

## Non-Functional Requirements
- [ ] NFR-1: 不误删用户主动导出的文件。
- [ ] NFR-2: 清理在后台执行并可报告结果。

## API Contract
本地功能，不依赖远端接口。

## UI Description
页面用卡片展示数据库、图片、附件、导出和临时文件占用；每项有单独清理按钮，页面底部有全量清理入口。

## Data Model
- `StorageUsageState(items, totalBytes, lastCleanupAt)`
- `StorageUsageItem(type, bytes, clearable)`

## Architecture Notes
统计与清理能力由 common/database 层实现；settings 只展示结果和触发动作。

## Dependencies
- [../../architecture/local-storage.md](../../architecture/local-storage.md)

## Acceptance Criteria
- 占用统计接近真实磁盘情况。
- 清理后数字和实际文件状态同步。
