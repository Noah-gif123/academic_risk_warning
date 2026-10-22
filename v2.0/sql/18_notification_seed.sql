-- ============================================================
-- 18_notification_seed.sql（已被 20_rebuild_demo.sql 取代：20 里已含四端消息，重复执行会重复插入）  四端消息中心演示数据
-- 说明：notification 为通知主体（聚合去重），notification_receipt 为“每个接收人 × 渠道”一行；
--       消息中心按 receipt(recipient_id, recipient_role, channel='IN_APP') 关联 notification 列表。
--       本脚本可重复执行：先按标题清理本脚本插入的演示通知，再重新写入。
-- 执行：mysql -uroot -proot study_warning_system_v2 < 18_notification_seed.sql
-- ============================================================
USE study_warning_system_v2;

-- 清理旧演示数据（仅限本脚本的标题，避免影响真实产生的通知）
DELETE FROM notification_receipt
 WHERE notification_id IN (
   SELECT id FROM notification WHERE title IN (
     '《计算机应用基础》期末总评已发布',
     '课程学情预警：缺课次数已达允许上限',
     '期中考勤已归档',
     '第 3 次课后作业有 12 份待批阅',
     '期中考勤归档窗口将于本周五关闭',
     '点名覆盖率偏低：CS-01 连续 2 周未点名',
     '班集体提醒（汇总）：计算机1班预警占比偏高',
     '重点关注学生：3 名学生风险课程数 ≥ 3',
     '上报结果：教务已受理 2 条上报',
     '归档待审：CS-01 期中考勤待审核',
     '入班申请待审：3 条申请待处理',
     '问卷待分发：教师风格问卷已提交 2 份'
   )
 );
DELETE FROM notification WHERE title IN (
     '《计算机应用基础》期末总评已发布',
     '课程学情预警：缺课次数已达允许上限',
     '期中考勤已归档',
     '第 3 次课后作业有 12 份待批阅',
     '期中考勤归档窗口将于本周五关闭',
     '点名覆盖率偏低：CS-01 连续 2 周未点名',
     '班集体提醒（汇总）：计算机1班预警占比偏高',
     '重点关注学生：3 名学生风险课程数 ≥ 3',
     '上报结果：教务已受理 2 条上报',
     '归档待审：CS-01 期中考勤待审核',
     '入班申请待审：3 条申请待处理',
     '问卷待分发：教师风格问卷已提交 2 份'
);

