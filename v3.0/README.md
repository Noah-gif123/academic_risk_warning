# v3.0 工作目录

> 本项目目录只放 **v3.0 的增量与文档**，不复制 v2.0 源码。
> v3.0 的代码改动仍在 `v2.0/backend`、`v2.0/frontend` 上做（因为 v3.0 是 v2.0 的延续），
> 本目录负责：数据脚本、工具脚本、评测资产、证据卡、文档。

## 目录布局（对齐 v2.0）

```
v3.0/
├─ backend/          后端增量说明（如新增统计/Agent 模块的改动清单，不复制源码）
├─ frontend/         前端增量说明（统计看板、轨迹页、记忆页、草稿交互）
├─ ai/               本机 Dify 配置与导出
│  └─ dify/          Agent 应用导出、共享 system prompt、启动脚本
├─ sql/              v3.0 数据脚本（判定修复、拟真数据、统计表）
│  └─ seeds/         params.json、data_story.md
├─ tool/             生成器、重算流水线、ci.ps1、评测脚本、PPT/文档生成脚本
├─ eval/             agent_cases.json、baseline.json、parity_report.md
├─ evidence/         P0–P5 证据卡（截图 + SQL/命令 + 关键数字）
├─ word/             文档产物（v3.0规划清单.docx 等）
├─ PLAN_v3.0.md      规划正文（终版 v3.0.3）
└─ README.md         本文件
```

## 已确认的关键口径

| 项 | 口径 |
|---|---|
| 关闭率分母 | 周期内新增预警 |
| 复评窗口 | 干预后第 7 天（<7 天不计改善，>14 天标逾期未复评） |
| Dify 部署 | 本机（复用 `v2.0/ai/dify`，端口 8090） |
| Agent 角色 | 四角色 Planner / Executor / Critic / Writer，支持自适应 2/4 |
| 记忆 | 按事件类型可开关；学生端可查可删 |
| 看板范围 | 先辅导员 + 教务，教师端 v3.1 |

## 文档

- `word/v3.0规划清单.docx`：可勾选执行的任务清单（P0–P5，49 项，约 42 人日）
- `PLAN_v3.0.md`：规划正文（决策、验收、架构、口径、风险、不做清单）
- 生成脚本：`tool/gen_v30_plan_docx.py`（改内容后重跑即可）

## 约定

1. **可回滚**：动数据前先有 `sql/snapshot_v2.0.sql`，任何时刻能退回 v2.0 演示快照。
2. **一条命令重算**：`tool/run_v3_pipeline.ps1`（seed → 判定 → 报告 → 快照 → 基线）。
3. **不推送**：v3.0 的全部改动只记录在本机。
