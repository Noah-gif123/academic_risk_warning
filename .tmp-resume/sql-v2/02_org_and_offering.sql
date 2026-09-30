-- =============================================================================
-- 学情预警系统 v2 重构 · 第 2 部分：组织与开课层（7 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01_base_layer.sql（term / course / major / teacher / administrative_class）
-- 依据：★20 考勤不得进评分项 ｜ ★21 默认 L2、L3 由教务批准
--       ★22 课程类型模板 ｜ ★23 老师平级 ⇒ 发起+全体确认+教务兜底裁定
-- 本层回答三个问题：
--   ① 这门课这学期由谁教、带哪些行政班（course_group / offering / teaching_class）
--   ② 这门课这学期上几周、几次课、及格线多少、有没有摸底考（course_offering）
--   ③ 这门课这学期怎么算分（score_template / score_scheme / score_scheme_item）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. course_group 课程组（课程 × 学期）
--    说明：① 无组长（老师平级），成员由本学期该课程下的 teaching_class 老师派生；
--          ② 层级 L1/L2/L3 与"升级 L3 的教务审批"在这里；
--          ③ 构成锁定状态不放这里（放 score_scheme，避免两套真相）。
--    与 course_offering 是 1:1（都按 course×term）；分开是因为：
--          课程组属"教学组织 + 层级审批"，开课属"教务排课 + 课次/及格线"，
--          录入人、录入时点、变更频率都不同。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_group`;
CREATE TABLE `course_group` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '课程组ID',
  `course_id`            bigint       NOT NULL                COMMENT '课程ID（科目）',
  `term_id`              bigint       NOT NULL                COMMENT '学期ID',
  `level`                varchar(10)  NOT NULL DEFAULT 'L2'    COMMENT '构成层级：L1只填三项总分/L2平时拆项(默认)/L3自定义项(实验、三四级项目等)',
  `level_request`        varchar(10)  NULL                    COMMENT '申请的层级（老师发起升级时填写）',
  `level_request_status` varchar(20)  NOT NULL DEFAULT 'NONE'  COMMENT '升级申请状态：NONE无/PENDING待教务审批/APPROVED已批准/REJECTED已驳回',
  `level_request_by`     bigint       NULL                    COMMENT '申请人（教师ID）',
  `level_request_time`   datetime     NULL                    COMMENT '申请时间',
  `level_request_reason` varchar(200) NULL                    COMMENT '申请理由（如：本课含 5 次实验 + 三级项目）',
  `level_approve_by`     bigint       NULL                    COMMENT '审批人（教务ID）',
  `level_approve_time`   datetime     NULL                    COMMENT '审批时间',
  `level_approve_note`   varchar(200) NULL                    COMMENT '审批意见',
  `status`               varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED',
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_term` (`course_id`, `term_id`),
  KEY `idx_term` (`term_id`),
  KEY `idx_level_request` (`level_request_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课程组表（课程×学期，无组长）';

-- -----------------------------------------------------------------------------
-- 2. course_offering 开课实例（课程 × 学期）
--    核心：计划课次（教务开学前导入锁定）+ 实际课次（老师期末申报、教务确认）
--          effective_sessions = 判定用分母（1/3 硬门槛与覆盖率终值都用它）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_offering`;
CREATE TABLE `course_offering` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '开课ID',
  `course_id`           bigint       NOT NULL                COMMENT '课程ID',
  `term_id`             bigint       NOT NULL                COMMENT '学期ID',
  `group_id`            bigint       NOT NULL                COMMENT '课程组ID',
  `planned_weeks`       int          NOT NULL DEFAULT 16     COMMENT '计划教学周数（教务开学前导入）',
  `planned_times_per_week` int       NOT NULL DEFAULT 2      COMMENT '计划每周课次（教务开学前导入）',
  `planned_sessions`    int          NOT NULL DEFAULT 0      COMMENT '计划总课次 = 计划周数 × 每周课次（教务锁定后不变）',
  `actual_weeks`        int          NULL                    COMMENT '实际教学周数（老师期末申报：因节假日调休等的真实值）',
  `actual_sessions`     int          NULL                    COMMENT '实际总课次（老师期末申报）',
  `actual_submit_status` varchar(20) NOT NULL DEFAULT 'NONE'  COMMENT '实际课次申报状态：NONE未申报/PENDING待教务确认/CONFIRMED已确认/RETURNED已退回',
  `actual_submit_by`    bigint       NULL                    COMMENT '申报人（教师ID）',
  `actual_submit_time`  datetime     NULL                    COMMENT '申报时间',
  `actual_submit_note`  varchar(200) NULL                    COMMENT '申报说明（如：国庆放假一周、校运会停课一次）',
  `actual_confirm_by`   bigint       NULL                    COMMENT '确认人（教务ID）',
  `actual_confirm_time` datetime     NULL                    COMMENT '确认时间',
  `effective_sessions`  int          NOT NULL DEFAULT 0      COMMENT '判定用分母课次：未确认前=计划课次，确认后=实际课次（1/3 门槛与覆盖率共用）',
  `effective_source`    varchar(20)  NOT NULL DEFAULT 'PLANNED' COMMENT '分母来源：PLANNED计划/ACTUAL教务确认后的实际课次',
  `pass_score`          int          NOT NULL DEFAULT 60     COMMENT '及格线（本学期该课适用）',
  `has_placement_test`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '本学期有无摸底考（教务在开课时标记；有则用作 A′ 锚点）',
  `status`              varchar(20)  NOT NULL DEFAULT 'PLANNING' COMMENT '状态：PLANNING筹备/ONGOING进行中/FINISHED已结束/ARCHIVED已归档',
  `remark`              varchar(200) NULL,
  `create_by`           bigint       NULL                    COMMENT '创建人（教务ID）',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           bigint       NULL,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_term` (`course_id`, `term_id`),
  KEY `idx_term_status` (`term_id`, `status`),
  KEY `idx_group` (`group_id`),
  KEY `idx_actual_status` (`actual_submit_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '开课实例表（计划/实际课次、及格线、摸底考标记）';

-- -----------------------------------------------------------------------------
-- 3. teaching_class 教学班（一个教学班只有 1 位老师）
--    is_retake_class：重修班标记，仅用于教务端筛选与展示（★13：算法不做特殊排除）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teaching_class`;
CREATE TABLE `teaching_class` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '教学班ID',
  `offering_id`     bigint       NOT NULL                COMMENT '开课ID',
  `teacher_id`      bigint       NOT NULL                COMMENT '任课教师ID（一个教学班只有一位老师）',
  `class_code`      varchar(30)  NOT NULL                COMMENT '教学班编码，如 CS101-01',
  `class_name`      varchar(50)  NULL                    COMMENT '教学班名称，如 计算机基础2-01班',
  `capacity`        int          NULL                    COMMENT '容量（可选）',
  `is_retake_class` tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否重修班：1是（仅展示与筛选用，统计上不作特殊处理）',
  `status`          varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED',
  `create_by`       bigint       NULL,
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`       bigint       NULL,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offering_class` (`offering_id`, `class_code`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_retake` (`is_retake_class`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教学班表（1 位老师）';

-- -----------------------------------------------------------------------------
-- 4. teaching_class_admin_class 教学班 ↔ 行政班（多对多）
--    对应你大纲原型：李老师带软件1/2/3班、王老师带软件4/5/6班 …
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teaching_class_admin_class`;
CREATE TABLE `teaching_class_admin_class` (
  `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint   NOT NULL                COMMENT '教学班ID',
  `admin_class_id`    bigint   NOT NULL                COMMENT '行政班ID',
  `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tc_class` (`teaching_class_id`, `admin_class_id`),
  KEY `idx_admin_class` (`admin_class_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教学班-行政班关联表';

-- -----------------------------------------------------------------------------
-- 5. score_template 成绩构成模板（按课程类型预置，课程组选模板后微调）
--    items_json 结构示例（两级）：
--    [
--      {"code":"USUAL","name":"平时","weight":40,"children":[
--          {"code":"HW","name":"课后作业","weight":40,"dataSource":"AUTO","expectedTimes":9,"aggregateMode":"AVG"},
--          {"code":"QUIZ","name":"课堂测试","weight":30,"dataSource":"MANUAL","expectedTimes":3,"aggregateMode":"AVG"},
--          {"code":"PERF","name":"课堂表现","weight":15,"dataSource":"MANUAL","isSubjective":true},
--          {"code":"EXP","name":"实验报告","weight":15,"dataSource":"MANUAL","expectedTimes":5,"aggregateMode":"AVG"}]},
--      {"code":"PROJECT","name":"三级项目","weight":20,"children":[
--          {"code":"PROJ_DOC","name":"项目文档","weight":40,"dataSource":"MANUAL"},
--          {"code":"PROJ_DEF","name":"项目答辩","weight":60,"dataSource":"MANUAL","isSubjective":true}]},
--      {"code":"FINAL","name":"期末","weight":40,"children":[
--          {"code":"FINAL_EXAM","name":"期末闭卷","weight":100,"dataSource":"MANUAL","isAnchor":true,"anchorLayer":"A"}]}
--    ]
--    注意：模板里不允许出现"考勤类"项（★20）。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `score_template`;
CREATE TABLE `score_template` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code` varchar(30)  NOT NULL                COMMENT '模板编码，如 EXAM_DEFAULT',
  `template_name` varchar(50)  NOT NULL                COMMENT '模板名称，如 考试课默认构成',
  `course_type`   varchar(20)  NOT NULL                COMMENT '适用课程类型：EXAM/ASSESS/EXPERIMENT/PROJECT',
  `level`         varchar(10)  NOT NULL DEFAULT 'L2'    COMMENT '适用层级：L1/L2/L3',
  `items_json`    json         NULL                    COMMENT '评分项定义（两级，结构见上方注释）',
  `description`   varchar(200) NULL                    COMMENT '说明',
  `is_default`    tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否该课程类型的默认模板',
  `status`        varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  `create_by`     bigint       NULL,
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     bigint       NULL,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_type_level` (`course_type`, `level`, `is_default`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '成绩构成模板表';

-- -----------------------------------------------------------------------------
-- 6. score_scheme 成绩构成头（一门课一套构成，课程组共用）
--    ★23 老师平级：任一老师发起 → 全体任课老师确认 → 锁定；
--         超时未确认 / 有分歧 → 教务裁定锁定（lock_type=ADMIN_RULED）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `score_scheme`;
CREATE TABLE `score_scheme` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '构成ID',
  `group_id`          bigint       NOT NULL                COMMENT '课程组ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID（业务表统一带）',
  `version`           int          NOT NULL DEFAULT 1      COMMENT '版本号（同一课程组内自增，最新版为当前生效版）',
  `level`             varchar(10)  NOT NULL DEFAULT 'L2'    COMMENT '锁定时的层级快照（L1/L2/L3）',
  `source_template_id` bigint      NULL                    COMMENT '来源模板ID（可空）',
  `total_weight`      decimal(5,2) NOT NULL DEFAULT 100.00 COMMENT '一级项权重合计（锁定前必须=100.00）',
  `confirm_status`    varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '确认状态：DRAFT草稿/CONFIRMING确认中/CONFIRMED已确认/LOCKED已锁定',
  `confirm_total`     int          NOT NULL DEFAULT 0      COMMENT '应确认人数（该课程组本学期任课老师数）',
  `confirm_done`      int          NOT NULL DEFAULT 0      COMMENT '已确认人数',
  `initiator_id`      bigint       NULL                    COMMENT '发起人（教师ID）',
  `confirmed_by_json` json         NULL                    COMMENT '确认明细：[{"teacherId":1,"teacherName":"李老师","time":"..."}]',
  `locked`            tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已锁定：1已锁（学期内不可改）',
  `locked_by`         bigint       NULL                    COMMENT '锁定操作人',
  `locked_at`         datetime     NULL                    COMMENT '锁定时间',
  `lock_type`         varchar(20)  NULL                    COMMENT '锁定方式：TEACHER_COLLECTIVE全体确认/ADMIN_RULED教务裁定/TEMPLATE_AUTO模板默认',
  `remark`            varchar(200) NULL                    COMMENT '备注（如教务裁定原因）',
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_version` (`group_id`, `version`),
  KEY `idx_group_locked` (`group_id`, `locked`),
  KEY `idx_confirm_status` (`confirm_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '成绩构成表（课程组共用，锁定后不可改）';

-- -----------------------------------------------------------------------------
-- 7. score_scheme_item 评分项定义（两级：一级项 → 二级项）
--    ★20 硬规则：category 不允许 'ATTENDANCE'（考勤只走扣分 P 与 1/3 硬门槛）
--    ★对算法的意义：
--      is_anchor=1        → 通道 C 的 A 层锚点（摸底考标 A_PRIME）
--      is_subjective=1    → 分项级 Δ 只在这些项上估计
--      kp_id              → 薄弱知识点 / 共识难点对比
--      data_source=AUTO   → 客观、跨班可比，进通道 B
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `score_scheme_item`;
CREATE TABLE `score_scheme_item` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '评分项ID',
  `scheme_id`      bigint       NOT NULL                COMMENT '构成ID',
  `parent_id`      bigint       NULL                    COMMENT '父项ID（NULL=一级项；非空=二级项）',
  `item_level`     tinyint      NOT NULL DEFAULT 1      COMMENT '层级：1一级项 2二级项',
  `item_code`      varchar(30)  NOT NULL                COMMENT '评分项编码（同一构成内唯一），如 USUAL / HW / FINAL_EXAM',
  `item_name`      varchar(50)  NOT NULL                COMMENT '评分项名称，如 课后作业 / 三级项目答辩',
  `category`       varchar(30)  NOT NULL                COMMENT '大类：HOMEWORK作业/QUIZ课堂测试/PERFORMANCE课堂表现/EXPERIMENT实验/PROJECT项目/PRACTICE实践/MIDTERM期中/FINAL期末/PLACEMENT摸底考/OTHER其他',
  `weight`         decimal(5,2) NOT NULL DEFAULT 0.00   COMMENT '权重(%)：一级项占最终总评、二级项占其父项；同级合计必须=100',
  `full_score`     decimal(5,1) NOT NULL DEFAULT 100.0  COMMENT '该项满分',
  `data_source`    varchar(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源：AUTO系统自动算（作业/机考）/MANUAL老师录入',
  `score_type`     varchar(20)  NOT NULL DEFAULT 'NUMERIC' COMMENT '分数形态：NUMERIC数值/PASS_FAIL通过制/GRADE等级制',
  `mapping_json`   json         NULL                    COMMENT '分数映射（等级制/通过制→数值），如 {"优秀":95,"良好":85,"通过":85,"不通过":50}',
  `expected_times` int          NOT NULL DEFAULT 1      COMMENT '预计次数（如作业 9 次、实验 5 次）',
  `aggregate_mode` varchar(20)  NOT NULL DEFAULT 'AVG'  COMMENT '多次项汇总方式：AVG均值/SUM累加/MAX最高/LAST最近一次',
  `is_anchor`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否锚点项：1是（统考=A层，摸底考=A′层）',
  `anchor_layer`   varchar(10)  NULL                    COMMENT '锚点层级：A统一命题闭卷/A_PRIME摸底考/NONE无',
  `is_subjective`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否主观项：1是（分项级 Δ 只在主观项上估计）',
  `kp_id`          bigint       NULL                    COMMENT '关联知识点ID（用于薄弱知识点与共识难点对比）',
  `scorer_role`    varchar(20)  NOT NULL DEFAULT 'TEACHER' COMMENT '打分主体：TEACHER/ADMIN/SYSTEM',
  `sort_no`        int          NOT NULL DEFAULT 0      COMMENT '排序号',
  `status`         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  `create_by`      bigint       NULL,
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      bigint       NULL,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheme_item` (`scheme_id`, `item_code`),
  KEY `idx_scheme_parent` (`scheme_id`, `parent_id`),
  KEY `idx_category` (`scheme_id`, `category`),
  KEY `idx_anchor` (`scheme_id`, `is_anchor`),
  KEY `idx_kp` (`kp_id`),
  CONSTRAINT `ck_item_no_attendance` CHECK (`category` <> 'ATTENDANCE')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '评分项定义表（两级；考勤不得入内）';

-- =============================================================================
-- 附一：三个必须由后端校验、DDL 无法表达的规则
--   R1 同级权重和 = 100.00（score_scheme.total_weight + 各 parent 下子项合计）
--   R2 L1 只允许 平时/期中/期末 三项一级项（无二级项）
--      L2 允许 平时 下挂 作业/测试/表现（二级项）
--      L3 允许自定义一级项（项目/实验等），仍是两级
--   R3 锁定动作：confirm_done = confirm_total 才可 TEACHER_COLLECTIVE 锁定；
--      超时（由 system_config 配置天数）未确认 → 教务可 ADMIN_RULED 直接锁定
-- 附二：模板/构成层级与 course_type 的默认对应（08 部分预置数据会给具体值）
--   考试课 EXAM      → L2：平时40(作业40/测试30/表现15/实验15) + 期中20 + 期末40
--   考查课 ASSESS    → L2：平时60(作业40/测试30/表现30) + 期末40
--   实验课 EXPERIMENT→ L3：平时30(实验报告60/操作20/表现20) + 实验考核40 + 期末30
--   项目课 PROJECT   → L3：平时20(考勤外：文档/汇报) + 三级项目50 + 答辩30
-- 附三：旧库映射
--   teacher_class（ID 区间切人）→ 废弃，改由 teaching_class + teaching_class_admin_class 表达
--   旧 course.usual_ratio/mid_ratio/final_ratio → 转成 score_scheme_item 的一级项权重
--   旧 course.total_class_times → course_offering.planned_sessions
-- =============================================================================
