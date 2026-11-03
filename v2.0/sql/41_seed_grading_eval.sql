-- 41_seed_grading_eval.sql
-- 目的：为「AI 批改」准备一套可评价的样本（原演示数据里简答题作答是「答：补充说明。」这类占位文本、
--       参考要点是「按课堂讲解要点给分。」，而人工分却被种子脚本随机写成 32~44 分，
--       任何模型都只能判 0 分，导致无法判断批改质量）。
-- 范围：只动 paper 20001（大学计算机基础（一）· 第1次随堂测验）的 2 道简答题 + 3 份提交（800001~800003）；
--       其它提交与题目不变。可重复执行（幂等）。
-- 评分维度（每题 50 分）：①概念/定义准确 15 ②关键步骤或流程完整 15 ③举出例子或应用 12 ④结论与注意事项 8

-- ---------- 1) 把 2 道简答题改成有明确题干与评分维度的题 ----------
update question
set stem = '请说明计算机内部为什么要用二进制表示数据，并各举一个「二进制适合表示」与「不适合直接表示」的例子。',
    answer_json = json_object(
        'ref', '评分维度（50 分）：①二进制的物理可实现性与运算简单性（15 分）；②与逻辑运算/布尔的对应（15 分）；③两个例子各 6 分（例如适合：开关状态、逻辑判断；不适合直接表示：十进制小数、字符与汉字，需要编码或定点/浮点表示）（12 分）；④结论：二进制是工程折中，需配合编码与精度方案（8 分）。',
        'rubric', json_array(
            '①概念与原因准确：物理可实现、运算规则简单、抗干扰（15 分）',
            '②与逻辑/布尔的对应（15 分）',
            '③正例与反例各一个，说明为什么（12 分）',
            '④结论与注意事项：编码、精度、效率的折中（8 分）'
        )
    ),
    analysis = '按四个维度打分；只写结论不给原因最多得 15 分；举不出例子扣 12 分。'
where paper_id = 20001 and q_no = 1;

update question
set stem = '请简述冯·诺依曼体系结构的五个组成部分与「存储程序」的工作流程，并说明它的一个主要局限。',
    answer_json = json_object(
        'ref', '评分维度（50 分）：①五部分：运算器、控制器、存储器、输入设备、输出设备（15 分）；②存储程序流程：程序与数据一起存入内存，按地址顺序取指—译码—执行—写回（15 分）；③举例说明取指执行或程序计数器的作用（12 分）；④主要局限：以运算器为中心导致总线瓶颈 / 顺序执行限制并行（冯·诺依曼瓶颈）（8 分）。',
        'rubric', json_array(
            '①五部分齐全且名称准确（15 分）',
            '②存储程序与取指执行流程（15 分）',
            '③能举例说明指令与数据同存、PC 的作用（12 分）',
            '④主要局限：总线瓶颈或顺序执行（8 分）'
        )
    ),
    analysis = '五部分每缺一项扣 3 分；没写「存储程序」扣 15 分；说不出局限扣 8 分。'
where paper_id = 20001 and q_no = 2;

-- ---------- 2) 三份提交写成三档质量的真实作答，并给出与维度一致的人工分 ----------
-- 800001：完整作答（覆盖四维）→ 人工 44 / 46
update submission_answer sa
    join question q on q.id = sa.question_id
set sa.answer_json = json_object('text',
        '因为二进制只有 0 和 1 两个状态，正好对应电路的「导通/截止」，物理上最容易实现，抗干扰能力强；而且加法、乘法规则极简单，可以用逻辑门直接实现。它还能天然对应布尔逻辑的真假，便于用逻辑电路做运算。适合的例子：开关状态、逻辑判断、磁盘磁化方向都可以用两种状态表示；不适合直接表示的例子：十进制小数（0.1 在二进制里是无限循环，只能近似）、汉字与字符（需要额外编码，如 GBK、UTF-8 用多个字节表示）。所以二进制是一种工程折中，实际使用时要配合编码方案与浮点精度设计。'),
    sa.score = 44.00,
    sa.marked_by_type = 'TEACHER',
    sa.marked_at = now()
where sa.submission_id = 800001 and q.paper_id = 20001 and q.q_no = 1;

update submission_answer sa
    join question q on q.id = sa.question_id
set sa.answer_json = json_object('text',
        '冯·诺依曼结构由运算器、控制器、存储器、输入设备、输出设备五部分组成。它的核心思想是「存储程序」：把程序指令和数据一起以二进制形式存进存储器，CPU 按程序计数器 PC 给出的地址取指令，译码后执行，再写回结果，然后 PC 自增，如此循环。例如执行一条加法指令时，先按 PC 取指令，再由控制器译码，从存储器取出操作数送运算器相加，结果写回内存或寄存器。它的主要局限是以运算器为中心、指令与数据共用一条总线，形成「冯·诺依曼瓶颈」，并且顺序执行限制了并行度。'),
    sa.score = 46.00,
    sa.marked_by_type = 'TEACHER',
    sa.marked_at = now()
where sa.submission_id = 800001 and q.paper_id = 20001 and q.q_no = 2;

-- 800002：部分作答（覆盖两维，缺例子与局限）→ 人工 24 / 22
update submission_answer sa
    join question q on q.id = sa.question_id
set sa.answer_json = json_object('text',
        '计算机用二进制是因为电路只有通和断两种状态，用 0 和 1 表示比较容易实现，而且运算规则简单。'),
    sa.score = 24.00,
    sa.marked_by_type = 'TEACHER',
    sa.marked_at = now()
where sa.submission_id = 800002 and q.paper_id = 20001 and q.q_no = 1;

update submission_answer sa
    join question q on q.id = sa.question_id
set sa.answer_json = json_object('text',
        '冯·诺依曼结构包括运算器、控制器、存储器、输入设备和输出设备五部分，程序和数据都放在存储器里，CPU 按顺序取指令执行。'),
    sa.score = 22.00,
    sa.marked_by_type = 'TEACHER',
    sa.marked_at = now()
where sa.submission_id = 800002 and q.paper_id = 20001 and q.q_no = 2;

-- 800003：敷衍作答（只有结论）→ 人工 6 / 5
update submission_answer sa
    join question q on q.id = sa.question_id
set sa.answer_json = json_object('text', '因为电路好实现，课本上讲过。'),
    sa.score = 6.00,
    sa.marked_by_type = 'TEACHER',
    sa.marked_at = now()
where sa.submission_id = 800003 and q.paper_id = 20001 and q.q_no = 1;

update submission_answer sa
    join question q on q.id = sa.question_id
set sa.answer_json = json_object('text', '就是五大部分一起工作，老师上课讲过。'),
    sa.score = 5.00,
    sa.marked_by_type = 'TEACHER',
    sa.marked_at = now()
where sa.submission_id = 800003 and q.paper_id = 20001 and q.q_no = 2;

-- ---------- 3) 清掉这三份提交此前的 AI 建议，便于重新评测 ----------
delete from ai_grading_suggest where submission_id in (800001, 800002, 800003);
