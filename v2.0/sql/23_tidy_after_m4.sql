-- 第 4 步验收后的演示数据整理：把验收用的申诉文案改成像真实场景的一句话
USE study_warning_system_v2;

UPDATE alert_record
SET student_response = '校运会集训那几次缺课有体育部名单，请老师核实',
    student_response_time = '2026-10-23 09:12:00'
WHERE id = 3143;

UPDATE alert_flow_log
SET detail = '学生提交申诉：校运会集训那几次缺课有体育部名单，请老师核实',
    event_time = '2026-10-23 09:12:00'
WHERE alert_id = 3143 AND event_type = 'APPEAL';

UPDATE notification
SET content = '20250001 学生1 对ORANGE预警结论提出申诉：校运会集训那几次缺课有体育部名单，请老师核实。请在「预警监控」中核实并记录处理。'
WHERE biz_type = 'ALERT_APPEAL' AND biz_id = 3143;

SELECT id, alert_level, status, LEFT(student_response, 40) AS appeal FROM alert_record WHERE id = 3143;
SELECT COUNT(*) AS appeal_flows FROM alert_flow_log WHERE alert_id = 3143 AND event_type = 'APPEAL';
