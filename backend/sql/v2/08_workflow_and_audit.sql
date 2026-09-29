-- =============================================================================
-- 学情预警系统 v2 重构 · 第 8 部分：教务流程与审计（8 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01–07 全部
-- 本层补齐"教务是数据枢纽"所需的四件事：督办、核对、可回滚、可审计，
-- 以及问卷子系统（含 G6 补丁：流转状态可见 + 超时提醒，不改链路结构）。
--   ① coverage_check_log    覆盖率体检记录（去重 + 督办跟踪）
--   ② exam_headcount        考前实际考核人数确认 + 成绩录入核对
--   ③ import_batch          导入批次（可查、可回滚）
--   ④ audit_log             统一审计（替代旧的两套日志）
--   ⑤ system_config         运营配置（归档周次、催办频率等）
--   ⑥⑦⑧ questionnaire / questionnaire_answer / questionnaire_flow  问卷
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. coverage_check_log 覆盖率体检记录
--    期内：每 coverage.soft.check.weeks 周对"连续 N 周零录入"的课软提醒
--    期中/期末归档节点：正式体检，< 1/2 进教务督办列表
--    去重：UNIQUE(教学班, 学期, 体检类型, 周次) —— 同一周同一类型只提醒一次
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `coverage_check_log`;
CREATE TABLE `coverage_check_log` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID',
  `teacher_id`        bigint       NOT NULL                COMMENT '任课教师ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `check_type`        varchar(20)  NOT NULL                COMMENT '体检类型：SOFT期内软提醒/MID期中正式/FINAL期末正式',
  `check_date`        date         NOT NULL                COMMENT '体检日期',
  `week_no`           int          NULL                    COMMENT '体检时教学周次',
  `due_sessions`      int          NOT NULL DEFAULT 0      COMMENT '应完成课次（分母：已过日期且未取消）',
  `recorded_sessions` int          NOT NULL DEFAULT 0      COMMENT '已点名课次（分子）',
  `coverage_rate`     decimal(5,2) NULL                    COMMENT '点名覆盖率(%)',
  `below_threshold`   tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否低于阈值：1是（进督办列表）',
  `notified`          tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已提醒（老师+教务）',
  `notification_id`   bigint       NULL                    COMMENT '关联提醒通知ID',
  `resolved`          tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已补录解决',
  `resolved_at`       datetime     NULL                    COMMENT '解决时间',
  `resolve_note`      varchar(200) NULL                    COMMENT '督办说明',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_type_week` (`teaching_class_id`, `term_id`, `check_type`, `week_no`),
  KEY `idx_teacher` (`teacher_id`, `check_type`),
  KEY `idx_below` (`below_threshold`, `resolved`),
  KEY `idx_term_type` (`term_id`, `check_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '覆盖率体检记录表（去重与督办跟踪）';

