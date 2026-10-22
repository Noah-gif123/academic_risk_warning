-- ============================================================
-- 21_tidy_after_rebuild.sql  重建后的收尾：清孤儿、隔离合成学期的任课关系
-- 可重复执行
-- ============================================================
USE study_warning_system_v2;

-- 1) 旧主线的 CS-01 教学班（offering 1 已重建为 101，这里成了孤儿）
DELETE FROM teaching_class_admin_class WHERE teaching_class_id IN (SELECT id FROM teaching_class WHERE offering_id NOT IN (SELECT id FROM course_offering));
DELETE FROM counselor_class WHERE admin_class_id NOT IN (SELECT id FROM administrative_class);
DELETE FROM teaching_class WHERE offering_id NOT IN (SELECT id FROM course_offering);

-- 1.1) 旧合成学期的教学班（7~24）已被 301~306 取代，且不再有选课
DELETE FROM teaching_class_admin_class WHERE teaching_class_id BETWEEN 2 AND 200;
DELETE FROM teaching_class WHERE id BETWEEN 2 AND 200;

-- 2) 合成学期（term 8）的教学班改用 6~9 号教师，避免和主线教师（1001~1005）混在一起
UPDATE teaching_class tc
  JOIN course_offering o ON o.id = tc.offering_id
   SET tc.teacher_id = 6 + (tc.id % 4)
 WHERE o.term_id = 8;

-- 3) 合成学期的成绩/提交备注清空（内部标记不写进业务字段）
UPDATE submission SET remark = NULL WHERE remark IS NOT NULL AND remark <> '';

-- 4) 校验
SELECT '教学班（应按学期分组）' AS r;
SELECT o.term_id, COUNT(*) AS teaching_classes
  FROM teaching_class tc JOIN course_offering o ON o.id = tc.offering_id
 GROUP BY o.term_id ORDER BY o.term_id;

SELECT '教师带班（仅主线 term5）' AS r;
SELECT t.teacher_no, t.teacher_name, COUNT(*) AS classes
  FROM teaching_class tc
  JOIN course_offering o ON o.id = tc.offering_id
  JOIN teacher t ON t.id = tc.teacher_id
 WHERE o.term_id = 5
 GROUP BY t.id ORDER BY t.id;

SELECT '孤儿检查（应全为 0）' AS r;
SELECT
  (SELECT COUNT(*) FROM teaching_class tc WHERE tc.offering_id NOT IN (SELECT id FROM course_offering)) AS orphan_tc,
  (SELECT COUNT(*) FROM enrollment e WHERE e.teaching_class_id NOT IN (SELECT id FROM teaching_class)) AS orphan_enroll,
  (SELECT COUNT(*) FROM attendance_record a WHERE a.enrollment_id NOT IN (SELECT id FROM enrollment)) AS orphan_att,
  (SELECT COUNT(*) FROM attendance_record a WHERE a.session_id NOT IN (SELECT id FROM course_session)) AS orphan_att_sess,
  (SELECT COUNT(*) FROM score_result s WHERE s.enrollment_id NOT IN (SELECT id FROM enrollment)) AS orphan_result,
  (SELECT COUNT(*) FROM submission s WHERE s.enrollment_id NOT IN (SELECT id FROM enrollment)) AS orphan_sub,
  (SELECT COUNT(*) FROM risk_state s WHERE s.enrollment_id NOT IN (SELECT id FROM enrollment)) AS orphan_risk,
  (SELECT COUNT(*) FROM student s WHERE s.admin_class_id NOT IN (SELECT id FROM administrative_class)) AS orphan_student;
