# v3.0 规划（终版 v3.0.4）

> 状态：**方向、口径、验收、工程约定全部确认，等待开工**
> 目标：从「一份能讲清的报告」升级为「会自己查、会自己算、有记忆、能证明成效」的系统
> 约束：本机开发与演示（含本机 Dify）；文档与代码只记录本机，不推送远端

---

## 〇、决策记录（全部已确认）

| # | 议题 | 决策 |
|---|---|---|
| 1 | `course_factor` 未初始化 | v3.0 第一件事修复，并重跑 C1/C2 消融 |
| 2 | 演示叙事 | 保留「02 班最吃力 + 重修班黄灯多」 |
| 3 | Agent 路线 | 自研主线 + Dify 双实现（**范围以 #17 为准：仅 Diagnostician 做 Dify 版**）；Dify 本机部署 |
| 4 | Agent 角色 | 四角色 Planner / Executor / Critic / Writer |
| 5 | Agent 记忆 | 要做；跨学期长期画像，写作时引用 |
| 6 | 统计看板 | 先辅导员 + 教务；教师端推迟 v3.1 |
| 7 | 统计口径 | 关闭率分母＝周期内新增；复评窗口＝干预后 7 天 |
| 8 | 记忆采样 | 要，做成按事件类型可开关（问卷/答疑低权重） |
| 9 | 评测集 | 我起草期望工具链，你复核；**期望数字由 SQL 生成** |
| 10 | CI | 要，本地一键脚本，不上云 |
| 11 | **证据台账 EvidenceLedger** | 采纳：Agent 每个数字必须命中台账（取代"文本 vs 事实包"） |
| 12 | **对照组** | 采纳：演示数据构造「延迟干预组」，使改善率可归因 |
| 13 | **记忆合规 + 草稿追踪** | 采纳：记忆可查可删；干预表记 `ai_draft_id` |
| 14 | **自适应编排** | 采纳：简单问题 2 角色、复杂问题 4 角色，可配 `auto/2/4` |
| 15 | **最小交付切分** | 采纳：P0+P1 为可随时停下的最小可用集 |
| 16 | **业务智能体清单** | 4 核心：学情研判 Diagnostician / 报告撰写 Reporter / 批改 Grader / 答疑 Tutor；2 可选：干预参谋 Coach / 数据质检 Steward；**不做**「周报解读 Agent」（Writer 已覆盖） |
| 17 | **实现归属** | **全自研为主**；仅 **Diagnostician 额外做一个 Dify Agent 版**（同一套 ToolSpec 生成 OpenAPI→Dify 工具，做 parity + 画布演示）；Tutor 的**检索段**复用 Dify 知识库，引用校验/拒答/记忆/落库留在自研层；其余 agent 不做 Dify 版 |
| 18 | **协作与冲突** | 智能体之间**不互相调用**，由 `IntentRouter` 分派、`AgentOrchestrator` 串流程；冲突一律**以证据台账为准**，台账缺证据就降级，**最终由人拍板** |

---

## 一、总体验收（做完才算 v3.0）

