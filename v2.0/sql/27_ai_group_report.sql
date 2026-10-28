-- =============================================================================
-- 27_ai_group_report.sql  第三阶段 M8：群体报告（教学班 / 行政班）
-- 说明：与 ai_report（个人报告）分表，避免给个人报告塞空的学生字段
-- =============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_group_report` (
  `id`                bigint        NOT NULL AUTO_INCREMENT,
  `scope`             varchar(20)   NOT NULL COMMENT 'CLASS=教学班报告 / ADMIN_CLASS=行政班报告',
  `target_id`         bigint        NOT NULL COMMENT '教学班ID 或 行政班ID',
  `teaching_class_id` bigint        NULL,
  `admin_class_id`    bigint        NULL,
  `offering_id`       bigint        NULL,
  `course_id`         bigint        NULL,
  `term_id`           bigint        NULL,
  `title`             varchar(120)  NOT NULL COMMENT '如「计基01班 · 班级学情报告」',
  `fact_json`         json          NULL COMMENT '生成所用事实（可复算）',
  `content_json`      json          NULL COMMENT '段落与动作',
  `summary`           varchar(300)  NULL COMMENT '一句话结论',
  `status`            varchar(20)   NOT NULL DEFAULT 'PUBLISHED',
  `source`            varchar(20)   NOT NULL DEFAULT 'TEMPLATE' COMMENT 'DIFY / LLM / TEMPLATE',
  `model`             varchar(50)   NULL,
  `app_id`            varchar(64)   NULL,
  `prompt_version`    varchar(20)   NULL,
  `run_id`            bigint        NULL,
  `tokens`            int           NULL,
  `cost`              decimal(10,6) NULL,
  `create_by`         bigint        NULL COMMENT '教师/辅导员ID',
  `publish_time`      datetime      NULL,
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scope_target` (`scope`, `target_id`, `create_time`),
  KEY `idx_class` (`teaching_class_id`, `create_time`),
  KEY `idx_admin_class` (`admin_class_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 群体学情报告';
