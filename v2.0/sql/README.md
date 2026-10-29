# v2.0 数据库 · 索引与口径速查（2026-09 口径冻结版）

> 本目录是 v2.0 的完整建表脚本，共 **15 个脚本、60 张表**，全部已在 **MySQL 8.0** 上按序实跑通过
> （校验库 `sws_v2_check`，无报错）。
> 依据：四轮需求确认文档（整体分析展望 v2.0.0 / 最终确认 / 补充 / 教师端补充）+ 两轮问答（回答001 等）
> 与《功能整理（口径冻结版）》。
>
> **本版关键变更（相对 48 张表的基线）**：
> ① 知识点相关全部删除（`course_knowledge_point` 表、`score_scheme_item.kp_id`、`questionnaire_answer.kp_id/kp_flag`）；
> ② 课程编码彻底删除（`course.course_code`）；
> ③ 作业改为题目型在线作答，`homework_item` 作废；
> ④ 覆盖率下限由 1/2 改为 **1/3**；
> ⑤ 新增教学资源与教学活动层 **14 张表**。
> 结果：48 → 46（删 2 张）→ **60 张**（+14）。

## 一、执行顺序（严格按序）

| 顺序 | 文件 | 内容 | 张数 |
|---|---|---|---|
| 01 | `01_base_layer.sql` | 基础层 | 12（其中知识点表已在 15 号脚本删除，实际保留 11） |
| 02 | `02_org_and_offering.sql` | 组织与开课（含成绩构成） | 7 |
| 03 | `03_enrollment_and_session.sql` | 选课与课次 | 2 |
| 04 | `04_attendance.sql` | 考勤双轨 | 3 |
| 05 | `05_score.sql` | 成绩（`homework_item` 已在 15 号脚本删除，实际保留 2） | 3 |
| 06 | `06_engine_outputs.sql` | 引擎产物 | 7 |
| 07 | `07_alert_and_intervention.sql` | 预警与干预闭环 | 6 |
| 08 | `08_workflow_and_audit.sql` | 教务流程与审计（含问卷） | 8 |
| 09 | `09_seed_data.sql` | 预置参考数据（学期 / 周历 / 专业 / 行政班 / 导员 / 构成模板 / 引擎配置 / 运营配置） | — |
| 10 | `10_migrate_from_v1.sql` | 旧库迁移（学生 / 教师 / 教务 / 课程 / 干预记录） | — |
| **11** | **`11_teaching_content.sql`** | **教学资源与课程内容（导学 / 教学目标 / 资源库 / 目录课时 / 课时资源）** | **5** |
| **12** | **`12_assessment.sql`** | **题目型教学活动（试卷 / 题目 / 提交 / 逐题作答）** | **4** |
| **13** | **`13_project.sql`** | **项目型活动（任务 / 组队 / 提交 / 贡献表）** | **4** |
| **14** | **`14_access.sql`** | **入课与邀请码（教学班扩列 + 入课申请）** | **1** |
| **15** | **`15_alter_2026_09.sql`** | **2026-09 口径修订（删知识点 / 删课程编码 / 作业改造 / 覆盖率 1/3）** | — |
| | | **合计建表** | **60** |

### 演示数据脚本（按需执行，不参与建表）

