-- =============================================================================
-- 学情预警系统 v2 重构 · 第 4 部分：考勤双轨（3 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01 基础层、02 组织与开课、03 选课与课次
-- 本层是「通道 A（确定性风险）」的唯一输入，也是老师端"30 秒点名单"的落点。
--
-- 【双轨制的落地方式 —— 只存一行，不存两套】
--   轨道① 平时：老师录入（source=TEACHER，phase=NORMAL）→ 学生端即时可见 → 喂实时预警
--   轨道② 归档：期中/期末老师提交 → 教务确认（source=ADMIN，phase=ARCHIVE，写 archive_id）
--                → 成为成绩计算与 1/3 硬门槛的权威依据
--   ★归档动作 = 就地把该阶段课次的记录"确认/覆盖"为归档值，并写入 attendance_record_log；
--     不复制第二套行，避免"两份考勤谁是真相"。
--   ★归档后被冻结：改必须由教务操作（change_type=ADMIN_CORRECT）且必须留痕。
--
-- 【三条数据纪律】
--   1) 未点名不留白：整节课没点名 → 不产生任何记录（course_session.roll_call_status='NOT_CALLED'）
--   2) 缺课次数是真值的下界：现实中老师会把"点名缺席"放宽成迟到 ⇒ 通道 A 只会漏报不会误报
--   3) 扣分不落库：P 由考勤记录实时算出（2×缺课 + 1×迟到 + 1×max(0, 请假−3)）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. attendance_record 考勤明细（一个学生 × 一次课 = 一行）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '考勤记录ID',
  `enrollment_id`  bigint       NOT NULL                COMMENT '选课ID（学生×教学班×学期）',
  `student_id`     bigint       NOT NULL                COMMENT '学生ID（由 enrollment 决定、写入时校验一致；学生端按此查询）',
  `session_id`     bigint       NOT NULL                COMMENT '课次ID',
  `term_id`        bigint       NOT NULL                COMMENT '学期ID',
  `status`         varchar(20)  NOT NULL                COMMENT '考勤状态：PRESENT出勤/LATE迟到/ABSENT缺课/LEAVE请假（四类必须分开）',
  `leave_type`     varchar(20)  NULL                    COMMENT '请假类型：PERSONAL事假/SICK病假/OFFICIAL公假（仅记录，不影响扣分口径）',
  `phase`          varchar(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '当前口径：NORMAL平时（轨道①）/ARCHIVE归档（轨道②，判定依据）',
  `source`         varchar(20)  NOT NULL DEFAULT 'TEACHER' COMMENT '录入来源：TEACHER老师直录/ADMIN教务统一录入',
  `archive_id`     bigint       NULL                    COMMENT '所属归档批次ID（phase=ARCHIVE 时非空）',
  `recorded_by`    bigint       NULL                    COMMENT '录入人ID',
  `recorded_role`  varchar(20)  NULL                    COMMENT '录入人角色：TEACHER/ADMIN',
  `recorded_at`    datetime     NULL                    COMMENT '录入时间',
  `updated_by`     bigint       NULL                    COMMENT '最后修改人',
  `updated_role`   varchar(20)  NULL                    COMMENT '最后修改人角色',
  `update_note`    varchar(200) NULL                    COMMENT '修改说明（归档后修改必须填写）',
  `batch_key`      varchar(40)  NULL                    COMMENT '录入批次号（一次点名单提交=一个批次，便于整批撤回）',
  `is_void`        tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废：1作废（整节课误录/退课等），不计入任何统计',
  `void_reason`    varchar(200) NULL                    COMMENT '作废原因',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enrollment_session` (`enrollment_id`, `session_id`),
  KEY `idx_student_term` (`student_id`, `term_id`),
  KEY `idx_session_status` (`session_id`, `status`),
  KEY `idx_archive` (`archive_id`),
  KEY `idx_batch` (`batch_key`),
  KEY `idx_status_void` (`status`, `is_void`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '考勤明细表（学生×课次，双轨：平时/归档）';

-- -----------------------------------------------------------------------------
-- 2. attendance_archive 归档批次（期中/期末各一批，逐课程）
--    流程：归档窗口开启 → 教师端强制待办 → 老师提交（SUBMITTED）→ 教务确认（REVIEWED）
--          或退回（RETURNED）→ 教务确认后该阶段记录变为 phase=ARCHIVE（判定依据）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `attendance_archive`;
CREATE TABLE `attendance_archive` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '归档批次ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID',
  `teaching_class_id` bigint       NULL                    COMMENT '教学班ID（老师按教学班提交；教务按课程汇总查看）',
  `teacher_id`        bigint       NULL                    COMMENT '提交老师ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `phase`             varchar(20)  NOT NULL                COMMENT '归档阶段：MID期中/FINAL期末',
  `window_open_time`  datetime     NULL                    COMMENT '归档窗口开启时间',
  `deadline`          datetime     NULL                    COMMENT '提交截止时间',
  `submit_time`       datetime     NULL                    COMMENT '老师提交时间',
  `submit_by`         bigint       NULL                    COMMENT '提交人（教师ID）',
  `submit_note`       varchar(200) NULL                    COMMENT '提交说明',
  `session_from`      int          NULL                    COMMENT '本次归档覆盖的起始课次序号',
  `session_to`        int          NULL                    COMMENT '本次归档覆盖的结束课次序号',
  `session_count`     int          NOT NULL DEFAULT 0      COMMENT '本次归档覆盖课次数',
  `record_count`      int          NOT NULL DEFAULT 0      COMMENT '本次归档涉及考勤记录条数',
  `coverage_rate`     decimal(5,2) NULL                    COMMENT '提交时的点名覆盖率(%)：已点名课次/应完成课次',
  `status`            varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待提交/SUBMITTED已提交待审核/REVIEWED已确认/RETURNED已退回',
  `review_by`         bigint       NULL                    COMMENT '审核人（教务ID）',
  `review_time`       datetime     NULL                    COMMENT '审核时间',
  `review_note`       varchar(200) NULL                    COMMENT '审核意见',
  `return_by`         bigint       NULL                    COMMENT '退回操作人（教务ID）',
  `return_time`       datetime     NULL                    COMMENT '退回时间',
  `return_reason`     varchar(200) NULL                    COMMENT '退回原因',
  `remind_count`      int          NOT NULL DEFAULT 0      COMMENT '催办次数（未提交时红点+站内信催办）',
  `last_remind_time`  datetime     NULL                    COMMENT '最近催办时间',
  `is_void`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废（重开批次等）',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_phase` (`teaching_class_id`, `phase`, `is_void`),
  KEY `idx_offering_phase` (`offering_id`, `phase`),
  KEY `idx_teacher_status` (`teacher_id`, `status`),
  KEY `idx_status_deadline` (`status`, `deadline`),
  KEY `idx_term_phase` (`term_id`, `phase`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '考勤归档批次表（期中/期末，双轨制的落点）';

-- -----------------------------------------------------------------------------
-- 3. attendance_record_log 考勤改动留痕（行级 before/after）
--    为什么单独建：考勤直接决定 1/3 挂科判定，是硬数据；全局 audit_log 记不住
--    "这一次课这个学生从缺课改成了迟到"这种行级细节。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `attendance_record_log`;
CREATE TABLE `attendance_record_log` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `record_id`      bigint       NOT NULL                COMMENT '考勤记录ID',
  `enrollment_id`  bigint       NOT NULL                COMMENT '选课ID',
  `student_id`     bigint       NOT NULL                COMMENT '学生ID',
  `session_id`     bigint       NOT NULL                COMMENT '课次ID',
  `old_status`     varchar(20)  NULL                    COMMENT '变更前状态',
  `new_status`     varchar(20)  NULL                    COMMENT '变更后状态',
  `old_phase`      varchar(20)  NULL                    COMMENT '变更前口径（NORMAL/ARCHIVE）',
  `new_phase`      varchar(20)  NULL                    COMMENT '变更后口径',
  `change_type`    varchar(30)  NOT NULL                COMMENT '变更类型：TEACHER_INPUT老师首次录入/TEACHER_EDIT老师平时修改/ARCHIVE_CONFIRM归档确认/ADMIN_CORRECT归档后教务更正/VOID作废/RESTORE恢复',
  `reason`         varchar(200) NULL                    COMMENT '变更原因',
  `changed_by`     bigint       NULL                    COMMENT '操作人ID',
  `changed_role`   varchar(20)  NULL                    COMMENT '操作人角色：TEACHER/ADMIN',
  `changed_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  `batch_key`      varchar(40)  NULL                    COMMENT '关联批次号',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_student` (`student_id`, `changed_at`),
  KEY `idx_session` (`session_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_changed_at` (`changed_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '考勤改动留痕表（行级 before/after）';

-- =============================================================================
-- 附一：本层的取数口径（写进代码时照此实现）
--   1) 学生考勤明细（学生端、只读）
--        SELECT ... FROM attendance_record r JOIN course_session s ON r.session_id=s.id
--        WHERE r.student_id=? AND r.term_id=? AND r.is_void=0 ORDER BY s.session_date DESC
--   2) 某门课的扣分（实时算，不落库）
--        缺课 N1 = count(status='ABSENT')  迟到 N2 = count(status='LATE')  请假 N3 = count(status='LEAVE')
--        P = 2×N1 + 1×N2 + 1×max(0, N3 − 3)        -- 请假每学期每门课免扣 3 次
--        ★无上限、不截断；T = W − P 可以为负
--   3) 1/3 硬门槛
--        N_max = floor(course_offering.effective_sessions / 3)
--        已缺课 = count(status='ABSENT')；挂科 ⟺ 已缺课 ≥ N_max+1
--        剩余可缺 = N_max − 已缺课；=0 → "再缺一次即挂"；≤−1 → 必然挂科
--   4) 点名覆盖率（教学班级，截至今天）
--        分子 = count(course_session 已过日期且未取消且 roll_call_status<>'NOT_CALLED')
--        分母 = count(course_session 已过日期且未取消且 is_void=0)
--        <1/2 → 提醒老师+教务，且该课置信度判低
--   5) 判定口径优先级
--        某课次已有 phase=ARCHIVE 记录 → 用归档值；否则回退用 phase=NORMAL 值
--        （若某课从未归档，则全部按平时值计算，并在 risk_state.data_quality 里标注）
-- 附二：老师端"一节课一张点名单"的写入方式（一个 batch_key 一批）
--   ① 默认全班 PRESENT：按 enrollment(status='ACTIVE') 生成/更新记录
--   ② 只提交异常：把勾选的学生改为 ABSENT/LATE/LEAVE
--   ③ 提交后回写 course_session.roll_call_status='CALLED'、roll_call_count、roll_call_by/time
--   ④ 每条记录写一行 attendance_record_log(change_type=TEACHER_INPUT)
-- 附三：旧库映射
--   旧 class_performance.absent_count / late_count（汇总，534 行）→ 仅作迁移期参考，
--   新库不导入；考勤改为 course_session + attendance_record 明细重建
--   旧 attendance（0 行汇总表）→ 废弃
-- =============================================================================
