# M16 判定口径改造 · 交接说明（v2 修订：事实已核对 + 口径可直接实现）

> 原稿的口径决定 **一条不改**：过程分只取非终结性分支分项、按已录项归一、覆盖率进置信度、期末只作"需考"目标、考试课 ×1.10 / 考查课 ×0.90。
> 本稿改的是**可开工性**：① 原稿三处"已核实"是错的（§1）；② 补上决定算法写法的"权重 + 数据可得性"表（§2）；③ 把公式写成可实现形式，含 `r_final` 定义与一个既存 bug（§3）；④ 补齐漏掉的改动点（§4）；⑤ 加验收线与反例诊断（§5）、回滚清理 SQL（§7）。

## 0. 目标与红线

- 口径：过程分 = 非终结性分支的加权平均（0–100）；期末/大作业只作为"还需考多少/还需得多少"的目标，**不再混进已得分**。
- 红线：`score_item` 里 term 5 的期末手工分不得再出现；`risk_state` 一人一课一行覆盖刷新；判定可重跑、可回滚。

## 1. 当前库状态（2026-10-30 实库核对，非推测）

| 项目 | 实测 |
|---|---|
| term 5 `score_item` | FINAL 手工分已删；剩 405 行：PERF 90@101、90@103；MID 90@102、135@105 |
| `score_result` | 未重算：101 均分 79.26、102 76.70、103 78.37、104 81.13、105 79.63（含期末旧值） |
| `risk_state` | 504 绿 / 20 黄 / 16 红，540 条选课，engine_version 全是旧值 |
| `alert_record` | 33 条 ACTIVE（13 红 + 20 黄）+ 3 条 CLOSED 红；`notification` 65 条 |
| `risk_engine_config` | v2.0.0（id=1，生效，阈值 75/60/40）；v2.1.0（id=2，未生效，阈值 70/55/35） |
| 9 个系数列 | 存在，但两行都是 DDL 默认值（1.10/0.90/5.00/1.05/1.20/1.05/1.10/1.05/0.95） |
| 前后端 | 后端 8080 当前**无监听**；前端 5173 在跑 |

### 三处必须纠正（原稿写错的）

1. **v2.1.0 那条行不是 `36_engine_v2_1_config.sql` 建的。** 库里 id=2 是 2026-10-23 19:06 的旧演示行：阈值 **70/55/35**、remark 写着"示例版本：期末阶段收紧三档阈值（演示用，未启用）"；而 36 号脚本的 INSERT 带 `NOT EXISTS(engine_version='v2.1.0')` 守卫 → **在现库重跑 36 是空操作**，只有空库才会按脚本生成 75/60/40。
   后果：直接激活 id=2 激活的是一条阈值为 70/55/35 的演示行，dev 库与全新库跑的**不是同一套口径**，审计上 remark 还自相矛盾。处理见 §6 第 2 步。
2. **"201 班"不存在**，是 `teaching_class.id = 201` = `CS-01-A`（大学计算机基础（一），offering 101，EXAM），只覆盖 101 的 45/90 人（另有 202）。写成"201 班"下个会话会去找班号 201。
3. **数据结构 104 不是"没数据"**：`score_item` 确实 0 行，但它的终结点（大作业 ASSIGN，60%）在 AUTO 侧有项目任务 + 45 份提交（均分 83）。写成"唯一考查课、要重点对比"没错，但不能按空课处理（见 §2）。

## 2. 决定算法写法的关键事实

scheme 是**两级**：根项（如 `USUAL 平时成绩`）带子项，子项权重是**父项内部**的百分比 → 绝对权重 = 父.weight × 子.weight / 100。

