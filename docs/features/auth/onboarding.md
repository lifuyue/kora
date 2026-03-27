---
status: draft
priority: P1
phase: 1-mvp
---

# Onboarding

## Overview
首次启动引导用户理解 Kora、配置服务器连接并进入首个聊天会话。

## Functional Requirements
- [ ] FR-1: 首次启动展示项目价值和基本说明。
- [ ] FR-2: 引导用户完成连接配置。
- [ ] FR-3: 配置完成后进入默认首页。

## Non-Functional Requirements
- [ ] NFR-1: 引导流程要短，避免不必要步骤。
- [ ] NFR-2: 中途退出后再次进入应从合理位置恢复。

## API Contract
依赖 [../../api/authentication.md](../../api/authentication.md) 相关连接与认证约束。

## UI Description
引导页可分为欢迎、能力介绍和连接配置三步；用户完成配置后直接跳转聊天首页；已完成引导用户不再重复看到欢迎页。

## Data Model
- `OnboardingStep`
- `OnboardingCompletionState`

## Architecture Notes
引导流程由 `:app` 决定首屏分发，具体配置页面复用设置与认证模块的能力。

## Dependencies
- 认证流程
- DataStore
- 导航

## Acceptance Criteria
- 首次启动可完整走通引导。
- 已完成引导的用户不会反复进入欢迎流程。

