-- =============================================================================
-- 学情预警系统 v2.0 · 第 11 部分：教学资源与课程内容层（5 张表）
-- -----------------------------------------------------------------------------
-- 依赖：01_base_layer.sql（term / student / teacher）、02_org_and_offering.sql
--       （course_offering / teaching_class）、03_enrollment_and_session.sql（course_session）
-- 依据：2026-09 口径冻结版《功能整理》
--       ★ 课程内容（导学 / 教学目标 / 目录 / 资料）由**每位任课教师各自维护自己那份**
--       ★ 目录按课次（course_session）一行，展开为 课前 / 课中 / 课后 三段
--       ★ 本版不做知识点：本层所有表都不带 kp 字段
-- 本层回答四个问题：
--   ① 这门课给学生的“导学”是什么（course_guide，富文本）
--   ② 这门课的教学目标有哪些（teaching_objective，表格展示 + 悬停看全文）
--   ③ 整个学期每次课讲什么（course_lesson，一行 = 一次课）
--   ④ 每次课的课前 / 课中 / 课后挂什么（lesson_resource：资料 / 测验 / 作业 / 项目）
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. course_guide 课程导学
--    说明：① 一教学班一条（uk_teaching_class），教师可反复保存，version 递增；
--          ② 富文本存 longtext（HTML），前端只读渲染；
--          ③ 与“课程简介”区分：课程简介是教务层面的科目说明，本版不做；导学是教师写的课程内容。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_guide`;
CREATE TABLE `course_guide` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID（一位老师一份）',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID（冗余，便于按开课查询）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `content`           longtext     NULL                    COMMENT '导学正文（HTML 富文本，内容随意）',
  `content_format`    varchar(10)  NOT NULL DEFAULT 'HTML'  COMMENT '正文格式：HTML/TEXT',
  `version`           int          NOT NULL DEFAULT 1      COMMENT '版本号（每次保存 +1）',
  `is_published`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已发布给学生：1是',
  `publish_time`      datetime     NULL                    COMMENT '发布时间',
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teaching_class` (`teaching_class_id`),
  KEY `idx_offering` (`offering_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课程导学（每位教师一份）';

-- -----------------------------------------------------------------------------
-- 2. teaching_objective 教学目标
--    说明：① 一教学班多条，按 sort_no 排序展示；
--          ② obj_code 是教师自编的编号（如 ADanaN4），非系统生成；
--          ③ obj_type 用 varchar 而不是枚举：允许教师自定义“能力目标 / 素质目标 / 知识目标”等；
--          ④ 内容较长时前端截断显示、悬停弹出全文，因此 length 给到 1000。
--          ⑤ 本版不设“关联知识技能点”。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teaching_objective`;
CREATE TABLE `teaching_objective` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint        NOT NULL                COMMENT '教学班ID',
  `offering_id`       bigint        NOT NULL                COMMENT '开课ID',
  `term_id`           bigint        NOT NULL                COMMENT '学期ID',
  `obj_code`          varchar(30)   NOT NULL                COMMENT '教学目标编号（教师自编，如 ADanaN4）',
  `obj_content`       varchar(1000) NOT NULL                COMMENT '教学目标内容',
  `obj_type`          varchar(30)   NULL                    COMMENT '目标类型（教师自填，如 能力目标/素质目标）',
  `sort_no`           int           NOT NULL DEFAULT 1      COMMENT '展示顺序',
  `status`            varchar(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `create_by`         bigint        NULL,
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint        NULL,
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tc_sort` (`teaching_class_id`, `sort_no`),
  KEY `idx_offering` (`offering_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '教学目标表';

-- -----------------------------------------------------------------------------
-- 3. resource_file 资源文件（课程资料库）
--    说明：① 只存“文件元信息”，文件本体落在服务器目录或对象存储（storage_type）；
--          ② teaching_class_id 可空：为空表示该教师的公共资源（跨课复用），非空表示属于某教学班；
--          ③ 逻辑作废用 is_void，不做物理删除（删除会破坏已有引用）。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `resource_file`;
CREATE TABLE `resource_file` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NULL                    COMMENT '所属教学班ID（空=教师公共资源）',
  `offering_id`       bigint       NULL                    COMMENT '开课ID（空=公共资源）',
  `term_id`           bigint       NULL                    COMMENT '学期ID（公共资源可为空）',
  `file_name`         varchar(200) NOT NULL                COMMENT '原始文件名（含扩展名）',
  `file_path`         varchar(500) NOT NULL                COMMENT '存储路径或对象 Key',
  `file_ext`          varchar(20)  NULL                    COMMENT '扩展名：pdf/ppt/pptx/doc/docx/xlsx/zip/mp4…',
  `file_size`         bigint       NOT NULL DEFAULT 0      COMMENT '文件大小（字节）',
  `mime_type`         varchar(100) NULL                    COMMENT 'MIME 类型',
  `storage_type`      varchar(20)  NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式：LOCAL本地目录/OSS对象存储',
  `upload_by`         bigint       NOT NULL                COMMENT '上传人（教师ID）',
  `upload_role`       varchar(20)  NOT NULL DEFAULT 'TEACHER' COMMENT '上传人角色',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_void`           tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否作废：1是（逻辑删除）',
  `void_reason`       varchar(200) NULL                    COMMENT '作废原因',
  PRIMARY KEY (`id`),
  KEY `idx_tc` (`teaching_class_id`),
  KEY `idx_upload` (`upload_by`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '资源文件表（课程资料库）';

-- -----------------------------------------------------------------------------
-- 4. course_lesson 目录课时（一行 = 一次课）
--    说明：① 目录是本系统课程内容的主干，学生端按课时折叠展开；
--          ② session_id 必填：课时**绑定课次**，这样课中测验/作业能自动挂到当次课，
--             并与考勤点名、覆盖率的分母对齐；
--          ③ 课次框架由教务按课表生成，教师可调整标题与顺序（sort_no）；
--          ④ 教师如需“一次课拆成两次”，由教务调整实际课次后自动多出一行，不在本表硬拆。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_lesson`;
CREATE TABLE `course_lesson` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID',
  `offering_id`       bigint       NOT NULL                COMMENT '开课ID',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `session_id`        bigint       NOT NULL                COMMENT '课次ID（course_session，一次课一行）',
  `week_no`           smallint     NOT NULL                COMMENT '第几周（冗余，便于展示与排序）',
  `session_no`        smallint     NOT NULL                COMMENT '第几次课（冗余）',
  `title`             varchar(500) NULL                    COMMENT '本次课要讲的内容标题（可含多条，用分号连接）',
  `sort_no`           int          NOT NULL DEFAULT 1      COMMENT '目录顺序',
  `status`            varchar(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tc_session` (`teaching_class_id`, `session_id`),
  KEY `idx_tc_week` (`teaching_class_id`, `week_no`, `sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '目录课时表（一行=一次课）';

-- -----------------------------------------------------------------------------
-- 5. lesson_resource 课时资源与任务（课前 / 课中 / 课后）
--    说明：① 三段用 phase 区分：BEFORE 课前预习 / DURING 课中随堂 / AFTER 课后作业；
--          ② res_type 决定 ref_id 指向谁：
--             FILE / LINK / TEXT → resource_file.id 或直接存 ext_url
--             PAPER             → paper.id（测验或作业，见 12_assessment.sql）
--             PROJECT           → project_task.id（见 13_project.sql）
--          ③ publish_status 决定学生端是否可见：未发布的任务学生看不到。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `lesson_resource`;
CREATE TABLE `lesson_resource` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lesson_id`         bigint       NOT NULL                COMMENT '课时ID（course_lesson）',
  `teaching_class_id` bigint       NOT NULL                COMMENT '教学班ID（冗余）',
  `term_id`           bigint       NOT NULL                COMMENT '学期ID',
  `phase`             varchar(10)  NOT NULL                COMMENT '阶段：BEFORE课前/DURING课中/AFTER课后',
  `res_type`          varchar(20)  NOT NULL                COMMENT '资源类型：FILE文件/LINK外链/TEXT富文本/PAPER测验或作业/PROJECT项目',
  `ref_id`            bigint       NULL                    COMMENT '关联对象ID（FILE→resource_file / PAPER→paper / PROJECT→project_task）',
  `ext_url`           varchar(500) NULL                    COMMENT '外链地址（LINK 时使用；视频/网页）',
  `title`             varchar(200) NOT NULL                COMMENT '展示标题',
  `sort_no`           int          NOT NULL DEFAULT 1      COMMENT '同组内顺序',
  `publish_status`    varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT草稿/PUBLISHED已发布',
  `publish_time`      datetime     NULL                    COMMENT '发布时间',
  `create_by`         bigint       NULL,
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         bigint       NULL,
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lesson_phase` (`lesson_id`, `phase`, `sort_no`),
  KEY `idx_tc` (`teaching_class_id`),
  KEY `idx_ref` (`res_type`, `ref_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT = '课时资源与任务表（课前/课中/课后）';