| 维度 | 指标 | 门槛 |
|---|---|---|
| 数据 | C1 消融效应量 | 教师松紧 1.5–3%、课程因子 2–4%、历史挂科率 1–2% |
| 数据 | 班间 avg 差 / 班内 sd | 同课程班间 6–12 分；班内 sd 9–12 |
| 数据 | 重算流水线 | 一条命令跑通（seed→判定→报告→快照→基线），耗时 <3 分钟 |
| Agent | 30 题：工具选择正确率 / 数字一致率 / P95 / 平均步数 | ≥90% / 100% / <8s / 2.5–3.5 |
| Agent | **业务智能体落地数** | 4 个核心可用（Diagnostician/Reporter/Grader/Tutor 各有契约与独立验收），不是"四角色"充数 |
| Agent | **证据台账命中率** | 100%（Writer 的每个数字都能追到某步工具返回） |
| Agent | 护栏注入 10 例 | 100% 有明确降级与留痕 |
| Agent | 自适应编排 | `auto` 模式平均调用次数比固定 4 角色低 ≥25%，质量不降 |
| Agent | 记忆有效性 | 同生二次提问能引用上次结论；不重复问已知信息 |
| Parity | LOCAL vs DIFY（30 题，共享 prompt） | 关键数字一致 ≥95% 或结论方向一致 ≥90% |
| 统计 | 图表 ↔ SQL 一致 | 100%（脚本断言）；首屏 <2s；快照过期可见 |
| 统计 | **对照组差值** | 干预组 7 天改善率 − 对照组改善率 ≥20 个百分点（演示数据可复现） |
| 合规 | 记忆可查可删 | 学生端可见摘要并可删除，删除后不再注入 |

---

## 二、轨道 A：Agent 平台化

### A0. 业务智能体清单与实现归属（v3.0.4 补充）

> 先回答一个必须诚实面对的问题：v2.0 的 AI 是**六场景一次性调用**，代码里没有任何 Agent 类。
> v3.0 要名副其实，业务智能体必须按"有独立职责、有工具、有输出契约、有独立验收"来落。

**清单一览**

| # | 智能体 | 职责（一句话） | 主实现 | Dify 版 | 工期 | 档位 |
|---|---|---|---|---|---|---|
| 1 | **Diagnostician** 学情研判 | 多步取证后回答"这个学生/班级为什么危险、证据是什么" | 自研 | **做（parity 用）** | 3.0 人日 | 核心 P2 |
| 2 | **Reporter** 报告撰写 | 每学期三类报告（学生/班级/教务），把已算好的判定写成人话 | 自研 | 不做 | 1.5 人日 | 核心 P2 |
| 3 | **Grader** 批改 | 主观题对照评分要点给分并给扣分理由，人可改 | 自研 | 不做 | 1.5 人日 | 核心 P2 |
| 4 | **Tutor** 答疑 | 结合课程知识库 + 该生学情回答学习问题，越界拒答 | 自研（检索段用 Dify KB） | 不做 | 1.0 人日 | 核心 P2 |
| 5 | **Coach** 干预参谋 | 基于研判结论起草干预方案（对象/动作/频次/复评日期） | 自研 | 不做 | 0.5 人日 | 可选 P4 |
| 6 | **Steward** 数据质检 | 扫描缺考/缺勤异常/成绩缺失，提示"判定可能失真" | 自研 | 不做 | 1.0 人日 | 可选 P4 |
| — | ~~周报解读~~ | — | — | — | — | **不做**（Writer + Reporter 覆盖） |

**实现原则（四条，写进代码评审清单）**

1. **自研为主**：业务智能体一律 Java 实现，走本地工具注册表。原因：要可追溯（每步入台账、落库可查）、可评测（30 题脚本化比对）、可离线（答辩现场不依赖外网）、可改分（人可改 AI 的评分与结论）。
2. **Dify 只用在能加分的地方**：`Diagnostician` 做 Dify Agent 版，作用是①同一份 ToolSpec 生成 OpenAPI 已验证契约可移植；②答辩时能展示"低代码画布跑出同样结论"；③parity 数据（LOCAL vs DIFY）本身是评测亮点。**它不是第二套产品，是同一份契约的第二个执行器。**
3. **Tutor 只借知识库**：Dify 仅承担"课程资料召回"，召回结果回到自研层做引用校验、拒答判定、记忆写入与留痕——避免"知识库一接，可信性就脱手"。
4. **其余不做 Dify 版**：Reporter/Grader/Coach/Steward 做第二实现维护成本翻倍、演示收益接近 0，明确写进不做清单（见第八节）。

**统一契约（每个智能体都必须满足，缺一不算"智能体"）**