| offering | 课程（course_type） | 非终结分支（绝对权重 %） | 终结分支 r_final | score_item 手工已录 | AUTO 侧有数 | 覆盖率（只算手工） | 覆盖率（含 AUTO） |
|---|---|---|---|---|---|---|---|
| 101 | 大学计算机基础（EXAM） | QUIZ 14 + P3 8 + PERF 2 + HW 16 | 60% | PERF 90 行 | QUIZ 7 卷/630 份、HW 4 卷/357、P3 90 份 | 2/40 = **5%** | 40/40 = 100% |
| 102 | 高等数学（EXAM） | QUIZ 12 + MID 8 + HW 20 | 60% | MID 90 行 | QUIZ 6 卷/540、HW 5 卷/450 | 8/40 = **20%** | 100% |
| 103 | 四史概述（EXAM） | QUIZ 21 + P3 12 + P4 24 + PERF 3 | 40% | PERF 90 行 | QUIZ 7 卷/630、P3 18、P4 18 | 3/60 = **5%** | 100% |
| 104 | 数据结构（ASSESS） | QUIZ 14 + P3 10 + HW 16 | 60%（大作业） | **0 行** | QUIZ 7 卷/945、HW 4 卷/540、P3 135、ASSIGN 45 份 | 0/40 = **0%** | 100% |
| 105 | 计算机网络（EXAM） | QUIZ 15 + MID 10 + HW 25 | 50% | MID 135 行 | QUIZ 6 卷/806、HW 5 卷/675 | 10/50 = **20%** | 100% |

**结论（最重要的一条）**：`QUIZ / HW / PROJECT / 大作业` 全是 `data_source = AUTO`，由试卷提交与项目提交聚合（逻辑在 `ScoreMatrixService.autoScores`，含缺考记 0、团队贡献占比、`aggregate_mode`）。若 `processScore` 只读 `score_item`，过程分只覆盖 **2%–10% 的权重**，算出来的数是"用一项课堂表现代表整学期"。
按现库实测，5 门课的非终结分支覆盖率是 **5%/20%/5%/0%/20%**（只读 score_item）vs **100%/100%/100%/100%/100%**（接 AUTO）。

## 3. 口径（写成可直接实现的公式）

### 3.1 绝对权重与折算

```
absW(item)   = 根项 weight                                  （item 是根）
             = 父.weight × item.weight / 100                （item 是子项）
加权点值     = Σ score_i / full_i × 100 × absW_i / 100        （i 取值见下）
```

**不要**把扁平权重直接相加（101 的 `40+35+20+5+40+60+100` = 200）。

### 3.2 过程分与覆盖率

```
域 D = 非终结分支中"有数据来源"的分项
       （MANUAL：该课该班有 score_item 行；AUTO：该 scheme_item 下有 PUBLISHED/CLOSED 的试卷或项目任务）
过程分 processScore = Σ_{i∈D} (score_i / full_i × 100 × absW_i) / Σ_{i∈D} absW_i     // 0–100 加权平均
过程分覆盖率 c_proc = Σ_{i∈D} absW_i / Σ_{i∈非终结分支} absW_i                        // 按课程算，不按人算
```

单个学生缺某次提交按 **0 分**计（沿用 `autoScores` 语义），不降低 `c_proc`。**必须**复用 `ScoreMatrixService` 的折算（建议把列矩阵 + `autoScores` 抽成共享 `ScoreCalculator`），不要在引擎里写第二套——否则成绩单页与预警页会出现两套数。

### 3.3 过程分外推 + 期末需考（含既存 bug 修正）

```
W_est = c_proc ≥ 0.30 ? 过程分 × (100 − r_final×100) / 100 : null   // 已得总评分（百分制点值）
x（需考）= (pass + P − W_est) / r_final，下限 0                       // P = 考勤扣分，沿用 RiskMath.deductionPoints
不可达   = x > finalFull，finalFull = risk_engine_config.final_full_score（现库 100）
r_final  = 终结性**根**分支 weight / 100      // 101/102/104 = 0.60，103 = 0.40，105 = 0.50
```

⚠️ **既存 bug（原稿让"复用"，会把 bug 带进新口径）**：`RiskEngineService.finalNeed()` 第 502 行取的是 `category='FINAL' AND parent_id IS NOT NULL`，命中的是**子项** `FINAL_EXAM / ASSIGN`，其 `weight = 100` → 现在算出来的 `ratio = 1.0`。于是原稿第 18 行的 `过程分 × (1 − r_final)` = `×(1 − 1.0)` = **0，过程分被整段丢掉**。`r_final` 必须取根分支（`FINAL_ROOT`）的 weight，子项 weight 只用于该子项内部折算。

