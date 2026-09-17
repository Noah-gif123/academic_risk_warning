/*
 Navicat Premium Data Transfer

 Source Server         : DK
 Source Server Type    : MySQL
 Source Server Version : 80030
 Source Host           : localhost:3306
 Source Schema         : study_warning_system

 Target Server Type    : MySQL
 Target Server Version : 80030
 File Encoding         : 65001

 Date: 13/09/2026 21:42:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `admin_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员账号（登录用）',
  `admin_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员姓名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_no`(`admin_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for agent_run
-- ----------------------------
DROP TABLE IF EXISTS `agent_run`;
CREATE TABLE `agent_run`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `pipeline` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流水线: FULL/FEEDBACK/EFFECT_BASED 等',
  `trigger_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发方式: MANUAL/SCHEDULED/FEEDBACK 等',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/SUCCESS/PARTIAL/FAILED',
  `total_steps` int NULL DEFAULT 0 COMMENT '总步数',
  `succeeded_steps` int NULL DEFAULT 0 COMMENT '成功步数',
  `failed_steps` int NULL DEFAULT 0 COMMENT '失败步数',
  `total_ms` bigint NULL DEFAULT NULL COMMENT '各步耗时之和(ms)',
  `error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因(首个错误)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_run_student`(`student_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_run_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '智能体流水线运行记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_run_step
-- ----------------------------
DROP TABLE IF EXISTS `agent_run_step`;
CREATE TABLE `agent_run_step`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '所属运行ID',
  `step_no` int NOT NULL COMMENT '步骤序号(从1开始)',
  `agent_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '智能体名称',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态: SUCCESS/FAILED',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '本步耗时(ms)',
  `output_digest` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产出摘要(截断)',
  `error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因',
  `validation_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '输出校验: PASS/WARN/FAIL/SKIP',
  `validation_detail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '校验说明',
  `reflection` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '反思信息(自检批评)',
  `attempts` int NULL DEFAULT 1 COMMENT 'LLM 调用轮次',
  `output_fields` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产出顶层字段清单(逗号分隔)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_step_run`(`run_id` ASC, `step_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '智能体单步运行明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `alert_operation_log`;
CREATE TABLE `alert_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_id` bigint NOT NULL COMMENT '关联预警ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID(教师或学生)',
  `operator_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人类型: TEACHER/STUDENT/SYSTEM',
  `operation` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型: GENERATE/ACKNOWLEDGE/HANDLE/DISMISS/ARCHIVE/REOPEN/CLOSE',
  `from_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作前状态',
  `to_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作后状态',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE,
  INDEX `idx_operator`(`operator_id` ASC, `operator_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 168 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警操作审计日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for alert_record
-- ----------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '预警学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '预警课程ID（可为空，表示综合预警）',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警等级：RED-红色、ORANGE-橙色、YELLOW-黄色、GREEN-绿色',
  `alert_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警类型：FAILURE-挂科风险、HOMEWORK-作业欠交、KNOWLEDGE-知识点断层、ABSENTEEISM-学习倦怠、DROP-成绩骤降、CUMULATIVE-累积风险',
  `alert_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '预警原因描述',
  `risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '综合风险分数（0-100）',
  `predicted_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '预测成绩',
  `academic_risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '学业风险分',
  `homework_risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '作业风险分',
  `attendance_risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '出勤风险分',
  `knowledge_risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '知识风险分',
  `history_risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '历史风险分',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '预警状态：ACTIVE-生效中、ACKNOWLEDGED-已确认、HANDLED-已处理、DISMISSED-已撤销',
  `teacher_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '教师处理备注',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '教师处理时间',
  `ack_time` datetime NULL DEFAULT NULL COMMENT '学生确认时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预警生成时间',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '预警失效时间',
  `notified` tinyint(1) NULL DEFAULT 0 COMMENT '是否已发送通知',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_alert_level`(`alert_level` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1285 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for alert_rule_config
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule_config`;
CREATE TABLE `alert_rule_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `system_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '体系: FRESHMAN-新生体系, SENIOR-老生体系',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID(NULL=该体系默认规则, 非NULL=课程专属规则)',
  `weight_academic` decimal(4, 3) NOT NULL COMMENT '学业风险权重',
  `weight_homework` decimal(4, 3) NOT NULL COMMENT '作业风险权重',
  `weight_knowledge` decimal(4, 3) NOT NULL COMMENT '知识点风险权重',
  `weight_attendance` decimal(4, 3) NOT NULL COMMENT '出勤风险权重',
  `weight_history` decimal(4, 3) NOT NULL COMMENT '历史风险权重',
  `threshold_red` decimal(5, 1) NOT NULL DEFAULT 40.0 COMMENT '红色预警总分阈值',
  `threshold_orange` decimal(5, 1) NOT NULL DEFAULT 30.0 COMMENT '橙色预警总分阈值',
  `threshold_yellow` decimal(5, 1) NOT NULL DEFAULT 15.0 COMMENT '黄色预警总分阈值',
  `homework_submit_rate_threshold` decimal(5, 1) NULL DEFAULT 70.0 COMMENT '作业提交率阈值(低于触发HOMEWORK预警)',
  `knowledge_correct_rate_threshold` decimal(5, 1) NULL DEFAULT 50.0 COMMENT '知识点正确率阈值(低于触发KNOWLEDGE预警)',
  `attendance_rate_threshold` decimal(5, 1) NULL DEFAULT 80.0 COMMENT '出勤率阈值(低于触发ABSENTEEISM预警)',
  `score_drop_threshold` decimal(5, 1) NULL DEFAULT 15.0 COMMENT '成绩骤降阈值(下降超过触发DROP预警)',
  `default_academic_risk` decimal(5, 1) NULL DEFAULT 50.0 COMMENT '无数据时学业默认风险分',
  `default_homework_risk` decimal(5, 1) NULL DEFAULT 30.0 COMMENT '无数据时作业默认风险分',
  `default_attendance_risk` decimal(5, 1) NULL DEFAULT 20.0 COMMENT '无数据时出勤默认风险分',
  `default_knowledge_risk` decimal(5, 1) NULL DEFAULT 25.0 COMMENT '无数据时知识点默认风险分',
  `default_history_risk` decimal(5, 1) NULL DEFAULT 15.0 COMMENT '老生无历史数据默认风险分',
  `default_history_freshman_risk` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '新生历史风险默认分(无历史=0)',
  `homework_submit_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '作业-提交率子权重(默认40)',
  `homework_ontime_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '作业-按时提交子权重(默认30)',
  `homework_avg_score_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '作业-均分子权重(默认30)',
  `attendance_absent_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '出勤-缺勤子权重(默认50)',
  `attendance_late_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '出勤-迟到子权重(默认30)',
  `attendance_quiz_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '出勤-随堂测验子权重(默认20)',
  `knowledge_correct_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '知识点-正确率子权重(默认50)',
  `knowledge_weak_rate_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '知识点-薄弱占比子权重(默认30)',
  `knowledge_basic_rate_weight` decimal(5, 1) NULL DEFAULT NULL COMMENT '知识点-基础正确率子权重(默认20)',
  `history_failed_bonus` decimal(5, 1) NULL DEFAULT NULL COMMENT '历史-挂科加分(默认15)',
  `history_unstable_bonus` decimal(5, 1) NULL DEFAULT NULL COMMENT '历史-不稳定加分(默认20)',
  `history_normal_bonus` decimal(5, 1) NULL DEFAULT NULL COMMENT '历史-一般加分(默认10)',
  `study_lookback_weeks` int NULL DEFAULT NULL COMMENT '学习时长-回溯周数(默认4)',
  `study_decline_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '学习时长-下滑判定比例(默认0.5)',
  `study_decline_bonus` decimal(5, 1) NULL DEFAULT NULL COMMENT '学习时长-下滑加分(默认10)',
  `study_default_risk` decimal(5, 1) NULL DEFAULT NULL COMMENT '学习时长-无数据默认风险分(默认10)',
  `predicted_pass_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '预测成绩-及格线(默认60)',
  `predicted_submit_threshold` decimal(5, 1) NULL DEFAULT NULL COMMENT '预测成绩-作业提交率扣分阈值(默认70)',
  `predicted_attendance_threshold` decimal(5, 1) NULL DEFAULT NULL COMMENT '预测成绩-出勤率扣分阈值(默认80)',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 1-启用, 0-停用',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_system_course`(`system_type` ASC, `course_id` ASC) USING BTREE,
  INDEX `idx_system_type`(`system_type` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警规则配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for alert_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `alert_snapshot`;
CREATE TABLE `alert_snapshot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `snapshot_date` date NOT NULL COMMENT '快照日期',
  `snapshot_week` int NULL DEFAULT NULL COMMENT '快照周次(教学周)',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预警等级: RED/ORANGE/YELLOW/GREEN',
  `risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '综合风险分',
  `academic_risk_score` decimal(5, 1) NULL DEFAULT NULL,
  `homework_risk_score` decimal(5, 1) NULL DEFAULT NULL,
  `attendance_risk_score` decimal(5, 1) NULL DEFAULT NULL,
  `knowledge_risk_score` decimal(5, 1) NULL DEFAULT NULL,
  `history_risk_score` decimal(5, 1) NULL DEFAULT NULL,
  `study_duration_risk_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '学习时长风险分(预留)',
  `predicted_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '预测成绩',
  `alert_types` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发的预警类型(逗号分隔)',
  `usual_score` int NULL DEFAULT NULL COMMENT '平时成绩',
  `mid_score` int NULL DEFAULT NULL COMMENT '期中成绩',
  `final_score` int NULL DEFAULT NULL COMMENT '期末成绩',
  `homework_submit_rate` decimal(5, 1) NULL DEFAULT NULL COMMENT '作业提交率%',
  `attendance_rate` decimal(5, 1) NULL DEFAULT NULL COMMENT '出勤率%',
  `knowledge_correct_rate` decimal(5, 1) NULL DEFAULT NULL COMMENT '知识点正确率%',
  `study_total_minutes` int NULL DEFAULT NULL COMMENT '近4周平均学习时长(分钟)',
  `is_generated_alert` tinyint(1) NULL DEFAULT 0 COMMENT '该快照是否触发了预警生成(1=是,0=仅快照)',
  `related_alert_id` bigint NULL DEFAULT NULL COMMENT '关联的alert_record.id(若触发了预警)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_date`(`student_id` ASC, `snapshot_date` ASC) USING BTREE,
  INDEX `idx_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE,
  INDEX `idx_snapshot_date`(`snapshot_date` ASC) USING BTREE,
  INDEX `idx_alert_id`(`related_alert_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3664 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警历史快照表(趋势分析和回测)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for analysis_report
-- ----------------------------
DROP TABLE IF EXISTS `analysis_report`;
CREATE TABLE `analysis_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `analysis_date` datetime NOT NULL COMMENT '分析时间',
  `weakness_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '薄弱点类别',
  `root_cause` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'LLM生成的根源分析',
  `priority_sequence` json NULL COMMENT '补强优先级排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_analysis`(`student_id` ASC, `analysis_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习分析报告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for attendance
-- ----------------------------
DROP TABLE IF EXISTS `attendance`;
CREATE TABLE `attendance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `total_hours` int NULL DEFAULT NULL COMMENT '总课时',
  `absent_count` int NULL DEFAULT 0 COMMENT '缺勤次数',
  `late_count` int NULL DEFAULT 0 COMMENT '迟到次数',
  `attendance_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '出勤率百分比',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生出勤信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for class_performance
-- ----------------------------
DROP TABLE IF EXISTS `class_performance`;
CREATE TABLE `class_performance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `total_class_times` int NULL DEFAULT 0 COMMENT '上课总次数',
  `absent_count` int NULL DEFAULT 0 COMMENT '缺勤次数',
  `late_count` int NULL DEFAULT 0 COMMENT '迟到次数',
  `quiz_score` int NULL DEFAULT 0 COMMENT '课堂小测得分',
  `qa_evaluate` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '课堂问答表现评价：优秀/一般/消极',
  `attitude_evaluate` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '课堂参与态度评价：积极/一般/消极',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 801 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课堂表现表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '课程名称',
  `usual_ratio` int NOT NULL DEFAULT 30 COMMENT '平时成绩占比(%)',
  `mid_ratio` int NOT NULL DEFAULT 30 COMMENT '期中成绩占比(%)',
  `final_ratio` int NOT NULL DEFAULT 40 COMMENT '期末成绩占比(%)',
  `pass_score` int NOT NULL DEFAULT 60 COMMENT '及格线',
  `total_class_times` int NULL DEFAULT NULL COMMENT '总上课次数',
  `total_homework` int NULL DEFAULT NULL COMMENT '总作业次数',
  `total_knowledge` int NULL DEFAULT NULL COMMENT '核心知识点总数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `prerequisite_course_id` bigint NULL DEFAULT NULL COMMENT '前序课程ID，有值表示该课程有前置课程要求',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for course_knowledge_point
-- ----------------------------
DROP TABLE IF EXISTS `course_knowledge_point`;
CREATE TABLE `course_knowledge_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父知识点ID（NULL=一级知识点）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识点名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识点介绍/说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_name`(`course_id` ASC, `name` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程知识点库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for exercise
-- ----------------------------
DROP TABLE IF EXISTS `exercise`;
CREATE TABLE `exercise`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teacher_id` bigint NOT NULL COMMENT '上传教师ID',
  `course_id` bigint NOT NULL COMMENT '所属课程ID',
  `kp_id` bigint NULL DEFAULT NULL COMMENT '关联知识点ID（已弃用，请使用 exercise_knowledge_point 关联表）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '练习题标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '题目描述/题干',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '附件地址',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '附件类型：PDF/DOCX/IMAGE',
  `difficulty` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEDIUM' COMMENT '难度：EASY/MEDIUM/HARD',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '题目类型：CHOICE-选择题 MULTIPLE-多选题 FILL_BLANK-填空题 JUDGE-判断题 SHORT_ANSWER-简答题',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '标准答案（文本格式）',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-停用',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_kp`(`course_id` ASC, `kp_id` ASC) USING BTREE,
  INDEX `idx_teacher_id`(`teacher_id` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习题表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for exercise_draft
-- ----------------------------
DROP TABLE IF EXISTS `exercise_draft`;
CREATE TABLE `exercise_draft`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recommend_id` bigint NOT NULL COMMENT '推荐记录ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '无子题时的作答文本',
  `sub_answers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'JSON格式的子题作答 Map<subQuestionId, answer>',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_recommend_student`(`recommend_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生作答草稿' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exercise_knowledge_point
-- ----------------------------
DROP TABLE IF EXISTS `exercise_knowledge_point`;
CREATE TABLE `exercise_knowledge_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exercise_id` bigint NOT NULL COMMENT '练习题ID',
  `kp_id` bigint NOT NULL COMMENT '知识点ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exercise_kp`(`exercise_id` ASC, `kp_id` ASC) USING BTREE,
  INDEX `idx_exercise_id`(`exercise_id` ASC) USING BTREE,
  INDEX `idx_kp_id`(`kp_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习题与知识点关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for exercise_recommendation
-- ----------------------------
DROP TABLE IF EXISTS `exercise_recommendation`;
CREATE TABLE `exercise_recommendation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exercise_id` bigint NOT NULL COMMENT '练习题ID（exercise.id）',
  `student_id` bigint NOT NULL COMMENT '被推荐学生ID',
  `alert_id` bigint NULL DEFAULT NULL COMMENT '关联预警记录ID（alert_record.id）',
  `knowledge_point` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识点名称（冗余，便于展示）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待完成 COMPLETED-已完成',
  `score` int NULL DEFAULT NULL COMMENT '作答得分（0-100）',
  `student_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '学生作答内容',
  `teacher_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '教师反馈',
  `recommend_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
  `complete_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `ai_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AI推荐理由',
  `priority` int NOT NULL DEFAULT 0 COMMENT '推荐优先级',
  `recommend_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推荐类型: EXERCISE/VIDEO/READING',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_exercise_id`(`exercise_id` ASC) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习推荐记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for exercise_sub_kp
-- ----------------------------
DROP TABLE IF EXISTS `exercise_sub_kp`;
CREATE TABLE `exercise_sub_kp`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sub_question_id` bigint NOT NULL,
  `kp_id` bigint NOT NULL,
  `weight` decimal(5, 4) NOT NULL DEFAULT 0.0000,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sub_question_id`(`sub_question_id` ASC) USING BTREE,
  INDEX `idx_kp_id`(`kp_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exercise_sub_question
-- ----------------------------
DROP TABLE IF EXISTS `exercise_sub_question`;
CREATE TABLE `exercise_sub_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exercise_id` bigint NOT NULL COMMENT '父习题ID（exercise.id）',
  `seq` int NOT NULL COMMENT '题号（从1开始）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子题标题',
  `kp_id` bigint NULL DEFAULT NULL COMMENT '关联知识点ID（course_knowledge_point.id）',
  `knowledge_point` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识点名称（冗余）',
  `score` int NOT NULL DEFAULT 10 COMMENT '该小题满分分值',
  `reference_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参考答案',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SINGLE',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exercise_id`(`exercise_id` ASC) USING BTREE,
  INDEX `idx_kp_id`(`kp_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '习题子题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for history_risk
-- ----------------------------
DROP TABLE IF EXISTS `history_risk`;
CREATE TABLE `history_risk`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `grade_level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '大二' COMMENT '学生年级',
  `last_term_failed` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '否' COMMENT '上一学期是否挂科：是/否',
  `study_stable` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学习稳定性评价：稳定/一般/不稳定',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1304 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历史风险与学习稳定性表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for homework_info
-- ----------------------------
DROP TABLE IF EXISTS `homework_info`;
CREATE TABLE `homework_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `total_homework` int NULL DEFAULT 0 COMMENT '课程作业总数',
  `submit_count` int NULL DEFAULT 0 COMMENT '提交次数',
  `not_submit_count` int NULL DEFAULT 0 COMMENT '未交次数',
  `late_submit_count` int NULL DEFAULT 0 COMMENT '迟交次数',
  `score_list` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '已提交作业各次得分（逗号分隔）',
  `avg_score` int NULL DEFAULT 0 COMMENT '作业平均分',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 801 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作业完成情况表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for intervention_record
-- ----------------------------
DROP TABLE IF EXISTS `intervention_record`;
CREATE TABLE `intervention_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_id` bigint NOT NULL COMMENT '关联预警ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `teacher_id` bigint NOT NULL COMMENT '执行干预的教师ID',
  `intervention_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '干预类型: TALK-约谈, TUTOR-辅导, PARENT-联系家长, SUPPLEMENT-补课补习, PLAN-学习计划, OTHER-其他',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '干预措施详细描述',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EXECUTING' COMMENT '状态: EXECUTING-执行中, COMPLETED-已完成, INEFFECTIVE-无效',
  `result_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '干预结果记录',
  `risk_score_before` decimal(5, 1) NULL DEFAULT NULL COMMENT '干预时风险分',
  `risk_score_after` decimal(5, 1) NULL DEFAULT NULL COMMENT '干预后最新风险分(回测填充)',
  `risk_score_change` decimal(5, 1) NULL DEFAULT NULL COMMENT '风险分变化(负数=改善)',
  `effect_check_time` datetime NULL DEFAULT NULL COMMENT '效果检查时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_teacher_id`(`teacher_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '干预措施记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for knowledge_mastery
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_mastery`;
CREATE TABLE `knowledge_mastery`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `total_knowledge` int NULL DEFAULT 0 COMMENT '核心知识点总数',
  `error_count` int NULL DEFAULT 0 COMMENT '核心知识点错题数',
  `total_question` int NULL DEFAULT 0 COMMENT '核心知识点总题目数',
  `weak_knowledge_count` int NULL DEFAULT 0 COMMENT '薄弱知识点数量',
  `basic_total` int NULL DEFAULT 0 COMMENT '基础题总题数',
  `basic_correct` int NULL DEFAULT 0 COMMENT '基础题对题数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 534 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识点掌握情况表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for monitor_report
-- ----------------------------
DROP TABLE IF EXISTS `monitor_report`;
CREATE TABLE `monitor_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `report_date` date NOT NULL COMMENT '报告日期',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'LLM生成的监测摘要',
  `risk_dimensions` json NULL COMMENT '六维风险分详情',
  `anomaly_flags` json NULL COMMENT '异常标记',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_date`(`student_id` ASC, `report_date` ASC) USING BTREE,
  INDEX `idx_course_date`(`course_id` ASC, `report_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学情监测报告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipient_id` bigint NOT NULL COMMENT '接收人ID(教师或学生)',
  `recipient_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收人类型: TEACHER/STUDENT',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '通知正文',
  `alert_id` bigint NULL DEFAULT NULL COMMENT '关联预警ID',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型: ALERT_NEW-新预警, ALERT_UPGRADE-等级升级, HANDLE_RESULT-已处理, STUDENT_RESPOND-学生回应, TIMEOUT-超时提醒, SYSTEM-系统通知',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道: IN_APP-站内信, EMAIL-邮件, SMS-短信',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `send_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '发送状态: PENDING-待发送, SENT-已发送, FAILED-失败',
  `send_time` datetime NULL DEFAULT NULL COMMENT '发送时间',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '跳转目标类型: ALERT/RECOMMEND/COURSE',
  `target_id` bigint NULL DEFAULT NULL COMMENT '跳转目标ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_recipient`(`recipient_id` ASC, `recipient_type` ASC) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1158 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for score_info
-- ----------------------------
DROP TABLE IF EXISTS `score_info`;
CREATE TABLE `score_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `usual_score` int NULL DEFAULT 0 COMMENT '平时表现成绩',
  `mid_score` int NULL DEFAULT 0 COMMENT '期中成绩',
  `final_score` int NULL DEFAULT 0 COMMENT '期末成绩',
  `comprehensive_score` int NULL DEFAULT 0 COMMENT '课程综合成绩',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 801 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学业成绩表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for strategy_record
-- ----------------------------
DROP TABLE IF EXISTS `strategy_record`;
CREATE TABLE `strategy_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `previous_strategy` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '上一策略描述',
  `new_strategy` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '新策略描述',
  `change_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调整原因',
  `trigger_source` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发来源: FEEDBACK/EFFECT_CHECK/MANUAL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_strategy`(`student_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '策略调整记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生主键ID',
  `student_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `student_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '年级',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '班级',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '123456' COMMENT '登录密码',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 801 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生基础信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_course
-- ----------------------------
DROP TABLE IF EXISTS `student_course`;
CREATE TABLE `student_course`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 801 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生-课程关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_goal
-- ----------------------------
DROP TABLE IF EXISTS `student_goal`;
CREATE TABLE `student_goal`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `goal_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标等级：冲刺/稳定/追赶/保底',
  `target_score` int NULL DEFAULT NULL COMMENT '目标分数(0-100)',
  `milestones` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '里程碑列表(JSON数组)',
  `note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生备注',
  `deadline` datetime NULL DEFAULT NULL COMMENT '截止日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生学习目标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_memory
-- ----------------------------
DROP TABLE IF EXISTS `student_memory`;
CREATE TABLE `student_memory`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID（NULL=综合）',
  `version` int NOT NULL DEFAULT 1 COMMENT '记忆版本（每次刷新+1）',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '压缩后的记忆摘要文本',
  `source_run_id` bigint NULL DEFAULT NULL COMMENT '产生该记忆的流水线运行ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_memory_student`(`student_id` ASC, `course_id` ASC, `version` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生长期记忆（跨轮次上下文）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_profile
-- ----------------------------
DROP TABLE IF EXISTS `student_profile`;
CREATE TABLE `student_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '鐠囧墽鈻糏D閿涘湤ULL=鐠恒劏顕崇粙瀣?偅閸氬牏鏁鹃崓蹇ョ礆',
  `knowledge_profile` json NULL COMMENT '知识点掌握度画像',
  `habit_profile` json NULL COMMENT '学习习惯画像',
  `goal_profile` json NULL COMMENT '学习目标画像',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生三维画像表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_weak_point
-- ----------------------------
DROP TABLE IF EXISTS `student_weak_point`;
CREATE TABLE `student_weak_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `kp_id` bigint NOT NULL COMMENT '知识点ID（关联course_knowledge_point.id）',
  `error_rate` decimal(5, 1) NULL DEFAULT NULL COMMENT '该知识点错误率（%）',
  `error_count` int NULL DEFAULT 0 COMMENT '该知识点错误数',
  `alert_id` bigint NULL DEFAULT NULL COMMENT '关联预警记录ID（alert_record.id）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_course`(`student_id` ASC, `course_id` ASC) USING BTREE,
  INDEX `idx_kp_id`(`kp_id` ASC) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 458 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生薄弱知识点明细' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for study_duration
-- ----------------------------
DROP TABLE IF EXISTS `study_duration`;
CREATE TABLE `study_duration`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `week_number` int NOT NULL COMMENT '教学周次(1-18)',
  `total_minutes` int NOT NULL DEFAULT 0 COMMENT '总学习时长(分钟)',
  `online_minutes` int NULL DEFAULT 0 COMMENT '线上学习时长(看视频/课件)',
  `offline_minutes` int NULL DEFAULT 0 COMMENT '线下自习时长',
  `exercise_minutes` int NULL DEFAULT 0 COMMENT '练习做题时长',
  `discussion_minutes` int NULL DEFAULT 0 COMMENT '讨论互动时长(论坛/答疑)',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最近一次登录时间',
  `record_date` date NOT NULL COMMENT '记录日期(该周周日)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_course_week`(`student_id` ASC, `course_id` ASC, `week_number` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_course_week`(`course_id` ASC, `week_number` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习时长记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for study_plan
-- ----------------------------
DROP TABLE IF EXISTS `study_plan`;
CREATE TABLE `study_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `plan_content` json NULL COMMENT 'LLM生成的学习计划内容',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/COMPLETED/CANCELLED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_plan`(`student_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_course_plan`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习计划表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sub_question_answer
-- ----------------------------
DROP TABLE IF EXISTS `sub_question_answer`;
CREATE TABLE `sub_question_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recommend_id` bigint NOT NULL COMMENT '推荐记录ID（exercise_recommendation.id）',
  `sub_question_id` bigint NOT NULL COMMENT '子题目ID（exercise_sub_question.id）',
  `student_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '学生作答内容',
  `score` int NULL DEFAULT NULL COMMENT '教师打分（0-子题满分）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ANSWERED/GRADED',
  `answer_time` datetime NULL DEFAULT NULL COMMENT '作答时间',
  `grade_time` datetime NULL DEFAULT NULL COMMENT '评分时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_recommend_id`(`recommend_id` ASC) USING BTREE,
  INDEX `idx_sub_question_id`(`sub_question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子题作答记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `teacher_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '教师工号（登录账号）',
  `teacher_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '教师姓名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密存储）',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_teacher_no`(`teacher_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for teacher_class
-- ----------------------------
DROP TABLE IF EXISTS `teacher_class`;
CREATE TABLE `teacher_class`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '班级名称（和student表一致）',
  `student_id_start` bigint NULL DEFAULT NULL COMMENT '分配学生ID起始值',
  `student_id_end` bigint NULL DEFAULT NULL COMMENT '分配学生ID结束值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_teacher_class`(`teacher_id` ASC, `class_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师负责班级表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for teacher_management_log
-- ----------------------------
DROP TABLE IF EXISTS `teacher_management_log`;
CREATE TABLE `teacher_management_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teacher_id` bigint NOT NULL COMMENT '被管理的教师ID',
  `admin_id` bigint NOT NULL COMMENT '操作的管理员ID',
  `action_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型: MARK_ATTENTION-标记关注, MARK_WARNING-标记警告, MARK_RECTIFY-标记整改, RESOLVE-解除标记, SEND_NOTICE-发送通知, ADD_REMARK-添加备注',
  `old_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更前教师管理状态: NORMAL-正常, ATTENTION-需关注, WARNING-警告, RECTIFY-整改中, RESOLVED-已解除',
  `new_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更后教师管理状态',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '管理备注/操作说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_teacher_id`(`teacher_id` ASC) USING BTREE,
  INDEX `idx_admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师管理记录表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
