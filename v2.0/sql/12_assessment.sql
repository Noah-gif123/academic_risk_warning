-- =============================================================================
-- 学情预警系统 v2.0 · 第 12 部分：题目型教学活动层（4 张表）
-- -----------------------------------------------------------------------------
-- 依赖：02（teaching_class）、03（enrollment / course_session）、11（course_lesson）
-- 依据：2026-09 口径冻结版《功能整理》
--       ★ 测验 / 作业 / 考试 共用一套“试卷—题目—作答—批改”引擎，只是入口与归属不同：
--         测验挂课中、作业挂课后、考试在导航栏“我的考试”（paper_type 区分）
--       ★ 题型：单选 / 多选 / 判断 / 填空 / 简答；客观题系统判分、主观题教师批阅
--       ★ 每道题记录判卷人（SYSTEM 系统批阅 / TEACHER 教师批阅；AI 预留第三阶段）
--       ★ 缺考 / 未交记 0 分；逾期不允许补交（应用层校验，表里不设“迟交”状态）
--       ★ 发布时三个开关：查看试卷是否可见、是否显示成绩、答案与解析可见性
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. paper 试卷（测验 / 作业 / 考试共用）
--    说明：① paper_type 区分 QUIZ 随堂小测 / HOMEWORK 课后作业 / EXAM 线上考试；
--          ② lesson_id 可空：测验与作业通常挂在某次课下，考试不挂；
--          ③ scheme_item_id 指向成绩构成里的评分项——本次得分**自动录入该评分项**，
--             下拉只列该课程成绩构成里真实存在的评分项（无对应项则不允许发布该类型）；
--          ④ 三个可见性开关：
--             allow_view_paper  考试/测验结束后学生能否查看试卷内容
--             show_score        是否向学生显示成绩（查看详情页是否出现“我的成绩”）
--             show_answer       答案与解析：NEVER 永远不可见 / AFTER_MARK 批阅后可见
--          ⑤ publisher_role/publisher_id 记录发起人（教师或教务），卡片上展示“发起人”。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID（冗余）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `paper_type`        varchar(20)  NOT NULL                COMMENT '类型：QUIZ随堂测验/HOMEWORK课后作业/EXAM线上考试',
  `paper_name`        varchar(200) NOT NULL                COMMENT '试卷名称（学生端卡片标题的一部分）',
  `lesson_id`         bigint       NULL                    COMMENT '所属课时ID（测验/作业挂课用；考试为空）',
  `start_time`        datetime     NULL                    COMMENT '开始时间',
  `end_time`          datetime     NULL                    COMMENT '结束时间（到点自动交卷、逾期不允许补交）',
  `duration_minutes`  int          NULL                    COMMENT '限时（分钟；为空表示不限时）',
  `full_score`        decimal(6,2) NOT NULL                COMMENT '试卷满分（本次分值，非平时成绩）',
  `scheme_item_id`    bigint       NULL                    COMMENT '计入的评分项ID（score_scheme_item）；为空表示仅记录不计分',
  `publisher_role`    varchar(20)  NOT NULL DEFAULT 'TEACHER' COMMENT '发起人角色：TEACHER教师/ADMIN教务',
  `publisher_id`      bigint       NOT NULL                COMMENT '发起人ID',
  `allow_view_paper`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '考试/测验完成后是否允许学生查看试卷：1是',
  `show_score`        tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否向学生显示成绩：1是',
  `show_answer`       varchar(20)  NOT NULL DEFAULT 'NEVER' COMMENT '答案与解析可见性：NEVER永远不可见/AFTER_MARK批阅后可见',
  `status`            varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/PUBLISHED已发布/CLOSED已截止',
  `remark`            varchar(200) NULL,
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tc_type` (`teaching_class_id`, `paper_type`, `status`),
  KEY `idx_lesson` (`lesson_id`),
  KEY `idx_time` (`start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '试卷表（测验/作业/考试共用）';