**终结分支已经录了分怎么办（本轮补的规则，必须照做）**：104 的大作业（终结 60%）在 term 5 已经批完了，这时再说"还需得 47 分"是错的，要按**缺口**算：

```
terminalActual = RiskMath.terminalScore(...)        // 终结分支的加权平均，无数据为 null
IF terminalActual == null  → 需考 x 上路（期末未考：101/102/103/105）
IF terminalActual != null  → gap = max(0, x − terminalActual)
                             基础分 = clamp(0,100, 100 × gap / 期末满分)
                             final_exam_needed 记 gap，文案"还差多少分"
```
两类分支都要再叠 §3.4 的 A 通道硬门槛与"再缺即挂"（取最大）。`data_quality_json.terminalScored` 区分这两种语义。


### 3.4 风险分与等级（解决原稿"保留阈值"与"规则分档"的矛盾）

```
基础分 = clamp(0, 100, 100 × (x − pass) / (期末满分 − pass))    // 需考 60→0、70→25、80→50、90→75、100→100
叠加（取最大，不连乘）：
  absent > allowed（硬线）      → 95   // A 通道，维持现状
  x > 期末满分（不可达）        → 100
  remaining ≤ 1（再缺 1 次即挂） → ≥ 55
  remaining = 2                 → ≥ 35
最后乘 C 通道乘子（§3.6），clamp 0–100 → 用 70/55/35 三档分级（阈值仍是唯一来源）
```

这样"不可达→红 / 需考临界→橙 / 缺口大→黄 / 达标→绿"全部由阈值落地，不再需要两套分档。**待拍板**：`pass` 用 60（课程 `pass_score`），映射斜率是否用 `100×(x−pass)/(full−pass)` 这一条直线，需要业务确认一次（见 §6 第 7 步）。

### 3.5 置信度与输出形态

```
confidence = 点名覆盖率 × 数据完备度 × 样本充足度 × c_proc   （clamp 0–1）
```

- `confidence_min_ok`（0.4）只允许抬"点名覆盖率不足"那一路：`点名覆盖率 < coverage_threshold` 时 `confidence = max(confidence, min_ok)`；**其他情况不得 floor**，否则 5% 权重的过程分会被抬成"看起来可信"。
- 闸门：`c_proc < 0.30` → `output_mode = BEHAVIOR_ONLY`、`final_exam_needed = null`（页面显示"—（分项未录齐）"），但 `risk_level` 仍按 A 通道硬线给（缺课超限该红就红）。
- `data_quality_json` 里补 `"processScore":…,"processCoverage":…,"finalRatio":…` —— 这是回归和排查的唯一依据（`risk_state` 没有这些列，见 §4）。

### 3.6 C 通道四因子（原稿最含糊的一段，这里给到取数）

| 因子 | 取数 | 规则 | 现状 |
|---|---|---|---|
| 课程类型 | `course.course_type`（EXAM/ASSESS） | EXAM 1.10 / ASSESS 0.90；方案无终结分支或 `r_final = 0` → 记数据异常、退回 1.00 | 可用（101/102/103/105 EXAM，104 ASSESS） |
| 该科历史挂科率 | **只能用** `course_fail_rate_history`（course_id, term_id, fail_rate） | `factor = clamp(fail_rate_step ^ ((fail_rate − fail_rate_base)/5), 0.90, fail_rate_cap)`（= 每高 5 个百分点乘 1.05，最高 1.20）；无历史 → 1.00 并标 `failRateHistory=absent` | **无历史**：表里只有 term 5 的 5 行、`is_initialized=0`、`course_factor=NULL` |
| 教师风格 | `teacher_style.delta`（term 5 已有 6 行、`sample_size=45`、`λ=0.620`） | `delta ≤ −1.0` → 1.05（严格）；`delta ≥ +0.5` → 0.95（宽松）；其间 1.00；`abs(delta) > delta_fuse_threshold(20)` → 1.00 + 告警 | 有数（−1.61 ~ +0.19）。注意 seed 的 λ=0.620 与 `delta_shrink_k=60`（45/105=0.429）对不上 |
| 个人前科 | `score_result` 中 `term_id < 5` 且 `term.status='CLOSED'` 的挂科门数 | 1 门 → `personal_prior_1`、≥2 门 → `personal_prior_2`；无历史 → 1.00；样本 < 30 按 `λ = n/(n+30)` 收缩 0.5 | **必须排除 term 8**（2027-2028 合成学期，库内备注"合成数据·勿混用"，且在 term 5 之后）。排除后只有 45/135 人有历史（term 4），其余 90 人 → 1.00 |

