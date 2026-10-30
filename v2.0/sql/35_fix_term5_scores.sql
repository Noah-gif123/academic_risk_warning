-- =============================================================================
-- 35_fix_term5_scores.sql  数据口径修正：学期第 9 周，期末还没考，不能有期末成绩
--   问题：早期演示数据（tool/seed_demo.js 生成的 20_rebuild_demo.sql）按"整学期结束"造数，
--         把 期末考试（FINAL_EXAM）的手工分一次性录满 → 第 9 周人人都有期末成绩，
--         总评也把未考的期末算了进去，学习评价/预警口径失真。
--   处理：删掉本学期（term 5）期末考试的手工分项（score_item），保留课堂表现（平时分）、期中考试。
--         之后必须重跑一次判定（教务端「规则配置 → 重新判定」或 POST /api/engine/evaluate），
--         让 score_result / risk_state / alert_record 按"期末未考"口径重算（期末需考由引擎测算）。
--   幂等：可重复执行，删完为 0 行即已修完。
-- =============================================================================

SET NAMES utf8mb4;

SELECT '删除前：期末考试分项条数' AS stage,
       COUNT(*) AS rows_cnt,
       COUNT(DISTINCT s.enrollment_id) AS students
FROM `score_item` s
         JOIN `score_scheme_item` si ON si.id = s.scheme_item_id
WHERE si.category = 'FINAL'
  AND s.term_id = 5
  AND s.is_void = 0;

DELETE s
FROM `score_item` s
         JOIN `score_scheme_item` si ON si.id = s.scheme_item_id
WHERE si.category = 'FINAL'
  AND s.term_id = 5;

SELECT '删除后：期末考试分项条数' AS stage,
       COUNT(*) AS rows_cnt
FROM `score_item` s
         JOIN `score_scheme_item` si ON si.id = s.scheme_item_id
WHERE si.category = 'FINAL'
  AND s.term_id = 5
  AND s.is_void = 0;

-- 顺带核对：本学期还剩哪些手工分项（应当只有 课堂表现 / 期中考试）
SELECT si.item_code, si.item_name, si.category, COUNT(s.id) AS rows_cnt,
       COUNT(DISTINCT s.enrollment_id) AS students
FROM `score_item` s
         JOIN `score_scheme_item` si ON si.id = s.scheme_item_id
WHERE s.term_id = 5
  AND s.is_void = 0
GROUP BY si.id
ORDER BY rows_cnt DESC;
