-- =============================================================================
-- 学情预警系统 v2.0 · 第 14 部分：入课与邀请码（1 张表 + 教学班扩列）
-- -----------------------------------------------------------------------------
-- 依赖：02（teaching_class）、01（student）
-- 依据：2026-09 口径冻结版《功能整理》
--       ★ 流程：教师生成 6 位邀请码 → 学生输码申请 → **教师同意** → 系统给教务一条
--         “知会”记录（教务只需要知道，**不做审批**）
--       ★ 只有教师开启了入班审核，学生才需要走“申请 + 同意”；未开启则输码直接入班
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 教学班扩列：邀请码与入班审核开关
--    说明：① invite_code 一个教学班一个 6 位码，学期内有效、使用次数不限；
--          ② join_review_enabled = 1 时，学生输码后需要教师同意才入班；
--          ③ uk_invite 保证邀请码全局唯一；可为 NULL（教师尚未生成），MySQL 允许多个 NULL。
-- -----------------------------------------------------------------------------
ALTER TABLE `teaching_class`
  ADD COLUMN `invite_code`         varchar(6) NULL COMMENT '6 位入班邀请码（教师生成，学期内有效、次数不限）' AFTER `is_retake_class`,
  ADD COLUMN `join_review_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启入班审核：1需要教师同意' AFTER `invite_code`,
  ADD UNIQUE KEY `uk_invite` (`invite_code`);

-- -----------------------------------------------------------------------------
-- 2. course_join_request 入课申请（学生申请 / 教师审核 / 教务知会）
--    说明：① 同一学生对同一教学班只有一条申请记录（uk_tc_student），被拒后可更新状态重申请；
--          ② status：APPLIED 已申请待审 / APPROVED 教师已同意（入班）/ REJECTED 已拒绝 /
--             AUTO 无需审核直接入班（教师未开启审核时）；
--          ③ notify_admin / notify_time：教师同意后写一条“知会”记录，
--             教务端只读可见，**不需要任何审批动作**；
--          ④ 审核通过后由应用层写入 enrollment（选课），本表只记录流程。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_join_request`;
CREATE TABLE `course_join_request` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID（申请加入的班）',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID（冗余）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `student_id`        bigint       NOT NULL                COMMENT '学生ID',
  `invite_code`       varchar(6)   NULL                    COMMENT '学生输入的邀请码（留档，便于纠纷追溯）',
  `apply_source`      varchar(20)  NOT NULL DEFAULT 'INVITE_CODE' COMMENT '申请来源：INVITE_CODE邀请码/ADMIN教务导入',
  `apply_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `status`            varchar(20)  NOT NULL DEFAULT 'APPLIED' COMMENT '状态：APPLIED待审/APPROVED已同意/REJECTED已拒绝/AUTO免审入班',
  `review_by`         bigint       NULL                    COMMENT '审核人（教师ID；AUTO 时为空）',
  `review_time`       datetime     NULL                    COMMENT '审核时间',
  `review_note`       varchar(200) NULL                    COMMENT '审核意见 / 拒绝原因',
  `notify_admin`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已向教务发出知会：1是',
  `notify_time`       datetime     NULL                    COMMENT '知会时间',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tc_student` (`teaching_class_id`, `student_id`),
  KEY `idx_student` (`student_id`, `term_id`),
  KEY `idx_status` (`status`, `apply_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '入课申请表（学生申请/教师审核/教务知会）';
