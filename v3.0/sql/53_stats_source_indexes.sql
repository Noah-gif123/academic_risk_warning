-- =====================================================================
-- P1 硬化 7/12：日快照来源标记 + 性能索引
--   source = MATERIALIZED（物化任务写入）/ DEMO_BACKFILL（P1-0 回填）
--   alert/intervention 常用过滤补复合索引，降低 trend/overview 查询时延。
-- 幂等：存在即跳过。
-- =====================================================================

-- 1) stat_daily_class.source
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='stat_daily_class' AND column_name='source');
SET @sql := IF(@exist=0,
  'ALTER TABLE `stat_daily_class` ADD COLUMN `source` varchar(20) NOT NULL DEFAULT ''MATERIALIZED'' COMMENT ''数据来源'' AFTER `is_stale`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) alert_record 复合索引
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='alert_record' AND index_name='idx_term_create');
SET @sql := IF(@exist=0,
  'ALTER TABLE `alert_record` ADD INDEX `idx_term_create` (`term_id`,`is_void`,`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='alert_record' AND index_name='idx_term_class_create');
SET @sql := IF(@exist=0,
  'ALTER TABLE `alert_record` ADD INDEX `idx_term_class_create` (`term_id`,`teaching_class_id`,`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) intervention_record 复合索引
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='intervention_record' AND index_name='idx_term_create');
SET @sql := IF(@exist=0,
  'ALTER TABLE `intervention_record` ADD INDEX `idx_term_create` (`term_id`,`is_void`,`create_time`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 自检
SELECT table_name, index_name, group_concat(column_name ORDER BY seq_in_index) cols
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND index_name IN ('idx_term_create','idx_term_class_create')
GROUP BY table_name, index_name ORDER BY table_name, index_name;
