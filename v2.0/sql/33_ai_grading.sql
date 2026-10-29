-- =============================================================================
-- 33_ai_grading.sql  第三阶段 M15：AI 批改辅助（建议分 + 评语，教师确认才生效）
--   设计：① AI **只给建议**，不改分数；教师点「采纳」后仍走原来的批改接口写分，责任链不变；
--         ② 一条主观题答案一条建议（uk_answer 保证重生成时覆盖而不是堆历史）；
--         ③ 教师最终采纳的分与评语单独存 final_*，用来对照"AI 建议 → 教师定稿"的差异；
--         ④ checks_json 记录三道校验：分数范围 / 评语不编数字 / 参考要点是否存在。
-- =============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_grading_suggest` (
  `id`                bigint       NOT NULL AUTO_INCREMENT,
  `submission_id`     bigint       NOT NULL COMMENT '提交记录ID',
  `answer_id`         bigint       NOT NULL COMMENT 'submission_answer.id（一条答案一条建议）',
  `question_id`       bigint       NOT NULL COMMENT '题目ID',
  `teaching_class_id` bigint       NOT NULL,
  `q_type`            varchar(20)  NOT NULL COMMENT 'SHORT / BLANK',
  `full_score`        decimal(6,2) NOT NULL COMMENT '本题满分',
  `student_answer`    text         NULL COMMENT '学生作答（快照，便于事后复盘）',
  `ref_answer`        text         NULL COMMENT '参考要点快照',
  `suggested_score`   decimal(6,2) NOT NULL COMMENT 'AI 建议分',
  `comment`           varchar(500) NULL COMMENT 'AI 评语',
  `rubric_json`       json         NULL COMMENT '模型给的评分要点（可展示给教师）',
  `checks_json`       json         NULL COMMENT '三道校验结果：scoreRange / commentDigits / refExists',
  `source`            varchar(20)  NOT NULL DEFAULT 'LLM' COMMENT 'LLM / TEMPLATE',
  `model`             varchar(50)  NULL,
  `prompt_version`    varchar(20)  NULL,
  `run_id`            bigint       NULL,
  `tokens`            int          NULL,
  `cost`              decimal(10,6) NULL,
  `status`            varchar(20)  NOT NULL DEFAULT 'SUGGESTED' COMMENT 'SUGGESTED / APPLIED / IGNORED',
  `final_score`       decimal(6,2) NULL COMMENT '教师最终采纳/修改后的分数',
  `final_comment`     varchar(500) NULL COMMENT '教师最终评语',
  `reviewer_id`       bigint       NULL,
  `review_time`       datetime     NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_answer` (`answer_id`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_tc_status` (`teaching_class_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 批改建议（不改分数，仅建议）';
