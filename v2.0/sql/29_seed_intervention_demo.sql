-- =============================================================================
-- 29_seed_intervention_demo.sql  M10 演示数据：补齐"含回访结果"的干预记录
-- 幂等：只对还没有干预记录的预警补数据；重复执行不会重复插入
-- 口径：rn<=6 有效(6)、rn<=8 无效(2)、其余待回访(2)；前 3 条同时把预警关闭
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 干预记录（10 条，按等级优先取红灯）
INSERT INTO `intervention_record`
(`student_id`, `term_id`, `alert_id`, `stage`, `operator_id`, `operator_role`, `action_type`,
 `content`, `result`, `effective`, `effect_check_time`,
 `risk_score_before`, `risk_score_after`, `risk_score_change`, `create_time`, `create_by`)
SELECT t.student_id,
       t.term_id,
       t.id,
       'FIRST',
       1,
       'COUNSELOR',
       CASE WHEN t.rn % 4 = 0 THEN 'MEETING' WHEN t.rn % 4 = 2 THEN 'PARENT' WHEN t.rn % 4 = 3 THEN 'OTHER' ELSE 'TALK' END,
       CONCAT('约谈学生，核对缺勤与作业情况，约定两周改善目标（第 ', t.rn, ' 例）'),
       CASE WHEN t.rn <= 6 THEN '回访：出勤恢复、作业补齐，风险下降'
            WHEN t.rn <= 8 THEN '回访：仍未改善，已上报教务'
            ELSE '尚未回访' END,
       CASE WHEN t.rn <= 6 THEN 'EFFECTIVE' WHEN t.rn <= 8 THEN 'INEFFECTIVE' ELSE 'PENDING' END,
       CASE WHEN t.rn <= 8 THEN DATE_ADD(t.create_time, INTERVAL 14 DAY) ELSE NULL END,
       t.risk_score,
       CASE WHEN t.rn <= 6 THEN GREATEST(COALESCE(t.risk_score, 60) - 8.5, 0)
            WHEN t.rn <= 8 THEN LEAST(COALESCE(t.risk_score, 60) + 1.5, 100)
            ELSE NULL END,
       CASE WHEN t.rn <= 6 THEN -8.5 WHEN t.rn <= 8 THEN 1.5 ELSE NULL END,
       DATE_ADD(t.create_time, INTERVAL 2 DAY),
       1
FROM (
    SELECT ar.*, ROW_NUMBER() OVER (ORDER BY FIELD(ar.alert_level, 'RED', 'YELLOW', 'GREEN'), ar.id) AS rn
    FROM `alert_record` ar
    WHERE ar.is_void = 0
      AND ar.status <> 'CLOSED'
      AND NOT EXISTS (SELECT 1 FROM `intervention_record` ir WHERE ir.alert_id = ar.id)
) t
WHERE t.rn <= 10;

-- 2) 前 3 条把预警关闭（形成"干预→关闭"的完整轨迹）
UPDATE `alert_record` ar
    JOIN (SELECT ir.alert_id, ir.create_time
          FROM `intervention_record` ir
          ORDER BY ir.id LIMIT 3) x ON x.alert_id = ar.id
SET ar.status = 'CLOSED',
    ar.close_time = DATE_ADD(x.create_time, INTERVAL 14 DAY),
    ar.close_note = '干预后风险下降，回访确认关闭'
WHERE ar.status <> 'CLOSED';

-- 3) 对应的流转日志（CLOSED），便于时间线展示
INSERT INTO `alert_flow_log`
(`alert_id`, `student_id`, `term_id`, `event_type`, `from_level`, `to_level`, `from_status`, `to_status`,
 `detail`, `operator_id`, `operator_role`, `event_time`, `create_time`)
SELECT ar.id, ar.student_id, ar.term_id, 'CLOSED', ar.alert_level, ar.alert_level, 'HANDLING', 'CLOSED',
       '干预后回访确认关闭（演示数据）', 1, 'COUNSELOR', ar.close_time, NOW()
FROM `alert_record` ar
WHERE ar.status = 'CLOSED'
  AND ar.is_void = 0
  AND NOT EXISTS (SELECT 1 FROM `alert_flow_log` l WHERE l.alert_id = ar.id AND l.event_type = 'CLOSED');
