-- =============================================================================
-- 32_seed_courseware.sql  第三阶段 M11 补强：演示课件（PPTX + DOCX）
--   作用：给教学班 201（大学计算机基础（一））挂两份"课件类"资料，
--         用来演示「教师上传课件 → 知识库自动入库 → 学生答疑带页码引用」。
--   前置：先准备文件本体（二选一）
--     ① 运行 tool/courseware 下的生成器（见该目录 README.md），
--        产物在 v2.0/demo/courseware/；
--     ② 直接用 demo/courseware 里已生成好的两份文件。
--     然后把它们复制到后端上传目录（相对 file-store 的路径必须与下面 file_path 一致）：
--        v2.0/demo/courseware/第2讲_Excel函数与图表.pptx  → v2.0/backend/file-store/201/demo_lecture02.pptx
--        v2.0/demo/courseware/实验二_成绩统计操作指导.docx → v2.0/backend/file-store/201/demo_exp02.docx
--   执行后需要触发一次知识库入库：教师端 POST /api/teacher/ai/kb/sync?courseId=1
--   （教师端上传接口也会"上传即入库"，本脚本只是模拟"课程资料已在库里"的初始状态）
--   幂等：先按 file_path 删除旧行与其知识库分块，再重新插入。
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 清掉旧的同名资料及其知识库分块（分块表要先删，避免留下孤儿片段参与检索）
DELETE c FROM `ai_kb_chunk` c
    JOIN `resource_file` r ON r.id = c.source_id AND c.source_type = 'RESOURCE'
WHERE r.file_path IN ('201/demo_lecture02.pptx', '201/demo_exp02.docx');

DELETE d FROM `ai_kb_doc` d
    JOIN `resource_file` r ON r.id = d.source_id AND d.source_type = 'RESOURCE'
WHERE r.file_path IN ('201/demo_lecture02.pptx', '201/demo_exp02.docx');

DELETE FROM `resource_file`
WHERE file_path IN ('201/demo_lecture02.pptx', '201/demo_exp02.docx');

-- 2) 插入两份课件（教学班 201 / 开课 101 / 学期 5 / 上传人：教师 1）
INSERT INTO `resource_file`
(`teaching_class_id`, `offering_id`, `term_id`, `file_name`, `file_path`, `file_ext`, `file_size`,
 `mime_type`, `storage_type`, `upload_by`, `upload_role`, `is_void`, `create_time`)
VALUES
(201, 101, 5, '第2讲_Excel函数与图表.pptx', '201/demo_lecture02.pptx', 'pptx', 42208,
 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 'LOCAL', 1, 'TEACHER', 0, NOW()),
(201, 101, 5, '实验二_成绩统计操作指导.docx', '201/demo_exp02.docx', 'docx', 4439,
 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'LOCAL', 1, 'TEACHER', 0, NOW());

SELECT id, file_name, file_path, file_ext, file_size
FROM `resource_file`
WHERE file_path IN ('201/demo_lecture02.pptx', '201/demo_exp02.docx');
