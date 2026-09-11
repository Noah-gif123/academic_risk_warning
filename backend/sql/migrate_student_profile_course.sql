-- =============================================================================
-- 升级脚本：student_profile 由「学生级」改为「(学生, 课程)级」
-- =============================================================================
-- 背景：修复前 ProfileAgent 只按 student_id 保存一行画像，学生修多门课时
--       后跑的课程会覆盖前一门课的画像，且不同课程的数据被混算。
-- 适用：已经建过 student_profile 表的旧库，执行一次即可（脚本可重复执行）。
--       全新环境使用 missing_tables.sql 建表，不需要再执行本脚本。
-- =============================================================================

-- 1) 补充 course_id 列（NULL 表示跨课程综合画像）
SET @has_column := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'student_profile' AND column_name = 'course_id'
);
SET @ddl := IF(@has_column = 0,
  'ALTER TABLE `student_profile` ADD COLUMN `course_id` bigint DEFAULT NULL COMMENT ''课程ID（NULL=跨课程综合画像）'' AFTER `student_id`',
  'SELECT ''student_profile.course_id 已存在，跳过'' AS skip_msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 去掉旧的「一个学生一行」唯一约束
SET @has_old_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'student_profile' AND index_name = 'uk_student'
);
SET @ddl := IF(@has_old_uk > 0,
  'ALTER TABLE `student_profile` DROP INDEX `uk_student`',
  'SELECT ''uk_student 不存在，跳过'' AS skip_msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 建立「(学生, 课程)」唯一约束
SET @has_new_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'student_profile' AND index_name = 'uk_student_course'
);
SET @ddl := IF(@has_new_uk = 0,
  'ALTER TABLE `student_profile` ADD UNIQUE KEY `uk_student_course` (`student_id`,`course_id`)',
  'SELECT ''uk_student_course 已存在，跳过'' AS skip_msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
