---
status: draft
priority: P2
phase: 2-knowledge
---

# Storage Cache

## Overview
展示本地存储占用并提供缓存清理能力，帮助用户控制磁盘空间与临时文件生命周期。

## Functional Requirements
- [ ] FR-1: 展示图片缓存、附件缓存、导出文件和数据库大小。
- [ ] FR-2: 支持清理不同类别缓存。
- [ ] FR-3: 支持查看最近一次清理结果。

## Non-Functional Requirements
- [ ] NFR-1: 清理缓存不能误删用户主动导出的文件。
- [ ] NFR-2: 清理操作要在后台执行并给出进度反馈。

## API Contract
本功能主要依赖本地文件与数据库能力，不依赖 FastGPT API。

## UI Description
页面展示各类缓存占用卡片和清理按钮；执行清理时显示进度和回收空间结果；危险清理操作需要确认。

## Data Model
- `StorageUsageItem`
- `CacheCleanupRequest`

## Architecture Notes
缓存统计与清理能力下沉到 `:core:common` 或 `:core:database`，设置页只负责展示与触发。

## Dependencies
- 本地文件系统
- Room
- 图片缓存

## Acceptance Criteria
- 用户能看到主要缓存类别的占用情况。
- 清理后空间数字和实际文件状态一致。

