# Solutions

## 记录标准
只记录已经验证有效、后续可以直接复用的解决方案。不要记录尚未稳定的尝试过程。

## 条目模板
### 标题
- 问题信号:
- 最小解决方案:
- 适用条件:
- 验证方式:
- 关联 mistake:

## 当前记录
暂无。形成可复用方案后再补充。

### 本地轻量 RAG 基准集
- 问题信号: 需要稳定衡量本地知识检索的精度和批量性能，但手工样本不可复现。
- 最小解决方案: 用仓库内确定性脚本 `scripts/generate_local_knowledge_benchmark.py` 生成固定 JSON 语料 `core/database/src/test/resources/local_knowledge_benchmark.json`，测试从 `src/test/resources` 读取，精度和性能阈值都保持宽松。
- 适用条件: Room-backed 本地检索、Robolectric 单测、需要覆盖长文分块和中英混合样本。
- 验证方式: 重新运行 `python3 scripts/generate_local_knowledge_benchmark.py`，再执行 `./gradlew :core:database:testDebugUnitTest --tests com.lifuyue.kora.core.database.LocalKnowledgeBenchmarkTest`。
