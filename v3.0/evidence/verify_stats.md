# v3.0 P1-8 统计对账报告

- 时间：2026-11-08T16:53:22+08:00
- 数据库：`study_warning_system_v3`；termId=5
- 结果：**PASS**（11/11）
- 耗时：10.3s

| # | 检查 | 结果 | 证据 |
|---|---|---|---|
| 1 | overview 风险面 | PASS | API 1927/141/138/383/1265 ratio=34.35；SQL 一致 |
| 2 | overview 关闭率 | PASS | API 54.83%；SQL 54.83%（363/662） |
| 3 | distribution 分布/校准 | PASS | 等级 RED=141；课程 9 门；校准 9 门 |
| 4 | compare 班级行数 | PASS | API 50 行；SQL 50 |
| 5 | funnel 新增=SQL | PASS | API created=662；SQL alerts=662 |
| 6 | effect T+7/对照组 | PASS | API due=510 improved=214 TREAT=54.18% CONTROL=24.64% diff=29.54pp；SQL due=510 improved=214 |
| 7 | trend 最新周风险面 | PASS | points=8 last=2026-11-02 ratio=34.35；SQL=34.35 |
| 8 | export CSV 与接口一致 | PASS | CSV 首行=指标,数值 |
| 9 | export XLSX 可下载 | PASS | bytes=3941（PK 头） |
| 10 | 权限 401/403 | PASS | 无 token=401；学生=403 |
| 11 | 辅导员范围裁剪 | PASS | C001=1222；SQL=1222 |

> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_stats.ps1`