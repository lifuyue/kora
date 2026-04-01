#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path


SCENARIOS = [
    {
        "slug": "openrouter",
        "title_cn": "OpenRouter 配置",
        "title_en": "OpenRouter Routing",
        "primary": "offline cache",
        "secondary": "模型路由",
        "synonym": "路由缓存",
        "code_prefix": "or",
        "mobile": "首屏延迟",
    },
    {
        "slug": "notify",
        "title_cn": "Android 通知权限",
        "title_en": "Android Notification Permission",
        "primary": "permission gate",
        "secondary": "通知权限",
        "synonym": "提醒授权",
        "code_prefix": "nt",
        "mobile": "后台保活",
    },
    {
        "slug": "compose",
        "title_cn": "Compose 列表性能",
        "title_en": "Compose List Performance",
        "primary": "lazy list",
        "secondary": "滚动卡顿",
        "synonym": "列表预取",
        "code_prefix": "cp",
        "mobile": "掉帧控制",
    },
    {
        "slug": "rag",
        "title_cn": "本地 RAG 检索",
        "title_en": "Local RAG Retrieval",
        "primary": "semantic overlap",
        "secondary": "分块重叠",
        "synonym": "段落续接",
        "code_prefix": "rg",
        "mobile": "索引体积",
    },
    {
        "slug": "delta",
        "title_cn": "Delta Stream 重连",
        "title_en": "Delta Stream Recovery",
        "primary": "heartbeat window",
        "secondary": "流式重连",
        "synonym": "连接续传",
        "code_prefix": "ds",
        "mobile": "省电模式",
    },
    {
        "slug": "sqlite",
        "title_cn": "SQLite 清理策略",
        "title_en": "SQLite Cleanup",
        "primary": "vacuum window",
        "secondary": "存储清理",
        "synonym": "压缩整理",
        "code_prefix": "sq",
        "mobile": "磁盘占用",
    },
    {
        "slug": "image",
        "title_cn": "图片上传压缩",
        "title_en": "Image Upload Compression",
        "primary": "upload budget",
        "secondary": "分辨率压缩",
        "synonym": "体积收敛",
        "code_prefix": "im",
        "mobile": "弱网耗时",
    },
    {
        "slug": "token",
        "title_cn": "Token 预算规划",
        "title_en": "Token Budget Plan",
        "primary": "context budget",
        "secondary": "提示词预算",
        "synonym": "上下文配额",
        "code_prefix": "tk",
        "mobile": "响应时延",
    },
    {
        "slug": "sync",
        "title_cn": "后台同步重试",
        "title_en": "Background Sync Retry",
        "primary": "retry ladder",
        "secondary": "断点同步",
        "synonym": "重试阶梯",
        "code_prefix": "sy",
        "mobile": "电量约束",
    },
    {
        "slug": "audio",
        "title_cn": "音频转写清洗",
        "title_en": "Audio Transcript Cleanup",
        "primary": "segment merge",
        "secondary": "转写纠错",
        "synonym": "片段归并",
        "code_prefix": "au",
        "mobile": "端侧内存",
    },
]


def repeated_line(text: str, count: int) -> str:
    return " ".join(text for _ in range(count))


def make_boundary_body(scenario: dict[str, str], code: str) -> str:
    lead = repeated_line(
        f"{scenario['title_cn']} 的背景记录强调移动端需要平衡准确率与耗时，常见讨论会反复提到性能、缓存、稳定性。",
        8,
    )
    anchor = (
        f"关键规则：编号 {code} 在边界 chunk 里必须保留 {scenario['primary']} 与 {scenario['secondary']}，"
        f"否则 {scenario['mobile']} 指标会失真。"
    )
    tail = repeated_line(
        f"补充说明继续讨论 {scenario['synonym']}、排序权重和噪声词干扰，保证 overlap 命中可以复现。",
        5,
    )
    return f"{lead}\n\n{anchor}\n\n{tail}"


