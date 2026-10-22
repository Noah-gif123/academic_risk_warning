-- ============================================================
-- 19_cleanup_demo.sql  演示库「清理脏数据」：只删不该进演示的东西，不动主线结构
-- 执行前请先备份：mysqldump study_warning_system_v2 > backup.sql
-- 可重复执行（幂等）
-- ============================================================
USE study_warning_system_v2;

-- ------------------------------------------------------------
-- 1) 删测试试卷（"111" / "222"），连同题目、提交、逐题作答
-- ------------------------------------------------------------
DELETE sa FROM submission_answer sa
  JOIN submission s ON s.id = sa.submission_id
 WHERE s.paper_id IN (52, 56);
DELETE FROM submission WHERE paper_id IN (52, 56);
DELETE FROM question   WHERE paper_id IN (52, 56);
DELETE FROM paper      WHERE id IN (52, 56);

-- ------------------------------------------------------------
-- 2) 删空壳/无用学期：1、2、3（v1 迁移遗留，全空）与 7（合成冒烟学期）
--    term 4 保留为“上一学期”（用于学生挂科历史），term 5 主线，term 8 合成样本
-- ------------------------------------------------------------
DELETE FROM enrollment             WHERE term_id IN (1, 2, 3, 7);
DELETE FROM attendance_record       WHERE term_id IN (1, 2, 3, 7);
DELETE FROM attendance_archive      WHERE term_id IN (1, 2, 3, 7);
DELETE FROM course_session          WHERE term_id IN (1, 2, 3, 7);
DELETE FROM score_item              WHERE term_id IN (1, 2, 3, 7);
DELETE FROM score_result            WHERE term_id IN (1, 2, 3, 7);
DELETE FROM submission              WHERE term_id IN (1, 2, 3, 7);
DELETE FROM paper                   WHERE term_id IN (1, 2, 3, 7);
DELETE FROM risk_state              WHERE term_id IN (1, 2, 3, 7);
DELETE FROM alert_record            WHERE term_id IN (1, 2, 3, 7);
DELETE FROM intervention_record     WHERE term_id IN (1, 2, 3, 7);
DELETE FROM student_fail_history    WHERE term_id IN (1, 2, 3, 7);
DELETE FROM course_fail_rate_history WHERE term_id IN (1, 2, 3, 7);
DELETE FROM teaching_class_fail_rate WHERE term_id IN (1, 2, 3, 7);
DELETE FROM admin_class_fail_rate   WHERE term_id IN (1, 2, 3, 7);
DELETE FROM coverage_check_log      WHERE term_id IN (1, 2, 3, 7);
DELETE FROM counselor_class         WHERE term_id IN (1, 2, 3, 7);
DELETE FROM course_offering         WHERE term_id IN (1, 2, 3, 7);
DELETE FROM course_group            WHERE term_id IN (1, 2, 3, 7);
DELETE FROM term_calendar           WHERE term_id IN (1, 2, 3, 7);
DELETE FROM term                    WHERE id     IN (1, 2, 3, 7);

-- ------------------------------------------------------------
-- 3) 学期状态机：同一时刻只允许一个 ACTIVE，且必须是 is_current=1 的那个
-- ------------------------------------------------------------
UPDATE term SET status = 'CLOSED' WHERE is_current = 0 AND status = 'ACTIVE';
UPDATE term SET status = 'ACTIVE' WHERE is_current = 1;

-- ------------------------------------------------------------
-- 4) 合成学期规范化：名字标注、不与主线混用
-- ------------------------------------------------------------
UPDATE term SET term_name = '2027-2028学年-第1学期（合成数据·勿混用）'
 WHERE term_code = 'SYN-2027-1';

-- ------------------------------------------------------------
-- 5) 枚举修正：intervention_record.stage 的非法值 STAGE1 → TEACHER
-- ------------------------------------------------------------
UPDATE intervention_record SET stage = 'TEACHER' WHERE stage = 'STAGE1';

-- ------------------------------------------------------------
-- 6) 清掉写进业务字段的内部标记（会显示在学生的“批注”里）
-- ------------------------------------------------------------
UPDATE submission SET remark = NULL
 WHERE remark IN ('合成数据', '演示数据')
    OR remark LIKE '%合成数据%'
    OR remark LIKE '%演示数据%';

-- ------------------------------------------------------------
-- 7) teacher_style 里的合成真值（engine_version = SYNTH-TRUTH）不在业务表里保留
--    —— 由 20_rebuild_demo.sql 按真实口径重算
-- ------------------------------------------------------------
DELETE FROM teacher_style WHERE engine_version = 'SYNTH-TRUTH';

-- ------------------------------------------------------------
-- 校验
-- ------------------------------------------------------------
SELECT '清理后各表行数' AS result;
SELECT 'term' AS t, COUNT(*) AS n FROM term
UNION ALL SELECT 'paper', COUNT(*) FROM paper
UNION ALL SELECT 'submission', COUNT(*) FROM submission
UNION ALL SELECT 'question', COUNT(*) FROM question
UNION ALL SELECT 'intervention_record', COUNT(*) FROM intervention_record
UNION ALL SELECT 'teacher_style', COUNT(*) FROM teacher_style
UNION ALL SELECT 'student', COUNT(*) FROM student;

SELECT '学期现状' AS result;
SELECT id, term_code, term_name, is_current, status FROM term ORDER BY id;
