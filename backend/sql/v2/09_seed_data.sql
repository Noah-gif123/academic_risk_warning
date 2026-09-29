-- =============================================================================
-- 学情预警系统 v2 重构 · 第 9 部分：预置数据（与旧库无关的参考数据）
-- -----------------------------------------------------------------------------
-- 适用：初始化一个空库（study_warning_system_v2）
-- 说明：
--   ① 本脚本只写"参考数据"：学期与周历、专业、行政班、导员、成绩构成模板、
--      引擎配置、运营配置。学生/教师/课程/知识点/干预记录由 10_migrate_from_v1.sql 从旧库迁移。
--   ② 会计账号（sys_account）**不在 SQL 里创建** —— 密码必须是 BCrypt 哈希，
--      纯 SQL 无法生成。账号由 Java 迁移任务创建并回填 student.account_id 等。
--      因此 student/teacher/admin 的 account_id 迁移后仍为空，属预期状态。
--   ③ 重复执行安全：脚本开头会清空下列参考表再重建（不触碰业务数据）。
-- =============================================================================

SET NAMES utf8mb4;

-- ---------- 0. 清理参考数据（仅参考表） ----------
DELETE FROM `system_config`;
DELETE FROM `risk_engine_config`;
DELETE FROM `score_template`;
DELETE FROM `counselor_class`;
DELETE FROM `counselor`;
DELETE FROM `administrative_class`;
DELETE FROM `major`;
DELETE FROM `term_calendar`;
DELETE FROM `term`;

-- ---------- 1. 学期（4 个；映射旧库 student.grade 的四个取值） ----------
-- 旧库把"大一上/大一下/大二上/大二下"塞在学生表里，这里还原为全局学期
INSERT INTO `term` (`id`,`term_code`,`school_year`,`term_no`,`term_name`,`start_date`,`end_date`,`total_weeks`,`is_current`,`status`) VALUES
 (1,'2024-2025-1','2024-2025',1,'2024-2025学年第一学期（对应旧库 大一上）','2024-09-02','2025-01-12',16,0,'CLOSED'),
 (2,'2024-2025-2','2024-2025',2,'2024-2025学年第二学期（对应旧库 大一下）','2025-02-24','2025-07-06',16,0,'CLOSED'),
 (3,'2025-2026-1','2025-2026',1,'2025-2026学年第一学期（对应旧库 大二上）','2025-09-01','2026-01-11',16,0,'CLOSED'),
 (4,'2025-2026-2','2025-2026',2,'2025-2026学年第二学期（对应旧库 大二下）','2026-02-23','2026-07-05',16,1,'ACTIVE');

-- ---------- 2. 教学周历（每个学期 16 周，按开学日期顺推；节假日由教务后续维护） ----------
INSERT INTO `term_calendar` (`term_id`,`week_no`,`start_date`,`end_date`,`week_type`,`holiday_days`,`note`)
WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n+1 FROM seq WHERE n < 16)
SELECT t.id, s.n,
       DATE_ADD(t.start_date, INTERVAL (s.n-1)*7 DAY),
       DATE_ADD(t.start_date, INTERVAL (s.n-1)*7 + 6 DAY),
       'NORMAL', 0, NULL
FROM `term` t JOIN seq s;

-- 标注一个示例假期（国庆：2025-2026 学年第一学期第 5 周），演示"应完成课次要扣假日"
UPDATE `term_calendar` SET `week_type`='HOLIDAY', `holiday_days`=3, `note`='国庆假期（示例）'
WHERE `term_id`=3 AND `week_no`=5;

-- ---------- 3. 专业（由旧库班级名"计算机x班"推断） ----------
INSERT INTO `major` (`id`,`major_code`,`major_name`,`college_code`,`college_name`,`subject_group`,`status`) VALUES
 (1,'CS','计算机科学与技术','CS01','计算机学院','物理','ACTIVE');

-- ---------- 4. 行政班（旧库 student.class_name 的 4 个取值 → 班级 + 年级） ----------
-- 旧库把"班级"和"学期"混在一个字符串里（计算机1班（大一）），此处拆成"班 + 年级"
INSERT INTO `administrative_class` (`id`,`class_code`,`class_name`,`major_id`,`grade_year`,`status`) VALUES
 (1,'CS-2025-1','计算机1班',1,2025,'ACTIVE'),
 (2,'CS-2025-2','计算机2班',1,2025,'ACTIVE'),
 (3,'CS-2024-1','计算机1班',1,2024,'ACTIVE'),
 (4,'CS-2024-2','计算机2班',1,2024,'ACTIVE');

