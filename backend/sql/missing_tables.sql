-- =============================================================================
-- 学情画像 / 智能体产物相关表补充脚本
-- =============================================================================
-- 背景：以下 7 张表此前只在开发库中存在，仓库的 study_warning_system.sql 与
--       exercise_draft.sql 均未包含，导致按 README 初始化数据库后，
--       学情画像（ProfileAgent）与智能体流水线运行时会报表不存在。
-- 用法：全新环境按 README 建库时，在 study_warning_system.sql、exercise_draft.sql 之后执行本脚本。
--       已存在的旧库请改用 migrate_student_profile_course.sql 升级 student_profile。
-- =============================================================================

-- 1. 出勤明细（Attendance 实体）
CREATE TABLE IF NOT EXISTS `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `total_hours` int DEFAULT NULL COMMENT '总课时',
  `absent_count` int DEFAULT 0 COMMENT '缺勤次数',
  `late_count` int DEFAULT 0 COMMENT '迟到次数',
  `attendance_rate` decimal(5,2) DEFAULT NULL COMMENT '出勤率(%)',
  PRIMARY KEY (`id`),
  KEY `idx_attendance_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生出勤明细';

-- 2. 监测智能体报告（MonitorReport 实体）
CREATE TABLE IF NOT EXISTS `monitor_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `report_date` date NOT NULL COMMENT '报告日期',
  `summary` text COMMENT '监测摘要',
  `risk_dimensions` json DEFAULT NULL COMMENT '五维风险快照(JSON)',
  `anomaly_flags` json DEFAULT NULL COMMENT '异常标记(JSON)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_monitor_student` (`student_id`),
  KEY `idx_monitor_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监测智能体报告';

-- 3. 分析智能体报告（AnalysisReport 实体）
CREATE TABLE IF NOT EXISTS `analysis_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `analysis_date` datetime NOT NULL COMMENT '分析时间',
  `weakness_category` varchar(100) DEFAULT NULL COMMENT '薄弱类别',
  `root_cause` text COMMENT '根源分析',
  `priority_sequence` json DEFAULT NULL COMMENT '补强优先级(JSON)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_analysis_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析智能体报告';

-- 4. 策略调整记录（StrategyRecord 实体）
CREATE TABLE IF NOT EXISTS `strategy_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `previous_strategy` varchar(500) DEFAULT NULL COMMENT '原策略',
  `new_strategy` varchar(500) DEFAULT NULL COMMENT '新策略',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '调整原因',
  `trigger_source` varchar(30) DEFAULT NULL COMMENT '触发来源: FEEDBACK/EFFECT_CHECK 等',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略智能体调整记录';

-- 5. 学习计划（StudyPlan 实体）
CREATE TABLE IF NOT EXISTS `study_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `plan_content` json DEFAULT NULL COMMENT '分阶段计划(JSON)',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/FINISHED/CANCELLED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_student` (`student_id`),
  KEY `idx_plan_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐智能体学习计划';

-- 6. 学生学习目标（StudentGoal 实体）
CREATE TABLE IF NOT EXISTS `student_goal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `goal_level` varchar(20) NOT NULL COMMENT '目标等级：冲刺/稳定/追赶/保底',
  `target_score` int DEFAULT NULL COMMENT '目标分数(0-100)',
  `milestones` text COMMENT '里程碑列表(JSON数组)',
  `note` varchar(500) DEFAULT NULL COMMENT '学生备注',
  `deadline` datetime DEFAULT NULL COMMENT '截止日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生学习目标';

-- 7. 学生三维画像（StudentProfile 实体）
--    注意：画像按 (学生, 课程) 维度保存；course_id 为 NULL 表示跨课程的综合画像。
CREATE TABLE IF NOT EXISTS `student_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=跨课程综合画像）',
  `knowledge_profile` json DEFAULT NULL COMMENT '知识掌握度画像',
  `habit_profile` json DEFAULT NULL COMMENT '学习习惯画像',
  `goal_profile` json DEFAULT NULL COMMENT '学习目标画像',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生三维画像';
