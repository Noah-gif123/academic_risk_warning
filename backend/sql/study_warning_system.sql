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

 Date: 08/07/2026 21:40:48
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
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin001', '系统管理员', '123456', '13800000000', '2026-05-26 16:52:44', '2026-05-26 16:52:44');

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
) ENGINE = InnoDB AUTO_INCREMENT = 113 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警操作审计日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of alert_operation_log
-- ----------------------------
INSERT INTO `alert_operation_log` VALUES (1, 458, 4, 'TEACHER', 'ACKNOWLEDGE', 'ACTIVE', 'ACKNOWLEDGED', '教师已查阅该预警', '2026-05-21 10:00:00');
INSERT INTO `alert_operation_log` VALUES (2, 458, 4, 'TEACHER', 'HANDLE', 'ACKNOWLEDGED', 'HANDLED', '已约谈学生，安排课后辅导', '2026-05-21 11:00:00');
INSERT INTO `alert_operation_log` VALUES (3, 463, 4, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '联系了家长，制定了学习计划', '2026-05-21 14:00:00');
INSERT INTO `alert_operation_log` VALUES (4, 472, 4, 'TEACHER', 'ACKNOWLEDGE', 'ACTIVE', 'ACKNOWLEDGED', NULL, '2026-05-21 15:00:00');
INSERT INTO `alert_operation_log` VALUES (5, 475, 4, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '学生已办理缓考，预警不适用', '2026-05-21 16:00:00');
INSERT INTO `alert_operation_log` VALUES (6, 559, 4, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '约谈', '2026-05-21 15:53:16');
INSERT INTO `alert_operation_log` VALUES (7, 561, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '着重加强知识点学习', '2026-05-21 16:03:28');
INSERT INTO `alert_operation_log` VALUES (8, 561, 1, 'STUDENT', 'RESPOND', 'HANDLED', 'HANDLED', '学生回应: 正在按老师要求积极改进中', '2026-05-21 16:16:17');
INSERT INTO `alert_operation_log` VALUES (9, 645, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '补加做题', '2026-05-21 16:30:01');
INSERT INTO `alert_operation_log` VALUES (10, 711, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '下周一来我办公室', '2026-05-21 18:40:26');
INSERT INTO `alert_operation_log` VALUES (11, 711, 54, 'STUDENT', 'RESPOND', 'HANDLED', 'HANDLED', '学生回应: 收到', '2026-05-21 18:40:56');
INSERT INTO `alert_operation_log` VALUES (12, 709, 54, 'STUDENT', 'ACKNOWLEDGE', 'ACTIVE', 'ACKNOWLEDGED', NULL, '2026-05-21 18:41:03');
INSERT INTO `alert_operation_log` VALUES (13, 710, 54, 'STUDENT', 'ACKNOWLEDGE', 'ACTIVE', 'ACKNOWLEDGED', NULL, '2026-05-21 18:41:05');
INSERT INTO `alert_operation_log` VALUES (14, 768, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '111', '2026-05-21 18:51:10');
INSERT INTO `alert_operation_log` VALUES (15, 768, 88, 'STUDENT', 'RESPOND', 'HANDLED', 'HANDLED', '学生回应: 正在按老师要求积极改进中', '2026-05-21 19:45:38');
INSERT INTO `alert_operation_log` VALUES (16, 768, 88, 'STUDENT', 'RESPOND', 'HANDLED', 'HANDLED', '学生回应: 正在按老师要求积极改进中', '2026-05-21 19:45:46');
INSERT INTO `alert_operation_log` VALUES (17, 767, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (18, 766, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (19, 765, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (20, 759, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (21, 764, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (22, 763, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (23, 762, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (24, 761, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (25, 760, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:05');
INSERT INTO `alert_operation_log` VALUES (26, 769, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (27, 758, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (28, 757, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (29, 756, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (30, 729, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (31, 730, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (32, 731, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (33, 732, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (34, 733, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (35, 734, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (36, 735, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (37, 736, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (38, 737, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (39, 739, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (40, 740, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (41, 741, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (42, 742, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (43, 743, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (44, 744, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (45, 745, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (46, 746, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (47, 747, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (48, 748, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (49, 750, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (50, 751, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (51, 752, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (52, 753, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (53, 754, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (54, 755, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (55, 749, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (56, 728, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (57, 738, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (58, 709, 1, 'TEACHER', 'HANDLE', 'ACKNOWLEDGED', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (59, 710, 1, 'TEACHER', 'HANDLE', 'ACKNOWLEDGED', 'HANDLED', '', '2026-05-21 19:58:06');
INSERT INTO `alert_operation_log` VALUES (60, 768, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (61, 767, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (62, 766, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (63, 765, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (64, 759, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (65, 764, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (66, 763, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (67, 762, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (68, 761, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (69, 760, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (70, 769, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (71, 758, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (72, 757, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (73, 756, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (74, 729, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (75, 730, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (76, 731, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (77, 732, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (78, 733, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (79, 734, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (80, 735, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (81, 736, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (82, 737, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (83, 739, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (84, 740, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (85, 741, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (86, 742, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (87, 743, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (88, 744, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (89, 745, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (90, 746, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (91, 747, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (92, 748, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (93, 750, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (94, 751, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (95, 752, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (96, 753, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (97, 754, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (98, 755, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (99, 749, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (100, 728, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (101, 738, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (102, 709, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (103, 710, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (104, 711, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-05-21 19:58:16');
INSERT INTO `alert_operation_log` VALUES (105, 779, 4, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-05-21 19:59:49');
INSERT INTO `alert_operation_log` VALUES (106, 829, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '11111', '2026-05-26 17:35:19');
INSERT INTO `alert_operation_log` VALUES (107, 830, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '11111', '2026-05-26 17:37:26');
INSERT INTO `alert_operation_log` VALUES (108, 830, 2, 'STUDENT', 'RESPOND', 'HANDLED', 'HANDLED', '学生回应: 正在按老师要求积极改进中', '2026-05-26 17:37:59');
INSERT INTO `alert_operation_log` VALUES (109, 858, 68, 'STUDENT', 'ACKNOWLEDGE', 'ACTIVE', 'ACKNOWLEDGED', NULL, '2026-05-26 17:38:36');
INSERT INTO `alert_operation_log` VALUES (110, 854, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '1111', '2026-05-26 17:42:13');
INSERT INTO `alert_operation_log` VALUES (111, 831, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '434545', '2026-05-26 17:49:54');
INSERT INTO `alert_operation_log` VALUES (112, 832, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '111', '2026-05-26 18:05:23');
INSERT INTO `alert_operation_log` VALUES (113, 864, 78, 'STUDENT', 'ACKNOWLEDGE', 'ACTIVE', 'ACKNOWLEDGED', NULL, '2026-05-26 18:06:41');
INSERT INTO `alert_operation_log` VALUES (114, 955, 1, 'TEACHER', 'HANDLE', 'ACTIVE', 'HANDLED', '进行练习', '2026-07-08 16:40:23');
INSERT INTO `alert_operation_log` VALUES (115, 955, 1, 'STUDENT', 'RESPOND', 'HANDLED', 'HANDLED', '学生回应: 正在按老师要求积极改进中', '2026-07-08 16:40:42');
INSERT INTO `alert_operation_log` VALUES (116, 955, 1, 'TEACHER', 'CLOSE', 'HANDLED', 'CLOSED', '', '2026-07-08 21:30:25');
INSERT INTO `alert_operation_log` VALUES (117, 1079, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (118, 1080, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (119, 1081, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (120, 1082, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (121, 1083, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (122, 1084, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (123, 1085, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (124, 1086, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (125, 1087, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (126, 1088, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (127, 1089, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (128, 1090, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (129, 1091, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (130, 1092, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (131, 1093, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (132, 1094, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (133, 1095, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (134, 1096, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (135, 1097, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (136, 1098, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (137, 1099, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (138, 1100, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (139, 1101, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (140, 1102, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (141, 1103, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (142, 1104, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (143, 1105, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (144, 1106, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (145, 1107, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (146, 1108, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (147, 1109, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (148, 1110, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (149, 1111, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (150, 1112, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (151, 1113, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (152, 1114, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (153, 1115, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (154, 1116, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (155, 1117, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (156, 1118, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');
INSERT INTO `alert_operation_log` VALUES (157, 1119, 1, 'TEACHER', 'DISMISS', 'ACTIVE', 'DISMISSED', '', '2026-07-08 21:31:13');

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
) ENGINE = InnoDB AUTO_INCREMENT = 954 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of alert_record
-- ----------------------------
INSERT INTO `alert_record` VALUES (955, 1, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', 26.0, 84.0, 35.5, 17.1, 12.6, 36.9, 0.0, 'CLOSED', '', '2026-07-08 21:30:25', NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (956, 2, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', 18.2, 56.0, 28.5, 8.1, 9.2, 24.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (957, 3, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', 16.3, 87.0, 18.0, 17.0, 5.4, 24.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (958, 5, 1, 'YELLOW', 'FAILURE', '预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', 23.3, 54.0, 40.0, 14.8, 10.5, 20.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (959, 12, 1, 'YELLOW', 'FAILURE', '预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', 24.2, 57.0, 28.5, 29.8, 10.3, 26.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (960, 13, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', 17.9, 77.0, 14.0, 32.0, 25.9, 5.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (961, 14, 1, 'ORANGE', 'FAILURE', '预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', 30.5, 37.0, 51.0, 27.7, 13.9, 18.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (962, 15, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', 21.3, 93.0, 20.5, 21.1, 6.7, 39.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (963, 19, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', 29.9, 71.0, 37.5, 24.2, 20.8, 38.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (964, 20, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', 19.6, 70.0, 25.0, 26.4, 9.9, 14.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (965, 21, 1, 'YELLOW', 'FAILURE', '预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', 27.1, 41.0, 39.5, 20.1, 8.0, 35.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (966, 23, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', 19.3, 67.0, 18.5, 14.5, 26.3, 26.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (967, 28, 1, 'YELLOW', 'FAILURE', '预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', 22.1, 55.0, 37.5, 20.5, 11.0, 11.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (968, 29, 1, 'ORANGE', 'FAILURE', '预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', 31.8, 35.0, 46.0, 26.4, 23.6, 27.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (969, 31, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', 20.8, 69.0, 26.0, 18.9, 9.9, 27.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (970, 32, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (971, 32, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (972, 32, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 27.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (973, 34, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', 19.5, 67.0, 18.5, 31.8, 16.4, 13.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (974, 36, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', 20.1, 82.0, 15.0, 30.5, 22.4, 19.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (975, 39, 1, 'ORANGE', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', 32.4, 78.0, 46.5, 29.5, 9.7, 36.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (976, 42, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', 18.5, 87.0, 17.0, 27.6, 24.7, 9.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (977, 52, 1, 'YELLOW', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', 16.2, 59.0, 30.0, 10.8, 8.4, 8.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (978, 54, 1, 'ORANGE', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (979, 54, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (980, 54, 1, 'RED', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 42.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (981, 56, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', 15.3, 75.0, 13.0, 18.9, 29.5, 8.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (982, 57, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', 21.3, 69.0, 19.5, 29.7, 16.2, 23.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (983, 65, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', 18.1, 65.0, 17.5, 33.0, 8.2, 12.4, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (984, 68, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', 17.3, 100.0, 10.5, 16.0, 21.5, 31.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (985, 70, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', 15.6, 97.0, 19.0, 13.9, 10.6, 19.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (986, 71, 1, 'ORANGE', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', 31.0, 58.0, 43.0, 30.8, 11.8, 32.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (987, 72, 1, 'YELLOW', 'FAILURE', '预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', 26.0, 48.0, 43.5, 29.2, 6.9, 12.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (988, 74, 1, 'YELLOW', 'FAILURE', '预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', 21.1, 49.0, 28.0, 15.3, 25.5, 18.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (989, 76, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', 32.1, 68.0, 37.5, 43.5, 10.6, 32.4, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (990, 78, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (991, 78, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (992, 78, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 35.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (993, 84, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', 27.0, 58.0, 49.0, 26.4, 12.5, 6.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (994, 85, 1, 'YELLOW', 'FAILURE', '预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', 25.5, 53.0, 34.5, 21.2, 21.5, 24.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (995, 88, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', 29.1, 77.0, 34.0, 22.4, 13.5, 48.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (996, 89, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', 18.4, 100.0, 10.0, 31.8, 13.3, 24.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 16:32:48', '2026-07-15 16:32:48', 1);
INSERT INTO `alert_record` VALUES (997, 2, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', 18.2, 56.0, 28.5, 8.1, 9.2, 24.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (998, 3, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', 16.3, 87.0, 18.0, 17.0, 5.4, 24.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (999, 5, 1, 'YELLOW', 'FAILURE', '预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', 23.3, 54.0, 40.0, 14.8, 10.5, 20.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1000, 12, 1, 'YELLOW', 'FAILURE', '预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', 24.2, 57.0, 28.5, 29.8, 10.3, 26.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1001, 13, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', 17.9, 77.0, 14.0, 32.0, 25.9, 5.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1002, 14, 1, 'ORANGE', 'FAILURE', '预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', 30.5, 37.0, 51.0, 27.7, 13.9, 18.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1003, 15, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', 21.3, 93.0, 20.5, 21.1, 6.7, 39.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1004, 19, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', 29.9, 71.0, 37.5, 24.2, 20.8, 38.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1005, 20, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', 19.6, 70.0, 25.0, 26.4, 9.9, 14.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:10', '2026-07-15 21:28:10', 1);
INSERT INTO `alert_record` VALUES (1006, 21, 1, 'YELLOW', 'FAILURE', '预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', 27.1, 41.0, 39.5, 20.1, 8.0, 35.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1007, 23, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', 19.3, 67.0, 18.5, 14.5, 26.3, 26.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1008, 28, 1, 'YELLOW', 'FAILURE', '预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', 22.1, 55.0, 37.5, 20.5, 11.0, 11.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1009, 29, 1, 'ORANGE', 'FAILURE', '预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', 31.8, 35.0, 46.0, 26.4, 23.6, 27.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1010, 31, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', 20.8, 69.0, 26.0, 18.9, 9.9, 27.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1011, 32, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1012, 32, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1013, 32, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 27.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1014, 34, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', 19.5, 67.0, 18.5, 31.8, 16.4, 13.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1015, 36, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', 20.1, 82.0, 15.0, 30.5, 22.4, 19.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1016, 39, 1, 'ORANGE', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', 32.4, 78.0, 46.5, 29.5, 9.7, 36.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1017, 42, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', 18.5, 87.0, 17.0, 27.6, 24.7, 9.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1018, 52, 1, 'YELLOW', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', 16.2, 59.0, 30.0, 10.8, 8.4, 8.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1019, 54, 1, 'ORANGE', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1020, 54, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1021, 54, 1, 'RED', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 42.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1022, 56, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', 15.3, 75.0, 13.0, 18.9, 29.5, 8.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1023, 57, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', 21.3, 69.0, 19.5, 29.7, 16.2, 23.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1024, 65, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', 18.1, 65.0, 17.5, 33.0, 8.2, 12.4, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1025, 68, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', 17.3, 100.0, 10.5, 16.0, 21.5, 31.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1026, 70, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', 15.6, 97.0, 19.0, 13.9, 10.6, 19.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1027, 71, 1, 'ORANGE', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', 31.0, 58.0, 43.0, 30.8, 11.8, 32.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1028, 72, 1, 'YELLOW', 'FAILURE', '预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', 26.0, 48.0, 43.5, 29.2, 6.9, 12.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1029, 74, 1, 'YELLOW', 'FAILURE', '预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', 21.1, 49.0, 28.0, 15.3, 25.5, 18.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1030, 76, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', 32.1, 68.0, 37.5, 43.5, 10.6, 32.4, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1031, 78, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1032, 78, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1033, 78, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 35.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1034, 84, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', 27.0, 58.0, 49.0, 26.4, 12.5, 6.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1035, 85, 1, 'YELLOW', 'FAILURE', '预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', 25.5, 53.0, 34.5, 21.2, 21.5, 24.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1036, 88, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', 29.1, 77.0, 34.0, 22.4, 13.5, 48.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1037, 89, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', 18.4, 100.0, 10.0, 31.8, 13.3, 24.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:11', '2026-07-15 21:28:11', 1);
INSERT INTO `alert_record` VALUES (1038, 2, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', 18.2, 56.0, 28.5, 8.1, 9.2, 24.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1039, 3, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', 16.3, 87.0, 18.0, 17.0, 5.4, 24.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1040, 5, 1, 'YELLOW', 'FAILURE', '预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', 23.3, 54.0, 40.0, 14.8, 10.5, 20.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1041, 12, 1, 'YELLOW', 'FAILURE', '预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', 24.2, 57.0, 28.5, 29.8, 10.3, 26.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1042, 13, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', 17.9, 77.0, 14.0, 32.0, 25.9, 5.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1043, 14, 1, 'ORANGE', 'FAILURE', '预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', 30.5, 37.0, 51.0, 27.7, 13.9, 18.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1044, 15, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', 21.3, 93.0, 20.5, 21.1, 6.7, 39.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1045, 19, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', 29.9, 71.0, 37.5, 24.2, 20.8, 38.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1046, 20, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', 19.6, 70.0, 25.0, 26.4, 9.9, 14.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1047, 21, 1, 'YELLOW', 'FAILURE', '预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', 27.1, 41.0, 39.5, 20.1, 8.0, 35.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1048, 23, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', 19.3, 67.0, 18.5, 14.5, 26.3, 26.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1049, 28, 1, 'YELLOW', 'FAILURE', '预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', 22.1, 55.0, 37.5, 20.5, 11.0, 11.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1050, 29, 1, 'ORANGE', 'FAILURE', '预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', 31.8, 35.0, 46.0, 26.4, 23.6, 27.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1051, 31, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', 20.8, 69.0, 26.0, 18.9, 9.9, 27.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1052, 32, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1053, 32, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1054, 32, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 27.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1055, 34, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', 19.5, 67.0, 18.5, 31.8, 16.4, 13.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1056, 36, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', 20.1, 82.0, 15.0, 30.5, 22.4, 19.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1057, 39, 1, 'ORANGE', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', 32.4, 78.0, 46.5, 29.5, 9.7, 36.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1058, 42, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', 18.5, 87.0, 17.0, 27.6, 24.7, 9.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1059, 52, 1, 'YELLOW', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', 16.2, 59.0, 30.0, 10.8, 8.4, 8.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1060, 54, 1, 'ORANGE', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1061, 54, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1062, 54, 1, 'RED', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 42.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1063, 56, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', 15.3, 75.0, 13.0, 18.9, 29.5, 8.2, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1064, 57, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', 21.3, 69.0, 19.5, 29.7, 16.2, 23.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1065, 65, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', 18.1, 65.0, 17.5, 33.0, 8.2, 12.4, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1066, 68, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', 17.3, 100.0, 10.5, 16.0, 21.5, 31.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1067, 70, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', 15.6, 97.0, 19.0, 13.9, 10.6, 19.1, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1068, 71, 1, 'ORANGE', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', 31.0, 58.0, 43.0, 30.8, 11.8, 32.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1069, 72, 1, 'YELLOW', 'FAILURE', '预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', 26.0, 48.0, 43.5, 29.2, 6.9, 12.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1070, 74, 1, 'YELLOW', 'FAILURE', '预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', 21.1, 49.0, 28.0, 15.3, 25.5, 18.3, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1071, 76, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', 32.1, 68.0, 37.5, 43.5, 10.6, 32.4, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1072, 78, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1073, 78, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1074, 78, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 35.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1075, 84, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', 27.0, 58.0, 49.0, 26.4, 12.5, 6.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1076, 85, 1, 'YELLOW', 'FAILURE', '预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', 25.5, 53.0, 34.5, 21.2, 21.5, 24.8, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1077, 88, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', 29.1, 77.0, 34.0, 22.4, 13.5, 48.0, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1078, 89, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', 18.4, 100.0, 10.0, 31.8, 13.3, 24.6, 0.0, 'ARCHIVED', NULL, NULL, NULL, '2026-07-08 21:28:23', '2026-07-15 21:28:23', 1);
INSERT INTO `alert_record` VALUES (1079, 2, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', 18.2, 56.0, 28.5, 8.1, 9.2, 24.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1080, 3, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', 16.3, 87.0, 18.0, 17.0, 5.4, 24.8, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1081, 5, 1, 'YELLOW', 'FAILURE', '预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', 23.3, 54.0, 40.0, 14.8, 10.5, 20.3, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1082, 12, 1, 'YELLOW', 'FAILURE', '预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', 24.2, 57.0, 28.5, 29.8, 10.3, 26.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1083, 13, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', 17.9, 77.0, 14.0, 32.0, 25.9, 5.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1084, 14, 1, 'ORANGE', 'FAILURE', '预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', 30.5, 37.0, 51.0, 27.7, 13.9, 18.1, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1085, 15, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', 21.3, 93.0, 20.5, 21.1, 6.7, 39.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1086, 19, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', 29.9, 71.0, 37.5, 24.2, 20.8, 38.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1087, 20, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', 19.6, 70.0, 25.0, 26.4, 9.9, 14.0, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1088, 21, 1, 'YELLOW', 'FAILURE', '预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', 27.1, 41.0, 39.5, 20.1, 8.0, 35.3, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1089, 23, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', 19.3, 67.0, 18.5, 14.5, 26.3, 26.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1090, 28, 1, 'YELLOW', 'FAILURE', '预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', 22.1, 55.0, 37.5, 20.5, 11.0, 11.0, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1091, 29, 1, 'ORANGE', 'FAILURE', '预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', 31.8, 35.0, 46.0, 26.4, 23.6, 27.7, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1092, 31, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', 20.8, 69.0, 26.0, 18.9, 9.9, 27.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1093, 32, 1, 'YELLOW', 'FAILURE', '预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1094, 32, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', 17.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1095, 32, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 27.6, 56.0, 28.0, 12.2, 23.2, 6.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1096, 34, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', 19.5, 67.0, 18.5, 31.8, 16.4, 13.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1097, 36, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', 20.1, 82.0, 15.0, 30.5, 22.4, 19.5, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1098, 39, 1, 'ORANGE', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', 32.4, 78.0, 46.5, 29.5, 9.7, 36.5, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1099, 42, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', 18.5, 87.0, 17.0, 27.6, 24.7, 9.7, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1100, 52, 1, 'YELLOW', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', 16.2, 59.0, 30.0, 10.8, 8.4, 8.8, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1101, 54, 1, 'ORANGE', 'FAILURE', '预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1102, 54, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', 32.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1103, 54, 1, 'RED', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 42.4, 59.0, 40.0, 44.6, 25.1, 17.5, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1104, 56, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', 15.3, 75.0, 13.0, 18.9, 29.5, 8.2, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1105, 57, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', 21.3, 69.0, 19.5, 29.7, 16.2, 23.1, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1106, 65, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', 18.1, 65.0, 17.5, 33.0, 8.2, 12.4, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1107, 68, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', 17.3, 100.0, 10.5, 16.0, 21.5, 31.8, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1108, 70, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', 15.6, 97.0, 19.0, 13.9, 10.6, 19.1, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1109, 71, 1, 'ORANGE', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', 31.0, 58.0, 43.0, 30.8, 11.8, 32.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1110, 72, 1, 'YELLOW', 'FAILURE', '预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', 26.0, 48.0, 43.5, 29.2, 6.9, 12.3, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1111, 74, 1, 'YELLOW', 'FAILURE', '预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', 21.1, 49.0, 28.0, 15.3, 25.5, 18.3, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1112, 76, 1, 'ORANGE', 'HOMEWORK', '作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', 32.1, 68.0, 37.5, 43.5, 10.6, 32.4, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1113, 78, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1114, 78, 1, 'YELLOW', 'DROP', '成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', 25.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1115, 78, 1, 'ORANGE', 'CUMULATIVE', '多维度风险叠加：同时存在2种风险问题，需要重点关注和干预', 35.3, 58.0, 31.0, 25.5, 15.4, 28.7, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1116, 84, 1, 'YELLOW', 'FAILURE', '预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', 27.0, 58.0, 49.0, 26.4, 12.5, 6.8, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1117, 85, 1, 'YELLOW', 'FAILURE', '预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', 25.5, 53.0, 34.5, 21.2, 21.5, 24.8, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1118, 88, 1, 'YELLOW', 'KNOWLEDGE', '知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', 29.1, 77.0, 34.0, 22.4, 13.5, 48.0, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);
INSERT INTO `alert_record` VALUES (1119, 89, 1, 'YELLOW', 'HOMEWORK', '作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', 18.4, 100.0, 10.0, 31.8, 13.3, 24.6, 0.0, 'DISMISSED', '', '2026-07-08 21:31:13', NULL, '2026-07-08 21:29:30', '2026-07-15 21:29:30', 1);

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
-- Records of alert_rule_config
-- ----------------------------
INSERT INTO `alert_rule_config` VALUES (1, 'FRESHMAN', NULL, 0.350, 0.250, 0.200, 0.150, 0.050, 40.0, 30.0, 15.0, 70.0, 50.0, 80.0, 15.0, 50.0, 30.0, 20.0, 25.0, 15.0, 0.0, 1, '新生体系默认规则', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `alert_rule_config` VALUES (2, 'SENIOR', NULL, 0.300, 0.200, 0.150, 0.100, 0.250, 40.0, 30.0, 15.0, 70.0, 50.0, 80.0, 15.0, 50.0, 30.0, 20.0, 25.0, 15.0, 0.0, 1, '老生体系默认规则', '2026-05-21 14:55:40', '2026-05-21 14:55:40');

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
) ENGINE = InnoDB AUTO_INCREMENT = 491 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警历史快照表(趋势分析和回测)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of alert_snapshot
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 534 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课堂表现表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of class_performance
-- ----------------------------
INSERT INTO `class_performance` VALUES (1, 1, 1, 39, 0, 6, 60, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (2, 2, 1, 36, 1, 1, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (3, 3, 1, 38, 2, 1, 90, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (4, 4, 1, 30, 3, 6, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (5, 5, 1, 38, 3, 0, 67, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (6, 6, 1, 31, 0, 2, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (7, 7, 1, 40, 4, 5, 61, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (8, 8, 1, 34, 4, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (9, 9, 1, 38, 3, 2, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (10, 10, 1, 34, 0, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (11, 11, 1, 33, 0, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (12, 12, 1, 34, 2, 0, 63, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (13, 13, 1, 34, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (14, 14, 1, 38, 3, 5, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (15, 15, 1, 39, 2, 1, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (16, 16, 1, 32, 3, 6, 84, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (17, 17, 1, 40, 3, 0, 60, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (18, 18, 1, 37, 4, 0, 48, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (19, 19, 1, 38, 0, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (20, 20, 1, 38, 1, 3, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (21, 21, 1, 32, 1, 0, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (22, 22, 1, 31, 0, 2, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (23, 23, 1, 32, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (24, 24, 1, 35, 3, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (25, 25, 1, 40, 2, 4, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (26, 26, 1, 31, 3, 4, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (27, 27, 1, 33, 0, 6, 100, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (28, 28, 1, 32, 1, 2, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (29, 29, 1, 36, 2, 1, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (30, 30, 1, 33, 4, 2, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (31, 31, 1, 40, 3, 2, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (32, 32, 1, 31, 2, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (33, 33, 1, 32, 3, 6, 86, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (34, 34, 1, 31, 2, 6, 63, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (35, 35, 1, 33, 4, 1, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (36, 36, 1, 33, 1, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (37, 37, 1, 40, 3, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (38, 38, 1, 30, 4, 3, 89, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (39, 39, 1, 33, 1, 2, 68, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (40, 40, 1, 38, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (41, 41, 1, 33, 3, 0, 95, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (42, 42, 1, 36, 1, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (43, 43, 1, 34, 4, 6, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (44, 44, 1, 39, 2, 5, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (45, 45, 1, 39, 4, 5, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (46, 46, 1, 33, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (47, 47, 1, 35, 3, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (48, 48, 1, 38, 3, 6, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (49, 49, 1, 34, 1, 1, 93, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (50, 50, 1, 34, 3, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (51, 51, 1, 34, 1, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (52, 52, 1, 37, 0, 0, 58, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (53, 53, 1, 30, 4, 2, 62, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (54, 54, 1, 35, 0, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (55, 55, 1, 31, 0, 2, 95, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (56, 56, 1, 37, 4, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (57, 57, 1, 35, 3, 6, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (58, 58, 1, 33, 3, 6, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (59, 59, 1, 30, 4, 2, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (60, 60, 1, 37, 2, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (61, 61, 1, 38, 1, 4, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (62, 62, 1, 39, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (63, 63, 1, 33, 1, 2, 53, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (64, 64, 1, 33, 0, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (65, 65, 1, 34, 2, 1, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (66, 66, 1, 40, 0, 3, 52, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (67, 67, 1, 39, 2, 1, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (68, 68, 1, 39, 0, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (69, 69, 1, 33, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (70, 70, 1, 31, 1, 6, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (71, 71, 1, 32, 1, 6, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (72, 72, 1, 38, 1, 0, 72, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (73, 73, 1, 33, 0, 0, 92, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (74, 74, 1, 40, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (75, 75, 1, 31, 0, 2, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (76, 76, 1, 34, 0, 2, 56, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (77, 77, 1, 30, 0, 4, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (78, 78, 1, 31, 3, 6, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (79, 79, 1, 40, 1, 0, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (80, 80, 1, 35, 0, 1, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (81, 81, 1, 32, 3, 5, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (82, 82, 1, 39, 3, 0, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (83, 83, 1, 37, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (84, 84, 1, 32, 3, 0, 61, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (85, 85, 1, 33, 1, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (86, 86, 1, 30, 0, 2, 90, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (87, 87, 1, 31, 0, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (88, 88, 1, 34, 1, 5, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (89, 89, 1, 30, 2, 5, 75, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (90, 90, 1, 33, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (91, 91, 1, 32, 4, 1, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (92, 92, 1, 32, 2, 2, 56, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (93, 93, 1, 39, 0, 6, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (94, 94, 1, 30, 1, 4, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (95, 95, 1, 36, 3, 1, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (96, 96, 1, 37, 1, 2, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (97, 97, 1, 35, 1, 1, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (98, 98, 1, 34, 2, 6, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (99, 99, 1, 39, 2, 2, 87, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (100, 100, 1, 30, 1, 4, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (101, 101, 1, 32, 4, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (102, 102, 1, 40, 0, 4, 96, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (103, 103, 1, 36, 2, 0, 69, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (104, 104, 1, 39, 4, 1, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (105, 105, 1, 32, 0, 1, 63, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (106, 106, 1, 34, 1, 0, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (107, 107, 1, 36, 2, 6, 49, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (108, 108, 1, 37, 0, 6, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (109, 109, 1, 34, 2, 0, 82, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (110, 110, 1, 30, 0, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (111, 111, 1, 36, 4, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (112, 112, 1, 31, 0, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (113, 113, 1, 32, 3, 3, 67, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (114, 114, 1, 40, 2, 5, 100, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (115, 115, 1, 33, 3, 2, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (116, 116, 1, 30, 3, 2, 100, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (117, 117, 1, 40, 0, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (118, 118, 1, 31, 0, 3, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (119, 119, 1, 36, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (120, 120, 1, 36, 1, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (121, 121, 1, 39, 1, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (122, 122, 1, 38, 3, 1, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (123, 123, 1, 32, 2, 5, 60, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (124, 124, 1, 32, 1, 5, 61, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (125, 125, 1, 34, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (126, 126, 1, 33, 4, 4, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (127, 127, 1, 37, 3, 5, 96, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (128, 128, 1, 32, 2, 0, 73, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (129, 129, 1, 32, 3, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (130, 130, 1, 39, 3, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (131, 131, 1, 36, 3, 4, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (132, 132, 1, 36, 3, 5, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (133, 133, 1, 40, 2, 6, 54, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (134, 134, 1, 35, 3, 3, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (135, 135, 1, 35, 0, 6, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (136, 136, 1, 31, 1, 1, 53, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (137, 137, 1, 39, 2, 5, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (138, 138, 1, 31, 2, 4, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (139, 139, 1, 35, 1, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (140, 140, 1, 39, 2, 6, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (141, 141, 1, 36, 4, 3, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (142, 142, 1, 35, 2, 6, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (143, 143, 1, 34, 1, 0, 84, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (144, 144, 1, 33, 2, 0, 64, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (145, 145, 1, 35, 4, 6, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (146, 146, 1, 33, 2, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (147, 147, 1, 35, 2, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (148, 148, 1, 33, 2, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (149, 149, 1, 36, 4, 0, 50, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (150, 150, 1, 36, 4, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (151, 151, 1, 37, 0, 5, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (152, 152, 1, 36, 0, 2, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (153, 153, 1, 35, 2, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (154, 154, 1, 34, 3, 6, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (155, 155, 1, 39, 0, 2, 59, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (156, 156, 1, 36, 1, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (157, 157, 1, 40, 3, 4, 59, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (158, 158, 1, 31, 0, 5, 94, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (159, 159, 1, 39, 3, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (160, 160, 1, 38, 2, 6, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (161, 161, 1, 31, 2, 2, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (162, 162, 1, 32, 4, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (163, 163, 1, 37, 4, 4, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (164, 164, 1, 34, 4, 4, 64, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (165, 165, 1, 36, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (166, 166, 1, 34, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (167, 167, 1, 35, 1, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (168, 168, 1, 36, 1, 1, 86, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (169, 169, 1, 38, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (170, 170, 1, 36, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (171, 171, 1, 32, 4, 4, 73, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (172, 172, 1, 37, 0, 5, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (173, 173, 1, 39, 2, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (174, 174, 1, 37, 4, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (175, 175, 1, 33, 4, 0, 99, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (176, 176, 1, 31, 1, 3, 86, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (177, 177, 1, 36, 1, 0, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (178, 178, 1, 30, 2, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (179, 179, 1, 39, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (180, 180, 1, 35, 2, 4, 100, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (181, 181, 1, 31, 2, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (182, 182, 1, 30, 2, 0, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (183, 183, 1, 35, 4, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (184, 184, 1, 34, 4, 6, 64, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (185, 185, 1, 39, 3, 3, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (186, 186, 1, 33, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (187, 187, 1, 39, 3, 1, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (188, 188, 1, 30, 3, 4, 67, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (189, 189, 1, 33, 0, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (190, 190, 1, 32, 0, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (191, 191, 1, 38, 3, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (192, 192, 1, 38, 3, 2, 72, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (193, 193, 1, 34, 3, 3, 72, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (194, 194, 1, 40, 0, 3, 87, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (195, 195, 1, 30, 0, 6, 57, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (196, 196, 1, 31, 2, 4, 93, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (197, 197, 1, 39, 3, 5, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (198, 198, 1, 32, 4, 4, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (199, 199, 1, 30, 1, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (200, 200, 1, 31, 4, 2, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (201, 201, 1, 32, 3, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (202, 202, 1, 32, 1, 0, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (203, 203, 1, 37, 1, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (204, 204, 1, 40, 4, 4, 72, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (205, 205, 1, 32, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (206, 206, 1, 30, 2, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (207, 207, 1, 31, 4, 5, 94, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (208, 208, 1, 31, 2, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (209, 209, 1, 40, 0, 5, 63, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (210, 210, 1, 33, 2, 3, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (211, 211, 1, 36, 3, 2, 91, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (212, 212, 1, 35, 4, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (213, 213, 1, 34, 2, 2, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (214, 214, 1, 33, 0, 0, 94, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (215, 215, 1, 39, 3, 1, 100, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (216, 216, 1, 30, 2, 0, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (217, 217, 1, 38, 3, 6, 93, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (218, 218, 1, 38, 0, 5, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (219, 219, 1, 34, 3, 3, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (220, 220, 1, 39, 0, 2, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (221, 221, 1, 33, 1, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (222, 222, 1, 30, 2, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (223, 223, 1, 37, 0, 5, 98, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (224, 224, 1, 31, 0, 0, 85, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (225, 225, 1, 40, 4, 2, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (226, 226, 1, 33, 3, 5, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (227, 227, 1, 37, 3, 4, 94, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (228, 228, 1, 36, 1, 2, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (229, 229, 1, 38, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (230, 230, 1, 36, 0, 6, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (231, 231, 1, 33, 0, 0, 56, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (232, 232, 1, 31, 1, 6, 94, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (233, 233, 1, 36, 3, 0, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (234, 234, 1, 33, 0, 3, 47, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (235, 235, 1, 33, 3, 1, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (236, 236, 1, 35, 2, 0, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (237, 237, 1, 38, 2, 5, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (238, 238, 1, 33, 0, 1, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (239, 239, 1, 39, 3, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (240, 240, 1, 37, 1, 1, 94, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (241, 241, 1, 39, 3, 6, 84, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (242, 242, 1, 36, 1, 4, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (243, 243, 1, 34, 0, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (244, 244, 1, 38, 4, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (245, 245, 1, 36, 3, 2, 53, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (246, 246, 1, 32, 1, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (247, 247, 1, 39, 4, 6, 57, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (248, 248, 1, 34, 1, 4, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (249, 249, 1, 32, 3, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (250, 250, 1, 38, 1, 5, 46, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (251, 251, 1, 40, 0, 3, 86, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (252, 252, 1, 33, 2, 5, 94, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (253, 253, 1, 37, 0, 1, 77, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (254, 254, 1, 32, 1, 6, 96, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (255, 255, 1, 40, 2, 6, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (256, 256, 1, 40, 1, 1, 47, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (257, 257, 1, 32, 0, 3, 92, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (258, 258, 1, 33, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (259, 259, 1, 35, 4, 4, 100, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (260, 260, 1, 30, 3, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (261, 261, 1, 30, 2, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (262, 262, 1, 35, 1, 2, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (263, 263, 1, 36, 4, 4, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (264, 264, 1, 32, 0, 2, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (265, 265, 1, 35, 4, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (266, 266, 1, 33, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (267, 267, 1, 34, 4, 4, 92, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (268, 268, 2, 31, 0, 5, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (269, 269, 2, 32, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (270, 270, 2, 31, 3, 5, 86, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (271, 271, 2, 37, 2, 2, 49, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (272, 272, 2, 40, 0, 5, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (273, 273, 2, 36, 2, 4, 89, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (274, 274, 2, 36, 1, 6, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (275, 275, 2, 38, 1, 2, 92, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (276, 276, 2, 30, 1, 0, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (277, 277, 2, 36, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (278, 278, 2, 35, 2, 3, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (279, 279, 2, 35, 2, 2, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (280, 280, 2, 39, 0, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (281, 281, 2, 34, 1, 3, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (282, 282, 2, 36, 4, 4, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (283, 283, 2, 39, 3, 5, 54, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (284, 284, 2, 32, 2, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (285, 285, 2, 32, 0, 6, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (286, 286, 2, 33, 3, 1, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (287, 287, 2, 32, 1, 1, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (288, 288, 2, 36, 0, 1, 100, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (289, 289, 2, 31, 4, 3, 89, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (290, 290, 2, 37, 0, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (291, 291, 2, 35, 3, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (292, 292, 2, 37, 0, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (293, 293, 2, 30, 0, 3, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (294, 294, 2, 38, 1, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (295, 295, 2, 30, 1, 2, 100, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (296, 296, 2, 32, 0, 3, 35, '消极', '消极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (297, 297, 2, 31, 3, 4, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (298, 298, 2, 33, 3, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (299, 299, 2, 31, 0, 4, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (300, 300, 2, 40, 0, 4, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (301, 301, 2, 37, 0, 0, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (302, 302, 2, 32, 4, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (303, 303, 2, 39, 3, 5, 61, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (304, 304, 2, 32, 1, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (305, 305, 2, 31, 0, 2, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (306, 306, 2, 34, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (307, 307, 2, 38, 1, 6, 41, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (308, 308, 2, 40, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (309, 309, 2, 30, 3, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (310, 310, 2, 35, 2, 0, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (311, 311, 2, 33, 1, 0, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (312, 312, 2, 30, 0, 5, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (313, 313, 2, 39, 1, 0, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (314, 314, 2, 40, 4, 6, 57, '消极', '消极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (315, 315, 2, 34, 1, 0, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (316, 316, 2, 38, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (317, 317, 2, 36, 0, 3, 56, '消极', '消极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (318, 318, 2, 39, 2, 4, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (319, 319, 2, 37, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (320, 320, 2, 34, 2, 6, 86, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (321, 321, 2, 33, 2, 3, 64, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (322, 322, 2, 37, 0, 4, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (323, 323, 2, 32, 2, 1, 85, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (324, 324, 2, 30, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (325, 325, 2, 38, 4, 1, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (326, 326, 2, 34, 2, 6, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (327, 327, 2, 31, 4, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (328, 328, 2, 33, 1, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (329, 329, 2, 38, 0, 3, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (330, 330, 2, 36, 0, 6, 69, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (331, 331, 2, 38, 4, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (332, 332, 2, 36, 0, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (333, 333, 2, 37, 2, 2, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (334, 334, 2, 40, 4, 2, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (335, 335, 2, 31, 0, 6, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (336, 336, 2, 31, 1, 2, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (337, 337, 2, 35, 1, 0, 98, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (338, 338, 2, 34, 2, 5, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (339, 339, 2, 36, 4, 1, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (340, 340, 2, 40, 2, 2, 61, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (341, 341, 2, 33, 2, 2, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (342, 342, 2, 33, 3, 4, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (343, 343, 2, 39, 3, 2, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (344, 344, 2, 39, 3, 2, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (345, 345, 2, 33, 1, 0, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (346, 346, 2, 36, 3, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (347, 347, 2, 34, 3, 1, 72, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (348, 348, 2, 38, 2, 6, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (349, 349, 2, 35, 4, 2, 81, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (350, 350, 2, 35, 1, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (351, 351, 2, 34, 3, 4, 100, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (352, 352, 2, 40, 1, 3, 100, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (353, 353, 2, 33, 1, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (354, 354, 2, 40, 4, 3, 96, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (355, 355, 2, 37, 3, 3, 64, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (356, 356, 2, 36, 2, 1, 71, '消极', '消极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (357, 357, 2, 37, 1, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (358, 358, 2, 40, 3, 2, 100, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (359, 359, 2, 37, 3, 0, 66, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (360, 360, 2, 40, 1, 3, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (361, 361, 2, 40, 4, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (362, 362, 2, 40, 2, 3, 86, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (363, 363, 2, 31, 3, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (364, 364, 2, 31, 2, 1, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (365, 365, 2, 30, 0, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (366, 366, 2, 33, 0, 2, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (367, 367, 2, 35, 1, 6, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (368, 368, 2, 40, 2, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (369, 369, 2, 35, 2, 4, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (370, 370, 2, 30, 0, 3, 61, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (371, 371, 2, 37, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (372, 372, 2, 35, 2, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (373, 373, 2, 37, 0, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (374, 374, 2, 30, 2, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (375, 375, 2, 38, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (376, 376, 2, 34, 3, 2, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (377, 377, 2, 34, 3, 4, 99, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (378, 378, 2, 36, 4, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (379, 379, 2, 34, 0, 3, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (380, 380, 2, 32, 3, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (381, 381, 2, 37, 2, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (382, 382, 2, 37, 4, 5, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (383, 383, 2, 32, 2, 4, 88, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (384, 384, 2, 39, 3, 2, 98, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (385, 385, 2, 36, 2, 2, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (386, 386, 2, 35, 3, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-21 08:12:10');
INSERT INTO `class_performance` VALUES (387, 387, 1, 36, 2, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (388, 388, 1, 31, 0, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (389, 389, 1, 33, 3, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (390, 390, 1, 40, 4, 5, 96, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (391, 391, 1, 37, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (392, 392, 1, 36, 1, 6, 73, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (393, 393, 1, 31, 4, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (394, 394, 1, 31, 2, 5, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (395, 395, 1, 39, 3, 6, 61, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (396, 396, 1, 33, 2, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (397, 397, 1, 33, 1, 2, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (398, 398, 1, 32, 4, 5, 51, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (399, 399, 1, 40, 3, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (400, 400, 1, 34, 4, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (401, 401, 1, 30, 2, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (402, 402, 1, 33, 2, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (403, 403, 1, 35, 2, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (404, 404, 1, 39, 2, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (405, 405, 1, 31, 3, 2, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (406, 406, 1, 31, 3, 5, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (407, 407, 1, 31, 1, 6, 50, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (408, 408, 1, 38, 4, 3, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (409, 409, 1, 40, 0, 0, 90, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (410, 410, 1, 32, 1, 4, 53, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (411, 411, 1, 33, 1, 5, 87, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (412, 412, 1, 31, 4, 1, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (413, 413, 1, 35, 4, 1, 100, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (414, 414, 1, 39, 3, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (415, 415, 1, 30, 2, 4, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (416, 416, 1, 34, 0, 3, 93, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (417, 417, 1, 31, 0, 1, 96, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (418, 418, 1, 34, 0, 0, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (419, 419, 1, 39, 4, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (420, 420, 1, 37, 4, 2, 83, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (421, 421, 1, 37, 3, 2, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (422, 422, 1, 35, 4, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (423, 423, 1, 33, 4, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (424, 424, 1, 31, 2, 1, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (425, 425, 1, 30, 0, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (426, 426, 1, 36, 2, 2, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (427, 427, 1, 40, 3, 6, 72, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (428, 428, 1, 34, 3, 0, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (429, 429, 1, 33, 1, 2, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (430, 430, 1, 31, 1, 5, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (431, 431, 1, 39, 2, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (432, 432, 1, 33, 2, 4, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (433, 433, 1, 37, 3, 3, 83, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (434, 434, 1, 35, 2, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (435, 435, 1, 30, 3, 4, 67, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (436, 436, 1, 39, 2, 1, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (437, 437, 1, 33, 1, 6, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (438, 438, 1, 38, 0, 0, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (439, 439, 1, 34, 1, 1, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (440, 440, 1, 36, 2, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (441, 441, 1, 33, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (442, 442, 1, 39, 0, 3, 88, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (443, 443, 1, 37, 4, 0, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (444, 444, 1, 37, 2, 2, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (445, 445, 1, 34, 1, 5, 78, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (446, 446, 1, 40, 4, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (447, 447, 1, 37, 3, 0, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (448, 448, 1, 30, 1, 5, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (449, 449, 1, 30, 0, 4, 67, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (450, 450, 1, 39, 4, 6, 90, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (451, 451, 1, 31, 3, 1, 44, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (452, 452, 1, 32, 3, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (453, 453, 1, 30, 2, 5, 58, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (454, 454, 1, 37, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (455, 455, 1, 38, 4, 3, 65, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (456, 456, 1, 36, 3, 1, 77, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (457, 457, 1, 35, 2, 4, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (458, 458, 1, 35, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (459, 459, 1, 34, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (460, 460, 1, 39, 2, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (461, 461, 1, 33, 4, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (462, 462, 1, 33, 2, 0, 59, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (463, 463, 1, 32, 3, 3, 85, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (464, 464, 1, 39, 2, 1, 74, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (465, 465, 1, 32, 4, 6, 86, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (466, 466, 1, 34, 2, 1, 54, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (467, 467, 1, 30, 4, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (468, 468, 1, 36, 0, 3, 63, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (469, 469, 1, 33, 2, 6, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (470, 470, 1, 39, 1, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (471, 471, 1, 31, 0, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (472, 472, 1, 37, 4, 1, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (473, 473, 1, 38, 2, 2, 63, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (474, 474, 1, 32, 1, 6, 85, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (475, 475, 1, 38, 4, 3, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (476, 476, 1, 40, 1, 6, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (477, 477, 1, 33, 0, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (478, 478, 1, 31, 4, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (479, 479, 1, 32, 2, 6, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (480, 480, 1, 38, 1, 0, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (481, 481, 1, 32, 2, 1, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (482, 482, 1, 35, 0, 2, 56, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (483, 483, 1, 38, 2, 6, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (484, 484, 1, 35, 2, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (485, 485, 1, 39, 0, 2, 69, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (486, 486, 1, 34, 2, 4, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (487, 487, 1, 30, 0, 3, 79, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (488, 488, 1, 38, 3, 6, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (489, 489, 1, 38, 2, 6, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (490, 490, 1, 36, 3, 1, 91, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (491, 491, 1, 34, 1, 2, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (492, 492, 1, 31, 3, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (493, 493, 1, 39, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (494, 494, 1, 36, 0, 3, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (495, 495, 1, 31, 0, 1, 92, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (496, 496, 1, 36, 0, 2, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (497, 497, 1, 39, 2, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (498, 498, 1, 30, 2, 6, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (499, 499, 1, 30, 0, 4, 84, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (500, 500, 1, 32, 2, 5, 70, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (501, 501, 1, 30, 0, 5, 56, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (502, 502, 1, 30, 4, 2, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (503, 503, 1, 36, 0, 2, 53, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (504, 504, 1, 39, 0, 3, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (505, 505, 1, 38, 0, 5, 86, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (506, 506, 1, 30, 1, 3, 64, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (507, 507, 1, 34, 0, 3, 82, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (508, 508, 1, 33, 0, 1, 80, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (509, 509, 1, 38, 4, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (510, 510, 1, 30, 0, 2, 81, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (511, 511, 1, 30, 1, 4, 79, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (512, 512, 1, 34, 2, 3, 72, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (513, 513, 1, 35, 2, 6, 49, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (514, 514, 1, 33, 4, 2, 88, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (515, 515, 1, 33, 0, 2, 75, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (516, 516, 1, 39, 0, 4, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (517, 517, 1, 39, 2, 1, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (518, 518, 1, 36, 0, 0, 94, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (519, 519, 1, 36, 3, 1, 90, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (520, 520, 1, 40, 1, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (521, 521, 1, 39, 0, 4, 57, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (522, 522, 1, 35, 3, 3, 0, '消极', '消极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (523, 523, 1, 36, 0, 5, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (524, 524, 1, 39, 1, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (525, 525, 1, 39, 3, 3, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (526, 526, 1, 30, 2, 3, 68, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (527, 527, 1, 31, 0, 6, 76, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (528, 528, 1, 40, 0, 0, 97, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (529, 529, 1, 32, 0, 1, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (530, 530, 1, 37, 4, 4, 0, '优秀', '积极', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (531, 531, 1, 40, 1, 0, 62, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (532, 532, 1, 36, 3, 3, 71, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (533, 533, 1, 40, 0, 1, 58, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');
INSERT INTO `class_performance` VALUES (534, 534, 1, 34, 2, 5, 0, '一般', '一般', '2026-05-16 17:03:40', '2026-05-16 17:03:40');

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
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, '大学计算机基础（一）', 50, 0, 50, 60, 39, 9, 20, '2026-05-16 17:03:07', NULL);
INSERT INTO `course` VALUES (2, '大学生计算机基础（二）', 50, 0, 50, 60, 39, 9, 20, '2026-05-18 20:17:18', 1);

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
-- Records of course_knowledge_point
-- ----------------------------
INSERT INTO `course_knowledge_point` VALUES (1, 1, NULL, '计算机概述', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (2, 1, 1, '计算机发展史', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (3, 1, 1, '计算机系统组成', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (4, 1, 1, '信息编码与数制', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (5, 1, NULL, '操作系统基础', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (6, 1, 5, 'Windows基本操作', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (7, 1, 5, '文件与目录管理', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (8, 1, 5, '进程与内存管理', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (9, 1, NULL, '办公软件应用', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (10, 1, 9, 'Word文档排版', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (11, 1, 9, 'Excel公式与函数', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (12, 1, 9, 'PowerPoint演示文稿', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (13, 1, NULL, '计算机网络基础', 4, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (14, 1, 13, 'OSI七层模型', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (15, 1, 13, 'TCP/IP协议栈', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (16, 1, 13, 'IP地址与子网划分', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (17, 1, NULL, '信息安全基础', 5, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (18, 1, 17, '密码学基础', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (19, 1, 17, '网络安全威胁', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (20, 1, 17, '数据加密技术', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (21, 2, NULL, '程序设计基础', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (22, 2, 21, '算法与流程图', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (23, 2, 21, '顺序与选择结构', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (24, 2, 21, '循环结构', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (25, 2, NULL, '数据结构', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (26, 2, 25, '数组与链表', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (27, 2, 25, '栈与队列', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (28, 2, 25, '树与二叉树', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (29, 2, NULL, '数据库基础', 3, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (30, 2, 29, '关系型数据库', 1, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (31, 2, 29, 'SQL查询语句', 2, NULL, '2026-06-24 20:17:04');
INSERT INTO `course_knowledge_point` VALUES (32, 2, 29, '数据库表设计', 3, NULL, '2026-06-24 20:17:04');

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
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_kp`(`course_id` ASC, `kp_id` ASC) USING BTREE,
  INDEX `idx_teacher_id`(`teacher_id` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习题表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exercise
-- ----------------------------
INSERT INTO `exercise` VALUES (4, 1, 1, 8, '进程调度算法分析', '假设有5个进程同时到达，运行时间为2、4、1、3、5个时间单位，请分别使用FCFS和SJF算法计算平均等待时间。', NULL, NULL, 'HARD', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (5, 1, 1, 11, 'Excel函数应用综合题', '给定学生成绩表，请使用VLOOKUP查找学生各科成绩，并用IF函数判定及格情况。', NULL, NULL, 'MEDIUM', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (6, 1, 1, 15, 'TCP三次握手练习', '请画图并说明TCP协议的三次握手过程，解释SYN和ACK标志位的含义。', NULL, NULL, 'MEDIUM', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (7, 1, 1, 16, '子网划分练习题', '给定192.168.1.0/24，需要划分4个子网，请写出每个子网的网络地址、广播地址和可用IP范围。', NULL, NULL, 'HARD', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (8, 1, 1, 11, 'Excel数据透视表练习', '使用提供的销售数据，创建数据透视表，按月份和产品类别汇总销售额。', NULL, NULL, 'MEDIUM', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (9, 1, 1, 10, 'Word长文档排版', '请对一个包含多级标题的论文设置自动目录、页眉页脚、页码格式。要求使用样式功能。', NULL, NULL, 'MEDIUM', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (10, 1, 1, 18, '密码学加密练习', '使用凯撒密码（移位为3）加密：HELLO WORLD，简述对称加密和非对称加密的区别。', NULL, NULL, 'EASY', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (11, 2, 2, 23, '条件判断练习题', '编写一个程序，输入三个整数，输出其中的最大值。要求使用if-else结构。', NULL, NULL, 'EASY', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (12, 2, 2, 24, '循环嵌套练习', '使用双重循环打印九九乘法表，要求格式对齐。', NULL, NULL, 'EASY', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (13, 2, 2, 26, '链表操作练习', '请实现单链表的创建、插入和删除操作，并分析各操作的时间复杂度。', NULL, NULL, 'MEDIUM', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (14, 2, 2, 31, 'SQL查询综合练习', '给定学生表和成绩表，请写出SQL：1.平均成绩高于80分的学生；2.每门课程的最高分和最低分。', NULL, NULL, 'MEDIUM', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (15, 2, 2, 31, '多表连接查询', '使用INNER JOIN查询所有学生的选课信息，包括学生姓名、课程名称和成绩。', NULL, NULL, 'EASY', NULL, NULL, 1, '2026-06-24 20:17:04');
INSERT INTO `exercise` VALUES (16, 1, 1, NULL, '测试001', '', '/uploads/exercises/b9460a4a-67ba-484c-864f-41877f4f6204.pdf', 'application/pdf', 'MEDIUM', NULL, NULL, 1, '2026-07-08 13:38:45');
INSERT INTO `exercise` VALUES (17, 1, 1, NULL, '测试001', '', '/uploads/exercises/7100ba54-a968-4307-a654-d2cc4bfe94c5.pdf', 'application/pdf', 'MEDIUM', NULL, NULL, 1, '2026-07-08 16:04:19');
INSERT INTO `exercise` VALUES (18, 1, 1, NULL, '测试002', '', '/uploads/exercises/b610ce77-d413-4415-8ad3-456090ab3643.pdf', 'application/pdf', 'MEDIUM', NULL, NULL, 1, '2026-07-08 16:04:37');
INSERT INTO `exercise` VALUES (19, 1, 1, NULL, '学生1', '', '/uploads/exercises/da2dc873-13b8-4f67-9a21-30707ea219b5.pdf', 'application/pdf', 'HARD', NULL, NULL, 1, '2026-07-08 21:00:16');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习题与知识点关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exercise_knowledge_point
-- ----------------------------
INSERT INTO `exercise_knowledge_point` VALUES (1, 16, 2);
INSERT INTO `exercise_knowledge_point` VALUES (2, 16, 4);
INSERT INTO `exercise_knowledge_point` VALUES (3, 17, 2);
INSERT INTO `exercise_knowledge_point` VALUES (4, 17, 4);
INSERT INTO `exercise_knowledge_point` VALUES (5, 18, 2);
INSERT INTO `exercise_knowledge_point` VALUES (6, 18, 4);
INSERT INTO `exercise_knowledge_point` VALUES (7, 19, 6);
INSERT INTO `exercise_knowledge_point` VALUES (10, 19, 10);
INSERT INTO `exercise_knowledge_point` VALUES (8, 19, 14);
INSERT INTO `exercise_knowledge_point` VALUES (9, 19, 18);

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_exercise_id`(`exercise_id` ASC) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习推荐记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exercise_recommendation
-- ----------------------------
INSERT INTO `exercise_recommendation` VALUES (1, 3, 1, 829, '文件与目录管理', 'COMPLETED', 85, '绝对路径是从根目录开始的完整路径；相对路径是相对于当前目录的路径。常见操作：创建(mkdir)、复制(copy)、移动(move)、删除(del)。', '回答完整，理解正确。建议进一步练习命令行操作。', '2026-06-24 09:00:00', '2026-06-24 15:30:00');
INSERT INTO `exercise_recommendation` VALUES (2, 6, 1, 829, 'TCP/IP协议栈', 'PENDING', NULL, NULL, NULL, '2026-06-24 20:00:00', NULL);
INSERT INTO `exercise_recommendation` VALUES (3, 5, 15, 836, 'Excel公式与函数', 'COMPLETED', 92, 'VLOOKUP(lookup_value, table_array, col_index, FALSE)查找成绩；IF(score>=60, \"及格\", \"不及格\")判定。', '完全正确，Excel函数运用熟练。', '2026-06-23 10:00:00', '2026-06-24 08:15:00');
INSERT INTO `exercise_recommendation` VALUES (4, 1, 15, 836, '计算机系统组成', 'PENDING', NULL, NULL, NULL, '2026-06-24 20:00:00', NULL);
INSERT INTO `exercise_recommendation` VALUES (5, 16, 1, 955, '计算机发展史、信息编码与数制', 'PENDING', NULL, NULL, NULL, '2026-07-08 16:40:24', NULL);

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
-- Records of exercise_sub_kp
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '习题子题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exercise_sub_question
-- ----------------------------
INSERT INTO `exercise_sub_question` VALUES (6, 18, 1, '111', 2, '', 5, '', '2026-07-08 19:24:06', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (7, 18, 2, '222', 2, '', 5, '', '2026-07-08 19:24:06', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (8, 18, 3, '333', 2, '', 5, '', '2026-07-08 19:24:06', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (9, 18, 4, '444', 4, '', 5, '', '2026-07-08 19:24:06', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (10, 18, 5, '555', 4, '', 5, '', '2026-07-08 19:24:06', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (16, 19, 1, '111', 6, '', 20, '', '2026-07-08 21:28:03', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (17, 19, 2, '222', 14, '', 20, '', '2026-07-08 21:28:03', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (18, 19, 3, '333', 10, '', 20, '', '2026-07-08 21:28:03', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (19, 19, 4, '444', 18, '', 20, '', '2026-07-08 21:28:03', 'SINGLE');
INSERT INTO `exercise_sub_question` VALUES (20, 19, 5, '555', 2, '', 20, '', '2026-07-08 21:28:03', 'SINGLE');

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
-- Records of history_risk
-- ----------------------------
INSERT INTO `history_risk` VALUES (1186, 268, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1187, 269, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1188, 270, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1189, 271, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1190, 272, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1191, 273, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1192, 274, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1193, 275, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1194, 276, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1195, 277, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1196, 278, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1197, 279, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1198, 280, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1199, 281, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1200, 282, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1201, 283, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1202, 284, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1203, 285, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1204, 286, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1205, 287, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1206, 288, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1207, 289, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1208, 290, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1209, 291, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1210, 292, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1211, 293, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1212, 294, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1213, 295, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1214, 296, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1215, 297, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1216, 298, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1217, 299, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1218, 300, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1219, 301, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1220, 302, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1221, 303, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1222, 304, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1223, 305, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1224, 306, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1225, 307, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1226, 308, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1227, 309, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1228, 310, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1229, 311, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1230, 312, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1231, 313, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1232, 314, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1233, 315, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1234, 316, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1235, 317, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1236, 318, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1237, 319, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1238, 320, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1239, 321, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1240, 322, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1241, 323, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1242, 324, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1243, 325, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1244, 326, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1245, 327, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1246, 328, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1247, 329, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1248, 330, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1249, 331, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1250, 332, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1251, 333, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1252, 334, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1253, 335, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1254, 336, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1255, 337, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1256, 338, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1257, 339, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1258, 340, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1259, 341, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1260, 342, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1261, 343, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1262, 344, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1263, 345, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1264, 346, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1265, 347, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1266, 348, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1267, 349, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1268, 350, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1269, 351, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1270, 352, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1271, 353, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1272, 354, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1273, 355, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1274, 356, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1275, 357, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1276, 358, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1277, 359, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1278, 360, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1279, 361, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1280, 362, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1281, 363, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1282, 364, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1283, 365, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1284, 366, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1285, 367, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1286, 368, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1287, 369, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1288, 370, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1289, 371, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1290, 372, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1291, 373, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1292, 374, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1293, 375, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1294, 376, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1295, 377, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1296, 378, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1297, 379, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1298, 380, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1299, 381, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1300, 382, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1301, 383, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1302, 384, 2, '大一下', '否', '一般', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1303, 385, 2, '大一下', '否', '稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');
INSERT INTO `history_risk` VALUES (1304, 386, 2, '大一下', '是', '不稳定', '2026-05-16 17:00:07', '2026-05-21 08:20:01');

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
) ENGINE = InnoDB AUTO_INCREMENT = 534 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作业完成情况表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of homework_info
-- ----------------------------
INSERT INTO `homework_info` VALUES (1, 1, 1, 9, 8, 1, 0, '77,70,66,82,56,63,66,72', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (2, 2, 1, 10, 10, 0, 0, '81,66,58,78,67,67,77,71,91,72', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (3, 3, 1, 8, 8, 0, 3, '89,67,92,77,87,74,83,77', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (4, 4, 1, 8, 6, 2, 3, '73,90,83,67,76,63', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (5, 5, 1, 13, 13, 0, 2, '81,61,64,77,61,77,62,64,91,53,62,54,53', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (6, 6, 1, 11, 9, 2, 3, '73,85,72,84,88,85,96,87,80', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (7, 7, 1, 15, 15, 0, 1, '79,80,65,68,95,63,69,93,80,72,66,82,63,75,85', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (8, 8, 1, 14, 11, 3, 1, '76,69,64,65,63,44,77,49,55,81,69', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (9, 9, 1, 15, 13, 2, 1, '83,79,86,85,80,91,76,79,87,90,88,96,74', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (10, 10, 1, 15, 15, 0, 2, '71,68,59,67,70,58,63,67,64,59,60,73,65,73,83', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (11, 11, 1, 16, 16, 0, 0, '70,76,72,87,80,78,84,78,84,96,70,99,81,83,75,74', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (12, 12, 1, 14, 11, 3, 3, '67,56,62,92,63,77,69,69,100,68,71', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (13, 13, 1, 8, 7, 1, 3, '53,50,56,62,79,60,60', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (14, 14, 1, 13, 11, 2, 2, '46,58,68,52,63,55,62,73,50,64,58', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (15, 15, 1, 10, 9, 1, 3, '79,73,62,77,92,87,91,84,100', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (16, 16, 1, 11, 8, 3, 3, '73,100,95,100,93,87,100,99', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (17, 17, 1, 14, 12, 2, 1, '88,89,79,88,66,81,52,64,82,72,71,65', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (18, 18, 1, 9, 9, 0, 3, '53,62,59,60,70,44,47,58,71', 58, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (19, 19, 1, 14, 12, 2, 2, '57,57,64,68,69,68,69,63,68,68,77,71', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (20, 20, 1, 9, 7, 2, 0, '61,59,77,63,70,42,73', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (21, 21, 1, 14, 13, 1, 3, '68,77,66,66,80,64,81,60,76,69,63,79,69', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (22, 22, 1, 8, 7, 1, 1, '74,72,71,72,81,70,80', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (23, 23, 1, 11, 10, 1, 0, '60,80,88,68,79,76,58,78,63,75', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (24, 24, 1, 16, 15, 1, 2, '52,68,63,60,63,55,64,46,54,66,73,63,75,57,45', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (25, 25, 1, 15, 13, 2, 0, '46,71,73,65,85,75,83,79,85,65,51,46,73', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (26, 26, 1, 14, 14, 0, 2, '73,66,88,70,90,73,84,90,85,75,100,82,76,79', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (27, 27, 1, 12, 11, 1, 3, '98,72,100,86,81,74,82,91,80,88,83', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (28, 28, 1, 14, 12, 2, 0, '60,64,58,74,58,60,57,71,71,69,62,75', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (29, 29, 1, 16, 14, 2, 3, '43,58,68,58,53,58,62,64,59,52,53,70,61,74', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (30, 30, 1, 14, 12, 2, 0, '100,67,76,70,100,73,92,76,94,86,88,79', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (31, 31, 1, 12, 11, 1, 2, '46,81,67,82,84,61,71,69,90,78,73', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (32, 32, 1, 16, 16, 0, 2, '73,77,69,64,65,68,58,77,61,77,84,76,70,74,85,81', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (33, 33, 1, 8, 8, 0, 0, '78,70,73,66,67,64,72,86', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (34, 34, 1, 11, 8, 3, 3, '88,88,83,100,90,74,86,68', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (35, 35, 1, 10, 7, 3, 0, '82,88,77,83,74,78,73', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (36, 36, 1, 8, 5, 3, 0, '85,77,83,92,91', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (37, 37, 1, 12, 11, 1, 1, '75,80,60,84,71,84,76,68,65,69,76', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (38, 38, 1, 13, 11, 2, 3, '89,76,86,80,71,52,94,83,89,72,95', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (39, 39, 1, 13, 11, 2, 2, '64,66,42,76,47,41,64,49,46,40,48', 53, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (40, 40, 1, 9, 8, 1, 3, '84,79,82,60,66,75,56,56', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (41, 41, 1, 10, 9, 1, 3, '87,71,73,61,64,63,73,69,82', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (42, 42, 1, 10, 7, 3, 0, '75,91,87,54,79,76,85', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (43, 43, 1, 12, 10, 2, 1, '59,81,65,79,77,68,75,74,89,63', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (44, 44, 1, 12, 12, 0, 0, '60,60,63,59,73,63,62,80,92,73,75,74', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (45, 45, 1, 15, 15, 0, 3, '73,71,72,66,79,79,61,70,75,93,96,71,70,77,84', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (46, 46, 1, 8, 6, 2, 3, '79,97,65,73,90,100', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (47, 47, 1, 10, 7, 3, 1, '79,85,92,85,85,81,93', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (48, 48, 1, 10, 7, 3, 2, '80,56,73,64,44,69,76', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (49, 49, 1, 11, 11, 0, 3, '97,93,79,72,100,88,91,63,78,100,90', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (50, 50, 1, 13, 11, 2, 2, '89,80,63,82,68,83,76,85,80,84,77', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (51, 51, 1, 12, 11, 1, 3, '60,71,44,56,67,69,57,76,55,54,58', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (52, 52, 1, 14, 14, 0, 1, '71,61,65,76,78,62,68,66,79,59,67,89,88,71', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (53, 53, 1, 9, 9, 0, 1, '57,58,53,62,57,53,68,47,62', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (54, 54, 1, 8, 5, 3, 2, '56,77,73,54,60', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (55, 55, 1, 10, 8, 2, 0, '88,99,86,100,100,83,74,93', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (56, 56, 1, 13, 11, 2, 0, '78,81,71,64,76,58,79,65,65,80,87', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (57, 57, 1, 10, 7, 3, 1, '77,86,68,89,74,100,71', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (58, 58, 1, 8, 8, 0, 3, '62,64,59,85,47,63,56,69', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (59, 59, 1, 11, 11, 0, 0, '77,100,92,86,75,74,88,98,79,79,75', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (60, 60, 1, 8, 7, 1, 0, '93,76,82,88,87,81,100', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (61, 61, 1, 10, 9, 1, 1, '65,67,72,76,69,61,57,62,81', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (62, 62, 1, 9, 9, 0, 1, '75,73,71,73,74,74,58,75,71', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (63, 63, 1, 13, 10, 3, 1, '70,70,67,68,42,60,61,70,69,66', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (64, 64, 1, 10, 9, 1, 3, '87,85,87,74,80,74,70,83,64', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (65, 65, 1, 11, 8, 3, 3, '72,70,90,73,87,83,75,95', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (66, 66, 1, 14, 14, 0, 2, '80,76,53,78,70,77,75,68,87,82,69,85,71,76', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (67, 67, 1, 14, 12, 2, 2, '64,70,62,59,60,69,71,57,53,52,58,56', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (68, 68, 1, 16, 14, 2, 1, '80,77,88,89,70,73,87,73,88,100,81,69,80,93', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (69, 69, 1, 8, 8, 0, 2, '64,84,88,76,89,93,64,86', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (70, 70, 1, 14, 14, 0, 3, '72,84,70,83,82,89,59,66,74,71,68,76,83,73', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (71, 71, 1, 10, 8, 2, 2, '64,55,62,64,65,61,71,72', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (72, 72, 1, 8, 7, 1, 2, '74,55,38,63,44,72,54', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (73, 73, 1, 11, 11, 0, 1, '75,73,60,78,73,100,90,64,82,72,84', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (74, 74, 1, 16, 15, 1, 2, '85,85,75,52,81,100,77,91,68,63,83,68,86,66,63', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (75, 75, 1, 8, 7, 1, 3, '67,86,89,76,79,81,70', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (76, 76, 1, 8, 5, 3, 1, '41,78,57,47,54', 55, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (77, 77, 1, 13, 11, 2, 0, '81,97,72,85,81,86,97,87,91,89,92', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (78, 78, 1, 16, 14, 2, 3, '71,60,53,72,70,41,81,53,62,74,70,68,54,53', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (79, 79, 1, 12, 11, 1, 0, '77,70,73,74,81,72,86,80,87,69,65', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (80, 80, 1, 12, 12, 0, 3, '63,72,81,94,76,75,78,67,86,92,83,91', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (81, 81, 1, 16, 16, 0, 1, '79,78,98,67,89,89,89,79,84,67,77,72,67,79,62,83', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (82, 82, 1, 10, 7, 3, 0, '90,69,82,89,78,73,100', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (83, 83, 1, 14, 14, 0, 3, '60,68,72,58,60,63,69,68,78,86,72,84,89,83', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (84, 84, 1, 12, 10, 2, 0, '42,53,48,66,36,59,42,66,47,49', 51, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (85, 85, 1, 15, 14, 1, 3, '81,49,69,74,50,65,53,82,62,63,56,61,72,70', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (86, 86, 1, 14, 13, 1, 1, '100,92,86,84,97,99,81,99,100,87,84,94,92', 92, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (87, 87, 1, 12, 10, 2, 1, '73,78,84,76,91,83,73,49,74,87', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (88, 88, 1, 16, 13, 3, 0, '82,72,61,81,46,76,82,78,66,72,68,46,62', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (89, 89, 1, 8, 5, 3, 1, '100,90,88,93,100', 94, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (90, 90, 1, 11, 9, 2, 0, '82,91,76,96,78,78,78,92,94', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (91, 91, 1, 15, 12, 3, 3, '84,58,81,87,87,81,72,88,73,63,76,75', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (92, 92, 1, 11, 11, 0, 3, '61,71,67,64,56,48,58,55,48,62,62', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (93, 93, 1, 12, 9, 3, 1, '75,62,67,65,71,68,61,44,75', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (94, 94, 1, 16, 16, 0, 0, '76,61,57,68,82,86,77,72,71,66,67,60,64,61,70,78', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (95, 95, 1, 16, 16, 0, 0, '69,66,72,72,70,65,60,76,77,73,78,57,61,71,71,73', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (96, 96, 1, 12, 9, 3, 2, '76,65,83,94,90,80,92,75,70', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (97, 97, 1, 14, 14, 0, 2, '85,69,64,56,56,73,56,67,62,55,60,54,61,60', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (98, 98, 1, 12, 10, 2, 1, '65,49,65,49,67,81,63,70,73,61', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (99, 99, 1, 8, 5, 3, 3, '86,90,98,83,85', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (100, 100, 1, 8, 5, 3, 3, '66,74,76,60,74', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (101, 101, 1, 11, 9, 2, 3, '60,61,72,78,70,53,80,79,73', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (102, 102, 1, 9, 6, 3, 2, '75,87,73,82,82,94', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (103, 103, 1, 13, 11, 2, 3, '78,63,75,91,80,84,78,87,85,89,96', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (104, 104, 1, 15, 15, 0, 1, '58,73,60,71,64,75,68,60,77,60,73,57,62,58,35', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (105, 105, 1, 9, 7, 2, 1, '64,58,66,63,37,56,58', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (106, 106, 1, 10, 8, 2, 0, '70,75,64,77,65,74,66,67', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (107, 107, 1, 9, 6, 3, 1, '61,57,53,70,69,69', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (108, 108, 1, 9, 6, 3, 3, '78,90,83,87,76,89', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (109, 109, 1, 13, 10, 3, 2, '88,83,86,89,88,80,96,97,85,74', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (110, 110, 1, 8, 7, 1, 0, '75,78,91,98,82,74,87', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (111, 111, 1, 13, 10, 3, 1, '76,86,83,81,79,80,64,73,71,82', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (112, 112, 1, 9, 9, 0, 1, '98,84,71,73,88,81,71,79,88', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (113, 113, 1, 16, 15, 1, 0, '79,71,76,62,65,79,77,66,52,69,58,65,75,62,63', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (114, 114, 1, 16, 16, 0, 1, '96,86,89,96,91,75,79,100,69,86,84,100,86,94,99,82', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (115, 115, 1, 11, 9, 2, 0, '73,91,87,82,80,62,84,90,71', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (116, 116, 1, 16, 16, 0, 0, '79,83,92,81,73,92,82,66,81,74,72,83,99,81,67,90', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (117, 117, 1, 10, 7, 3, 0, '85,63,67,61,68,74,74', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (118, 118, 1, 12, 12, 0, 2, '58,64,69,67,69,79,72,69,74,64,71,69', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (119, 119, 1, 11, 8, 3, 3, '67,74,77,50,57,67,80,80', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (120, 120, 1, 11, 8, 3, 3, '57,76,79,76,86,99,70,81', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (121, 121, 1, 16, 15, 1, 2, '99,73,80,100,92,92,82,82,76,88,78,90,76,82,97', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (122, 122, 1, 10, 9, 1, 0, '72,63,62,91,65,86,59,93,63', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (123, 123, 1, 13, 11, 2, 0, '67,76,77,72,84,59,60,63,69,53,60', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (124, 124, 1, 11, 10, 1, 0, '49,50,66,66,63,71,57,70,56,55', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (125, 125, 1, 11, 8, 3, 3, '68,68,64,70,60,70,73,75', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (126, 126, 1, 13, 10, 3, 2, '59,71,71,65,62,58,53,45,61,72', 62, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (127, 127, 1, 12, 11, 1, 0, '81,74,68,86,84,90,100,77,94,88,77', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (128, 128, 1, 13, 11, 2, 1, '84,75,78,79,98,70,70,79,65,93,80', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (129, 129, 1, 12, 10, 2, 1, '69,81,90,85,59,68,58,59,53,69', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (130, 130, 1, 8, 7, 1, 1, '98,99,77,64,63,90,52', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (131, 131, 1, 15, 15, 0, 2, '76,83,75,76,60,89,72,75,63,80,81,50,78,88,71', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (132, 132, 1, 8, 8, 0, 0, '91,88,80,86,100,87,86,77', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (133, 133, 1, 9, 9, 0, 3, '69,66,84,63,77,88,82,71,95', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (134, 134, 1, 10, 9, 1, 1, '56,58,45,40,58,37,44,57,50', 49, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (135, 135, 1, 14, 12, 2, 2, '77,62,70,66,84,86,80,73,85,96,79,81', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (136, 136, 1, 15, 15, 0, 2, '63,46,60,70,65,60,68,79,57,65,71,60,77,62,75', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (137, 137, 1, 15, 13, 2, 0, '77,65,78,63,76,67,66,74,71,81,56,67,49', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (138, 138, 1, 11, 11, 0, 3, '86,80,55,92,56,77,64,63,78,73,83', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (139, 139, 1, 11, 8, 3, 0, '78,61,94,81,74,57,68,82', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (140, 140, 1, 11, 9, 2, 3, '94,67,70,60,49,71,49,80,72', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (141, 141, 1, 13, 10, 3, 0, '54,64,65,74,85,64,66,78,65,76', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (142, 142, 1, 14, 12, 2, 0, '79,64,74,87,72,74,66,78,79,86,81,87', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (143, 143, 1, 13, 10, 3, 1, '99,81,84,96,83,100,93,100,99,96', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (144, 144, 1, 10, 8, 2, 3, '50,47,74,61,62,46,69,54', 58, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (145, 145, 1, 14, 13, 1, 0, '77,100,100,84,87,96,100,100,81,100,92,88,83', 91, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (146, 146, 1, 10, 8, 2, 1, '78,72,76,86,78,73,87,63', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (147, 147, 1, 13, 13, 0, 1, '100,100,90,100,79,96,95,89,100,95,97,100,100', 95, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (148, 148, 1, 8, 8, 0, 3, '83,65,77,83,82,96,80,72', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (149, 149, 1, 10, 7, 3, 1, '60,79,78,67,77,80,62', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (150, 150, 1, 10, 9, 1, 1, '61,77,70,65,73,70,75,70,62', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (151, 151, 1, 9, 6, 3, 0, '90,73,83,70,65,98', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (152, 152, 1, 16, 15, 1, 3, '76,71,64,76,82,82,82,80,77,87,61,56,100,85,60', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (153, 153, 1, 12, 12, 0, 2, '71,93,83,75,79,100,100,91,70,68,80,83', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (154, 154, 1, 14, 13, 1, 3, '69,67,53,60,62,83,78,84,71,90,90,75,72', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (155, 155, 1, 11, 8, 3, 1, '54,77,60,42,44,60,64,46', 56, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (156, 156, 1, 11, 10, 1, 0, '86,73,77,79,61,70,87,88,58,77', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (157, 157, 1, 10, 8, 2, 0, '43,60,55,61,67,63,70,51', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (158, 158, 1, 12, 11, 1, 3, '100,82,88,80,89,92,81,74,99,77,96', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (159, 159, 1, 13, 10, 3, 0, '79,63,68,80,75,86,77,72,67,87', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (160, 160, 1, 10, 7, 3, 0, '98,77,86,98,86,77,92', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (161, 161, 1, 16, 13, 3, 2, '64,84,95,73,97,59,75,96,81,86,97,79,74', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (162, 162, 1, 14, 14, 0, 2, '90,99,74,69,91,79,82,79,88,99,73,78,94,94', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (163, 163, 1, 13, 12, 1, 1, '71,83,85,80,99,81,95,93,93,97,99,87', 89, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (164, 164, 1, 9, 6, 3, 3, '42,50,61,69,72,59', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (165, 165, 1, 15, 14, 1, 3, '52,55,67,66,51,66,64,57,78,63,82,69,82,67', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (166, 166, 1, 8, 8, 0, 3, '95,75,79,85,83,93,49,87', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (167, 167, 1, 12, 12, 0, 1, '48,75,85,52,66,60,67,56,76,63,53,74', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (168, 168, 1, 8, 7, 1, 3, '81,73,58,83,89,87,98', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (169, 169, 1, 15, 12, 3, 2, '68,54,73,79,63,78,74,52,66,62,47,56', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (170, 170, 1, 15, 12, 3, 1, '54,56,73,63,60,60,72,48,60,63,52,71', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (171, 171, 1, 13, 11, 2, 3, '63,54,73,64,73,30,86,58,79,60,78', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (172, 172, 1, 11, 8, 3, 0, '64,67,45,68,71,76,71,63', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (173, 173, 1, 8, 8, 0, 1, '85,63,70,83,83,70,85,80', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (174, 174, 1, 9, 6, 3, 1, '76,66,56,80,72,77', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (175, 175, 1, 12, 12, 0, 2, '88,97,88,88,90,100,82,100,91,100,99,98', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (176, 176, 1, 11, 11, 0, 3, '57,73,53,69,89,56,73,89,86,95,73', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (177, 177, 1, 14, 13, 1, 3, '61,65,60,34,48,56,52,72,70,64,52,61,52', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (178, 178, 1, 12, 9, 3, 3, '79,69,79,95,86,92,66,61,76', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (179, 179, 1, 12, 9, 3, 3, '74,72,91,91,94,78,66,69,81', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (180, 180, 1, 11, 8, 3, 1, '84,83,100,100,100,88,97,91', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (181, 181, 1, 11, 10, 1, 1, '56,49,71,67,42,69,60,66,54,83', 62, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (182, 182, 1, 13, 13, 0, 0, '98,94,82,87,72,100,100,96,92,89,86,77,92', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (183, 183, 1, 11, 9, 2, 3, '55,59,70,65,54,70,55,72,57', 62, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (184, 184, 1, 11, 8, 3, 1, '87,80,87,83,63,77,68,100', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (185, 185, 1, 16, 13, 3, 2, '60,63,91,81,73,91,82,90,61,70,88,92,66', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (186, 186, 1, 16, 16, 0, 1, '83,88,78,87,72,100,82,76,87,76,84,72,85,62,86,76', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (187, 187, 1, 15, 15, 0, 2, '62,62,81,82,90,81,73,80,89,85,93,79,67,76,74', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (188, 188, 1, 11, 9, 2, 1, '74,88,67,84,69,79,67,74,76', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (189, 189, 1, 12, 9, 3, 1, '71,53,60,59,57,64,41,59,72', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (190, 190, 1, 13, 12, 1, 1, '74,73,74,74,85,75,66,80,81,87,76,84', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (191, 191, 1, 9, 9, 0, 0, '84,77,100,76,56,70,100,68,82', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (192, 192, 1, 12, 11, 1, 3, '81,88,57,74,65,77,65,75,73,60,61', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (193, 193, 1, 8, 5, 3, 3, '99,80,93,85,96', 91, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (194, 194, 1, 15, 14, 1, 2, '82,83,87,73,58,83,71,94,96,73,84,92,63,98', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (195, 195, 1, 14, 14, 0, 0, '70,87,63,85,64,64,74,74,80,79,57,60,67,73', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (196, 196, 1, 15, 12, 3, 2, '59,67,76,63,63,76,86,83,96,68,92,67', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (197, 197, 1, 9, 8, 1, 1, '72,95,84,66,81,82,68,71', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (198, 198, 1, 12, 11, 1, 1, '62,61,81,53,58,58,52,66,72,59,83', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (199, 199, 1, 12, 12, 0, 3, '86,99,82,83,72,89,85,81,74,80,86,92', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (200, 200, 1, 12, 9, 3, 0, '67,64,70,93,77,76,73,60,76', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (201, 201, 1, 12, 12, 0, 2, '52,78,82,59,57,85,68,66,80,58,79,75', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (202, 202, 1, 14, 13, 1, 0, '68,64,61,61,88,68,70,65,58,63,67,55,69', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (203, 203, 1, 13, 12, 1, 1, '95,85,85,93,100,84,87,100,78,82,73,95', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (204, 204, 1, 8, 8, 0, 1, '73,81,86,65,100,83,70,76', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (205, 205, 1, 9, 9, 0, 0, '83,83,61,86,66,70,78,67,72', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (206, 206, 1, 9, 6, 3, 1, '69,83,83,81,58,75', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (207, 207, 1, 15, 12, 3, 2, '77,81,70,83,99,100,100,81,91,100,84,93', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (208, 208, 1, 15, 15, 0, 0, '84,80,91,96,88,77,73,74,94,74,77,89,86,89,100', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (209, 209, 1, 16, 13, 3, 1, '61,85,72,80,59,62,73,58,67,52,67,68,64', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (210, 210, 1, 10, 7, 3, 0, '67,74,54,74,68,45,84', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (211, 211, 1, 11, 8, 3, 3, '61,66,71,73,84,77,77,92', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (212, 212, 1, 16, 15, 1, 3, '75,73,67,66,80,62,81,63,78,78,70,70,58,83,63', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (213, 213, 1, 15, 13, 2, 0, '59,72,70,61,74,88,85,77,70,72,85,85,90', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (214, 214, 1, 8, 6, 2, 3, '79,94,77,87,86,92', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (215, 215, 1, 8, 8, 0, 2, '100,100,77,84,87,97,90,84', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (216, 216, 1, 11, 8, 3, 1, '89,100,95,87,66,89,90,84', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (217, 217, 1, 9, 9, 0, 3, '87,74,95,94,70,89,93,80,87', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (218, 218, 1, 12, 11, 1, 2, '84,86,83,74,74,78,71,78,80,74,70', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (219, 219, 1, 11, 11, 0, 0, '64,70,73,55,77,82,78,75,68,83,80', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (220, 220, 1, 12, 9, 3, 1, '72,60,87,85,81,61,63,95,70', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (221, 221, 1, 14, 14, 0, 0, '56,69,92,67,76,79,75,77,65,81,70,50,78,79', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (222, 222, 1, 16, 16, 0, 0, '80,82,74,76,65,100,83,83,90,77,86,87,81,91,70,94', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (223, 223, 1, 14, 13, 1, 2, '100,93,99,93,100,91,100,100,100,100,94,80,85', 95, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (224, 224, 1, 8, 5, 3, 3, '40,67,74,54,57', 58, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (225, 225, 1, 8, 6, 2, 0, '72,71,80,89,66,65', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (226, 226, 1, 9, 9, 0, 0, '83,86,64,65,96,91,67,73,93', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (227, 227, 1, 11, 10, 1, 3, '79,76,65,80,94,99,86,63,90,64', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (228, 228, 1, 11, 8, 3, 3, '90,76,91,77,73,91,97,93', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (229, 229, 1, 12, 12, 0, 2, '56,82,58,68,95,73,64,59,66,81,75,65', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (230, 230, 1, 8, 7, 1, 0, '60,68,68,73,72,63,57', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (231, 231, 1, 16, 13, 3, 1, '90,65,86,71,66,69,72,59,62,96,78,59,72', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (232, 232, 1, 11, 11, 0, 2, '87,100,88,96,93,100,90,86,90,98,99', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (233, 233, 1, 14, 12, 2, 2, '85,66,91,81,71,74,73,80,70,71,67,70', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (234, 234, 1, 13, 11, 2, 1, '55,83,56,67,59,74,63,55,75,56,65', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (235, 235, 1, 13, 12, 1, 2, '68,56,60,62,52,81,60,74,78,54,70,72', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (236, 236, 1, 11, 10, 1, 2, '66,89,81,77,81,66,62,84,70,75', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (237, 237, 1, 9, 9, 0, 1, '89,100,83,78,87,79,84,70,100', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (238, 238, 1, 11, 8, 3, 1, '85,77,79,82,96,89,91,100', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (239, 239, 1, 10, 8, 2, 0, '73,78,79,65,66,74,77,61', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (240, 240, 1, 13, 12, 1, 2, '91,87,87,79,88,84,67,68,80,62,92,93', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (241, 241, 1, 12, 10, 2, 2, '89,95,90,73,100,83,100,100,98,88', 92, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (242, 242, 1, 12, 11, 1, 0, '79,67,63,73,68,71,62,68,69,57,83', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (243, 243, 1, 14, 14, 0, 0, '82,78,63,94,93,78,71,75,65,92,93,77,79,67', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (244, 244, 1, 13, 11, 2, 2, '83,79,85,84,83,85,67,78,83,88,66', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (245, 245, 1, 16, 13, 3, 3, '77,76,87,77,78,52,77,78,75,62,61,44,70', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (246, 246, 1, 16, 14, 2, 0, '56,68,56,46,77,65,75,74,57,80,77,46,63,87', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (247, 247, 1, 14, 11, 3, 0, '62,53,41,60,69,62,42,40,42,48,86', 55, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (248, 248, 1, 13, 13, 0, 0, '69,83,56,68,78,54,67,57,65,72,55,63,40', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (249, 249, 1, 15, 13, 2, 3, '52,57,83,62,78,59,62,72,43,69,53,47,62', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (250, 250, 1, 15, 14, 1, 0, '70,69,73,64,63,83,49,69,46,69,58,56,70,60', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (251, 251, 1, 11, 10, 1, 0, '58,85,91,100,80,79,79,73,87,82', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (252, 252, 1, 13, 12, 1, 0, '97,100,84,89,100,100,95,93,98,100,100,96', 96, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (253, 253, 1, 8, 6, 2, 1, '94,86,83,87,75,93', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (254, 254, 1, 13, 12, 1, 3, '83,77,89,93,82,97,94,90,66,84,86,63', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (255, 255, 1, 12, 11, 1, 3, '58,64,56,46,61,73,79,62,92,75,69', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (256, 256, 1, 8, 7, 1, 3, '85,82,73,64,52,62,76', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (257, 257, 1, 8, 8, 0, 3, '95,81,93,75,81,82,73,76', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (258, 258, 1, 9, 7, 2, 3, '70,54,82,80,69,78,79', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (259, 259, 1, 16, 15, 1, 3, '80,69,75,83,76,79,76,84,77,70,75,66,100,74,95', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (260, 260, 1, 8, 8, 0, 2, '73,68,88,81,100,77,90,85', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (261, 261, 1, 15, 15, 0, 0, '89,85,95,78,66,70,90,92,82,87,75,78,100,84,84', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (262, 262, 1, 13, 13, 0, 3, '71,72,77,87,82,70,68,83,85,58,68,68,66', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (263, 263, 1, 16, 15, 1, 0, '87,79,80,77,69,74,64,75,69,88,74,91,83,99,95', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (264, 264, 1, 9, 6, 3, 2, '74,69,82,64,85,70', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (265, 265, 1, 11, 9, 2, 0, '87,76,71,80,40,65,64,50,52', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (266, 266, 1, 10, 9, 1, 1, '66,87,65,66,74,72,75,87,90', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (267, 267, 1, 14, 14, 0, 3, '65,72,65,60,86,60,64,80,84,78,64,83,73,57', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (268, 268, 2, 8, 7, 1, 3, '76,85,64,71,70,78,71', 74, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (269, 269, 2, 16, 15, 1, 1, '89,76,70,66,84,85,84,88,79,73,67,70,97,54,70', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (270, 270, 2, 10, 10, 0, 0, '71,77,73,94,85,73,100,77,94,76', 82, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (271, 271, 2, 9, 9, 0, 1, '77,58,70,73,61,82,85,75,80', 73, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (272, 272, 2, 8, 5, 3, 1, '75,64,75,71,99', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (273, 273, 2, 12, 11, 1, 2, '81,73,81,77,65,61,92,89,84,70,77', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (274, 274, 2, 10, 10, 0, 2, '76,79,89,54,85,95,66,62,64,77', 75, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (275, 275, 2, 16, 14, 2, 3, '54,67,80,65,62,90,60,87,66,80,72,82,69,63', 71, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (276, 276, 2, 8, 7, 1, 2, '48,79,52,55,70,76,74', 65, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (277, 277, 2, 9, 7, 2, 1, '64,70,49,74,81,63,78', 68, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (278, 278, 2, 14, 11, 3, 1, '86,84,87,86,71,78,85,100,72,83,77', 83, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (279, 279, 2, 15, 15, 0, 0, '95,90,82,95,81,83,100,81,90,100,90,82,74,100,66', 87, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (280, 280, 2, 11, 11, 0, 3, '63,61,77,75,50,61,84,75,53,76,64', 67, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (281, 281, 2, 14, 14, 0, 1, '71,82,64,64,64,59,60,61,52,70,81,70,62,70', 66, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (282, 282, 2, 10, 7, 3, 1, '81,70,61,75,77,100,56', 74, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (283, 283, 2, 9, 8, 1, 1, '68,66,72,67,56,59,65,58', 64, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (284, 284, 2, 13, 12, 1, 2, '97,75,73,61,91,74,87,83,85,77,75,53', 78, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (285, 285, 2, 8, 8, 0, 1, '78,73,74,73,90,83,80,85', 80, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (286, 286, 2, 12, 12, 0, 1, '86,73,62,68,87,76,57,85,81,91,74,54', 75, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (287, 287, 2, 9, 6, 3, 2, '78,67,73,91,78,79', 78, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (288, 288, 2, 9, 7, 2, 2, '91,76,94,100,89,81,96', 90, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (289, 289, 2, 9, 7, 2, 0, '100,95,92,87,100,86,90', 93, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (290, 290, 2, 8, 6, 2, 0, '75,72,68,86,81,72', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (291, 291, 2, 11, 8, 3, 0, '72,86,61,69,72,73,70,72', 72, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (292, 292, 2, 14, 13, 1, 0, '80,65,66,88,63,90,68,77,83,60,78,85,83', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (293, 293, 2, 8, 8, 0, 3, '72,69,63,82,60,79,76,62', 70, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (294, 294, 2, 9, 8, 1, 0, '62,66,71,76,78,53,51,80', 67, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (295, 295, 2, 10, 8, 2, 2, '75,91,65,81,83,91,69,52', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (296, 296, 2, 16, 16, 0, 1, '53,49,67,55,55,59,61,59,66,59,69,55,69,63,48,57', 59, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (297, 297, 2, 9, 8, 1, 2, '74,69,70,83,80,68,88,66', 75, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (298, 298, 2, 9, 7, 2, 3, '85,76,74,71,84,69,69', 75, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (299, 299, 2, 9, 8, 1, 0, '85,93,100,80,73,74,100,91', 87, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (300, 300, 2, 10, 8, 2, 3, '83,69,79,85,96,91,91,78', 84, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (301, 301, 2, 9, 9, 0, 2, '70,66,72,82,70,46,66,41,66', 64, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (302, 302, 2, 9, 8, 1, 1, '50,55,58,59,79,55,67,66', 61, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (303, 303, 2, 9, 9, 0, 1, '74,86,71,68,75,62,76,90,81', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (304, 304, 2, 9, 7, 2, 2, '72,62,61,65,70,68,65', 66, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (305, 305, 2, 11, 11, 0, 1, '67,63,55,66,52,64,60,57,66,76,73', 64, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (306, 306, 2, 15, 12, 3, 2, '88,81,90,74,96,95,80,80,68,78,73,70', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (307, 307, 2, 16, 15, 1, 1, '54,71,100,55,45,50,61,66,57,80,72,79,59,65,73', 66, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (308, 308, 2, 8, 6, 2, 3, '71,93,90,100,88,68', 85, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (309, 309, 2, 8, 5, 3, 3, '57,79,74,68,72', 70, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (310, 310, 2, 16, 15, 1, 1, '83,81,74,79,88,78,92,82,75,77,100,92,74,87,97', 84, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (311, 311, 2, 13, 10, 3, 0, '58,67,72,68,68,75,89,39,83,65', 68, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (312, 312, 2, 8, 8, 0, 1, '67,81,95,78,98,85,96,81', 85, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (313, 313, 2, 14, 12, 2, 1, '82,83,77,83,89,94,73,69,86,71,54,80', 78, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (314, 314, 2, 8, 5, 3, 2, '62,63,60,57,58', 60, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (315, 315, 2, 15, 12, 3, 2, '78,68,81,64,69,73,72,93,73,69,80,72', 74, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (316, 316, 2, 12, 9, 3, 2, '65,71,74,72,63,95,64,58,74', 71, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (317, 317, 2, 16, 14, 2, 1, '40,62,59,54,58,37,53,59,45,49,62,67,61,63', 55, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (318, 318, 2, 16, 16, 0, 1, '87,90,92,81,78,81,75,78,89,88,74,87,89,73,78,91', 83, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (319, 319, 2, 14, 13, 1, 1, '60,73,72,67,70,82,86,76,71,74,59,74,84', 73, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (320, 320, 2, 11, 8, 3, 3, '82,86,86,91,90,80,82,77', 84, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (321, 321, 2, 13, 12, 1, 2, '89,100,95,83,61,76,77,79,82,59,89,80', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (322, 322, 2, 11, 11, 0, 0, '67,81,64,90,79,69,82,77,74,65,100', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (323, 323, 2, 13, 10, 3, 3, '64,77,68,78,78,75,79,90,66,72', 75, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (324, 324, 2, 9, 7, 2, 0, '64,59,73,57,79,73,77', 69, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (325, 325, 2, 15, 15, 0, 0, '65,59,70,79,83,82,83,75,63,69,67,50,66,73,53', 69, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (326, 326, 2, 9, 7, 2, 2, '63,73,73,77,68,63,90', 72, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (327, 327, 2, 9, 8, 1, 0, '88,93,86,75,90,78,71,77', 82, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (328, 328, 2, 10, 8, 2, 1, '54,65,41,78,47,84,59,67', 62, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (329, 329, 2, 8, 7, 1, 0, '66,57,83,68,68,57,69', 67, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (330, 330, 2, 10, 7, 3, 1, '99,100,74,74,80,100,86', 88, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (331, 331, 2, 11, 9, 2, 1, '92,87,70,76,86,71,87,82,86', 82, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (332, 332, 2, 9, 9, 0, 0, '80,69,69,75,73,73,73,81,61', 73, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (333, 333, 2, 8, 6, 2, 2, '64,76,68,92,82,74', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (334, 334, 2, 8, 8, 0, 2, '80,84,67,65,61,61,54,54', 66, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (335, 335, 2, 9, 9, 0, 2, '54,66,62,62,81,65,57,75,65', 65, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (336, 336, 2, 16, 14, 2, 2, '78,82,69,57,71,93,78,75,78,70,84,87,64,82', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (337, 337, 2, 8, 5, 3, 2, '71,80,68,70,71', 72, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (338, 338, 2, 9, 8, 1, 2, '68,65,52,54,68,81,66,71', 66, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (339, 339, 2, 8, 8, 0, 1, '75,99,86,65,64,62,60,79', 74, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (340, 340, 2, 9, 6, 3, 0, '86,65,64,67,59,94', 73, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (341, 341, 2, 11, 10, 1, 1, '83,93,63,78,74,85,83,90,78,82', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (342, 342, 2, 12, 12, 0, 3, '63,75,71,75,82,82,64,73,69,62,86,80', 74, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (343, 343, 2, 8, 8, 0, 2, '77,60,82,100,86,97,96,96', 87, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (344, 344, 2, 15, 12, 3, 0, '84,79,89,95,86,100,75,92,94,79,100,97', 89, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (345, 345, 2, 16, 13, 3, 3, '76,71,74,68,52,78,71,69,71,69,70,53,83', 70, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (346, 346, 2, 15, 12, 3, 2, '54,73,84,66,83,54,50,65,60,65,49,72', 65, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (347, 347, 2, 10, 8, 2, 1, '79,100,99,84,67,90,90,99', 89, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (348, 348, 2, 13, 13, 0, 1, '68,85,84,74,94,69,99,57,78,92,90,82,80', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (349, 349, 2, 11, 11, 0, 0, '100,74,92,83,100,100,82,89,100,100,98', 93, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (350, 350, 2, 14, 12, 2, 3, '66,82,61,73,61,71,67,77,75,62,76,89', 72, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (351, 351, 2, 15, 13, 2, 1, '75,85,94,76,71,73,86,85,82,93,88,75,92', 83, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (352, 352, 2, 16, 16, 0, 0, '98,86,100,78,100,81,100,97,100,93,91,93,100,94,73,91', 92, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (353, 353, 2, 11, 11, 0, 1, '72,81,82,86,82,66,73,80,77,87,83', 79, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (354, 354, 2, 12, 9, 3, 3, '83,80,92,71,87,85,96,84,80', 84, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (355, 355, 2, 12, 10, 2, 1, '67,91,81,79,93,84,87,62,82,70', 80, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (356, 356, 2, 11, 10, 1, 3, '49,75,47,48,45,59,60,51,66,65', 57, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (357, 357, 2, 16, 13, 3, 3, '68,67,75,70,76,55,72,71,74,71,72,70,72', 70, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (358, 358, 2, 12, 9, 3, 2, '73,92,89,85,71,58,76,65,82', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (359, 359, 2, 11, 9, 2, 3, '80,91,68,69,71,79,97,82,58', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (360, 360, 2, 16, 15, 1, 3, '77,57,73,73,77,73,90,64,74,73,80,77,64,68,60', 72, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (361, 361, 2, 15, 12, 3, 3, '86,79,64,55,73,73,78,88,65,72,70,74', 73, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (362, 362, 2, 15, 15, 0, 3, '100,100,90,61,88,85,89,100,94,77,97,91,91,86,90', 89, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (363, 363, 2, 13, 13, 0, 1, '72,98,82,77,98,90,90,95,91,93,95,90,71', 88, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (364, 364, 2, 12, 11, 1, 0, '50,59,91,44,62,63,91,63,78,67,63', 66, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (365, 365, 2, 14, 14, 0, 1, '81,91,100,89,81,87,100,80,91,93,86,73,91,93', 88, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (366, 366, 2, 13, 10, 3, 0, '76,84,72,67,73,62,75,69,60,76', 71, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (367, 367, 2, 15, 15, 0, 1, '73,77,83,75,80,69,77,94,99,95,72,80,87,78,78', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (368, 368, 2, 9, 8, 1, 2, '53,61,63,72,65,53,74,71', 64, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (369, 369, 2, 9, 8, 1, 0, '62,67,67,77,65,74,62,76', 69, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (370, 370, 2, 13, 12, 1, 3, '64,52,51,63,49,78,49,78,68,68,45,69', 61, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (371, 371, 2, 12, 12, 0, 3, '100,70,87,80,78,87,68,77,78,74,89,92', 82, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (372, 372, 2, 15, 13, 2, 3, '69,79,88,92,87,81,88,78,78,61,85,85,81', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (373, 373, 2, 16, 14, 2, 1, '82,59,96,69,64,84,69,64,76,79,68,100,78,77', 76, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (374, 374, 2, 9, 9, 0, 1, '64,75,75,51,43,45,72,51,51', 59, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (375, 375, 2, 13, 11, 2, 1, '85,78,88,79,81,89,76,82,68,72,70', 79, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (376, 376, 2, 12, 11, 1, 3, '55,70,63,78,58,81,69,91,73,73,73', 71, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (377, 377, 2, 11, 9, 2, 3, '93,70,73,80,92,72,69,74,67', 77, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (378, 378, 2, 13, 11, 2, 0, '81,70,70,81,87,89,84,78,65,90,79', 79, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (379, 379, 2, 10, 8, 2, 1, '53,48,74,58,59,73,74,76', 64, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (380, 380, 2, 16, 15, 1, 3, '81,84,83,93,71,76,92,79,86,69,75,72,86,75,88', 81, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (381, 381, 2, 12, 10, 2, 3, '79,71,84,76,74,85,77,81,74,77', 78, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (382, 382, 2, 16, 15, 1, 0, '93,90,77,82,82,79,100,82,98,66,66,100,77,84,76', 83, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (383, 383, 2, 16, 13, 3, 3, '90,93,87,87,87,83,95,93,69,95,78,85,75', 86, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (384, 384, 2, 11, 8, 3, 2, '82,81,65,83,81,71,83,91', 80, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (385, 385, 2, 10, 7, 3, 2, '70,84,63,73,79,75,61', 72, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (386, 386, 2, 15, 13, 2, 0, '85,75,71,93,70,90,93,81,81,100,75,93,62', 82, '2026-05-16 17:01:19', '2026-05-21 08:12:10');
INSERT INTO `homework_info` VALUES (387, 387, 1, 11, 9, 2, 1, '77,74,99,80,83,84,79,79,79', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (388, 388, 1, 11, 8, 3, 1, '65,76,87,75,94,80,63,70', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (389, 389, 1, 8, 6, 2, 1, '69,90,72,66,58,81', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (390, 390, 1, 12, 10, 2, 3, '100,94,91,89,88,74,96,92,81,97', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (391, 391, 1, 14, 14, 0, 2, '74,84,74,55,72,88,68,72,88,74,72,66,79,73', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (392, 392, 1, 12, 11, 1, 0, '59,81,74,63,80,71,81,84,50,61,70', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (393, 393, 1, 15, 12, 3, 1, '50,49,72,82,60,53,48,65,60,52,59,74', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (394, 394, 1, 8, 5, 3, 1, '74,62,57,65,85', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (395, 395, 1, 13, 13, 0, 3, '75,75,78,69,76,70,88,62,87,64,48,78,71', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (396, 396, 1, 9, 9, 0, 3, '76,55,74,66,67,78,80,80,88', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (397, 397, 1, 16, 15, 1, 1, '53,60,51,54,62,56,64,64,68,65,55,42,50,68,73', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (398, 398, 1, 8, 7, 1, 3, '58,50,60,61,45,57,67', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (399, 399, 1, 10, 10, 0, 1, '76,87,66,82,81,82,74,76,71,69', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (400, 400, 1, 15, 12, 3, 3, '74,71,76,78,78,69,80,65,82,71,68,67', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (401, 401, 1, 15, 14, 1, 3, '78,86,77,85,65,78,78,63,85,73,72,75,66,69', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (402, 402, 1, 10, 7, 3, 0, '80,82,80,83,77,65,64', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (403, 403, 1, 9, 8, 1, 3, '65,74,70,58,71,55,52,72', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (404, 404, 1, 11, 10, 1, 2, '77,60,71,68,73,81,53,74,73,65', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (405, 405, 1, 11, 8, 3, 0, '83,43,86,68,82,72,77,63', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (406, 406, 1, 9, 8, 1, 2, '80,79,79,77,83,74,72,46', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (407, 407, 1, 10, 9, 1, 1, '54,69,76,69,49,67,65,40,51', 60, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (408, 408, 1, 12, 10, 2, 3, '71,72,75,75,70,66,70,74,91,78', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (409, 409, 1, 9, 7, 2, 3, '62,80,99,68,83,72,74', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (410, 410, 1, 16, 14, 2, 1, '86,77,92,100,69,79,74,78,64,80,86,65,92,54', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (411, 411, 1, 11, 11, 0, 3, '85,76,61,73,100,76,62,62,82,92,87', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (412, 412, 1, 15, 13, 2, 2, '54,32,54,55,41,52,64,56,43,38,43,37,30', 46, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (413, 413, 1, 11, 9, 2, 3, '73,80,100,86,83,80,92,100,70', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (414, 414, 1, 11, 10, 1, 1, '71,63,77,68,67,48,71,60,57,56', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (415, 415, 1, 14, 12, 2, 0, '75,96,81,53,64,84,79,62,72,65,83,92', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (416, 416, 1, 9, 6, 3, 3, '68,77,64,53,81,67', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (417, 417, 1, 15, 15, 0, 0, '79,72,86,68,76,90,81,80,53,80,85,59,65,61,75', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (418, 418, 1, 11, 8, 3, 0, '61,71,96,86,100,79,79,68', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (419, 419, 1, 16, 15, 1, 0, '96,76,66,94,95,81,100,90,87,98,89,90,85,79,78', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (420, 420, 1, 12, 12, 0, 2, '83,84,89,80,86,79,84,84,83,82,81,90', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (421, 421, 1, 11, 10, 1, 3, '53,59,25,44,61,40,59,69,64,60', 53, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (422, 422, 1, 10, 8, 2, 0, '47,57,49,51,61,57,53,60', 54, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (423, 423, 1, 12, 9, 3, 0, '80,49,76,66,61,76,73,64,63', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (424, 424, 1, 12, 11, 1, 1, '75,79,65,64,83,77,80,68,84,90,61', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (425, 425, 1, 13, 12, 1, 1, '72,69,94,68,53,83,63,75,66,82,96,72', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (426, 426, 1, 15, 12, 3, 0, '66,82,72,89,87,74,69,100,82,97,92,100', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (427, 427, 1, 12, 12, 0, 3, '74,94,87,90,76,76,78,78,87,81,63,74', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (428, 428, 1, 8, 6, 2, 0, '93,73,100,70,75,61', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (429, 429, 1, 14, 13, 1, 1, '94,93,93,89,99,83,97,85,90,77,93,99,83', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (430, 430, 1, 13, 11, 2, 3, '56,83,83,77,75,93,93,79,94,94,92', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (431, 431, 1, 12, 11, 1, 1, '64,62,83,64,86,64,66,75,69,75,71', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (432, 432, 1, 13, 12, 1, 1, '48,70,73,55,71,74,61,50,68,66,72,53', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (433, 433, 1, 13, 11, 2, 0, '80,100,89,92,77,92,100,100,100,80,95', 91, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (434, 434, 1, 12, 12, 0, 2, '73,81,77,93,81,96,64,70,88,92,92,63', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (435, 435, 1, 12, 9, 3, 3, '86,89,84,72,84,91,90,87,53', 82, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (436, 436, 1, 12, 10, 2, 0, '63,71,62,80,87,74,72,71,64,72', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (437, 437, 1, 14, 13, 1, 2, '79,91,79,76,63,81,74,72,86,91,90,85,88', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (438, 438, 1, 10, 10, 0, 3, '65,63,63,43,58,49,73,61,46,66', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (439, 439, 1, 10, 7, 3, 3, '69,82,73,72,71,93,100', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (440, 440, 1, 15, 13, 2, 1, '85,76,90,98,88,82,92,80,93,89,99,88,92', 89, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (441, 441, 1, 9, 6, 3, 2, '80,87,72,74,73,76', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (442, 442, 1, 12, 9, 3, 2, '75,71,74,80,73,69,60,88,74', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (443, 443, 1, 9, 8, 1, 2, '79,85,68,66,71,74,73,77', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (444, 444, 1, 8, 7, 1, 0, '70,72,88,67,77,77,83', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (445, 445, 1, 13, 10, 3, 2, '76,62,63,81,85,71,82,69,85,53', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (446, 446, 1, 14, 12, 2, 0, '78,70,81,68,60,75,76,73,76,62,89,96', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (447, 447, 1, 13, 12, 1, 3, '62,67,63,60,49,44,57,65,41,74,49,63', 58, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (448, 448, 1, 13, 11, 2, 2, '100,89,90,93,84,95,79,93,89,94,81', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (449, 449, 1, 13, 13, 0, 0, '78,80,66,73,90,81,85,72,64,84,78,65,64', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (450, 450, 1, 11, 11, 0, 0, '83,90,86,98,100,100,100,92,100,89,98', 94, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (451, 451, 1, 14, 11, 3, 0, '35,59,66,64,60,79,62,60,57,72,57', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (452, 452, 1, 15, 14, 1, 0, '68,79,72,72,88,69,85,77,92,76,80,86,90,90', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (453, 453, 1, 10, 8, 2, 1, '69,75,88,61,83,76,74,97', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (454, 454, 1, 9, 8, 1, 2, '89,86,64,62,79,70,52,64', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (455, 455, 1, 15, 15, 0, 3, '81,69,79,78,74,74,76,64,55,78,71,67,79,72,81', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (456, 456, 1, 12, 10, 2, 1, '74,70,67,61,90,75,79,86,55,73', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (457, 457, 1, 9, 8, 1, 1, '86,83,68,43,85,75,91,63', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (458, 458, 1, 14, 11, 3, 2, '79,76,55,58,64,86,64,72,96,63,78', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (459, 459, 1, 11, 10, 1, 3, '70,69,59,53,65,85,85,63,59,62', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (460, 460, 1, 10, 10, 0, 2, '65,82,66,70,73,60,78,53,58,93', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (461, 461, 1, 16, 16, 0, 1, '85,84,84,63,73,71,67,65,80,71,89,65,79,71,82,83', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (462, 462, 1, 13, 11, 2, 0, '60,72,70,81,74,62,88,71,51,77,73', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (463, 463, 1, 10, 7, 3, 3, '100,94,100,94,94,95,85', 95, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (464, 464, 1, 10, 10, 0, 2, '72,64,72,66,83,80,78,89,84,67', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (465, 465, 1, 11, 10, 1, 1, '92,79,77,77,91,95,77,71,73,66', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (466, 466, 1, 15, 14, 1, 1, '65,52,61,62,82,75,72,75,84,88,81,70,74,76', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (467, 467, 1, 14, 11, 3, 2, '81,84,87,91,73,67,67,88,93,85,72', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (468, 468, 1, 13, 11, 2, 0, '74,90,78,89,68,83,100,68,91,55,89', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (469, 469, 1, 14, 12, 2, 1, '73,78,60,80,85,67,78,60,86,64,62,84', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (470, 470, 1, 14, 13, 1, 2, '81,67,72,70,75,58,75,49,69,62,69,78,81', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (471, 471, 1, 13, 13, 0, 2, '72,62,63,62,66,70,73,70,62,71,76,86,72', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (472, 472, 1, 11, 9, 2, 1, '64,65,97,87,59,51,52,62,69', 67, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (473, 473, 1, 15, 13, 2, 2, '68,67,80,59,79,67,63,87,62,65,58,66,81', 69, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (474, 474, 1, 14, 12, 2, 1, '90,68,76,98,88,90,99,79,100,69,90,72', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (475, 475, 1, 9, 6, 3, 2, '100,100,72,75,88,87', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (476, 476, 1, 8, 6, 2, 2, '57,97,88,69,69,77', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (477, 477, 1, 14, 11, 3, 1, '72,69,66,87,100,66,69,71,70,82,76', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (478, 478, 1, 11, 8, 3, 1, '68,74,92,63,61,69,66,86', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (479, 479, 1, 13, 10, 3, 1, '92,88,69,79,91,69,93,79,73,64', 80, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (480, 480, 1, 9, 8, 1, 0, '68,89,69,87,95,92,89,80', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (481, 481, 1, 16, 14, 2, 2, '54,57,57,80,79,71,75,65,64,79,51,68,74,72', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (482, 482, 1, 13, 12, 1, 1, '91,63,80,89,100,82,84,84,80,79,86,94', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (483, 483, 1, 10, 8, 2, 0, '74,62,62,50,70,74,66,73', 66, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (484, 484, 1, 16, 15, 1, 1, '87,66,73,72,76,78,64,63,81,77,76,72,70,87,78', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (485, 485, 1, 15, 13, 2, 1, '76,60,68,83,82,76,69,69,75,82,88,72,83', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (486, 486, 1, 12, 12, 0, 0, '70,61,68,65,61,87,62,76,62,71,77,86', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (487, 487, 1, 9, 6, 3, 2, '80,68,61,86,67,82', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (488, 488, 1, 15, 15, 0, 0, '85,85,81,82,78,72,82,92,89,73,76,74,83,86,74', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (489, 489, 1, 11, 9, 2, 2, '66,64,50,66,66,72,69,73,63', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (490, 490, 1, 13, 13, 0, 2, '87,87,96,90,72,88,95,100,73,84,89,81,83', 87, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (491, 491, 1, 11, 10, 1, 2, '96,98,100,90,95,98,80,92,95,87', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (492, 492, 1, 13, 12, 1, 2, '76,91,71,80,79,75,64,74,85,92,77,86', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (493, 493, 1, 16, 16, 0, 1, '81,77,93,71,90,68,69,60,77,72,75,77,78,59,81,75', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (494, 494, 1, 16, 14, 2, 2, '81,90,98,91,90,88,100,81,85,95,72,70,79,88', 86, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (495, 495, 1, 14, 12, 2, 3, '84,65,76,68,74,69,84,65,71,69,66,81', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (496, 496, 1, 8, 6, 2, 3, '100,100,84,85,100,91', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (497, 497, 1, 14, 14, 0, 2, '72,95,100,78,76,82,91,83,85,64,86,91,97,73', 84, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (498, 498, 1, 15, 13, 2, 1, '78,71,58,71,75,56,73,63,67,70,64,82,82', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (499, 499, 1, 14, 11, 3, 1, '63,77,71,70,90,78,94,81,82,88,58', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (500, 500, 1, 9, 6, 3, 1, '64,77,64,68,82,85', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (501, 501, 1, 14, 13, 1, 2, '75,52,61,64,54,66,42,45,77,61,62,63,67', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (502, 502, 1, 10, 9, 1, 3, '77,85,82,70,79,100,92,76,83', 83, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (503, 503, 1, 12, 10, 2, 3, '51,56,64,54,56,50,57,53,78,52', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (504, 504, 1, 14, 12, 2, 1, '72,83,85,79,67,72,77,78,66,79,58,62', 73, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (505, 505, 1, 11, 8, 3, 2, '83,88,80,78,73,68,90,86', 81, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (506, 506, 1, 14, 14, 0, 3, '75,62,79,66,82,70,68,80,70,71,73,99,80,79', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (507, 507, 1, 10, 8, 2, 1, '91,100,90,87,82,78,94,80', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (508, 508, 1, 9, 8, 1, 2, '73,58,81,87,77,62,62,96', 75, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (509, 509, 1, 8, 6, 2, 1, '77,61,69,89,62,83', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (510, 510, 1, 13, 10, 3, 3, '79,77,73,67,73,79,73,86,72,100', 78, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (511, 511, 1, 16, 16, 0, 3, '71,76,83,84,85,97,91,86,72,83,87,92,80,82,86,99', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (512, 512, 1, 8, 8, 0, 1, '61,54,75,63,58,50,60,51', 59, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (513, 513, 1, 11, 9, 2, 3, '51,45,67,64,70,64,69,74,61', 63, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (514, 514, 1, 8, 6, 2, 3, '93,83,86,100,75,100', 90, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (515, 515, 1, 13, 13, 0, 3, '84,81,93,74,67,85,91,62,63,73,88,70,67', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (516, 516, 1, 10, 8, 2, 2, '59,73,52,62,70,72,67,57', 64, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (517, 517, 1, 8, 7, 1, 2, '87,96,81,89,90,96,79', 88, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (518, 518, 1, 12, 9, 3, 0, '82,95,77,79,79,70,63,77,74', 77, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (519, 519, 1, 14, 13, 1, 3, '82,100,90,87,90,84,81,97,95,90,88,94,82', 89, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (520, 520, 1, 10, 10, 0, 2, '55,58,69,62,56,49,59,47,38,44', 54, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (521, 521, 1, 15, 14, 1, 3, '89,69,74,71,71,79,57,73,83,60,73,69,74,68', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (522, 522, 1, 8, 5, 3, 2, '54,65,64,45,59', 57, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (523, 523, 1, 14, 11, 3, 2, '100,81,77,73,83,100,74,78,85,89,100', 85, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (524, 524, 1, 8, 5, 3, 2, '67,55,59,68,56', 61, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (525, 525, 1, 14, 14, 0, 0, '83,66,71,74,77,62,72,85,74,79,74,92,62,88', 76, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (526, 526, 1, 8, 6, 2, 3, '79,93,47,75,69,83', 74, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (527, 527, 1, 15, 15, 0, 0, '75,88,66,83,86,82,68,71,79,70,100,77,72,69,95', 79, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (528, 528, 1, 14, 12, 2, 2, '81,89,100,88,91,100,93,93,92,98,95,97', 93, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (529, 529, 1, 11, 10, 1, 3, '74,64,87,68,61,44,56,60,65,71', 65, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (530, 530, 1, 12, 10, 2, 2, '89,89,100,89,100,96,76,80,100,93', 91, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (531, 531, 1, 16, 16, 0, 3, '65,82,71,43,86,67,76,70,59,58,100,78,75,80,69,64', 71, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (532, 532, 1, 15, 12, 3, 1, '60,68,81,55,74,66,77,74,79,72,81,77', 72, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (533, 533, 1, 10, 7, 3, 1, '55,71,68,96,60,61,76', 70, '2026-05-16 17:01:19', '2026-05-16 17:01:19');
INSERT INTO `homework_info` VALUES (534, 534, 1, 12, 12, 0, 1, '61,74,71,66,75,64,74,58,67,77,65,60', 68, '2026-05-16 17:01:19', '2026-05-16 17:01:19');

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
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '干预措施记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of intervention_record
-- ----------------------------
INSERT INTO `intervention_record` VALUES (1, 458, 268, 4, 'TALK', '一对一约谈，了解学习困难：学生反映课程二（大学生计算机基础（二））知识点掌握不牢，前序课程一基础薄弱', 'COMPLETED', '学生态度积极，愿意配合辅导', 19.0, NULL, NULL, NULL, '2026-05-21 11:00:00', '2026-05-21 11:00:00');
INSERT INTO `intervention_record` VALUES (2, 458, 268, 4, 'TUTOR', '安排每周三下午课后辅导1小时，重点补习薄弱知识点', 'EXECUTING', '辅导已进行1次，学生出勤正常', 19.0, NULL, NULL, NULL, '2026-05-21 11:30:00', '2026-05-21 11:30:00');
INSERT INTO `intervention_record` VALUES (3, 463, 277, 4, 'PARENT', '电话联系家长，告知挂科风险及作业欠交情况', 'COMPLETED', '家长表示会加强监督，配合学校督促学习', 20.5, NULL, NULL, NULL, '2026-05-21 14:00:00', '2026-05-21 14:00:00');
INSERT INTO `intervention_record` VALUES (4, 463, 277, 4, 'PLAN', '制定为期4周的学习追赶计划：每天额外30分钟编程练习', 'EXECUTING', NULL, 20.5, NULL, NULL, NULL, '2026-05-21 14:30:00', '2026-05-21 14:30:00');
INSERT INTO `intervention_record` VALUES (5, 561, 1, 1, 'PLAN', '着重加强知识点学习', 'EXECUTING', NULL, 26.0, NULL, NULL, NULL, '2026-05-21 16:03:28', '2026-05-21 16:03:28');
INSERT INTO `intervention_record` VALUES (6, 645, 1, 1, 'PLAN', '补加做题', 'EXECUTING', NULL, 26.0, NULL, NULL, NULL, '2026-05-21 16:30:01', '2026-05-21 16:30:01');
INSERT INTO `intervention_record` VALUES (7, 711, 54, 1, 'TALK', '下周一来我办公室', 'COMPLETED', NULL, 42.4, NULL, NULL, NULL, '2026-05-21 18:40:26', '2026-05-21 19:58:16');
INSERT INTO `intervention_record` VALUES (8, 768, 88, 1, 'PLAN', '111', 'COMPLETED', NULL, 29.1, NULL, NULL, NULL, '2026-05-21 18:51:10', '2026-05-21 19:58:16');
INSERT INTO `intervention_record` VALUES (9, 829, 1, 1, 'TUTOR', '11111', 'EXECUTING', NULL, 26.0, NULL, NULL, NULL, '2026-05-26 17:35:19', '2026-05-26 17:35:19');
INSERT INTO `intervention_record` VALUES (10, 830, 2, 1, 'TUTOR', '11111', 'EXECUTING', NULL, 18.2, NULL, NULL, NULL, '2026-05-26 17:37:26', '2026-05-26 17:37:26');
INSERT INTO `intervention_record` VALUES (11, 854, 54, 1, 'TALK', '1111', 'EXECUTING', NULL, 42.4, NULL, NULL, NULL, '2026-05-26 17:42:13', '2026-05-26 17:42:13');
INSERT INTO `intervention_record` VALUES (12, 831, 3, 1, 'PARENT', '434545', 'EXECUTING', NULL, 16.3, NULL, NULL, NULL, '2026-05-26 17:49:54', '2026-05-26 17:49:54');
INSERT INTO `intervention_record` VALUES (13, 832, 5, 1, 'TUTOR', '111', 'EXECUTING', NULL, 23.3, NULL, NULL, NULL, '2026-05-26 18:05:23', '2026-05-26 18:05:23');
INSERT INTO `intervention_record` VALUES (14, 955, 1, 1, 'PLAN', '进行练习', 'COMPLETED', NULL, 26.0, NULL, NULL, NULL, '2026-07-08 16:40:23', '2026-07-08 21:30:25');

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
-- Records of knowledge_mastery
-- ----------------------------
INSERT INTO `knowledge_mastery` VALUES (1, 1, 1, 20, 9, 17, 4, 36, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (2, 2, 1, 14, 3, 22, 5, 24, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (3, 3, 1, 10, 9, 25, 2, 25, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (4, 4, 1, 16, 6, 29, 1, 31, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (5, 5, 1, 18, 9, 23, 0, 28, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (6, 6, 1, 11, 8, 20, 4, 37, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (7, 7, 1, 18, 2, 19, 1, 34, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (8, 8, 1, 20, 8, 25, 3, 34, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (9, 9, 1, 20, 4, 28, 2, 35, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (10, 10, 1, 14, 8, 17, 4, 39, 36, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (11, 11, 1, 10, 0, 25, 4, 39, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (12, 12, 1, 15, 9, 27, 2, 36, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (13, 13, 1, 12, 0, 28, 1, 39, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (14, 14, 1, 15, 1, 26, 5, 29, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (15, 15, 1, 16, 10, 15, 2, 37, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (16, 16, 1, 11, 8, 29, 4, 24, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (17, 17, 1, 10, 5, 28, 2, 37, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (18, 18, 1, 17, 5, 17, 4, 22, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (19, 19, 1, 20, 10, 15, 2, 22, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (20, 20, 1, 17, 3, 19, 3, 24, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (21, 21, 1, 17, 7, 15, 3, 27, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (22, 22, 1, 10, 5, 24, 4, 37, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (23, 23, 1, 17, 9, 29, 3, 37, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (24, 24, 1, 18, 1, 21, 2, 38, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (25, 25, 1, 11, 5, 27, 0, 37, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (26, 26, 1, 10, 2, 20, 0, 36, 36, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (27, 27, 1, 12, 4, 27, 5, 40, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (28, 28, 1, 13, 2, 23, 0, 24, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (29, 29, 1, 15, 7, 17, 2, 32, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (30, 30, 1, 14, 4, 17, 4, 21, 11, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (31, 31, 1, 15, 6, 23, 5, 22, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (32, 32, 1, 17, 1, 22, 2, 26, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (33, 33, 1, 10, 10, 28, 4, 40, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (34, 34, 1, 10, 2, 25, 1, 29, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (35, 35, 1, 12, 4, 19, 0, 21, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (36, 36, 1, 12, 2, 26, 4, 32, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (37, 37, 1, 10, 3, 26, 3, 34, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (38, 38, 1, 16, 5, 20, 1, 40, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (39, 39, 1, 16, 10, 19, 4, 37, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (40, 40, 1, 15, 3, 23, 5, 29, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (41, 41, 1, 13, 1, 17, 3, 36, 36, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (42, 42, 1, 14, 1, 21, 1, 23, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (43, 43, 1, 19, 0, 24, 1, 32, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (44, 44, 1, 19, 1, 15, 3, 27, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (45, 45, 1, 18, 1, 17, 3, 20, 14, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (46, 46, 1, 19, 3, 16, 2, 24, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (47, 47, 1, 11, 8, 21, 3, 21, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (48, 48, 1, 18, 4, 17, 0, 23, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (49, 49, 1, 14, 10, 18, 3, 23, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (50, 50, 1, 14, 0, 17, 1, 40, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (51, 51, 1, 20, 3, 27, 2, 31, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (52, 52, 1, 18, 2, 28, 2, 21, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (53, 53, 1, 12, 2, 20, 4, 24, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (54, 54, 1, 11, 5, 22, 0, 23, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (55, 55, 1, 17, 0, 22, 1, 37, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (56, 56, 1, 20, 0, 22, 3, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (57, 57, 1, 16, 5, 15, 2, 37, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (58, 58, 1, 10, 5, 27, 3, 34, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (59, 59, 1, 14, 7, 24, 3, 25, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (60, 60, 1, 19, 5, 18, 4, 30, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (61, 61, 1, 15, 3, 20, 0, 30, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (62, 62, 1, 15, 9, 22, 1, 32, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (63, 63, 1, 19, 1, 16, 4, 27, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (64, 64, 1, 14, 7, 15, 4, 39, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (65, 65, 1, 13, 0, 29, 5, 23, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (66, 66, 1, 19, 2, 16, 1, 27, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (67, 67, 1, 13, 1, 30, 0, 29, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (68, 68, 1, 18, 10, 21, 3, 33, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (69, 69, 1, 12, 2, 30, 2, 30, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (70, 70, 1, 12, 5, 28, 3, 37, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (71, 71, 1, 11, 6, 24, 5, 31, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (72, 72, 1, 17, 3, 15, 1, 40, 39, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (73, 73, 1, 16, 10, 22, 1, 36, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (74, 74, 1, 20, 3, 17, 3, 20, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (75, 75, 1, 19, 5, 21, 0, 40, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (76, 76, 1, 10, 4, 24, 5, 22, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (77, 77, 1, 17, 7, 18, 3, 37, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (78, 78, 1, 18, 10, 21, 1, 25, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (79, 79, 1, 18, 4, 20, 1, 32, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (80, 80, 1, 16, 4, 22, 2, 22, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (81, 81, 1, 11, 8, 18, 2, 24, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (82, 82, 1, 15, 10, 30, 3, 35, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (83, 83, 1, 20, 10, 27, 5, 31, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (84, 84, 1, 15, 1, 16, 0, 22, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (85, 85, 1, 13, 10, 28, 3, 29, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (86, 86, 1, 20, 0, 19, 4, 32, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (87, 87, 1, 20, 6, 21, 4, 26, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (88, 88, 1, 12, 9, 16, 5, 27, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (89, 89, 1, 19, 7, 24, 5, 28, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (90, 90, 1, 11, 2, 24, 5, 33, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (91, 91, 1, 20, 10, 27, 1, 34, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (92, 92, 1, 14, 5, 22, 4, 37, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (93, 93, 1, 16, 7, 25, 4, 38, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (94, 94, 1, 20, 0, 29, 5, 27, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (95, 95, 1, 17, 0, 21, 1, 28, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (96, 96, 1, 16, 4, 30, 0, 32, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (97, 97, 1, 17, 6, 21, 3, 29, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (98, 98, 1, 19, 8, 25, 0, 31, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (99, 99, 1, 17, 3, 29, 5, 35, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (100, 100, 1, 20, 10, 19, 0, 29, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (101, 101, 1, 12, 5, 27, 1, 34, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (102, 102, 1, 11, 10, 23, 3, 21, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (103, 103, 1, 20, 7, 25, 2, 33, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (104, 104, 1, 14, 0, 25, 5, 30, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (105, 105, 1, 18, 1, 26, 5, 29, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (106, 106, 1, 15, 9, 24, 0, 29, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (107, 107, 1, 12, 10, 18, 3, 39, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (108, 108, 1, 11, 7, 25, 1, 34, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (109, 109, 1, 18, 8, 30, 4, 21, 14, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (110, 110, 1, 12, 6, 22, 4, 36, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (111, 111, 1, 10, 10, 21, 5, 22, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (112, 112, 1, 10, 5, 15, 1, 22, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (113, 113, 1, 16, 5, 26, 3, 23, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (114, 114, 1, 14, 2, 16, 5, 24, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (115, 115, 1, 13, 9, 18, 3, 37, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (116, 116, 1, 19, 10, 18, 5, 25, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (117, 117, 1, 11, 7, 18, 5, 38, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (118, 118, 1, 18, 8, 18, 4, 37, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (119, 119, 1, 16, 6, 18, 4, 29, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (120, 120, 1, 17, 3, 26, 5, 25, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (121, 121, 1, 13, 2, 18, 3, 28, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (122, 122, 1, 11, 3, 20, 3, 21, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (123, 123, 1, 16, 5, 24, 4, 37, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (124, 124, 1, 17, 4, 22, 0, 25, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (125, 125, 1, 14, 4, 24, 1, 29, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (126, 126, 1, 19, 1, 24, 2, 35, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (127, 127, 1, 12, 0, 20, 5, 25, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (128, 128, 1, 13, 2, 24, 2, 40, 40, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (129, 129, 1, 15, 2, 24, 2, 27, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (130, 130, 1, 19, 7, 23, 5, 20, 13, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (131, 131, 1, 19, 10, 27, 3, 36, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (132, 132, 1, 17, 5, 18, 3, 35, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (133, 133, 1, 19, 0, 25, 2, 30, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (134, 134, 1, 10, 5, 19, 0, 27, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (135, 135, 1, 12, 8, 29, 4, 32, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (136, 136, 1, 19, 3, 27, 1, 23, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (137, 137, 1, 18, 7, 30, 2, 35, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (138, 138, 1, 12, 7, 15, 3, 34, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (139, 139, 1, 18, 3, 19, 5, 29, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (140, 140, 1, 19, 1, 17, 0, 27, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (141, 141, 1, 17, 1, 28, 4, 40, 40, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (142, 142, 1, 12, 7, 20, 3, 29, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (143, 143, 1, 18, 2, 25, 2, 34, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (144, 144, 1, 17, 4, 30, 0, 26, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (145, 145, 1, 20, 2, 17, 4, 32, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (146, 146, 1, 10, 8, 21, 0, 28, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (147, 147, 1, 11, 3, 21, 0, 30, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (148, 148, 1, 20, 5, 25, 3, 33, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (149, 149, 1, 11, 6, 15, 0, 32, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (150, 150, 1, 17, 10, 26, 3, 25, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (151, 151, 1, 15, 2, 30, 1, 37, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (152, 152, 1, 10, 10, 19, 4, 32, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (153, 153, 1, 16, 3, 23, 0, 23, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (154, 154, 1, 11, 3, 18, 4, 32, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (155, 155, 1, 18, 9, 20, 2, 29, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (156, 156, 1, 14, 10, 27, 4, 33, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (157, 157, 1, 18, 10, 24, 3, 35, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (158, 158, 1, 18, 4, 30, 4, 34, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (159, 159, 1, 14, 9, 18, 3, 34, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (160, 160, 1, 20, 2, 19, 2, 24, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (161, 161, 1, 13, 8, 26, 3, 32, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (162, 162, 1, 18, 6, 26, 3, 40, 40, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (163, 163, 1, 17, 4, 23, 5, 32, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (164, 164, 1, 14, 5, 20, 1, 23, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (165, 165, 1, 18, 10, 30, 3, 23, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (166, 166, 1, 20, 1, 16, 0, 30, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (167, 167, 1, 20, 7, 26, 1, 39, 39, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (168, 168, 1, 18, 10, 23, 5, 23, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (169, 169, 1, 11, 5, 26, 4, 30, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (170, 170, 1, 20, 9, 23, 0, 35, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (171, 171, 1, 16, 7, 16, 0, 34, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (172, 172, 1, 16, 5, 22, 3, 38, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (173, 173, 1, 15, 10, 20, 3, 27, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (174, 174, 1, 15, 6, 20, 0, 23, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (175, 175, 1, 18, 1, 25, 1, 32, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (176, 176, 1, 13, 5, 22, 2, 21, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (177, 177, 1, 17, 10, 28, 3, 22, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (178, 178, 1, 14, 9, 18, 4, 22, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (179, 179, 1, 15, 3, 18, 3, 37, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (180, 180, 1, 16, 8, 15, 5, 36, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (181, 181, 1, 16, 5, 21, 0, 20, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (182, 182, 1, 10, 1, 24, 1, 28, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (183, 183, 1, 10, 6, 15, 2, 27, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (184, 184, 1, 14, 1, 25, 0, 29, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (185, 185, 1, 16, 1, 18, 0, 21, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (186, 186, 1, 19, 10, 20, 5, 33, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (187, 187, 1, 17, 5, 22, 0, 24, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (188, 188, 1, 10, 9, 27, 2, 27, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (189, 189, 1, 18, 4, 25, 4, 35, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (190, 190, 1, 18, 8, 29, 3, 20, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (191, 191, 1, 18, 9, 30, 4, 35, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (192, 192, 1, 10, 1, 25, 5, 21, 13, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (193, 193, 1, 18, 9, 30, 1, 22, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (194, 194, 1, 12, 8, 22, 1, 32, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (195, 195, 1, 19, 5, 19, 0, 38, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (196, 196, 1, 11, 3, 28, 4, 40, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (197, 197, 1, 18, 1, 22, 1, 29, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (198, 198, 1, 14, 8, 22, 5, 40, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (199, 199, 1, 12, 5, 19, 2, 39, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (200, 200, 1, 15, 9, 28, 0, 20, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (201, 201, 1, 11, 3, 26, 4, 20, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (202, 202, 1, 13, 5, 25, 2, 21, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (203, 203, 1, 12, 8, 30, 5, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (204, 204, 1, 12, 4, 19, 3, 28, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (205, 205, 1, 19, 10, 23, 5, 21, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (206, 206, 1, 17, 9, 17, 0, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (207, 207, 1, 13, 6, 15, 0, 33, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (208, 208, 1, 10, 9, 16, 0, 23, 13, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (209, 209, 1, 20, 6, 26, 4, 21, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (210, 210, 1, 14, 2, 29, 5, 26, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (211, 211, 1, 14, 6, 21, 3, 22, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (212, 212, 1, 13, 0, 19, 5, 21, 13, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (213, 213, 1, 12, 10, 23, 0, 24, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (214, 214, 1, 13, 3, 15, 2, 27, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (215, 215, 1, 19, 3, 30, 2, 23, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (216, 216, 1, 15, 10, 17, 2, 36, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (217, 217, 1, 14, 10, 30, 4, 21, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (218, 218, 1, 14, 0, 26, 1, 22, 13, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (219, 219, 1, 19, 5, 19, 3, 39, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (220, 220, 1, 13, 5, 20, 5, 23, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (221, 221, 1, 17, 0, 23, 3, 28, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (222, 222, 1, 15, 1, 22, 5, 40, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (223, 223, 1, 16, 8, 23, 2, 36, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (224, 224, 1, 19, 3, 21, 0, 34, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (225, 225, 1, 17, 3, 23, 1, 27, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (226, 226, 1, 19, 6, 20, 5, 32, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (227, 227, 1, 16, 10, 25, 1, 39, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (228, 228, 1, 15, 9, 29, 1, 23, 14, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (229, 229, 1, 13, 10, 23, 1, 35, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (230, 230, 1, 11, 1, 16, 2, 33, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (231, 231, 1, 18, 7, 27, 1, 26, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (232, 232, 1, 11, 4, 27, 0, 35, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (233, 233, 1, 12, 2, 25, 4, 24, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (234, 234, 1, 16, 1, 18, 0, 29, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (235, 235, 1, 11, 0, 19, 5, 28, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (236, 236, 1, 19, 9, 21, 2, 35, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (237, 237, 1, 17, 1, 28, 0, 25, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (238, 238, 1, 12, 0, 18, 3, 36, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (239, 239, 1, 11, 10, 28, 5, 25, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (240, 240, 1, 13, 0, 16, 1, 33, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (241, 241, 1, 16, 3, 27, 0, 33, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (242, 242, 1, 11, 6, 30, 2, 24, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (243, 243, 1, 17, 10, 29, 2, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (244, 244, 1, 16, 3, 16, 0, 34, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (245, 245, 1, 15, 0, 20, 2, 31, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (246, 246, 1, 19, 0, 29, 0, 31, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (247, 247, 1, 17, 6, 23, 0, 23, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (248, 248, 1, 18, 8, 27, 5, 23, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (249, 249, 1, 10, 6, 29, 4, 25, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (250, 250, 1, 19, 5, 28, 4, 30, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (251, 251, 1, 20, 5, 21, 5, 20, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (252, 252, 1, 20, 5, 27, 1, 25, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (253, 253, 1, 10, 5, 22, 2, 31, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (254, 254, 1, 15, 7, 15, 5, 32, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (255, 255, 1, 10, 7, 30, 3, 37, 36, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (256, 256, 1, 20, 6, 19, 4, 20, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (257, 257, 1, 14, 7, 16, 3, 39, 36, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (258, 258, 1, 10, 8, 16, 1, 24, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (259, 259, 1, 18, 4, 16, 2, 33, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (260, 260, 1, 14, 7, 30, 4, 37, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (261, 261, 1, 20, 4, 27, 0, 34, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (262, 262, 1, 11, 4, 20, 1, 37, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (263, 263, 1, 17, 0, 24, 2, 27, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (264, 264, 1, 10, 7, 18, 4, 31, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (265, 265, 1, 20, 10, 25, 1, 39, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (266, 266, 1, 13, 4, 29, 0, 31, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (267, 267, 1, 19, 8, 27, 4, 22, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (268, 268, 2, 12, 9, 29, 0, 23, 14, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (269, 269, 2, 10, 2, 19, 5, 33, 27, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (270, 270, 2, 10, 2, 24, 4, 27, 24, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (271, 271, 2, 16, 9, 29, 4, 36, 33, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (272, 272, 2, 15, 2, 28, 5, 34, 29, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (273, 273, 2, 12, 0, 22, 3, 20, 20, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (274, 274, 2, 20, 4, 20, 2, 34, 29, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (275, 275, 2, 18, 2, 15, 5, 37, 37, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (276, 276, 2, 18, 7, 24, 0, 30, 22, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (277, 277, 2, 14, 7, 19, 5, 35, 33, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (278, 278, 2, 18, 3, 22, 2, 40, 40, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (279, 279, 2, 14, 4, 20, 0, 24, 19, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (280, 280, 2, 17, 8, 22, 3, 30, 26, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (281, 281, 2, 20, 5, 23, 4, 38, 38, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (282, 282, 2, 16, 8, 19, 1, 27, 22, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (283, 283, 2, 18, 1, 29, 2, 32, 27, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (284, 284, 2, 14, 8, 17, 1, 39, 34, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (285, 285, 2, 15, 4, 30, 5, 29, 24, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (286, 286, 2, 14, 0, 24, 5, 25, 24, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (287, 287, 2, 10, 7, 24, 4, 28, 19, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (288, 288, 2, 20, 2, 28, 3, 33, 31, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (289, 289, 2, 18, 8, 16, 4, 40, 38, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (290, 290, 2, 17, 3, 19, 3, 40, 36, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (291, 291, 2, 12, 3, 15, 2, 28, 27, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (292, 292, 2, 20, 1, 17, 5, 21, 16, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (293, 293, 2, 20, 2, 22, 1, 26, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (294, 294, 2, 17, 4, 24, 5, 37, 35, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (295, 295, 2, 11, 9, 21, 0, 38, 34, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (296, 296, 2, 14, 6, 18, 1, 35, 26, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (297, 297, 2, 11, 3, 24, 5, 32, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (298, 298, 2, 15, 2, 23, 5, 32, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (299, 299, 2, 16, 0, 28, 0, 27, 17, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (300, 300, 2, 13, 8, 27, 1, 37, 32, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (301, 301, 2, 20, 8, 23, 2, 26, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (302, 302, 2, 17, 6, 26, 1, 35, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (303, 303, 2, 18, 2, 22, 3, 28, 26, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (304, 304, 2, 14, 8, 24, 2, 22, 16, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (305, 305, 2, 15, 4, 27, 5, 34, 31, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (306, 306, 2, 11, 0, 22, 4, 34, 29, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (307, 307, 2, 11, 1, 20, 5, 37, 27, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (308, 308, 2, 15, 1, 29, 3, 29, 25, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (309, 309, 2, 20, 6, 28, 4, 27, 19, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (310, 310, 2, 15, 2, 29, 3, 35, 31, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (311, 311, 2, 16, 5, 23, 1, 31, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (312, 312, 2, 15, 7, 21, 5, 38, 37, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (313, 313, 2, 13, 10, 30, 1, 25, 20, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (314, 314, 2, 13, 3, 21, 1, 23, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (315, 315, 2, 20, 4, 27, 2, 31, 25, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (316, 316, 2, 19, 4, 21, 5, 24, 22, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (317, 317, 2, 16, 6, 28, 5, 40, 38, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (318, 318, 2, 13, 3, 28, 4, 39, 33, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (319, 319, 2, 13, 5, 20, 5, 32, 32, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (320, 320, 2, 10, 3, 29, 5, 24, 20, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (321, 321, 2, 15, 0, 18, 2, 31, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (322, 322, 2, 18, 7, 21, 4, 29, 26, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (323, 323, 2, 20, 4, 27, 3, 39, 39, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (324, 324, 2, 14, 9, 28, 2, 32, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (325, 325, 2, 11, 0, 16, 2, 23, 18, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (326, 326, 2, 13, 1, 15, 0, 22, 14, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (327, 327, 2, 19, 9, 22, 3, 37, 34, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (328, 328, 2, 11, 1, 28, 1, 38, 31, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (329, 329, 2, 18, 6, 26, 1, 34, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (330, 330, 2, 17, 2, 17, 0, 23, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (331, 331, 2, 14, 9, 25, 3, 37, 37, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (332, 332, 2, 14, 8, 18, 0, 28, 22, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (333, 333, 2, 19, 8, 25, 0, 33, 25, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (334, 334, 2, 20, 6, 29, 1, 30, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (335, 335, 2, 20, 2, 27, 4, 34, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (336, 336, 2, 12, 5, 17, 3, 24, 17, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (337, 337, 2, 11, 2, 29, 0, 31, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (338, 338, 2, 11, 9, 15, 0, 24, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (339, 339, 2, 12, 0, 24, 1, 23, 14, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (340, 340, 2, 15, 8, 29, 4, 33, 26, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (341, 341, 2, 17, 8, 20, 0, 30, 20, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (342, 342, 2, 10, 5, 30, 1, 22, 22, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (343, 343, 2, 11, 4, 23, 2, 24, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (344, 344, 2, 13, 3, 24, 2, 29, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (345, 345, 2, 18, 7, 19, 2, 38, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (346, 346, 2, 14, 7, 15, 0, 27, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (347, 347, 2, 18, 8, 28, 1, 35, 33, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (348, 348, 2, 15, 9, 23, 4, 22, 13, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (349, 349, 2, 15, 5, 26, 5, 37, 37, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (350, 350, 2, 11, 4, 30, 4, 22, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (351, 351, 2, 10, 4, 24, 1, 26, 22, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (352, 352, 2, 16, 9, 29, 4, 32, 29, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (353, 353, 2, 20, 5, 26, 5, 34, 33, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (354, 354, 2, 18, 1, 28, 0, 36, 31, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (355, 355, 2, 15, 4, 27, 5, 30, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (356, 356, 2, 18, 10, 23, 5, 36, 35, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (357, 357, 2, 18, 4, 16, 0, 35, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (358, 358, 2, 13, 4, 21, 3, 34, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (359, 359, 2, 11, 1, 16, 0, 25, 25, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (360, 360, 2, 19, 5, 21, 0, 31, 31, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (361, 361, 2, 13, 7, 30, 2, 31, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (362, 362, 2, 10, 8, 29, 1, 37, 37, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (363, 363, 2, 18, 10, 22, 3, 28, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (364, 364, 2, 15, 6, 19, 4, 39, 29, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (365, 365, 2, 10, 10, 20, 3, 26, 18, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (366, 366, 2, 10, 8, 24, 0, 21, 20, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (367, 367, 2, 14, 9, 19, 2, 23, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (368, 368, 2, 14, 8, 23, 3, 32, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (369, 369, 2, 16, 5, 26, 2, 39, 32, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (370, 370, 2, 19, 7, 22, 4, 35, 34, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (371, 371, 2, 13, 0, 25, 1, 34, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (372, 372, 2, 14, 5, 26, 4, 27, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (373, 373, 2, 18, 0, 28, 2, 26, 17, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (374, 374, 2, 18, 10, 26, 5, 20, 11, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (375, 375, 2, 10, 8, 22, 4, 40, 37, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (376, 376, 2, 11, 4, 20, 1, 32, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (377, 377, 2, 17, 4, 24, 4, 38, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (378, 378, 2, 19, 4, 27, 4, 32, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (379, 379, 2, 13, 3, 29, 1, 35, 33, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (380, 380, 2, 18, 0, 15, 2, 30, 21, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (381, 381, 2, 14, 1, 19, 5, 36, 32, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (382, 382, 2, 17, 6, 28, 5, 26, 18, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (383, 383, 2, 19, 5, 15, 1, 35, 25, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (384, 384, 2, 10, 2, 19, 0, 32, 30, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (385, 385, 2, 10, 3, 20, 1, 32, 23, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (386, 386, 2, 16, 0, 30, 3, 38, 28, '2026-05-16 17:00:42', '2026-05-21 08:12:10');
INSERT INTO `knowledge_mastery` VALUES (387, 387, 1, 18, 9, 24, 5, 30, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (388, 388, 1, 19, 2, 15, 5, 21, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (389, 389, 1, 10, 7, 25, 5, 31, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (390, 390, 1, 19, 8, 20, 5, 27, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (391, 391, 1, 16, 3, 21, 5, 40, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (392, 392, 1, 17, 5, 27, 0, 38, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (393, 393, 1, 10, 6, 22, 0, 38, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (394, 394, 1, 20, 7, 29, 3, 35, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (395, 395, 1, 15, 3, 18, 2, 32, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (396, 396, 1, 14, 4, 29, 3, 34, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (397, 397, 1, 11, 9, 17, 3, 38, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (398, 398, 1, 15, 6, 20, 5, 25, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (399, 399, 1, 15, 3, 15, 4, 40, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (400, 400, 1, 19, 7, 19, 1, 21, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (401, 401, 1, 19, 4, 23, 0, 21, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (402, 402, 1, 19, 3, 18, 1, 40, 39, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (403, 403, 1, 20, 6, 20, 1, 29, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (404, 404, 1, 14, 4, 22, 3, 32, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (405, 405, 1, 12, 2, 15, 1, 29, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (406, 406, 1, 19, 5, 17, 3, 30, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (407, 407, 1, 14, 4, 18, 0, 29, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (408, 408, 1, 20, 4, 24, 3, 39, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (409, 409, 1, 11, 3, 25, 1, 23, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (410, 410, 1, 12, 2, 15, 2, 27, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (411, 411, 1, 15, 4, 20, 5, 21, 11, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (412, 412, 1, 13, 2, 25, 1, 37, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (413, 413, 1, 20, 1, 22, 4, 39, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (414, 414, 1, 18, 9, 24, 2, 38, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (415, 415, 1, 19, 5, 25, 0, 26, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (416, 416, 1, 16, 9, 26, 0, 30, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (417, 417, 1, 14, 10, 29, 5, 30, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (418, 418, 1, 13, 6, 16, 5, 30, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (419, 419, 1, 17, 2, 21, 0, 29, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (420, 420, 1, 13, 3, 18, 2, 31, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (421, 421, 1, 18, 8, 30, 3, 35, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (422, 422, 1, 14, 1, 26, 4, 28, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (423, 423, 1, 15, 5, 30, 4, 30, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (424, 424, 1, 10, 0, 19, 5, 34, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (425, 425, 1, 18, 10, 20, 5, 24, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (426, 426, 1, 18, 6, 28, 3, 22, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (427, 427, 1, 10, 3, 29, 1, 29, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (428, 428, 1, 18, 4, 19, 3, 38, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (429, 429, 1, 10, 4, 22, 1, 34, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (430, 430, 1, 10, 0, 19, 3, 20, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (431, 431, 1, 16, 10, 18, 2, 39, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (432, 432, 1, 20, 0, 24, 1, 26, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (433, 433, 1, 14, 5, 19, 1, 28, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (434, 434, 1, 13, 7, 30, 4, 27, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (435, 435, 1, 13, 10, 19, 5, 28, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (436, 436, 1, 20, 4, 23, 1, 34, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (437, 437, 1, 14, 7, 19, 5, 23, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (438, 438, 1, 11, 2, 17, 4, 32, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (439, 439, 1, 18, 7, 19, 3, 35, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (440, 440, 1, 20, 8, 23, 3, 38, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (441, 441, 1, 19, 0, 26, 3, 39, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (442, 442, 1, 10, 6, 19, 4, 20, 10, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (443, 443, 1, 15, 5, 29, 0, 34, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (444, 444, 1, 20, 7, 28, 5, 31, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (445, 445, 1, 11, 4, 18, 5, 24, 14, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (446, 446, 1, 16, 4, 29, 0, 34, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (447, 447, 1, 20, 8, 29, 4, 30, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (448, 448, 1, 16, 6, 20, 5, 38, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (449, 449, 1, 12, 2, 24, 2, 32, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (450, 450, 1, 19, 6, 25, 5, 37, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (451, 451, 1, 14, 3, 24, 3, 37, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (452, 452, 1, 14, 9, 18, 4, 36, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (453, 453, 1, 10, 1, 29, 1, 24, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (454, 454, 1, 19, 1, 17, 3, 29, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (455, 455, 1, 17, 0, 26, 2, 38, 37, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (456, 456, 1, 20, 10, 28, 2, 21, 11, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (457, 457, 1, 11, 8, 18, 2, 20, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (458, 458, 1, 18, 1, 29, 5, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (459, 459, 1, 11, 2, 28, 2, 33, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (460, 460, 1, 12, 1, 30, 4, 21, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (461, 461, 1, 16, 6, 25, 3, 26, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (462, 462, 1, 12, 2, 16, 2, 36, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (463, 463, 1, 11, 3, 20, 5, 30, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (464, 464, 1, 20, 9, 22, 1, 38, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (465, 465, 1, 17, 3, 30, 1, 38, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (466, 466, 1, 20, 9, 25, 0, 21, 11, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (467, 467, 1, 18, 5, 22, 1, 34, 32, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (468, 468, 1, 17, 4, 25, 3, 31, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (469, 469, 1, 13, 1, 25, 3, 30, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (470, 470, 1, 18, 10, 23, 1, 22, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (471, 471, 1, 17, 2, 19, 4, 37, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (472, 472, 1, 16, 3, 25, 4, 21, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (473, 473, 1, 18, 10, 20, 5, 31, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (474, 474, 1, 12, 0, 30, 5, 28, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (475, 475, 1, 16, 2, 27, 4, 37, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (476, 476, 1, 14, 0, 23, 4, 37, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (477, 477, 1, 20, 2, 20, 0, 22, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (478, 478, 1, 11, 7, 20, 0, 25, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (479, 479, 1, 20, 7, 17, 3, 30, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (480, 480, 1, 18, 0, 29, 1, 35, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (481, 481, 1, 20, 2, 26, 5, 34, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (482, 482, 1, 17, 2, 28, 4, 20, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (483, 483, 1, 20, 5, 15, 1, 22, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (484, 484, 1, 14, 0, 24, 2, 36, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (485, 485, 1, 13, 8, 27, 0, 22, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (486, 486, 1, 12, 0, 23, 3, 39, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (487, 487, 1, 11, 2, 25, 2, 38, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (488, 488, 1, 19, 8, 20, 2, 36, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (489, 489, 1, 18, 10, 27, 3, 26, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (490, 490, 1, 10, 10, 27, 1, 21, 12, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (491, 491, 1, 14, 5, 23, 2, 23, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (492, 492, 1, 11, 1, 26, 1, 39, 30, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (493, 493, 1, 20, 8, 17, 1, 28, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (494, 494, 1, 13, 10, 16, 0, 28, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (495, 495, 1, 14, 1, 29, 2, 38, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (496, 496, 1, 13, 1, 19, 1, 21, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (497, 497, 1, 15, 2, 26, 4, 30, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (498, 498, 1, 19, 10, 23, 4, 23, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (499, 499, 1, 10, 5, 30, 0, 25, 16, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (500, 500, 1, 13, 6, 25, 2, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (501, 501, 1, 18, 6, 24, 3, 38, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (502, 502, 1, 13, 4, 25, 0, 34, 24, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (503, 503, 1, 16, 5, 22, 1, 39, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (504, 504, 1, 20, 8, 21, 0, 29, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (505, 505, 1, 10, 1, 21, 1, 35, 33, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (506, 506, 1, 14, 5, 21, 5, 23, 14, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (507, 507, 1, 15, 7, 15, 0, 24, 20, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (508, 508, 1, 14, 7, 19, 5, 38, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (509, 509, 1, 12, 8, 26, 1, 37, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (510, 510, 1, 16, 5, 23, 1, 27, 26, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (511, 511, 1, 13, 7, 28, 1, 40, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (512, 512, 1, 19, 10, 19, 5, 22, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (513, 513, 1, 18, 5, 17, 2, 34, 25, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (514, 514, 1, 15, 6, 18, 0, 22, 15, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (515, 515, 1, 19, 3, 28, 0, 38, 34, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (516, 516, 1, 10, 7, 18, 3, 26, 17, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (517, 517, 1, 20, 7, 26, 3, 31, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (518, 518, 1, 16, 5, 21, 1, 27, 22, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (519, 519, 1, 19, 3, 15, 5, 32, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (520, 520, 1, 10, 7, 22, 2, 26, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (521, 521, 1, 15, 4, 19, 4, 29, 27, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (522, 522, 1, 19, 3, 17, 3, 22, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (523, 523, 1, 13, 5, 23, 3, 35, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (524, 524, 1, 12, 9, 21, 5, 40, 35, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (525, 525, 1, 18, 1, 18, 1, 20, 19, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (526, 526, 1, 10, 10, 29, 0, 38, 38, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (527, 527, 1, 10, 8, 26, 4, 33, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (528, 528, 1, 17, 0, 15, 0, 22, 21, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (529, 529, 1, 15, 7, 27, 0, 36, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (530, 530, 1, 20, 1, 24, 1, 30, 29, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (531, 531, 1, 17, 8, 21, 3, 29, 23, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (532, 532, 1, 17, 0, 28, 2, 34, 31, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (533, 533, 1, 10, 8, 26, 4, 25, 18, '2026-05-16 17:00:42', '2026-05-16 17:00:42');
INSERT INTO `knowledge_mastery` VALUES (534, 534, 1, 13, 5, 29, 1, 31, 28, '2026-05-16 17:00:42', '2026-05-16 17:00:42');

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
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_recipient`(`recipient_id` ASC, `recipient_type` ASC) USING BTREE,
  INDEX `idx_alert_id`(`alert_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 530 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 4, 'TEACHER', '新预警: 学生268触发黄色挂科风险', '学生268(20250268)在《大学生计算机基础（二）》中触发黄色挂科预警，预测期末成绩44分，综合风险分20.5，请及时查看处理。', 463, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 19:59:34', 'SENT', '2026-05-21 13:37:18', '2026-05-21 13:37:18');
INSERT INTO `notification` VALUES (2, 4, 'TEACHER', '新预警: 学生268触发黄色知识点断层', '学生268(20250268)在《大学生计算机基础（二）》中触发黄色知识点断层预警，知识点正确率偏低，综合风险分19.0，请及时查看处理。', 458, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 14:00:00', 'SENT', '2026-05-21 13:37:18', '2026-05-21 13:37:18');
INSERT INTO `notification` VALUES (3, 4, 'TEACHER', '学生268已回应您的处理意见', '学生268对预警「黄色挂科风险」的回应：正在按老师要求积极改进中，已参加课后辅导', 463, 'STUDENT_RESPOND', 'IN_APP', 1, '2026-05-21 19:59:34', 'SENT', '2026-05-21 16:00:00', '2026-05-21 16:00:00');
INSERT INTO `notification` VALUES (4, 4, 'TEACHER', '风险升级: 学生309从黄色升为橙色', '学生309(20250309)在《大学生计算机基础（二）》中风险等级从黄色升级为橙色，当前风险分34.2，请重点关注。', 475, 'ALERT_UPGRADE', 'IN_APP', 1, '2026-05-21 19:59:35', 'SENT', '2026-05-21 13:37:18', '2026-05-21 13:37:18');
INSERT INTO `notification` VALUES (5, 268, 'STUDENT', '学业预警通知: 挂科风险', '你在《大学生计算机基础（二）》中存在挂科风险，预测期末成绩44分，综合风险分20.5。请尽快复习，老师已安排课后辅导。', 463, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 15:00:00', 'SENT', '2026-05-21 13:37:18', '2026-05-21 13:37:18');
INSERT INTO `notification` VALUES (6, 268, 'STUDENT', '教师处理结果: 已安排课后辅导', '马思国老师已处理你的预警，处理措施：【约谈 + 课后辅导】每周三下午1小时。请按时参加。', 463, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 15:00:00', '2026-05-21 15:00:00');
INSERT INTO `notification` VALUES (7, 277, 'STUDENT', '学业预警通知: 挂科风险', '你在《大学生计算机基础（二）》中存在挂科风险，预测期末成绩44分，综合风险分20.5。老师已联系家长并制定学习计划。', 463, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 14:00:00', '2026-05-21 14:00:00');
INSERT INTO `notification` VALUES (8, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：下周一来我办公室', 711, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:40:26', '2026-05-21 18:40:26');
INSERT INTO `notification` VALUES (9, 1, 'TEACHER', '学生54已回应您的处理意见', '学生54对预警的回应：收到', 711, 'STUDENT_RESPOND', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:40:56', '2026-05-21 18:40:56');
INSERT INTO `notification` VALUES (10, 1, 'TEACHER', '新预警: 学生1(20250001)触发黄色知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (11, 1, 'STUDENT', '学业预警通知: 知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-06-23 20:57:58', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (12, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (13, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:38:02', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (14, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (15, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (16, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (17, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (18, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (19, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (20, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (21, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (22, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (23, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (24, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (25, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (26, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (27, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (28, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (29, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (30, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (31, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (32, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (33, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (34, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (35, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (36, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (37, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (38, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (39, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (40, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (41, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (42, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (43, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (44, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (45, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (46, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (47, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (48, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (49, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 20:23:26', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (50, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (51, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (52, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (53, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (54, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (55, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (56, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (57, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (58, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (59, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (60, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (61, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:32', '2026-05-21 18:46:32');
INSERT INTO `notification` VALUES (62, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:57', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (63, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (64, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:58', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (65, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (66, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (67, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (68, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (69, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (70, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (71, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (72, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (73, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (74, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (75, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (76, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (77, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (78, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (79, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (80, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (81, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (82, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (83, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (84, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:59', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (85, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 19:45:41', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (86, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-21 18:50:55', 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (87, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 18:46:33', '2026-05-21 18:46:33');
INSERT INTO `notification` VALUES (88, 88, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：111', 768, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-21 19:45:41', 'SENT', '2026-05-21 18:51:10', '2026-05-21 18:51:10');
INSERT INTO `notification` VALUES (89, 1, 'TEACHER', '学生88已回应您的处理意见', '学生88对预警的回应：正在按老师要求积极改进中', 768, 'STUDENT_RESPOND', 'IN_APP', 1, '2026-05-21 19:45:58', 'SENT', '2026-05-21 19:45:38', '2026-05-21 19:45:38');
INSERT INTO `notification` VALUES (90, 1, 'TEACHER', '学生88已回应您的处理意见', '学生88对预警的回应：正在按老师要求积极改进中', 768, 'STUDENT_RESPOND', 'IN_APP', 1, '2026-05-21 19:45:58', 'SENT', '2026-05-21 19:45:46', '2026-05-21 19:45:46');
INSERT INTO `notification` VALUES (91, 85, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 767, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (92, 84, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 766, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (93, 78, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 765, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (94, 71, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 759, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (95, 78, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 764, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (96, 78, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 763, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (97, 76, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 762, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (98, 74, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 761, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:05', '2026-05-21 19:58:05');
INSERT INTO `notification` VALUES (99, 72, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 760, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (100, 89, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 769, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (101, 70, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 758, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (102, 68, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 757, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (103, 65, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 756, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (104, 2, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 729, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-26 17:38:02', 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (105, 3, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 730, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (106, 5, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 731, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (107, 12, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 732, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (108, 13, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 733, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (109, 14, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 734, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (110, 15, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 735, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (111, 19, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 736, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (112, 20, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 737, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (113, 23, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 739, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (114, 28, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 740, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (115, 29, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 741, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (116, 31, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 742, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (117, 32, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 743, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (118, 32, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 744, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (119, 32, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 745, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (120, 34, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 746, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (121, 36, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 747, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (122, 39, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 748, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-21 20:23:26', 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (123, 52, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 750, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (124, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 751, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (125, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 752, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (126, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 753, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (127, 56, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 754, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (128, 57, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 755, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (129, 42, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 749, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (130, 1, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 728, 'HANDLE_RESULT', 'IN_APP', 1, '2026-06-23 20:57:58', 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (131, 21, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 738, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (132, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 709, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (133, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警。', 710, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:58:06', '2026-05-21 19:58:06');
INSERT INTO `notification` VALUES (134, 4, 'TEACHER', '新预警: 学生268(20250268)触发黄色成绩骤降', '学生268(20250268)在课程中触发黄色级成绩骤降预警，综合风险分15.5。成绩出现明显下降趋势，综合风险分15.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (135, 268, 'STUDENT', '学业预警通知: 成绩骤降', '学生268(20250268)在课程中触发黄色级成绩骤降预警，综合风险分15.5。成绩出现明显下降趋势，综合风险分15.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (136, 4, 'TEACHER', '新预警: 学生269(20250269)触发黄色成绩骤降', '学生269(20250269)在课程中触发黄色级成绩骤降预警，综合风险分15.2。成绩出现明显下降趋势，综合风险分15.2。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (137, 269, 'STUDENT', '学业预警通知: 成绩骤降', '学生269(20250269)在课程中触发黄色级成绩骤降预警，综合风险分15.2。成绩出现明显下降趋势，综合风险分15.2。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (138, 4, 'TEACHER', '新预警: 学生272(20250272)触发黄色作业欠交', '学生272(20250272)在课程中触发黄色级作业欠交预警，综合风险分18.8。作业提交率较低，综合风险分18.8。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (139, 272, 'STUDENT', '学业预警通知: 作业欠交', '学生272(20250272)在课程中触发黄色级作业欠交预警，综合风险分18.8。作业提交率较低，综合风险分18.8。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (140, 4, 'TEACHER', '新预警: 学生277(20250277)触发黄色挂科风险', '学生277(20250277)在课程中触发黄色级挂科风险预警，综合风险分25.8。预测期末成绩44.0分，存在挂科风险。综合风险分25.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (141, 277, 'STUDENT', '学业预警通知: 挂科风险', '学生277(20250277)在课程中触发黄色级挂科风险预警，综合风险分25.8。预测期末成绩44.0分，存在挂科风险。综合风险分25.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (142, 4, 'TEACHER', '新预警: 学生280(20250280)触发黄色挂科风险', '学生280(20250280)在课程中触发黄色级挂科风险预警，综合风险分19.0。预测期末成绩52.0分，存在挂科风险。综合风险分19.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (143, 280, 'STUDENT', '学业预警通知: 挂科风险', '学生280(20250280)在课程中触发黄色级挂科风险预警，综合风险分19.0。预测期末成绩52.0分，存在挂科风险。综合风险分19.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (144, 4, 'TEACHER', '新预警: 学生281(20250281)触发黄色挂科风险', '学生281(20250281)在课程中触发黄色级挂科风险预警，综合风险分16.6。预测期末成绩47.0分，存在挂科风险。综合风险分16.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (145, 281, 'STUDENT', '学业预警通知: 挂科风险', '学生281(20250281)在课程中触发黄色级挂科风险预警，综合风险分16.6。预测期末成绩47.0分，存在挂科风险。综合风险分16.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (146, 4, 'TEACHER', '新预警: 学生281(20250281)触发黄色成绩骤降', '学生281(20250281)在课程中触发黄色级成绩骤降预警，综合风险分16.6。成绩出现明显下降趋势，综合风险分16.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (147, 281, 'STUDENT', '学业预警通知: 成绩骤降', '学生281(20250281)在课程中触发黄色级成绩骤降预警，综合风险分16.6。成绩出现明显下降趋势，综合风险分16.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (148, 4, 'TEACHER', '新预警: 学生284(20250284)触发黄色成绩骤降', '学生284(20250284)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (149, 284, 'STUDENT', '学业预警通知: 成绩骤降', '学生284(20250284)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:41', '2026-05-21 19:59:41');
INSERT INTO `notification` VALUES (150, 4, 'TEACHER', '新预警: 学生287(20250287)触发黄色作业欠交', '学生287(20250287)在课程中触发黄色级作业欠交预警，综合风险分21.8。作业提交率较低，综合风险分21.8。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (151, 287, 'STUDENT', '学业预警通知: 作业欠交', '学生287(20250287)在课程中触发黄色级作业欠交预警，综合风险分21.8。作业提交率较低，综合风险分21.8。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (152, 4, 'TEACHER', '新预警: 学生296(20250296)触发黄色挂科风险', '学生296(20250296)在课程中触发黄色级挂科风险预警，综合风险分20.3。预测期末成绩43.0分，存在挂科风险。综合风险分20.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (153, 296, 'STUDENT', '学业预警通知: 挂科风险', '学生296(20250296)在课程中触发黄色级挂科风险预警，综合风险分20.3。预测期末成绩43.0分，存在挂科风险。综合风险分20.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (154, 4, 'TEACHER', '新预警: 学生296(20250296)触发黄色成绩骤降', '学生296(20250296)在课程中触发黄色级成绩骤降预警，综合风险分20.3。成绩出现明显下降趋势，综合风险分20.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (155, 296, 'STUDENT', '学业预警通知: 成绩骤降', '学生296(20250296)在课程中触发黄色级成绩骤降预警，综合风险分20.3。成绩出现明显下降趋势，综合风险分20.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (156, 4, 'TEACHER', '新预警: 学生304(20250304)触发黄色挂科风险', '学生304(20250304)在课程中触发黄色级挂科风险预警，综合风险分22.2。预测期末成绩59.0分，存在挂科风险。综合风险分22.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (157, 304, 'STUDENT', '学业预警通知: 挂科风险', '学生304(20250304)在课程中触发黄色级挂科风险预警，综合风险分22.2。预测期末成绩59.0分，存在挂科风险。综合风险分22.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (158, 4, 'TEACHER', '新预警: 学生306(20250306)触发黄色挂科风险', '学生306(20250306)在课程中触发黄色级挂科风险预警，综合风险分18.1。预测期末成绩49.0分，存在挂科风险。综合风险分18.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (159, 306, 'STUDENT', '学业预警通知: 挂科风险', '学生306(20250306)在课程中触发黄色级挂科风险预警，综合风险分18.1。预测期末成绩49.0分，存在挂科风险。综合风险分18.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (160, 4, 'TEACHER', '新预警: 学生308(20250308)触发黄色成绩骤降', '学生308(20250308)在课程中触发黄色级成绩骤降预警，综合风险分16.5。成绩出现明显下降趋势，综合风险分16.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (161, 308, 'STUDENT', '学业预警通知: 成绩骤降', '学生308(20250308)在课程中触发黄色级成绩骤降预警，综合风险分16.5。成绩出现明显下降趋势，综合风险分16.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (162, 4, 'TEACHER', '新预警: 学生309(20250309)触发橙色作业欠交', '学生309(20250309)在课程中触发橙色级作业欠交预警，综合风险分33.1。作业提交率较低，综合风险分33.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (163, 309, 'STUDENT', '学业预警通知: 作业欠交', '学生309(20250309)在课程中触发橙色级作业欠交预警，综合风险分33.1。作业提交率较低，综合风险分33.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 19:59:42', '2026-05-21 19:59:42');
INSERT INTO `notification` VALUES (164, 1, 'TEACHER', '新预警: 学生1(20250001)触发黄色知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (165, 1, 'STUDENT', '学业预警通知: 知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-06-23 20:57:58', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (166, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (167, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:38:02', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (168, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (169, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (170, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (171, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (172, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (173, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (174, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (175, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (176, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (177, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (178, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (179, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (180, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (181, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (182, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (183, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (184, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (185, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (186, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (187, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (188, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (189, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (190, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (191, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (192, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (193, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (194, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (195, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (196, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (197, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (198, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (199, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (200, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (201, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (202, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (203, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (204, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (205, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (206, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (207, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (208, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (209, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (210, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (211, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (212, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (213, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (214, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (215, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (216, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (217, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (218, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (219, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (220, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (221, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (222, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (223, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (224, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (225, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (226, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (227, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (228, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (229, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (230, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (231, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (232, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (233, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (234, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (235, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (236, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (237, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (238, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (239, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (240, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-25 11:05:39', 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (241, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-21 20:34:11', '2026-05-21 20:34:11');
INSERT INTO `notification` VALUES (242, 1, 'TEACHER', '【每日预警汇总】2026-05-25', '【每日预警汇总】2026-05-25\n━━━━━━━━━━\n✅ 今日无新增预警，一切正常', NULL, 'DAILY_SUMMARY', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-25 11:57:22', '2026-05-25 11:57:22');
INSERT INTO `notification` VALUES (243, 1, 'TEACHER', '【本周预警周报】2026-05-25 - 2026-05-31', '【本周预警周报】2026-05-25 - 2026-05-31\n━━━━━━━━━━\n✅ 本周无新增预警，一切正常', NULL, 'WEEKLY_SUMMARY', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-25 11:57:26', '2026-05-25 11:57:26');
INSERT INTO `notification` VALUES (244, 1, 'TEACHER', '⏰ 催办：学生1的预警已超2天未处理', '学生1的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 787, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (245, 1, 'TEACHER', '⏰ 催办：学生2的预警已超2天未处理', '学生2的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 788, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (246, 1, 'TEACHER', '⏰ 催办：学生3的预警已超2天未处理', '学生3的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 789, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (247, 1, 'TEACHER', '⏰ 催办：学生5的预警已超2天未处理', '学生5的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 790, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (248, 1, 'TEACHER', '⏰ 催办：学生12的预警已超2天未处理', '学生12的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 791, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (249, 1, 'TEACHER', '⏰ 催办：学生13的预警已超2天未处理', '学生13的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 792, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (250, 1, 'TEACHER', '⏰ 催办：学生14的预警已超2天未处理', '学生14的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 793, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (251, 1, 'TEACHER', '⏰ 催办：学生15的预警已超2天未处理', '学生15的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 794, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (252, 1, 'TEACHER', '⏰ 催办：学生19的预警已超2天未处理', '学生19的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 795, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (253, 1, 'TEACHER', '⏰ 催办：学生20的预警已超2天未处理', '学生20的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 796, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (254, 1, 'TEACHER', '⏰ 催办：学生21的预警已超2天未处理', '学生21的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 797, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (255, 1, 'TEACHER', '⏰ 催办：学生23的预警已超2天未处理', '学生23的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 798, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (256, 1, 'TEACHER', '⏰ 催办：学生28的预警已超2天未处理', '学生28的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 799, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (257, 1, 'TEACHER', '⏰ 催办：学生29的预警已超2天未处理', '学生29的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 800, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (258, 1, 'TEACHER', '⏰ 催办：学生31的预警已超2天未处理', '学生31的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 801, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (259, 1, 'TEACHER', '⏰ 催办：学生32的预警已超2天未处理', '学生32的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 802, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (260, 1, 'TEACHER', '⏰ 催办：学生32的预警已超2天未处理', '学生32的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 803, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (261, 1, 'TEACHER', '⏰ 催办：学生32的预警已超2天未处理', '学生32的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 804, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (262, 1, 'TEACHER', '⏰ 催办：学生34的预警已超2天未处理', '学生34的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 805, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (263, 1, 'TEACHER', '⏰ 催办：学生36的预警已超2天未处理', '学生36的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 806, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (264, 1, 'TEACHER', '⏰ 催办：学生39的预警已超2天未处理', '学生39的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 807, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (265, 1, 'TEACHER', '⏰ 催办：学生42的预警已超2天未处理', '学生42的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 808, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (266, 1, 'TEACHER', '⏰ 催办：学生52的预警已超2天未处理', '学生52的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 809, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (267, 1, 'TEACHER', '⏰ 催办：学生54的预警已超2天未处理', '学生54的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 810, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (268, 1, 'TEACHER', '⏰ 催办：学生54的预警已超2天未处理', '学生54的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 811, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (269, 1, 'TEACHER', '⏰ 催办：学生54的预警已超2天未处理', '学生54的红色（高风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 812, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (270, 1, 'TEACHER', '⏰ 催办：学生56的预警已超2天未处理', '学生56的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 813, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (271, 1, 'TEACHER', '⏰ 催办：学生57的预警已超2天未处理', '学生57的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 814, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (272, 1, 'TEACHER', '⏰ 催办：学生65的预警已超2天未处理', '学生65的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 815, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (273, 1, 'TEACHER', '⏰ 催办：学生68的预警已超2天未处理', '学生68的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 816, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (274, 1, 'TEACHER', '⏰ 催办：学生70的预警已超2天未处理', '学生70的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 817, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (275, 1, 'TEACHER', '⏰ 催办：学生71的预警已超2天未处理', '学生71的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 818, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (276, 1, 'TEACHER', '⏰ 催办：学生72的预警已超2天未处理', '学生72的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 819, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (277, 1, 'TEACHER', '⏰ 催办：学生74的预警已超2天未处理', '学生74的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 820, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (278, 1, 'TEACHER', '⏰ 催办：学生76的预警已超2天未处理', '学生76的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 821, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (279, 1, 'TEACHER', '⏰ 催办：学生78的预警已超2天未处理', '学生78的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 822, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (280, 1, 'TEACHER', '⏰ 催办：学生78的预警已超2天未处理', '学生78的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 823, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (281, 1, 'TEACHER', '⏰ 催办：学生78的预警已超2天未处理', '学生78的橙色（中风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 824, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (282, 1, 'TEACHER', '⏰ 催办：学生84的预警已超2天未处理', '学生84的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 825, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (283, 1, 'TEACHER', '⏰ 催办：学生85的预警已超2天未处理', '学生85的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 826, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (284, 1, 'TEACHER', '⏰ 催办：学生88的预警已超2天未处理', '学生88的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 827, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (285, 1, 'TEACHER', '⏰ 催办：学生89的预警已超2天未处理', '学生89的黄色（低风险）预警生成于2026-05-21，已超过2天未处理，请尽快处理。', 828, 'TIMEOUT', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 10:23:48', '2026-05-26 10:23:48');
INSERT INTO `notification` VALUES (286, 1, 'TEACHER', '【每日预警汇总】2026-05-26', '【每日预警汇总】2026-05-26\n━━━━━━━━━━\n✅ 今日无新增预警，一切正常', NULL, 'DAILY_SUMMARY', 'IN_APP', 1, '2026-05-26 11:31:30', 'SENT', '2026-05-26 11:31:12', '2026-05-26 11:31:12');
INSERT INTO `notification` VALUES (287, 1, 'TEACHER', '111', '请尽快处理', NULL, 'ADMIN_NOTICE', 'IN_APP', 1, '2026-05-26 17:16:23', 'SENT', '2026-05-26 17:16:07', '2026-05-26 17:16:07');
INSERT INTO `notification` VALUES (288, 1, 'TEACHER', '新预警: 学生1(20250001)触发黄色知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (289, 1, 'STUDENT', '学业预警通知: 知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-06-23 20:57:58', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (290, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (291, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:38:02', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (292, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (293, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (294, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (295, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (296, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (297, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (298, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (299, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (300, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (301, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (302, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (303, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (304, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (305, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (306, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (307, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (308, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:50', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (309, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (310, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (311, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (312, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (313, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (314, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (315, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (316, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (317, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (318, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (319, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (320, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (321, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (322, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (323, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (324, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (325, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (326, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (327, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (328, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (329, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (330, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (331, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (332, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (333, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (334, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (335, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (336, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (337, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (338, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (339, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (340, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (341, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (342, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (343, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (344, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (345, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (346, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (347, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (348, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (349, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (350, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (351, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (352, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (353, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (354, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (355, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (356, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:49', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (357, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 18:06:49', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (358, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:49', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (359, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (360, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:48', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (361, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (362, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:47', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (363, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (364, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-05-26 17:35:51', 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (365, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:32:14', '2026-05-26 17:32:14');
INSERT INTO `notification` VALUES (366, 1, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：11111', 829, 'HANDLE_RESULT', 'IN_APP', 1, '2026-06-23 20:57:58', 'SENT', '2026-05-26 17:35:19', '2026-05-26 17:35:19');
INSERT INTO `notification` VALUES (367, 2, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：11111', 830, 'HANDLE_RESULT', 'IN_APP', 1, '2026-05-26 17:38:02', 'SENT', '2026-05-26 17:37:26', '2026-05-26 17:37:26');
INSERT INTO `notification` VALUES (368, 1, 'TEACHER', '学生2已回应您的处理意见', '学生2对预警的回应：正在按老师要求积极改进中', 830, 'STUDENT_RESPOND', 'IN_APP', 1, '2026-05-26 17:42:21', 'SENT', '2026-05-26 17:37:59', '2026-05-26 17:37:59');
INSERT INTO `notification` VALUES (369, 54, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：1111', 854, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:42:13', '2026-05-26 17:42:13');
INSERT INTO `notification` VALUES (370, 1, 'TEACHER', '【每日预警汇总】2026-05-26', '【每日预警汇总】2026-05-26\n━━━━━━━━━━━━━━━━━━━\n? 今日新增: 42条（?1 ?9 ?32）\n⚠️ 待处理: 38条\n✅ 已处理: 3条 (7%)\n\n? 高风险学生:\n  学生54 - 42.4分(RED)\n  学生78 - 35.3分(ORANGE)\n  学生39 - 32.4分(ORANGE)\n\n? 班级分布:\n  计算机1班（大一）: 42条(?1?9?32)\n', NULL, 'DAILY_SUMMARY', 'IN_APP', 1, '2026-05-26 18:05:45', 'SENT', '2026-05-26 17:42:44', '2026-05-26 17:42:44');
INSERT INTO `notification` VALUES (371, 3, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：434545', 831, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 17:49:54', '2026-05-26 17:49:54');
INSERT INTO `notification` VALUES (372, 5, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：111', 832, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-05-26 18:05:23', '2026-05-26 18:05:23');
INSERT INTO `notification` VALUES (373, 1, 'TEACHER', '【每日预警汇总】2026-05-26', '【每日预警汇总】2026-05-26\n━━━━━━━━━━━━━━━━━━━\n? 今日新增: 42条（?1 ?9 ?32）\n⚠️ 待处理: 36条\n✅ 已处理: 5条 (12%)\n\n? 高风险学生:\n  学生54 - 42.4分(RED)\n  学生78 - 35.3分(ORANGE)\n  学生39 - 32.4分(ORANGE)\n\n? 班级分布:\n  计算机1班（大一）: 42条(?1?9?32)\n', NULL, 'DAILY_SUMMARY', 'IN_APP', 1, '2026-05-26 18:05:42', 'SENT', '2026-05-26 18:05:35', '2026-05-26 18:05:35');
INSERT INTO `notification` VALUES (374, 1, 'TEACHER', '【每日预警汇总】2026-06-23', '【每日预警汇总】2026-06-23\n━━━━━━━━━━\n✅ 今日无新增预警，一切正常', NULL, 'DAILY_SUMMARY', 'IN_APP', 1, '2026-06-23 21:34:13', 'SENT', '2026-06-23 20:56:10', '2026-06-23 20:56:10');
INSERT INTO `notification` VALUES (375, 1, 'TEACHER', '新预警: 学生1(20250001)触发黄色知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (376, 1, 'STUDENT', '学业预警通知: 知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (377, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (378, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (379, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (380, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (381, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (382, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:18', '2026-06-25 20:59:18');
INSERT INTO `notification` VALUES (383, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (384, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (385, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (386, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (387, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (388, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (389, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (390, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (391, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (392, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (393, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (394, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (395, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (396, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (397, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (398, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (399, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (400, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (401, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (402, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (403, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (404, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (405, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (406, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (407, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (408, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (409, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (410, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (411, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (412, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (413, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (414, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (415, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (416, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (417, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (418, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (419, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (420, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (421, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (422, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (423, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (424, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (425, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (426, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (427, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (428, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (429, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (430, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (431, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (432, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (433, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (434, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (435, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (436, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (437, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (438, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (439, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (440, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (441, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (442, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (443, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (444, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (445, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (446, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (447, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (448, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (449, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (450, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (451, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (452, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 20:59:19', '2026-06-25 20:59:19');
INSERT INTO `notification` VALUES (453, 1, 'TEACHER', '新预警: 学生1(20250001)触发黄色知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (454, 1, 'STUDENT', '学业预警通知: 知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (455, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (456, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (457, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (458, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (459, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (460, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (461, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (462, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (463, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (464, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (465, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (466, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (467, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (468, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (469, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (470, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (471, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (472, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (473, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (474, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (475, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (476, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (477, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (478, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (479, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (480, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (481, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (482, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (483, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (484, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (485, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (486, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (487, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (488, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (489, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (490, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (491, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (492, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (493, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (494, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (495, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:05', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (496, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (497, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (498, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (499, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (500, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (501, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (502, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (503, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (504, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (505, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (506, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (507, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (508, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (509, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (510, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (511, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (512, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (513, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (514, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (515, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (516, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (517, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (518, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (519, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (520, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (521, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (522, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (523, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (524, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (525, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (526, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (527, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (528, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (529, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (530, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-06-25 21:12:06', '2026-06-25 21:12:06');
INSERT INTO `notification` VALUES (531, 1, 'TEACHER', '新预警: 学生1(20250001)触发黄色知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (532, 1, 'STUDENT', '学业预警通知: 知识点断层', '学生1(20250001)在课程中触发黄色级知识点断层预警，综合风险分26.0。知识点掌握率偏低，综合风险分26.0。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (533, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (534, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (535, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (536, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (537, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (538, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (539, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (540, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (541, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (542, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (543, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (544, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (545, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (546, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (547, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (548, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (549, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (550, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (551, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (552, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (553, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (554, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (555, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (556, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (557, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (558, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (559, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (560, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (561, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (562, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (563, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (564, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (565, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (566, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (567, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (568, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (569, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (570, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (571, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (572, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (573, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (574, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (575, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (576, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (577, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (578, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (579, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (580, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (581, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (582, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (583, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (584, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (585, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (586, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (587, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (588, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (589, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (590, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (591, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (592, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (593, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (594, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (595, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (596, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (597, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (598, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (599, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (600, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (601, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (602, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (603, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (604, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (605, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (606, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (607, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (608, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:32:48', '2026-07-08 16:32:48');
INSERT INTO `notification` VALUES (609, 1, 'STUDENT', '教师已处理你的预警', '老师已处理你的学业预警，处理备注：进行练习', 955, 'HANDLE_RESULT', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 16:40:23', '2026-07-08 16:40:23');
INSERT INTO `notification` VALUES (610, 1, 'TEACHER', '学生1已回应您的处理意见', '学生1对预警的回应：正在按老师要求积极改进中', 955, 'STUDENT_RESPOND', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 16:40:42', '2026-07-08 16:40:42');
INSERT INTO `notification` VALUES (611, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (612, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (613, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (614, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (615, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (616, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (617, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (618, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (619, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (620, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (621, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (622, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (623, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (624, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (625, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (626, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (627, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (628, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:10', '2026-07-08 21:28:10');
INSERT INTO `notification` VALUES (629, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (630, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (631, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (632, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (633, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (634, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (635, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (636, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (637, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (638, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (639, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (640, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (641, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (642, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (643, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (644, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (645, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (646, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (647, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (648, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (649, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (650, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (651, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (652, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (653, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (654, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (655, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (656, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (657, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (658, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (659, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (660, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (661, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (662, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (663, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (664, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (665, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (666, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (667, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (668, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (669, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (670, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (671, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (672, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (673, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (674, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (675, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (676, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (677, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (678, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (679, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (680, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (681, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (682, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (683, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (684, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (685, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (686, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:11', '2026-07-08 21:28:11');
INSERT INTO `notification` VALUES (687, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (688, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (689, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (690, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (691, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (692, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (693, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (694, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (695, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (696, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (697, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (698, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (699, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (700, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (701, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (702, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (703, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (704, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (705, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (706, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (707, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (708, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (709, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (710, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (711, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (712, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (713, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (714, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (715, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (716, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (717, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (718, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (719, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (720, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (721, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (722, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (723, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (724, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (725, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (726, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (727, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (728, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (729, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (730, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (731, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (732, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (733, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (734, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (735, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (736, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (737, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (738, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (739, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (740, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (741, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (742, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (743, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (744, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (745, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (746, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (747, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (748, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (749, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (750, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (751, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (752, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (753, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (754, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (755, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (756, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (757, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (758, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (759, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (760, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (761, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (762, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:28:23', '2026-07-08 21:28:23');
INSERT INTO `notification` VALUES (763, 1, 'TEACHER', '新预警: 学生2(20250002)触发黄色挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (764, 2, 'STUDENT', '学业预警通知: 挂科风险', '学生2(20250002)在课程中触发黄色级挂科风险预警，综合风险分18.2。预测期末成绩56.0分，存在挂科风险。综合风险分18.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (765, 1, 'TEACHER', '新预警: 学生3(20250003)触发黄色成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (766, 3, 'STUDENT', '学业预警通知: 成绩骤降', '学生3(20250003)在课程中触发黄色级成绩骤降预警，综合风险分16.3。成绩出现明显下降趋势，综合风险分16.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (767, 1, 'TEACHER', '新预警: 学生5(20250005)触发黄色挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (768, 5, 'STUDENT', '学业预警通知: 挂科风险', '学生5(20250005)在课程中触发黄色级挂科风险预警，综合风险分23.3。预测期末成绩54.0分，存在挂科风险。综合风险分23.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (769, 1, 'TEACHER', '新预警: 学生12(20250012)触发黄色挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (770, 12, 'STUDENT', '学业预警通知: 挂科风险', '学生12(20250012)在课程中触发黄色级挂科风险预警，综合风险分24.2。预测期末成绩57.0分，存在挂科风险。综合风险分24.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (771, 1, 'TEACHER', '新预警: 学生13(20250013)触发黄色成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (772, 13, 'STUDENT', '学业预警通知: 成绩骤降', '学生13(20250013)在课程中触发黄色级成绩骤降预警，综合风险分17.9。成绩出现明显下降趋势，综合风险分17.9。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (773, 1, 'TEACHER', '新预警: 学生14(20250014)触发橙色挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (774, 14, 'STUDENT', '学业预警通知: 挂科风险', '学生14(20250014)在课程中触发橙色级挂科风险预警，综合风险分30.5。预测期末成绩37.0分，存在挂科风险。综合风险分30.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (775, 1, 'TEACHER', '新预警: 学生15(20250015)触发黄色知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (776, 15, 'STUDENT', '学业预警通知: 知识点断层', '学生15(20250015)在课程中触发黄色级知识点断层预警，综合风险分21.3。知识点掌握率偏低，综合风险分21.3。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (777, 1, 'TEACHER', '新预警: 学生19(20250019)触发黄色知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (778, 19, 'STUDENT', '学业预警通知: 知识点断层', '学生19(20250019)在课程中触发黄色级知识点断层预警，综合风险分29.9。知识点掌握率偏低，综合风险分29.9。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (779, 1, 'TEACHER', '新预警: 学生20(20250020)触发黄色成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (780, 20, 'STUDENT', '学业预警通知: 成绩骤降', '学生20(20250020)在课程中触发黄色级成绩骤降预警，综合风险分19.6。成绩出现明显下降趋势，综合风险分19.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (781, 1, 'TEACHER', '新预警: 学生21(20250021)触发黄色挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (782, 21, 'STUDENT', '学业预警通知: 挂科风险', '学生21(20250021)在课程中触发黄色级挂科风险预警，综合风险分27.1。预测期末成绩41.0分，存在挂科风险。综合风险分27.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (783, 1, 'TEACHER', '新预警: 学生23(20250023)触发黄色成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (784, 23, 'STUDENT', '学业预警通知: 成绩骤降', '学生23(20250023)在课程中触发黄色级成绩骤降预警，综合风险分19.3。成绩出现明显下降趋势，综合风险分19.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (785, 1, 'TEACHER', '新预警: 学生28(20250028)触发黄色挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (786, 28, 'STUDENT', '学业预警通知: 挂科风险', '学生28(20250028)在课程中触发黄色级挂科风险预警，综合风险分22.1。预测期末成绩55.0分，存在挂科风险。综合风险分22.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (787, 1, 'TEACHER', '新预警: 学生29(20250029)触发橙色挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (788, 29, 'STUDENT', '学业预警通知: 挂科风险', '学生29(20250029)在课程中触发橙色级挂科风险预警，综合风险分31.8。预测期末成绩35.0分，存在挂科风险。综合风险分31.8，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (789, 1, 'TEACHER', '新预警: 学生31(20250031)触发黄色成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (790, 31, 'STUDENT', '学业预警通知: 成绩骤降', '学生31(20250031)在课程中触发黄色级成绩骤降预警，综合风险分20.8。成绩出现明显下降趋势，综合风险分20.8。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (791, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (792, 32, 'STUDENT', '学业预警通知: 挂科风险', '学生32(20250032)在课程中触发黄色级挂科风险预警，综合风险分17.6。预测期末成绩56.0分，存在挂科风险。综合风险分17.6，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (793, 1, 'TEACHER', '新预警: 学生32(20250032)触发黄色成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (794, 32, 'STUDENT', '学业预警通知: 成绩骤降', '学生32(20250032)在课程中触发黄色级成绩骤降预警，综合风险分17.6。成绩出现明显下降趋势，综合风险分17.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (795, 1, 'TEACHER', '新预警: 学生34(20250034)触发黄色成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (796, 34, 'STUDENT', '学业预警通知: 成绩骤降', '学生34(20250034)在课程中触发黄色级成绩骤降预警，综合风险分19.5。成绩出现明显下降趋势，综合风险分19.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (797, 1, 'TEACHER', '新预警: 学生36(20250036)触发黄色作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (798, 36, 'STUDENT', '学业预警通知: 作业欠交', '学生36(20250036)在课程中触发黄色级作业欠交预警，综合风险分20.1。作业提交率较低，综合风险分20.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (799, 1, 'TEACHER', '新预警: 学生39(20250039)触发橙色知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (800, 39, 'STUDENT', '学业预警通知: 知识点断层', '学生39(20250039)在课程中触发橙色级知识点断层预警，综合风险分32.4。知识点掌握率偏低，综合风险分32.4。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (801, 1, 'TEACHER', '新预警: 学生42(20250042)触发黄色成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (802, 42, 'STUDENT', '学业预警通知: 成绩骤降', '学生42(20250042)在课程中触发黄色级成绩骤降预警，综合风险分18.5。成绩出现明显下降趋势，综合风险分18.5。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (803, 1, 'TEACHER', '新预警: 学生52(20250052)触发黄色挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (804, 52, 'STUDENT', '学业预警通知: 挂科风险', '学生52(20250052)在课程中触发黄色级挂科风险预警，综合风险分16.2。预测期末成绩59.0分，存在挂科风险。综合风险分16.2，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (805, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (806, 54, 'STUDENT', '学业预警通知: 挂科风险', '学生54(20250054)在课程中触发橙色级挂科风险预警，综合风险分32.4。预测期末成绩59.0分，存在挂科风险。综合风险分32.4，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (807, 1, 'TEACHER', '新预警: 学生54(20250054)触发橙色作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (808, 54, 'STUDENT', '学业预警通知: 作业欠交', '学生54(20250054)在课程中触发橙色级作业欠交预警，综合风险分32.4。作业提交率较低，综合风险分32.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (809, 1, 'TEACHER', '新预警: 学生56(20250056)触发黄色成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (810, 56, 'STUDENT', '学业预警通知: 成绩骤降', '学生56(20250056)在课程中触发黄色级成绩骤降预警，综合风险分15.3。成绩出现明显下降趋势，综合风险分15.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (811, 1, 'TEACHER', '新预警: 学生57(20250057)触发黄色成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (812, 57, 'STUDENT', '学业预警通知: 成绩骤降', '学生57(20250057)在课程中触发黄色级成绩骤降预警，综合风险分21.3。成绩出现明显下降趋势，综合风险分21.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (813, 1, 'TEACHER', '新预警: 学生65(20250065)触发黄色成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (814, 65, 'STUDENT', '学业预警通知: 成绩骤降', '学生65(20250065)在课程中触发黄色级成绩骤降预警，综合风险分18.1。成绩出现明显下降趋势，综合风险分18.1。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (815, 1, 'TEACHER', '新预警: 学生68(20250068)触发黄色成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (816, 68, 'STUDENT', '学业预警通知: 成绩骤降', '学生68(20250068)在课程中触发黄色级成绩骤降预警，综合风险分17.3。成绩出现明显下降趋势，综合风险分17.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (817, 1, 'TEACHER', '新预警: 学生70(20250070)触发黄色成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (818, 70, 'STUDENT', '学业预警通知: 成绩骤降', '学生70(20250070)在课程中触发黄色级成绩骤降预警，综合风险分15.6。成绩出现明显下降趋势，综合风险分15.6。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (819, 1, 'TEACHER', '新预警: 学生71(20250071)触发橙色挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (820, 71, 'STUDENT', '学业预警通知: 挂科风险', '学生71(20250071)在课程中触发橙色级挂科风险预警，综合风险分31.0。预测期末成绩58.0分，存在挂科风险。综合风险分31.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (821, 1, 'TEACHER', '新预警: 学生72(20250072)触发黄色挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (822, 72, 'STUDENT', '学业预警通知: 挂科风险', '学生72(20250072)在课程中触发黄色级挂科风险预警，综合风险分26.0。预测期末成绩48.0分，存在挂科风险。综合风险分26.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (823, 1, 'TEACHER', '新预警: 学生74(20250074)触发黄色挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (824, 74, 'STUDENT', '学业预警通知: 挂科风险', '学生74(20250074)在课程中触发黄色级挂科风险预警，综合风险分21.1。预测期末成绩49.0分，存在挂科风险。综合风险分21.1，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (825, 1, 'TEACHER', '新预警: 学生76(20250076)触发橙色作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (826, 76, 'STUDENT', '学业预警通知: 作业欠交', '学生76(20250076)在课程中触发橙色级作业欠交预警，综合风险分32.1。作业提交率较低，综合风险分32.1。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (827, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (828, 78, 'STUDENT', '学业预警通知: 挂科风险', '学生78(20250078)在课程中触发黄色级挂科风险预警，综合风险分25.3。预测期末成绩58.0分，存在挂科风险。综合风险分25.3，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (829, 1, 'TEACHER', '新预警: 学生78(20250078)触发黄色成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (830, 78, 'STUDENT', '学业预警通知: 成绩骤降', '学生78(20250078)在课程中触发黄色级成绩骤降预警，综合风险分25.3。成绩出现明显下降趋势，综合风险分25.3。建议分析原因并给予关注。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (831, 1, 'TEACHER', '新预警: 学生84(20250084)触发黄色挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (832, 84, 'STUDENT', '学业预警通知: 挂科风险', '学生84(20250084)在课程中触发黄色级挂科风险预警，综合风险分27.0。预测期末成绩58.0分，存在挂科风险。综合风险分27.0，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (833, 1, 'TEACHER', '新预警: 学生85(20250085)触发黄色挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (834, 85, 'STUDENT', '学业预警通知: 挂科风险', '学生85(20250085)在课程中触发黄色级挂科风险预警，综合风险分25.5。预测期末成绩53.0分，存在挂科风险。综合风险分25.5，建议加强复习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (835, 1, 'TEACHER', '新预警: 学生88(20250088)触发黄色知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (836, 88, 'STUDENT', '学业预警通知: 知识点断层', '学生88(20250088)在课程中触发黄色级知识点断层预警，综合风险分29.1。知识点掌握率偏低，综合风险分29.1。建议针对性练习和辅导。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (837, 1, 'TEACHER', '新预警: 学生89(20250089)触发黄色作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 1, '2026-07-08 21:40:06', 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');
INSERT INTO `notification` VALUES (838, 89, 'STUDENT', '学业预警通知: 作业欠交', '学生89(20250089)在课程中触发黄色级作业欠交预警，综合风险分18.4。作业提交率较低，综合风险分18.4。建议督促完成作业并了解原因。', NULL, 'ALERT_NEW', 'IN_APP', 0, NULL, 'SENT', '2026-07-08 21:29:30', '2026-07-08 21:29:30');

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
) ENGINE = InnoDB AUTO_INCREMENT = 534 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学业成绩表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of score_info
-- ----------------------------
INSERT INTO `score_info` VALUES (1, 1, 1, 45, 76, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (2, 2, 1, 87, 88, 56, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (3, 3, 1, 77, 54, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (4, 4, 1, 89, 78, 63, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (5, 5, 1, 66, 82, 54, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (6, 6, 1, 70, 87, 89, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (7, 7, 1, 70, 81, 75, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (8, 8, 1, 47, 67, 92, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (9, 9, 1, 70, 65, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (10, 10, 1, 44, 84, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (11, 11, 1, 56, 80, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (12, 12, 1, 86, 80, 57, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (13, 13, 1, 95, 31, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (14, 14, 1, 61, 88, 37, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (15, 15, 1, 66, 74, 93, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (16, 16, 1, 64, 100, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (17, 17, 1, 86, 72, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (18, 18, 1, 66, 55, 64, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (19, 19, 1, 54, 74, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (20, 20, 1, 80, 54, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (21, 21, 1, 80, 100, 41, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (22, 22, 1, 79, 66, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (23, 23, 1, 96, 68, 67, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (24, 24, 1, 55, 65, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (25, 25, 1, 78, 73, 67, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (26, 26, 1, 47, 98, 86, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (27, 27, 1, 100, 64, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (28, 28, 1, 70, 75, 55, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (29, 29, 1, 73, 69, 35, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (30, 30, 1, 93, 81, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (31, 31, 1, 79, 62, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (32, 32, 1, 88, 68, 56, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (33, 33, 1, 58, 83, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (34, 34, 1, 96, 64, 67, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (35, 35, 1, 68, 76, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (36, 36, 1, 88, 100, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (37, 37, 1, 81, 83, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (38, 38, 1, 84, 72, 94, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (39, 39, 1, 29, 83, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (40, 40, 1, 62, 83, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (41, 41, 1, 61, 68, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (42, 42, 1, 79, 63, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (43, 43, 1, 66, 66, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (44, 44, 1, 75, 71, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (45, 45, 1, 69, 73, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (46, 46, 1, 87, 79, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (47, 47, 1, 83, 71, 86, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (48, 48, 1, 46, 77, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (49, 49, 1, 100, 56, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (50, 50, 1, 83, 73, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (51, 51, 1, 57, 57, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (52, 52, 1, 81, 72, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (53, 53, 1, 54, 57, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (54, 54, 1, 61, 73, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (55, 55, 1, 88, 100, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (56, 56, 1, 99, 48, 75, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (57, 57, 1, 92, 75, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (58, 58, 1, 65, 76, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (59, 59, 1, 77, 85, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (60, 60, 1, 82, 84, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (61, 61, 1, 54, 94, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (62, 62, 1, 76, 63, 66, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (63, 63, 1, 43, 40, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (64, 64, 1, 76, 97, 75, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (65, 65, 1, 100, 78, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (66, 66, 1, 68, 58, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (67, 67, 1, 65, 63, 63, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (68, 68, 1, 79, 58, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (69, 69, 1, 79, 80, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (70, 70, 1, 65, 48, 97, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (71, 71, 1, 56, 92, 58, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (72, 72, 1, 65, 64, 48, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (73, 73, 1, 94, 45, 99, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (74, 74, 1, 95, 88, 49, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (75, 75, 1, 64, 89, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (76, 76, 1, 57, 100, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (77, 77, 1, 87, 84, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (78, 78, 1, 80, 54, 58, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (79, 79, 1, 100, 82, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (80, 80, 1, 58, 87, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (81, 81, 1, 65, 84, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (82, 82, 1, 75, 100, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (83, 83, 1, 61, 77, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (84, 84, 1, 44, 85, 58, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (85, 85, 1, 78, 71, 53, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (86, 86, 1, 93, 74, 89, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (87, 87, 1, 77, 100, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (88, 88, 1, 55, 72, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (89, 89, 1, 80, 91, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (90, 90, 1, 79, 70, 97, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (91, 91, 1, 99, 79, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (92, 92, 1, 48, 85, 56, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (93, 93, 1, 76, 61, 64, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (94, 94, 1, 95, 52, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (95, 95, 1, 74, 67, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (96, 96, 1, 64, 78, 98, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (97, 97, 1, 89, 73, 63, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (98, 98, 1, 61, 41, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (99, 99, 1, 75, 73, 92, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (100, 100, 1, 55, 41, 94, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (101, 101, 1, 85, 92, 64, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (102, 102, 1, 78, 84, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (103, 103, 1, 84, 66, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (104, 104, 1, 57, 73, 49, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (105, 105, 1, 64, 53, 54, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (106, 106, 1, 61, 80, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (107, 107, 1, 54, 86, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (108, 108, 1, 84, 80, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (109, 109, 1, 89, 97, 75, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (110, 110, 1, 89, 96, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (111, 111, 1, 80, 70, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (112, 112, 1, 80, 100, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (113, 113, 1, 64, 80, 57, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (114, 114, 1, 91, 94, 92, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (115, 115, 1, 80, 62, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (116, 116, 1, 64, 84, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (117, 117, 1, 86, 78, 50, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (118, 118, 1, 55, 82, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (119, 119, 1, 84, 47, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (120, 120, 1, 52, 99, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (121, 121, 1, 99, 62, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (122, 122, 1, 81, 71, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (123, 123, 1, 87, 72, 56, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (124, 124, 1, 52, 56, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (125, 125, 1, 72, 98, 49, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (126, 126, 1, 71, 71, 64, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (127, 127, 1, 79, 67, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (128, 128, 1, 90, 69, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (129, 129, 1, 59, 66, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (130, 130, 1, 66, 89, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (131, 131, 1, 63, 81, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (132, 132, 1, 67, 93, 89, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (133, 133, 1, 70, 72, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (134, 134, 1, 43, 58, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (135, 135, 1, 84, 66, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (136, 136, 1, 76, 54, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (137, 137, 1, 81, 93, 48, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (138, 138, 1, 71, 92, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (139, 139, 1, 81, 65, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (140, 140, 1, 66, 62, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (141, 141, 1, 86, 53, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (142, 142, 1, 91, 57, 88, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (143, 143, 1, 94, 81, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (144, 144, 1, 67, 99, 57, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (145, 145, 1, 83, 86, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (146, 146, 1, 69, 65, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (147, 147, 1, 90, 100, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (148, 148, 1, 73, 79, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (149, 149, 1, 90, 80, 51, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (150, 150, 1, 97, 56, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (151, 151, 1, 85, 77, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (152, 152, 1, 96, 81, 47, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (153, 153, 1, 100, 100, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (154, 154, 1, 58, 68, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (155, 155, 1, 61, 75, 54, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (156, 156, 1, 73, 72, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (157, 157, 1, 64, 56, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (158, 158, 1, 70, 100, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (159, 159, 1, 72, 89, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (160, 160, 1, 82, 57, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (161, 161, 1, 62, 82, 88, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (162, 162, 1, 86, 76, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (163, 163, 1, 89, 100, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (164, 164, 1, 61, 77, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (165, 165, 1, 76, 57, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (166, 166, 1, 100, 81, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (167, 167, 1, 66, 67, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (168, 168, 1, 85, 89, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (169, 169, 1, 80, 54, 63, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (170, 170, 1, 37, 75, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (171, 171, 1, 69, 61, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (172, 172, 1, 36, 71, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (173, 173, 1, 73, 97, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (174, 174, 1, 98, 55, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (175, 175, 1, 97, 65, 93, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (176, 176, 1, 83, 71, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (177, 177, 1, 67, 81, 44, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (178, 178, 1, 80, 71, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (179, 179, 1, 91, 73, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (180, 180, 1, 79, 92, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (181, 181, 1, 49, 77, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (182, 182, 1, 86, 90, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (183, 183, 1, 84, 51, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (184, 184, 1, 70, 65, 97, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (185, 185, 1, 86, 63, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (186, 186, 1, 79, 60, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (187, 187, 1, 54, 78, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (188, 188, 1, 60, 70, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (189, 189, 1, 65, 91, 55, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (190, 190, 1, 77, 57, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (191, 191, 1, 72, 81, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (192, 192, 1, 53, 75, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (193, 193, 1, 83, 72, 88, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (194, 194, 1, 80, 83, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (195, 195, 1, 81, 66, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (196, 196, 1, 68, 76, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (197, 197, 1, 78, 87, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (198, 198, 1, 53, 66, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (199, 199, 1, 73, 93, 88, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (200, 200, 1, 64, 70, 71, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (201, 201, 1, 82, 100, 42, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (202, 202, 1, 49, 50, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (203, 203, 1, 90, 93, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (204, 204, 1, 53, 84, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (205, 205, 1, 59, 76, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (206, 206, 1, 71, 89, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (207, 207, 1, 61, 100, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (208, 208, 1, 63, 80, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (209, 209, 1, 89, 55, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (210, 210, 1, 88, 76, 60, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (211, 211, 1, 79, 99, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (212, 212, 1, 62, 87, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (213, 213, 1, 76, 65, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (214, 214, 1, 80, 100, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (215, 215, 1, 80, 63, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (216, 216, 1, 97, 83, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (217, 217, 1, 90, 100, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (218, 218, 1, 79, 67, 92, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (219, 219, 1, 56, 55, 98, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (220, 220, 1, 66, 86, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (221, 221, 1, 74, 76, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (222, 222, 1, 86, 74, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (223, 223, 1, 99, 90, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (224, 224, 1, 72, 96, 58, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (225, 225, 1, 77, 47, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (226, 226, 1, 100, 72, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (227, 227, 1, 58, 79, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (228, 228, 1, 88, 87, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (229, 229, 1, 56, 88, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:22');
INSERT INTO `score_info` VALUES (230, 230, 1, 83, 86, 52, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (231, 231, 1, 86, 78, 54, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (232, 232, 1, 91, 91, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (233, 233, 1, 86, 61, 60, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (234, 234, 1, 68, 58, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (235, 235, 1, 62, 73, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (236, 236, 1, 73, 60, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (237, 237, 1, 90, 94, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (238, 238, 1, 78, 75, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (239, 239, 1, 62, 81, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (240, 240, 1, 80, 71, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (241, 241, 1, 81, 100, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (242, 242, 1, 62, 82, 66, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (243, 243, 1, 87, 83, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (244, 244, 1, 75, 84, 75, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (245, 245, 1, 70, 87, 67, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (246, 246, 1, 81, 74, 64, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (247, 247, 1, 43, 70, 55, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (248, 248, 1, 88, 59, 54, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (249, 249, 1, 70, 55, 66, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (250, 250, 1, 82, 68, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (251, 251, 1, 97, 62, 88, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (252, 252, 1, 94, 83, 97, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (253, 253, 1, 82, 86, 92, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (254, 254, 1, 84, 64, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (255, 255, 1, 72, 65, 64, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (256, 256, 1, 45, 77, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (257, 257, 1, 88, 62, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (258, 258, 1, 85, 76, 47, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (259, 259, 1, 100, 77, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (260, 260, 1, 73, 90, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (261, 261, 1, 94, 73, 88, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (262, 262, 1, 70, 59, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (263, 263, 1, 89, 52, 93, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (264, 264, 1, 80, 68, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (265, 265, 1, 85, 61, 43, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (266, 266, 1, 57, 65, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (267, 267, 1, 92, 78, 45, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (268, 268, 2, 88, 66, 75, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (269, 269, 2, 100, 46, 75, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (270, 270, 2, 77, 74, 82, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (271, 271, 2, 49, 79, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (272, 272, 2, 73, 86, 73, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (273, 273, 2, 89, 64, 84, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (274, 274, 2, 77, 69, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (275, 275, 2, 68, 73, 74, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (276, 276, 2, 62, 60, 71, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (277, 277, 2, 85, 93, 44, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (278, 278, 2, 77, 66, 85, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (279, 279, 2, 69, 94, 95, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (280, 280, 2, 86, 72, 52, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (281, 281, 2, 100, 60, 47, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (282, 282, 2, 75, 73, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (283, 283, 2, 24, 59, 97, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (284, 284, 2, 94, 64, 64, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (285, 285, 2, 72, 57, 95, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (286, 286, 2, 54, 59, 89, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (287, 287, 2, 52, 98, 89, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (288, 288, 2, 79, 100, 95, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (289, 289, 2, 93, 94, 90, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (290, 290, 2, 88, 78, 69, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (291, 291, 2, 73, 70, 73, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (292, 292, 2, 85, 88, 65, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (293, 293, 2, 56, 56, 93, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (294, 294, 2, 58, 92, 78, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (295, 295, 2, 55, 79, 95, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (296, 296, 2, 75, 59, 43, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (297, 297, 2, 70, 83, 61, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (298, 298, 2, 72, 77, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (299, 299, 2, 100, 61, 93, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (300, 300, 2, 64, 86, 88, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (301, 301, 2, 60, 65, 69, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (302, 302, 2, 56, 77, 70, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (303, 303, 2, 80, 99, 70, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (304, 304, 2, 78, 71, 59, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (305, 305, 2, 82, 45, 83, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (306, 306, 2, 93, 90, 49, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (307, 307, 2, 37, 78, 84, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (308, 308, 2, 79, 62, 83, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (309, 309, 2, 78, 75, 65, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (310, 310, 2, 100, 83, 83, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (311, 311, 2, 59, 75, 60, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (312, 312, 2, 100, 85, 76, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (313, 313, 2, 76, 86, 65, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (314, 314, 2, 53, 73, 51, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (315, 315, 2, 61, 91, 83, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (316, 316, 2, 65, 72, 69, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (317, 317, 2, 59, 63, 49, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (318, 318, 2, 70, 90, 85, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (319, 319, 2, 63, 70, 74, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (320, 320, 2, 85, 72, 89, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (321, 321, 2, 91, 61, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (322, 322, 2, 65, 100, 81, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (323, 323, 2, 57, 71, 100, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (324, 324, 2, 65, 67, 70, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (325, 325, 2, 79, 62, 84, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (326, 326, 2, 84, 62, 72, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (327, 327, 2, 73, 98, 83, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (328, 328, 2, 57, 77, 62, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (329, 329, 2, 75, 52, 100, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (330, 330, 2, 100, 57, 81, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (331, 331, 2, 67, 90, 91, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (332, 332, 2, 65, 92, 59, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (333, 333, 2, 73, 68, 75, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (334, 334, 2, 69, 48, 67, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (335, 335, 2, 55, 78, 66, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (336, 336, 2, 72, 60, 85, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (337, 337, 2, 77, 83, 82, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (338, 338, 2, 58, 76, 65, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (339, 339, 2, 92, 71, 70, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (340, 340, 2, 95, 65, 52, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (341, 341, 2, 79, 71, 81, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (342, 342, 2, 69, 73, 75, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (343, 343, 2, 88, 80, 81, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (344, 344, 2, 78, 99, 86, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (345, 345, 2, 63, 90, 70, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (346, 346, 2, 100, 43, 63, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (347, 347, 2, 74, 76, 89, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (348, 348, 2, 77, 85, 94, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (349, 349, 2, 82, 85, 97, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (350, 350, 2, 63, 84, 70, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (351, 351, 2, 96, 70, 78, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (352, 352, 2, 96, 100, 91, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (353, 353, 2, 72, 84, 78, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (354, 354, 2, 74, 92, 90, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (355, 355, 2, 47, 100, 93, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (356, 356, 2, 48, 45, 75, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (357, 357, 2, 83, 67, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (358, 358, 2, 76, 75, 83, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (359, 359, 2, 74, 71, 77, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (360, 360, 2, 67, 57, 84, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (361, 361, 2, 71, 62, 73, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (362, 362, 2, 95, 90, 84, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (363, 363, 2, 100, 77, 93, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (364, 364, 2, 84, 55, 55, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (365, 365, 2, 83, 77, 96, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (366, 366, 2, 84, 71, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (367, 367, 2, 91, 59, 95, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (368, 368, 2, 65, 51, 80, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (369, 369, 2, 63, 63, 74, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (370, 370, 2, 61, 67, 61, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (371, 371, 2, 88, 94, 73, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (372, 372, 2, 67, 100, 100, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (373, 373, 2, 65, 87, 78, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (374, 374, 2, 68, 51, 66, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (375, 375, 2, 84, 70, 71, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (376, 376, 2, 70, 65, 73, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (377, 377, 2, 65, 100, 74, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (378, 378, 2, 93, 79, 65, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (379, 379, 2, 46, 84, 74, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (380, 380, 2, 82, 65, 100, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (381, 381, 2, 58, 74, 86, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (382, 382, 2, 82, 84, 92, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (383, 383, 2, 82, 82, 88, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (384, 384, 2, 79, 58, 100, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (385, 385, 2, 64, 86, 75, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (386, 386, 2, 85, 88, 71, NULL, '2026-05-16 17:00:07', '2026-05-21 08:12:10');
INSERT INTO `score_info` VALUES (387, 387, 1, 91, 84, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (388, 388, 1, 67, 70, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (389, 389, 1, 52, 88, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (390, 390, 1, 89, 46, 94, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (391, 391, 1, 85, 59, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (392, 392, 1, 69, 73, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (393, 393, 1, 66, 62, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (394, 394, 1, 57, 54, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (395, 395, 1, 69, 80, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (396, 396, 1, 71, 92, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (397, 397, 1, 91, 57, 44, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (398, 398, 1, 67, 67, 57, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (399, 399, 1, 70, 82, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (400, 400, 1, 88, 80, 53, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (401, 401, 1, 87, 50, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (402, 402, 1, 65, 69, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (403, 403, 1, 49, 70, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (404, 404, 1, 64, 78, 82, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (405, 405, 1, 96, 82, 52, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (406, 406, 1, 95, 80, 55, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (407, 407, 1, 72, 56, 54, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (408, 408, 1, 93, 58, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (409, 409, 1, 62, 81, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (410, 410, 1, 72, 82, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (411, 411, 1, 82, 59, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (412, 412, 1, 47, 62, 51, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (413, 413, 1, 78, 80, 89, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (414, 414, 1, 57, 84, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (415, 415, 1, 92, 52, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (416, 416, 1, 94, 72, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (417, 417, 1, 53, 74, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (418, 418, 1, 65, 70, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (419, 419, 1, 97, 90, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (420, 420, 1, 77, 83, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (421, 421, 1, 78, 21, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (422, 422, 1, 44, 53, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (423, 423, 1, 96, 87, 52, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (424, 424, 1, 65, 95, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (425, 425, 1, 67, 62, 94, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (426, 426, 1, 87, 76, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (427, 427, 1, 77, 62, 92, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (428, 428, 1, 91, 59, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (429, 429, 1, 100, 97, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (430, 430, 1, 71, 76, 90, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (431, 431, 1, 73, 81, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (432, 432, 1, 68, 69, 57, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (433, 433, 1, 89, 81, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (434, 434, 1, 73, 90, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (435, 435, 1, 82, 81, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (436, 436, 1, 52, 100, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (437, 437, 1, 100, 91, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (438, 438, 1, 45, 67, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (439, 439, 1, 97, 66, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (440, 440, 1, 86, 78, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (441, 441, 1, 85, 89, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (442, 442, 1, 81, 80, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (443, 443, 1, 79, 100, 49, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (444, 444, 1, 100, 49, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (445, 445, 1, 95, 74, 69, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (446, 446, 1, 73, 79, 86, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (447, 447, 1, 67, 47, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (448, 448, 1, 92, 94, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (449, 449, 1, 74, 65, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (450, 450, 1, 100, 75, 99, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (451, 451, 1, 59, 73, 48, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (452, 452, 1, 47, 87, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (453, 453, 1, 61, 85, 94, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (454, 454, 1, 73, 49, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (455, 455, 1, 99, 67, 63, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (456, 456, 1, 65, 63, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (457, 457, 1, 63, 76, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (458, 458, 1, 90, 70, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (459, 459, 1, 68, 91, 60, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (460, 460, 1, 84, 100, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (461, 461, 1, 92, 76, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (462, 462, 1, 63, 73, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (463, 463, 1, 94, 82, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (464, 464, 1, 62, 80, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (465, 465, 1, 79, 79, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (466, 466, 1, 68, 77, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (467, 467, 1, 65, 83, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (468, 468, 1, 70, 76, 83, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (469, 469, 1, 43, 76, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (470, 470, 1, 74, 85, 60, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (471, 471, 1, 55, 85, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (472, 472, 1, 77, 76, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (473, 473, 1, 75, 60, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (474, 474, 1, 93, 66, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (475, 475, 1, 96, 90, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (476, 476, 1, 94, 100, 41, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (477, 477, 1, 90, 78, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (478, 478, 1, 84, 75, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (479, 479, 1, 76, 82, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (480, 480, 1, 100, 49, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (481, 481, 1, 59, 60, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (482, 482, 1, 80, 84, 89, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (483, 483, 1, 75, 84, 51, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (484, 484, 1, 88, 77, 67, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (485, 485, 1, 88, 66, 56, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (486, 486, 1, 89, 65, 56, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (487, 487, 1, 68, 79, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (488, 488, 1, 80, 58, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (489, 489, 1, 33, 75, 95, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (490, 490, 1, 73, 75, 94, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (491, 491, 1, 92, 90, 85, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (492, 492, 1, 78, 81, 79, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (493, 493, 1, 80, 85, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (494, 494, 1, 100, 100, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (495, 495, 1, 60, 53, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (496, 496, 1, 100, 100, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (497, 497, 1, 71, 89, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (498, 498, 1, 72, 72, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (499, 499, 1, 80, 89, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (500, 500, 1, 67, 62, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (501, 501, 1, 78, 39, 61, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (502, 502, 1, 83, 50, 78, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (503, 503, 1, 77, 67, 40, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (504, 504, 1, 79, 67, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (505, 505, 1, 65, 83, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (506, 506, 1, 73, 54, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (507, 507, 1, 69, 95, 81, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (508, 508, 1, 73, 72, 62, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (509, 509, 1, 81, 87, 68, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (510, 510, 1, 93, 60, 87, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (511, 511, 1, 72, 83, 96, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (512, 512, 1, 67, 54, 44, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (513, 513, 1, 63, 93, 70, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (514, 514, 1, 90, 100, 72, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (515, 515, 1, 82, 91, 67, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (516, 516, 1, 74, 85, 49, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (517, 517, 1, 77, 77, 100, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (518, 518, 1, 89, 75, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (519, 519, 1, 98, 100, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (520, 520, 1, 55, 53, 74, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (521, 521, 1, 81, 84, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (522, 522, 1, 61, 65, 43, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (523, 523, 1, 82, 70, 99, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (524, 524, 1, 59, 71, 73, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (525, 525, 1, 64, 69, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (526, 526, 1, 64, 77, 84, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (527, 527, 1, 99, 86, 59, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (528, 528, 1, 100, 97, 80, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (529, 529, 1, 86, 71, 51, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (530, 530, 1, 94, 91, 91, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (531, 531, 1, 74, 64, 77, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (532, 532, 1, 68, 80, 76, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (533, 533, 1, 66, 46, 65, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');
INSERT INTO `score_info` VALUES (534, 534, 1, 87, 57, 63, NULL, '2026-05-16 17:00:07', '2026-05-18 20:35:23');

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
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 800 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生基础信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, '20250001', '学生1', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (2, '20250002', '学生2', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (3, '20250003', '学生3', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (4, '20250004', '学生4', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (5, '20250005', '学生5', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (6, '20250006', '学生6', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (7, '20250007', '学生7', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (8, '20250008', '学生8', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (9, '20250009', '学生9', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (10, '20250010', '学生10', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (11, '20250011', '学生11', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (12, '20250012', '学生12', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (13, '20250013', '学生13', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (14, '20250014', '学生14', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (15, '20250015', '学生15', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (16, '20250016', '学生16', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (17, '20250017', '学生17', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (18, '20250018', '学生18', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (19, '20250019', '学生19', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (20, '20250020', '学生20', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (21, '20250021', '学生21', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (22, '20250022', '学生22', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (23, '20250023', '学生23', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (24, '20250024', '学生24', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (25, '20250025', '学生25', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (26, '20250026', '学生26', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (27, '20250027', '学生27', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (28, '20250028', '学生28', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (29, '20250029', '学生29', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (30, '20250030', '学生30', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (31, '20250031', '学生31', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (32, '20250032', '学生32', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (33, '20250033', '学生33', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (34, '20250034', '学生34', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (35, '20250035', '学生35', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (36, '20250036', '学生36', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (37, '20250037', '学生37', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (38, '20250038', '学生38', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (39, '20250039', '学生39', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (40, '20250040', '学生40', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (41, '20250041', '学生41', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (42, '20250042', '学生42', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (43, '20250043', '学生43', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (44, '20250044', '学生44', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (45, '20250045', '学生45', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (46, '20250046', '学生46', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (47, '20250047', '学生47', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (48, '20250048', '学生48', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (49, '20250049', '学生49', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (50, '20250050', '学生50', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (51, '20250051', '学生51', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (52, '20250052', '学生52', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (53, '20250053', '学生53', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (54, '20250054', '学生54', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (55, '20250055', '学生55', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (56, '20250056', '学生56', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (57, '20250057', '学生57', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (58, '20250058', '学生58', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (59, '20250059', '学生59', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (60, '20250060', '学生60', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (61, '20250061', '学生61', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (62, '20250062', '学生62', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (63, '20250063', '学生63', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (64, '20250064', '学生64', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (65, '20250065', '学生65', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (66, '20250066', '学生66', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (67, '20250067', '学生67', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (68, '20250068', '学生68', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (69, '20250069', '学生69', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (70, '20250070', '学生70', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (71, '20250071', '学生71', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (72, '20250072', '学生72', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (73, '20250073', '学生73', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (74, '20250074', '学生74', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (75, '20250075', '学生75', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (76, '20250076', '学生76', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (77, '20250077', '学生77', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (78, '20250078', '学生78', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (79, '20250079', '学生79', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (80, '20250080', '学生80', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (81, '20250081', '学生81', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (82, '20250082', '学生82', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (83, '20250083', '学生83', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (84, '20250084', '学生84', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (85, '20250085', '学生85', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (86, '20250086', '学生86', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (87, '20250087', '学生87', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (88, '20250088', '学生88', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (89, '20250089', '学生89', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (90, '20250090', '学生90', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (91, '20250091', '学生91', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (92, '20250092', '学生92', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (93, '20250093', '学生93', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (94, '20250094', '学生94', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (95, '20250095', '学生95', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (96, '20250096', '学生96', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (97, '20250097', '学生97', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (98, '20250098', '学生98', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (99, '20250099', '学生99', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (100, '20250100', '学生100', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (101, '20250101', '学生101', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (102, '20250102', '学生102', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (103, '20250103', '学生103', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (104, '20250104', '学生104', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (105, '20250105', '学生105', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (106, '20250106', '学生106', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (107, '20250107', '学生107', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (108, '20250108', '学生108', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (109, '20250109', '学生109', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (110, '20250110', '学生110', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (111, '20250111', '学生111', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (112, '20250112', '学生112', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (113, '20250113', '学生113', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (114, '20250114', '学生114', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (115, '20250115', '学生115', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (116, '20250116', '学生116', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (117, '20250117', '学生117', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (118, '20250118', '学生118', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (119, '20250119', '学生119', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (120, '20250120', '学生120', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (121, '20250121', '学生121', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (122, '20250122', '学生122', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (123, '20250123', '学生123', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (124, '20250124', '学生124', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (125, '20250125', '学生125', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (126, '20250126', '学生126', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (127, '20250127', '学生127', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (128, '20250128', '学生128', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (129, '20250129', '学生129', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (130, '20250130', '学生130', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (131, '20250131', '学生131', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (132, '20250132', '学生132', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (133, '20250133', '学生133', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (134, '20250134', '学生134', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (135, '20250135', '学生135', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (136, '20250136', '学生136', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (137, '20250137', '学生137', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (138, '20250138', '学生138', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (139, '20250139', '学生139', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (140, '20250140', '学生140', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (141, '20250141', '学生141', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (142, '20250142', '学生142', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (143, '20250143', '学生143', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (144, '20250144', '学生144', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (145, '20250145', '学生145', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (146, '20250146', '学生146', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (147, '20250147', '学生147', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (148, '20250148', '学生148', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (149, '20250149', '学生149', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (150, '20250150', '学生150', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (151, '20250151', '学生151', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (152, '20250152', '学生152', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (153, '20250153', '学生153', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (154, '20250154', '学生154', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (155, '20250155', '学生155', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (156, '20250156', '学生156', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (157, '20250157', '学生157', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (158, '20250158', '学生158', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (159, '20250159', '学生159', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (160, '20250160', '学生160', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (161, '20250161', '学生161', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (162, '20250162', '学生162', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (163, '20250163', '学生163', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (164, '20250164', '学生164', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (165, '20250165', '学生165', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (166, '20250166', '学生166', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (167, '20250167', '学生167', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (168, '20250168', '学生168', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (169, '20250169', '学生169', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (170, '20250170', '学生170', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (171, '20250171', '学生171', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (172, '20250172', '学生172', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (173, '20250173', '学生173', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (174, '20250174', '学生174', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (175, '20250175', '学生175', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (176, '20250176', '学生176', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (177, '20250177', '学生177', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (178, '20250178', '学生178', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (179, '20250179', '学生179', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (180, '20250180', '学生180', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (181, '20250181', '学生181', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (182, '20250182', '学生182', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (183, '20250183', '学生183', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (184, '20250184', '学生184', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (185, '20250185', '学生185', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (186, '20250186', '学生186', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (187, '20250187', '学生187', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (188, '20250188', '学生188', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (189, '20250189', '学生189', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (190, '20250190', '学生190', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (191, '20250191', '学生191', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (192, '20250192', '学生192', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (193, '20250193', '学生193', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (194, '20250194', '学生194', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (195, '20250195', '学生195', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (196, '20250196', '学生196', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (197, '20250197', '学生197', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (198, '20250198', '学生198', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (199, '20250199', '学生199', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (200, '20250200', '学生200', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (201, '20250201', '学生201', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (202, '20250202', '学生202', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (203, '20250203', '学生203', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (204, '20250204', '学生204', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (205, '20250205', '学生205', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (206, '20250206', '学生206', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (207, '20250207', '学生207', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (208, '20250208', '学生208', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (209, '20250209', '学生209', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (210, '20250210', '学生210', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (211, '20250211', '学生211', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (212, '20250212', '学生212', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (213, '20250213', '学生213', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (214, '20250214', '学生214', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (215, '20250215', '学生215', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (216, '20250216', '学生216', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (217, '20250217', '学生217', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (218, '20250218', '学生218', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (219, '20250219', '学生219', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (220, '20250220', '学生220', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (221, '20250221', '学生221', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (222, '20250222', '学生222', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (223, '20250223', '学生223', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (224, '20250224', '学生224', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (225, '20250225', '学生225', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (226, '20250226', '学生226', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (227, '20250227', '学生227', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (228, '20250228', '学生228', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (229, '20250229', '学生229', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (230, '20250230', '学生230', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (231, '20250231', '学生231', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (232, '20250232', '学生232', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (233, '20250233', '学生233', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (234, '20250234', '学生234', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (235, '20250235', '学生235', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (236, '20250236', '学生236', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (237, '20250237', '学生237', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (238, '20250238', '学生238', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (239, '20250239', '学生239', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (240, '20250240', '学生240', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (241, '20250241', '学生241', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (242, '20250242', '学生242', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (243, '20250243', '学生243', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (244, '20250244', '学生244', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (245, '20250245', '学生245', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (246, '20250246', '学生246', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (247, '20250247', '学生247', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (248, '20250248', '学生248', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (249, '20250249', '学生249', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (250, '20250250', '学生250', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (251, '20250251', '学生251', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (252, '20250252', '学生252', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (253, '20250253', '学生253', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (254, '20250254', '学生254', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (255, '20250255', '学生255', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (256, '20250256', '学生256', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (257, '20250257', '学生257', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (258, '20250258', '学生258', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (259, '20250259', '学生259', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (260, '20250260', '学生260', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (261, '20250261', '学生261', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (262, '20250262', '学生262', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (263, '20250263', '学生263', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (264, '20250264', '学生264', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (265, '20250265', '学生265', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (266, '20250266', '学生266', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (267, '20250267', '学生267', '大一上', '计算机1班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (268, '20250268', '学生268', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (269, '20250269', '学生269', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (270, '20250270', '学生270', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (271, '20250271', '学生271', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (272, '20250272', '学生272', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (273, '20250273', '学生273', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (274, '20250274', '学生274', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (275, '20250275', '学生275', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (276, '20250276', '学生276', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (277, '20250277', '学生277', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (278, '20250278', '学生278', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (279, '20250279', '学生279', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (280, '20250280', '学生280', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (281, '20250281', '学生281', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (282, '20250282', '学生282', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (283, '20250283', '学生283', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (284, '20250284', '学生284', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (285, '20250285', '学生285', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (286, '20250286', '学生286', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (287, '20250287', '学生287', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (288, '20250288', '学生288', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (289, '20250289', '学生289', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (290, '20250290', '学生290', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (291, '20250291', '学生291', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (292, '20250292', '学生292', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (293, '20250293', '学生293', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (294, '20250294', '学生294', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (295, '20250295', '学生295', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (296, '20250296', '学生296', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (297, '20250297', '学生297', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (298, '20250298', '学生298', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (299, '20250299', '学生299', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (300, '20250300', '学生300', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (301, '20250301', '学生301', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (302, '20250302', '学生302', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (303, '20250303', '学生303', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (304, '20250304', '学生304', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (305, '20250305', '学生305', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (306, '20250306', '学生306', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (307, '20250307', '学生307', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (308, '20250308', '学生308', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (309, '20250309', '学生309', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (310, '20250310', '学生310', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (311, '20250311', '学生311', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (312, '20250312', '学生312', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (313, '20250313', '学生313', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (314, '20250314', '学生314', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (315, '20250315', '学生315', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (316, '20250316', '学生316', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (317, '20250317', '学生317', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (318, '20250318', '学生318', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (319, '20250319', '学生319', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (320, '20250320', '学生320', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (321, '20250321', '学生321', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (322, '20250322', '学生322', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (323, '20250323', '学生323', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (324, '20250324', '学生324', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (325, '20250325', '学生325', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (326, '20250326', '学生326', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (327, '20250327', '学生327', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (328, '20250328', '学生328', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (329, '20250329', '学生329', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (330, '20250330', '学生330', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (331, '20250331', '学生331', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (332, '20250332', '学生332', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (333, '20250333', '学生333', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (334, '20250334', '学生334', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (335, '20250335', '学生335', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (336, '20250336', '学生336', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (337, '20250337', '学生337', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (338, '20250338', '学生338', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (339, '20250339', '学生339', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (340, '20250340', '学生340', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (341, '20250341', '学生341', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (342, '20250342', '学生342', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (343, '20250343', '学生343', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (344, '20250344', '学生344', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (345, '20250345', '学生345', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (346, '20250346', '学生346', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (347, '20250347', '学生347', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (348, '20250348', '学生348', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (349, '20250349', '学生349', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (350, '20250350', '学生350', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (351, '20250351', '学生351', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (352, '20250352', '学生352', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (353, '20250353', '学生353', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (354, '20250354', '学生354', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (355, '20250355', '学生355', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (356, '20250356', '学生356', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (357, '20250357', '学生357', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (358, '20250358', '学生358', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (359, '20250359', '学生359', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (360, '20250360', '学生360', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (361, '20250361', '学生361', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (362, '20250362', '学生362', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (363, '20250363', '学生363', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (364, '20250364', '学生364', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (365, '20250365', '学生365', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (366, '20250366', '学生366', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (367, '20250367', '学生367', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (368, '20250368', '学生368', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (369, '20250369', '学生369', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (370, '20250370', '学生370', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (371, '20250371', '学生371', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (372, '20250372', '学生372', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (373, '20250373', '学生373', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (374, '20250374', '学生374', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (375, '20250375', '学生375', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (376, '20250376', '学生376', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (377, '20250377', '学生377', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (378, '20250378', '学生378', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (379, '20250379', '学生379', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (380, '20250380', '学生380', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (381, '20250381', '学生381', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (382, '20250382', '学生382', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (383, '20250383', '学生383', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (384, '20250384', '学生384', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (385, '20250385', '学生385', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (386, '20250386', '学生386', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (387, '20250387', '学生387', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (388, '20250388', '学生388', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (389, '20250389', '学生389', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (390, '20250390', '学生390', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (391, '20250391', '学生391', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (392, '20250392', '学生392', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (393, '20250393', '学生393', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (394, '20250394', '学生394', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (395, '20250395', '学生395', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (396, '20250396', '学生396', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (397, '20250397', '学生397', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (398, '20250398', '学生398', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (399, '20250399', '学生399', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (400, '20250400', '学生400', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (401, '20250401', '学生401', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (402, '20250402', '学生402', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (403, '20250403', '学生403', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (404, '20250404', '学生404', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (405, '20250405', '学生405', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (406, '20250406', '学生406', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (407, '20250407', '学生407', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (408, '20250408', '学生408', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (409, '20250409', '学生409', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (410, '20250410', '学生410', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (411, '20250411', '学生411', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (412, '20250412', '学生412', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (413, '20250413', '学生413', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (414, '20250414', '学生414', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (415, '20250415', '学生415', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (416, '20250416', '学生416', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (417, '20250417', '学生417', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (418, '20250418', '学生418', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (419, '20250419', '学生419', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (420, '20250420', '学生420', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (421, '20250421', '学生421', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (422, '20250422', '学生422', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (423, '20250423', '学生423', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (424, '20250424', '学生424', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (425, '20250425', '学生425', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (426, '20250426', '学生426', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (427, '20250427', '学生427', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (428, '20250428', '学生428', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (429, '20250429', '学生429', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (430, '20250430', '学生430', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (431, '20250431', '学生431', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (432, '20250432', '学生432', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (433, '20250433', '学生433', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (434, '20250434', '学生434', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (435, '20250435', '学生435', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (436, '20250436', '学生436', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (437, '20250437', '学生437', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (438, '20250438', '学生438', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (439, '20250439', '学生439', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (440, '20250440', '学生440', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (441, '20250441', '学生441', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (442, '20250442', '学生442', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (443, '20250443', '学生443', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (444, '20250444', '学生444', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (445, '20250445', '学生445', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (446, '20250446', '学生446', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (447, '20250447', '学生447', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (448, '20250448', '学生448', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (449, '20250449', '学生449', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (450, '20250450', '学生450', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (451, '20250451', '学生451', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (452, '20250452', '学生452', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (453, '20250453', '学生453', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (454, '20250454', '学生454', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (455, '20250455', '学生455', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (456, '20250456', '学生456', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (457, '20250457', '学生457', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (458, '20250458', '学生458', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (459, '20250459', '学生459', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (460, '20250460', '学生460', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (461, '20250461', '学生461', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (462, '20250462', '学生462', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (463, '20250463', '学生463', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (464, '20250464', '学生464', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (465, '20250465', '学生465', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (466, '20250466', '学生466', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (467, '20250467', '学生467', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (468, '20250468', '学生468', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (469, '20250469', '学生469', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (470, '20250470', '学生470', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (471, '20250471', '学生471', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (472, '20250472', '学生472', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (473, '20250473', '学生473', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (474, '20250474', '学生474', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (475, '20250475', '学生475', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (476, '20250476', '学生476', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (477, '20250477', '学生477', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (478, '20250478', '学生478', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (479, '20250479', '学生479', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (480, '20250480', '学生480', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (481, '20250481', '学生481', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (482, '20250482', '学生482', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (483, '20250483', '学生483', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (484, '20250484', '学生484', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (485, '20250485', '学生485', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (486, '20250486', '学生486', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (487, '20250487', '学生487', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (488, '20250488', '学生488', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (489, '20250489', '学生489', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (490, '20250490', '学生490', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (491, '20250491', '学生491', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (492, '20250492', '学生492', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (493, '20250493', '学生493', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (494, '20250494', '学生494', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (495, '20250495', '学生495', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (496, '20250496', '学生496', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (497, '20250497', '学生497', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (498, '20250498', '学生498', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (499, '20250499', '学生499', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (500, '20250500', '学生500', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (501, '20250501', '学生501', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (502, '20250502', '学生502', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (503, '20250503', '学生503', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (504, '20250504', '学生504', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (505, '20250505', '学生505', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (506, '20250506', '学生506', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (507, '20250507', '学生507', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (508, '20250508', '学生508', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (509, '20250509', '学生509', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (510, '20250510', '学生510', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (511, '20250511', '学生511', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (512, '20250512', '学生512', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (513, '20250513', '学生513', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (514, '20250514', '学生514', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (515, '20250515', '学生515', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (516, '20250516', '学生516', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (517, '20250517', '学生517', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (518, '20250518', '学生518', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (519, '20250519', '学生519', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (520, '20250520', '学生520', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (521, '20250521', '学生521', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (522, '20250522', '学生522', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (523, '20250523', '学生523', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (524, '20250524', '学生524', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (525, '20250525', '学生525', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (526, '20250526', '学生526', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (527, '20250527', '学生527', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (528, '20250528', '学生528', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (529, '20250529', '学生529', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (530, '20250530', '学生530', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (531, '20250531', '学生531', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (532, '20250532', '学生532', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (533, '20250533', '学生533', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (534, '20250534', '学生534', '大一下', '计算机2班（大一）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (535, '20250535', '学生535', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (536, '20250536', '学生536', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (537, '20250537', '学生537', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (538, '20250538', '学生538', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (539, '20250539', '学生539', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (540, '20250540', '学生540', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (541, '20250541', '学生541', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (542, '20250542', '学生542', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (543, '20250543', '学生543', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (544, '20250544', '学生544', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (545, '20250545', '学生545', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (546, '20250546', '学生546', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (547, '20250547', '学生547', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (548, '20250548', '学生548', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (549, '20250549', '学生549', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (550, '20250550', '学生550', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (551, '20250551', '学生551', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (552, '20250552', '学生552', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (553, '20250553', '学生553', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (554, '20250554', '学生554', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (555, '20250555', '学生555', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (556, '20250556', '学生556', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (557, '20250557', '学生557', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (558, '20250558', '学生558', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (559, '20250559', '学生559', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (560, '20250560', '学生560', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (561, '20250561', '学生561', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (562, '20250562', '学生562', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (563, '20250563', '学生563', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (564, '20250564', '学生564', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (565, '20250565', '学生565', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (566, '20250566', '学生566', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (567, '20250567', '学生567', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (568, '20250568', '学生568', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (569, '20250569', '学生569', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (570, '20250570', '学生570', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (571, '20250571', '学生571', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (572, '20250572', '学生572', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (573, '20250573', '学生573', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (574, '20250574', '学生574', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (575, '20250575', '学生575', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (576, '20250576', '学生576', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (577, '20250577', '学生577', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (578, '20250578', '学生578', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (579, '20250579', '学生579', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (580, '20250580', '学生580', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (581, '20250581', '学生581', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (582, '20250582', '学生582', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (583, '20250583', '学生583', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (584, '20250584', '学生584', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (585, '20250585', '学生585', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (586, '20250586', '学生586', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (587, '20250587', '学生587', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (588, '20250588', '学生588', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (589, '20250589', '学生589', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (590, '20250590', '学生590', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (591, '20250591', '学生591', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (592, '20250592', '学生592', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (593, '20250593', '学生593', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (594, '20250594', '学生594', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (595, '20250595', '学生595', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (596, '20250596', '学生596', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (597, '20250597', '学生597', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (598, '20250598', '学生598', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (599, '20250599', '学生599', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (600, '20250600', '学生600', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (601, '20250601', '学生601', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (602, '20250602', '学生602', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (603, '20250603', '学生603', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (604, '20250604', '学生604', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (605, '20250605', '学生605', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (606, '20250606', '学生606', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (607, '20250607', '学生607', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (608, '20250608', '学生608', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (609, '20250609', '学生609', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (610, '20250610', '学生610', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (611, '20250611', '学生611', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (612, '20250612', '学生612', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (613, '20250613', '学生613', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (614, '20250614', '学生614', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (615, '20250615', '学生615', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (616, '20250616', '学生616', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (617, '20250617', '学生617', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (618, '20250618', '学生618', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (619, '20250619', '学生619', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (620, '20250620', '学生620', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (621, '20250621', '学生621', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (622, '20250622', '学生622', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (623, '20250623', '学生623', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (624, '20250624', '学生624', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (625, '20250625', '学生625', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (626, '20250626', '学生626', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (627, '20250627', '学生627', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (628, '20250628', '学生628', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (629, '20250629', '学生629', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (630, '20250630', '学生630', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (631, '20250631', '学生631', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (632, '20250632', '学生632', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (633, '20250633', '学生633', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (634, '20250634', '学生634', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (635, '20250635', '学生635', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (636, '20250636', '学生636', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (637, '20250637', '学生637', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (638, '20250638', '学生638', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (639, '20250639', '学生639', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (640, '20250640', '学生640', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (641, '20250641', '学生641', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (642, '20250642', '学生642', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (643, '20250643', '学生643', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (644, '20250644', '学生644', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (645, '20250645', '学生645', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (646, '20250646', '学生646', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (647, '20250647', '学生647', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (648, '20250648', '学生648', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (649, '20250649', '学生649', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (650, '20250650', '学生650', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (651, '20250651', '学生651', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (652, '20250652', '学生652', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (653, '20250653', '学生653', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (654, '20250654', '学生654', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (655, '20250655', '学生655', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (656, '20250656', '学生656', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (657, '20250657', '学生657', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (658, '20250658', '学生658', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (659, '20250659', '学生659', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (660, '20250660', '学生660', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (661, '20250661', '学生661', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (662, '20250662', '学生662', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (663, '20250663', '学生663', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (664, '20250664', '学生664', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (665, '20250665', '学生665', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (666, '20250666', '学生666', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (667, '20250667', '学生667', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (668, '20250668', '学生668', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (669, '20250669', '学生669', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (670, '20250670', '学生670', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (671, '20250671', '学生671', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (672, '20250672', '学生672', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (673, '20250673', '学生673', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (674, '20250674', '学生674', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (675, '20250675', '学生675', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (676, '20250676', '学生676', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (677, '20250677', '学生677', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (678, '20250678', '学生678', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (679, '20250679', '学生679', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (680, '20250680', '学生680', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (681, '20250681', '学生681', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (682, '20250682', '学生682', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (683, '20250683', '学生683', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (684, '20250684', '学生684', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (685, '20250685', '学生685', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (686, '20250686', '学生686', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (687, '20250687', '学生687', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (688, '20250688', '学生688', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (689, '20250689', '学生689', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (690, '20250690', '学生690', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (691, '20250691', '学生691', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (692, '20250692', '学生692', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (693, '20250693', '学生693', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (694, '20250694', '学生694', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (695, '20250695', '学生695', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (696, '20250696', '学生696', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (697, '20250697', '学生697', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (698, '20250698', '学生698', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (699, '20250699', '学生699', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (700, '20250700', '学生700', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (701, '20250701', '学生701', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (702, '20250702', '学生702', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (703, '20250703', '学生703', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (704, '20250704', '学生704', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (705, '20250705', '学生705', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (706, '20250706', '学生706', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (707, '20250707', '学生707', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (708, '20250708', '学生708', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (709, '20250709', '学生709', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (710, '20250710', '学生710', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (711, '20250711', '学生711', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (712, '20250712', '学生712', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (713, '20250713', '学生713', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (714, '20250714', '学生714', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (715, '20250715', '学生715', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (716, '20250716', '学生716', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (717, '20250717', '学生717', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (718, '20250718', '学生718', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (719, '20250719', '学生719', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (720, '20250720', '学生720', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (721, '20250721', '学生721', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (722, '20250722', '学生722', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (723, '20250723', '学生723', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (724, '20250724', '学生724', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (725, '20250725', '学生725', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (726, '20250726', '学生726', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (727, '20250727', '学生727', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (728, '20250728', '学生728', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (729, '20250729', '学生729', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (730, '20250730', '学生730', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (731, '20250731', '学生731', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (732, '20250732', '学生732', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (733, '20250733', '学生733', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (734, '20250734', '学生734', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (735, '20250735', '学生735', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (736, '20250736', '学生736', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (737, '20250737', '学生737', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (738, '20250738', '学生738', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (739, '20250739', '学生739', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (740, '20250740', '学生740', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (741, '20250741', '学生741', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (742, '20250742', '学生742', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (743, '20250743', '学生743', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (744, '20250744', '学生744', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (745, '20250745', '学生745', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (746, '20250746', '学生746', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (747, '20250747', '学生747', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (748, '20250748', '学生748', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (749, '20250749', '学生749', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (750, '20250750', '学生750', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (751, '20250751', '学生751', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (752, '20250752', '学生752', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (753, '20250753', '学生753', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (754, '20250754', '学生754', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (755, '20250755', '学生755', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (756, '20250756', '学生756', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (757, '20250757', '学生757', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (758, '20250758', '学生758', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (759, '20250759', '学生759', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (760, '20250760', '学生760', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (761, '20250761', '学生761', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (762, '20250762', '学生762', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (763, '20250763', '学生763', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (764, '20250764', '学生764', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (765, '20250765', '学生765', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (766, '20250766', '学生766', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (767, '20250767', '学生767', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (768, '20250768', '学生768', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (769, '20250769', '学生769', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (770, '20250770', '学生770', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (771, '20250771', '学生771', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (772, '20250772', '学生772', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (773, '20250773', '学生773', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (774, '20250774', '学生774', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (775, '20250775', '学生775', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (776, '20250776', '学生776', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (777, '20250777', '学生777', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (778, '20250778', '学生778', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (779, '20250779', '学生779', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (780, '20250780', '学生780', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (781, '20250781', '学生781', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (782, '20250782', '学生782', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (783, '20250783', '学生783', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (784, '20250784', '学生784', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (785, '20250785', '学生785', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (786, '20250786', '学生786', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (787, '20250787', '学生787', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (788, '20250788', '学生788', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (789, '20250789', '学生789', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (790, '20250790', '学生790', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (791, '20250791', '学生791', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (792, '20250792', '学生792', '大二上', '计算机1班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (793, '20250793', '学生793', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (794, '20250794', '学生794', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (795, '20250795', '学生795', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (796, '20250796', '学生796', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (797, '20250797', '学生797', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (798, '20250798', '学生798', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (799, '20250799', '学生799', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');
INSERT INTO `student` VALUES (800, '20250800', '学生800', '大二下', '计算机2班（大二）', '2026-05-16 16:49:40', '123456');

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
) ENGINE = InnoDB AUTO_INCREMENT = 534 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生-课程关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_course
-- ----------------------------
INSERT INTO `student_course` VALUES (1, 1, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (2, 2, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (3, 3, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (4, 4, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (5, 5, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (6, 6, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (7, 7, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (8, 8, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (9, 9, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (10, 10, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (11, 11, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (12, 12, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (13, 13, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (14, 14, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (15, 15, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (16, 16, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (17, 17, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (18, 18, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (19, 19, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (20, 20, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (21, 21, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (22, 22, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (23, 23, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (24, 24, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (25, 25, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (26, 26, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (27, 27, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (28, 28, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (29, 29, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (30, 30, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (31, 31, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (32, 32, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (33, 33, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (34, 34, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (35, 35, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (36, 36, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (37, 37, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (38, 38, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (39, 39, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (40, 40, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (41, 41, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (42, 42, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (43, 43, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (44, 44, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (45, 45, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (46, 46, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (47, 47, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (48, 48, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (49, 49, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (50, 50, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (51, 51, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (52, 52, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (53, 53, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (54, 54, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (55, 55, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (56, 56, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (57, 57, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (58, 58, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (59, 59, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (60, 60, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (61, 61, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (62, 62, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (63, 63, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (64, 64, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (65, 65, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (66, 66, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (67, 67, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (68, 68, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (69, 69, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (70, 70, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (71, 71, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (72, 72, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (73, 73, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (74, 74, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (75, 75, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (76, 76, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (77, 77, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (78, 78, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (79, 79, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (80, 80, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (81, 81, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (82, 82, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (83, 83, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (84, 84, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (85, 85, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (86, 86, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (87, 87, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (88, 88, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (89, 89, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (90, 90, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (91, 91, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (92, 92, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (93, 93, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (94, 94, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (95, 95, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (96, 96, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (97, 97, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (98, 98, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (99, 99, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (100, 100, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (101, 101, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (102, 102, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (103, 103, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (104, 104, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (105, 105, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (106, 106, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (107, 107, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (108, 108, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (109, 109, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (110, 110, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (111, 111, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (112, 112, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (113, 113, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (114, 114, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (115, 115, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (116, 116, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (117, 117, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (118, 118, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (119, 119, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (120, 120, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (121, 121, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (122, 122, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (123, 123, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (124, 124, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (125, 125, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (126, 126, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (127, 127, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (128, 128, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (129, 129, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (130, 130, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (131, 131, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (132, 132, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (133, 133, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (134, 134, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (135, 135, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (136, 136, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (137, 137, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (138, 138, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (139, 139, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (140, 140, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (141, 141, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (142, 142, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (143, 143, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (144, 144, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (145, 145, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (146, 146, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (147, 147, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (148, 148, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (149, 149, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (150, 150, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (151, 151, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (152, 152, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (153, 153, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (154, 154, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (155, 155, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (156, 156, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (157, 157, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (158, 158, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (159, 159, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (160, 160, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (161, 161, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (162, 162, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (163, 163, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (164, 164, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (165, 165, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (166, 166, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (167, 167, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (168, 168, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (169, 169, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (170, 170, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (171, 171, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (172, 172, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (173, 173, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (174, 174, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (175, 175, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (176, 176, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (177, 177, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (178, 178, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (179, 179, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (180, 180, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (181, 181, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (182, 182, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (183, 183, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (184, 184, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (185, 185, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (186, 186, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (187, 187, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (188, 188, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (189, 189, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (190, 190, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (191, 191, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (192, 192, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (193, 193, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (194, 194, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (195, 195, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (196, 196, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (197, 197, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (198, 198, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (199, 199, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (200, 200, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (201, 201, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (202, 202, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (203, 203, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (204, 204, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (205, 205, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (206, 206, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (207, 207, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (208, 208, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (209, 209, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (210, 210, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (211, 211, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (212, 212, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (213, 213, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (214, 214, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (215, 215, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (216, 216, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (217, 217, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (218, 218, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (219, 219, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (220, 220, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (221, 221, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (222, 222, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (223, 223, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (224, 224, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (225, 225, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (226, 226, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (227, 227, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (228, 228, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (229, 229, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (230, 230, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (231, 231, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (232, 232, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (233, 233, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (234, 234, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (235, 235, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (236, 236, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (237, 237, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (238, 238, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (239, 239, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (240, 240, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (241, 241, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (242, 242, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (243, 243, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (244, 244, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (245, 245, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (246, 246, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (247, 247, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (248, 248, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (249, 249, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (250, 250, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (251, 251, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (252, 252, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (253, 253, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (254, 254, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (255, 255, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (256, 256, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (257, 257, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (258, 258, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (259, 259, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (260, 260, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (261, 261, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (262, 262, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (263, 263, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (264, 264, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (265, 265, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (266, 266, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (267, 267, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (268, 268, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (269, 269, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (270, 270, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (271, 271, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (272, 272, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (273, 273, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (274, 274, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (275, 275, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (276, 276, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (277, 277, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (278, 278, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (279, 279, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (280, 280, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (281, 281, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (282, 282, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (283, 283, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (284, 284, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (285, 285, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (286, 286, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (287, 287, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (288, 288, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (289, 289, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (290, 290, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (291, 291, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (292, 292, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (293, 293, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (294, 294, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (295, 295, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (296, 296, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (297, 297, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (298, 298, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (299, 299, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (300, 300, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (301, 301, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (302, 302, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (303, 303, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (304, 304, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (305, 305, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (306, 306, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (307, 307, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (308, 308, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (309, 309, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (310, 310, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (311, 311, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (312, 312, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (313, 313, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (314, 314, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (315, 315, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (316, 316, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (317, 317, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (318, 318, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (319, 319, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (320, 320, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (321, 321, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (322, 322, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (323, 323, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (324, 324, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (325, 325, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (326, 326, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (327, 327, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (328, 328, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (329, 329, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (330, 330, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (331, 331, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (332, 332, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (333, 333, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (334, 334, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (335, 335, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (336, 336, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (337, 337, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (338, 338, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (339, 339, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (340, 340, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (341, 341, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (342, 342, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (343, 343, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (344, 344, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (345, 345, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (346, 346, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (347, 347, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (348, 348, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (349, 349, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (350, 350, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (351, 351, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (352, 352, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (353, 353, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (354, 354, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (355, 355, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (356, 356, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (357, 357, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (358, 358, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (359, 359, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (360, 360, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (361, 361, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (362, 362, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (363, 363, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (364, 364, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (365, 365, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (366, 366, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (367, 367, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (368, 368, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (369, 369, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (370, 370, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (371, 371, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (372, 372, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (373, 373, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (374, 374, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (375, 375, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (376, 376, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (377, 377, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (378, 378, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (379, 379, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (380, 380, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (381, 381, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (382, 382, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (383, 383, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (384, 384, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (385, 385, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (386, 386, 2, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (387, 387, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (388, 388, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (389, 389, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (390, 390, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (391, 391, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (392, 392, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (393, 393, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (394, 394, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (395, 395, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (396, 396, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (397, 397, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (398, 398, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (399, 399, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (400, 400, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (401, 401, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (402, 402, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (403, 403, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (404, 404, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (405, 405, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (406, 406, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (407, 407, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (408, 408, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (409, 409, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (410, 410, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (411, 411, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (412, 412, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (413, 413, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (414, 414, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (415, 415, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (416, 416, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (417, 417, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (418, 418, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (419, 419, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (420, 420, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (421, 421, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (422, 422, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (423, 423, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (424, 424, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (425, 425, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (426, 426, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (427, 427, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (428, 428, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (429, 429, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (430, 430, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (431, 431, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (432, 432, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (433, 433, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (434, 434, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (435, 435, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (436, 436, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (437, 437, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (438, 438, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (439, 439, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (440, 440, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (441, 441, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (442, 442, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (443, 443, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (444, 444, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (445, 445, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (446, 446, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (447, 447, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (448, 448, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (449, 449, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (450, 450, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (451, 451, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (452, 452, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (453, 453, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (454, 454, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (455, 455, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (456, 456, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (457, 457, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (458, 458, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (459, 459, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (460, 460, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (461, 461, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (462, 462, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (463, 463, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (464, 464, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (465, 465, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (466, 466, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (467, 467, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (468, 468, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (469, 469, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (470, 470, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (471, 471, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (472, 472, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (473, 473, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (474, 474, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (475, 475, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (476, 476, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (477, 477, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (478, 478, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (479, 479, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (480, 480, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (481, 481, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (482, 482, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (483, 483, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (484, 484, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (485, 485, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (486, 486, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (487, 487, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (488, 488, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (489, 489, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (490, 490, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (491, 491, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (492, 492, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (493, 493, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (494, 494, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (495, 495, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (496, 496, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (497, 497, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (498, 498, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (499, 499, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (500, 500, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (501, 501, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (502, 502, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (503, 503, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (504, 504, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (505, 505, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (506, 506, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (507, 507, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (508, 508, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (509, 509, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (510, 510, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (511, 511, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (512, 512, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (513, 513, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (514, 514, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (515, 515, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (516, 516, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (517, 517, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (518, 518, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (519, 519, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (520, 520, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (521, 521, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (522, 522, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (523, 523, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (524, 524, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (525, 525, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (526, 526, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (527, 527, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (528, 528, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (529, 529, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (530, 530, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (531, 531, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (532, 532, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (533, 533, 1, '2026-05-16 16:59:23');
INSERT INTO `student_course` VALUES (534, 534, 1, '2026-05-16 16:59:23');

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
) ENGINE = InnoDB AUTO_INCREMENT = 436 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生薄弱知识点明细' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_weak_point
-- ----------------------------
INSERT INTO `student_weak_point` VALUES (9, 100, 2, 23, 72.0, 6, NULL, '2026-06-24 20:17:04');
INSERT INTO `student_weak_point` VALUES (10, 100, 2, 26, 80.0, 8, NULL, '2026-06-24 20:17:04');
INSERT INTO `student_weak_point` VALUES (11, 100, 2, 31, 66.0, 5, NULL, '2026-06-24 20:17:04');
INSERT INTO `student_weak_point` VALUES (15, 2, 1, 20, 31.1, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (16, 2, 1, 4, 33.5, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (17, 3, 1, 15, 34.2, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (18, 3, 1, 5, 33.2, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (19, 3, 1, 11, 45.4, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (20, 4, 1, 15, 40.5, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (21, 4, 1, 4, 27.3, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (22, 4, 1, 16, 47.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (23, 4, 1, 7, 20.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (24, 5, 1, 15, 46.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (25, 5, 1, 16, 46.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (26, 5, 1, 13, 30.6, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (27, 6, 1, 3, 31.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (28, 6, 1, 19, 32.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (29, 7, 1, 4, 31.8, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (30, 7, 1, 5, 21.0, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (31, 8, 1, 1, 21.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (32, 8, 1, 18, 27.2, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (33, 9, 1, 14, 36.6, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (34, 9, 1, 10, 17.7, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (35, 9, 1, 16, 39.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (36, 10, 1, 6, 17.1, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (37, 10, 1, 14, 38.6, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (38, 11, 1, 6, 17.1, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (39, 11, 1, 4, 40.4, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (40, 11, 1, 13, 32.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (41, 11, 1, 1, 21.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (42, 12, 1, 14, 44.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (43, 12, 1, 16, 42.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (44, 12, 1, 8, 34.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (45, 13, 1, 20, 42.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (46, 13, 1, 13, 18.0, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (47, 14, 1, 3, 20.9, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (48, 14, 1, 12, 12.2, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (49, 15, 1, 4, 30.8, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (50, 15, 1, 18, 36.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (51, 16, 1, 10, 21.6, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (52, 16, 1, 6, 22.9, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (53, 17, 1, 14, 30.4, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (54, 17, 1, 17, 19.4, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (55, 17, 1, 9, 23.0, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (56, 18, 1, 16, 35.4, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (57, 18, 1, 3, 25.9, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (58, 18, 1, 9, 29.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (59, 18, 1, 13, 25.8, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (60, 19, 1, 18, 36.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (61, 19, 1, 15, 37.4, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (62, 19, 1, 9, 28.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (63, 19, 1, 4, 42.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (64, 20, 1, 17, 36.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (65, 20, 1, 14, 47.0, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (66, 20, 1, 20, 43.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (67, 20, 1, 5, 35.1, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (68, 21, 1, 20, 39.1, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (69, 21, 1, 1, 18.9, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (70, 21, 1, 4, 45.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (71, 21, 1, 5, 27.1, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (72, 22, 1, 1, 22.2, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (73, 22, 1, 6, 31.6, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (74, 22, 1, 15, 43.2, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (75, 22, 1, 20, 51.1, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (76, 23, 1, 16, 45.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (77, 23, 1, 18, 46.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (78, 23, 1, 13, 25.7, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (79, 23, 1, 3, 32.9, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (80, 23, 1, 11, 46.4, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (81, 23, 1, 17, 33.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (82, 24, 1, 8, 41.8, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (83, 24, 1, 3, 40.8, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (84, 24, 1, 17, 39.3, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (85, 24, 1, 2, 31.1, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (86, 24, 1, 15, 41.2, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (87, 24, 1, 20, 39.7, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (88, 25, 1, 11, 45.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (89, 25, 1, 12, 19.6, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (90, 25, 1, 17, 26.1, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (91, 25, 1, 8, 51.3, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (92, 25, 1, 7, 25.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (93, 26, 1, 17, 37.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (94, 26, 1, 11, 43.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (95, 26, 1, 2, 25.3, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (96, 27, 1, 18, 36.6, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (97, 27, 1, 19, 28.8, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (98, 27, 1, 11, 53.2, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (99, 27, 1, 16, 48.4, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (100, 28, 1, 15, 46.0, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (101, 28, 1, 16, 50.5, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (102, 28, 1, 2, 26.4, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (103, 28, 1, 20, 39.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (104, 29, 1, 8, 49.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (105, 29, 1, 16, 38.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (106, 29, 1, 11, 36.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (107, 30, 1, 17, 34.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (108, 30, 1, 4, 46.4, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (109, 30, 1, 16, 36.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (110, 31, 1, 1, 30.7, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (111, 31, 1, 7, 25.3, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (112, 31, 1, 13, 26.4, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (113, 31, 1, 4, 54.9, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (114, 31, 1, 6, 21.7, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (115, 31, 1, 11, 36.7, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (116, 32, 1, 17, 38.4, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (117, 32, 1, 8, 36.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (118, 32, 1, 9, 30.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (119, 32, 1, 5, 36.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (120, 33, 1, 18, 38.7, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (121, 33, 1, 20, 53.6, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (122, 33, 1, 19, 36.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (123, 33, 1, 4, 52.0, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (124, 34, 1, 20, 35.5, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (125, 34, 1, 19, 26.5, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (126, 34, 1, 4, 36.7, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (127, 35, 1, 18, 40.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (128, 35, 1, 17, 37.1, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (129, 35, 1, 8, 35.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (130, 35, 1, 9, 41.7, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (131, 36, 1, 5, 31.1, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (132, 36, 1, 2, 20.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (133, 36, 1, 17, 34.7, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (134, 36, 1, 18, 43.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (135, 36, 1, 8, 43.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (136, 37, 1, 16, 39.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (137, 37, 1, 15, 49.2, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (138, 37, 1, 5, 37.3, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (139, 37, 1, 7, 24.6, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (140, 37, 1, 17, 30.3, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (141, 38, 1, 13, 41.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (142, 38, 1, 9, 39.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (143, 38, 1, 14, 54.9, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (144, 39, 1, 9, 28.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (145, 39, 1, 8, 54.4, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (146, 39, 1, 4, 54.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (147, 39, 1, 17, 33.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (148, 39, 1, 1, 22.4, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (149, 40, 1, 15, 46.9, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (150, 40, 1, 16, 41.0, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (151, 40, 1, 20, 37.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (152, 41, 1, 15, 43.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (153, 41, 1, 13, 25.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (154, 41, 1, 1, 25.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (155, 41, 1, 16, 51.3, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (156, 41, 1, 18, 40.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (157, 42, 1, 18, 38.9, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (158, 42, 1, 20, 35.3, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (159, 42, 1, 3, 35.8, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (160, 43, 1, 8, 49.4, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (161, 43, 1, 20, 52.9, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (162, 43, 1, 4, 42.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (163, 43, 1, 14, 53.0, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (164, 43, 1, 13, 41.0, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (165, 44, 1, 20, 52.3, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (166, 44, 1, 18, 52.5, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (167, 44, 1, 14, 40.9, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (168, 44, 1, 13, 39.7, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (169, 44, 1, 16, 44.7, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (170, 44, 1, 11, 43.5, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (171, 45, 1, 7, 18.4, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (172, 45, 1, 9, 39.8, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (173, 45, 1, 3, 34.8, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (174, 46, 1, 20, 52.8, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (175, 46, 1, 17, 37.4, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (176, 46, 1, 18, 39.0, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (177, 47, 1, 3, 31.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (178, 47, 1, 6, 27.4, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (179, 47, 1, 20, 35.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (180, 47, 1, 10, 15.1, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (181, 47, 1, 15, 37.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (182, 47, 1, 11, 41.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (183, 48, 1, 15, 46.2, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (184, 48, 1, 4, 45.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (185, 48, 1, 5, 34.4, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (186, 48, 1, 20, 37.4, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (187, 49, 1, 16, 47.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (188, 49, 1, 14, 54.2, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (189, 49, 1, 9, 33.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (190, 50, 1, 8, 37.4, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (191, 50, 1, 1, 19.4, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (192, 50, 1, 20, 45.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (193, 51, 1, 12, 31.5, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (194, 51, 1, 4, 46.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (195, 51, 1, 17, 36.2, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (196, 51, 1, 18, 47.4, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (197, 51, 1, 3, 31.7, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (198, 52, 1, 8, 37.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (199, 52, 1, 11, 40.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (200, 52, 1, 1, 27.7, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (201, 53, 1, 20, 36.3, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (202, 53, 1, 11, 48.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (203, 53, 1, 3, 41.9, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (204, 53, 1, 14, 49.1, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (205, 54, 1, 8, 54.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (206, 54, 1, 16, 51.8, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (207, 54, 1, 17, 31.1, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (208, 54, 1, 18, 46.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (209, 55, 1, 10, 15.9, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (210, 55, 1, 14, 42.9, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (211, 55, 1, 5, 34.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (212, 56, 1, 1, 27.5, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (213, 56, 1, 4, 35.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (214, 56, 1, 15, 36.8, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (215, 56, 1, 10, 19.7, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (216, 57, 1, 13, 41.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (217, 57, 1, 11, 36.2, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (218, 57, 1, 3, 32.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (219, 57, 1, 19, 40.6, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (220, 58, 1, 14, 40.3, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (221, 58, 1, 18, 42.5, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (222, 58, 1, 7, 18.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (223, 58, 1, 3, 28.5, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (224, 58, 1, 13, 30.4, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (225, 58, 1, 11, 39.9, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (226, 59, 1, 20, 35.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (227, 59, 1, 17, 38.5, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (228, 59, 1, 8, 39.8, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (229, 59, 1, 7, 31.1, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (230, 59, 1, 18, 39.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (231, 59, 1, 9, 36.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (232, 60, 1, 11, 49.3, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (233, 60, 1, 20, 50.3, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (234, 60, 1, 4, 42.1, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (235, 61, 1, 2, 24.5, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (236, 61, 1, 16, 35.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (237, 61, 1, 6, 26.2, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (238, 61, 1, 20, 52.9, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (239, 61, 1, 3, 41.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (240, 62, 1, 4, 43.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (241, 62, 1, 9, 38.6, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (242, 62, 1, 20, 53.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (243, 62, 1, 16, 38.9, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (244, 62, 1, 2, 18.0, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (245, 62, 1, 13, 35.2, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (246, 63, 1, 17, 29.0, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (247, 63, 1, 5, 34.9, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (248, 63, 1, 16, 45.8, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (249, 63, 1, 4, 41.3, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (250, 63, 1, 18, 44.5, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (251, 63, 1, 14, 37.7, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (252, 64, 1, 7, 35.0, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (253, 64, 1, 4, 55.0, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (254, 64, 1, 17, 44.6, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (255, 64, 1, 8, 51.1, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (256, 64, 1, 3, 44.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (257, 64, 1, 9, 37.4, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (258, 64, 1, 12, 39.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (259, 65, 1, 15, 43.6, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (260, 65, 1, 18, 47.4, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (261, 65, 1, 14, 56.2, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (262, 65, 1, 16, 59.2, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (263, 65, 1, 8, 61.4, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (264, 65, 1, 3, 34.3, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (265, 65, 1, 19, 49.0, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (266, 66, 1, 14, 54.0, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (267, 66, 1, 13, 42.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (268, 66, 1, 16, 51.8, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (269, 66, 1, 8, 46.5, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (270, 66, 1, 20, 46.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (271, 66, 1, 17, 42.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (272, 66, 1, 3, 34.1, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (273, 67, 1, 3, 33.9, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (274, 67, 1, 9, 45.1, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (275, 67, 1, 11, 44.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (276, 67, 1, 14, 60.9, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (277, 67, 1, 5, 33.4, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (278, 67, 1, 4, 59.3, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (279, 68, 1, 3, 45.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (280, 68, 1, 19, 48.0, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (281, 68, 1, 5, 35.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (282, 68, 1, 9, 44.3, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (283, 68, 1, 8, 50.5, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (284, 69, 1, 12, 25.1, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (285, 69, 1, 20, 49.1, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (286, 69, 1, 16, 56.4, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (287, 69, 1, 7, 31.6, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (288, 69, 1, 17, 40.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (289, 69, 1, 18, 54.1, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (290, 69, 1, 8, 61.8, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (291, 69, 1, 11, 49.1, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (292, 70, 1, 18, 55.0, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (293, 70, 1, 8, 55.6, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (294, 70, 1, 1, 34.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (295, 70, 1, 20, 54.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (296, 70, 1, 10, 30.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (297, 70, 1, 16, 48.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (298, 70, 1, 19, 34.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (299, 71, 1, 16, 47.1, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (300, 71, 1, 14, 54.9, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (301, 71, 1, 15, 47.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (302, 71, 1, 17, 35.0, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (303, 71, 1, 4, 52.9, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (304, 71, 1, 13, 43.6, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (305, 72, 1, 6, 24.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (306, 72, 1, 8, 60.2, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (307, 72, 1, 18, 45.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (308, 72, 1, 15, 59.1, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (309, 72, 1, 7, 32.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (310, 72, 1, 14, 46.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (311, 72, 1, 11, 54.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (312, 72, 1, 20, 49.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (313, 73, 1, 4, 51.2, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (314, 73, 1, 8, 61.3, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (315, 73, 1, 3, 48.4, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (316, 73, 1, 2, 24.6, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (317, 73, 1, 18, 62.0, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (318, 73, 1, 20, 59.2, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (319, 74, 1, 18, 59.4, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (320, 74, 1, 16, 45.6, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (321, 74, 1, 14, 62.4, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (322, 74, 1, 6, 38.4, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (323, 74, 1, 4, 46.0, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (324, 74, 1, 13, 41.9, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (325, 74, 1, 8, 60.8, 15, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (326, 74, 1, 11, 56.6, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (327, 75, 1, 6, 28.0, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (328, 75, 1, 18, 50.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (329, 75, 1, 13, 34.4, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (330, 75, 1, 14, 55.6, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (331, 75, 1, 20, 45.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (332, 75, 1, 19, 44.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (333, 75, 1, 9, 46.2, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (334, 76, 1, 17, 46.9, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (335, 76, 1, 13, 42.8, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (336, 76, 1, 7, 28.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (337, 76, 1, 19, 48.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (338, 76, 1, 11, 51.5, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (339, 76, 1, 8, 44.6, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (340, 76, 1, 1, 39.0, 6, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (341, 76, 1, 3, 36.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (342, 77, 1, 18, 45.0, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (343, 77, 1, 4, 53.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (344, 77, 1, 11, 61.0, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (345, 77, 1, 14, 59.2, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (346, 77, 1, 16, 59.6, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (347, 77, 1, 10, 38.0, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (348, 77, 1, 13, 41.0, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (349, 78, 1, 7, 34.1, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (350, 78, 1, 13, 42.4, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (351, 78, 1, 10, 23.9, 3, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (352, 78, 1, 18, 43.7, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (353, 78, 1, 17, 45.5, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (354, 78, 1, 19, 41.9, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (355, 78, 1, 3, 49.8, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (356, 79, 1, 18, 43.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (357, 79, 1, 16, 53.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (358, 79, 1, 10, 26.4, 4, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (359, 79, 1, 20, 46.6, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (360, 79, 1, 14, 62.6, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (361, 79, 1, 1, 29.8, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (362, 79, 1, 17, 36.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (363, 79, 1, 9, 43.0, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (364, 80, 1, 20, 50.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (365, 80, 1, 10, 34.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (366, 80, 1, 17, 37.8, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (367, 80, 1, 14, 57.3, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (368, 80, 1, 13, 33.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (369, 81, 1, 18, 49.8, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (370, 81, 1, 20, 54.5, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (371, 81, 1, 16, 49.8, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (372, 81, 1, 19, 54.4, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (373, 81, 1, 14, 63.4, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (374, 81, 1, 8, 59.0, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (375, 81, 1, 11, 63.8, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (376, 81, 1, 17, 55.9, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (377, 82, 1, 13, 54.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (378, 82, 1, 16, 57.8, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (379, 82, 1, 17, 49.8, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (380, 82, 1, 18, 62.4, 15, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (381, 82, 1, 5, 52.2, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (382, 82, 1, 11, 49.6, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (383, 82, 1, 4, 57.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (384, 83, 1, 16, 61.2, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (385, 83, 1, 9, 48.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (386, 83, 1, 15, 53.8, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (387, 83, 1, 8, 64.3, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (388, 83, 1, 5, 45.4, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (389, 83, 1, 19, 47.2, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (390, 83, 1, 14, 62.8, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (391, 83, 1, 11, 57.7, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (392, 84, 1, 11, 53.3, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (393, 84, 1, 3, 47.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (394, 84, 1, 5, 53.8, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (395, 84, 1, 19, 44.1, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (396, 84, 1, 20, 59.1, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (397, 84, 1, 14, 53.9, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (398, 85, 1, 20, 61.1, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (399, 85, 1, 11, 60.1, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (400, 85, 1, 18, 53.3, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (401, 85, 1, 4, 54.8, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (402, 85, 1, 14, 56.0, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (403, 85, 1, 10, 29.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (404, 85, 1, 5, 53.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (405, 86, 1, 11, 62.9, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (406, 86, 1, 19, 40.8, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (407, 86, 1, 16, 58.7, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (408, 86, 1, 20, 57.9, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (409, 86, 1, 10, 41.6, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (410, 86, 1, 7, 33.8, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (411, 86, 1, 18, 52.5, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (412, 86, 1, 14, 62.5, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (413, 87, 1, 12, 40.5, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (414, 87, 1, 2, 37.2, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (415, 87, 1, 19, 55.8, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (416, 87, 1, 14, 57.5, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (417, 87, 1, 4, 50.8, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (418, 87, 1, 16, 55.3, 12, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (419, 88, 1, 16, 52.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (420, 88, 1, 9, 48.5, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (421, 88, 1, 15, 59.8, 10, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (422, 88, 1, 8, 58.4, 13, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (423, 88, 1, 18, 56.5, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (424, 88, 1, 12, 34.4, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (425, 88, 1, 2, 37.6, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (426, 88, 1, 17, 51.1, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (427, 88, 1, 20, 49.7, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (428, 89, 1, 16, 54.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (429, 89, 1, 13, 48.7, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (430, 89, 1, 10, 31.5, 5, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (431, 89, 1, 8, 59.7, 14, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (432, 89, 1, 11, 49.1, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (433, 89, 1, 4, 52.0, 11, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (434, 89, 1, 15, 49.3, 9, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (435, 89, 1, 6, 40.9, 8, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (436, 89, 1, 1, 39.8, 7, NULL, '2026-06-25 20:00:00');
INSERT INTO `student_weak_point` VALUES (437, 1, 1, 14, 55.1, 9, 955, '2026-07-08 16:40:23');
INSERT INTO `student_weak_point` VALUES (438, 1, 1, 6, 58.0, 9, 955, '2026-07-08 16:40:23');
INSERT INTO `student_weak_point` VALUES (439, 1, 1, 10, 51.5, 9, 955, '2026-07-08 16:40:23');
INSERT INTO `student_weak_point` VALUES (440, 1, 1, 18, 49.4, 9, 955, '2026-07-08 16:40:23');

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
-- Records of study_duration
-- ----------------------------
INSERT INTO `study_duration` VALUES (1, 1, 1, 1, 360, 120, 100, 100, 40, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (2, 1, 1, 2, 380, 130, 100, 110, 40, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (3, 1, 1, 3, 340, 110, 100, 100, 30, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (4, 1, 1, 4, 390, 140, 100, 120, 30, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (5, 1, 1, 5, 370, 130, 90, 110, 40, NULL, '2026-03-29', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (6, 1, 1, 6, 400, 150, 100, 110, 40, NULL, '2026-04-05', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (7, 1, 1, 7, 380, 140, 90, 120, 30, NULL, '2026-04-12', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (8, 1, 1, 8, 410, 150, 100, 120, 40, NULL, '2026-04-19', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (9, 1, 1, 9, 390, 140, 100, 110, 40, NULL, '2026-04-26', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (10, 1, 1, 10, 420, 160, 100, 120, 40, NULL, '2026-05-03', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (11, 1, 1, 11, 400, 150, 100, 110, 40, NULL, '2026-05-10', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (12, 1, 1, 12, 410, 150, 110, 110, 40, NULL, '2026-05-17', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (13, 2, 1, 1, 280, 100, 80, 80, 20, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (14, 2, 1, 2, 300, 110, 70, 100, 20, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (15, 2, 1, 3, 260, 90, 70, 80, 20, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (16, 2, 1, 4, 320, 120, 80, 100, 20, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (17, 2, 1, 5, 290, 110, 60, 100, 20, NULL, '2026-03-29', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (18, 2, 1, 6, 330, 130, 80, 100, 20, NULL, '2026-04-05', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (19, 2, 1, 7, 310, 120, 70, 100, 20, NULL, '2026-04-12', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (20, 2, 1, 8, 350, 140, 70, 120, 20, NULL, '2026-04-19', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (21, 2, 1, 9, 280, 100, 60, 100, 20, NULL, '2026-04-26', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (22, 2, 1, 10, 270, 100, 50, 100, 20, NULL, '2026-05-03', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (23, 2, 1, 11, 300, 120, 60, 100, 20, NULL, '2026-05-10', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (24, 2, 1, 12, 290, 110, 60, 100, 20, NULL, '2026-05-17', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (25, 5, 1, 1, 310, 120, 80, 80, 30, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (26, 5, 1, 2, 290, 110, 70, 80, 30, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (27, 5, 1, 3, 260, 100, 60, 80, 20, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (28, 5, 1, 4, 230, 90, 50, 70, 20, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (29, 5, 1, 5, 200, 80, 40, 60, 20, NULL, '2026-03-29', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (30, 5, 1, 6, 160, 60, 30, 50, 20, NULL, '2026-04-05', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (31, 5, 1, 7, 130, 50, 30, 40, 10, NULL, '2026-04-12', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (32, 5, 1, 8, 100, 40, 20, 30, 10, NULL, '2026-04-19', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (33, 5, 1, 9, 80, 30, 20, 20, 10, NULL, '2026-04-26', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (34, 5, 1, 10, 60, 20, 20, 10, 10, NULL, '2026-05-03', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (35, 5, 1, 11, 50, 20, 10, 10, 10, NULL, '2026-05-10', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (36, 5, 1, 12, 40, 10, 10, 10, 10, NULL, '2026-05-17', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (37, 10, 1, 1, 120, 50, 30, 30, 10, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (38, 10, 1, 2, 350, 150, 80, 100, 20, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (39, 10, 1, 3, 80, 30, 20, 20, 10, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (40, 10, 1, 4, 300, 120, 70, 90, 20, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (41, 10, 1, 5, 90, 30, 30, 20, 10, NULL, '2026-03-29', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (42, 10, 1, 6, 400, 160, 100, 110, 30, NULL, '2026-04-05', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (43, 10, 1, 7, 70, 20, 20, 20, 10, NULL, '2026-04-12', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (44, 10, 1, 8, 280, 110, 60, 90, 20, NULL, '2026-04-19', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (45, 10, 1, 9, 60, 20, 20, 10, 10, NULL, '2026-04-26', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (46, 10, 1, 10, 250, 100, 50, 80, 20, NULL, '2026-05-03', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (47, 10, 1, 11, 100, 40, 30, 20, 10, NULL, '2026-05-10', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (48, 10, 1, 12, 150, 60, 40, 30, 20, NULL, '2026-05-17', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (49, 14, 1, 1, 80, 30, 20, 20, 10, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (50, 14, 1, 2, 60, 20, 20, 10, 10, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (51, 14, 1, 3, 40, 10, 10, 10, 10, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (52, 14, 1, 4, 50, 20, 10, 10, 10, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (53, 14, 1, 5, 30, 10, 10, 5, 5, NULL, '2026-03-29', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (54, 14, 1, 6, 70, 30, 20, 10, 10, NULL, '2026-04-05', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (55, 14, 1, 7, 40, 10, 10, 10, 10, NULL, '2026-04-12', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (56, 14, 1, 8, 60, 20, 20, 10, 10, NULL, '2026-04-19', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (57, 14, 1, 9, 80, 30, 20, 20, 10, NULL, '2026-04-26', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (58, 14, 1, 10, 35, 10, 10, 10, 5, NULL, '2026-05-03', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (59, 14, 1, 11, 50, 20, 10, 10, 10, NULL, '2026-05-10', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (60, 14, 1, 12, 45, 15, 10, 10, 10, NULL, '2026-05-17', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (61, 20, 1, 1, 150, 50, 40, 40, 20, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (62, 20, 1, 2, 170, 60, 50, 40, 20, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (63, 20, 1, 3, 120, 40, 30, 30, 20, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (64, 20, 1, 4, 160, 60, 40, 40, 20, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (65, 20, 1, 5, 180, 70, 40, 50, 20, NULL, '2026-03-29', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (66, 20, 1, 6, 140, 50, 30, 40, 20, NULL, '2026-04-05', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (67, 20, 1, 7, 130, 50, 30, 30, 20, NULL, '2026-04-12', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (68, 20, 1, 8, 110, 40, 30, 30, 10, NULL, '2026-04-19', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (69, 20, 1, 9, 150, 60, 40, 30, 20, NULL, '2026-04-26', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (70, 20, 1, 10, 120, 40, 30, 30, 20, NULL, '2026-05-03', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (71, 20, 1, 11, 100, 30, 30, 30, 10, NULL, '2026-05-10', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (72, 20, 1, 12, 130, 50, 30, 30, 20, NULL, '2026-05-17', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (73, 268, 2, 1, 300, 120, 70, 80, 30, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (74, 268, 2, 2, 320, 130, 80, 80, 30, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (75, 268, 2, 3, 280, 110, 60, 80, 30, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (76, 268, 2, 4, 310, 120, 80, 80, 30, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (77, 269, 2, 1, 180, 70, 50, 40, 20, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (78, 269, 2, 2, 160, 60, 40, 40, 20, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (79, 269, 2, 3, 200, 80, 50, 50, 20, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (80, 269, 2, 4, 150, 60, 40, 30, 20, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (81, 272, 2, 1, 100, 40, 30, 20, 10, NULL, '2026-03-01', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (82, 272, 2, 2, 90, 30, 30, 20, 10, NULL, '2026-03-08', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (83, 272, 2, 3, 70, 20, 20, 20, 10, NULL, '2026-03-15', '2026-05-21 14:55:40', '2026-05-21 14:55:40');
INSERT INTO `study_duration` VALUES (84, 272, 2, 4, 60, 20, 20, 10, 10, NULL, '2026-03-22', '2026-05-21 14:55:40', '2026-05-21 14:55:40');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子题作答记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sub_question_answer
-- ----------------------------

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
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_teacher_no`(`teacher_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES (1, '1001', '李兑', '123456', '13867653390', '2026-05-16 17:12:27', '2026-05-16 20:25:56');
INSERT INTO `teacher` VALUES (2, '1002', '王仝', '123456', '18257689009', '2026-05-16 17:12:27', '2026-05-16 20:26:38');
INSERT INTO `teacher` VALUES (3, '1003', '张里', '123456', '19938475123', '2026-05-16 17:12:27', '2026-05-16 20:25:31');
INSERT INTO `teacher` VALUES (4, '1004', '马思国', '123456', '19337685990', '2026-05-16 20:25:13', '2026-05-16 20:25:13');
INSERT INTO `teacher` VALUES (5, '1005', '旁云', '123456', '13556273384', '2026-05-16 20:53:09', '2026-05-16 20:53:09');
INSERT INTO `teacher` VALUES (6, '1006', '赵明', '123456', '13800000006', '2026-05-20 15:11:01', '2026-05-20 15:11:01');
INSERT INTO `teacher` VALUES (7, '1007', '孙丽', '123456', '13800000007', '2026-05-20 15:11:01', '2026-05-20 15:11:01');
INSERT INTO `teacher` VALUES (8, '1008', '周强', '123456', '13800000008', '2026-05-20 15:11:01', '2026-05-20 15:11:01');
INSERT INTO `teacher` VALUES (9, '1009', '吴静', '123456', '13800000009', '2026-05-20 15:11:01', '2026-05-20 15:11:01');

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
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师负责班级表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher_class
-- ----------------------------
INSERT INTO `teacher_class` VALUES (9, 1, '计算机1班（大一）', 1, 89, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (10, 6, '计算机1班（大一）', 90, 178, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (11, 7, '计算机1班（大一）', 179, 267, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (12, 4, '计算机2班（大一）', 268, 311, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (13, 8, '计算机2班（大一）', 312, 355, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (14, 9, '计算机2班（大一）', 356, 400, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (15, 2, '计算机1班（大二）', 535, 660, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (16, 3, '计算机2班（大二）', 661, 800, '2026-05-20 15:11:01');
INSERT INTO `teacher_class` VALUES (18, 5, '计算机1班（大一下）', 401, 534, '2026-05-21 14:42:30');

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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师管理记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher_management_log
-- ----------------------------
INSERT INTO `teacher_management_log` VALUES (1, 1, 1, 'MARK_ATTENTION', 'NORMAL', 'ATTENTION', '', '2026-05-26 17:15:34');
INSERT INTO `teacher_management_log` VALUES (2, 1, 1, 'SEND_NOTICE', 'ATTENTION', 'ATTENTION', '111: 请尽快处理', '2026-05-26 17:16:07');
INSERT INTO `teacher_management_log` VALUES (3, 1, 1, 'MARK_WARNING', 'ATTENTION', 'WARNING', '', '2026-05-26 18:07:45');
INSERT INTO `teacher_management_log` VALUES (4, 1, 1, 'RESOLVE', 'WARNING', 'NORMAL', '', '2026-05-26 18:07:59');
INSERT INTO `teacher_management_log` VALUES (5, 1, 1, 'MARK_ATTENTION', 'NORMAL', 'ATTENTION', '', '2026-06-23 20:57:12');
INSERT INTO `teacher_management_log` VALUES (6, 1, 1, 'MARK_WARNING', 'ATTENTION', 'WARNING', '', '2026-06-24 19:58:08');

SET FOREIGN_KEY_CHECKS = 1;
