-- =============================================================================
-- 28_ai_alert_explain.sql  第三阶段 M9：预警解释卡 + 干预建议包
-- 说明：一条预警可以反复生成（保留历史），页面取最新一条
-- =============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_alert_explain` (
  `id`                bigint        NOT NULL AUTO_INCREMENT,
  `alert_id`          bigint        NOT NULL COMMENT '预警事件ID',
  `enrollment_id`     bigint        NOT NULL,
  `student_id`        bigint        NOT NULL,
  `teaching_class_id` bigint        NULL,
  `course_id`         bigint        NULL,
  `term_id`           bigint        NULL,
  `alert_level`       varchar(10)   NULL COMMENT '生成时的预警等级快照',
  `trigger_type`      varchar(20)   NOT NULL COMMENT 'ABSENCE/HOMEWORK/PROJECT/SCORE/MULTI/DATA',
  `summary`           varchar(300)  NULL COMMENT '一句话结论',
  `fact_json`         json          NULL COMMENT '事实快照（可复算）',
  `content_json`      json          NULL COMMENT '段落 + 干预动作',
  `status`            varchar(20)   NOT NULL DEFAULT 'PUBLISHED',
  `source`            varchar(20)   NOT NULL DEFAULT 'TEMPLATE' COMMENT 'DIFY / LLM / TEMPLATE',
  `model`             varchar(50)   NULL,
  `app_id`            varchar(64)   NULL,
  `prompt_version`    varchar(20)   NULL,
  `run_id`            bigint        NULL,
  `tokens`            int           NULL,
  `cost`              decimal(10,6) NULL,
  `create_by`         bigint        NULL,
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert` (`alert_id`, `create_time`),
  KEY `idx_student` (`student_id`, `create_time`),
  KEY `idx_class` (`teaching_class_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 预警解释卡';