**禁用**：`teaching_class_fail_rate` / `admin_class_fail_rate` 当"历史挂科率"——它们是本期数据、且是 `1-10.v1` 旧口径在 2026-10-20（删期末分之前）算出来的，用它等于用旧口径的旧结论喂新口径。
乘子一律乘在**风险分**上（不是需考分），并把实际取值写进 `risk_state.course_factor_used` / `delta_used`（列已存在，现在恒为 null）。

### 3.7 EXAM / ASSESS 不分支线（已实现成这条）

统一用 §3.3 的同一个公式：差别只在于**终结分支有没有录分**。
- EXAM（101/102/103/105）：期末未考 → 输出"需考 x"。
- ASSESS（104）：大作业已批 → 输出"还差 gap 分"（本轮实测 gap 多为 0，只有 1 人因缺课硬线判红）。

原稿"ASSESS 走过程分达标线"会把 60% 的大作业直接丢掉，等于只拿 40% 的成绩做判定，不做这个分支。


## 4. 代码改动点

| # | 位置 | 现状 | 改成 |
|---|---|---|---|
| 1 | `RiskEngineService` 409/414 `resultsOf()` | ~~读 `score_result.weighted_score`~~ | ✅ **已完成**：改为 `ScoreCalculator.compute(tc)` 现算（见 §4.5），`resultsOf` 已删 |
| 2 | 192 `channelBScore(T, absent)` | ~~用总评映射风险分~~ | ✅ **已完成**：§3.4 绝对口径（`guardedRiskScore`），B 通道只剩排序（`channel_b_level=SORT_ONLY`） |
| 3 | 208 `riskLevel()` | 阈值分档 | ✅ 保留阈值，输入改为校准后的风险分（C 通道乘子乘完再分级） |
| 4 | 495 `finalNeed()` | ~~按子项权重反推、`eq(groupId, offeringId)` 取方案~~ | ✅ **已完成**：`r_final` 取根分支、`W_est` 由 `ScoreCalculator` 给、终结分支已录分走"缺口"语义、`c_proc<0.30` 返回 null |
| 5 | 533 `rankOf()` | 参与等级 | ✅ 已删，改名次由"风险分从高到低"统一给；页面文案待 §4 第 12 项改 |
| 6 | 新增 | — | ✅ C 通道乘子按 §3.6 取数，`course_factor_used` / `delta_used` 已落库（实测 v2.0.0 下全为 1.000） |
| 7 | 其他 | — | ✅ `course_type` 与方案一致性：无终结分支 / `r_final=0` 时不输出需考，只留 A 通道 |
| 8 | ~~**`entity/RiskEngineConfig`**~~ | ~~缺 9 个新字段~~ | ✅ **已完成**：补了 `courseFactorExam/Assess`、`failRateBase/Step/Cap`、`personalPrior1/2`、`teacherStrict/LooseFactor`；`RiskRuleRequest` 同步 |
| 9 | ~~**`service/RuleConfigService`**~~ | ~~`copyOf()` 逐字段复制、`validate()` 不校验新系数~~ | ✅ **已完成**：`copyOf` 复制 9 个系数、`create` 支持传入、`validate` 校验（乘子 0.5~1.5、`fail_rate_step/cap ≥ 1`、基准 ≥ 0）、`brief` 回显（版本对比可审计） |
| 10 | `risk_state` 落库 | 没有过程分列 | **先写 `data_quality_json`**（`processScore` / `processCoverage` / `finalRatio` / `missingWeight`），不加列 → 免掉"代码先上、建列脚本没跑"导致 MP 查不存在的列。要 SQL 直查时用 `json_extract(data_quality_json,'$.processCoverage')`；确实需要列再补 `sql/37` |

