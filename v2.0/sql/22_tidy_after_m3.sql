-- 第 3 步验收后的演示库整理：删除测试过程产生的残留预警（不动物业数据）
USE study_warning_system_v2;

-- 1) 20250043（M2 升级测试学生，考勤已复原、当前 GREEN）的测试预警
DELETE FROM notification WHERE biz_type LIKE 'ALERT%' AND biz_id = 3173;
DELETE FROM alert_flow_log WHERE alert_id = 3173;
DELETE FROM intervention_record WHERE alert_id = 3173;
DELETE FROM alert_record WHERE id = 3173 AND student_id = 1042;

-- 2) 20250009 / 20250012：冷却期内的重复生成（原记录为人工关闭 / 已忽略）
DELETE FROM notification WHERE biz_type LIKE 'ALERT%' AND biz_id IN (3174, 3175);
DELETE FROM alert_flow_log WHERE alert_id IN (3174, 3175);
DELETE FROM intervention_record WHERE alert_id IN (3174, 3175);
DELETE FROM alert_record WHERE id IN (3174, 3175);

-- 3) 复核
SELECT 'alert_record' AS t, status, COUNT(*) AS n FROM alert_record WHERE teaching_class_id = 201 AND is_void = 0 GROUP BY status;
SELECT 'current_201' AS t, COUNT(*) AS n FROM alert_record WHERE teaching_class_id = 201 AND is_current = 1 AND is_void = 0;
SELECT 'student_20250043' AS t, risk_level, absent_count, risk_score FROM risk_state WHERE student_id = 1042;
