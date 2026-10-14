-- 学生作答草稿表
CREATE TABLE IF NOT EXISTS `exercise_draft` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `recommend_id` BIGINT NOT NULL COMMENT '推荐记录ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `answer` TEXT COMMENT '无子题时的作答文本',
  `sub_answers` TEXT COMMENT 'JSON格式的子题作答 Map<subQuestionId, answer>',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_recommend_student` (`recommend_id`, `student_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生作答草稿';