| 文件 | 作用 | 说明 |
|---|---|---|
| `16_auth_token.sql` | 令牌落库 | `sys_account` 增加 `token` / `token_expire_time` + `uk_token` |
| `17_dict_align.sql` | 数据字典口径对齐 | 只改列注释（`enrollment.status`、`password_hash`） |
| `18_notification_seed.sql` | 四端消息演示数据 | **已被 `20_rebuild_demo.sql` 覆盖，勿与 20 一起执行**（会重复） |
| **`19_cleanup_demo.sql`** | **清理脏数据** | 删测试卷、空壳学期；修枚举值；清内部标记；`teacher_style` 中的合成真值 |
| **`20_rebuild_demo.sql`** | **重建主线演示数据**（由 `tool/seed_demo.js` 生成） | 3 班 × 45 人、5 门课（计基 12 周 / 高数 16 周 / 四史概述 12 周 / 数据结构(Java) 16 周 / 计网 12 周）、12 教学班、540 选课、164 课次、5535 考勤、试卷 126（124 份随堂测验/作业 + 2 张进行中）、提交 5580、项目 12 个（团队贡献表 81 份）、手工分项 810、540 成绩单、问卷四环节；term 4 历史 + term 8 合成样本。**课堂测试/作业来自试卷、三级/四级项目与大作业来自项目模块（都按 scheme_item_id 自动汇总）**；风险状态与预警不在此脚本内，重建后由 `POST /api/engine/evaluate` 真实生成 |
| **`21_tidy_after_rebuild.sql`** | 重建后收尾 | 清孤儿教学班/选课，合成学期改由 6~9 号教师带班 |
| `22_tidy_after_m3.sql` / `23_tidy_after_m4.sql` / `24_tidy_after_m5.sql` | 第二阶段各步验收后的演示库整理 | 教师端验收残留 / 申诉文案 / 规则阈值测试产生的预警与规则版本 |
| **`25_alert_reply.sql`** | 申诉闭环 | `alert_record` 增加 `teacher_reply` / `teacher_reply_time`（幂等，可重复执行） |
| `29_seed_intervention_demo.sql` | M10 干预回访演示数据 | 幂等补 10 条干预记录（6 有效 / 2 无效 / 2 待回访），让覆盖率、有效率、响应时长、复发率有数可算（详见第十章） |
| `32_seed_courseware.sql` | M11 课件演示数据 | 给教学班 201 挂 PPTX + DOCX 两份课件，演示"课件入库 + 页码引用"（需先把文件放进 `file-store/201/`，详见第十三章） |

> **一键重建**：`powershell -NoProfile -File ../tool/rebuild_demo.ps1`（内含 19→20→21→25、判定、演示痕迹与导学发布）。

> 重建顺序：`19 → 20 → 21` → 起后端跑一次判定（`tool/run_engine_after_seed.ps1`）→ 需要演示痕迹时跑 `tool/stage2_demo_data.ps1`（认领处理 / 提醒 / 申诉，走真实接口）。

```bash
# 建表（01→15）
mysql -uroot -p study_warning_system_v2 < 01_base_layer.sql
# … 依次 02 → 14，再 15
mysql -uroot -p study_warning_system_v2 < 15_alter_2026_09.sql

# 演示数据（一条主线 + 历史 + 合成样本）；20 由 tool/seed_demo.js 生成在 tool/ 目录下
mysql -uroot -p study_warning_system_v2 < 19_cleanup_demo.sql
mysql -uroot -p study_warning_system_v2 < ../tool/20_rebuild_demo.sql
mysql -uroot -p study_warning_system_v2 < 21_tidy_after_rebuild.sql
# 然后起后端并跑一次判定（写入 risk_state / alert_record），需要演示痕迹再跑 stage2_demo_data.ps1
```

> 基线（01–10）保持原样、修订集中在 15 号脚本，便于审计“哪些是后来改的”。
> 从零重建后最终结构与第 15 号脚本的说明一致；15 号脚本末尾自带四段自检 SQL，执行后可直接核对。

## 二、60 张表清单（按 12 层）

### 基础层（11）
`term` 学期 ｜ `term_calendar` 教学周历 ｜ `sys_account` 统一账号 ｜ `major` 专业 ｜ `administrative_class` 行政班 ｜ `student` 学生 ｜ `teacher` 教师 ｜ `counselor` 辅导员 ｜ `admin` 教务管理员 ｜ `counselor_class` 导员—行政班 ｜ `course` 课程（**已无 course_code**）

### 组织与开课（7）
`course_group` 课程组 ｜ `course_offering` 开课实例 ｜ `teaching_class` 教学班（**新增 invite_code / join_review_enabled**）｜ `teaching_class_admin_class` ｜ `score_template` ｜ `score_scheme` ｜ `score_scheme_item` 评分项（**已无 kp_id**）

### 选课与课次（2）
`enrollment` 选课 ｜ `course_session` 课次

### 考勤（3）
`attendance_record` 考勤明细（双轨相位）｜ `attendance_archive` 归档批次 ｜ `attendance_record_log` 行级留痕

