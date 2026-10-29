-- =============================================================================
-- 34_seed_grading_demo.sql  第三阶段 M15 演示数据：一份"能看出 AI 批改有没有用"的简答卷
--   为什么要造：种子库里的简答题是占位题（"按课堂讲解要点给分"），学生答案也是模板句，
--              拿来演示 AI 批改看不出差异。这里给教学班 201 造一份真实口径的简答卷：
--                第1题 绝对/相对引用（5 分）—— 学生只答了一半（漏了"拖动不变"）
--                第2题 VLOOKUP #N/A 排查（5 分）—— 学生只答了一种原因
--              期望 AI 给"部分分 + 指出缺失要点"，教师采纳或改分后定稿。
--   幂等：先按试卷名删除旧卷、旧提交与旧建议，再插入。
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 清理旧数据（先删建议，再删提交，最后删卷与题）
DELETE g FROM `ai_grading_suggest` g
    JOIN `submission` s ON s.id = g.submission_id
    JOIN `paper` p ON p.id = s.paper_id
WHERE p.paper_name = 'AI 批改演示卷 · Excel 函数与图表';

DELETE sa FROM `submission_answer` sa
    JOIN `submission` s ON s.id = sa.submission_id
    JOIN `paper` p ON p.id = s.paper_id
WHERE p.paper_name = 'AI 批改演示卷 · Excel 函数与图表';

DELETE s FROM `submission` s
    JOIN `paper` p ON p.id = s.paper_id
WHERE p.paper_name = 'AI 批改演示卷 · Excel 函数与图表';

DELETE q FROM `question` q
    JOIN `paper` p ON p.id = q.paper_id
WHERE p.paper_name = 'AI 批改演示卷 · Excel 函数与图表';

DELETE FROM `paper` WHERE paper_name = 'AI 批改演示卷 · Excel 函数与图表';

-- 2) 试卷（已发布，直接可批改；不计入成绩构成，避免污染演示分数）
INSERT INTO `paper`
(`teaching_class_id`, `offering_id`, `term_id`, `paper_type`, `paper_name`, `end_time`, `duration_minutes`,
 `full_score`, `scheme_item_id`, `publisher_role`, `publisher_id`, `allow_view_paper`, `show_score`, `show_answer`,
 `status`, `remark`, `create_by`)
VALUES (201, 101, 5, 'QUIZ', 'AI 批改演示卷 · Excel 函数与图表', DATE_SUB(NOW(), INTERVAL 1 DAY), 20,
        10.00, NULL, 'TEACHER', 1, 1, 1, 'AFTER_MARK',
        'PUBLISHED', 'M15 批改辅助演示卷：两道简答，AI 给建议分，教师定稿', 1);

SET @paper_id = LAST_INSERT_ID();

-- 3) 两道简答题（参考答案写清楚要点，AI 才有据可依）
INSERT INTO `question` (`paper_id`, `q_no`, `q_type`, `stem`, `options_json`, `answer_json`, `analysis`,
                        `full_score`, `group_name`, `sort_no`)
VALUES
(@paper_id, 1, 'SHORT',
 '请说明公式中 A1 与 $A$1 的区别，并说明向右、向下拖动填充时两者分别会怎样变化。',
 NULL,
 '{"ref":"A1 是相对引用：拖动时行号和列标都会跟着变；$A$1 是绝对引用：拖动时行列都不变，按 F4 可快速切换；混合引用 A$1 / $A1 只锁定行或只锁定列。"}',
 '要点三条：相对引用会变、绝对引用不变、能用 F4 切换（混合引用可选）。',
 5.00, '简答题', 1),
(@paper_id, 2, 'SHORT',
 '用 VLOOKUP 查成绩时出现 #N/A，请写出两种常见原因，并说明各自怎么排查。',
 NULL,
 '{"ref":"① 查找值不在查找区域的第一列（返回列号从查找区域第 1 列起算，不是从 A 列起算）；② 学号带空格或格式不一致，可用 TRIM 清理、统一为文本后再查。"}',
 '要点两条原因 + 对应排查动作，每条 2.5 分。',
 5.00, '简答题', 2);

SET @q1 = (SELECT id FROM `question` WHERE paper_id = @paper_id AND q_no = 1);
SET @q2 = (SELECT id FROM `question` WHERE paper_id = @paper_id AND q_no = 2);

-- 4) 学生 20250001（student_id = 1）的提交：只答了一半，正好让 AI 给部分分
SET @enr = (SELECT id FROM `enrollment` WHERE teaching_class_id = 201 AND student_id = 1 LIMIT 1);

INSERT INTO `submission`
(`paper_id`, `teaching_class_id`, `term_id`, `student_id`, `enrollment_id`, `submit_status`, `start_time`,
 `submit_time`, `duration_seconds`, `objective_score`, `subjective_score`, `total_score`, `marked_status`,
 `is_absent`)
VALUES (@paper_id, 201, 5, 1, @enr, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 DAY),
        DATE_SUB(NOW(), INTERVAL 2 DAY), 900, 0.00, 0.00, NULL, 'SYSTEM_MARKED', 0);

SET @sub_id = LAST_INSERT_ID();

INSERT INTO `submission_answer` (`submission_id`, `question_id`, `answer_json`, `score`, `is_correct`,
                                 `comment`, `marked_by_type`, `marked_by`, `marked_at`)
VALUES
(@sub_id, @q1, '{"text":"A1 是相对引用，拖动的时候会跟着变；加了美元符号就是绝对引用。"}',
 NULL, NULL, NULL, NULL, NULL, NULL),
(@sub_id, @q2, '{"text":"查找值不在查找区域的第一列，所以查不到。"}',
 NULL, NULL, NULL, NULL, NULL, NULL);

SELECT s.id AS submission_id, p.id AS paper_id, p.paper_name, p.full_score,
       (SELECT count(*) FROM question q WHERE q.paper_id = p.id) AS question_count,
       (SELECT count(*) FROM submission_answer sa WHERE sa.submission_id = s.id) AS answer_count
FROM submission s
         JOIN paper p ON p.id = s.paper_id
WHERE p.paper_name = 'AI 批改演示卷 · Excel 函数与图表';
