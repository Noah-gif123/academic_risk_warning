-- v3.0 报告快照收口（阶段1）
-- ai_report 本身就是“学生 × 课程 × 学期”的一行快照；这里补六维指标、版本与过期标记。
-- ai_group_report 同步补指标/过期字段，保持群体报告契约一致。
ALTER TABLE ai_report
    ADD COLUMN metrics_json JSON NULL COMMENT '六维指标快照：学生值/班级均值',
    ADD COLUMN report_version INT NOT NULL DEFAULT 1 COMMENT '报告版本，每次重新生成递增',
    ADD COLUMN stale TINYINT(1) NOT NULL DEFAULT 0 COMMENT '源数据变更后标记过期',
    ADD COLUMN stale_reason VARCHAR(300) NULL,
    ADD COLUMN snapshot_at DATETIME NULL COMMENT '事实/指标快照时间';

ALTER TABLE ai_group_report
    ADD COLUMN metrics_json JSON NULL COMMENT '报告指标快照',
    ADD COLUMN stale TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN stale_reason VARCHAR(300) NULL,
    ADD COLUMN snapshot_at DATETIME NULL;