### 成绩（2）
`score_item` 分项得分 ｜ `score_result` 最终成绩单

### 引擎产物（7）
`risk_engine_config` 规则版本（**覆盖率阈值 = 0.333**）｜ `risk_state` 当前风险状态 ｜ `teacher_style` 教师尺度 Δ ｜ `student_fail_history` ｜ `teaching_class_fail_rate` ｜ `admin_class_fail_rate` ｜ `course_fail_rate_history`

### 预警与干预（6）
`alert_record` ｜ `alert_flow_log` 生命周期 ｜ `intervention_record` 三级干预 ｜ `notification` ｜ `notification_receipt` ｜ `class_alert_notification`

### 教务流程与审计（8）
`coverage_check_log` ｜ `exam_headcount` ｜ `import_batch` ｜ `audit_log` ｜ `system_config` ｜ `questionnaire`（**只留 TEACHER_EVAL**）｜ `questionnaire_answer`（**已无 kp 字段**）｜ `questionnaire_flow`

### 教学资源与课程内容（5，新增 · 11 号）
| 表 | 作用 | 关键约束 |
|---|---|---|
| `course_guide` | 课程导学（每位教师一份，HTML 富文本，version 递增） | `uk_teaching_class` 一教学班一条 |
| `teaching_objective` | 教学目标（编号 / 内容 / 类型，教师自编编号） | `idx_tc_sort` 按顺序展示；**无知识点列** |
| `resource_file` | 资源库文件元信息（本地或对象存储） | `is_void` 逻辑作废，不做物理删除 |
| `course_lesson` | 目录课时（**一行 = 一次课，必绑 course_session**） | `uk_tc_session` 一教学班一次课一条 |
| `lesson_resource` | 课时资源与任务（课前 / 课中 / 课后三段） | `res_type` 指向文件 / 外链 / 试卷 / 项目 |

### 题目型教学活动（4，新增 · 12 号）
| 表 | 作用 | 关键点 |
|---|---|---|
| `paper` | 试卷：QUIZ 随堂测验 / HOMEWORK 课后作业 / EXAM 线上考试 | 三个可见性开关：`allow_view_paper`、`show_score`、`show_answer`（NEVER / AFTER_MARK）；`scheme_item_id` 决定计入哪个评分项 |
| `question` | 题目：单选 / 多选 / 判断 / 填空 / 简答 | 选项与答案存 JSON；客观题自动判分、简答教师批阅 |
| `submission` | 提交记录（一试卷一学生一条） | `uk_paper_student`；缺考 `is_absent=1` 且记 0 分；分数拆客观 / 主观 |
| `submission_answer` | 逐题作答与判分 | `uk_sub_q`；**每题记录判卷人**（SYSTEM / TEACHER / AI） |

### 项目型活动（4，新增 · 13 号）
| 表 | 作用 | 关键点 |
|---|---|---|
| `project_task` | 项目任务（是否团队、允许的提交形式、满分、计入哪个评分项） | 逾期不允许补交 |
| `project_team` | 组队（组长唯一） | `uk_project_leader` |
| `project_submission` | 提交与批阅（文件 + 仓库链接 + 项目总分） | 团队一份、个人一份 |
| `project_contribution` | **组长提交的贡献表，提交后不可修改** | `uk_project_team` 一组一份；CHECK `total_ratio = 1.0000` |

### 入课（1，新增 · 14 号）
`course_join_request` 入课申请：教师生成邀请码 → 学生申请 → 教师同意 → **写一条教务知会记录**（教务不审批）；`uk_tc_student` 防止重复申请。

## 三、核心口径速查（代码实现照此）

