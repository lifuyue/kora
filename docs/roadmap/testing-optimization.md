# Testing Optimization Roadmap

## Direction
从当前功能出发，下一阶段主导方向是“测试优化应用”：
- 让已有聊天、知识库、设置能力更稳
- 降低每次修复后的回归成本
- 缩短从定位问题到确认修复的闭环

## Why Now
- 主要能力已经落地，新增大块规划文档的收益下降
- 最近的高成本问题集中在聊天发送、流式解析、导航参数和设备侧状态不一致
- 这些问题更适合通过契约测试、状态测试和少量设备验收解决

## Workstreams

### A. Chat First
- 继续强化 `RoomChatRepositoryTest`、`ChatRepositoryTest`、`ChatViewModelTest`
- 把 direct-openai、FastGPT、兼容模式差异都沉到测试
- 为高频回归保留可单独运行的定向测试命令

### B. Knowledge Catch-up
- 为数据集浏览、导入、检索测试补充 repository 和 ViewModel 层覆盖
- 重点不是堆 UI snapshot，而是校验状态转换和错误路径

### C. Settings Stability
- 保持连接配置、语言、主题、音频偏好的现有覆盖
- 对真正影响启动和聊天主链路的设置行为补 acceptance 级验证

### D. Test Runtime Hygiene
- 修复 flaky 测试、缓存损坏、增量编译异常
- 优化开发默认命令，让模块级回归优先于全仓库长链

## Exit Criteria
- 聊天主链路回归可通过定向测试在分钟级完成
- 新回归默认能先在自动化测试中复现
- 设备验证只保留少量必要链路
- 文档入口不再指向过期阶段计划，而是指向当前测试策略
