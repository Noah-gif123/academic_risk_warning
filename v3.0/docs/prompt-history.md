# Prompt 演进记录

本文件记录系统实际使用的 Prompt 版本、变更原因和证据位置。原则：

> Prompt 文本变更必须可追溯；不能只保留最终版本。

## 1. 当前版本清单

| Prompt | 当前版本 | 代码/文件位置 | 用途 |
|---|---|---|---|
| 群体报告（教学班/课程/行政班） | `group-report-v1` | `GroupReportService.groupSystemPrompt` + `persist` | 生成班级/课程/行政班报告 |
| 学生报告 | `report-student-v1` | `StudentReportService` | 学生单课程评价报告 |
| 预警解释卡 | `alert-explain-v1` | `AlertExplainService` | 解释单个预警 |
| AI 批改辅助 | `grading-assist-v1` | `GradingAssistService` | 批改建议与评语 |
| AI 出题 | `question-gen-v1` | `QuestionGenService` | 题目草稿 |
| Diagnostician（本地/Dify 共用） | `p3-diagnostician-v1` | `v3.0/p3/diagnostician-system-prompt.md` | 学情研判 + 工具调用 |

## 2. 变更记录

### 2026-11-08：事实包与报告基线

- 建立群体报告事实包：风险分布、课程风险、平均分、缺勤超限、重点学生。
- 确立约束：**数字由规则引擎计算，模型只负责写作**。
- 证据：`v3.0/evidence/P0.md`。

### 2026-11-09：报告结构与图表字段

- 教学班报告扩展为：报告摘要 / 班级画像 / 风险结构 / 成绩与考核项 / 重点关注学生 / 教学动作。
- 课程报告扩展为：课程摘要 / 教学班对比 / 课程风险结构 / 成绩与考核项 / 跨班重点学生 / 课程教学动作。
- 行政班报告扩展为：报告摘要 / 班级画像 / 集中风险课程 / 多门预警学生 / 班级风险结构 / 重点关注学生 / 干预动作。
- 事实包增加 `courseRisks`、`items`、`multiRiskStudents` 等图表字段，长名单改为数据图表。
- 注意：代码中的 `prompt_version` 仍标记为 `group-report-v1`；结构与 Prompt 文本已扩展但未 bump 版本，这是需要继续收口的技术债。

### 2026-11-10：Diagnostician Prompt 与工具契约

- 创建 `v3.0/p3/diagnostician-system-prompt.md`，版本 `p3-diagnostician-v1`。
- 约束：
  - 只能调用 8 个只读工具；
  - 每个结论必须有工具证据；
  - `evidence` 的 `value` 必须为标量；
  - 无证据时拒答；
  - 不输出学号、内部 ID、时间戳。
- 证据：`v3.0/evidence/P3.md`、`v3.0/eval/p3_parity.json`。

### 2026-11-15：数字对账与模型记录

- 修复行政班报告课程级风险率与班级总风险率的误判；`NumberAuditor.fromAdminClass` 支持 per-course `风险率`。
- 报告落库记录真实模型（Dify 用 `ai.model`，直连用 `directModel`），不再固定写 `qwen-plus`。
- 报告返回 `model` / `promptVersion` / `tokens`，前端显示来源徽标。
- 证据：`v3.0/eval/ablation_v3.json`、`v3.0/evidence/P0.md`。

## 3. 评测与证据

| 资产 | 说明 |
|---|---|
| `v3.0/eval/agent_eval_report.json` | 30 题工具/数字一致性 |
| `v3.0/eval/p3_parity.json` | 本地 vs Dify 30 题 parity |
| `v3.0/eval/rag_replay.json` | RAG 检索回放 |
| `v3.0/evidence/P0.md` | 数据底座与消融 |
| `v3.0/evidence/P3.md` | Dify Diagnostician 证据 |

## 4. 后续版本规则

- 任何 System Prompt 文本变化都要 bump 版本；
- 每个版本记录：修改原因、影响范围、评测结果；
- 报告表中的 `prompt_version` 必须与文档一致；
- 若只修改报告结构而不修改 Prompt，也要记录为“结构变更”，不能静默覆盖。