| 项 | 要求 |
|---|---|
| 输入 | 显式参数对象（学生/教学班/学期/问题），不接受自由文本直接当参数 |
| 工具集 | 只允许 `A2` 白名单里的工具；只读，`draftIntervention` 只出草稿 |
| 最大步数 | 全局 ≤4 步；单步超时 3s；总预算 12s、token 上限可配 |
| 输出 | 强 schema（结论 / 证据引用 / 动作 / 置信度 / 是否拒答），结构化落库 |
| 校验 | 三道闸门：结构 → 证据台账对账 → 拒答与越权检查 |
| 降级 | 模型不可用 → 模板；台账不命中 → 重写 1 次 → 仍失败则模板 + 标注"未通过校验" |
| 人确认 | 涉"改数据/发通知/定成绩"的动作**只出草稿**，一律人工确认后落库 |
| 留痕 | `ai_run` + `ai_run_step` + 台账，轨迹页可逐步点开 |

**协作方式（不做自由群聊）**

```
用户问题
   │
   ├─ IntentRouter（规则优先 + 模型兜底，命中即定）
   │    单生诊断→Diagnostician  班级趋势/对比→Diagnostician(+Reporter)
   │    学期报告→Reporter        主观题批改→Grader
   │    学习答疑→Tutor           干预方案→Coach(Diagnostician 结论为输入)
   │
   └─ AgentOrchestrator（固定流水，串行，≤1 次 Critic 驳回）
        Diagnostician → (可选)Reporter → Writer → 三道闸门 → 发布/降级
        Grader / Tutor / Steward 单智能体自闭环，不走多步编排
```

**冲突与失败（答辩必问，先写答案）**

| 情形 | 处理 |
|---|---|
| 两个智能体结论不一致 | 不回环争论：以**台账里的数字**为准；数字一致而结论不同 → 输出"存在分歧"并附两方依据，交人判断 |
| Critic 认为证据不足 | 回 Planner 继续取证，最多 4 步；仍不足 → 明确输出"证据不足，无法断言"，不编 |
| 工具失败/超时 | 该步留痕失败，按最小证据集判断能否继续；不能 → 降级模板并标注缺哪个字段 |
| 模型与规则冲突（如策略要求"必须给建议"但证据不足） | **规则优先**：拒答 + 说明缺什么，不允许为了好看而生成 |
| 用户要求越权（教师问别班学生） | 路由层先鉴权，未通过直接拒答并记 `ai_run.gate_status=REJECTED` |

**逐智能体验收（并入 P2 交付条件）**

| 智能体 | 独立验收 |
|---|---|
| Diagnostician | 30 题工具选择 ≥90%、台账命中 100%、P95<8s；Dify 版 parity（数字一致 ≥95% 或方向一致 ≥90%） |
| Reporter | 三类报告结构 100%、与判定快照逐字段对账 100%、生成 3–15s |
| Grader | 可评样本 MAE ≤2 分、评分要点逐条有对应、人改后不留旧分 |
| Tutor | 60 题盲评：引用命中率 ≥0.9、该拒答时拒答率 ≥0.9、误拒 ≤10% |
| Coach（可选） | 草案含对象/动作/频次/复评日期四要素 100%，不得自动下发 |
| Steward（可选） | 注入 10 类脏数据，检出 ≥8 类，且不误伤正常样本 |

### A1. 契约层：ToolSpec 单一来源（含证据字段）

```yaml
- name: getClassRiskStats
  description: 查某教学班本学期的风险分布与未关闭预警数
  read_only: true
  timeout_ms: 3000
  params:
    teachingClassId: { type: integer, required: true }
    termId: { type: integer, required: false }
  returns: { studentCount: int, red: int, yellow: int, green: int,
             hardFail: int, activeAlertCount: int, avgScore: number }
  evidence_fields: [red, yellow, green, studentCount, hardFail, activeAlertCount, avgScore]
```

