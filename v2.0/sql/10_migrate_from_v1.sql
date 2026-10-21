-- =============================================================================
-- 学情预警系统 v2 重构 · 第 10 部分：旧库 → 新库 迁移
-- -----------------------------------------------------------------------------
-- 前提：
--   ① 旧库 `study_warning_system` 原样保留（本脚本只读它，不修改）
--   ② 新库 `study_warning_system_v2` 已执行 01–09（基础层 + 参考数据）
--   ③ 执行顺序：01 → … → 08 → 09（预置数据）→ **10（本脚本）**
-- 迁移策略（已确认"重构、旧数据不删光"）：
--   · **保留主键**：INSERT 时显式带上旧 id ⇒ 学生/教师/课程的 id 不变，
--     后续任何引用（含旧 intervention_record.student_id）都仍然对得上
--   · **只迁"主体与内容资产"**：学生、教师、教务、课程、知识点、干预记录
--   · **不迁业务层**：选课、成绩、作业、考勤、预警、通知、学习计划、画像等
--     （口径已整体更换：旧阈值红=40、新阈值红=75，混在一起必然出现假跳变）
--   · **账号另建**：sys_account 需 BCrypt 哈希，纯 SQL 无法生成 ⇒
--     由 Java 迁移任务创建账号并回填 student.account_id / teacher.account_id / admin.account_id
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 1. 学生（800 行）→ student
--    字段映射：
--      id            → id（保留）
--      student_no    → student_no
--      student_name  → student_name
--      class_name    → admin_class_id（按 4 个取值映射到 4 个行政班）
--      grade（"大一上"等，实为学期）→ enroll_year（由班级年级推断）
--      password      → 丢弃（改由 sys_account 存 BCrypt）
--      account_id    → NULL（待 Java 任务回填）
-- =============================================================================
INSERT INTO `student` (`id`,`account_id`,`student_no`,`student_name`,`enroll_year`,`major_id`,`admin_class_id`,`status`,`email`)
SELECT s.`id`, NULL, s.`student_no`, s.`student_name`,
       CASE
         WHEN s.`class_name` LIKE '%（大一）%' THEN 2025
         WHEN s.`class_name` LIKE '%（大二）%' THEN 2024
         ELSE NULL
       END AS enroll_year,
       1 AS major_id,
       CASE s.`class_name`
         WHEN '计算机1班（大一）' THEN 1
         WHEN '计算机2班（大一）' THEN 2
         WHEN '计算机1班（大二）' THEN 3
         WHEN '计算机2班（大二）' THEN 4
         ELSE NULL
       END AS admin_class_id,
       'IN_SCHOOL' AS status,
       s.`email`
FROM `study_warning_system`.`student` s
ON DUPLICATE KEY UPDATE
  `student_name`=VALUES(`student_name`), `enroll_year`=VALUES(`enroll_year`),
  `major_id`=VALUES(`major_id`), `admin_class_id`=VALUES(`admin_class_id`);

-- =============================================================================
-- 2. 教师（9 行）→ teacher
-- =============================================================================
INSERT INTO `teacher` (`id`,`account_id`,`teacher_no`,`teacher_name`,`phone`,`email`,`status`)
SELECT t.`id`, NULL, t.`teacher_no`, t.`teacher_name`, t.`phone`, t.`email`, 'ACTIVE'
FROM `study_warning_system`.`teacher` t
ON DUPLICATE KEY UPDATE
  `teacher_name`=VALUES(`teacher_name`), `phone`=VALUES(`phone`), `email`=VALUES(`email`);

-- =============================================================================
-- 3. 教务/管理员（1 行）→ admin
-- =============================================================================
INSERT INTO `admin` (`id`,`account_id`,`admin_no`,`admin_name`,`admin_type`,`status`)
SELECT a.`id`, NULL, a.`admin_no`, a.`admin_name`, 'ACADEMIC', 'ACTIVE'
FROM `study_warning_system`.`admin` a
ON DUPLICATE KEY UPDATE `admin_name`=VALUES(`admin_name`);

-- =============================================================================
-- 4. 课程（2 行）→ course
--    字段映射：usual/mid/final_ratio → 丢弃（改由 score_scheme_item 表达）
--              total_class_times    → 丢弃（改由 course_offering 表达）
--              total_homework / total_knowledge → 丢弃（可由数据数出来）
--    course_type：旧库两门课期中占比为 0（50/0/50）⇒ 判定为考查课 ASSESS
-- =============================================================================
INSERT INTO `course` (`id`,`course_code`,`course_name`,`course_type`,`prerequisite_course_id`,`college_name`,`status`)
SELECT c.`id`,
       CONCAT('C', LPAD(c.`id`,3,'0')),
       c.`course_name`,
       CASE WHEN c.`mid_ratio` = 0 THEN 'ASSESS' ELSE 'EXAM' END,
       c.`prerequisite_course_id`,
       '计算机学院',
       'ACTIVE'
FROM `study_warning_system`.`course` c
ON DUPLICATE KEY UPDATE
  `course_name`=VALUES(`course_name`), `course_type`=VALUES(`course_type`),
  `prerequisite_course_id`=VALUES(`prerequisite_course_id`);

-- =============================================================================
-- 5. 知识点（32 行）→ course_knowledge_point
--    name → kp_name ｜ sort_order → sort_no ｜ description → description（富文本保留）
--    parent_id 保留（旧库已是两级：父=章节，子=知识点）
--    kp_level：parent_id IS NULL → 1（章）；否则 3（知识点）
-- =============================================================================
INSERT INTO `course_knowledge_point`
 (`id`,`course_id`,`kp_code`,`kp_name`,`parent_id`,`chapter`,`kp_level`,`sort_no`,`description`,`status`)
