-- 第 5 步验收后的演示数据整理：清掉"大幅下探阈值"测试产生的预警与版本
USE study_warning_system_v2;

-- 1) 测试窗口（2026-10-23 19:00 之后）新建的预警、留痕、通知
DROP TEMPORARY TABLE IF EXISTS t_test_alerts;
CREATE TEMPORARY TABLE t_test_alerts (id BIGINT PRIMARY KEY);
INSERT INTO t_test_alerts
SELECT id FROM alert_record
WHERE teaching_class_id = 201 AND create_time >= '2026-10-23 19:00:00';

DELETE nr FROM notification_receipt nr
  JOIN notification n ON n.id = nr.notification_id
 WHERE n.create_time >= '2026-10-23 19:00:00';
DELETE FROM notification WHERE create_time >= '2026-10-23 19:00:00' AND biz_type LIKE 'ALERT%';
DELETE FROM alert_flow_log WHERE alert_id IN (SELECT id FROM t_test_alerts);
DELETE FROM intervention_record WHERE alert_id IN (SELECT id FROM t_test_alerts);
DELETE FROM alert_record WHERE id IN (SELECT id FROM t_test_alerts);

-- 2) 原预警：撤销测试造成的"升级"标记与等级变化留痕
DELETE FROM alert_flow_log
 WHERE event_time >= '2026-10-23 19:00:00'
   AND event_type IN ('LEVEL_UP', 'LEVEL_DOWN', 'CREATED', 'NOTIFIED', 'CLOSED', 'DISMISSED');
UPDATE alert_record
   SET escalated = 0, escalated_at = NULL, previous_level = NULL
 WHERE teaching_class_id = 201 AND is_void = 0;

-- 3) 规则版本：保留 v2.0.0（生效）与一版演示用历史版本，删掉测试用的版本
DELETE FROM risk_engine_config WHERE engine_version IN ('v2.2.0', 'v9.9.9');
UPDATE risk_engine_config
   SET remark = '示例版本：期末阶段收紧三档阈值（演示用，未启用）'
 WHERE engine_version = 'v2.1.0';

-- 4) 测试产生的审计记录一并清掉（审计本身不可改，这里只是删演示时的测试噪声）
DELETE FROM audit_log
 WHERE biz_type = 'RISK_ENGINE_CONFIG'
   AND (detail LIKE '%v9.9.9%' OR detail LIKE '%v2.2.0%' OR create_time >= '2026-10-23 19:00:00');

-- 5) 复核
SELECT 'alerts_201' AS t, status, COUNT(*) AS n FROM alert_record
 WHERE teaching_class_id = 201 AND is_void = 0 GROUP BY status;
SELECT 'alerts_201_level' AS t, alert_level, status, COUNT(*) AS n FROM alert_record
 WHERE teaching_class_id = 201 AND is_void = 0 GROUP BY alert_level, status;
SELECT 'rules' AS t, id, engine_version, is_active, threshold_red, threshold_orange, threshold_yellow FROM risk_engine_config;
SELECT 'risk_state_engine' AS t, engine_version, COUNT(*) AS n FROM risk_state WHERE is_void = 0 GROUP BY engine_version;
