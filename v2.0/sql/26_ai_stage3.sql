-- =============================================================================
-- 26_ai_stage3.sql  第三阶段 AI 基础表（幂等，可重复执行）
-- 说明：不引入知识点；AI 产物全部走"草稿/报告"独立表，不改任何既有表结构
-- 依赖：05 / 06（成绩与预警相关表已存在）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. ai_run  一次 AI 任务（出题 / 报告 / 批改辅助 / 问答）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_run` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '运行ID',
  `scene`          varchar(30)  NOT NULL COMMENT '场景：REPORT / QUESTION_GEN / DRAFT_GRADE / QA',
  `target_type`    varchar(30)  NULL     COMMENT '对象类型：ENROLLMENT / TEACHING_CLASS / RESOURCE',
  `target_id`      bigint       NULL     COMMENT '对象ID',
  `status`         varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / RUNNING / SUCCESS / FAILED / FALLBACK',
  `source`         varchar(20)  NULL     COMMENT 'DIFY / TEMPLATE',
  `model`          varchar(50)  NULL     COMMENT '模型名',
  `app_id`         varchar(64)  NULL     COMMENT 'Dify 应用ID',
  `prompt_version` varchar(20)  NULL     COMMENT '提示词/工作流版本',
  `total_tokens`   int          NULL     COMMENT '总 token',
  `total_cost`     decimal(10,6) NULL    COMMENT '估算费用（元）',
  `operator_id`    bigint       NULL     COMMENT '触发人',
  `error_message`  varchar(500) NULL     COMMENT '失败原因',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time`    datetime     NULL,
  PRIMARY KEY (`id`),
  KEY `idx_scene_time` (`scene`, `create_time`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 运行主记录';

-- -----------------------------------------------------------------------------
-- 2. ai_run_step  步级明细（审计与排查）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_run_step` (
  `id`                bigint       NOT NULL AUTO_INCREMENT,
  `run_id`            bigint       NOT NULL,
  `step_no`           int          NOT NULL DEFAULT 1,
  `name`              varchar(50)  NOT NULL COMMENT '步骤名',
  `input_digest`      varchar(500) NULL     COMMENT '输入摘要',
  `output_digest`     varchar(500) NULL     COMMENT '输出摘要',
  `validation_status` varchar(20)  NULL     COMMENT 'PASS / WARN / FAIL',
  `validation_detail` varchar(500) NULL,
  `duration_ms`       int          NULL,
  `error`             varchar(500) NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run` (`run_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 步级明细';

-- -----------------------------------------------------------------------------
-- 3. ai_usage  计费与限流
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_usage` (
  `id`                bigint        NOT NULL AUTO_INCREMENT,
  `run_id`            bigint        NULL,
  `scene`             varchar(30)   NOT NULL,
  `model`             varchar(50)   NULL,
  `prompt_tokens`     int           NULL,
  `completion_tokens` int           NULL,
  `cost`              decimal(10,6) NULL,
  `operator_id`       bigint        NULL,
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene_day` (`scene`, `create_time`),
  KEY `idx_operator` (`operator_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 调用计费';

-- -----------------------------------------------------------------------------
-- 4. ai_report  学习评价报告（本期先做学生端个人报告）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_report` (
  `id`                bigint        NOT NULL AUTO_INCREMENT,
  `student_id`        bigint        NOT NULL,
  `enrollment_id`     bigint        NOT NULL,
  `teaching_class_id` bigint        NOT NULL,
  `offering_id`       bigint        NOT NULL,
  `course_id`         bigint        NOT NULL,
  `term_id`           bigint        NOT NULL,
  `report_type`       varchar(20)   NOT NULL DEFAULT 'STAGE' COMMENT 'STAGE / ALERT / MANUAL',
  `scope`             varchar(20)   NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT / CLASS / ADMIN_CLASS',
  `period_start`      date          NULL,
  `period_end`        date          NULL,
  `risk_level`        varchar(10)   NULL COMMENT '生成时的预警等级快照',
  `total_score`       decimal(6,2)  NULL,
  `fact_json`         json          NULL COMMENT '生成所用事实（可复算）',
  `content_json`      json          NULL COMMENT '正文段落 + 动作按钮',
  `summary`           varchar(300)  NULL COMMENT '一句话结论',
  `status`            varchar(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT / PUBLISHED',
  `source`            varchar(20)   NOT NULL DEFAULT 'TEMPLATE' COMMENT 'DIFY / TEMPLATE',
  `model`             varchar(50)   NULL,
  `app_id`            varchar(64)   NULL,
  `prompt_version`    varchar(20)   NULL,
  `run_id`            bigint        NULL,
  `tokens`            int           NULL,
  `cost`              decimal(10,6) NULL,
  `read_time`         datetime      NULL,
  `publish_time`      datetime      NULL,
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`, `create_time`),
  KEY `idx_enrollment` (`enrollment_id`, `report_type`, `create_time`),
  KEY `idx_class` (`teaching_class_id`, `scope`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 学习评价报告';

-- -----------------------------------------------------------------------------
-- 5. ai_report_feedback  报告反馈
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_report_feedback` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `report_id`   bigint       NOT NULL,
  `student_id`  bigint       NOT NULL,
  `useful`      tinyint(1)   NOT NULL DEFAULT 1,
  `reason`      varchar(300) NULL,
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report` (`report_id`),
  KEY `idx_student` (`student_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 报告反馈';

-- -----------------------------------------------------------------------------
-- 6. ai_prompt_template  提示词 / 工作流版本登记
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
  `id`               bigint       NOT NULL AUTO_INCREMENT,
  `code`             varchar(40)  NOT NULL COMMENT '如 REPORT_STUDENT / QUESTION_GEN',
  `version`          varchar(20)  NOT NULL,
  `dify_app_id`      varchar(64)  NULL,
  `workflow_version` varchar(20)  NULL,
  `note`             varchar(300) NULL,
  `status`           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_version` (`code`, `version`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 提示词版本';

-- -----------------------------------------------------------------------------
-- 7. ai_question_draft  AI 出题草稿（本期先建表，M2 使用）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_question_draft` (
  `id`             bigint        NOT NULL AUTO_INCREMENT,
  `offering_id`    bigint        NOT NULL,
  `lesson_id`      bigint        NULL,
  `resource_ids`   varchar(300)  NULL COMMENT '来源资料ID，逗号分隔',
  `q_type`         varchar(20)   NOT NULL COMMENT 'SINGLE / MULTI / JUDGE / BLANK / SHORT',
  `difficulty`     varchar(10)   NULL COMMENT 'easy / medium / hard',
  `stem`           text          NOT NULL,
  `options_json`   json          NULL,
  `answer_json`    json          NULL,
  `analysis`       text          NULL,
  `group_name`     varchar(100)  NULL COMMENT '沿用试卷分组名，替代知识点',
  `source_pages`   varchar(100)  NULL COMMENT '依据页码，如 3,4',
  `quality_json`   json          NULL COMMENT '五道闸门结果',
  `full_score`     decimal(6,2)  NULL,
  `status`         varchar(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / APPROVED / REJECTED / ADOPTED',
  `model`          varchar(50)   NULL,
  `prompt_version` varchar(20)   NULL,
  `run_id`         bigint        NULL,
  `tokens`         int           NULL,
  `cost`           decimal(10,6) NULL,
  `create_by`      bigint        NULL,
  `reviewer_id`    bigint        NULL,
  `review_time`    datetime      NULL,
  `deleted`        tinyint(1)    NOT NULL DEFAULT 0,
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_offering_status` (`offering_id`, `status`),
  KEY `idx_lesson` (`lesson_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 出题草稿';
