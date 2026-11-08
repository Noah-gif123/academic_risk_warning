-- =====================================================================
-- P2-11 记忆表（v3.0）
-- student_memory：摘要（≤300 字）+ 关键标签；student_memory_event：事件流。
-- 合规：学生删除后摘要与事件均删除，不再注入 Agent。
-- =====================================================================
CREATE TABLE IF NOT EXISTS `student_memory` (
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `summary` varchar(300) DEFAULT NULL COMMENT '画像摘要（≤300字）',
  `top_weak_groups` json DEFAULT NULL COMMENT '薄弱知识点/课程',
  `attendance_tag` varchar(50) DEFAULT NULL COMMENT '考勤标签',
  `intervention_count` int NOT NULL DEFAULT 0 COMMENT '干预次数',
  `last_intervention_conclusion` varchar(300) DEFAULT NULL COMMENT '上次干预结论',
  `recent_levels` varchar(100) DEFAULT NULL COMMENT '最近风险等级序列',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生画像摘要';

CREATE TABLE IF NOT EXISTS `student_memory_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `type` varchar(32) NOT NULL COMMENT 'RISK_CHANGE/INTERVENTION/QA/QUESTIONNAIRE',
  `summary` varchar(300) NOT NULL,
  `source_id` varchar(64) DEFAULT NULL,
  `weight` varchar(10) NOT NULL DEFAULT 'LOW' COMMENT 'HIGH/LOW',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_type` (`student_id`,`type`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生画像事件流';

SELECT table_name FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN ('student_memory','student_memory_event')
ORDER BY table_name;
