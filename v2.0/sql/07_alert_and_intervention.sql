-- =============================================================================
-- 学情预警系统 v2 重构 · 第 7 部分：预警与干预闭环（6 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01–06 全部
-- 本层把"预警"变成"能追、能处理、能升级、能闭环"的链路：
--   ① alert_record          预警记录（重建：带通道/置信度/引擎版本）
--   ② alert_flow_log        预警生命周期时间线（替代已删除的快照曲线）
--   ③ intervention_record   干预记录（重构 + 迁移旧 21 行）
--   ④ notification          通知主体（内容 + 业务关联 + 聚合）
--   ⑤ notification_receipt  每个接收人的投递与回执（★16 班集体与个体都要回执）
--   ⑥ class_alert_notification 班集体提醒去重（防天天提醒同一个班）
--
-- 【升级链状态机（★导员端）】
--   导员第1次处理 → 无效(下次 risk_state 刷新 / 最长 14 天) → 导员第2次
--   → 仍无效 → 导员手动【上报教务】→ 教务面谈 / 具体帮助 → 期末终判
--   → 再无改善 → 【终止干预】
--   全程写 intervention_record；每次事件同时写 alert_flow_log。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. alert_record 预警记录
--    "当前生效预警"用 is_current 表达：1=当前，NULL=历史
--    （MySQL 唯一索引允许多个 NULL，因此 uk_enroll_current 能保证
--      "同一选课同时只有一条当前预警"，同时保留完整历史）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `student_id`         bigint       NOT NULL                COMMENT '学生ID',
  `enrollment_id`      bigint       NOT NULL                COMMENT '选课ID',
  `offering_id`        bigint       NOT NULL                COMMENT '开课ID',
  `teaching_class_id`  bigint       NOT NULL                COMMENT '教学班ID',
  `course_id`          bigint       NOT NULL                COMMENT '课程ID',
  `term_id`            bigint       NOT NULL                COMMENT '学期ID',
  `alert_level`        varchar(10)  NOT NULL                COMMENT '预警等级：RED/ORANGE/YELLOW',
  `trigger_channel`    varchar(10)  NOT NULL                COMMENT '触发通道：A确定性/B班内相对/C跨班可比',
  `trigger_rule`       varchar(50)  NULL                    COMMENT '触发规则：A_HARD_LINE已超1/3/A_NEAR_LINE剩一次/A_UNREACHABLE期末不可达/B_RANK班内靠前/C_THRESHOLD跨班超标',
  `risk_score`         decimal(5,1) NULL                    COMMENT '触发时风险分',
  `confidence`         decimal(4,3) NULL                    COMMENT '触发时置信度',
  `output_mode`        varchar(20)  NULL                    COMMENT '触发时输出形态：FULL_SCORE/SCORE_WITH_NOTE/BEHAVIOR_ONLY/INSUFFICIENT',
  `alert_reason`       text         NULL                    COMMENT '人话版预警原因（面向老师/学生）',
  `evidence_json`      json         NULL                    COMMENT '触发依据快照：{"absent":9,"allowed":10,"remaining":1,"needFinal":93.8,"coverage":0.72}',
  `channel_snapshot_json` json      NULL                    COMMENT '触发时三通道值快照：{"a":"DANGER","b":68.2,"c":null}',
  `status`             varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE处理中/ACKNOWLEDGED已知悉/HANDLING处理中/CLOSED已关闭/DISMISSED已忽略/EXPIRED超时关闭',
  `is_current`         tinyint(1)   NULL                    COMMENT '是否当前生效预警：1当前 / NULL历史（配合唯一键保证每生每课仅一条当前预警）',
  `escalated`          tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已升级过等级',
  `escalated_at`       datetime     NULL                    COMMENT '升级时间',
  `previous_level`     varchar(10)  NULL                    COMMENT '升级前等级',
  `notified`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已发出通知',
  `first_notify_time`  datetime     NULL                    COMMENT '首次通知时间',
  `expire_time`        datetime     NULL                    COMMENT '超时时间（超时未处理 → 自动升级/关闭）',
  `handle_by`          bigint       NULL                    COMMENT '处理人ID',
  `handle_role`        varchar(20)  NULL                    COMMENT '处理人角色：TEACHER/COUNSELOR/ADMIN',
  `handle_time`        datetime     NULL                    COMMENT '处理时间',
  `handle_note`        text         NULL                    COMMENT '处理说明',
  `close_by`           bigint       NULL                    COMMENT '关闭人ID',
  `close_time`         datetime     NULL                    COMMENT '关闭时间',
  `close_note`         varchar(500) NULL                    COMMENT '关闭说明/结论',
  `dismiss_reason`     varchar(200) NULL                    COMMENT '忽略原因',
  `student_response`   text         NULL                    COMMENT '学生回应（学生端可回执）',
  `student_response_time` datetime  NULL                    COMMENT '学生回应时间',
  `engine_version`     varchar(20)  NULL                    COMMENT '产生该预警的引擎版本',
  `is_void`            tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废（退课/重算）：1作废，不进任何统计与列表',
  `void_reason`        varchar(200) NULL,
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enroll_current` (`enrollment_id`, `is_current`),
  KEY `idx_student_status` (`student_id`, `status`, `is_void`),
  KEY `idx_teacher_query` (`teaching_class_id`, `status`),
  KEY `idx_offering_level` (`offering_id`, `alert_level`),
  KEY `idx_term_level` (`term_id`, `alert_level`),
  KEY `idx_status_expire` (`status`, `expire_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '预警记录表（重建：带通道/置信度/引擎版本）';