| 11 | **`ScoreMatrixService.commitSummary()`** | 有缺项就抛"还有评分项没有录入" | 现存接口**无法重算** `score_result`。二选一：(a) 新增"按已录项重算"（缺项按未录、按已录权重归一）；(b) 明确接受 `score_result` 停在旧口径，并在成绩单页标注。选 (a) 才配得上"话术改过程分" |
| 12 | 前端 | Monitor.vue 37 行口径说明 + 96/244 列；Warnings.vue 65/69/138/142；Scores.vue 14 行公式；Rules.vue（无新系数表单） | 一并改；Rules.vue 要能看能改 9 个系数 |
| 13 | 测试 | 已有 `RiskMathTest`（原 7 个用例）+ `QuestionGenServiceGateTest`；**不是零测试** | 已扩到 16 个用例（M16 过程分/r_final/绝对风险分/叠加规则）；改引擎时继续往这里加 |

## 4.5 已完成的底座（2026-10-30 本轮）

- **新增 `service/ScoreCalculator`**：列矩阵 + MANUAL 聚合 + **AUTO 聚合** + 两级折算 + 过程分/覆盖率/终结点占比，作为成绩单页与风险引擎的**唯一**折算入口；`ScoreMatrixService.buildMatrix` 已改为委托它，成绩单页数字口径不变（只搬代码，未改算法）。
  - 单次 `compute(teachingClass)` 返回：`columns / enrollments / students / weights(绝对权重+terminal+available) / finalRatio / byEnrollment{ cells, missingPaths, weightedScore, processScore, processCoverage, missingWeight }`。
  - 方案查找改成 **offering → courseGroup(courseId, termId) → scheme**（见下面这条 bug）。
- **`RiskMath` 新增纯函数**：`SchemeWeight` / `ProcessScore` / `processScore()` / `finalRatio()` / `estimatedEarned()` / `absoluteRiskScore()` / `guardedRiskScore()` / `clamp100()`；旧函数一字未动。
- **测试**：`RiskMathTest` 扩到 16 个用例，全部通过（本轮用 javac + JUnit Platform launcher 实跑验证，`gradlew test` 在沙箱里因写 `%USERPROFILE%\.gradle` 被拒）。
- **新发现的 bug（要修，属 §4 第 4 项）**：`RiskEngineService.finalNeed()` 用 `eq(ScoreScheme::getGroupId, offeringId)` 找方案——term 5 的 `course_group.id` 恰好等于 offering id（都是 101~105）所以没暴露；term 8 的 group 是 5~10、offering 是 4~9，会**静默取到隔壁课的成绩构成**。必须改成 `ScoreCalculator` 的 offering → courseGroup → scheme 路径。

### 本轮第二批（配置层）也已完成

- `RiskEngineConfig` / `RiskRuleRequest` / `RuleConfigService` 打通 9 个系数：引擎能读到、`/api/admin/rules` 能读能写、`copyOf` 不再丢值、`validate` 有区间校验、`brief` 在版本历史里回显。
- `admin/Rules.vue`：当前生效卡新增"M16 口径系数"区（考试课/考查课、历史挂科率、教师风格、个人前科 + C 通道开关状态），"新增版本"表单补 9 个输入（留空沿用当前版本）。已用 `vue/compiler-sfc` 编译校验通过（`vite build` 在本沙箱内会因 esbuild spawn 被拒，改用 SFC 编译代替）。
- **还没有动的**：`RiskEngineService` 本身（§4 第 1–7 项）、话术页（Monitor/Warnings/Scores，§4 第 12 项）。

### 本轮第三批（引擎改造）已完成并实跑验证