-- ---------- 5. 导员（新角色主体；示例：马导管 1、3 班，刘导管 2、4 班） ----------
INSERT INTO `counselor` (`id`,`counselor_no`,`counselor_name`,`gender`,`college_name`,`phone`,`status`) VALUES
 (1,'C001','马思国',1,'计算机学院','13800001001','ACTIVE'),
 (2,'C002','刘慧',2,'计算机学院','13800001002','ACTIVE');

INSERT INTO `counselor_class` (`counselor_id`,`admin_class_id`,`term_id`,`is_primary`) VALUES
 (1,1,4,1),(1,3,4,1),
 (2,2,4,1),(2,4,4,1);

-- ---------- 6. 成绩构成模板（4 套，对应课程类型；★考勤不出现在任何模板里） ----------
INSERT INTO `score_template` (`id`,`template_code`,`template_name`,`course_type`,`level`,`items_json`,`description`,`is_default`,`status`) VALUES
 (1,'EXAM_DEFAULT','考试课默认构成','EXAM','L2',
  '[{"code":"USUAL","name":"平时","weight":40,"children":[
      {"code":"HW","name":"课后作业","weight":40,"dataSource":"AUTO","expectedTimes":9,"aggregateMode":"AVG"},
      {"code":"QUIZ","name":"课堂测试","weight":30,"dataSource":"MANUAL","expectedTimes":3,"aggregateMode":"AVG"},
      {"code":"PERF","name":"课堂表现","weight":15,"dataSource":"MANUAL","isSubjective":true},
      {"code":"EXP","name":"实验报告","weight":15,"dataSource":"MANUAL","expectedTimes":5,"aggregateMode":"AVG"}]},
    {"code":"MID","name":"期中","weight":20,"children":[
      {"code":"MID_EXAM","name":"期中考试","weight":100,"dataSource":"MANUAL","isAnchor":true,"anchorLayer":"A"}]},
    {"code":"FINAL","name":"期末","weight":40,"children":[
      {"code":"FINAL_EXAM","name":"期末闭卷","weight":100,"dataSource":"MANUAL","isAnchor":true,"anchorLayer":"A"}]}]',
  '考试课：平时40 + 期中20 + 期末40；平时含作业/测试/表现/实验',1,'ACTIVE'),
 (2,'ASSESS_DEFAULT','考查课默认构成','ASSESS','L2',
  '[{"code":"USUAL","name":"平时","weight":60,"children":[
      {"code":"HW","name":"课后作业","weight":40,"dataSource":"AUTO","expectedTimes":9,"aggregateMode":"AVG"},
      {"code":"QUIZ","name":"课堂测试","weight":30,"dataSource":"MANUAL","expectedTimes":3,"aggregateMode":"AVG"},
      {"code":"PERF","name":"课堂表现","weight":30,"dataSource":"MANUAL","isSubjective":true}]},
    {"code":"FINAL","name":"期末","weight":40,"children":[
      {"code":"FINAL_EXAM","name":"期末考核","weight":100,"dataSource":"MANUAL","isAnchor":true,"anchorLayer":"A"}]}]',
  '考查课：平时60 + 期末40（旧库两门课即此类：期中占比为 0）',1,'ACTIVE'),
 (3,'EXPERIMENT_DEFAULT','实验课默认构成','EXPERIMENT','L3',
  '[{"code":"USUAL","name":"平时","weight":30,"children":[
      {"code":"EXP_RPT","name":"实验报告","weight":60,"dataSource":"MANUAL","expectedTimes":5,"aggregateMode":"AVG"},
      {"code":"EXP_OP","name":"实验操作","weight":20,"dataSource":"MANUAL","isSubjective":true},
      {"code":"EXP_PERF","name":"实验表现","weight":20,"dataSource":"MANUAL","isSubjective":true}]},
    {"code":"EXP_EXAM","name":"实验考核","weight":40,"dataSource":"MANUAL","isAnchor":true,"anchorLayer":"A"},
    {"code":"FINAL","name":"期末","weight":30,"children":[
      {"code":"FINAL_EXAM","name":"期末理论","weight":100,"dataSource":"MANUAL","isAnchor":true,"anchorLayer":"A"}]}]',
  '实验课：平时30(报告/操作/表现) + 实验考核40 + 期末30',1,'ACTIVE'),
 (4,'PROJECT_DEFAULT','项目课默认构成','PROJECT','L3',
  '[{"code":"USUAL","name":"平时","weight":20,"children":[
      {"code":"DOC","name":"阶段文档","weight":50,"dataSource":"MANUAL","expectedTimes":3,"aggregateMode":"AVG"},
      {"code":"REPORT","name":"阶段汇报","weight":50,"dataSource":"MANUAL","isSubjective":true,"expectedTimes":3,"aggregateMode":"AVG"}]},
    {"code":"PROJECT","name":"三级项目","weight":50,"children":[
      {"code":"PROJ_DOC","name":"项目文档","weight":40,"dataSource":"MANUAL"},
      {"code":"PROJ_DEF","name":"项目答辩","weight":60,"dataSource":"MANUAL","isSubjective":true}]},
    {"code":"DEFENSE","name":"答辩","weight":30,"dataSource":"MANUAL","isSubjective":true,"isAnchor":true,"anchorLayer":"A"}]',
  '项目课：平时20 + 三级项目50 + 答辩30（纯主观项占比高，输出降级）',1,'ACTIVE');