-- -----------------------------------------------------------------------------
-- 2. exam_headcount 考前实际考核人数确认 + 成绩录入核对
--    "教务在考前二次确认实际考核人数" → 这是挂科率分母的唯一权威来源
--    "成绩录入后核对人数（录入 vs 考核）" → entered_count / check_result
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_headcount`;
CREATE TABLE `exam_headcount` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `offering_id`         bigint       NOT NULL                COMMENT '开课ID',
  `term_id`             bigint       NOT NULL                COMMENT '学期ID',
  `phase`               varchar(20)  NOT NULL                COMMENT '阶段：MID期中/FINAL期末',
  `roster_count`        int          NULL                    COMMENT '名单人数（选课 ACTIVE 人数）',
  `expected_count`      int          NULL                    COMMENT '预计考核人数（扣除已审批退课/免修）',
  `confirmed_count`     int          NULL                    COMMENT '教务确认的实际考核人数（挂科率分母）',
  `deferred_count`      int          NOT NULL DEFAULT 0      COMMENT '其中缓考人数（不计入分母）',
  `no_score_count`      int          NOT NULL DEFAULT 0      COMMENT '其中无成绩待定人数（不计入分母）',
  `diff_reason`         varchar(200) NULL                    COMMENT '与名单人数不一致的原因说明',
  `confirm_status`      varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '确认状态：PENDING待确认/CONFIRMED已确认',
  `confirm_by`          bigint       NULL                    COMMENT '确认人（教务ID）',
  `confirm_time`        datetime     NULL                    COMMENT '确认时间',
  `entered_count`       int          NULL                    COMMENT '实际录入成绩人数',
  `check_result`        varchar(20)  NULL                    COMMENT '录入核对结果：MATCH一致/MISMATCH不一致/NOT_CHECKED未核对',
  `check_time`          datetime     NULL                    COMMENT '核对时间',
  `check_note`          varchar(200) NULL                    COMMENT '核对说明',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offering_phase` (`offering_id`, `phase`),
  KEY `idx_term_phase` (`term_id`, `phase`),
  KEY `idx_status` (`confirm_status`),
  KEY `idx_check` (`check_result`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '实际考核人数确认与成绩录入核对表';

-- -----------------------------------------------------------------------------
-- 3. import_batch 导入批次（可查、可回滚）
--    回滚约定：业务表记录批次号（如 attendance_record.batch_key、
--    student_admission_score.import_batch_id），回滚按批次作废/删除并写 audit_log
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `import_batch`;
CREATE TABLE `import_batch` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no`      varchar(40)  NOT NULL                COMMENT '批次号（业务表按此回滚）',
  `import_type`   varchar(30)  NOT NULL                COMMENT '类型：ADMISSION_SCORE高考成绩/TIMETABLE周数课次/SCORE成绩/HOMEWORK作业/ATTENDANCE考勤/ROSTER名单/KP知识点/OTHER',
  `term_id`       bigint       NULL                    COMMENT '学期ID（部分导入与学期无关，可空）',
  `file_name`     varchar(200) NULL                    COMMENT '原始文件名',
  `total_rows`    int          NOT NULL DEFAULT 0      COMMENT '总行数',
  `success_rows`  int          NOT NULL DEFAULT 0      COMMENT '成功行数',
  `failed_rows`   int          NOT NULL DEFAULT 0      COMMENT '失败行数',
  `error_json`    json         NULL                    COMMENT '失败明细（行号 + 原因）',
  `status`        varchar(20)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING进行中/SUCCESS全部成功/PARTIAL部分成功/FAILED失败/ROLLED_BACK已回滚',
  `operator_id`   bigint       NULL                    COMMENT '操作人ID',
  `operator_role` varchar(20)  NULL                    COMMENT '操作人角色',
  `start_time`    datetime     NULL                    COMMENT '开始时间',
  `end_time`      datetime     NULL                    COMMENT '结束时间',
  `rollback_by`   bigint       NULL                    COMMENT '回滚人',
  `rollback_time` datetime     NULL                    COMMENT '回滚时间',
  `rollback_reason` varchar(200) NULL                  COMMENT '回滚原因',
  `remark`        varchar(200) NULL,
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_type_time` (`import_type`, `create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_operator` (`operator_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '导入批次表（可查可回滚）';

-- -----------------------------------------------------------------------------
-- 4. audit_log 统一审计（替代旧 teacher_management_log + alert_operation_log）
--    覆盖：改分、改考勤、归档/退回、上报/终止、成绩构成配置与锁定、L3 审批、
--          申报实际课次与确认、账号与权限变更、导入与回滚
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`      varchar(30)  NOT NULL                COMMENT '业务类型：SCORE成绩/ATTENDANCE考勤/ARCHIVE归档/REFER上报/INTERVENTION干预/COURSE_GROUP课程组/SCORE_SCHEME成绩构成/OFFERING开课/ACTUAL_SESSIONS实际课次/EXAM_HEADCOUNT考核人数/ACCOUNT账号/NOTIFICATION通知/IMPORT导入/QUESTIONNAIRE问卷/SYSTEM系统',
  `biz_id`        bigint       NULL                    COMMENT '业务对象ID',
  `action`        varchar(30)  NOT NULL                COMMENT '动作：CREATE/UPDATE/DELETE/VOID/RESTORE/LOCK/UNLOCK/CONFIRM/APPROVE/REJECT/SUBMIT/RETURN/REFER/TERMINATE/IMPORT/ROLLBACK/LOGIN/PASSWORD_RESET',
  `operator_id`   bigint       NULL                    COMMENT '操作人ID',
  `operator_role` varchar(20)  NULL                    COMMENT '操作人角色：STUDENT/TEACHER/COUNSELOR/ADMIN/SYSTEM',
  `before_json`   json         NULL                    COMMENT '变更前快照（敏感字段脱敏，如密码只记是否变更）',
  `after_json`    json         NULL                    COMMENT '变更后快照',
  `detail`        varchar(500) NULL                    COMMENT '说明（人可读）',
  `ip`            varchar(45)  NULL                    COMMENT '来源IP',
  `user_agent`    varchar(200) NULL                    COMMENT '客户端信息（可选）',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_operator_time` (`operator_id`, `create_time`),
  KEY `idx_action_time` (`action`, `create_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '统一审计日志表';

-- -----------------------------------------------------------------------------
-- 5. system_config 运营配置（与引擎参数分开：这里是"运营/流程"类开关与周次）
--    预置键见文末附一；学期级特殊值直接放 term / course_offering，本表保持全局
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`    varchar(60)  NOT NULL                COMMENT '配置键，如 archive.mid.week',
  `config_value`  varchar(500) NULL                    COMMENT '配置值',
  `config_type`   varchar(20)  NOT NULL DEFAULT 'STRING' COMMENT '值类型：STRING/INT/DECIMAL/BOOL/JSON',
  `config_group`  varchar(30)  NOT NULL DEFAULT 'SYSTEM' COMMENT '分组：ARCHIVE归档/NOTIFY通知/ATTENDANCE考勤/COVERAGE覆盖率/QUESTIONNAIRE问卷/SYSTEM系统',
  `description`   varchar(200) NULL                    COMMENT '说明',
  `is_editable`   tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否允许界面修改',
  `create_by`     bigint       NULL,
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     bigint       NULL,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_group` (`config_group`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '运营配置表';

-- -----------------------------------------------------------------------------
-- 6. questionnaire 问卷（产出两类：① 老师评价 ② 知识点讲解反馈）
--    流转（已拍板）：导员统一收集 → 教务接收 → 教务分发给该课程授课组全体老师
--    题目定义放 question_json（避免再多一张表）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `questionnaire`;
CREATE TABLE `questionnaire` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '问卷ID',
  `offering_id`           bigint       NOT NULL                COMMENT '开课ID',
  `course_id`             bigint       NOT NULL                COMMENT '课程ID',
  `term_id`               bigint       NOT NULL                COMMENT '学期ID',
  `questionnaire_type`    varchar(30)  NOT NULL                COMMENT '类型：TEACHER_EVAL老师评价/KP_FEEDBACK知识点讲解反馈',
  `title`                 varchar(200) NULL                    COMMENT '问卷标题',
  `question_json`         json         NULL                    COMMENT '题目定义：[{"code":"Q1","type":"SCORE","text":"..."},{"code":"K1","type":"KP_FLAG","kpId":12}]',
  `is_anonymous`          tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否匿名（老师评价默认匿名：不存学生ID，只存去重令牌）',
  `collector_type`        varchar(20)  NOT NULL DEFAULT 'COUNSELOR' COMMENT '收集方式：COUNSELOR导员统一收集/ONLINE线上直收',
  `collector_id`          bigint       NULL                    COMMENT '收集人（导员ID）',
  `collector_class_ids_json` json      NULL                    COMMENT '收集覆盖的行政班ID数组',
  `open_time`             datetime     NULL                    COMMENT '开放时间',
  `close_time`            datetime     NULL                    COMMENT '截止时间',
  `status`                varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/OPEN收集中/CLOSED已截止/SUBMITTED已交教务/DISTRIBUTED已分发/ARCHIVED已归档',
  `answer_count`          int          NOT NULL DEFAULT 0      COMMENT '已作答份数',
  `submit_to_admin_by`    bigint       NULL                    COMMENT '提交教务的人（导员ID）',
  `submit_to_admin_time`  datetime     NULL                    COMMENT '提交教务时间',
  `distributed_by`        bigint       NULL                    COMMENT '分发人（教务ID）',
  `distributed_time`      datetime     NULL                    COMMENT '分发时间',
  `remark`                varchar(200) NULL,
  `create_by`             bigint       NULL,
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`             bigint       NULL,
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offering_type` (`offering_id`, `questionnaire_type`),
  KEY `idx_term_status` (`term_id`, `status`),
  KEY `idx_collector` (`collector_id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '问卷表（老师评价 / 知识点讲解反馈）';

-- -----------------------------------------------------------------------------
-- 7. questionnaire_answer 问卷作答明细
--    匿名保护：匿名问卷不存 student_id，只存 respondent_token（学号+盐 的哈希）
--    去重：UNIQUE(questionnaire_id, respondent_token, item_code) —— 每人每题一次
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `questionnaire_answer`;
CREATE TABLE `questionnaire_answer` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `questionnaire_id`  bigint       NOT NULL                COMMENT '问卷ID',
  `respondent_token`  varchar(64)  NOT NULL                COMMENT '作答人去重令牌（匿名问卷用于防重复提交，不暴露身份）',
  `student_id`        bigint       NULL                    COMMENT '学生ID（匿名问卷为空）',
  `enrollment_id`     bigint       NULL                    COMMENT '选课ID（可空）',
  `target_teacher_id` bigint       NULL                    COMMENT '被评价教师ID（老师评价类）',
  `item_code`         varchar(30)  NOT NULL                COMMENT '题目编码',
  `answer_type`       varchar(20)  NOT NULL                COMMENT '作答类型：SCORE评分/TEXT文本/KP_FLAG知识点反馈',
  `score_value`       decimal(5,2) NULL                    COMMENT '评分值（如 1–5 分）',
  `text_value`        text         NULL                    COMMENT '文本作答',
  `kp_id`             bigint       NULL                    COMMENT '知识点ID（KP_FLAG：懂/不懂/没讲）',
  `kp_flag`           varchar(20)  NULL                    COMMENT '知识点反馈：UNDERSTOOD懂/NOT_UNDERSTOOD不懂/NOT_COVERED没讲',
  `submit_time`       datetime     NULL                    COMMENT '提交时间',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_q_token_item` (`questionnaire_id`, `respondent_token`, `item_code`),
  KEY `idx_q_teacher` (`questionnaire_id`, `target_teacher_id`),
  KEY `idx_kp` (`kp_id`, `kp_flag`),
  KEY `idx_student` (`student_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '问卷作答明细表（匿名去重）';

-- -----------------------------------------------------------------------------
-- 8. questionnaire_flow 问卷流转状态（G6 补丁：状态可见 + 超时提醒）
--    四个环节：导员收集 → 教务接收 → 教务分发 → 授课组查看
--    只为"看得见卡在哪、该催谁"，不改动既定流转结构
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `questionnaire_flow`;
CREATE TABLE `questionnaire_flow` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `questionnaire_id`  bigint       NOT NULL                COMMENT '问卷ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `stage`             varchar(30)  NOT NULL                COMMENT '环节：COUNSELOR_COLLECT导员收集/ADMIN_RECEIVE教务接收/ADMIN_DISTRIBUTE教务分发/TEACHER_VIEW授课组查看',
  `stage_order`       int          NOT NULL DEFAULT 1      COMMENT '环节顺序（1–4）',
  `holder_id`         bigint       NULL                    COMMENT '当前责任人ID',
  `holder_role`       varchar(20)  NULL                    COMMENT '当前责任人角色：COUNSELOR/ADMIN/TEACHER',
  `entered_at`        datetime     NULL                    COMMENT '进入本环节时间',
  `deadline`          datetime     NULL                    COMMENT '本环节截止时间',
  `status`            varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING未开始/IN_PROGRESS进行中/DONE已完成/OVERDUE已超时',
  `done_at`           datetime     NULL                    COMMENT '完成时间',
  `note`              varchar(200) NULL                    COMMENT '说明',
  `remind_count`      int          NOT NULL DEFAULT 0      COMMENT '催办次数',
  `last_remind_time`  datetime     NULL                    COMMENT '最近催办时间',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_q_stage` (`questionnaire_id`, `stage`),
  KEY `idx_holder_status` (`holder_id`, `holder_role`, `status`),
  KEY `idx_status_deadline` (`status`, `deadline`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '问卷流转状态表（防 4 环节断链）';

-- =============================================================================
-- 附一：system_config 建议预置键（09 部分会给具体值）
--   archive.mid.week            期中归档周次（建议 9）
--   archive.final.week          期末归档周次（建议 17）
--   archive.window.days         归档窗口开放天数（建议 7）
--   archive.allow.admin.extend  是否允许教务延长窗口（true）
--   notify.remind.interval.days 归档催办间隔天数（3）
--   notify.remind.max.times     归档最多催办次数（3）
--   coverage.soft.check.weeks   期内软提醒检查间隔周数（2）
--   coverage.soft.zero.weeks    连续零录入多少周触发软提醒（4）
--   questionnaire.flow.timeout.days 问卷每个环节超时天数（7）
--   session.generate.weekday    课表默认上课星期（用于批量生成 course_session）
--   risk.dashboard.refresh.cron 风险状态批量刷新时间（如 每日 02:30）
-- 附二：审计必须覆盖的动作清单（实现时逐条对照）
--   成绩：录入/修改/批量导入/回滚/最终总评重算
--   考勤：点名提交/平时修改/归档提交/归档确认/退回/归档后更正/作废
--   开课：周数课次导入/实际课次申报与确认/及格线调整/摸底考标记
--   成绩构成：创建/修改/发起确认/全体确认/教务裁定锁定/L3 升级审批
--   干预：创建/修改成效/上报教务/教务处理/终止干预
--   其他：账号增删改与角色变更、改密、规则版本切换、系统配置修改
-- 附三：旧库映射
--   旧 teacher_management_log + alert_operation_log → 废弃，统一进 audit_log
--   旧 notification（1158 行）→ 结构重建（主体 + 回执）；旧行随旧库归档
--   问卷相关：旧库没有 → 全新建立
-- =============================================================================