-- ---------------- 通知主体 ----------------
INSERT INTO notification (biz_type, biz_id, title, content, payload_json, aggregate_key, aggregate_count, audience_desc, priority, expire_time, create_by, create_time, update_time) VALUES
-- 学生
('SCORE_TASK', 1, '《计算机应用基础》期末总评已发布',
 '你的期末总评已发布：加权分 78.5、扣分 6.0、总评 72.5，高于及格线 60。可到“我的成绩”查看分项构成。',
 '{"route":"/student/scores","offeringId":1}', NULL, 1, '学生 20250001', 'NORMAL', DATE_ADD(NOW(), INTERVAL 7 DAY), 1442, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
('ALERT_NEW', 1, '课程学情预警：缺课次数已达允许上限',
 '你在《计算机应用基础》的缺课次数已达允许上限（10 次），再缺 1 次将触发“缺课超三分之一直接挂科”。请及时与任课教师联系。',
 '{"route":"/student/warnings","offeringId":1}', NULL, 1, '学生 20250001', 'HIGH', DATE_ADD(NOW(), INTERVAL 3 DAY), 1431, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),
('ARCHIVE_RESULT', 1, '期中考勤已归档',
 '本课程期中点名已由教务归档，归档后教师不可再改动；如需更正请联系教务处。',
 '{"route":"/student/attendance","offeringId":1}', NULL, 1, '学生 20250001', 'NORMAL', DATE_ADD(NOW(), INTERVAL 30 DAY), 1442, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
-- 教师
('SCORE_TASK', 1, '第 3 次课后作业有 12 份待批阅',
 'CS-01《计算机应用基础》第 3 次课后作业共 267 份，已提交 240 份，其中 12 份主观题待批阅。',
 '{"route":"/teacher/grading","offeringId":1}', NULL, 1, '教师 1001', 'HIGH', DATE_ADD(NOW(), INTERVAL 5 DAY), 1431, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW()),
('ARCHIVE_TASK', 1, '期中考勤归档窗口将于本周五关闭',
 'CS-01 期中考勤归档尚未提交，请在截止时间前完成，逾期需联系教务重开批次。',
 '{"route":"/teacher/attendance","offeringId":1}', NULL, 1, '教师 1001', 'HIGH', DATE_ADD(NOW(), INTERVAL 2 DAY), 1442, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
('COVERAGE_REMIND', 1, '点名覆盖率偏低：CS-01 连续 2 周未点名',
 'CS-01 最近两次课未点名，点名覆盖率低于三分之一会让风险判定置信度被判低。请在课后及时补录。',
 '{"route":"/teacher/attendance","offeringId":1}', NULL, 1, '教师 1001', 'NORMAL', DATE_ADD(NOW(), INTERVAL 7 DAY), 1442, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
-- 导员
('CLASS_ALERT', 1, '班集体提醒（汇总）：计算机1班预警占比偏高',
 '计算机1班（2025级）预警学生 14 人、占 5.2%，未达档位阈值；本周仅作个体提醒，无需召开班会。',
 '{"route":"/counselor/classes","adminClassId":1}', 'CLASS_ALERT:1440:5:8', 1, '导员 马思国', 'NORMAL', DATE_ADD(NOW(), INTERVAL 7 DAY), 1442, DATE_SUB(NOW(), INTERVAL 8 HOUR), NOW()),
('MULTI_COURSE', 1, '重点关注学生：3 名学生风险课程数 ≥ 3',
 '计算机1班有 3 名学生同时有 ≥3 门课程处于风险状态，建议优先约谈；名单见“重点关注的”页面。',
 '{"route":"/counselor/focus","adminClassId":1}', NULL, 1, '导员 马思国', 'HIGH', DATE_ADD(NOW(), INTERVAL 5 DAY), 1442, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
('REFER_RESULT', 1, '上报结果：教务已受理 2 条上报',
 '你上报的 2 条个案已进入教务处理，处理结果会在此回执中同步。',
 '{"route":"/counselor/interventions"}', NULL, 1, '导员 马思国', 'NORMAL', DATE_ADD(NOW(), INTERVAL 14 DAY), 1442, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
-- 教务
('ARCHIVE_TASK', 1, '归档待审：CS-01 期中考勤待审核',
 'CS-01 已提交期中考勤归档，覆盖课次 24 次、记录 6 408 条，点名覆盖率 100%，请审核。',
 '{"route":"/admin/attendance","archiveId":1}', NULL, 1, '教务处', 'HIGH', DATE_ADD(NOW(), INTERVAL 3 DAY), 1431, DATE_SUB(NOW(), INTERVAL 5 HOUR), NOW()),
('SYSTEM', 1, '入班申请待审：3 条申请待处理',
 '有 3 条学生入班申请等待处理，其中 2 条来自同一教学班。',
 '{"route":"/admin/workflow"}', NULL, 1, '教务处', 'NORMAL', DATE_ADD(NOW(), INTERVAL 7 DAY), 1431, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
('QUESTIONNAIRE', 1, '问卷待分发：教师风格问卷已提交 2 份',
 '导员已提交 2 份教师风格问卷，待教务分发至对应课程组。',
 '{"route":"/admin/workflow"}', NULL, 1, '教务处', 'NORMAL', DATE_ADD(NOW(), INTERVAL 10 DAY), 1440, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ---------------- 接收人回执（逐个接收人一行） ----------------
INSERT INTO notification_receipt (notification_id, recipient_id, recipient_role, channel, send_status, send_time, is_read, read_time, ack_required, ack_status, ack_time, ack_note, action_taken, create_time, update_time)
SELECT n.id, 631, 'STUDENT', 'IN_APP', 'SENT', n.create_time, IF(n.title = '《计算机应用基础》期末总评已发布', 1, 0),
       IF(n.title = '《计算机应用基础》期末总评已发布', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
       0, 'NONE', NULL, NULL, NULL, NOW(), NOW()
  FROM notification n
 WHERE n.audience_desc = '学生 20250001';

INSERT INTO notification_receipt (notification_id, recipient_id, recipient_role, channel, send_status, send_time, is_read, read_time, ack_required, ack_status, ack_time, ack_note, action_taken, create_time, update_time)
SELECT n.id, 1431, 'TEACHER', 'IN_APP', 'SENT', n.create_time, IF(n.title LIKE '点名覆盖率偏低%', 1, 0),
       IF(n.title LIKE '点名覆盖率偏低%', DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL),
       0, 'NONE', NULL, NULL, NULL, NOW(), NOW()
  FROM notification n
 WHERE n.audience_desc = '教师 1001';

INSERT INTO notification_receipt (notification_id, recipient_id, recipient_role, channel, send_status, send_time, is_read, read_time, ack_required, ack_status, ack_time, ack_note, action_taken, create_time, update_time)
SELECT n.id, 1440, 'COUNSELOR', 'IN_APP', 'SENT', n.create_time, IF(n.biz_type = 'REFER_RESULT', 1, 0),
       IF(n.biz_type = 'REFER_RESULT', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
       IF(n.biz_type = 'CLASS_ALERT', 1, 0),
       IF(n.biz_type = 'CLASS_ALERT', 'PENDING', 'NONE'), NULL, NULL, NULL, NOW(), NOW()
  FROM notification n
 WHERE n.audience_desc = '导员 马思国';

INSERT INTO notification_receipt (notification_id, recipient_id, recipient_role, channel, send_status, send_time, is_read, read_time, ack_required, ack_status, ack_time, ack_note, action_taken, create_time, update_time)
SELECT n.id, 1442, 'ADMIN', 'IN_APP', 'SENT', n.create_time, 0, NULL, 0, 'NONE', NULL, NULL, NULL, NOW(), NOW()
  FROM notification n
 WHERE n.audience_desc = '教务处';

-- ---------------- 校验 ----------------
SELECT recipient_role, COUNT(*) AS 消息数, SUM(is_read = 0) AS 未读数
  FROM notification_receipt
 GROUP BY recipient_role
 ORDER BY recipient_role;
