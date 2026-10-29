-- =============================================================================
-- 31_ai_qa.sql  第三阶段 M11：课程答疑（RAG 问答）
--   一次提问一行：问题、答案、引用、命中分、是否拒答、通道与成本
-- =============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_qa_log` (
  `id`                bigint        NOT NULL AUTO_INCREMENT,
  `student_id`        bigint        NOT NULL,
  `course_id`         bigint        NOT NULL,
  `teaching_class_id` bigint        NULL,
  `question`          varchar(500)  NOT NULL,
  `answer`            text          NULL,
  `citations_json`    json          NULL COMMENT '引用：[{docName,sectionNo}]',
  `hit_count`         int           NOT NULL DEFAULT 0,
  `top_score`         decimal(8,4)  NULL COMMENT '最高相关度',
  `refused`           tinyint(1)    NOT NULL DEFAULT 0 COMMENT '1=无依据拒答',
  `status`            varchar(20)   NOT NULL DEFAULT 'OK' COMMENT 'OK / REFUSAL / FALLBACK',
  `source`            varchar(20)   NULL COMMENT 'LLM / TEMPLATE',
  `tokens`            int           NULL,
  `cost`              decimal(10,6) NULL,
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`, `create_time`),
  KEY `idx_course` (`course_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 课程答疑记录';
