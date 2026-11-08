-- =====================================================================
-- P1-2 补充：intervention_record 增加 T+7 复评字段（v3.0）
-- 依据：PLAN_v3.0.md §D1/D2；v3.0/docs/metrics.md 指标 3/4/对照组
--   level_before   干预时风险等级（RED/ORANGE/YELLOW/GREEN）
--   level_after    T+7 复评等级（未复评为 NULL）
--   cohort         TREAT 干预组 / CONTROL 延迟干预对照组
-- 幂等：用 information_schema 判断后动态执行，可重复运行。
-- =====================================================================

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'intervention_record'
                 AND column_name = 'level_before');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `intervention_record` ADD COLUMN `level_before` varchar(20) NULL COMMENT ''干预时风险等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'intervention_record'
                 AND column_name = 'level_after');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `intervention_record` ADD COLUMN `level_after` varchar(20) NULL COMMENT ''T+7 复评等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'intervention_record'
                 AND column_name = 'cohort');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `intervention_record` ADD COLUMN `cohort` varchar(10) NULL COMMENT ''TREAT/CONTROL''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'intervention_record'
                 AND index_name = 'idx_term_cohort_create');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `intervention_record` ADD INDEX `idx_term_cohort_create` (`term_id`,`cohort`,`create_time`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 自检
SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'intervention_record'
  AND column_name IN ('level_before','level_after','cohort')
ORDER BY ordinal_position;
