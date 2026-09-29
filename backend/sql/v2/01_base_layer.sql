-- =============================================================================
-- 学情预警系统 v2 重构 · 第 1 部分：基础层（12 张表）
-- -----------------------------------------------------------------------------
-- 依据：9月13四轮讨论 + 最终回答.docx + v2.0.0改造清单.md（§2.0.1 基础表层清单）
-- 目标库：MySQL 8.0+，InnoDB，utf8mb4 / utf8mb4_0900_ai_ci，ROW_FORMAT=DYNAMIC
-- 约定：
--   1) 主键统一 bigint AUTO_INCREMENT；时间统一 datetime（create_time / update_time）
--   2) 不使用外键约束（与旧库一致，便于迁移与分批导入），关联关系靠索引保证
--   3) 逻辑作废用 is_void / status，不做物理删除
--   4) 基础层只保留"跨学期稳定、不随业务过程变化、被其它表引用"的实体
--   5) 基础层不含 created_by / updated_by（业务表才带审计人）
-- 执行顺序：本文件 → 02_组织与开课 → 03_选课与课次 → 04_考勤 → 05_成绩
--           → 06_引擎产物 → 07_流程与审计 → 08_预置数据
-- =============================================================================

-- CREATE DATABASE IF NOT EXISTS `study_warning_system_v2`
--   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. term 学期（全库时间轴：所有业务表都带 term_id）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `term`;
CREATE TABLE `term` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '学期ID',
  `term_code`    varchar(20)  NOT NULL                COMMENT '学期编码，如 2024-2025-1',
  `school_year`  varchar(20)  NOT NULL                COMMENT '学年，如 2024-2025',
  `term_no`      tinyint      NOT NULL                COMMENT '学期序号：1=秋季 2=春季 3=小学期',
  `term_name`    varchar(30)  NOT NULL                COMMENT '学期显示名，如 2024-2025学年第一学期',
  `start_date`   date         NOT NULL                COMMENT '开学日期',
  `end_date`     date         NOT NULL                COMMENT '结束日期',
  `total_weeks`  int          NOT NULL DEFAULT 16     COMMENT '教学周数（默认值，具体以各开课为准）',
  `is_current`   tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否当前学期：1是 0否（全库只允许一条为1）',
  `status`       varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：PLANNING筹备中/ACTIVE进行中/CLOSED已结束',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_code` (`term_code`),
  KEY `idx_is_current` (`is_current`),
  KEY `idx_year_no` (`school_year`, `term_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '学期表（全库时间轴）';

-- -----------------------------------------------------------------------------
-- 2. term_calendar 教学周历（"第几周""应完成课次""节假日"的唯一真相）
--    用途：覆盖率分母、课表驱动点名定位、"第 N 周"判定（导员阈值第5周起等）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `term_calendar`;
CREATE TABLE `term_calendar` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `term_id`      bigint       NOT NULL                COMMENT '学期ID',
  `week_no`      int          NOT NULL                COMMENT '教学周次（1..total_weeks）',
  `start_date`   date         NOT NULL                COMMENT '本周起始日期（周一）',
  `end_date`     date         NOT NULL                COMMENT '本周结束日期（周日）',
  `week_type`    varchar(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '周类型：NORMAL正常/HOLIDAY假期/EXAM考试周/PRACTICE实践周',
  `holiday_days` int          NOT NULL DEFAULT 0      COMMENT '本周内法定假日天数（用于扣减应上课次）',
  `note`         varchar(100) NULL                    COMMENT '备注，如 国庆假期/校运动会',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_week` (`term_id`, `week_no`),
  KEY `idx_date` (`start_date`, `end_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教学周历表（周次/假期/考试周）';

-- -----------------------------------------------------------------------------
-- 3. sys_account 统一账号（四端：学生/教师/导员/教务）
--    说明：旧库 admin/teacher/student 三张表各自存明文 password，此处统一
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_account`;
CREATE TABLE `sys_account` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '账号ID',
  `login_no`        varchar(30)  NOT NULL                COMMENT '登录号（学号/工号/管理员号，全库唯一）',
  `password_hash`   varchar(100) NOT NULL                COMMENT '密码哈希（BCrypt）',
  `role`            varchar(20)  NOT NULL                COMMENT '角色：STUDENT/TEACHER/COUNSELOR/ADMIN',
  `real_name`       varchar(30)  NULL                    COMMENT '姓名（冗余展示用，可与档案表同步）',
  `status`          varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE正常/DISABLED停用/LOCKED锁定',
  `pwd_update_time` datetime     NULL                    COMMENT '最近改密时间',
  `last_login_time` datetime     NULL                    COMMENT '最近登录时间',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_login_no` (`login_no`),
  KEY `idx_role_status` (`role`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '统一账号表（四端登录）';

-- -----------------------------------------------------------------------------
-- 4. major 专业/学院（Δ 的专业组交互项、高考分分组标准化都依赖它）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `major`;
CREATE TABLE `major` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '专业ID',
  `major_code`     varchar(20)  NOT NULL                COMMENT '专业代码',
  `major_name`     varchar(50)  NOT NULL                COMMENT '专业名称',
  `college_code`   varchar(20)  NULL                    COMMENT '学院代码',
  `college_name`   varchar(50)  NULL                    COMMENT '学院名称',
  `subject_group`  varchar(10)  NULL                    COMMENT '默认首选科目组：物理/历史（用于高考分组内标准化）',
  `status`         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_code` (`major_code`),
  KEY `idx_college` (`college_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '专业/学院表';

-- -----------------------------------------------------------------------------
-- 5. administrative_class 行政班（导员管辖单元 + 挂科率统计粒度之一）
--    注意：不带 counselor_id —— 导员关系放 counselor_class（带 term_id，天然留痕）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `administrative_class`;
CREATE TABLE `administrative_class` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '行政班ID',
  `class_code`   varchar(30)  NOT NULL                COMMENT '班级编码，如 SOFT-2024-1',
  `class_name`   varchar(50)  NOT NULL                COMMENT '班级名称，如 软件1班',
  `major_id`     bigint       NOT NULL                COMMENT '所属专业ID',
  `grade_year`   int          NOT NULL                COMMENT '年级（入学年份），如 2024',
  `status`       varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/GRADUATED/DISMISSED',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_code` (`class_code`),
  KEY `idx_major_grade` (`major_id`, `grade_year`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '行政班表';

-- -----------------------------------------------------------------------------
-- 6. student 学生（去掉 password / class_name；grade 语义纠正为入学年份）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `account_id`     bigint       NULL                    COMMENT '账号ID（迁移期可为空，迁移后应非空）',
  `student_no`     varchar(20)  NOT NULL                COMMENT '学号',
  `student_name`   varchar(30)  NOT NULL                COMMENT '姓名',
  `gender`         tinyint(1)   NULL                    COMMENT '性别：1男 2女 0未知',
  `enroll_year`    int          NULL                    COMMENT '入学年份，如 2024（年级由此推算，不再存"大一上"这类学期值）',
  `major_id`       bigint       NULL                    COMMENT '专业ID（转专业后更新，历史由 audit_log 留痕）',
  `admin_class_id` bigint       NULL                    COMMENT '行政班ID',
  `status`         varchar(20)  NOT NULL DEFAULT 'IN_SCHOOL' COMMENT '学籍状态：IN_SCHOOL在读/SUSPENDED休学/WITHDRAWN退学/GRADUATED毕业',
  `phone`          varchar(20)  NULL,
  `email`          varchar(100) NULL,
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_account` (`account_id`),
  KEY `idx_admin_class` (`admin_class_id`),
  KEY `idx_major` (`major_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '学生表';

-- -----------------------------------------------------------------------------
-- 7. teacher 教师（去掉 password；新增 title 职称）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `account_id`   bigint       NULL                    COMMENT '账号ID',
  `teacher_no`   varchar(20)  NOT NULL                COMMENT '工号',
  `teacher_name` varchar(30)  NOT NULL                COMMENT '姓名',
  `gender`       tinyint(1)   NULL                    COMMENT '性别：1男 2女 0未知',
  `title`        varchar(30)  NULL                    COMMENT '职称：教授/副教授/讲师/助教/未定级（展示用；Δ 收缩由样本量自动决定，不依赖职称）',
  `college_name` varchar(50)  NULL                    COMMENT '所属学院/教研室',
  `phone`        varchar(20)  NULL,
  `email`        varchar(100) NULL,
  `status`       varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/LEFT离职',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`),
  KEY `idx_account` (`account_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教师表';

-- -----------------------------------------------------------------------------
-- 8. counselor 导员（新增角色主体：跨课程、跨教学班，管行政班）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `counselor`;
CREATE TABLE `counselor` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '导员ID',
  `account_id`     bigint       NULL                    COMMENT '账号ID',
  `counselor_no`   varchar(20)  NOT NULL                COMMENT '工号',
  `counselor_name` varchar(30)  NOT NULL                COMMENT '姓名',
  `gender`         tinyint(1)   NULL                    COMMENT '性别：1男 2女 0未知',
  `college_name`   varchar(50)  NULL                    COMMENT '所属学院',
  `phone`          varchar(20)  NULL,
  `email`          varchar(100) NULL,
  `status`         varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/LEFT离职',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_counselor_no` (`counselor_no`),
  KEY `idx_account` (`account_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '导员表';

-- -----------------------------------------------------------------------------
-- 9. counselor_class 导员—行政班（带 term_id：一导员多班、换导员天然留痕）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `counselor_class`;
CREATE TABLE `counselor_class` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `counselor_id`   bigint      NOT NULL                COMMENT '导员ID',
  `admin_class_id` bigint      NOT NULL                COMMENT '行政班ID',
  `term_id`        bigint      NOT NULL                COMMENT '学期ID（同班换导员时按学期留痕）',
  `is_primary`     tinyint(1)  NOT NULL DEFAULT 1      COMMENT '是否主责导员：1主 0副（副导员可多人）',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_term_counselor` (`admin_class_id`, `term_id`, `counselor_id`),
  KEY `idx_counselor` (`counselor_id`, `term_id`),
  KEY `idx_primary` (`admin_class_id`, `term_id`, `is_primary`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '导员-行政班关联表';

-- -----------------------------------------------------------------------------
-- 10. admin 教务/管理员（去掉 password；新增 admin_type）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `account_id`   bigint       NULL                    COMMENT '账号ID',
  `admin_no`     varchar(20)  NOT NULL                COMMENT '管理员号（登录号）',
  `admin_name`   varchar(30)  NOT NULL                COMMENT '姓名',
  `admin_type`   varchar(20)  NOT NULL DEFAULT 'ACADEMIC' COMMENT '类型：ACADEMIC教务/SYS系统管理员',
  `dept_name`    varchar(50)  NULL                    COMMENT '所属部门（教务处等）',
  `phone`        varchar(20)  NULL,
  `email`        varchar(100) NULL,
  `status`       varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_no` (`admin_no`),
  KEY `idx_account` (`account_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教务/管理员表';

-- -----------------------------------------------------------------------------
-- 11. course 课程（科目层：跨学期稳定的"课"，如 大学计算机基础（一））
--     注意：成绩构成比例、总课次都不在这里（分别属于 score_scheme / course_offering）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '课程ID（科目）',
  `course_code`           varchar(30)  NULL                    COMMENT '课程代码',
  `course_name`           varchar(50)  NOT NULL                COMMENT '课程名称',
  `course_type`           varchar(20)  NOT NULL DEFAULT 'EXAM' COMMENT '课程类型：EXAM考试课/ASSESS考查课/EXPERIMENT实验课/PROJECT项目课（决定默认成绩构成模板与通道策略）',
  `credit`                decimal(3,1) NULL                    COMMENT '学分',
  `total_hours`           int          NULL                    COMMENT '计划学时（展示用；课次口径以开课为准）',
  `prerequisite_course_id` bigint      NULL                    COMMENT '先修课程ID（同科目序列，如 高数1→高数2→高数3）',
  `college_name`          varchar(50)  NULL                    COMMENT '开课学院',
  `status`                varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED停开',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_course_type` (`course_type`),
  KEY `idx_prerequisite` (`prerequisite_course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课程（科目）表';

-- -----------------------------------------------------------------------------
-- 12. course_knowledge_point 知识点（跨学期复用的课程内容资产）
--     用途：薄弱知识点、共识难点对比、评分项关联知识点（score_scheme_item.kp_id）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_knowledge_point`;
CREATE TABLE `course_knowledge_point` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '知识点ID',
  `course_id`    bigint       NOT NULL                COMMENT '课程ID',
  `kp_code`      varchar(30)  NULL                    COMMENT '知识点编码（可选）',
  `kp_name`      varchar(100) NOT NULL                COMMENT '知识点名称',
  `parent_id`    bigint       NULL                    COMMENT '父知识点ID（支持章节→知识点两级）',
  `chapter`      varchar(50)  NULL                    COMMENT '所属章节，如 第2章 操作系统基础',
  `kp_level`     tinyint      NOT NULL DEFAULT 3      COMMENT '层级：1章 2节 3知识点',
  `difficulty`   tinyint      NULL                    COMMENT '难度：1易 2中 3难（可选）',
  `description`  text         NULL                    COMMENT '知识点说明（旧库 32 行已有富文本描述，迁移时保留，可供 RAG 与学习建议使用）',
  `sort_no`      int          NOT NULL DEFAULT 0      COMMENT '排序号',
  `status`       varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_kp_code` (`course_id`, `kp_code`),
  KEY `idx_course` (`course_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课程知识点表';

-- =============================================================================
-- 附：旧库 → 基础层 的清洗映射（迁移脚本另出，此处只记规则）
-- -----------------------------------------------------------------------------
-- term         ← 旧 student.grade 的 4 个取值（大一上/大一下/大二上/大二下）
--                映射为全局学期（如 大一上=2024-2025-1），并把 is_current 指向最新
-- term_calendar← 无（教务按校历录入，或按 start_date 自动生成 16 周）
-- sys_account  ← student.password / teacher.password / admin.password（明文 → BCrypt）
-- major        ← 无（新建；由行政班名称推断，如"计算机"）
-- administrative_class ← 旧 student.class_name 去学期后的实体：
--                计算机1班（大一）/计算机2班（大一）/计算机1班（大二）/计算机2班（大二）
--                → class_name=计算机1班…, grade_year=2024/2023, major_id=计算机专业
-- student      ← 旧 student：去掉 grade/class_name，补 enroll_year / admin_class_id / status
-- teacher      ← 旧 teacher：去掉 password，补 account_id / title
-- admin        ← 旧 admin（1 行）：去掉 password，补 account_id
-- course       ← 旧 course：去掉 usual_ratio/mid_ratio/final_ratio/total_class_times/
--                total_homework/total_knowledge；补 course_type（2 门课均为考查课：期中占比为 0）
-- course_knowledge_point ← 旧 32 行，补 chapter/parent_id/kp_level
-- =============================================================================
