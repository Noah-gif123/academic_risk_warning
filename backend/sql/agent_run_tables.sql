-- =============================================================================
-- 智能体运行记录表（W1：可观测 / 可复现）
-- =============================================================================
-- 用途：一次流水线 = 一条 agent_run；每一步智能体 = 一条 agent_run_step。
--       用于教师端"智能体运行历史"面板与 /api/agent/stats 的真实统计
--       （成功率、各智能体失败率与平均耗时、P95 耗时）。
-- 说明：脚本可重复执行（IF NOT EXISTS）。
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agent_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `pipeline` varchar(30) NOT NULL COMMENT '流水线: FULL/FEEDBACK/EFFECT_BASED 等',
  `trigger_type` varchar(30) DEFAULT NULL COMMENT '触发方式: MANUAL/SCHEDULED/FEEDBACK 等',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/SUCCESS/PARTIAL/FAILED',
  `total_steps` int DEFAULT 0 COMMENT '总步数',
  `succeeded_steps` int DEFAULT 0 COMMENT '成功步数',
  `failed_steps` int DEFAULT 0 COMMENT '失败步数',
  `total_ms` bigint DEFAULT NULL COMMENT '各步耗时之和(ms)',
  `error` varchar(1000) DEFAULT NULL COMMENT '失败原因(首个错误)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `finish_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_run_student` (`student_id`, `create_time`),
  KEY `idx_run_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体流水线运行记录';

CREATE TABLE IF NOT EXISTS `agent_run_step` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '所属运行ID',
  `step_no` int NOT NULL COMMENT '步骤序号(从1开始)',
  `agent_name` varchar(50) NOT NULL COMMENT '智能体名称',
  `status` varchar(20) NOT NULL COMMENT '状态: SUCCESS/FAILED',
  `duration_ms` bigint DEFAULT NULL COMMENT '本步耗时(ms)',
  `output_digest` varchar(500) DEFAULT NULL COMMENT '产出摘要(截断)',
  `error` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `validation_status` varchar(20) DEFAULT NULL COMMENT '输出校验: PASS/WARN/FAIL/SKIP',
  `validation_detail` varchar(500) DEFAULT NULL COMMENT '校验说明(结构/取值/真值一致性)',
  `reflection` varchar(1000) DEFAULT NULL COMMENT '反思信息(自检批评)',
  `attempts` int DEFAULT 1 COMMENT 'LLM 调用轮次(>1 表示触发了反思重写)',
  `output_fields` varchar(300) DEFAULT NULL COMMENT '产出顶层字段清单(逗号分隔)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_step_run` (`run_id`, `step_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体单步运行明细';
