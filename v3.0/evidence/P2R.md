# v3.0 P2-R 证据（RAG 工具化 / 引用绑定 / 独立重放）

- 时间：2026-11-08T20:01:12+08:00
- 结果：**PASS**（10/10）

| # | 检查 | 结果 | 证据 |
|---|---|---|---|
| 1 | P2R-1 工具返回结构化命中 | PASS | keys=chunkId,docId,docName,sectionNo,score,rrfRank |
| 2 | P2R-1 Diagnostician 用同一结构 | PASS | chunkId=790 |
| 3 | P2R-1 Tutor 用同一结构 | PASS | chunkId=786 valid=True |
| 4 | P2R-1 三个入口结构一致 | PASS | tool/diagnoser/tutor keys 一致 |
| 5 | P2R-2 引用可定位 chunk | PASS | citations=4 found=4 |
| 6 | P2R-2 点开原文片段 | PASS | chunk=786 len=254 |
| 7 | P2R-2 轨迹含 chunkId 回溯 | PASS | entries=1 |
| 8 | P2R-3 语料 100-150 篇/9 门课 | PASS | docs=101 chunks=475 courses=9 |
| 9 | P2R-3 五类混淆齐全 | PASS | {"hit":42,"miss":7,"correctRefuse":8,"falseRefuse":5,"emptyCandidate":2,"total":64} |
| 10 | P2R-3 独立重算差异 <0.5% | PASS | recall=0.8148 vs 0.8148；refuse=0.8 vs 0.8；false=0.0926 vs 0.0926 |

> 复现：`powershell -File v3.0/tool/verify_p2r.ps1`