-- 固定演示通知：2 条
SET NAMES utf8mb4;
INSERT INTO notification (biz_type, biz_id, title, content, aggregate_key, aggregate_count, audience_desc, priority, create_by)
VALUES
('ALERT_WEEKLY', NULL, '本周学情预警汇总', '计算机1班本周新增 3 名红色风险学生，请优先安排约谈。', 'demo-alert-weekly', 1, '辅导员 C001', 'HIGH', 1),
('RULE_PUBLISH', NULL, '学期预警规则已生效', '2026-2027-1 学期规则 v2.1.0 已生效，判定口径可在教务端查看。', 'demo-rule-v210', 1, '辅导员 C001', 'NORMAL', 1);

INSERT INTO notification_receipt (notification_id, recipient_id, recipient_role, channel, send_status, send_time, is_read)
SELECT id, 1, 'COUNSELOR', 'IN_APP', 'SENT', NOW(), 0 FROM notification;