-- -----------------------------------------------------------------------------
-- 2. question 题目
--    说明：① 五种题型：SINGLE单选/MULTI多选/JUDGE判断/BLANK填空/SHORT简答；
--          ② 客观题（前四种）由系统自动判分，SHORT 由教师批阅；
--          ③ options_json：选择题选项，如 [{"key":"A","text":"0个"},{"key":"B","text":"1个"}]
--             answer_json：标准答案，如
--               单选 {"keys":["B"]} ／ 多选 {"keys":["A","C"]} ／ 判断 {"value":true}
--               填空 {"answers":["192.168.1.1","192.168.1.1/24"],"ignoreCase":true,"trim":true}
--               简答 {"ref":"参考要点（供教师批阅参考）"}
--          ④ 题目从 Word / PDF 导入后先落成草稿（paper.status = DRAFT），教师在校对页修正后发布。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `paper_id`      bigint       NOT NULL                COMMENT '试卷ID',
  `q_no`          int          NOT NULL                COMMENT '题号（试卷内序号）',
  `q_type`        varchar(20)  NOT NULL                COMMENT '题型：SINGLE单选/MULTI多选/JUDGE判断/BLANK填空/SHORT简答',
  `stem`          text         NOT NULL                COMMENT '题干',
  `options_json`  json         NULL                    COMMENT '选项（选择题使用）',
  `answer_json`   json         NULL                    COMMENT '标准答案（结构见上）',
  `analysis`      text         NULL                    COMMENT '解析（学生端是否可见取决于 paper.show_answer）',
  `full_score`    decimal(6,2) NOT NULL                COMMENT '本题分值',
  `group_name`    varchar(100) NULL                    COMMENT '大题名称（如“第一大题 单选题”），用于学生端分组展示',
  `sort_no`       int          NOT NULL DEFAULT 1      COMMENT '排序',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_paper_sort` (`paper_id`, `sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '题目表';

-- -----------------------------------------------------------------------------
-- 3. submission 提交 / 作答记录（一试卷一学生一条）
--    说明：① submit_status：SUBMITTED已交卷 / MARKED已批阅 / ABSENT缺考；
--          ② 缺考记 0 分：is_absent = 1 且 total_score = 0；
--          ③ 分数拆成 objective_score（系统判）与 subjective_score（教师判），
--             便于学生端展示“其中客观题得分 X 分”；
--          ④ marked_status 是**判卷进度**，与每题判卷人配合使用：
--             NOT_MARKED 未批 / SYSTEM_MARKED 只完成客观题 / ALL_MARKED 全部批完。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `submission`;
CREATE TABLE `submission` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `paper_id`          bigint       NOT NULL                COMMENT '试卷ID',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID（冗余）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `student_id`        bigint       NOT NULL                COMMENT '学生ID',
  `enrollment_id`     bigint       NOT NULL                COMMENT '选课ID（过程数据挂在选课上）',
  `submit_status`     varchar(20)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED已交/MARKED已批阅/ABSENT缺考',
  `start_time`        datetime     NULL                    COMMENT '开考时间',
  `submit_time`       datetime     NULL                    COMMENT '交卷时间',
  `duration_seconds`  int          NULL                    COMMENT '答题用时（秒）',
  `objective_score`   decimal(6,2) NOT NULL DEFAULT 0      COMMENT '客观题得分（系统判）',
  `subjective_score`  decimal(6,2) NOT NULL DEFAULT 0      COMMENT '主观题得分（教师判）',
  `total_score`       decimal(6,2) NULL                    COMMENT '总分（批阅完成后写入）',
  `marked_status`     varchar(20)  NOT NULL DEFAULT 'NOT_MARKED' COMMENT '批阅进度：NOT_MARKED/SYSTEM_MARKED客观已判/ALL_MARKED全部批完',
  `marked_by`         bigint       NULL                    COMMENT '最后批阅人（教师ID；系统判分为空）',
  `marked_at`         datetime     NULL                    COMMENT '最后批阅时间',
  `is_absent`         tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否缺考：1是（记 0 分）',
  `remark`            varchar(200) NULL                    COMMENT '整卷批注',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_student` (`paper_id`, `student_id`),
  KEY `idx_tc_status` (`teaching_class_id`, `submit_status`),
  KEY `idx_enrollment` (`enrollment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '提交与作答记录表';

-- -----------------------------------------------------------------------------
-- 4. submission_answer 逐题作答与判分
--    说明：① 每题一行，存学生答案、得分、对错、批注与**判卷人**；
--          ② marked_by_type：SYSTEM 系统批阅（客观题自动判）/ TEACHER 教师批阅 /
--             AI AI 预批改（第三阶段，与教师双轨，界面上必须标注是谁判的）；
--          ③ is_correct 只对客观题有意义；主观题留空，仅看 score。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `submission_answer`;
CREATE TABLE `submission_answer` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id`   bigint       NOT NULL                COMMENT '提交记录ID',
  `question_id`     bigint       NOT NULL                COMMENT '题目ID',
  `answer_json`     json         NULL                    COMMENT '学生答案（结构同 question.answer_json）',
  `score`           decimal(6,2) NULL                    COMMENT '本题得分',
  `is_correct`      tinyint(1)   NULL                    COMMENT '客观题是否正确：1对/0错（主观题为空）',
  `comment`         varchar(500) NULL                    COMMENT '教师批注',
  `marked_by_type`  varchar(20)  NULL                    COMMENT '判卷人类型：SYSTEM系统/TEACHER教师/AI智能',
  `marked_by`       bigint       NULL                    COMMENT '判卷人ID（教师ID；系统或AI为空）',
  `marked_at`       datetime     NULL                    COMMENT '判卷时间',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sub_q` (`submission_id`, `question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '逐题作答与判分表';
