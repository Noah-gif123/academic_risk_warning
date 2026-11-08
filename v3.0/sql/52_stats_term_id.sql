-- =====================================================================
-- P1 硬化 2：stat 三表增加 term_id（多学期支持）
-- 背景：50_stats_ddl.sql 初版唯一键未含学期，只能服务 term=5。
-- 幂等：information_schema 判断后动态执行。
-- =====================================================================

-- 1) 加列
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='stat_daily_class' AND column_name='term_id');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_daily_class` ADD COLUMN `term_id` bigint NOT NULL DEFAULT 5 COMMENT ''学期ID'' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='stat_weekly_kpi' AND column_name='term_id');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_weekly_kpi` ADD COLUMN `term_id` bigint NOT NULL DEFAULT 5 COMMENT ''学期ID'' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='stat_intervention_effect' AND column_name='term_id');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_intervention_effect` ADD COLUMN `term_id` bigint NOT NULL DEFAULT 5 COMMENT ''学期ID'' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 回填（历史行均为 term=5；已有 term_id 的行不动）
UPDATE stat_daily_class SET term_id = 5 WHERE term_id IS NULL OR term_id = 0;
UPDATE stat_weekly_kpi SET term_id = 5 WHERE term_id IS NULL OR term_id = 0;
UPDATE stat_intervention_effect SET term_id = 5 WHERE term_id IS NULL OR term_id = 0;

-- 3) 删旧唯一键（存在才删）
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_daily_class' AND index_name='uk_date_class');
SET @sql := IF(@exist>0, 'ALTER TABLE `stat_daily_class` DROP INDEX `uk_date_class`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_weekly_kpi' AND index_name='uk_week_scope_metric');
SET @sql := IF(@exist>0, 'ALTER TABLE `stat_weekly_kpi` DROP INDEX `uk_week_scope_metric`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_intervention_effect' AND index_name='uk_week_class_cohort');
SET @sql := IF(@exist>0, 'ALTER TABLE `stat_intervention_effect` DROP INDEX `uk_week_class_cohort`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) 新唯一键（含 term_id）
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_daily_class' AND index_name='uk_term_date_class');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_daily_class` ADD UNIQUE KEY `uk_term_date_class` (`term_id`,`stat_date`,`teaching_class_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_weekly_kpi' AND index_name='uk_term_week_scope_metric');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_weekly_kpi` ADD UNIQUE KEY `uk_term_week_scope_metric` (`term_id`,`week_start`,`scope_type`,`scope_id`,`metric`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_intervention_effect' AND index_name='uk_term_week_class_cohort');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_intervention_effect` ADD UNIQUE KEY `uk_term_week_class_cohort` (`term_id`,`week_start`,`teaching_class_id`,`cohort`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) 查询索引
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_daily_class' AND index_name='idx_term_date');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_daily_class` ADD INDEX `idx_term_date` (`term_id`,`stat_date`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_weekly_kpi' AND index_name='idx_term_metric_week');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_weekly_kpi` ADD INDEX `idx_term_metric_week` (`term_id`,`metric`,`week_start`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='stat_intervention_effect' AND index_name='idx_term_week');
SET @sql := IF(@exist=0, 'ALTER TABLE `stat_intervention_effect` ADD INDEX `idx_term_week` (`term_id`,`week_start`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 自检
SELECT table_name, column_name FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name LIKE 'stat%' AND column_name='term_id';