| 项 | 口径 |
|---|---|
| 允许缺课上限 | `N_max = floor(effective_sessions / 3)`（12 课次 → 4）｜**仅引擎内部与教师 / 教务端可见** |
| 挂科硬线 | `已缺课 ≥ N_max + 1`（分子只算缺课） |
| 剩余可缺 | `N_max − 已缺`；**=0 → “再缺一次即挂”**；**≤−1 → 必然挂科**。★学生端只显示状态、不显示数值 |
| 扣分 | `P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)`；**无上限、不截断**，`T = W − P` 可为负 |
| 请假免扣 | 每学期每门课 3 次 |
| 及格 | `T ≥ pass_score` 且 `已缺课 ≤ N_max` |
| 期末需考 | `x = (pass_score + P − W_其余项) / r_f`；x > 100 → 实际不可达 |
| **点名覆盖率** | 已点名课次 / 应完成课次（应完成＝已过日期且未取消且未作废）；**低于 1/3 → 提醒补录 + 教务督办 + 置信度判低** |
| 置信度 W | 锚点强度 × 数据完备度 × 样本充足度；≥0.7 完整分档 / 0.4–0.7 带标注 / 0.15–0.4 仅行为 / <0.15 数据不足 |
| 风险档位 | 红 ≥75 ｜ 橙 60–75 ｜ 黄 40–60 ｜ 绿 <40；取三族规则最高者 |
| 导员纳入范围 | 风险分 ≥ 40（不出现绿色） |
| 多科目预警 | ≥3 门 |
| 班集体提醒 | 第 1–4 周不触发；≥30% 且 ≥5 人 → 班会；≥50% 且 ≥10 人 → 重点关注 + 抄送教务 |
| 干预无效判定 | 下次 `risk_state` 刷新 或 14 天，先到为准；期末终判 |
| 教学活动的分 | 活动得分 ÷ 活动满分 × 该评分项权重 → 自动录入对应评分项（批改完成后） |
| 贡献度 | 组长一份、提交后不可改、教师也不可改；个人得分 = 项目总分 × 个人贡献占比 |
| 答案与解析可见性 | `paper.show_answer`：NEVER 永远不可见 / AFTER_MARK 批阅后可见 |
| 缺考与逾期 | 未交 / 缺考记 0 分；作业与项目**都不允许迟交、不允许补交**；教师不代交 |
| 引擎版本 | 规则 / 权重 / 公式任一变化即升版；结果记录 `engine_version`，跨版本不做趋势连线 |

## 四、设计约定

1. **无外键约束**：关联靠索引 + 应用层保证，便于分批导入与旧库迁移。
2. **业务表统一带 `term_id`**；新增的教学内容与活动表同时带 `teaching_class_id`（每位教师各自一份）。
3. **逻辑作废**用 `is_void` / `status`，不做物理删除；退课、误录、重算一律可追溯。
4. **审计字段**：业务表带 `create_by/create_time/update_by/update_time`；基础层只带时间。
5. 统一 `utf8mb4 / utf8mb4_0900_ai_ci`、`InnoDB`、`ROW_FORMAT=DYNAMIC`。
6. **能由明细实时算出的汇总一律不落库**（扣分、班级汇总等）。
7. **每日快照已删除**：当前风险由 `risk_state` 承载，变化过程由 `alert_flow_log` 追溯。
8. 关键口径用约束兜底：`project_contribution.total_ratio` CHECK、`submission.uk_paper_student`、`course_lesson.uk_tc_session`、`teaching_class.uk_invite`。

## 五、本次新增层的三个设计取舍

1. **课时必绑课次**（`course_lesson.session_id` 必填）：这样“课中测验/作业”能自动挂到当次课，与考勤点名的分母对齐；教师想“一次课拆两次”应由教务调整实际课次，而不是在目录里硬拆。
2. **测验 / 作业 / 考试共用一套试卷引擎**（`paper.paper_type` 区分）：三者只是入口与归属不同（课中 / 课后 / 独立模块），共用题目、作答、判分与“判卷人”记录，避免三套几乎一样的实现。
3. **贡献表用 CHECK + 唯一键双保险**：合计必须 1.0000、一个组只有一份、`is_locked` 默认 1。教师不可修改，如需更正只能走教务特批并写 `audit_log`。

## 六、下一步（属于 Java 侧）

