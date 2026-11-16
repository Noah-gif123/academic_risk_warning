-- v3.0 固定录制演示库清理
-- 目标：1 学期 / 1 行政班 / 12 学生 / 4 课程 / 4 教学班 / 48 选课
-- 只对 study_warning_system_demo 执行，不操作 study_warning_system_v3

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- 基础组织
DELETE FROM student WHERE id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022);
DELETE FROM administrative_class WHERE id <> 1;
DELETE FROM teacher WHERE id <> 1;
DELETE FROM counselor WHERE id <> 1;
DELETE FROM admin WHERE id <> 1;
DELETE FROM counselor_class WHERE admin_class_id <> 1 OR counselor_id <> 1 OR term_id <> 5;
DELETE FROM term WHERE id <> 5;
DELETE FROM term_calendar WHERE term_id <> 5;

-- 课程与教学班
DELETE FROM course WHERE id NOT IN (1,4,7,10);
DELETE FROM course_offering WHERE id NOT IN (101,102,103,104);
DELETE FROM teaching_class WHERE id NOT IN (201,202,203,204);
UPDATE teaching_class SET teacher_id = 1 WHERE id IN (202,203,204);
DELETE FROM teaching_class_admin_class WHERE admin_class_id <> 1 OR teaching_class_id NOT IN (201,202,203,204);

-- 选课 / 风险 / 分数 / 考勤
DELETE FROM enrollment WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                         OR teaching_class_id NOT IN (201,202,203,204);
DELETE FROM risk_state WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                         OR teaching_class_id NOT IN (201,202,203,204);
DELETE FROM alert_record WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                           OR teaching_class_id NOT IN (201,202,203,204);
DELETE FROM alert_flow_log WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                             OR term_id <> 5;
DELETE FROM intervention_record WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                                 OR term_id <> 5;
DELETE FROM score_result WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                           OR offering_id NOT IN (101,102,103,104);
DELETE FROM score_item WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                         OR term_id <> 5;
DELETE FROM attendance_record WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                               OR term_id <> 5;
DELETE FROM attendance_record_log WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022);
DELETE FROM student_fail_history WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022)
                                   OR course_id NOT IN (1,4,7,10);
DELETE FROM student_memory WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022);
DELETE FROM student_memory_event WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022);

-- 教学运行
DELETE FROM course_lesson WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM course_session WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM attendance_archive WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM course_guide WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM coverage_check_log WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM teaching_objective WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM lesson_resource WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM resource_file WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM course_join_request WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM paper WHERE teaching_class_id NOT IN (201,202,203,204);
DELETE FROM submission WHERE teaching_class_id NOT IN (201,202,203,204) OR student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022);
DELETE FROM project_submission WHERE student_id NOT IN (1,1001,1002,1004,1006,1008,1012,1014,1018,1019,1020,1022);
TRUNCATE TABLE project_contribution;
TRUNCATE TABLE project_team;
TRUNCATE TABLE project_task;
TRUNCATE TABLE question;
TRUNCATE TABLE submission_answer;

-- 报告 / 预警解释 / AI 运行：清空后由演示流程重新生成
TRUNCATE TABLE ai_group_report;
TRUNCATE TABLE ai_report;
TRUNCATE TABLE ai_report_feedback;
TRUNCATE TABLE ai_alert_explain;
TRUNCATE TABLE ai_evidence_ledger;
TRUNCATE TABLE ai_run;
TRUNCATE TABLE ai_run_step;
TRUNCATE TABLE ai_usage;
TRUNCATE TABLE ai_grading_suggest;
TRUNCATE TABLE ai_question_draft;
TRUNCATE TABLE ai_qa_feedback;
TRUNCATE TABLE ai_qa_log;
TRUNCATE TABLE ai_qa_message;
TRUNCATE TABLE ai_qa_session;

-- 问卷 / 通知 / 审计：录制主链路不使用
TRUNCATE TABLE questionnaire;
TRUNCATE TABLE questionnaire_answer;
TRUNCATE TABLE questionnaire_flow;
TRUNCATE TABLE notification;
TRUNCATE TABLE notification_receipt;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE import_batch;

-- 物化统计：启动后重新计算
TRUNCATE TABLE stat_daily_class;
TRUNCATE TABLE stat_weekly_kpi;
TRUNCATE TABLE stat_intervention_effect;

-- 课程级/教师级历史辅助表
DELETE FROM admin_class_fail_rate WHERE admin_class_id <> 1 OR course_id NOT IN (1,4,7,10) OR term_id <> 5;
DELETE FROM teaching_class_fail_rate WHERE teaching_class_id NOT IN (201,202,203,204) OR term_id <> 5;
DELETE FROM course_fail_rate_history WHERE course_id NOT IN (1,4,7,10) OR term_id <> 5;
DELETE FROM teacher_style WHERE teacher_id <> 1 OR term_id <> 5;
DELETE FROM class_alert_notification WHERE admin_class_id <> 1 OR term_id <> 5;

SET FOREIGN_KEY_CHECKS = 1;