SELECT k.`id`, k.`course_id`,
       CONCAT('KP', LPAD(k.`id`,4,'0')),
       k.`name`,
       k.`parent_id`,
       (SELECT p.`name` FROM `study_warning_system`.`course_knowledge_point` p WHERE p.`id` = k.`parent_id`) AS chapter,
       CASE WHEN k.`parent_id` IS NULL THEN 1 ELSE 3 END,
       k.`sort_order`,
       k.`description`,
       'ACTIVE'
FROM `study_warning_system`.`course_knowledge_point` k
ON DUPLICATE KEY UPDATE
  `kp_name`=VALUES(`kp_name`), `parent_id`=VALUES(`parent_id`),
  `chapter`=VALUES(`chapter`), `sort_no`=VALUES(`sort_no`), `description`=VALUES(`description`);

-- =============================================================================
-- 6. 干预记录（旧 21 行）→ intervention_record
--    映射（见 v2.0.0改造清单 §2.2 的旧行迁移映射）：
--      alert_id          → 置 NULL，旧值写入 remark（新库不迁旧预警，禁止假外键）
--                          同时写入 related_alert_ids 作 legacy 标记
--      teacher_id        → operator_id + operator_role='TEACHER'
--      intervention_type → action_type
--      status            → effective
--      description       → content ｜ result_note → result
--      risk_score_before/after/change、create_time、effect_check_time → 原样保留
--      stage             → 'TEACHER'（旧系统只有教师处理）
-- =============================================================================
INSERT INTO `intervention_record`
 (`id`,`student_id`,`term_id`,`alert_id`,`related_alert_ids`,`stage`,`operator_id`,`operator_role`,
  `action_type`,`content`,`result`,`effective`,`effect_check_time`,
  `risk_score_before`,`risk_score_after`,`risk_score_change`,`remark`,`create_time`)
SELECT i.`id`, i.`student_id`, NULL, NULL,
       JSON_ARRAY(CONCAT('legacy_alert:', i.`alert_id`)),
       'TEACHER', i.`teacher_id`, 'TEACHER',
       CASE i.`intervention_type`
         WHEN 'TALK' THEN 'TALK'
         WHEN 'TUTOR' THEN 'SUPPLEMENT'
         WHEN 'PARENT' THEN 'PARENT'
         WHEN 'SUPPLEMENT' THEN 'SUPPLEMENT'
         WHEN 'PLAN' THEN 'PLAN'
         ELSE 'OTHER'
       END,
       i.`description`, i.`result_note`,
       CASE i.`status`
         WHEN 'INEFFECTIVE' THEN 'INEFFECTIVE'
         WHEN 'COMPLETED' THEN 'EFFECTIVE'
         ELSE 'PENDING'
       END,
       i.`effect_check_time`,
       i.`risk_score_before`, i.`risk_score_after`, i.`risk_score_change`,
       CONCAT('旧库预警ID=', i.`alert_id`),
       i.`create_time`
FROM `study_warning_system`.`intervention_record` i
ON DUPLICATE KEY UPDATE
  `action_type`=VALUES(`action_type`), `content`=VALUES(`content`),
  `result`=VALUES(`result`), `effective`=VALUES(`effective`);

-- =============================================================================
-- 校验：迁移结果
-- =============================================================================
SELECT 'student' AS tbl, COUNT(*) AS rows_ FROM student
UNION ALL SELECT 'student.admin_class 已匹配', COUNT(*) FROM student WHERE admin_class_id IS NOT NULL
UNION ALL SELECT 'student.enroll_year 已匹配', COUNT(*) FROM student WHERE enroll_year IS NOT NULL
UNION ALL SELECT 'teacher', COUNT(*) FROM teacher
UNION ALL SELECT 'admin', COUNT(*) FROM admin
UNION ALL SELECT 'course', COUNT(*) FROM course
UNION ALL SELECT 'course_knowledge_point', COUNT(*) FROM course_knowledge_point
UNION ALL SELECT 'intervention_record', COUNT(*) FROM intervention_record;

-- =============================================================================
-- 附：本脚本"故意不迁移"的清单（口径已更换，迁进来只会污染新库）
--   student_course(534)      → 选课改由 enrollment（需教学班/学期，旧数据无法对应）
--   score_info(534)          → 成绩改由 score_scheme_item + score_item + score_result
--   homework_info(534)       → 作业改由 homework_item + score_item
--   class_performance(534)   → 考勤改由 course_session + attendance_record 明细
--   attendance(0)            → 空汇总表，废弃
--   alert_record(164)/alert_snapshot(604) → 旧阈值 40 vs 新阈值 75，不可比
--   notification(1158)       → 结构重建（主体 + 回执）
--   monitor_report/analysis_report/strategy_record/student_profile/student_goal/
--   study_plan/student_memory/history_risk/study_duration → 旧口径结论或 AI 产物
--   teacher_class(9)         → ID 区间切人，改由 teaching_class + 关联表
--   exercise* 过程表          → 练习模块延后（仅保留 exercise + 关联为内容资产，暂不迁）
--
-- 账号创建（Java 迁移任务，建议与主体迁移同一事务批次）：
--   1) 学生：login_no=student_no，role=STUDENT，password_hash=BCrypt(初始口令)
--   2) 教师：login_no=teacher_no，role=TEACHER
--   3) 教务：login_no=admin_no，role=ADMIN
--   4) 导员：login_no=counselor_no，role=COUNSELOR（09 已预置 2 位）
--   5) 回填 student.account_id / teacher.account_id / admin.account_id / counselor.account_id
--   6) 全程写 audit_log（action=IMPORT, biz_type=ACCOUNT）
-- =============================================================================
