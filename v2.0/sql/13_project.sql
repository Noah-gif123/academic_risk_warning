-- =============================================================================
-- 学情预警系统 v2.0 · 第 13 部分：项目型教学活动层（4 张表）
-- -----------------------------------------------------------------------------
-- 依赖：02（teaching_class）、03（enrollment）、11（course_lesson）
-- 依据：2026-09 口径冻结版《功能整理》
--       ★ 项目 = 文件型提交（Word / PDF / Java 代码 / PPT / 压缩包 / 仓库链接），
--         与“作业=题目型在线作答”明确分工；考查课以项目结课的也走这里
--       ★ 团队项目由**组长提交一份贡献表**：提交前弹声明、提交后不可修改，
--         教师也不能改，一律以组长提交为准
--       ★ 个人得分 = 教师给的项目总分 × 个人贡献占比
--       ★ 逾期不允许补交（应用层校验，表里不设“迟交可提交”状态）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. project_task 项目任务（教师发布）
--    说明：① is_team 区分个人项目与团队项目；团队项目才有贡献表；
--          ② allow_formats 允许的提交形式（逗号分隔），如 'WORD,PDF,CODE,PPT,ZIP,LINK'；
--          ③ scheme_item_id 指向成绩构成中的评分项，项目得分按它折算进总评；
--          ④ lesson_id 可空：挂在某次课的“课中”或“课后”时填写。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `project_task`;
CREATE TABLE `project_task` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID（冗余）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `project_name`      varchar(200) NOT NULL                COMMENT '项目名称',
  `description`       text         NULL                    COMMENT '要求说明',
  `lesson_id`         bigint       NULL                    COMMENT '所属课时ID（可空）',
  `start_time`        datetime     NULL                    COMMENT '开始时间',
  `end_time`          datetime     NULL                    COMMENT '截止时间（逾期不允许补交）',
  `is_team`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否团队项目：1是（需组长提交贡献表）',
  `full_score`        decimal(6,2) NOT NULL                COMMENT '项目满分',
  `allow_formats`     varchar(200) NOT NULL DEFAULT 'WORD,PDF,CODE,PPT,ZIP,LINK' COMMENT '允许的提交形式（逗号分隔）',
  `scheme_item_id`    bigint       NULL                    COMMENT '计入的评分项ID（为空表示仅记录不计分）',
  `publisher_id`      bigint       NOT NULL                COMMENT '发起人（教师ID）',
  `status`            varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/PUBLISHED已发布/CLOSED已截止',
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tc_status` (`teaching_class_id`, `status`),
  KEY `idx_time` (`start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '项目任务表';

-- -----------------------------------------------------------------------------
-- 2. project_team 项目组队（团队项目使用）
--    说明：① leader_student_id 即组长，贡献表只能由组长提交；
--          ② member_json 存成员快照：[{"studentId":1,"studentNo":"..","studentName":".."}]，
--             不另外开成员表，避免与选课关系重复维护；
--          ③ 同一项目内组长唯一（uk_project_leader），防止一人带两个组。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `project_team`;
CREATE TABLE `project_team` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`        bigint       NOT NULL                COMMENT '项目任务ID',
  `team_name`         varchar(100) NULL                    COMMENT '组名',
  `leader_student_id` bigint       NOT NULL                COMMENT '组长学生ID（贡献表提交人）',
  `member_json`       json         NOT NULL                COMMENT '成员列表快照',
  `member_count`      int          NOT NULL DEFAULT 1      COMMENT '成员数（冗余，便于校验）',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_leader` (`project_id`, `leader_student_id`),
  KEY `idx_project` (`project_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '项目组队表';

-- -----------------------------------------------------------------------------
-- 3. project_submission 项目提交与批阅
--    说明：① 团队项目一个组一条（team_id 必填，student_id 记组长）；
--             个人项目一人一条（team_id 为空，student_id 记本人）；
--          ② files_json 存文件列表：[{"fileId":12,"name":"报告.pdf"}]，
--             文件本体在 resource_file 中；
--          ③ project_score 是**教师给的项目总分**（团队项目即整组分），
--             成员个人得分不落这张表，由贡献表折算后展示（或写入分项得分）。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `project_submission`;
CREATE TABLE `project_submission` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`     bigint       NOT NULL                COMMENT '项目任务ID',
  `team_id`        bigint       NULL                    COMMENT '组ID（个人项目为空）',
  `student_id`     bigint       NOT NULL                COMMENT '提交人（团队项目=组长）',
  `submit_time`    datetime     NULL                    COMMENT '提交时间',
  `files_json`     json         NULL                    COMMENT '提交文件列表',
  `repo_url`       varchar(500) NULL                    COMMENT '代码仓库链接（可选）',
  `remark`         varchar(500) NULL                    COMMENT '提交说明',
  `submit_status`  varchar(20)  NOT NULL DEFAULT 'NOT_SUBMITTED' COMMENT '状态：NOT_SUBMITTED未提交/SUBMITTED已提交/MARKED已批阅',
  `project_score`  decimal(6,2) NULL                    COMMENT '教师给的项目总分（团队项目为整组分）',
  `comment`        varchar(500) NULL                    COMMENT '教师评语',
  `marked_by`      bigint       NULL                    COMMENT '批阅教师ID',
  `marked_at`      datetime     NULL                    COMMENT '批阅时间',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_team` (`project_id`, `team_id`),
  KEY `idx_project_student` (`project_id`, `student_id`),
  KEY `idx_status` (`submit_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '项目提交与批阅表';

-- -----------------------------------------------------------------------------
-- 4. project_contribution 团队贡献表（组长提交，提交后不可修改）
--    说明：① 一个组只有一份（uk_project_team），提交人为组长；
--          ② contribution_json：[{"studentId":1,"studentName":"..","ratio":0.30}]，
--             total_ratio 必须等于 1.0000（CHECK 约束兜底，应用层也要校验合计 100%）；
--          ③ is_locked 默认 1：提交即锁定，**教师也没有修改权限**；
--             如需更正只能走教务特批（在 audit_log 中记录），不做常规入口；
--          ④ 个人得分 = 项目总分（project_submission.project_score）× ratio。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `project_contribution`;
CREATE TABLE `project_contribution` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`        bigint       NOT NULL                COMMENT '项目任务ID',
  `team_id`           bigint       NOT NULL                COMMENT '组ID',
  `leader_student_id` bigint       NOT NULL                COMMENT '组长学生ID（提交人）',
  `contribution_json` json         NOT NULL                COMMENT '成员贡献占比列表',
  `total_ratio`       decimal(6,4) NOT NULL DEFAULT 1.0000 COMMENT '占比合计（必须=1.0000）',
  `submit_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间（提交后不可改）',
  `is_locked`         tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否锁定：1提交即锁定（不允许任何角色修改）',
  `remark`            varchar(200) NULL                    COMMENT '组长备注',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_team` (`project_id`, `team_id`),
  CONSTRAINT `ck_contribution_total` CHECK (`total_ratio` = 1.0000)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '团队贡献表（组长提交，不可修改）';
