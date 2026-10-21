-- =============================================================================
-- 学情预警系统 v2.0 · 第 15 部分：2026-09 口径修订
-- -----------------------------------------------------------------------------
-- 本脚本把“2026-09 口径冻结”确定下来的变更应用到 01–10 的基线上：
--   ① 知识点相关**全部删除**（v2.0 不做任何知识点功能）
--      - DROP course_knowledge_point
--      - score_scheme_item.kp_id
--      - questionnaire_answer.kp_id / kp_flag
--      - questionnaire.questionnaire_type 只保留 TEACHER_EVAL（去掉 KP_FEEDBACK）
--      - questionnaire.question_json / questionnaire_answer.answer_type 注释同步
--      - import_batch.import_type 去掉 KP
--   ② 课程编码**彻底删除**（course.course_code）
--   ③ 作业改为题目型在线作答（12_assessment.sql），homework_item 不再使用
--   ④ 覆盖率下限由 1/2 改为 **1/3**（提醒补录、教务督办、置信度判低三条逻辑一起改）
--
-- 执行顺序：01 → 10（基线） → 11 → 12 → 13 → 14（新增层） → 15（本脚本）
-- 说明：为便于审计，本脚本把“删除与改默认值”的操作集中在 15 号，
--       01–10 仍按基线原样保留；从零重建后最终结构与本节描述一致。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- ① 知识点：全部删除
-- -----------------------------------------------------------------------------
-- 1.1 知识点表整表删除（旧库迁移数据不再进入新库）
DROP TABLE IF EXISTS `course_knowledge_point`;

-- 1.2 评分项不再关联知识点
ALTER TABLE `score_scheme_item` DROP COLUMN `kp_id`;

-- 1.3 问卷作答不再有知识点反馈
ALTER TABLE `questionnaire_answer` DROP COLUMN `kp_flag`;
ALTER TABLE `questionnaire_answer` DROP COLUMN `kp_id`;
ALTER TABLE `questionnaire_answer`
  MODIFY COLUMN `answer_type` varchar(20) NOT NULL COMMENT '作答类型：SCORE评分/TEXT文本（本版只此两种）';

-- 1.4 问卷类型只保留“老师评价”一种（本版问卷 = 教师风格判定）
ALTER TABLE `questionnaire`
  MODIFY COLUMN `questionnaire_type` varchar(30) NOT NULL COMMENT '类型：TEACHER_EVAL老师评价（本版只此一种，用于教师风格判定）';
ALTER TABLE `questionnaire`
  MODIFY COLUMN `question_json` json NULL COMMENT '题目定义：[{"code":"Q1","type":"SCORE","text":"..."}]';

-- 1.5 导入批次不再支持知识点导入
ALTER TABLE `import_batch`
  MODIFY COLUMN `import_type` varchar(30) NOT NULL COMMENT '类型：TIMETABLE周数课次/SCORE成绩/ATTENDANCE考勤/ROSTER名单/PAPER试卷题目/OTHER（已移除 KP）';

-- -----------------------------------------------------------------------------
-- ② 课程编码：彻底删除（界面与数据库都不保留）
--    注意：删除后教务批量导入排课改为按“课程名称 + 教学班 + 教师工号”匹配，
--          同名课程由系统检测后要求人工确认，避免错配。
-- -----------------------------------------------------------------------------
ALTER TABLE `course` DROP COLUMN `course_code`;

-- -----------------------------------------------------------------------------
-- ③ 作业：改为题目型在线作答，homework_item 作废
--    作业的“提交事实”改由 submission / submission_answer 承载（见 12_assessment.sql）。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `homework_item`;

-- -----------------------------------------------------------------------------
-- ④ 覆盖率下限：1/2 → 1/3
--    2026-09 确认：考勤次数不低于课次的 1/3；提醒、督办、置信度判低统一按 1/3。
-- -----------------------------------------------------------------------------
ALTER TABLE `risk_engine_config`
  MODIFY COLUMN `coverage_threshold` decimal(4,3) NOT NULL DEFAULT 0.333 COMMENT '点名覆盖率阈值（1/3；低于此值：提醒补录+教务督办+置信度判低）';
UPDATE `risk_engine_config` SET `coverage_threshold` = 0.333 WHERE `coverage_threshold` = 0.500;

-- -----------------------------------------------------------------------------
-- 修订结果自检（执行后可单独运行这段核对）
-- -----------------------------------------------------------------------------
SELECT '修订后表数量' AS `检查项`, COUNT(*) AS `值`
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';

SELECT '课程编码列是否已删' AS `检查项`,
       IF(COUNT(*) = 0, '已删除', '仍存在') AS `值`
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'course' AND column_name = 'course_code';

SELECT '知识点表是否已删' AS `检查项`,
       IF(COUNT(*) = 0, '已删除', '仍存在') AS `值`
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'course_knowledge_point';

SELECT '含 kp 字段的表' AS `检查项`,
       IFNULL(GROUP_CONCAT(DISTINCT CONCAT(table_name, '.', column_name)), '无') AS `值`
FROM information_schema.columns
WHERE table_schema = DATABASE() AND (column_name LIKE 'kp%' OR column_name = 'knowledge_point_id');