1. **实体与 Mapper**：按 60 张表生成 MyBatis-Plus 实体与 Mapper（旧实体整体弃用，不复用）。
2. **账号创建任务**：`sys_account` 需要 BCrypt 哈希，由 Java 迁移任务按学号 / 工号建账号并回填 `account_id`，全程写 `audit_log`。
3. **合成数据生成器**：720 学生 / 6 门课 / 每生 6 门 / 32 课次，注入教师尺度真值，用于联调与算法验证。
4. **口径落地校验**：把“1/3 门槛、扣分、覆盖率、期末需考、置信度分档、贡献度折算”写成单元测试，与本文第三节逐条对齐。

## 七、第三阶段（AI）新增表：`26_ai_stage3.sql`

`26_ai_stage3.sql` 幂等新增 7 张表，**不改动任何既有业务表**（这也是"不引入知识点"的直接结果）：

| 表 | 用途 |
| --- | --- |
| `ai_run` | 一次 AI 任务（场景 / 对象 / 来源 DIFY·LLM·TEMPLATE / token / 费用 / 错误） |
| `ai_run_step` | 步级明细与校验结论（数字回验 PASS/WARN/FAIL） |
| `ai_usage` | 计费与限额统计 |
| `ai_report` | 学习评价报告（`fact_json` 事实快照 + `content_json` 段落与动作） |
| `ai_report_feedback` | 学生"有用/没用"反馈 |
| `ai_prompt_template` | 提示词与工作流版本登记（配 `v2.0/ai/dify-workflows/*.yml`） |
| `ai_question_draft` | AI 出题草稿 + 审核状态（M2 使用，已建表待用） |

设计取舍：
1. **报告存事实快照**（`fact_json`）：报告发布后即使成绩改了，也能复算"当时为什么这么说"，答辩与申诉都用得上；
2. **AI 产物与业务表隔离**：草稿不进 `question`（`paper_id` 非空），报告不进 `score_result`，AI 挂了业务不受影响；
3. **成本落库**：每次调用写 `ai_usage`，报表页可直接展示"本月花了多少、有多少次走了模板兜底"。
## 八、M8 新增表：`27_ai_group_report.sql`

| 表 | 用途 |
| --- | --- |
| `ai_group_report` | 群体学情报告（`scope=CLASS` 教学班 / `scope=ADMIN_CLASS` 行政班），与个人报告 `ai_report` 分表，避免个人表塞空的学生字段 |

字段要点：`target_id`（教学班或行政班 ID）+ `fact_json`（事实快照，可复算）+ `content_json`（段落与动作）+ `source`（DIFY/LLM/TEMPLATE）+ `tokens/cost`（成本可见）。
## 九、M9 新增表：`28_ai_alert_explain.sql`

| 表 | 用途 |
| --- | --- |
| `ai_alert_explain` | 预警解释卡（一条预警可反复生成，页面取最新）：`alert_id` + `trigger_type`（ABSENCE/HOMEWORK/PROJECT/SCORE/MULTI/DATA）+ `fact_json` 事实快照 + `content_json`（四段正文与三条例行动作）+ `source`（DIFY/LLM/TEMPLATE）+ `tokens/cost` |

设计取舍：**触发类型与三条例行动作由规则库给出，模型只负责表述**——这样"怎么补"永远可执行、可追溯，也避免模型给出不存在的资源链接。
## 十、M10 新增：`29_seed_intervention_demo.sql`（演示数据）

M10 的口径（覆盖率 / 有效率 / 响应时长 / 复发率）依赖 `intervention_record` 的回访结论，而演示库里原本只有 1 条。
该脚本幂等补 10 条：6 条 `EFFECTIVE`、2 条 `INEFFECTIVE`、2 条 `PENDING`，并把前 3 条对应的预警置为 `CLOSED` 且写 `alert_flow_log`，
从而形成"预警 → 干预 → 回访 → 关闭"的完整轨迹，指标才有数可算。

统计口径（`InterventionMetricsService`，全部 SQL 聚合）：

| 指标 | 定义 |
| --- | --- |
| 干预覆盖率 | 有干预记录的预警 ÷ 预警总数 |
| 回访有效率 | 回访结论 EFFECTIVE ÷（EFFECTIVE + INEFFECTIVE） |
| 平均响应时长 | 预警创建 → 首次干预，按小时平均 |
| 复发率 | 干预后同一门课又产生新预警的数量 ÷ 已干预预警数 |
## 十一、M11 新增表：`30_ai_kb.sql`（课程知识库）

