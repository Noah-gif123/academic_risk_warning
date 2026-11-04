-- 42_seed_grading_demo_paper.sql
-- 目的：为「AI 批改 + AI 推荐得分」准备一份完整试卷演示（教师 1001 / 学生 20250001）
--   6 题 100 分：3 道客观题（自动判分）+ 3 道主观题（AI 给建议分，教师定稿）
-- 幂等：每次执行先清掉旧卷（含其提交与 AI 建议），再重建题目；学生交卷由 demo_grading_full.ps1 走接口完成
-- 注意：publisher_id 用 sys_account.id（1001 → 1431），与既有 paper 2011 一致

-- ---------- 清理旧数据 ----------
delete from ai_grading_suggest
where submission_id in (select id from submission where paper_id = 20130);
delete sa from submission_answer sa
    join submission s on s.id = sa.submission_id
where s.paper_id = 20130;
delete from submission where paper_id = 20130;
delete from question where paper_id = 20130;
delete from paper where id = 20130;

-- ---------- 试卷 ----------
insert into paper (id, teaching_class_id, offering_id, term_id, paper_type, paper_name,
                   start_time, end_time, duration_minutes, full_score,
                   publisher_role, publisher_id, allow_view_paper, show_score, show_answer,
                   status, remark, create_by, create_time, update_by, update_time)
values (20130, 201, 101, 5, 'QUIZ', 'AI 批改演示卷 · 计算机基础综合（客观题 + 主观题）',
        date_sub(now(), interval 1 day), date_add(now(), interval 30 day), 45, 100.00,
        'TEACHER', 1431, 1, 1, 1,
        'PUBLISHED', '答辩演示用：客观题自动判分，主观题由 AI 给建议分、教师定稿', 1431, now(), 1431, now());

-- ---------- 题目（3 客观 + 3 主观） ----------
insert into question (paper_id, q_no, q_type, stem, options_json, answer_json, analysis,
                      full_score, group_name, sort_no, create_time, update_time)
values
(20130, 1, 'SINGLE', '下列关于存储容量单位的说法，正确的是：',
 '[{"key":"A","text":"1 KB = 1000 B"},{"key":"B","text":"1 KB = 1024 B"},{"key":"C","text":"1 MB = 1000 KB"},{"key":"D","text":"1 GB = 1000 B"}]',
 '{"keys":["B"]}',
 '存储容量按 2 的幂换算：1 KB = 1024 B、1 MB = 1024 KB、1 GB = 1024 MB。',
 10.00, '计算机基础', 1, now(), now()),

(20130, 2, 'SINGLE', '冯·诺依曼体系结构中，负责指令译码并发出控制信号的部件是：',
 '[{"key":"A","text":"运算器"},{"key":"B","text":"控制器"},{"key":"C","text":"存储器"},{"key":"D","text":"输入设备"}]',
 '{"keys":["B"]}',
 '控制器负责取指令、译码并发出控制信号；运算器负责算术与逻辑运算。',
 10.00, '计算机基础', 2, now(), now()),

(20130, 3, 'MULTI', '下列属于课程「过程性考核」的有（多选）：',
 '[{"key":"A","text":"课堂测试"},{"key":"B","text":"课后作业"},{"key":"C","text":"期末考试"},{"key":"D","text":"三级项目"}]',
 '{"keys":["A","B","D"]}',
 '过程性考核指平时环节：课堂测试、课后作业、三级项目；期末考试属于终结性考核。',
 10.00, '计算机基础', 3, now(), now()),

(20130, 4, 'SHORT',
 '请说明计算机内部为什么要用二进制表示数据，并各举一个「二进制适合表示」与「不适合直接表示」的例子。',
 null,
 json_object(
   'ref', '评分维度（25 分）：①二进制在物理上易实现、运算规则简单（8 分）；②与逻辑运算/布尔的对应关系（7 分）；③「适合」与「不适合」两个例子各 3 分，须说明原因（6 分）；④结论：二进制是工程折中，需配合编码与精度方案（4 分）。',
   'rubric', json_array(
     '①物理可实现性与运算简单性（8 分）',
     '②与逻辑运算/布尔的对应（7 分）',
     '③正例与反例各举一个并说明原因（6 分）',
     '④工程折中的结论与注意事项（4 分）'
   )
 ),
 '按四个维度打分；只给结论不给原因，最多得 8 分；举不出例子扣 6 分。',
 25.00, '简答题', 4, now(), now()),

(20130, 5, 'SHORT',
 '请简述冯·诺依曼体系结构的五个组成部分与「存储程序」的工作流程，并说明它的一个主要局限。',
 null,
 json_object(
   'ref', '评分维度（25 分）：①五部分：运算器、控制器、存储器、输入设备、输出设备（7 分）；②存储程序流程：程序与数据同存内存，按地址取指—译码—执行—写回（8 分）；③举例说明取指执行或 PC 的作用（6 分）；④主要局限：以运算器为中心的总线瓶颈或顺序执行限制并行（4 分）。',
   'rubric', json_array(
     '①五部分齐全且名称准确（7 分）',
     '②存储程序与取指执行流程（8 分）',
     '③举例说明指令数据同存或 PC 的作用（6 分）',
     '④主要局限（总线瓶颈/顺序执行）（4 分）'
   )
 ),
 '五部分每缺一项扣 1.5 分；未提「存储程序」扣 8 分；说不出局限扣 4 分。',
 25.00, '简答题', 5, now(), now()),

(20130, 6, 'SHORT',
 '结合你本人的学习情况，说明课程「过程性考核」中你认为最容易被忽视的一项，并给出可执行的改进措施。',
 null,
 json_object(
   'ref', '评分维度（20 分）：①明确指出被忽视的考核项，且属于过程性考核范畴（6 分）；②说明为什么容易被忽视（结合自身情况，讲清原因）（8 分）；③给出可执行的改进措施（有频次、有做法、可检查）（6 分）。',
   'rubric', json_array(
     '①指出具体的被忽视项（6 分）',
     '②结合自身说明原因（8 分）',
     '③改进措施可执行、可检查（6 分）'
   )
 ),
 '开放题：只写「会认真对待」这类表态，最多得 6 分；没有具体做法的扣 6 分。',
 20.00, '简答题', 6, now(), now());