`RiskEngineService` 已按 §3 改造完成：不再读 `score_result`，改由 `ScoreCalculator` 现算；`finalNeed` 换根分支 + `W_est`；风险分走 §3.4 绝对口径 + 叠加；C 通道四因子按 §3.6 取数（缺历史一律 1.00）；名次改成"风险分从高到低"的排序字段（`channel_b_level=SORT_ONLY`）；过程分/覆盖率/四个因子全部写进 `data_quality_json`；`RiskRowVO` 增加 `processScore` / `processCoverage`。

**实跑验证（本机后端 + 真库，cfg 仍 v2.0.0 所以 C 通道乘子全为 1.00）**：

| 试点班 | 结果 |
|---|---|
| 201（CS-01-A，101 大学计算机基础，EXAM，45 条） | 39 绿 / 5 黄 / 1 红（红=缺课 6>4 硬线），需考 35–70（均值 46.6），`processCoverage=1.0`（AUTO 已接上）、`finalRatio=0.6`、`terminalScored=false`，等级变化 3 条 |
| 207（CS-01-D，104 数据结构，ASSESS，45 条） | 44 绿 / 1 红（硬线），大作业已录 → `terminalScored=true`、gap 多为 0，`finalRatio=0.6`，等级变化 2 条 |

交叉校验：教学班 201 成绩矩阵里学生 1 的 `weightedScore=26.17`，正好等于 `processScore(65.43) × 0.4`——成绩单页与预警页现在是同一套数。
副作用可控：预警 36→38 条、通知 65→68 条（两班共 5 条等级变化）。

**顺带修掉的两个新坑**：
1. `risk_engine_config` 的列名是 `personal_prior_1` / `personal_prior_2`（带下划线），实体必须是 `@TableField("personal_prior_1")`，否则 MyBatis-Plus 生成的 `personal_prior1` 直接 SQL 报错（已实跑撞到）。
2. 沙箱内没有 `gradlew`（要写 `%USERPROFILE%\.gradle`）时的复现方式：`javac -parameters` 必须加（否则 Spring MVC 报 "parameter name information not available"），Lombok 要从 gradle 缓存补到 `-classpath/-processorpath`，运行时依赖直接从旧 `bootJar` 的 `BOOT-INF/lib` 解出来（67 个 jar），再 `java -cp build-verify;src/main/resources;<libs> <主类>`。




## 5. 验收标准（先定线，再跑）

**反例诊断表**（跑完对不上就说明实现有问题，不要当结论接受）：

| 现象 | 结论 |
|---|---|
| 103 需考 ≈143、105 ≈104，两门全员不可达 | `processScore` 没算 AUTO 分项，只读了 `score_item`（现库已按这条路径模拟过，结论就是这组数） |
| 104 的 135 人需考全空 | 大作业 AUTO 没接（它有 45 份提交） |
| 需考全员恒为 0 | `r_final` 仍取成子项 100% → `(1−r_final)=0`，或 `W_est` 用了旧 `score_result` |
| 红黄数量 0 | 风险分映射把需考全压在 60 以下，检查 `pass` 与斜率 |
| `data_quality_json.processCoverage` 是 0.05 / 0.20 | AUTO 未接（正确值应 ≈ 1.00，五个 offering 都是 100%） |

**正向预期**（按现有 AUTO 分项均分、`P = 0` 估算）：101 ≈43、102 ≈45、103 ≈26、104 ≈44、105 ≈36 → 红黄应是个位数、随考勤与过程分变化，既不全是绿也不该是全红。

**数量级闸门**：与旧结果（504/20/16、33 条 ACTIVE）同量级；**红 > 50%（270 人）一律视为实现或数据未就绪**，回滚排查，不得当结论收下。

**试点**：先跑 `teaching_class` 201（CS-01-A）+ 207（CS-01-D）两班 90 条，抽 20 条手工核算（过程分 → c_proc → r_final → 需考 → 风险分 → 等级），通过再全量。

## 6. 收尾步骤（按序，含前置）