- `evidence_fields` 声明"哪些数字可被引用"，是证据台账入账的依据
- 同一份契约生成：本地工具注册表 + Dify 的 `tools.openapi.json`

### A2. 工具清单（9 个，全只读）

`getClassRiskStats`（现有）、`getRiskTrend`（新增·统计）、`getCourseCompare`（现有）、
`getStudentRisk`（现有）、`getInterventions`（现有）、`retrieveCourseDocs`（RAG）、
`getAlertExplain`（现有）、`getStudentMemory`（新增·记忆）、`draftIntervention`（新增·只出草稿）

### A3. 四角色 + 自适应编排

```
问题 →[Planner]→ [Executor] →[Critic]─不够→ 回 Planner（≤4 步）
                                   └够→ [Writer] → 校验（结构/台账对账）→ 发布或降级
```

| 角色 | 职责 | 独立验收 |
|---|---|---|
| Planner | 选下一步工具 + 参数 + 理由 | 30 题中 ≥20 题第一步合理 |
| Executor | 执行：白名单、参数校验、超时、留痕 | 成功率 100%，非法参数 100% 拦截 |
| Critic | **规则（最小证据集）+ 模型**判断证据是否足够 | 10 个"信息不足"用例 ≥8 次要求继续取证；连续两步同工具同参数判空转 |
| Writer | 结论 + 3 条动作 + 依据（引用台账与记忆） | 结构 100%、台账命中 100%、动作含对象与频次 |

**最小证据集**（Critic 的规则部分）：

| 问题类型 | 必需字段 |
|---|---|
| 单生诊断 | 该生三通道判定 + 缺勤 + 需考分 |
| 班级趋势 | 近 4 周等级序列 + 当前分布 |
| 跨班对比 | 各班红黄灯 + 平均分 + 最低分 |
| 干预成效 | 干预记录 + T+7 复评等级 |
| 记忆类 | 记忆摘要命中（≥1 条事件） |

**自适应**：`agent.roles: auto|2|4`
- `auto`：问题命中"单生/记忆"→ 2 角色（Planner+Writer）；"趋势/对比/成效"→ 4 角色
- 评测时对比 `auto` 与固定 `4` 的质量与调用次数

### A4. 证据台账（EvidenceLedger）——v3.0 可信性核心

```
每次运行维护一张台账：{ 数字: {值, 来源步号, 工具名} }
步1 getClassRiskStats → 入账 red=6, yellow=46, alerts=52
步2 getRiskTrend     → 入账 week12.yellow=41
Writer 输出 → 抽取所有数字 → 逐个查台账
   命中 → ✅ ；未命中 → ❌ 标红并要求重写（最多 1 次），仍不通过则降级
```

- 台账随运行落库（`ai_run_step` 扩展或独立 `ai_evidence_ledger`）
- 轨迹页展示"每个数字来自第几步"，答辩可逐项点开
- 这是 v2.0「数字白名单 + 定向对账」在多步场景下的正确升级形态

### A5. 记忆（跨学期，含合规）

| 表 | 关键字段 |
|---|---|
| `student_memory` | student_id、summary、top_weak_groups、attendance_tag、intervention_count、last_intervention_conclusion、recent_levels、updated_at |
| `student_memory_event` | student_id、type、summary、source_id、weight、created_at |

配置：

```yaml
app.ai.memory:
  enabled: true
  types:
    RISK_CHANGE:   { enabled: true, weight: high }
    INTERVENTION:  { enabled: true, weight: high }
    QA:            { enabled: true, weight: low }
    QUESTIONNAIRE: { enabled: true, weight: low }
  summaryMaxChars: 300
```

- 高权重进摘要，低权重只进事件流；关闭类型后不再写入，已有摘要保留并在轨迹页标注
- **合规**：学生端提供「我的画像摘要」查看 + 删除；删除后 `getStudentMemory` 不再返回，事件流保留审计 id 但不注入

### A6. 观测与轨迹页

