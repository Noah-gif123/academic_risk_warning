-- =====================================================================
-- 39_archive_old_reports.sql —— A5：旧报告清理
--
-- 背景：报告在 M16 口径改造前用"当前总分"表述；分项没录齐时那句话是错的
--       （当时把已加权得分当成总评说，实际应以过程分为主口径）。
--       另外演示与联调反复生成，同一门课堆了几十份，列表里噪声很大。
--
-- 原则：只归档、不删除。status 改成 ARCHIVED 后，学生端/教师端列表查不到
--       （列表只取 PUBLISHED），但数据仍在，审计与对比可用。
--
-- 幂等：重复执行不会重复归档（只处理 PUBLISHED 的行）。
-- =====================================================================

-- 1) 旧口径措辞：正文里出现"当前总分"的报告一律归档
UPDATE ai_report
SET status = 'ARCHIVED'
WHERE status = 'PUBLISHED'
  AND (content_json LIKE '%当前总分%' OR summary LIKE '%当前总分%');

SELECT '旧措辞已归档' AS step, ROW_COUNT() AS rows_affected;

-- 2) 重复报告：同一学生、同一选课、同一类型只保留最新一份
UPDATE ai_report r
    JOIN (SELECT student_id, enrollment_id, report_type, MAX(id) AS keep_id
          FROM ai_report
          WHERE status = 'PUBLISHED'
          GROUP BY student_id, enrollment_id, report_type) k
    ON r.student_id = k.student_id
        AND r.enrollment_id = k.enrollment_id
        AND r.report_type = k.report_type
SET r.status = 'ARCHIVED'
WHERE r.status = 'PUBLISHED'
  AND r.id <> k.keep_id;

SELECT '重复报告已归档' AS step, ROW_COUNT() AS rows_affected;

-- 3) 解释卡：同一预警只保留最新一张（旧卡片口径可能已变）
UPDATE ai_alert_explain e
    JOIN (SELECT alert_id, MAX(id) AS keep_id
          FROM ai_alert_explain
          WHERE status = 'PUBLISHED'
          GROUP BY alert_id) k
    ON e.alert_id = k.alert_id
SET e.status = 'ARCHIVED'
WHERE e.status = 'PUBLISHED'
  AND e.id <> k.keep_id;

SELECT '解释卡已归档' AS step, ROW_COUNT() AS rows_affected;

-- 4) 群体报告：同一对象（教学班/行政班）只保留最新一份
UPDATE ai_group_report g
    JOIN (SELECT scope, target_id, MAX(id) AS keep_id
          FROM ai_group_report
          WHERE status = 'PUBLISHED'
          GROUP BY scope, target_id) k
    ON g.scope = k.scope AND g.target_id = k.target_id
SET g.status = 'ARCHIVED'
WHERE g.status = 'PUBLISHED'
  AND g.id <> k.keep_id;

SELECT '群体报告已归档' AS step, ROW_COUNT() AS rows_affected;

-- 5) 结果核对
SELECT 'ai_report' AS t, status, COUNT(*) AS n FROM ai_report GROUP BY status
UNION ALL
SELECT 'ai_alert_explain', status, COUNT(*) FROM ai_alert_explain GROUP BY status
UNION ALL
SELECT 'ai_group_report', status, COUNT(*) FROM ai_group_report GROUP BY status;