0. **前置**：本机 8080 无监听 → `./gradlew bootJar` 后起后端；前端 5173 已在跑（话术改完即时可见）；准备一个教务 token。
1. `./gradlew build`（本工程**只有 gradle**，没有 mvn）+ 补跑 `RiskMathTest`。
2. **配置对齐**（原稿第 2 步的前提）：
   ```sql
   delete from risk_engine_config where engine_version = 'v2.1.0' and remark like '示例版本%';
   ```
   再执行 `36_engine_v2_1_config.sql`（不先删就是空操作），核对生成行的阈值 **75/60/40** 与 9 个系数；之后才
   `update risk_engine_config set is_active=1 where engine_version='v2.1.0';`
   若业务决定沿用 70/55/35，就改脚本再跑，别让"脚本"与"库"长期不一致。
3. 试点两班（先抑制通知，见第 4 条）。
4. 全量：`POST /api/engine/evaluate`（教务 token）。首次全量会 `alertEventService.sync` 生成预警并给教师/学生写通知（当前 33 条 ACTIVE / 65 条通知，全量后可能数百条）→ 要么临时关通知，要么在演示前留出清理时间。
5. 回归：按 offering / teaching_class 分组统计红黄绿、预警条数、需考分布、`processCoverage`；**必须按 `engine_version` 过滤**（历史 `risk_state` 行仍带旧版本号，混着统计会失真）。
6. 话术 / 文档 / 打包（§8）。
7. **待业务拍板**：§3.4 的风险分映射与 §3.6 的教师风格判别阈值，定稿前确认一次。

## 7. 回滚（原稿"不会留脏数据"不成立）

计算层可回滚，但**事件层不会自动复原**：`alert_record` / `alert_flow_log` / `notification` 只增不减（只 close），`sync` 还有"人工关闭冷却期"会挡住重建。先备份，再：

```sql
-- 清掉 v2.1.0 产生的事件与状态（先备份 alert_record/alert_flow_log/notification/risk_state）
DELETE n FROM notification n JOIN alert_record a ON a.id = n.biz_id
 WHERE n.biz_type LIKE 'ALERT%' AND a.engine_version = 'v2.1.0';
DELETE f FROM alert_flow_log f JOIN alert_record a ON a.id = f.alert_id
 WHERE a.engine_version = 'v2.1.0';
DELETE FROM alert_record  WHERE engine_version = 'v2.1.0';
DELETE FROM risk_state    WHERE engine_version = 'v2.1.0' AND is_void = 0;
UPDATE risk_engine_config SET is_active = 0 WHERE engine_version = 'v2.1.0';
UPDATE risk_engine_config SET is_active = 1 WHERE engine_version = 'v2.0.0';
```

然后重跑一次 `POST /api/engine/evaluate` 回到旧结果。

- **期末分"恢复"要当心**：`v2.0/tool/20_rebuild_demo.sql`（一键重建）**仍含那 405 条 term5 期末手工分**（707×90、713×90、720×90、732×135），这次只改了 `seed_demo.js`。谁点一键重建谁复发 → 重建后**必须**再跑 `35_fix_term5_scores.sql` 并重跑判定。根治：重新生成该脚本并提交（§8 第 1 条）。
- **代码回滚**：`git stash` / `git revert <commit>` + `./gradlew bootJar`；数据库侧只有 `risk_state` / `alert_*` 受影响（见上）。

## 8. 文档与脚本清单

| 对象 | 要改什么 |
|---|---|
| `v2.0/tool/20_rebuild_demo.sql` | **重新生成**，去掉 405 条 term5 期末分（否则交付包带着 bug） |
| `v2.0/sql/README.md` | 登记 35/36（新增加列脚本则 37）；136 行"期末需考"公式与本文对齐 |
| `v2.0/backend/README.md` | 204/205/212 行的 B 通道口径、`/api/engine/evaluate` 说明 |
| `v2.0/第二阶段功能演示脚本.md` | "期末需考 67.9 / 61.9 / 70.3"等示例数按新口径重算 |
| `v2.0/frontend/src/pages/...` | `teacher/Monitor.vue`（37、96、244）、`student/Warnings.vue`（65/69/138/142）、`teacher/Scores.vue`（14）、`admin/Rules.vue`（9 个系数） |
| AI 手册 | `FactPackBuilder` / `AlertExplainService` 的字段与话术（过程分 + 需考） |
| 打包 | `git archive`；推 gitee/origin（GitHub 直连不稳，用本地 7890 代理） |