| 表 | 用途 |
| --- | --- |
| `ai_kb_doc` | 知识库文档（来源：导学 GUIDE / 资料 RESOURCE），记录 content_hash 做幂等、chunk_count 与状态 |
| `ai_kb_chunk` | 知识库分块，**带 ngram 全文索引**（`FULLTEXT ... WITH PARSER ngram`），检索与引用的最小单位（doc_name + section_no） |

首版 RAG 的"R"用 MySQL ngram 全文检索实现（中文按 2-gram 切分，相关度由 MySQL 打分）；
分块与引用结构与向量库方案一致，后续接向量检索只替换 `KbService.retrieve()` 的内部实现。
## 十二、M11 新增表：`31_ai_qa.sql`（课程答疑记录）

| 表 | 用途 |
| --- | --- |
| `ai_qa_log` | 一次提问一行：问题、答案、引用（docName+sectionNo）、命中数、最高相关度、是否拒答、通道与成本 |

配套：`KbService`（入库 + 检索）+ `StudentQaService`（检索→生成→**引用校验**→拒答）+ `POST /api/student/ai/qa`。
拒答条件有两条：ngram 相关度低于 0.10（绝对下限），或「词面支撑度」低于 0.10（把问题切成 2 字片段后，
出现在最佳片段里的比例——该比例不受语料规模影响，用来拦住"检索到东西但和问题无关"）；
引用与检索结果对不上时，整段引用作废并退模板。
## 十三、M11 补强：`32_seed_courseware.sql`（演示课件）

| 内容 | 说明 |
| --- | --- |
| 两份演示课件 | `第2讲_Excel函数与图表.pptx`（9 页，2 页带讲稿备注）+ `实验二_成绩统计操作指导.docx`（6 章，含评分标准表格），挂在教学班 201（大学计算机基础（一）） |
| 前置 | 文件本体放 `backend/file-store/201/` 下，路径与脚本里的 `file_path` 一致（由 `v2.0/tool/courseware` 生成） |
| 执行后 | 教师端触发一次 `POST /api/teacher/ai/kb/sync?courseId=1`，两份课件进知识库（9 + 4 = 13 个分块） |

课件入库口径（`KbExtractor`）：支持 `txt/md/csv/docx/pptx`。PPTX 一页一个文本块、块首写 `【第N页】`，
分块在页边界强制断开，所以引用既能给「第N段」（分块序号）又能给出「第N页」（课件页码）；
DOCX 按原文顺序拍平段落与表格（表格行用 `|` 分隔，标题加 `## `）。
教师上传课件后**上传即入库**（不占额外操作）；资料作废后，下次 `sync` 会把对应分块自动下线（返回值里的 `purged`）。
## 十四、M14 AI 出题：复用 26_ai_stage3.sql 里的 i_question_draft

出题**没有新增表**——第三阶段建表时就留好了草稿表，M14 只是开始真正往里写：

| 字段 | M14 的用法 |
| --- | --- |
| offering_id + group_name | 按开课实例隔离草稿；大题名沿用试卷的分组名（仍然不用知识点） |
| q_type / difficulty / stem / options_json / nswer_json / nalysis | 题型、难度、题干、选项、标准答案、解析；答案结构与 question.answer_json 一致，所以能直接入卷 |
| source_pages | 引用出处：课件页码（PPTX）或分段序号（其它格式） |
| quality_json | **五道闸门的逐项结果**（checks[].code/pass/detail + passed），答辩时可逐题展示"为什么这题被拦下" |
| status | DRAFT 生成 → ADOPTED 采纳 / REJECTED 弃用；入卷由 paper/question 负责，这里只留痕 |
| un_id / 	okens / cost / model / prompt_version | 可追溯到具体一次 AI 运行与成本（prompt_version = question-gen-v1） |

口径：**AI 只出草稿，成题责任在教师**；草稿表不参与学生端任何查询，只有 /api/teacher/ai/questions/* 会读它。