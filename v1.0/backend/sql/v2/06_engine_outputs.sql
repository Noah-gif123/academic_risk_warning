-- =============================================================================
-- 学情预警系统 v2 重构 · 第 6 部分：引擎产物（7 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01/02/03/04/05 全部
-- 本层是"算法算完往哪里放"：
--   ① 引擎参数（版本化）：risk_engine_config
--   ② 当前风险状态（替代已删除的每日快照）：risk_state
--   ③ 教师尺度 Δ（跨班可比的关键量）：teacher_style
--   ④ 学生跨学期成绩/挂科历史（大纲第 4 维）：student_fail_history
--   ⑤⑥⑦ 三级挂科率（教学班→老师 / 行政班→导员 / 课程→入历史）
--
-- 【每日快照为什么删掉、用什么替代】
--   每日全量快照（旧 alert_snapshot，604 行/天）已删除；"这个学生现在几分"
--   改由 risk_state 承载：一人一课一行、覆盖式更新。
--   "什么时候变危险"不再靠快照曲线，而由预警时间线（alert_flow_log，07 部分）追溯。
--   伴随修正：干预"无效"的判定窗口 = 下次 risk_state 刷新 / 最长 2 周（原写"下次快照"）。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. risk_engine_config 引擎配置（版本化：规则/权重/公式任一变化即升版）
--    全库只允许一条 is_active=1；历史版本只读保留，便于复现旧结果
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `risk_engine_config`;
CREATE TABLE `risk_engine_config` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `engine_version`      varchar(20)  NOT NULL                COMMENT '引擎版本号，如 v2.0.0；规则/权重/公式变化即升版',
  `effective_time`      datetime     NOT NULL                COMMENT '生效时间',
  `is_active`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否当前生效版本（全库仅一条为1）',
  `channel_a_enabled`   tinyint(1)   NOT NULL DEFAULT 1      COMMENT '通道A（确定性风险）开关',
  `channel_b_enabled`   tinyint(1)   NOT NULL DEFAULT 1      COMMENT '通道B（班内相对）开关',
  `channel_c_enabled`   tinyint(1)   NOT NULL DEFAULT 0      COMMENT '通道C（跨班可比，依赖Δ）开关',
  `signal_weights_json` json         NULL                    COMMENT '个体信号权重：{"freshman":{"attendance":0.40,"homework":0.30,"score":0.25,"subjective":0.05},"normal":{"attendance":0.30,"homework":0.25,"score":0.30,"subjective":0.15}}',
  `delta_enabled`       tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否启用教师尺度 Δ 归一化',
  `course_factor_enabled` tinyint(1) NOT NULL DEFAULT 1      COMMENT '是否启用课程因子 c 归一化',
  `threshold_red`       decimal(5,1) NOT NULL DEFAULT 75.0   COMMENT '风险分档：红 ≥',
  `threshold_orange`    decimal(5,1) NOT NULL DEFAULT 60.0   COMMENT '橙 60–75',
  `threshold_yellow`    decimal(5,1) NOT NULL DEFAULT 40.0   COMMENT '黄 40–60（绿 <40）',
  `coverage_threshold`  decimal(4,3) NOT NULL DEFAULT 0.500  COMMENT '点名覆盖率阈值（低于此值：提醒+置信度判低）',
  `coverage_soft_weeks` int          NOT NULL DEFAULT 4      COMMENT '期内软提醒：连续 N 周零录入即提醒',
  `counselor_week_start` int         NOT NULL DEFAULT 5      COMMENT '导员班集体提醒起始周（第1–4周不触发）',
  `counselor_rate_l1`   decimal(4,3) NOT NULL DEFAULT 0.300  COMMENT '班集体提醒档位1：预警占比 ≥',
  `counselor_min_l1`    int          NOT NULL DEFAULT 5      COMMENT '档位1：绝对人数 ≥',
  `counselor_rate_l2`   decimal(4,3) NOT NULL DEFAULT 0.500  COMMENT '档位2（升级）：占比 ≥',
  `counselor_min_l2`    int          NOT NULL DEFAULT 10     COMMENT '档位2：绝对人数 ≥',
  `counselor_risk_floor` decimal(5,1) NOT NULL DEFAULT 40.0  COMMENT '导员端纳入范围：风险分 ≥（≥40 黄橙红进名单；绿不出现）',
  `multi_course_threshold` int       NOT NULL DEFAULT 3      COMMENT '多科目预警门限（≥3 门触发"重点关注该学生"）',
  `invalid_check_days`  int          NOT NULL DEFAULT 14     COMMENT '干预无效判定最长观察天数（或下次 risk_state 刷新，先到为准）',
  `deduct_absent`       decimal(4,1) NOT NULL DEFAULT 2.0    COMMENT '扣分系数：每缺课一次',
  `deduct_late`         decimal(4,1) NOT NULL DEFAULT 1.0    COMMENT '扣分系数：每迟到一次',
  `deduct_leave`        decimal(4,1) NOT NULL DEFAULT 1.0    COMMENT '扣分系数：超免扣次数后每次请假',
  `leave_free_times`    int          NOT NULL DEFAULT 3      COMMENT '请假免扣次数（每学期每门课）',
  `final_full_score`    decimal(5,1) NOT NULL DEFAULT 100.0  COMMENT '期末满分（"期末需考 x > 此值 → 实际不可达"的硬上限）',
  `delta_shrink_k`      int          NOT NULL DEFAULT 60     COMMENT 'Δ 收缩常数 k：λ = n/(n+k)',
  `delta_min_sample`    int          NOT NULL DEFAULT 30     COMMENT 'Δ 可用最小样本量（低于则 λ<0.33，视为不可用）',
  `delta_fuse_threshold` decimal(5,1) NOT NULL DEFAULT 20.0  COMMENT '|Δ| 熔断阈值：超过则停用该 Δ 并推教务复核',
  `confidence_min_ok`   decimal(4,3) NOT NULL DEFAULT 0.400  COMMENT '置信度硬规则：覆盖率达标且锚点为A/A′时，W 不得低于此值',
  `remark`              varchar(200) NULL,
  `create_by`           bigint       NULL,
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           bigint       NULL,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_engine_version` (`engine_version`),
  KEY `idx_active` (`is_active`, `effective_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '风险引擎配置表（版本化）';