## 10. 本轮收尾状态（2026-10-30 晚，已上线到本机演示库）
- **配置已对齐并激活**：删掉库里那条演示版 v2.1.0 → 重跑 `36_engine_v2_1_config.sql` → 生成 v2.1.0（阈值 **75/60/40**、9 个系数、`channel_c_enabled=1`）→ 通过 `POST /api/admin/rules/8/activate` 切换（留了审计）。
- **全量 540 条已按 v2.1.0 重跑**（4.6 s，等级变化 47 条）：**16 红 / 0 橙 / 65 黄 / 459 绿**；
  `processCoverage` 全部 = 1.0（AUTO 已接上）、无人"需考不可达"、无 `BEHAVIOR_ONLY`。
  分课程：101 → 3 红 13 黄；102 → 2 红 10 黄；103 → 3 红 16 黄；104 → 4 红 7 黄（全为缺课硬线，大作业缺口多为 0）；105 → 4 红 19 黄。
- **通知开关**：新增 `app.alert.notify-enabled`（环境变量 `ALERT_NOTIFY_ENABLED=false`），本轮全量重跑期间关闭，
  `notification` 表 68 条**一条没增**；预警记录照常生成（38→69 条，ACTIVE 56）。
- **前端口径话术已改**：`teacher/Monitor.vue`（口径说明 + "需考 / 还差"列）、`student/Warnings.vue`（主数字改"过程分"，
  取自 `risk_state.data_quality_json`）、`admin/Rules.vue`（9 个系数可看可改）。
- **文档已同步**：`v2.0/sql/README.md`（35/36 登记 + 过程分/需考/绝对风险分/门槛地板四条口径）、`v2.0/backend/README.md`（风险判定与学生端口径）。
- **观察点（不是 bug，是标定问题）**：本套数据里过程分普遍 82–85 分，需考只在 35–73 之间，按 `需考 60→0` 的直线映射，
  分数型风险最高只到 ~36（绿）；因此**橙档为空**，红=缺课硬线、黄="剩余可扣 1–2 次"的门槛地板。
  若要让"需考高"进入橙/红，需要业务调两个旋钮之一：把 `absoluteRiskScore` 的起点从 `pass` 前移到 `pass − 15` 左右，
  或把"剩余 2 次"的地板从 40 调低（当前 43 人因它进黄）。
- **剩余待办**：① `score_result` 仍无法按已录项重算（`commitSummary` 有缺项即抛），成绩单页与预警页暂时靠"过程分"错开；
  ② `v2.0/tool/20_rebuild_demo.sql` 仍含 405 条 term 5 期末分，一键重建会复发（重建后必须再跑 35）；
  ③ `第二阶段功能演示脚本.md` 里的示例数字（期末需考 67.9 等）仍是旧口径；④ 打包 `git archive` + 推 gitee/origin 未做。

## 9. 开工顺序（把风险前置，本轮已全部走完，留档备查）

1. **数据体检**：跑一遍 §2 那张表（换环境/换学期都要重跑），确认"只读 score_item"与"含 AUTO"的覆盖率差距；
2. ~~抽 `ScoreCalculator` + `RiskMath` 纯函数 + `RiskMathTest`~~ → **已完成，见 §4.5**；
3. ~~配置实体 / 规则页 / 校验 / 版本复制~~ → **已完成，见 §4.6**；
4. ~~改引擎~~ → **已完成并实跑（201/207 两班 + 全量 540），见 §4.7 / §10**；
5. ~~配置对齐、全量重跑、话术页、文档~~ → **已完成，见 §10**；
6. 剩下：`20_rebuild_demo.sql` 重生成、`score_result` 按已录项重算、演示脚本示例数、打包推送（§10 剩余待办）。

