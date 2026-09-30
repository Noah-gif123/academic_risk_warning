# v2 数据库设计 · 索引与口径速查

> 本目录是 v2 重构的完整建表脚本。全部脚本已在 **MySQL 8.0** 上实跑通过
> （临时库 `study_warning_system_v2`，共 **49 张表**，无报错）。
> 依据：`9月13讨论第四轮.docx` + `最终回答.docx` + `v2.0.0改造清单.md`

## 一、执行顺序（严格按序）

| 顺序 | 文件 | 内容 | 张数 |
|---|---|---|---|
| 01 | `01_base_layer.sql` | 基础层 | 12 |
| 02 | `02_org_and_offering.sql` | 组织与开课（含成绩构成） | 7 |
| 03 | `03_enrollment_and_session.sql` | 选课与课次 | 2 |
| 04 | `04_attendance.sql` | 考勤双轨 | 3 |
| 05 | `05_score.sql` | 成绩 | 4 |
| 06 | `06_engine_outputs.sql` | 引擎产物 | 7 |
| 07 | `07_alert_and_intervention.sql` | 预警与干预闭环 | 6 |
| 08 | `08_workflow_and_audit.sql` | 教务流程与审计 | 8 |
| 09 | `09_seed_data.sql` | 预置参考数据（学期/周历/专业/行政班/导员/构成模板/引擎配置/运营配置） | — |
| 10 | `10_migrate_from_v1.sql` | 旧库迁移（学生/教师/教务/课程/知识点/干预记录） | — |
| | | **合计建表** | **49** |

```bash
mysql --host=127.0.0.1 -uroot -p study_warning_system_v2 < 01_base_layer.sql
# …依次到 08
mysql --host=127.0.0.1 -uroot -p study_warning_system_v2 < 09_seed_data.sql
mysql --host=127.0.0.1 -uroot -p study_warning_system_v2 < 10_migrate_from_v1.sql
```

**迁移实测结果**（本次已跑通）：学生 800（`admin_class_id` 与 `enroll_year` 100% 匹配）、教师 9、教务 1、课程 2、知识点 32（8 章 + 24 知识点，章节与描述全部回填）、干预记录 21（`stage=TEACHER`、动作与成效已按映射转换）、学期 4、周历 64 周、构成模板 4 套、引擎配置 1 版、运营配置 11 项。

## 二、49 张表清单（按层）

### 基础层（12）
| 表 | 作用 |
|---|---|
| `term` | 学期＝全库时间轴 |
| `term_calendar` | 教学周历（第几周/节假日/考试周） |
| `sys_account` | 四端统一账号（学生/教师/导员/教务） |
| `major` | 专业/学院 |
| `administrative_class` | 行政班 |
| `student` / `teacher` / `counselor` / `admin` | 四类主体档案 |
| `counselor_class` | 导员—行政班（带学期，换导员留痕） |
| `course` | 课程（科目：类型/学分/先修课） |
| `course_knowledge_point` | 知识点 |

### 组织与开课（7）
| 表 | 作用 |
|---|---|
| `course_group` | 课程组（课程×学期，无组长；L1/L2/L3 与教务审批） |
| `course_offering` | 开课（计划课次 / 实际课次申报与确认 / `effective_sessions` / 及格线 / 摸底考） |
| `teaching_class` | 教学班（1 位老师，含重修班标记） |
| `teaching_class_admin_class` | 教学班 ↔ 行政班 |
| `score_template` | 成绩构成模板（考试/考查/实验/项目课） |
| `score_scheme` | 成绩构成（版本、层级快照、平级全体确认、锁定） |
| `score_scheme_item` | 评分项（两级；**CHECK 禁止考勤类项**） |

### 选课与课次（2）
`enrollment`（学生×教学班×学期；退课/重修状态）、`course_session`（一次课一行；点名状态）

### 考勤（3）
`attendance_record`（明细，双轨相位）、`attendance_archive`（归档批次）、`attendance_record_log`（行级留痕）

### 成绩（4）
`student_admission_score`（高考分/位次百分位）、`score_item`（分项得分）、`homework_item`（提交事实）、`score_result`（最终成绩单）

### 引擎产物（7）
`risk_engine_config`（版本化参数）、`risk_state`（当前风险状态，替代每日快照）、`teacher_style`（Δ）、`student_fail_history`、`teaching_class_fail_rate`、`admin_class_fail_rate`、`course_fail_rate_history`

### 预警与干预（6）
`alert_record`、`alert_flow_log`（生命周期时间线）、`intervention_record`、`notification`、`notification_receipt`、`class_alert_notification`

### 教务流程与审计（8）
`coverage_check_log`、`exam_headcount`、`import_batch`、`audit_log`、`system_config`、`questionnaire`、`questionnaire_answer`、`questionnaire_flow`

## 三、核心口径速查（代码实现照此）