-- ---------- 7. 引擎配置（v2.0.0 初始版本，全部为已确认口径） ----------
INSERT INTO `risk_engine_config`
 (`id`,`engine_version`,`effective_time`,`is_active`,
  `channel_a_enabled`,`channel_b_enabled`,`channel_c_enabled`,
  `signal_weights_json`,`delta_enabled`,`course_factor_enabled`,
  `threshold_red`,`threshold_orange`,`threshold_yellow`,`coverage_threshold`,`coverage_soft_weeks`,
  `counselor_week_start`,`counselor_rate_l1`,`counselor_min_l1`,`counselor_rate_l2`,`counselor_min_l2`,`counselor_risk_floor`,
  `multi_course_threshold`,`invalid_check_days`,
  `deduct_absent`,`deduct_late`,`deduct_leave`,`leave_free_times`,`final_full_score`,
  `delta_shrink_k`,`delta_min_sample`,`delta_fuse_threshold`,`confidence_min_ok`,`remark`) VALUES
 (1,'v2.0.0',NOW(),1,
  1,1,0,
  '{"freshman":{"attendance":0.40,"homework":0.30,"score":0.25,"subjective":0.05},"normal":{"attendance":0.30,"homework":0.25,"score":0.30,"subjective":0.15}}',1,1,
  75.0,60.0,40.0,0.500,4,
  5,0.300,5,0.500,10,40.0,
  3,14,
  2.0,1.0,1.0,3,100.0,
  60,30,20.0,0.400,'通道C先关闭（Δ 需积累数据后启用）；其余为四轮讨论已确认口径');

-- ---------- 8. 运营配置 ----------
INSERT INTO `system_config` (`config_key`,`config_value`,`config_type`,`config_group`,`description`,`is_editable`) VALUES
 ('archive.mid.week','9','INT','ARCHIVE','期中归档周次（建议第 9 周，需教务确认）',1),
 ('archive.final.week','17','INT','ARCHIVE','期末归档周次（建议第 17 周，需教务确认）',1),
 ('archive.window.days','7','INT','ARCHIVE','归档窗口开放天数',1),
 ('archive.allow.admin.extend','true','BOOL','ARCHIVE','是否允许教务延长归档窗口',1),
 ('notify.remind.interval.days','3','INT','NOTIFY','归档催办间隔天数',1),
 ('notify.remind.max.times','3','INT','NOTIFY','归档最多催办次数',1),
 ('coverage.soft.check.weeks','2','INT','COVERAGE','期内软提醒检查间隔周数',1),
 ('coverage.soft.zero.weeks','4','INT','COVERAGE','连续零录入多少周触发软提醒',1),
 ('questionnaire.flow.timeout.days','7','INT','QUESTIONNAIRE','问卷每个环节超时天数',1),
 ('session.generate.weekday','1','INT','ATTENDANCE','课表默认上课星期（1=周一），批量生成 course_session 用',1),
 ('risk.dashboard.refresh.cron','0 30 2 * * ?','STRING','SYSTEM','风险状态批量刷新时间',1);

-- ---------- 校验 ----------
SELECT 'term' AS t, COUNT(*) AS rows_ FROM term
UNION ALL SELECT 'term_calendar', COUNT(*) FROM term_calendar
UNION ALL SELECT 'major', COUNT(*) FROM major
UNION ALL SELECT 'administrative_class', COUNT(*) FROM administrative_class
UNION ALL SELECT 'counselor', COUNT(*) FROM counselor
UNION ALL SELECT 'counselor_class', COUNT(*) FROM counselor_class
UNION ALL SELECT 'score_template', COUNT(*) FROM score_template
UNION ALL SELECT 'risk_engine_config', COUNT(*) FROM risk_engine_config
UNION ALL SELECT 'system_config', COUNT(*) FROM system_config;