-- -----------------------------------------------------------------------------
-- 2. risk_state 当前风险状态（替代每日快照；一人一课一行，覆盖式更新）
--    三通道结果 + 置信度 + 输出形态；折算分不回写成绩表（影子分数）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `risk_state`;
CREATE TABLE `risk_state` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enrollment_id`        bigint       NOT NULL                COMMENT '选课ID（一人一课一行）',
  `student_id`           bigint       NOT NULL                COMMENT '学生ID',
  `offering_id`          bigint       NOT NULL                COMMENT '开课ID',
  `teaching_class_id`    bigint       NOT NULL                COMMENT '教学班ID',
  `term_id`              bigint       NOT NULL                COMMENT '学期ID',
  -- ===== 通道 A：确定性风险（规则，100% 可信）=====
  `absent_count`         int          NOT NULL DEFAULT 0      COMMENT 'A通道：当前累计缺课次数',
  `late_count`           int          NOT NULL DEFAULT 0      COMMENT 'A通道：当前累计迟到次数',
  `leave_count`          int          NOT NULL DEFAULT 0      COMMENT 'A通道：当前累计请假次数',
  `allowed_absent`       int          NULL                    COMMENT 'A通道：允许缺课上限 N_max = floor(分母课次/3)',
  `remaining_absent`     int          NULL                    COMMENT 'A通道：剩余可缺次数 = N_max − 已缺；0=再缺一次即挂；≤−1=必然挂科',
  `hard_fail`            tinyint(1)   NOT NULL DEFAULT 0      COMMENT 'A通道：是否已触发 1/3 硬门槛',
  `near_fail`            tinyint(1)   NOT NULL DEFAULT 0      COMMENT 'A通道：是否高危（剩余可缺 ≤ 1）',
  `final_exam_needed`    decimal(6,1) NULL                    COMMENT 'A通道：期末需考分数 x = (pass_score + P − W_其余项)/r_f',
  `final_unreachable`    tinyint(1)   NOT NULL DEFAULT 0      COMMENT 'A通道：x > 期末满分 → 实际不可达',
  `channel_a_level`      varchar(20)  NULL                    COMMENT 'A通道等级：SAFE正常/WATCH警戒/DANGER再缺一次即挂/FAIL必然挂科',
  -- ===== 通道 B：班内相对（同班同尺度）=====
  `channel_b_score`      decimal(5,2) NULL                    COMMENT 'B通道：班内相对风险分（0–100）',
  `channel_b_rank`       int          NULL                    COMMENT 'B通道：班内风险排名（1 最危险）',
  `channel_b_class_size` int          NULL                    COMMENT 'B通道：同班人数',
  `channel_b_level`      varchar(20)  NULL                    COMMENT 'B通道等级：SAME_AS_A 语义不混算，仅作分档展示',
  -- ===== 通道 C：跨班可比（Δ + c 折算后）=====
  `channel_c_score`      decimal(6,2) NULL                    COMMENT 'C通道：折算后的跨班可比风险分（影子分数，不落成绩表）',
  `delta_used`           decimal(5,2) NULL                    COMMENT 'C通道：本次使用的教师尺度 Δ̂（收缩后）',
  `course_factor_used`   decimal(5,3) NULL                    COMMENT 'C通道：本次使用的课程因子 c',
  `channel_c_level`      varchar(20)  NULL                    COMMENT 'C通道等级：RED/ORANGE/YELLOW/GREEN（按 engine 阈值分档）',
  -- ===== 综合输出（不合并三通道为一个真相，而是标明"当前采用哪条"）=====
  `risk_score`           decimal(5,1) NULL                    COMMENT '当前对外风险分（有 C 用 C，无 C 用 B，A 单列展示）',
  `risk_level`           varchar(10)  NULL                    COMMENT '当前等级：RED/ORANGE/YELLOW/GREEN',
  `active_channel`       varchar(10)  NULL                    COMMENT '当前生效通道：A/B/C（教务看C、导员看A+B）',
  `confidence`           decimal(4,3) NULL                    COMMENT '置信度 W = 锚点强度 × 数据完备度 × 样本充足度（0–1）',
  `confidence_level`     varchar(20)  NULL                    COMMENT '置信度档：HIGH/MEDIUM/LOW/VERY_LOW',
  `output_mode`          varchar(20)  NULL                    COMMENT '输出形态：FULL_SCORE完整分档/SCORE_WITH_NOTE带标注/BEHAVIOR_ONLY仅行为预警/INSUFFICIENT数据不足',
  `data_quality_json`    json         NULL                    COMMENT '数据质量明细：{"coverage":0.62,"anchorStrength":0.8,"sampleSize":120,"attendanceSourceMix":"NORMAL","missing":["mid_score"]}',
  `trigger_source`       varchar(20)  NULL                    COMMENT '本次刷新来源：EVALUATION评估/DATA_CHANGE数据变更/MANUAL人工/WITHDRAW退课作废',
  `last_eval_time`       datetime     NULL                    COMMENT '最近一次刷新时间（干预"无效判定"以此为窗口起点之一）',
  `engine_version`       varchar(20)  NULL                    COMMENT '计算所用引擎版本',
  `is_void`              tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废（退课/重算）：1作废，不进任何统计与列表',
  `void_reason`          varchar(200) NULL,
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enrollment` (`enrollment_id`),
  KEY `idx_student_void` (`student_id`, `is_void`),
  KEY `idx_offering_level` (`offering_id`, `risk_level`),
  KEY `idx_class_level` (`teaching_class_id`, `risk_level`),
  KEY `idx_term_level` (`term_id`, `risk_level`),
  KEY `idx_eval_time` (`last_eval_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '当前风险状态表（替代每日快照；一人一课一行）';

-- -----------------------------------------------------------------------------
-- 3. teacher_style 教师尺度 Δ（分项级：作业松、考试严）
--    粒度：教师 × 开课 × 评分项；整体级时 item_code = 'OVERALL'
--    收缩：λ = n/(n+k)，Δ̂ = Δ × λ；|Δ̂| 超阈值 → 熔断（FUSED）并推教务复核
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teacher_style`;
CREATE TABLE `teacher_style` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teacher_id`     bigint       NOT NULL                COMMENT '教师ID',
  `offering_id`    bigint       NOT NULL                COMMENT '开课ID（Δ 是"该教师在该课"的尺度）',
  `course_id`      bigint       NOT NULL                COMMENT '课程ID',
  `term_id`        bigint       NOT NULL                COMMENT '学期ID',
  `scheme_item_id` bigint       NULL                    COMMENT '评分项ID（分项级 Δ；整体级为空）',
  `item_code`      varchar(30)  NOT NULL DEFAULT 'OVERALL' COMMENT '评分项编码；整体级固定 OVERALL',
  `raw_delta`      decimal(5,2) NULL                    COMMENT '未收缩的原始 Δ（班 vs 课程平均）',
  `delta`          decimal(5,2) NULL                    COMMENT '收缩后的 Δ̂ = raw_delta × λ（实际用于折算）',
  `shrink_lambda`  decimal(4,3) NULL                    COMMENT '收缩系数 λ = n/(n+k)，k 见 risk_engine_config',
  `sample_size`    int          NOT NULL DEFAULT 0      COMMENT '参与估计的样本量（学生数）',
  `std_dev`        decimal(5,2) NULL                    COMMENT '样本标准差（异常检测用）',
  `fused`          tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否熔断：1停用该 Δ（|Δ| 超阈值，等教务复核）',
  `fuse_reason`    varchar(200) NULL                    COMMENT '熔断原因/复核说明',
  `review_by`      bigint       NULL                    COMMENT '复核人（教务ID）',
  `review_time`    datetime     NULL                    COMMENT '复核时间',
  `status`         varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE可用/INSUFFICIENT样本不足/FUSED已熔断/REVIEWED复核通过',
  `engine_version` varchar(20)  NULL,
  `calc_time`      datetime     NULL                    COMMENT '估计时间',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_offering_item` (`teacher_id`, `offering_id`, `item_code`),
  KEY `idx_offering` (`offering_id`),
  KEY `idx_course_term` (`course_id`, `term_id`),
  KEY `idx_status` (`status`, `fused`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教师尺度Δ表（分项级，含收缩与熔断）';

-- -----------------------------------------------------------------------------
-- 4. student_fail_history 学生跨学期成绩/挂科历史（大纲第 4 维）
--    用途：学生端"历年挂科情况"、D 层/α_i 先验、通道 C 的跨届可比
--    注意：大一上无此数据（最早大一下才有），引擎需支持"无历史"
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `student_fail_history`;
CREATE TABLE `student_fail_history` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`     bigint       NOT NULL                COMMENT '学生ID',
  `term_id`        bigint       NOT NULL                COMMENT '学期ID',
  `course_id`      bigint       NOT NULL                COMMENT '课程ID',
  `offering_id`    bigint       NOT NULL                COMMENT '开课ID',
  `final_score`    decimal(6,2) NULL                    COMMENT '最终总评 T（快照）',
  `pass_score`     int          NULL                    COMMENT '及格线（快照）',
  `is_fail`        tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否挂科',
  `hard_fail`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否因 1/3 硬门槛挂科',
  `is_retake`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否重修记录',
  `score_status`   varchar(20)  NULL                    COMMENT '成绩状态（FINAL/DEFERRED_GIVEUP/ABSENT_EXAM…）',
  `engine_version` varchar(20)  NULL,
  `archived_at`    datetime     NULL                    COMMENT '归档为历史的时间（学期结束结算后写入）',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_offering` (`student_id`, `offering_id`),
  KEY `idx_student` (`student_id`, `term_id`),
  KEY `idx_course_term` (`course_id`, `term_id`),
  KEY `idx_fail` (`is_fail`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '学生跨学期成绩/挂科历史表';

-- -----------------------------------------------------------------------------
-- 5. teaching_class_fail_rate 教学班挂科率（反馈给老师）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teaching_class_fail_rate`;
CREATE TABLE `teaching_class_fail_rate` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID',
  `teacher_id`        bigint       NOT NULL                COMMENT '任课教师ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `examinee_count`    int          NOT NULL DEFAULT 0      COMMENT '分母：实际考核人数（教务二次确认，扣除缓考/无成绩）',
  `fail_count`        int          NOT NULL DEFAULT 0      COMMENT '分子：挂科人数',
  `fail_rate`         decimal(5,2) NOT NULL DEFAULT 0.00   COMMENT '挂科率(%) = fail_count / examinee_count × 100',
  `hard_fail_count`   int          NOT NULL DEFAULT 0      COMMENT '其中因 1/3 硬门槛挂科的人数',
  `is_retake_class`   tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否重修班（仅展示；不作特殊排除）',
  `engine_version`    varchar(20)  NULL,
  `calc_time`         datetime     NULL                    COMMENT '计算时间',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_term` (`teaching_class_id`, `term_id`),
  KEY `idx_teacher_term` (`teacher_id`, `term_id`),
  KEY `idx_offering` (`offering_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教学班挂科率表（→老师）';

-- -----------------------------------------------------------------------------
-- 6. admin_class_fail_rate 行政班挂科率（反馈给导员）
--    粒度：行政班 × 开课（该班在这门课的挂科率）；跨课汇总实时算
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `admin_class_fail_rate`;
CREATE TABLE `admin_class_fail_rate` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_class_id` bigint       NOT NULL                COMMENT '行政班ID',
  `offering_id`    bigint       NOT NULL                COMMENT '开课ID',
  `course_id`      bigint       NOT NULL                COMMENT '课程ID',
  `term_id`        bigint       NOT NULL                COMMENT '学期ID',
  `examinee_count` int          NOT NULL DEFAULT 0      COMMENT '分母：实际考核人数',
  `fail_count`     int          NOT NULL DEFAULT 0      COMMENT '分子：挂科人数',
  `fail_rate`      decimal(5,2) NOT NULL DEFAULT 0.00   COMMENT '挂科率(%)',
  `hard_fail_count` int         NOT NULL DEFAULT 0      COMMENT '其中硬门槛挂科人数',
  `engine_version` varchar(20)  NULL,
  `calc_time`      datetime     NULL,
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_offering` (`admin_class_id`, `offering_id`),
  KEY `idx_class_term` (`admin_class_id`, `term_id`),
  KEY `idx_offering` (`offering_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '行政班挂科率表（→导员）';

-- -----------------------------------------------------------------------------
-- 7. course_fail_rate_history 课程挂科率历史（→课程因子 c，供下学期使用）
--    按人数加权：= 该课程全部教学班合计挂科数 / 合计考核人数（不简单取平均）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_fail_rate_history`;
CREATE TABLE `course_fail_rate_history` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`           bigint       NOT NULL                COMMENT '课程ID',
  `term_id`             bigint       NOT NULL                COMMENT '学期ID',
  `examinee_count`      int          NOT NULL DEFAULT 0      COMMENT '分母：该课程本学期合计实际考核人数（按人数加权）',
  `fail_count`          int          NOT NULL DEFAULT 0      COMMENT '分子：合计挂科人数',
  `fail_rate`           decimal(5,2) NOT NULL DEFAULT 0.00   COMMENT '课程挂科率(%)（入历史，供下学期课程因子 c 标定）',
  `hard_fail_count`     int          NOT NULL DEFAULT 0,
  `teaching_class_count` int         NOT NULL DEFAULT 0      COMMENT '本学期该课程教学班数',
  `is_initialized`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否手工初始化值（历史首值先手填，后续自动更新）',
  `course_factor`       decimal(5,3) NULL                    COMMENT '由本挂科率映射出的课程因子 c（映射函数由引擎定义）',
  `engine_version`      varchar(20)  NULL,
  `calc_time`           datetime     NULL,
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_term` (`course_id`, `term_id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课程挂科率历史表（→课程因子 c）';

-- =============================================================================
-- 附一：通道与输出形态的对应（引擎实现口径）
--   通道 A（确定性）    ：无置信度概念，A 判定 100% 可信；独立置顶展示，与风险分不混算
--   通道 B（班内相对）  ：第 2–3 周起可用；输出"班内排序 + 相对分"，供导员/班主任
--   通道 C（跨班可比）  ：第 6–9 周起（依赖 Δ 与锚点）；输出"全校可比风险分"，供教务
--   置信度 W            ：HIGH(≥0.7) 完整分档｜MEDIUM(0.4–0.7) 分数+标注
--                         LOW(0.15–0.4) 仅行为预警｜VERY_LOW(<0.15) 数据不足，无法评估
--   ★硬规则：覆盖率 < coverage_threshold → 直接判 LOW 及以下，不参与乘法
--   ★硬规则：覆盖率达标且锚点为 A/A′ → W 不得低于 confidence_min_ok（避免被相乘压死）
-- 附二：三级挂科率的三个用途（不要混用）
--   教学班 → 反馈给任课老师（我教得怎么样）
--   行政班 → 反馈给导员（我这个班整体如何）
--   课程   → 入历史，标定下学期课程因子 c（不按老师拆，防止 Δ 与 c 双重计算）
-- 附三：本层与"每日快照删除"相关的两处口径修正
--   1) 干预无效判定窗口：原"下次快照" → 现"下次 risk_state 刷新 / 最长 invalid_check_days 天"
--   2) 历史变化追溯：原靠快照曲线 → 现靠 alert_flow_log（07 部分）的预警生命周期时间线
-- 附四：旧库映射
--   旧 alert_snapshot（604 行，含五维分）→ 废弃（每日快照取消）；当前状态改由 risk_state
--   旧 alert_rule_config（五维权重/阈值）→ 废弃；改由 risk_engine_config 版本化管理
--   旧 history_risk → 废弃（旧口径派生分）；改由 student_fail_history + 原始成绩
--   旧 class_performance 的挂科相关派生 → 不迁移，由 score_result 重算
-- =============================================================================
