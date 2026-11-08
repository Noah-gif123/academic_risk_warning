# v3.0 CI 报告

- 时间：2026-11-08T19:20:12+08:00
- 数据库：`study_warning_system_v3`；总耗时：39.7s
- 结果：**PASS**（6/6 步通过）

| # | 步骤 | 结果 | 耗时 | 证据 |
|---|---|---|---|---|
| 1 | compile | ✅ PASS | 13.3s | javac OK |
| 2 | unit-tests | ✅ PASS | 1.4s | RiskMathTest 20/20；TriggerGateTest 5/5 |
| 3 | data-verify | ✅ PASS | 2.7s | 43/43 通过（PASS） |
| 4 | report-audit | ✅ PASS | 9.7s | class/201 6/6；offering/101 10/10；adminClass/1 6/6 |
| 5 | baseline-diff | ✅ PASS | 0.5s | 与 baseline 一致 |
| 6 | stats-verify | ✅ PASS | 11.9s | == 统计对账：PASS（22/22），报告 -> E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v3.0\evidence\verify_stats.md |

> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/ci.ps1`