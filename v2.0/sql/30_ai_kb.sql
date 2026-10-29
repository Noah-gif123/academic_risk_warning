-- =============================================================================
-- 30_ai_kb.sql  第三阶段 M11：课程知识库（RAG 语料与检索）
--   设计：先落"分块表 + ngram 全文索引"，用 MySQL 全文检索做首版 RAG 的 R（检索）；
--         后续接入向量库时，只替换检索实现，分块与引用（doc_name + section_no）不变。
--   语料来源：课程导学（course_guide，HTML→纯文本）、课程资料（resource_file，txt）
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 知识库文档：一条来源一行，记录入库状态与分块数（幂等靠 hash）
CREATE TABLE IF NOT EXISTS `ai_kb_doc` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `course_id`     bigint       NOT NULL,
  `offering_id`   bigint       NULL,
  `source_type`   varchar(20)  NOT NULL COMMENT 'GUIDE 导学 / RESOURCE 资料 / QUESTION 题目',
  `source_id`     bigint       NOT NULL COMMENT '来源表主键',
  `doc_name`      varchar(200) NOT NULL COMMENT '引用时展示的名称',
  `content_hash`  varchar(64)  NOT NULL COMMENT '来源内容 sha256，用于幂等重入',
  `char_count`    int          NOT NULL DEFAULT 0,
  `chunk_count`   int          NOT NULL DEFAULT 0,
  `status`        varchar(20)  NOT NULL DEFAULT 'READY' COMMENT 'READY / FAILED',
  `error_message` varchar(300) NULL,
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`course_id`, `source_type`, `source_id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档';

-- 2) 知识库分块：检索与引用的最小单位（section_no 用于"第几段"引用）
CREATE TABLE IF NOT EXISTS `ai_kb_chunk` (
  `id`                bigint       NOT NULL AUTO_INCREMENT,
  `course_id`         bigint       NOT NULL,
  `offering_id`       bigint       NULL,
  `teaching_class_id` bigint       NULL,
  `source_type`       varchar(20)  NOT NULL,
  `source_id`         bigint       NOT NULL,
  `doc_name`          varchar(200) NOT NULL,
  `section_no`        int          NOT NULL COMMENT '文档内第几段（从 1 开始）',
  `content`           text         NOT NULL,
  `char_len`          int          NOT NULL DEFAULT 0,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_source` (`course_id`, `source_type`, `source_id`),
  FULLTEXT KEY `ft_content` (`content`) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库分块（ngram 全文索引）';
