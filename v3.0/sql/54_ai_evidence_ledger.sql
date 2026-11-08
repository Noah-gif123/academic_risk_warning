-- =====================================================================
-- P2-6 EvidenceLedger 落库（v3.0）
-- 每次 Agent 运行维护数字台账：工具名 + 指标 + 值 + 来源步号。
-- Writer 生成答案后逐数字校验；未命中 → 重写一次 → 仍失败降级。
-- =====================================================================
CREATE TABLE IF NOT EXISTS `ai_evidence_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `run_key` varchar(64) NOT NULL COMMENT '运行标识',
  `step_no` int NOT NULL COMMENT '来源工具步号',
  `tool_name` varchar(64) NOT NULL COMMENT '工具名',
  `metric` varchar(64) NOT NULL COMMENT '证据指标名',
  `value_text` varchar(255) DEFAULT NULL COMMENT '原始值文本',
  `numeric_value` decimal(18,6) DEFAULT NULL COMMENT '数值（可解析时）',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源表/说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_key` (`run_key`),
  KEY `idx_run_metric` (`run_key`,`metric`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 证据台账（数字可追溯）';

SELECT table_name FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name='ai_evidence_ledger';