-- -----------------------------------------------------------------------------
-- 2. alert_flow_log 预警生命周期时间线
--    替代"快照曲线"：回答"什么时候变危险的、谁做了什么"
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `alert_flow_log`;
CREATE TABLE `alert_flow_log` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_id`      bigint       NOT NULL                COMMENT '预警ID',
  `student_id`    bigint       NOT NULL                COMMENT '学生ID',
  `term_id`       bigint       NOT NULL                COMMENT '学期ID',
  `event_type`    varchar(30)  NOT NULL                COMMENT '事件：CREATED生成/LEVEL_UP升级/LEVEL_DOWN降级/NOTIFIED已通知/ACKNOWLEDGED已知悉/HANDLED已处理/CLOSED关闭/DISMISSED忽略/EXPIRED超时/ESCALATED已升级上报/REFERRED上报教务/ADMIN_HANDLED教务处理/TERMINATED终止干预/REOPENED重新打开/VOIDED作废',
  `from_level`    varchar(10)  NULL                    COMMENT '变更前等级',
  `to_level`      varchar(10)  NULL                    COMMENT '变更后等级',
  `from_status`   varchar(20)  NULL                    COMMENT '变更前状态',
  `to_status`     varchar(20)  NULL                    COMMENT '变更后状态',
  `detail`        varchar(500) NULL                    COMMENT '事件说明（如"缺课达 11 次，触发 1/3 硬门槛"）',
  `operator_id`   bigint       NULL                    COMMENT '操作人ID（系统触发为空）',
  `operator_role` varchar(20)  NULL                    COMMENT '操作人角色：TEACHER/COUNSELOR/ADMIN/SYSTEM',
  `event_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_time` (`alert_id`, `event_time`),
  KEY `idx_student_time` (`student_id`, `event_time`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_event_time` (`event_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '预警生命周期时间线（替代快照曲线）';

-- -----------------------------------------------------------------------------
-- 3. intervention_record 干预记录（重构）
--    旧表 21 行迁移映射见文末附三；旧 teacher_id 非空、alert_id 非空已解除
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `intervention_record`;
CREATE TABLE `intervention_record` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '干预记录ID',
  `student_id`        bigint       NOT NULL                COMMENT '学生ID',
  `term_id`           bigint       NULL                    COMMENT '学期ID（旧数据迁移时可为空）',
  `alert_id`          bigint       NULL                    COMMENT '关联预警ID（可空：可由教师主动发起，不只针对单条预警）',
  `related_alert_ids` json         NULL                    COMMENT '关联预警ID数组（一次处理可覆盖多条预警）',
  `stage`             varchar(30)  NOT NULL                COMMENT '阶段：TEACHER教师处理/COUNSELOR_1导员第1次/COUNSELOR_2导员第2次/REFERRED已上报教务/ADMIN_TALK教务面谈/ADMIN_HELP教务具体帮助/TERMINATED终止干预',
  `operator_id`       bigint       NOT NULL                COMMENT '处理人ID',
  `operator_role`     varchar(20)  NOT NULL                COMMENT '处理人角色：TEACHER/COUNSELOR/ADMIN',
  `action_type`       varchar(30)  NOT NULL                COMMENT '动作：TALK谈话/CLASS_MEETING班会/PARENT联系家长/SUPPLEMENT补课补习/PLAN学习计划/INTERVIEW面谈/HELP具体帮助/OTHER其他',
  `content`           text         NULL                    COMMENT '处理内容',
  `result`            text         NULL                    COMMENT '结果描述',
  `effective`         varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '是否有成效：EFFECTIVE有效/INEFFECTIVE无效/PENDING待观察',
  `effect_check_time` datetime     NULL                    COMMENT '成效检查时间（= 下次 risk_state 刷新 或 干预后 14 天，先到为准）',
  `risk_score_before` decimal(5,1) NULL                    COMMENT '干预时风险分（快照）',
  `risk_score_after`  decimal(5,1) NULL                    COMMENT '成效检查时的风险分',
  `risk_score_change` decimal(5,1) NULL                    COMMENT '风险分变化（负数=改善）',
  `refer_status`      varchar(20)  NULL                    COMMENT '上报状态：NONE未上报/REFERRED已上报/ADMIN_HANDLING教务处理中/ADMIN_DONE教务已完结',
  `refer_by`          bigint       NULL                    COMMENT '上报人（导员ID）',
  `refer_time`        datetime     NULL                    COMMENT '上报时间',
  `refer_note`        varchar(500) NULL                    COMMENT '上报说明（导员判定"收集齐全"的说明）',
  `admin_feedback`    text         NULL                    COMMENT '教务处理反馈（导员可追踪"交上去之后怎么样了"）',
  `is_void`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废',
  `remark`            varchar(200) NULL,
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_time` (`student_id`, `create_time`),
  KEY `idx_alert` (`alert_id`),
  KEY `idx_operator` (`operator_id`, `operator_role`),
  KEY `idx_stage` (`stage`),
  KEY `idx_effective` (`effective`, `effect_check_time`),
  KEY `idx_refer` (`refer_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '干预记录表（重构：支持教师/导员/教务三级与上报闭环）';

-- -----------------------------------------------------------------------------
-- 4. notification 通知主体（内容 + 业务关联 + 聚合）
--    ★改进：把"收件人"从本表移到 notification_receipt，
--      因为一条通知可能发给多人（如班集体提醒：导员 + 抄送教务），各自回执状态不同
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `biz_type`        varchar(30)  NOT NULL                COMMENT '业务类型：ALERT_NEW/ALERT_UPGRADE/CLASS_ALERT班集体提醒/ARCHIVE_TASK归档待办/ARCHIVE_RESULT归档结果/COVERAGE_REMIND覆盖率提醒/SCORE_TASK打分待办/MULTI_COURSE多科目预警/REFER_RESULT上报结果/QUESTIONNAIRE问卷/SYSTEM系统',
  `biz_id`          bigint       NULL                    COMMENT '关联业务ID（预警/归档批次/开课/问卷等）',
  `title`           varchar(200) NOT NULL                COMMENT '标题',
  `content`         text         NULL                    COMMENT '正文',
  `payload_json`    json         NULL                    COMMENT '结构化载荷（跳转参数、明细列表等）',
  `aggregate_key`   varchar(60)  NULL                    COMMENT '聚合键：如 CLASS_ALERT:{counselorId}:{termId}:{weekNo}，同键只发一条汇总',
  `aggregate_count` int          NOT NULL DEFAULT 1      COMMENT '本通知聚合了多少条明细（防骚扰：一次一条汇总）',
  `audience_desc`   varchar(100) NULL                    COMMENT '接收范围描述，如"软件1–4班导员 + 教务处"',
  `priority`        varchar(10)  NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：HIGH/NORMAL/LOW',
  `expire_time`     datetime     NULL                    COMMENT '过期时间',
  `create_by`       bigint       NULL,
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_key` (`aggregate_key`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '通知主体表（聚合与业务关联）';

-- -----------------------------------------------------------------------------
-- 5. notification_receipt 通知投递与回执（每个接收人一行）
--    ★16：班集体提醒与个体提醒**都需要回执**（否则导员会忘）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `notification_receipt`;
CREATE TABLE `notification_receipt` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notification_id` bigint      NOT NULL                COMMENT '通知ID',
  `recipient_id`   bigint       NOT NULL                COMMENT '接收人ID（sys_account.id）',
  `recipient_role` varchar(20)  NOT NULL                COMMENT '接收人角色：STUDENT/TEACHER/COUNSELOR/ADMIN',
  `channel`        varchar(20)  NOT NULL DEFAULT 'IN_APP' COMMENT '渠道：IN_APP站内信/EMAIL邮件/SMS短信',
  `send_status`    varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态：PENDING待发/SENT已发/FAILED失败',
  `send_time`      datetime     NULL                    COMMENT '发送时间',
  `is_read`        tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已读',
  `read_time`      datetime     NULL                    COMMENT '已读时间',
  `ack_required`   tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否需要回执确认',
  `ack_status`     varchar(20)  NOT NULL DEFAULT 'NONE' COMMENT '回执状态：NONE无需/PENDING待确认/ACKED已确认/IGNORED已忽略',
  `ack_time`       datetime     NULL                    COMMENT '确认时间',
  `ack_note`       varchar(500) NULL                    COMMENT '确认说明',
  `action_taken`   varchar(200) NULL                    COMMENT '已采取的行动（如"已开班会"、"已约谈学生"）',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_recipient_channel` (`notification_id`, `recipient_id`, `channel`),
  KEY `idx_recipient` (`recipient_id`, `recipient_role`, `is_read`),
  KEY `idx_ack` (`ack_required`, `ack_status`),
  KEY `idx_send_status` (`send_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '通知投递与回执表（每个接收人一行）';

-- -----------------------------------------------------------------------------
-- 6. class_alert_notification 班集体提醒去重（导员端）
--    规则（★）：第 1–4 周不触发；第 5 周起
--      档位1：预警占比 ≥30% 且 ≥5 人   → 班集体提醒（建议开班会）
--      档位2：预警占比 ≥50% 且 ≥10 人  → 升级"重点关注班级" + 抄送教务
--    UNIQUE(class, term, rule_level) ⇒ 同一班同一学期同一档位只提醒一次（绝不天天提醒）
--    档位从 L1 升到 L2 属不同 rule_level，允许再发一次（这正是"升级"）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `class_alert_notification`;
CREATE TABLE `class_alert_notification` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_class_id`      bigint       NOT NULL                COMMENT '行政班ID',
  `term_id`             bigint       NOT NULL                COMMENT '学期ID',
  `counselor_id`        bigint       NOT NULL                COMMENT '主责导员ID',
  `rule_level`          varchar(20)  NOT NULL                COMMENT '档位：L1_MEETING建议开班会/L2_FOCUS重点关注班级',
  `week_no`             int          NOT NULL                COMMENT '触发时的教学周次（≥5 才可能触发）',
  `class_student_count` int          NOT NULL DEFAULT 0      COMMENT '该班学生数（分母）',
  `alert_student_count` int          NOT NULL DEFAULT 0      COMMENT '预警学生数（分子，风险分 ≥ 阈值）',
  `alert_rate`          decimal(5,2) NOT NULL DEFAULT 0.00   COMMENT '预警占比(%)',
  `risk_floor_used`     decimal(5,1) NULL                    COMMENT '本次统计使用的风险分下限（默认 40）',
  `threshold_json`      json         NULL                    COMMENT '触发阈值快照：{"rate":0.30,"minCount":5,"weekStart":5}',
  `student_ids_json`    json         NULL                    COMMENT '纳入本次提醒的学生ID列表（可回溯"这条提醒包含谁"）',
  `notification_id`     bigint       NULL                    COMMENT '生成的汇总通知ID',
  `cc_admin`            tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否抄送教务（档位2 为 1）',
  `is_sent`             tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已发送',
  `sent_time`           datetime     NULL,
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_term_level` (`admin_class_id`, `term_id`, `rule_level`),
  KEY `idx_counselor_term` (`counselor_id`, `term_id`),
  KEY `idx_notification` (`notification_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '班集体提醒去重表（导员端，防天天提醒）';

-- =============================================================================
-- 附一：多科目预警（≥3 门）与班集体提醒的取数口径
--   多科目预警：某生本学期 risk_level ∈ (RED,ORANGE,YELLOW) 的课程数 ≥ 3
--     → 生成 notification(biz_type='MULTI_COURSE') 给其行政班的主责导员
--     → 每次 risk_state 批量刷新后统一计算，同一学生同学期只提醒一次
--   班集体提醒：某行政班本学期"进行中课程"产生的预警学生占比（风险分 ≥ 40）
--     → 命中档位后写 class_alert_notification 去重，再生成一条汇总 notification
--     → 抄送教务（L2）走 notification_receipt(role=ADMIN)
-- 附二：干扰控制（防骚扰）
--   个体提醒：按预警去重（每条当前预警只发一次）
--   班集体提醒：按 (班, 学期, 档位) 去重
--   覆盖率提醒：按 coverage_check_log 去重（08 部分）
--   归档催办：按 attendance_archive.remind_count 限频（如 3 天一次，最多 3 次）
-- 附三：旧 intervention_record（21 行）迁移映射
--   alert_id            → alert_id（可空，原值可空性不变）+ related_alert_ids=[原 alert_id]
--   teacher_id          → operator_id，operator_role='TEACHER'
--   intervention_type   → action_type（TALK→TALK / TUTOR→SUPPLEMENT / PARENT→PARENT / SUPPLEMENT→SUPPLEMENT / PLAN→PLAN / OTHER→OTHER）
--   status              → effective：INEFFECTIVE→INEFFECTIVE / COMPLETED→EFFECTIVE / EXECUTING→PENDING
--   description         → content；result_note → result
--   risk_score_before/after/change → 同名字段原样保留
--   stage               → 统一填 'TEACHER'（旧系统只有教师处理）
--   create_time/update_time/effect_check_time → 原样保留
-- 附四：旧库映射（其余）
--   旧 alert_record（164 行）→ 不迁入新库（口径不同，阈值 40 vs 75）；旧库整体归档为 legacy
--   旧 alert_operation_log    → 废弃，改由 alert_flow_log 承担
--   旧 notification（1158 行）→ 结构重建（拆主体+回执）；旧行不回填，随旧库归档
-- =============================================================================
