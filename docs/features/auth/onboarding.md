---
status: implemented
priority: P1
phase: 1-mvp
---

# Onboarding

## Overview
首次启动引导负责介绍 Kora 的定位、说明需要 FastGPT 连接，并把用户带到连接配置。

## Functional Requirements
- [ ] FR-1: 展示简短欢迎与能力说明。
- [ ] FR-2: 引导用户完成连接配置和首次测试。
- [ ] FR-3: 完成后进入 app selector 或默认聊天首页。

## Non-Functional Requirements
- [ ] NFR-1: 步骤简洁，避免冗长导览。
- [ ] NFR-2: 中断后可从合理位置恢复。

## API Contract
连接校验依赖 [../../api/authentication.md](../../api/authentication.md)。

## UI Description
建议三步：欢迎、能力简介、连接配置。完成后记录 `onboarding_completed=true`。

## Data Model
- `OnboardingState(currentStep, completed)`

## Architecture Notes
onboarding 由 app 层决定是否显示；连接配置页直接复用 settings/auth 模块的表单和校验逻辑。

## Dependencies
- [../settings/connection-config.md](../settings/connection-config.md)

## Acceptance Criteria
- 首次用户可在引导中完成连接并进入主流程。
- 已完成用户不再重复看到欢迎步骤。
