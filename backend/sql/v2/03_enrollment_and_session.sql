-- =============================================================================
-- 学情预警系统 v2 重构 · 第 3 部分：选课与课次（2 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01 基础层（student / course / term）、02 组织与开课（course_offering /
--       teaching_class / teaching_class_admin_class）
-- 本层回答两个问题：
--   ① 谁选了哪门课（enrollment：学生 × 教学班 × 学期）
--   ② 这门课到底上了几次课、哪次点了名（course_session：一次课一行）
-- ★两个不同用途的"课次分母"，务必区分（本项目最容易混淆的地方）：
--   ① 1/3 挂科硬门槛的分母 = course_offering.effective_sessions
--      （教务确认后的"实际课次"，权威值，来自老师期末申报）
--   ② 点名覆盖率的分母 = 该教学班"应完成课次"
--      = count(course_session WHERE session_date <= 今天 AND status <> 'CANCELLED')
--      （按课表逐次生成，能精确反映节假日与调课）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. enrollment 选课（学生 × 教学班 × 学期）
--    ★14 退课：status=WITHDRAWN，学生端显示"经教务批准已退课"，相关预警作废
--    ★13 重修：status=REPEAT（重修班单独开班；统计上不作特殊排除）
--    ★12 缓考：不是选课状态，属于成绩状态（见 05 部分 score_result）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `enrollment`;
CREATE TABLE `enrollment` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '选课ID',
  `student_id`        bigint       NOT NULL                COMMENT '学生ID',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID（决定任课老师与所属行政班范围）',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID（由 teaching_class 决定、写入时校验一致；用于覆盖率/成绩按开课汇总）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `status`            varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE在读/REPEAT重修/WITHDRAWN已退课/EXEMPT免修',
  `retake_times`      int          NOT NULL DEFAULT 0      COMMENT '重修次数（0=首次修读，1=第一次重修…）',
  `enroll_time`       datetime     NULL                    COMMENT '选课/录入时间',
  `withdraw_time`     datetime     NULL                    COMMENT '退课时间',
  `withdraw_reason`   varchar(200) NULL                    COMMENT '退课原因',
  `withdraw_approve_by` bigint     NULL                    COMMENT '退课批准人（教务ID）',
  `remark`            varchar(200) NULL,
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_offering` (`student_id`, `offering_id`),
  UNIQUE KEY `uk_student_teaching_class` (`student_id`, `teaching_class_id`),
  KEY `idx_teaching_class` (`teaching_class_id`),
  KEY `idx_offering_status` (`offering_id`, `status`),
  KEY `idx_student_term` (`student_id`, `term_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '选课表（学生×教学班×学期）';

-- -----------------------------------------------------------------------------
-- 2. course_session 课次（一次课一行）
--    生成方式：教务按"课表（周次 × 星期 × 节次）+ term_calendar 教学周历"批量生成，
--             遇到法定假日整周跳过；生成后可按实际情况调整（停课/调课）。
--    点名方式：教师端"一节课一张点名单"（默认全勤、只勾例外），对应 roll_call_status。
--    ★"未点名不留白"：整节课没点名 → roll_call_status='NOT_CALLED'，
--      且**不产生任何 attendance_record**（不默认全勤、不默认缺课）。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_session`;
CREATE TABLE `course_session` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '课次ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID（由 teaching_class 派生，冗余用于按开课汇总）',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID（课次与点名按教学班进行）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `week_no`           int          NOT NULL                COMMENT '教学周次（对应 term_calendar.week_no）',
  `session_no`        int          NOT NULL                COMMENT '本教学班第几次课（1..planned_sessions，课表驱动定位用）',
  `session_date`      date         NOT NULL                COMMENT '上课日期',
  `start_time`        varchar(10)  NULL                    COMMENT '开始时间，如 08:00',
  `end_time`          varchar(10)  NULL                    COMMENT '结束时间，如 09:40',
  `location`          varchar(50)  NULL                    COMMENT '上课地点',
  `session_type`      varchar(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '课型：NORMAL普通/LAB实验/EXAM考试/OTHER',
  `status`            varchar(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '课次状态：NORMAL正常/CANCELLED停课/ADJUSTED已调课/MERGED合并',
  `adjust_note`       varchar(200) NULL                    COMMENT '停课/调课说明（如 国庆放假、校运动会）',
  `roll_call_status`  varchar(20)  NOT NULL DEFAULT 'NOT_CALLED' COMMENT '点名状态：NOT_CALLED未点名/CALLED已点名/PARTIAL部分点名',
  `roll_call_time`    datetime     NULL                    COMMENT '最近一次点名提交时间',
  `roll_call_by`      bigint       NULL                    COMMENT '点名操作人（教师ID）',
  `roll_call_count`   int          NOT NULL DEFAULT 0      COMMENT '该课次产生的考勤记录条数（=0 表示未点名）',
  `is_void`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废（重排课/误录）：1作废，不计入任何分母',
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_session_no` (`teaching_class_id`, `session_no`),
  KEY `idx_class_week` (`teaching_class_id`, `week_no`),
  KEY `idx_date` (`session_date`),
  KEY `idx_offering` (`offering_id`),
  KEY `idx_roll_call` (`teaching_class_id`, `roll_call_status`),
  KEY `idx_status` (`status`, `is_void`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课次表（一次课一行；点名与覆盖率的基础）';

-- =============================================================================
-- 附一：覆盖率与门槛的取数口径（写进代码时直接照此实现）
--   上课覆盖率（该教学班，截至今天）
--     = count(session WHERE teaching_class_id=? AND is_void=0 AND status<>'CANCELLED'
--             AND session_date <= CURDATE() AND roll_call_status<>'NOT_CALLED')
--       / count(session WHERE teaching_class_id=? AND is_void=0 AND status<>'CANCELLED'
--             AND session_date <= CURDATE())
--   < 1/2 → 提醒任课老师 + 教务，且该课置信度判低
--   1/3 硬门槛
--     N_max = floor(course_offering.effective_sessions / 3)
--     已缺课次数 = count(attendance_record WHERE enrollment_id=? AND status='ABSENT')
--     挂科 ⟺ 已缺课 ≥ N_max + 1
--   剩余可缺次数 = N_max − 已缺课；= 0 → "再缺一次即挂"；≤ −1 → 必然挂科
--   期末需考分数 x = (pass_score + P − W_其余项) / r_f   ← ★修正：W_其余项 = 除"期末类项"以外的加权分
--     例：平时50% 考了85、期末50%（尚无分）、及格60、P=11 → x = (60 + 11 − 42.5)/0.5 = 57 分
--     x > 100 → 实际不可达（期末满分 100 是硬上限）
--     边际换算（UI 文案用）：每多缺一次课（P +2）→ 期末需多考 2 / r_f 分
--       （期末占比 60% → 3.3 分；50% → 4.0 分；40% → 5.0 分，与四轮文档的表一致）
-- 附二：旧库映射
--   student_course（534 行，每生 1 门课）→ enrollment
--     需补齐：teaching_class_id（按旧 teacher_class 的 ID 区间推断）、offering_id、term_id
--   表结构脚本里没有的 attendance 明细 → course_session + attendance_record 全新建
-- =============================================================================