`ai_run` 增 `provider(LOCAL/DIFY)`、`role_count`；`ai_run_step` 增 `step_role`、`tool_name`、`args_digest`、`result_digest`、`gate_status`。
轨迹页：左问题 + 最终答案（含每个数字的来源步号）；右时间线（角色/工具/入参/返回摘要/耗时/校验）；顶部步数、耗时、token、provider、是否降级。

### A7. 评测集（30 题，期望数字由 SQL 生成）

| 类型 | 题数 |
|---|---|
| 单生诊断 | 6 |
| 班级趋势 | 6 |
| 跨班对比 | 5 |
| 干预成效 | 5 |
| 记忆类 | 4 |
| 应拒答（越权 / 无法断言） | 4 |

流程：
1. 我写 `v3.0/eval/agent_cases.json`（问题 + 期望工具链 + 期望 SQL 语句 + 拒答标记）
2. 脚本执行这些 SQL → 生成 `expected_numbers`（**不手写**）
3. 评测比对 Agent 输出与 SQL 真值；你只复核"工具链合理性"与"4 道拒答题是否该拒"

---

## 三、轨道 B：数据层（P0）

### B1. 必修 Bug

| 问题 | 修法 |
|---|---|
| `course_factor` 全 NULL | `factor = clamp(1 + k*(fail_rate - baseline), 0.90, 1.15)`，写回 `is_initialized=1`；引擎读取 NULL 时按 1.0 并记 WARN |
| λ 恒定 0.620 | 改为随样本量变化（`λ = n/(n+k)`），小样本收缩更狠 |
| 无熔断案例 | 留 1–2 个班样本 <10，`fused=1` + 原因 |

### B2. 拟真参数（生成器 + `data_story.md`）

| 参数 | 目标 |
|---|---|
| 教师 / 课程 / 行政班 | 12–15 / 8–10 / 6–8（30–45 人） |
| 学期 | 当前 + 3 个历史 |
| 班间 avg 差 / 班内 sd | 6–12 分 / 9–12 |
| 课程挂科率 | 3%–25% |
| 干预覆盖率 | **60%**（其余进入"延迟干预组"，见 D2） |
| 姓名 | 中文拟真姓名库（去重） |

生成器输出 `v3.0/sql/seeds/data_story.md`：每个班/课的参数与"成因"（为什么这个班均分低、为什么这个老师偏严），答辩被问数据怎么造时有话讲。

### B3. 重算流水线（一条命令）

```
seed v3.0 → 重算判定 → 重生成演示报告（3 类） → 重算统计快照 → 更新对账基线 → 输出 data_story.md
```

- 幂等、可回滚（`snapshot_v2.0.sql`）；耗时目标 <3 分钟
- 缺这条流水线，改数据要手工点十几次且容易漏

### B4. 回滚与校验

- `v3.0/sql` 独立目录；`snapshot_v2.0.sql` 一键回滚
- 校验脚本：分类合计=样本数、无孤儿选课、无重复判定、三通道非空率、效应量抽样断言

---

## 四、轨道 D：统计（辅导员 + 教务）

### D1. 五个闭环指标（口径已定）

| # | 指标 | 主口径 | 辅助 |
|---|---|---|---|
| 1 | 预警关闭率 | 周期内新增且已关闭 ÷ 周期内新增 | 消化能力（关闭总数÷新增，可 >100%） |
| 2 | 平均处置时长 | 周期内关闭：关闭−创建 的均值（小时） | 中位数 |
| 3 | 干预后等级下降比例 | 干预 + T+7 复评改善数 ÷ 干预数 | 持平/恶化计数 |
| 4 | 7 天复评改善率 | 到期干预中改善数 ÷ 到期数 | 未复评单列 |
| 5 | 风险面变化率 | 红黄灯占比，本周期 vs 上周期 | 分课程/班拆分 |

