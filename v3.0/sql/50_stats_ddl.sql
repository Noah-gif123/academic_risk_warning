-- =====================================================================
-- P1-2  统计物化表 DDL（v3.0，P1 硬化后终版）
-- 依据：PLAN_v3.0.md §D3；v3.0/docs/metrics.md §8
-- 三张表均带 term_id / engine_version / source_max_update_time / is_stale。
-- 旧库升级走 52_stats_term_id.sql；本文件供全新 bootstrap 使用。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `stat_daily_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `term_id` bigint NOT NULL COMMENT '学期ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `teaching_class_id` bigint NOT NULL COMMENT '教学班ID',
  `student_count` int NOT NULL DEFAULT 0 COMMENT '班级学生数',
  `red` int NOT NULL DEFAULT 0,
  `orange` int NOT NULL DEFAULT 0,
  `yellow` int NOT NULL DEFAULT 0,
  `green` int NOT NULL DEFAULT 0,
  `avg_score` decimal(6,2) DEFAULT NULL COMMENT '平均风险分',
  `alert_new` int NOT NULL DEFAULT 0 COMMENT '当日新增预警',
  `alert_closed` int NOT NULL DEFAULT 0 COMMENT '当日关闭预警',
  `intervention_count` int NOT NULL DEFAULT 0 COMMENT '当日干预数',
  `engine_version` varchar(20) DEFAULT NULL,
  `source_max_update_time` datetime DEFAULT NULL,
  `is_stale` tinyint(1) NOT NULL DEFAULT 0,
  `source` varchar(20) NOT NULL DEFAULT 'MATERIALIZED' COMMENT 'MATERIALIZED/DEMO_BACKFILL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_date_class` (`term_id`,`stat_date`,`teaching_class_id`),
  KEY `idx_term_date` (`term_id`,`stat_date`),
  KEY `idx_class_date` (`teaching_class_id`,`stat_date`),
  KEY `idx_stale_date` (`is_stale`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级日快照（统计物化）';

CREATE TABLE IF NOT EXISTS `stat_weekly_kpi` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `term_id` bigint NOT NULL COMMENT '学期ID',
  `week_start` date NOT NULL COMMENT '自然周起始（周一）',
  `scope_type` varchar(20) NOT NULL COMMENT 'ALL/COUNSELOR/CLASS/COURSE',
  `scope_id` bigint NOT NULL DEFAULT 0 COMMENT '作用域ID（ALL=0）',
  `metric` varchar(40) NOT NULL COMMENT '指标编码',
  `current_value` decimal(12,4) DEFAULT NULL,
  `prev_value` decimal(12,4) DEFAULT NULL,
  `delta_rate` decimal(8,2) DEFAULT NULL,
  `engine_version` varchar(20) DEFAULT NULL,
  `source_max_update_time` datetime DEFAULT NULL,
  `is_stale` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_week_scope_metric` (`term_id`,`week_start`,`scope_type`,`scope_id`,`metric`),
  KEY `idx_term_metric_week` (`term_id`,`metric`,`week_start`),
  KEY `idx_metric_week` (`metric`,`week_start`),
  KEY `idx_stale` (`is_stale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='周 KPI（统计物化）';

CREATE TABLE IF NOT EXISTS `stat_intervention_effect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `term_id` bigint NOT NULL COMMENT '学期ID',
  `week_start` date NOT NULL COMMENT '自然周起始（周一）',
  `teaching_class_id` bigint NOT NULL DEFAULT 0 COMMENT '教学班ID（0=全部）',
  `cohort` varchar(10) NOT NULL COMMENT 'TREAT/CONTROL',
  `intervened` int NOT NULL DEFAULT 0,
  `improved` int NOT NULL DEFAULT 0,
  `unchanged` int NOT NULL DEFAULT 0,
  `worsened` int NOT NULL DEFAULT 0,
  `engine_version` varchar(20) DEFAULT NULL,
  `source_max_update_time` datetime DEFAULT NULL,
  `is_stale` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_week_class_cohort` (`term_id`,`week_start`,`teaching_class_id`,`cohort`),
  KEY `idx_term_week` (`term_id`,`week_start`),
  KEY `idx_cohort_week` (`cohort`,`week_start`),
  KEY `idx_stale` (`is_stale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='干预成效（统计物化，含对照组）';

SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('stat_daily_class','stat_weekly_kpi','stat_intervention_effect')
ORDER BY table_name;