def make_documents() -> list[dict[str, str]]:
    documents: list[dict[str, str]] = []
    for scenario_index, scenario in enumerate(SCENARIOS, start=1):
        code_prefix = scenario["code_prefix"]
        for variant in range(10):
            code = f"{code_prefix}-{scenario_index:02d}-k{variant:02d}"
            document_id = f"bench-{scenario['slug']}-{variant:02d}"
            title_base = f"{scenario['title_cn']} {scenario['title_en']}"
            if variant == 0:
                title = f"{title_base} 指南"
                body = (
                    f"{scenario['title_cn']} 的标准做法同时要求 {scenario['primary']} 与 {scenario['secondary']} 保持一致。"
                    f" 这份指南给出 {code} 的默认流程，并说明如何避免噪声词把真正命中挤出前列。"
                )
            elif variant == 1:
                title = f"杂项记录 {code}"
                body = (
                    f"这里只在正文里顺手提到 {scenario['title_cn']}、{scenario['secondary']} 和 {scenario['primary']}，"
                    f"但标题并不表达真实主题，用来模拟正文误命中。"
                )
            elif variant == 2:
                title = f"{scenario['synonym']} 实战 {code}"
                body = (
                    f"{scenario['synonym']} 是团队内部对 {scenario['secondary']} 的另一种叫法。"
                    f" 实战笔记会同时出现 {scenario['primary']}、{scenario['mobile']} 和 {scenario['title_en']}。"
                )
            elif variant == 3:
                title = f"{scenario['title_cn']} 边界命中 {code}"
                body = make_boundary_body(scenario, code)
            elif variant == 4:
                title = f"{scenario['title_en']} bilingual memo {code}"
                body = (
                    f"This memo mixes English and Chinese. It covers {scenario['title_en']}, {scenario['primary']}, "
                    f"{scenario['secondary']}, and the mobile tradeoff named {scenario['mobile']}."
                )
            elif variant == 5:
                title = f"{scenario['title_cn']} 噪声对照 {code}"
                body = (
                    repeated_line("缓存 性能 稳定性 体验", 18)
                    + f" 真正的区分信息只有 {scenario['secondary']} 和 {scenario['primary']}。"
                )
            elif variant == 6:
                title = f"{scenario['title_cn']} 邻近主题 {code}"
                body = (
                    f"这份文档讨论与 {scenario['secondary']} 相邻的话题，"
                    f"会多次出现 {scenario['primary']}，但结论转向了别的处理路径，不应稳定排第一。"
                )
            elif variant == 7:
                title = f"{scenario['title_cn']} 编号案例 {code}"
                body = (
                    f"排障案例 {code} 记录了 {scenario['secondary']} 的线上故障。"
                    f" 只有这份文档同时给出了编号、{scenario['primary']} 和恢复步骤。"
                )
            elif variant == 8:
                title = f"{scenario['title_cn']} 移动端优化 {code}"
                body = (
                    f"移动端优化专门关注 {scenario['mobile']}。"
                    f" 文档建议限制 chunk 数量，保留 {scenario['primary']}，并让 {scenario['secondary']} 的检索优先命中标题。"
                )
            else:
                title = f"{scenario['title_cn']} 近重复样本 {code}"
                body = (
                    f"这是近重复样本，用于观察排序稳定性。正文包含 {scenario['secondary']}、{scenario['primary']}、{scenario['synonym']}，"
                    f"但编号是 {code}，不应盖过真正的主文档。"
                )
            documents.append(
                {
                    "id": document_id,
                    "title": title,
                    "sourceLabel": "benchmark-fixture",
                    "body": body,
                }
            )
    return documents


def make_queries() -> list[dict[str, object]]:
    queries: list[dict[str, object]] = []
    for scenario_index, scenario in enumerate(SCENARIOS, start=1):
        queries.extend(
            [
                {
                    "query": f"{scenario['title_cn']} {scenario['secondary']}",
                    "expectedDocumentId": f"bench-{scenario['slug']}-00",
                    "minimumRank": 1,
                    "weight": 1.2,
                    "tag": "title_exact",
                },
                {
                    "query": f"{scenario['synonym']} {scenario['mobile']}",
                    "expectedDocumentId": f"bench-{scenario['slug']}-02",
                    "minimumRank": 3,
                    "weight": 1.0,
                    "tag": "synonym",
                },
                {
                    "query": f"{scenario['code_prefix']}-{scenario_index:02d}-k03 {scenario['primary']}",
                    "expectedDocumentId": f"bench-{scenario['slug']}-03",
                    "minimumRank": 1,
                    "weight": 1.1,
                    "tag": "boundary",
                },
                {
                    "query": f"{scenario['title_en']} {scenario['secondary']}",
                    "expectedDocumentId": f"bench-{scenario['slug']}-04",
                    "minimumRank": 3,
                    "weight": 0.9,
                    "tag": "bilingual",
                },
                {
                    "query": f"{scenario['mobile']} {scenario['secondary']}",
                    "expectedDocumentId": f"bench-{scenario['slug']}-08",
                    "minimumRank": 3,
                    "weight": 1.0,
                    "tag": "mobile",
                },
            ]
        )
    return queries


def build_fixture() -> dict[str, object]:
    documents = make_documents()
    queries = make_queries()
    return {
        "metadata": {
            "documentCount": len(documents),
            "queryCount": len(queries),
            "generator": "scripts/generate_local_knowledge_benchmark.py",
        },
        "documents": documents,
        "queries": queries,
    }


def main() -> None:
    repo_root = Path(__file__).resolve().parent.parent
    output_path = repo_root / "core/database/src/test/resources/local_knowledge_benchmark.json"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(build_fixture(), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