口径细节：复评早于 7 天不计改善；晚于 14 天标「逾期未复评」；关闭率分母固定"周期内新增"。

### D2. 对照组（使改善率可归因）

- 数据侧构造「延迟干预组」：同批红黄灯学生中 40% 未在本周期干预（或晚 7 天）
- 统计侧输出 `干预组改善率 vs 对照组改善率`，并给出差值（演示数据可复现 ≥20 个百分点）
- 看板上单列一张"对照组对比"，答辩时这是最硬的一页

### D3. 物化快照 + 失效机制

```sql
stat_daily_class(id, stat_date, teaching_class_id, student_count, red, orange, yellow, green,
                 avg_score, alert_new, alert_closed, intervention_count,
                 engine_version, source_max_update_time, is_stale)
stat_weekly_kpi(id, week_start, scope_type, scope_id, metric, current_value, prev_value, delta_rate, is_stale)
stat_intervention_effect(id, week_start, teaching_class_id, cohort /*TREAT|CONTROL*/,
                         intervened, improved, unchanged, worsened, is_stale)
```

- 读取时校验 `engine_version` 与 `source_max_update_time`：过期即重算并标 `is_stale`，前端显示"数据重算于…"
- 每日定时 + 手动重算按钮（幂等）

### D4. 接口与看板

`/api/stats`: `overview` / `trend` / `distribution` / `compare` / `funnel` / `effect` / `export`；统一返回 `current/previous/deltaRate`。

- **辅导员端**：班级风险趋势（折线+变化率）、多门预警学生变化、干预成效漏斗、**对照组对比**、班级对比柱状
- **教务端**：覆盖率与处置率、课程集中风险 Top5、**课程因子校准散点（挂科率 vs factor）**、处置时效分布、AI 质量小卡片
- 每图配「口径说明」气泡（定义 + SQL）

---

## 五、轨道 C：AI 亮点

| 亮点 | 做法 | 指标 |
|---|---|---|
| 报告 → 干预草稿 | 一键生成约谈提纲/补交清单（只出草稿） | 生成率、**采纳率、平均编辑字数**（记 `ai_draft_id`） |
| RAG 评测看板 | 64 例盲评按课程/题型筛选、可重跑 | 命中/拒答/误拒率趋势 |
| 幻觉与降级看板 | 台账未命中数、对账不一致、来源分布 | 台账命中率、拦截率、降级率 |
| 个性化解释卡 | 用记忆调整措辞与动作顺序 | 同生两次解释动作命中率 |
| 流式 + 轨迹同步 | SSE 事件：`step_start/step_end/token/answer` | 首字 <1s，步骤实时可见 |

干预表增 `ai_draft_id`（AI 草稿 → 人工修改 → 采纳/弃用 的追踪）。

---

## 六、Dify 双实现（本机，范围已收窄）

> **结论（决策 17）**：全部业务智能体以自研为主；**只给 Diagnostician 做 Dify 版**。
> 理由：Dify 版的价值不在"多一个功能"，而在三件事——契约可移植性的证明、答辩现场的低代码演示、parity 评测数据。
> 做 4 个 Dify 版等于四份维护成本，收益不增，故明确不做。

1. 本机 docker（复用 `v2.0/ai/dify`，8090），一条命令拉起
2. `ToolSpec` 生成 `tools.openapi.json` → Dify「自定义工具」导入（9 个工具中 **8 个只读**；`draftIntervention` 不进 Dify，防止外部编排绕过人工确认）
3. 只建 **1 个 Agent 应用 = Diagnostician**（非普通工作流），inputs 仅 `teachingClassId / studentId? / question / termId?`
4. **共享 prompt**：system prompt 以文件为单一来源，本地与 Dify 同时注入（parity 公平的前提）
5. 输出强制 JSON `{answer, actions[], evidence[], refuse}`，`DifyAgentProvider` **复用自研层同一套校验**（结构 + 证据台账 + 拒答/越权），不允许"Dify 版校验更松"
6. Parity 测试：30 题双跑 → 对比表（数字一致率/方向一致率/步数/耗时/成本），数字口径由 SQL 生成
7. 演示三段：Dify 画布（设计态）→ 自研轨迹页（运行态）→ parity 表（可信度）。附 1–2 分钟录屏备份，防现场网络/容器故障
8. **Tutor 的边界**：若接入 Dify 知识库，只把 Dify 当"课程资料召回器"；召回结果回到自研层做引用校验、拒答判定、记忆写入与留痕。**知识库可替、可信性不替**
9. **不做 Dify 版的四个**：Reporter / Grader / Coach / Steward —— 记入第八节不做清单，答辩时作为"范围克制"的主动说明，而非遗漏

