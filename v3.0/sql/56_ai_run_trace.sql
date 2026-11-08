-- =====================================================================
-- P2-12 ai_run / ai_run_step 扩展（轨迹页）
-- ai_run：provider / role_count / run_key（关联 P2-6 台账）
-- ai_run_step：step_role / tool_name / args_digest / result_digest / gate_status
-- =====================================================================
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run' AND column_name='provider');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run` ADD COLUMN `provider` varchar(20) DEFAULT ''LOCAL'' COMMENT ''LOCAL/DIFY''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run' AND column_name='role_count');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run` ADD COLUMN `role_count` int NOT NULL DEFAULT 0 COMMENT ''参与角色数''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run' AND column_name='run_key');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run` ADD COLUMN `run_key` varchar(64) DEFAULT NULL COMMENT ''证据台账 runKey''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run_step' AND column_name='step_role');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run_step` ADD COLUMN `step_role` varchar(20) DEFAULT NULL COMMENT ''PLANNER/EXECUTOR/CRITIC/WRITER''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run_step' AND column_name='tool_name');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run_step` ADD COLUMN `tool_name` varchar(64) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run_step' AND column_name='args_digest');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run_step` ADD COLUMN `args_digest` varchar(64) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run_step' AND column_name='result_digest');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run_step` ADD COLUMN `result_digest` varchar(64) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name='ai_run_step' AND column_name='gate_status');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run_step` ADD COLUMN `gate_status` varchar(20) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema=DATABASE() AND table_name='ai_run' AND index_name='idx_run_key');
SET @sql := IF(@exist=0, 'ALTER TABLE `ai_run` ADD INDEX `idx_run_key` (`run_key`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT column_name FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='ai_run_step'
  AND column_name IN ('step_role','tool_name','args_digest','result_digest','gate_status');
