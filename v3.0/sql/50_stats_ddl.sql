-- =====================================================================
-- P1-2  统计物化表 DDL（v3.0）
-- 依据：PLAN_v3.0.md §D3；v3.0/docs/metrics.md §8
-- 三张表均带 engine_version / source_max_update_time / is_stale，
-- 供 P1-3 物化任务做失效校验与重算。
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `stat_daily_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日期（自然日）',
  `teaching_class_id` bigint NOT NULL COMMENT '教学班ID',
  `student_count` int NOT NULL DEFAULT 0 COMMENT '班级学生数',
  `red` int NOT NULL DEFAULT 0 COMMENT '红灯数',
  `orange` int NOT NULL DEFAULT 0 COMMENT '橙灯数',
  `yellow` int NOT NULL DEFAULT 0 COMMENT '黄灯数',
  `green` int NOT NULL DEFAULT 0 COMMENT '绿灯数',
  `avg_score` decimal(6,2) DEFAULT NULL COMMENT '平均风险分',
  `alert_new` int NOT NULL DEFAULT 0 COMMENT '当日新增预警',
  `alert_closed` int NOT NULL DEFAULT 0 COMMENT '当日关闭预警',
  `intervention_count` int NOT NULL DEFAULT 0 COMMENT '当日干预数',
  `engine_version` varchar(20) DEFAULT NULL COMMENT '生成时引擎版本',
  `source_max_update_time` datetime DEFAULT NULL COMMENT '数据源最大更新时间',
  `is_stale` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1=已过期待重算',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_class` (`stat_date`,`teaching_class_id`),
  KEY `idx_stale_date` (`is_stale`,`stat_date`),
  KEY `idx_class_date` (`teaching_class_id`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级日快照（统计物化）';

CREATE TABLE IF NOT EXISTS `stat_weekly_kpi` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `week_start` date NOT NULL COMMENT '自然周起始（周一）',
  `scope_type` varchar(20) NOT NULL COMMENT '作用域：ALL/COUNSELOR/CLASS/COURSE',
  `scope_id` bigint NOT NULL DEFAULT 0 COMMENT '作用域ID（ALL=0）',
  `metric` varchar(40) NOT NULL COMMENT '指标编码：close_rate/avg_handle_hours/improve_rate/risk_ratio...',
  `current_value` decimal(12,4) DEFAULT NULL COMMENT '本期值',
  `prev_value` decimal(12,4) DEFAULT NULL COMMENT '上期值',
  `delta_rate` decimal(8,2) DEFAULT NULL COMMENT '环比（%）',
  `engine_version` varchar(20) DEFAULT NULL COMMENT '生成时引擎版本',
  `source_max_update_time` datetime DEFAULT NULL COMMENT '数据源最大更新时间',
  `is_stale` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1=已过期待重算',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_week_scope_metric` (`week_start`,`scope_type`,`scope_id`,`metric`),
  KEY `idx_metric_week` (`metric`,`week_start`),
  KEY `idx_stale` (`is_stale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='周 KPI（统计物化）';

CREATE TABLE IF NOT EXISTS `stat_intervention_effect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `week_start` date NOT NULL COMMENT '自然周起始（周一）',
  `teaching_class_id` bigint NOT NULL DEFAULT 0 COMMENT '教学班ID（0=全部）',
  `cohort` varchar(10) NOT NULL COMMENT '组别：TREAT/CONTROL',
  `intervened` int NOT NULL DEFAULT 0 COMMENT '干预数',
  `improved` int NOT NULL DEFAULT 0 COMMENT '改善数',
  `unchanged` int NOT NULL DEFAULT 0 COMMENT '持平数',
  `worsened` int NOT NULL DEFAULT 0 COMMENT '恶化数',
  `engine_version` varchar(20) DEFAULT NULL COMMENT '生成时引擎版本',
  `source_max_update_time` datetime DEFAULT NULL COMMENT '数据源最大更新时间',
  `is_stale` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1=已过期待重算',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_week_class_cohort` (`week_start`,`teaching_class_id`,`cohort`),
  KEY `idx_cohort_week` (`cohort`,`week_start`),
  KEY `idx_stale` (`is_stale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='干预成效（统计物化，含对照组）';

-- 自检
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('stat_daily_class','stat_weekly_kpi','stat_intervention_effect')
ORDER BY table_name;
