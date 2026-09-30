-- =============================================================================
-- 学情预警系统 v2 重构 · 第 5 部分：成绩层（4 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01 基础层、02 组织与开课（score_scheme_item）、03 选课（enrollment）
-- 本层回答四个问题：
--   ① 新生入学前的能力先验（student_admission_score → D 层）
--   ② 每个评分项每次拿了多少分（score_item → 分项级 Δ 的数据基础）
--   ③ 作业到底交没交、迟没迟（homework_item → 通道 B 的客观行为信号）
--   ④ 这门课最终算多少分、挂没挂、是不是缓考（score_result → 判定结果留档）
--
-- 【成绩计算链路（写代码时照此实现）】
--   score_item 明细
--     → 按 scheme_item.aggregate_mode（AVG/SUM/MAX/LAST）聚合成"项得分"
--     → 二级项按权重汇总到一级项，一级项按权重汇总成加权分 W = Σ(得分/满分×100×权重)
--     → 扣分 P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)   （来自考勤，实时算）
--     → 总评 T = W − P（无上限、不截断，可为负）
--     → 及格 ⟺ T ≥ pass_score 且 未触发 1/3 硬门槛
--     → 结果落 score_result（留档，供挂科率统计与历史对照）
--   ★折算分（Δ/c 折出来的）是"影子分数"：只活在风险引擎里，**绝不回写本层**
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. student_admission_score 高考/入学成绩（D 层先验）
--    说明：学生级数据，不随学期变化 ⇒ 本表是"业务表统一带 term_id"的唯一例外，
--          用 enroll_year（高考/入学年份）代替 term_id。
--    口径：一律用【位次百分位】比较，不比较原始分；物理组/历史组各自组内标准化
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `student_admission_score`;
CREATE TABLE `student_admission_score` (
  `id`                      bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`              bigint       NOT NULL                COMMENT '学生ID',
  `enroll_year`             int          NOT NULL                COMMENT '入学年份（=高考年份）',
  `province`                varchar(30)  NULL                    COMMENT '生源省份',
  `subject_group`           varchar(10)  NULL                    COMMENT '首选科目组：物理/历史（分组内标准化，绝不跨组比较）',
  `total_score`             decimal(6,1) NULL                    COMMENT '高考总分',
  `full_score`              decimal(6,1) NULL                    COMMENT '该省总分满分（用于折算，如 750）',
  `province_rank`           int          NULL                    COMMENT '省内位次',
  `province_candidate_count` int         NULL                    COMMENT '该省该科类考生总数（用于算位次百分位）',
  `rank_percentile`         decimal(6,3) NULL                    COMMENT '位次百分位(%) = 位次÷考生数×100（派生冗余，越小越靠前；跨省可比的唯一口径）',
  `subject_scores_json`     json         NULL                    COMMENT '单科成绩，如 {"语文":120,"数学":135}（基础课可做科目级映射；专业课不做）',
  `source`                  varchar(20)  NOT NULL DEFAULT 'ADMIN_IMPORT' COMMENT '来源：ADMIN_IMPORT教务导入/MANUAL手工补录',
  `import_batch_id`         bigint       NULL                    COMMENT '导入批次ID（对应 import_batch）',
  `remark`                  varchar(200) NULL,
  `create_by`               bigint       NULL,
  `create_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`               bigint       NULL,
  `update_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student` (`student_id`),
  KEY `idx_year_group` (`enroll_year`, `subject_group`),
  KEY `idx_batch` (`import_batch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '新生入学/高考成绩表（D 层先验，按位次百分位）';

-- -----------------------------------------------------------------------------
-- 2. score_item 分项得分明细（每个评分项、每次得分一行）
--    多次项：item 的 expected_times=9 → 这里就是 times_no 1..9 九行
--    通过制/等级制：raw_value 存原值（"通过"/"优秀"），score 存按 mapping_json 换算后的数值
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `score_item`;
CREATE TABLE `score_item` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enrollment_id`  bigint       NOT NULL                COMMENT '选课ID',
  `student_id`     bigint       NOT NULL                COMMENT '学生ID（由 enrollment 决定，写入时校验一致）',
  `scheme_item_id` bigint       NOT NULL                COMMENT '评分项ID（score_scheme_item）',
  `term_id`        bigint       NOT NULL                COMMENT '学期ID',
  `times_no`       int          NOT NULL DEFAULT 1      COMMENT '第几次（作业第几次、实验第几次；单次项恒为 1）',
  `raw_value`      varchar(20)  NULL                    COMMENT '原始取值（等级制/通过制用，如 优秀/通过/不通过）',
  `score`          decimal(5,1) NULL                    COMMENT '得分（等级制已按 mapping_json 换算为数值）',
  `full_score`     decimal(5,1) NULL                    COMMENT '该项满分（写入时快照，防止构成改动影响历史）',
  `is_absent`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否缺交/缺考：1是（区别于"尚未录入"；统计时按 0 分计）',
  `source`         varchar(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL老师录入/AUTO系统自动算（如作业均分）',
  `scorer_id`      bigint       NULL                    COMMENT '打分人ID',
  `scorer_role`    varchar(20)  NULL                    COMMENT '打分人角色：TEACHER/ADMIN/SYSTEM',
  `scored_at`      datetime     NULL                    COMMENT '打分时间',
  `is_void`        tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废',
  `void_reason`    varchar(200) NULL,
  `remark`         varchar(200) NULL,
  `create_by`      bigint       NULL,
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      bigint       NULL,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enroll_item_times` (`enrollment_id`, `scheme_item_id`, `times_no`),
  KEY `idx_enrollment` (`enrollment_id`),
  KEY `idx_item` (`scheme_item_id`),
  KEY `idx_term_student` (`term_id`, `student_id`),
  KEY `idx_scored_at` (`scored_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '分项得分明细表（分项级 Δ 的数据基础）';

-- -----------------------------------------------------------------------------
-- 3. homework_item 作业提交事实（客观行为信号，进通道 B）
--    与"得分"分开：得分在 score_item（scheme_item + times_no 对应），
--    这里只记"交没交、迟没迟" —— 这是跨班可比的客观行为，且不受老师打分松紧影响。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `homework_item`;
CREATE TABLE `homework_item` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enrollment_id`  bigint       NOT NULL                COMMENT '选课ID',
  `student_id`     bigint       NOT NULL                COMMENT '学生ID',
  `offering_id`    bigint       NOT NULL                COMMENT '开课ID',
  `term_id`        bigint       NOT NULL                COMMENT '学期ID',
  `scheme_item_id` bigint       NULL                    COMMENT '对应评分项ID（作业类项；若作业不计分可空）',
  `homework_no`    int          NOT NULL                COMMENT '第几次作业',
  `title`          varchar(100) NULL                    COMMENT '作业标题',
  `assigned_date`  date         NULL                    COMMENT '布置日期',
  `due_date`       date         NULL                    COMMENT '截止日期',
  `submit_status`  varchar(20)  NOT NULL DEFAULT 'MISSING' COMMENT '提交状态：ON_TIME按时/LATE迟交/MISSING未交/EXEMPT免交',
  `submit_time`    datetime     NULL                    COMMENT '实际提交时间',
  `is_late`        tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否迟交：1是',
  `remark`         varchar(200) NULL,
  `create_by`      bigint       NULL,
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      bigint       NULL,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enroll_hw` (`enrollment_id`, `homework_no`),
  KEY `idx_enrollment_status` (`enrollment_id`, `submit_status`),
  KEY `idx_offering` (`offering_id`),
  KEY `idx_due` (`due_date`),
  KEY `idx_term_student` (`term_id`, `student_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '作业提交事实表（通道 B 客观行为信号）';

-- -----------------------------------------------------------------------------
-- 4. score_result 最终成绩单（判定结果留档）
--    为什么落库：挂科率三方统计、历史对照、以及"当时到底按什么算的"必须可复现；
--               ★折算分不在这里（影子分数不落库）。
--    ★缓考口径（★12）：缓考照常预警（预警发生在期末考前）；缓考只影响结算 ——
--      score_status=DEFERRED 期间不进"成绩分母"，等成绩齐了再算最终总评。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `score_result`;
CREATE TABLE `score_result` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enrollment_id`     bigint       NOT NULL                COMMENT '选课ID',
  `student_id`        bigint       NOT NULL                COMMENT '学生ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `weighted_score`    decimal(6,2) NULL                    COMMENT '加权分 W = Σ(得分/满分×100×权重)',
  `deduction_points`  decimal(6,2) NULL                    COMMENT '扣分 P = 2×缺课 + 1×迟到 + 1×max(0,请假−3)（无上限，不截断）',
  `total_score`       decimal(6,2) NULL                    COMMENT '总评 T = W − P（可为负）',
  `pass_score`        int          NULL                    COMMENT '判定时适用的及格线（快照）',
  `absent_count`      int          NOT NULL DEFAULT 0      COMMENT '判定时的缺课次数（快照，1/3 门槛的分子）',
  `late_count`        int          NOT NULL DEFAULT 0      COMMENT '判定时的迟到次数（快照）',
  `leave_count`       int          NOT NULL DEFAULT 0      COMMENT '判定时的请假次数（快照）',
  `allowed_absent`    int          NULL                    COMMENT '允许缺课上限 N_max = floor(分母课次/3)（快照）',
  `effective_sessions` int         NULL                    COMMENT '判定时使用的分母课次（教务确认后的实际课次快照）',
  `hard_fail`         tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否触发 1/3 硬门槛：1是（不看分数，直接挂科）',
  `is_fail`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否挂科：T < 及格线 或 触发硬门槛',
  `fail_reason`       varchar(30)  NULL                    COMMENT '挂科原因：SCORE分数不足/HARD_LINE缺课超1/3/NO_SCORE无成绩',
  `score_status`      varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '成绩状态：PENDING待定/FINAL已定/DEFERRED缓考/DEFERRED_GIVEUP缓考后弃考/ABSENT_EXAM旷考/NO_SCORE成绩未齐',
  `retake_times`      int          NOT NULL DEFAULT 0      COMMENT '重修次数（0首次）',
  `engine_version`    varchar(20)  NULL                    COMMENT '计算所用引擎版本（规则/权重/公式变化即升版）',
  `calculated_at`     datetime     NULL                    COMMENT '本次计算结果时间',
  `is_void`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废（退课、重算覆盖等）',
  `remark`            varchar(200) NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enrollment` (`enrollment_id`),
  KEY `idx_offering_fail` (`offering_id`, `is_fail`, `is_void`),
  KEY `idx_student_term` (`student_id`, `term_id`),
  KEY `idx_term_status` (`term_id`, `score_status`),
  KEY `idx_hard_fail` (`hard_fail`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '最终成绩单（判定结果留档；折算分不落库）';

-- =============================================================================
-- 附一：缓考 / 旷考 / 弃考 / 退课 的处理规则（★12、★14）
--   缓考 DEFERRED            ：照常预警、照常进通道 A/B；只在"挂科率统计的分母"里等成绩
--   缓考后弃考 DEFERRED_GIVEUP：直接挂科（不管平时分多高，没成绩不可能够及格线）
--   旷考 ABSENT_EXAM          ：挂科（学生自身原因）
--   退课                      ：enrollment.status=WITHDRAWN → 本表记录 is_void=1，
--                              相关预警/风险状态一并作废，学生端显示"经教务批准已退课"
--   重修                      ：enrollment.status=REPEAT + retake_times，统计上不作特殊排除（★13）
-- 附二：挂科率统计的分母（口径写死）
--   分母 = 教务二次确认的"实际考核人数"（exam_headcount），
--          扣除 score_status ∈ (DEFERRED, NO_SCORE) 的记录；分子 = is_fail=1 且 is_void=0
--   三方粒度：教学班→老师 / 行政班→导员 / 课程→入历史（按人数加权，不简单取平均）
-- 附三：旧库映射
--   旧 score_info（usual_score/mid_score/final_score/comprehensive_score，534 行）
--     → 拆成：score_scheme_item 的一级项（平时/期中/期末）+ score_item 明细 + score_result
--     注意：旧库期中占比为 0（50/0/50），迁移时为这两门课只建 平时/期末 两项
--   旧 homework_info（submit_count/not_submit_count/late_submit_count/score_list，534 行）
--     → homework_item（逐次提交事实）+ score_item（逐次得分，来自 score_list 拆分）
--   旧 student.history_risk（旧口径派生分）→ 废弃；新口径改用 student_fail_history（06 部分）
--   旧库没有高考成绩 → student_admission_score 全新建，由教务导入
-- =============================================================================
