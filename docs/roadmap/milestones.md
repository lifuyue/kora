# Milestones

## Current Stage
项目主功能已经成形，当前里程碑从“继续补阶段规划”切换到“测试收敛与回归效率提升”。

## Active Milestones

### T1: 回归基线收敛
- 固化聊天、知识库、设置三条主链路的最小自动化回归。
- 让真实高频回归优先落到 unit / Robolectric / Compose 测试，而不是只靠模拟器复现。

### T2: 测试分层清晰化
- 区分 pure unit、repository contract、Compose screen、acceptance harness 四层职责。
- 清理重复、低信号、只验证实现细节的测试。

### T3: 设备验收最小化
- 保留少量高价值的模拟器路径验收。
- 把设备验证从“主要发现手段”降级为“最终链路确认”。

### T4: 构建与回归提速
- 优先治理 flaky 测试、缓存损坏、增量编译不稳定和超长回归链。
- 让模块级定向测试成为默认开发路径。

## 不再维护的阶段文档
- 原 `phase-1-mvp.md`
- 原 `phase-2-knowledge.md`
- 原 `phase-3-advanced.md`

这些文档描述的是早期规划阶段，不再作为当前开发方向依据。
