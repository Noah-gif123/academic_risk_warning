-- =============================================================================
-- 36_engine_v2_1_config.sql  C 通道（课程类型 / 历史挂科率 / 教师风格 / 个人前科）配置项
--   v2.1.0 判定口径：考试课更难、考查课更松；同一水平的学生在不同课/不同老师手里风险不同。
--   系数全部落库（可改、可审计），引擎只读不改。
--   幂等：列不存在才 ADD，配置行按 engine_version 覆盖。
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 加列（MySQL 8.0 没有 ADD COLUMN IF NOT EXISTS，用 information_schema 判一下）
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'course_factor_exam');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `course_factor_exam` decimal(4,2) NOT NULL DEFAULT 1.10 COMMENT ''考试课难度系数（卷面难→风险上调）''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'course_factor_assess');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `course_factor_assess` decimal(4,2) NOT NULL DEFAULT 0.90 COMMENT ''考查课难度系数（大作业为主→风险下调）''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'fail_rate_base');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `fail_rate_base` decimal(5,2) NOT NULL DEFAULT 5.00 COMMENT ''历史挂科率基准（%）''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'fail_rate_step');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `fail_rate_step` decimal(4,2) NOT NULL DEFAULT 1.05 COMMENT ''历史挂科率每高 5 个百分点的乘数''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'fail_rate_cap');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `fail_rate_cap` decimal(4,2) NOT NULL DEFAULT 1.20 COMMENT ''历史挂科率系数上限''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'personal_prior_1');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `personal_prior_1` decimal(4,2) NOT NULL DEFAULT 1.05 COMMENT ''个人历史 1 门前科系数''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'personal_prior_2');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `personal_prior_2` decimal(4,2) NOT NULL DEFAULT 1.10 COMMENT ''个人历史 ≥2 门前科系数''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'teacher_strict_factor');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `teacher_strict_factor` decimal(4,2) NOT NULL DEFAULT 1.05 COMMENT ''严格型教师系数''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'risk_engine_config'
             AND column_name = 'teacher_loose_factor');
SET @s := IF(@c = 0,
    'ALTER TABLE `risk_engine_config` ADD COLUMN `teacher_loose_factor` decimal(4,2) NOT NULL DEFAULT 0.95 COMMENT ''宽松型教师系数''',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2) 新增 v2.1.0 配置（沿用 v2.0.0 的阈值与开关，再打开课程因子/教师尺度）
INSERT INTO `risk_engine_config`
(`engine_version`, `effective_time`, `is_active`, `channel_a_enabled`, `channel_b_enabled`, `channel_c_enabled`,
 `delta_enabled`, `course_factor_enabled`, `threshold_red`, `threshold_orange`, `threshold_yellow`,
 `coverage_threshold`, `delta_shrink_k`, `delta_min_sample`, `delta_fuse_threshold`,
 `course_factor_exam`, `course_factor_assess`, `fail_rate_base`, `fail_rate_step`, `fail_rate_cap`,
 `personal_prior_1`, `personal_prior_2`, `teacher_strict_factor`, `teacher_loose_factor`,
 `remark`, `create_by`)
SELECT 'v2.1.0', NOW(), 0, 1, 1, 1,
       1, 1, c.threshold_red, c.threshold_orange, c.threshold_yellow,
       c.coverage_threshold, c.delta_shrink_k, c.delta_min_sample, c.delta_fuse_threshold,
       1.10, 0.90, 5.00, 1.05, 1.20,
       1.05, 1.10, 1.05, 0.95,
       'v2.1.0：过程分口径 + 绝对达标判定 + C 通道（考试课/考查课、历史挂科率、教师风格、个人前科）', c.create_by
FROM `risk_engine_config` c
WHERE c.engine_version = 'v2.0.0'
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM `risk_engine_config`) t WHERE t.engine_version = 'v2.1.0');

SELECT engine_version, is_active, channel_c_enabled, course_factor_enabled, delta_enabled,
       course_factor_exam, course_factor_assess, fail_rate_base, fail_rate_step, fail_rate_cap,
       personal_prior_1, personal_prior_2, teacher_strict_factor, teacher_loose_factor
FROM `risk_engine_config` ORDER BY id DESC LIMIT 3;
