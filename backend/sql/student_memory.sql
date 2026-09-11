-- =============================================================================
-- 学生长期记忆表（W3：让智能体"记得上次建议"）
-- =============================================================================
-- 每次流水线跑完，把该生该课程的画像等级、上期策略、最近干预、学生目标、
-- 最近预警与推荐计划压缩成一段短文本存下来（按版本递增，保留历史）；
-- 下次跑分析/推荐时把最新一条注入 prompt，实现跨轮次的"记忆"。
-- 说明：脚本可重复执行（IF NOT EXISTS）。
-- =============================================================================

CREATE TABLE IF NOT EXISTS `student_memory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `version` int NOT NULL DEFAULT 1 COMMENT '记忆版本（每次刷新+1）',
  `summary` text NOT NULL COMMENT '压缩后的记忆摘要文本',
  `source_run_id` bigint DEFAULT NULL COMMENT '产生该记忆的流水线运行ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_memory_student` (`student_id`, `course_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生长期记忆（跨轮次上下文）';
