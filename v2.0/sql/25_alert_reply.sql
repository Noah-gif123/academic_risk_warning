-- ============================================================
-- 25_alert_reply.sql  预警申诉闭环：教师回复字段（幂等，可重复执行）
-- ============================================================
USE study_warning_system_v2;

SET @has_reply := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert_record' AND COLUMN_NAME = 'teacher_reply'
);
SET @ddl := IF(@has_reply = 0,
  'ALTER TABLE alert_record
     ADD COLUMN teacher_reply VARCHAR(500) NULL COMMENT ''教师对申诉的回复'' AFTER student_response_time,
     ADD COLUMN teacher_reply_time DATETIME NULL COMMENT ''教师回复时间'' AFTER teacher_reply',
  'SELECT ''teacher_reply 列已存在，跳过'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'alert_record 申诉字段' AS r;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert_record'
   AND COLUMN_NAME IN ('student_response', 'student_response_time', 'teacher_reply', 'teacher_reply_time');