---

## 七、排期（约 8 周；**P0+P1 为最小可用集**）

> 与 v3.0.3 相比的唯一变化：P2 由 2 周调为 **2.5 周**（+4.75 人日），因为四个业务智能体要真的落地而不是"四角色"充数；
> P3 由 1 周压到 **0.5–1 周**（-0.75 人日），因为只做 Diagnostician 一个 Dify 版。总量 +4 人日。

| 阶段 | 时长 | 交付 | 出口 |
|---|---|---|---|
| **P0 数据底座** | 0.5–1 周 | course_factor 修复、λ 生效、熔断案例、拟真名册（含对照组构造）、3 学期、**重算流水线**、回滚快照、校验脚本、`data_story.md`、**CI 骨架** | 流水线一条命令跑通；C1/C2 重跑达标 |
| **P1 统计模块** ✅ 最小集 | 1 周 | 物化 3 表 + 失效机制 + `/api/stats` 7 接口 + 辅导员/教务看板 + 对照组对比图 + 导出 | 图表↔SQL 一致；首屏 <2s；对照差值可复现 |
| P2 Agent 平台化（框架 8.75 + 4 智能体 4.75 + 记忆/轨迹/评测 5.5 ≈ **19.0 人日**） | 2.5 周 | ①框架：`ToolSpec`(含 evidence_fields) + `ToolRegistry` + `AgentOrchestrator` + `IntentRouter` + **`EvidenceLedger`** + `Guardrails`；②**4 个核心智能体**：Diagnostician 2.0 / Reporter 1.0 / Grader 1.0 / Tutor 0.75（后三个复用 v2.0 的 REPORT / GRADING / QA 场景，补契约与校验）；③四角色 + 自适应 `auto/2/4` + Critic 最小证据集；④记忆（分级 + 可查可删）+ 轨迹页；⑤30 题集（我起草，你复核）+ 护栏 10 例 | 第一节 Agent 行全过 + A0 逐智能体验收 |
| P3 Dify 第二实现（仅 Diagnostician，**4.75 人日**） | 0.5–1 周 | OpenAPI 生成与导入（8 只读工具）、**1 个** Dify Agent 应用、共享 prompt、`DifyAgentProvider`（复用自研校验）、30 题 parity 报告、三段录屏（画布/轨迹/parity）；Tutor 的 Dify KB 召回适配（可选 0.5 人日） | parity 达标；Dify 版与自研版走同一套闸门 |
| P4 AI 亮点 | 1 周 | 干预草稿 Coach（含采纳追踪 `ai_draft_id`）、记忆可查可删、RAG 看板、幻觉看板、流式；**可选** Steward 数据质检 1.0 人日 | 四项有周趋势 |
| P5 文档答辩 | 0.5–1 周 | v3.0 说明、PPT 增页（Agent 架构/台账/统计/对照组/**双实现 parity**）、演示脚本、**证据卡** | 可复现可演 |

> 任何时间点停下：P0+P1 完成即是"数据修好 + 统计看板可用"的独立交付。

---

## 八、风险与对策

| 风险 | 对策 |
|---|---|
| 双实现分叉 | ToolSpec 单一来源 + 共享 prompt + parity 进 CI |
| 多智能体变"多次调用" | 四角色各自验收；Critic 连续两步同工具同参数判空转 |
| 四角色成本翻倍 | 自适应 `auto`（2/4 角色），评测对比质量与调用次数 |
| 台账形同虚设 | 台账未命中率进 CI 门禁；轨迹页逐个数字可点开 |
| 数据改造破坏 v2.0 演示 | 独立 `v3.0/sql` + 快照回滚 + 改造后先跑 v2.0 验收 |
| 快照与业务表不一致 | `engine_version + source_max_update_time` 失效校验 + `is_stale` 展示 |
| 记忆隐私 | 学生端可查可删；关闭类型即停止注入 |
| 改善率不可归因 | 对照组（延迟干预 40%）+ 差值指标 |
| 范围过大中途打断 | P0+P1 最小集；P2 之后为增强 |

---

## 九、工程约定

### 9.1 CI（本地一键）

`pwsh v3.0/tool/ci.ps1`：
1. 编译 → 2. 单测 → 3. 数据校验 → 4. 报告对账（不一致=0） → 5. 快照一致性断言
6. Agent 冒烟（30 题取 5 题，工具选择 ≥80%） → 7. **行为基线 diff**（关键报告文本/数字与基线比较，变化需显式确认）
8. 输出 `v3.0/tool/ci_report.md`；非零退出即不可提交；Dify 用例标 `[skip-if-offline]`

### 9.2 行为基线（防"改坏了不知道"）

- 维护 `v3.0/eval/baseline.json`：三份演示报告的关键数字、统计看板 5 个指标、Agent 评测汇总
- CI 中 diff 超阈值即失败，需人工确认后更新基线

### 9.3 证据卡（论文/答辩素材）

每阶段产出一张 `v3.0/evidence/P{n}.md`：截图要点 + 复现 SQL/命令 + 关键数字。P0 的证据卡就是"修前 vs 修后的 C1 消融对照"。

---

## 十、明确不做

| 想法 | 不做的理由 |
|---|---|
| 评测集扩到 100 题 | 30 题已覆盖类型，扩量只增加标注成本 |
| 给每个教师/班都做报告 | 演示只需 2–3 份讲得清的报告 |
| 向量化记忆 | 演示规模下摘要足够，成本收益不划算 |
| 接 LangChain4j 等框架 | 护栏/台账/留痕是核心资产，套框架要迁就抽象 |
| 教师端统计看板（v3.0） | 先做辅导员+教务，教师端 v3.1 |
| **给 Reporter/Grader/Coach/Steward 也做 Dify 版** | 维护成本翻倍、演示收益≈0；Dify 的价值用 Diagnostician 一个 parity 已证明完 |
| **「周报解读 Agent」（第七个智能体）** | 职责与 Reporter + Writer 重叠，多一个只会多一处不一致 |
| **智能体自由群聊/互相调用** | 不可评测、不可复现、成本不可控；改由路由分派 + 固定流水 + 台账仲裁 |
| **让 Agent 直接改数据/发通知/定成绩** | 越权风险；一律只出草稿 + 人确认（`ai_draft_id` 可追溯） |
| **Dify 版放宽校验（以免跑不通）** | 一旦放宽，parity 就失去意义；跑不通就如实记失败并给结论 |

---

## 十一、下一步（P0 动手顺序）

1. `v3.0/sql/` + `snapshot_v2.0.sql`（回滚）
2. `course_factor` 初始化 + 引擎 NULL 兜底
3. λ 随样本量生效 + 熔断案例
4. 拟真数据生成器（含对照组 60/40）
5. 重算流水线（一条命令）
6. 数据校验脚本 + `data_story.md`
7. 重跑 C1/C2 → 出「修前 vs 修后」对照（P0 证据卡）
8. `ci.ps1` 骨架（编译/单测/数据校验/报告对账）