| 项 | 口径 |
|---|---|
| 允许缺课上限 | `N_max = floor(effective_sessions / 3)`（12 课次 → 4） |
| 挂科硬线 | `已缺课 ≥ N_max + 1`（分子只算缺课） |
| 剩余可缺 | `N_max − 已缺`；**=0 → 再缺一次即挂**；**≤−1 → 必然挂科** |
| 扣分 | `P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)`；**无上限、不截断**，`T = W − P` 可为负 |
| 请假免扣 | 每学期每门课 3 次 |
| 及格 | `T ≥ pass_score` 且 `已缺课 ≤ N_max` |
| 期末需考 | `x = (pass_score + P − W_其余项) / r_f`；**x > 100 → 实际不可达**（`W_其余项` 不含期末分） |
| 边际换算 | 每多缺一次课 → 期末需多考 `2 / r_f` 分 |
| 点名覆盖率 | `已点名课次 / 应完成课次`（应完成＝已过日期且未取消且未作废）；< 1/2 → 提醒 + 置信度判低 |
| 置信度 W | 锚点强度 × 数据完备度 × 样本充足度；≥0.7 完整分档 / 0.4–0.7 带标注 / 0.15–0.4 仅行为 / <0.15 数据不足；**覆盖率达标且锚点为 A/A′ 时不得低于 0.4** |
| 风险档位 | 红 ≥75 ｜ 橙 60–75 ｜ 黄 40–60 ｜ 绿 <40 |
| 导员纳入范围 | 风险分 ≥ 40（绿不出现） |
| 班集体提醒 | 第 1–4 周不触发；≥30% 且 ≥5 人 → 班会；≥50% 且 ≥10 人 → 重点关注 + 抄送教务 |
| 多科目预警 | ≥3 门 |
| 干预无效判定 | 下次 `risk_state` 刷新 或 14 天，先到为准；期末终判 |
| 引擎版本 | 规则/权重/公式任一变化即升版 |

## 四、设计约定

1. **无外键约束**（与旧库一致），关联靠索引 + 应用层保证，便于分批导入。
2. **业务表统一带 `term_id`**；唯一例外是 `student_admission_score`（学生级，用 `enroll_year`）。
3. **逻辑作废**用 `is_void` / `status`，不做物理删除；退课、误录、重算一律可追溯。
4. **审计字段**：业务表带 `create_by/create_time/update_by/update_time`；基础层只带时间。
5. 统一 `utf8mb4 / utf8mb4_0900_ai_ci`、`InnoDB`、`ROW_FORMAT=DYNAMIC`。
6. **每日快照已删除**："当前风险"由 `risk_state` 承载，"变化过程"由 `alert_flow_log` 追溯。
7. **能由明细实时算出的汇总一律不落库**（扣分明细、班级汇总等）。

## 五、相对旧库（38 张）的变化

**新增/重建（关键）**：`term`、`term_calendar`、`sys_account`、`counselor`、`administrative_class`、`course_group`、`course_offering`、`teaching_class(+关联)`、`enrollment`、`course_session`、`attendance_record(+archive+log)`、`score_scheme(+item+template)`、`score_item`、`homework_item`、`score_result`、`student_admission_score`、`student_fail_history`、`risk_engine_config`、`risk_state`、`teacher_style`、三级挂科率、`alert_flow_log`、`notification_receipt`、`class_alert_notification`、`coverage_check_log`、`exam_headcount`、`import_batch`、`audit_log`、`system_config`、`questionnaire(+answer+flow)`

**新库不再出现**：`class_performance`、`attendance`、`alert_snapshot`、`alert_rule_config`、`student_course`、`teacher_class`、`study_duration`、`history_risk`、`teacher_management_log`、`alert_operation_log`、`monitor_report`、`analysis_report`、`strategy_record`、`student_profile`、`student_goal`、`study_plan`、`student_memory`、`exercise_draft`、`sub_question_answer`、`exercise_recommendation`、`exercise_sub_kp`
（旧库整体归档为 `legacy_` 前缀或独立 archive 库，数据不销毁）

**字段级删除**：三表 `password`（→`sys_account`）、`student.class_name`/`grade`、`course.usual/mid/final_ratio`/`total_class_times`、`teacher_style.style_score`、`enrollment.status.PENDING`

## 六、数据库阶段已完成，接下来的工作

**已完成（本目录 10 个脚本，全部实跑通过）**：49 张表 DDL + 参考数据预置 + 旧库主体迁移。

**下一步（属于 Java 侧，不属于本目录）**：

1. **账号创建任务**：`sys_account` 需要 BCrypt 哈希，纯 SQL 无法生成 ⇒ 由 Java 迁移任务按
   `student_no / teacher_no / admin_no / counselor_no` 建账号（角色分别为 STUDENT/TEACHER/ADMIN/COUNSELOR），
   并回填 `student.account_id / teacher.account_id / admin.account_id / counselor.account_id`，全程写 `audit_log`。
2. **实体与 Mapper**：按 49 张表生成 MyBatis-Plus 实体与 Mapper（旧实体整体弃用，不复用）。
3. **合成数据生成器**：720 学生 / 6 课 / 每生 6 门 / 32 课次，**注入 Δ 真值**（如张老师 −8、王老师 +2）用于算法验证与论文实验表。
4. **口径落地校验**：把"1/3 门槛、扣分、覆盖率、期末需考、置信度分档"写成单元测试，与 `README` 第三节的口径表逐条对齐。
3. **合成数据生成器**（另立脚本）：720 学生 / 6 课 / 每生 6 门 / 32 课次，注入 Δ 真值用于算法验证。
