-- =====================================================================
-- 37_kb_retrieval_log.sql —— RAG 混合检索的可观测字段（R2/R4 用）
--
-- 目的：让「这次问答到底走了哪条检索路、每路各排第几、融合分多少」可回溯，
--       这是做三行消融对比（全文 / 向量 / 混合）与 recall@k 统计的前提。
--
-- 幂等：重复执行不报错。
-- =====================================================================

-- 1) 本次使用的检索模式：FULLTEXT / VECTOR / HYBRID
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_qa_log' AND COLUMN_NAME = 'retrieval_mode');
SET @s := IF(@c = 0,
  'ALTER TABLE ai_qa_log ADD COLUMN retrieval_mode varchar(16) NULL COMMENT ''检索模式：FULLTEXT/VECTOR/HYBRID'' AFTER refused',
  'SELECT ''retrieval_mode 已存在'' AS msg');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2) 检索诊断：每路命中数、top 分、名次、融合分、耗时、降级原因
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_qa_log' AND COLUMN_NAME = 'retrieval_json');
SET @s := IF(@c = 0,
  'ALTER TABLE ai_qa_log ADD COLUMN retrieval_json json NULL COMMENT ''检索诊断：各路命中/名次/融合分/耗时'' AFTER retrieval_mode',
  'SELECT ''retrieval_json 已存在'' AS msg');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3) 评测集与评测结果（R4）：问题 → 标准答案所在文档，用于算 recall@k / MRR / nDCG
CREATE TABLE IF NOT EXISTS ai_rag_eval_case (
  id            bigint       NOT NULL AUTO_INCREMENT,
  course_id     bigint       NOT NULL,
  question      varchar(500) NOT NULL,
  answerable    tinyint(1)   NOT NULL DEFAULT 1 COMMENT '1=可答（有标准出处），0=不可答（应拒答）',
  gold_doc      varchar(200) NULL     COMMENT '标准答案所在文档名（answerable=1 必填）',
  gold_section  int          NULL     COMMENT '标准答案所在段号（可选，段落级 recall 用）',
  gold_keywords varchar(500) NULL     COMMENT '答案必须命中的关键词，用 | 分隔（引用/正确性校验用）',
  note          varchar(200) NULL,
  create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rag_eval_course (course_id, answerable)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 检索评测集';

CREATE TABLE IF NOT EXISTS ai_rag_eval_run (
  id           bigint      NOT NULL AUTO_INCREMENT,
  mode         varchar(16) NOT NULL COMMENT 'FULLTEXT / VECTOR / HYBRID',
  k            int         NOT NULL DEFAULT 4,
  cases        int         NOT NULL DEFAULT 0,
  recall_at_k  decimal(6,4) NULL,
  mrr          decimal(6,4) NULL,
  ndcg_at_k    decimal(6,4) NULL,
  refuse_acc   decimal(6,4) NULL COMMENT '不可答问题被正确拒答的比例',
  false_refuse decimal(6,4) NULL COMMENT '可答问题被误拒的比例',
  avg_ms       int         NULL,
  detail_json  json        NULL,
  create_time  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rag_eval_mode (mode, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 检索评测结果（三行对比表的数据源）';
