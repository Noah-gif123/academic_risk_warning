/* mker.js — build ER / concept diagram specs (auto grid layout) from inline definitions
 * Writes spec/*.json consumed by render.js
 */
'use strict';
const fs = require('fs');
const path = require('path');
const dir = __dirname;

const top = 92, marginX = 46, gapY = 34;

const DOMAINS = [
  {
    file: '209_concept', fig: '图2.9', title: '学情预警系统核心实体关系图（概念级）',
    cols: 4, cw: 262,
    entities: [
      { c: '学期', n: 'term' }, { c: '行政班', n: 'administrative_class' }, { c: '学生', n: 'student' }, { c: '教师', n: 'teacher' },
      { c: '课程', n: 'course' }, { c: '开课实例', n: 'course_offering' }, { c: '教学班', n: 'teaching_class' }, { c: '选课', n: 'enrollment' },
      { c: '课次', n: 'course_session' }, { c: '考勤记录', n: 'attendance_record' }, { c: '分项得分', n: 'score_item' }, { c: '风险状态', n: 'risk_state' },
      { c: '通知', n: 'notification' }, { c: '预警记录', n: 'alert_record' }, { c: '干预记录', n: 'intervention_record' }, { c: '辅导员', n: 'counselor' }
    ],
    edges: [
      { f: '行政班', to: '学生', lf: '1', lt: 'N' },
      { f: '学生', to: '选课', lf: '1', lt: 'N' },
      { f: '教师', to: '教学班', lf: '1', lt: 'N' },
      { f: '课程', to: '开课实例', lf: '1', lt: 'N' },
      { f: '学期', to: '开课实例', lf: '1', lt: 'N' },
      { f: '开课实例', to: '教学班', lf: '1', lt: 'N' },
      { f: '开课实例', to: '课次', lf: '1', lt: 'N' },
      { f: '教学班', to: '选课', lf: '1', lt: 'N' },
      { f: '选课', to: '考勤记录', lf: '1', lt: 'N' },
      { f: '课次', to: '考勤记录', lf: '1', lt: 'N' },
      { f: '选课', to: '分项得分', lf: '1', lt: 'N' },
      { f: '选课', to: '风险状态', lf: '1', lt: '1' },
      { f: '选课', to: '预警记录', lf: '1', lt: 'N' },
      { f: '预警记录', to: '干预记录', lf: '1', lt: 'N' },
      { f: '预警记录', to: '通知', lf: '1', lt: 'N' },
      { f: '辅导员', to: '行政班', lf: 'N', lt: 'M' }
    ],
    caption: '注：概念级实体关系图只画实体与联系，不列字段；各域的完整字段与关系见图2.10～图2.16。'
  },
  {
    file: '210_basic', fig: '图2.10', title: '基础层与组织域实体关系',
    cols: 3, cw: 268,
    entities: [
      { c: '学期 term', n: 'term', rows: ['id*', 'term_code / term_name', 'start_date / end_date', 'total_weeks / is_current'] },
      { c: '教学周历 term_calendar', n: 'term_calendar', rows: ['id*', 'term_id', 'week_no / week_type', 'start_date / end_date', 'holiday_days'] },
      { c: '统一账号 sys_account', n: 'sys_account', rows: ['id*', 'login_no / password_hash', 'role', 'real_name', 'status / last_login_time'] },
      { c: '专业 major', n: 'major', rows: ['id*', 'major_code / major_name', 'college_code / college_name'] },
      { c: '行政班 administrative_class', n: 'administrative_class', rows: ['id*', 'class_code / class_name', 'major_id', 'grade_year / status'] },
      { c: '学生 student', n: 'student', rows: ['id*', 'account_id', 'student_no / student_name', 'enroll_year', 'major_id / admin_class_id'] },
      { c: '教师 teacher', n: 'teacher', rows: ['id*', 'account_id', 'teacher_no / teacher_name', 'title / college_name'] },
      { c: '辅导员 counselor', n: 'counselor', rows: ['id*', 'account_id', 'counselor_no / counselor_name', 'college_name / phone'] },
      { c: '导员-班级 counselor_class', n: 'counselor_class', rows: ['id*', 'counselor_id', 'admin_class_id', 'term_id / is_primary'] }
    ],
    edges: [
      { f: '学期 term', to: '教学周历 term_calendar', lf: '1', lt: 'N' },
      { f: '统一账号 sys_account', to: '学生 student', lf: '1', lt: '1' },
      { f: '专业 major', to: '行政班 administrative_class', lf: '1', lt: 'N' },
      { f: '行政班 administrative_class', to: '学生 student', lf: '1', lt: 'N' },
      { f: '辅导员 counselor', to: '导员-班级 counselor_class', lf: '1', lt: 'N' },
      { f: '行政班 administrative_class', to: '导员-班级 counselor_class', lf: '1', lt: 'N' },
      { f: '学期 term', to: '导员-班级 counselor_class', lf: '1', lt: 'N' }
    ],
    caption: '注：教师、辅导员的 account_id 同样指向统一账号（与 student 一致，图中从简）；账号表是四端登录的唯一入口。'
  },
  {
    file: '211_offering', fig: '图2.11', title: '开课与成绩构成域实体关系',
    cols: 3, cw: 272,
    entities: [
      { c: '课程 course', n: 'course', rows: ['id*', 'course_code / course_name', 'course_type / credit', 'difficulty_factor_code', 'prerequisite_course_id'] },
      { c: '知识点 course_knowledge_point', n: 'course_knowledge_point', rows: ['id*', 'course_id / parent_id', 'kp_code / kp_name', 'chapter / kp_level'] },
      { c: '课程组 course_group', n: 'course_group', rows: ['id*', 'course_id / term_id', 'level', 'level_request_status', 'status'] },
      { c: '开课实例 course_offering', n: 'course_offering', rows: ['id*', 'course_id / term_id / group_id', 'planned_sessions', 'actual_sessions', 'effective_sessions', 'pass_score / status'] },
      { c: '教学班 teaching_class', n: 'teaching_class', rows: ['id*', 'offering_id / teacher_id', 'class_code / class_name', 'capacity', 'is_retake_class / status'] },
      { c: '教学班-行政班 teaching_class_admin_class', n: 'teaching_class_admin_class', rows: ['id*', 'teaching_class_id', 'admin_class_id'] },
      { c: '成绩构成模板 score_template', n: 'score_template', rows: ['id*', 'template_code / template_name', 'course_type / level', 'items_json', 'is_default'] },
      { c: '成绩构成 score_scheme', n: 'score_scheme', rows: ['id*', 'group_id / term_id', 'version / level', 'source_template_id', 'confirm_status', 'locked / locked_at'] },
      { c: '评分项 score_scheme_item', n: 'score_scheme_item', rows: ['id*', 'scheme_id / parent_id', 'item_code / item_name', 'category / weight / full_score', 'is_anchor / is_subjective', 'data_source / score_type'] }
    ],
    edges: [
      { f: '课程 course', to: '知识点 course_knowledge_point', lf: '1', lt: 'N' },
      { f: '课程 course', to: '课程组 course_group', lf: '1', lt: 'N' },
      { f: '课程组 course_group', to: '开课实例 course_offering', lf: '1', lt: 'N' },
      { f: '开课实例 course_offering', to: '教学班 teaching_class', lf: '1', lt: 'N' },
      { f: '教学班 teaching_class', to: '教学班-行政班 teaching_class_admin_class', lf: '1', lt: 'N' },
      { f: '成绩构成模板 score_template', to: '成绩构成 score_scheme', lf: '1', lt: 'N' },
      { f: '课程组 course_group', to: '成绩构成 score_scheme', lf: '1', lt: 'N' },
      { f: '成绩构成 score_scheme', to: '评分项 score_scheme_item', lf: '1', lt: 'N' },
      { f: '评分项 score_scheme_item', to: '知识点 course_knowledge_point', lf: 'N', lt: '1', k: 'dash' }
    ],
    caption: '注：评分项通过 parent_id 自关联形成两级结构；同一层级权重之和必须为 100% 才能锁定。'
  },
  {
    file: '212_attendance', fig: '图2.12', title: '选课、课次与考勤域实体关系',
    cols: 3, cw: 270,
    entities: [
      { c: '选课 enrollment', n: 'enrollment', rows: ['id*', 'student_id', 'teaching_class_id', 'offering_id / term_id', 'status / retake_times'] },
      { c: '课次 course_session', n: 'course_session', rows: ['id*', 'offering_id / teaching_class_id', 'week_no / session_no', 'session_date', 'status / is_void', 'roll_call_status'] },
      { c: '考勤记录 attendance_record', n: 'attendance_record', rows: ['id*', 'enrollment_id / student_id', 'session_id', 'status / leave_type', 'phase / source', 'archive_id', 'recorded_by / is_void'] },
      { c: '归档批次 attendance_archive', n: 'attendance_archive', rows: ['id*', 'offering_id / teaching_class_id', 'teacher_id / term_id', 'phase', 'session_from / session_to', 'coverage_rate', 'status / review_by'] },
      { c: '考勤留痕 attendance_record_log', n: 'attendance_record_log', rows: ['id*', 'record_id', 'old_status / new_status', 'old_phase / new_phase', 'change_type / reason', 'changed_by / changed_at'] }
    ],
    edges: [
      { f: '选课 enrollment', to: '考勤记录 attendance_record', lf: '1', lt: 'N' },
      { f: '课次 course_session', to: '考勤记录 attendance_record', lf: '1', lt: 'N' },
      { f: '归档批次 attendance_archive', to: '考勤记录 attendance_record', lf: '1', lt: 'N' },
      { f: '考勤记录 attendance_record', to: '考勤留痕 attendance_record_log', lf: '1', lt: 'N' }
    ],
    caption: '注：一名学生一次课只有一条考勤记录；“归档”只改变该记录的阶段与来源标记，不另存一份数据，行级改动全部写留痕表。'
  },
  {
    file: '213_score', fig: '图2.13', title: '成绩域实体关系',
    cols: 3, cw: 272,
    entities: [
      { c: '选课 enrollment', n: 'enrollment', rows: ['id*', 'student_id', 'teaching_class_id', 'term_id'] },
      { c: '评分项 score_scheme_item', n: 'score_scheme_item', rows: ['id*', 'scheme_id', 'item_code / item_name', 'category / weight', 'is_anchor / is_subjective'] },
      { c: '分项得分 score_item', n: 'score_item', rows: ['id*', 'enrollment_id / student_id', 'scheme_item_id', 'times_no', 'raw_value / score / full_score', 'is_absent / scorer_id'] },
      { c: '作业提交 homework_item', n: 'homework_item', rows: ['id*', 'enrollment_id / student_id', 'offering_id / scheme_item_id', 'homework_no', 'due_date / submit_time', 'submit_status / is_late'] },
      { c: '成绩单 score_result', n: 'score_result', rows: ['id*', 'enrollment_id / student_id', 'offering_id / term_id', 'weighted_score', 'deduction_points / total_score', 'absent_count / allowed_absent', 'hard_fail / is_fail / fail_reason'] }
    ],
    edges: [
      { f: '选课 enrollment', to: '分项得分 score_item', lf: '1', lt: 'N' },
      { f: '评分项 score_scheme_item', to: '分项得分 score_item', lf: '1', lt: 'N' },
      { f: '选课 enrollment', to: '作业提交 homework_item', lf: '1', lt: 'N' },
      { f: '选课 enrollment', to: '成绩单 score_result', lf: '1', lt: '1' }
    ],
    caption: '注：教师录入的是原始分；折算后的分数只存在于引擎内部，不回写 score_item，成绩单中也不保存折算分。'
  },
  {
    file: '214_engine', fig: '图2.14', title: '引擎产物域实体关系',
    cols: 3, cw: 272,
    entities: [
      { c: '引擎配置 risk_engine_config', n: 'risk_engine_config', rows: ['id*', 'engine_version / effective_time', 'is_active', 'threshold_red / orange / yellow', 'coverage_threshold', 'deduct_absent / late / leave', 'delta_shrink_k / delta_fuse_threshold'] },
      { c: '风险状态 risk_state', n: 'risk_state', rows: ['id*', 'enrollment_id / student_id', 'offering_id / term_id', 'absent_count / late_count / leave_count', 'allowed_absent / remaining_absent', 'hard_fail / near_fail', 'final_exam_needed / final_unreachable', 'risk_score / risk_level', 'active_channel / confidence', 'output_mode / engine_version'] },
      { c: '教师尺度 teacher_style', n: 'teacher_style', rows: ['id*', 'teacher_id / offering_id', 'course_id / term_id', 'scheme_item_id / item_code', 'raw_delta / delta', 'shrink_lambda / sample_size', 'fused / status / engine_version'] },
      { c: '学生历史 student_fail_history', n: 'student_fail_history', rows: ['id*', 'student_id / term_id', 'course_id / offering_id', 'final_score / pass_score', 'is_fail / hard_fail', 'is_retake / engine_version'] },
      { c: '教学班挂科率 teaching_class_fail_rate', n: 'teaching_class_fail_rate', rows: ['id*', 'teaching_class_id / teacher_id', 'term_id', 'examinee_count / fail_count', 'fail_rate / hard_fail_count', 'engine_version / calc_time'] },
      { c: '行政班挂科率 admin_class_fail_rate', n: 'admin_class_fail_rate', rows: ['id*', 'admin_class_id', 'offering_id / term_id', 'examinee_count / fail_count', 'fail_rate / hard_fail_count', 'engine_version'] },
      { c: '课程挂科率 course_fail_rate_history', n: 'course_fail_rate_history', rows: ['id*', 'course_id / term_id', 'examinee_count / fail_count', 'fail_rate / hard_fail_count', 'course_factor', 'is_initialized / engine_version'] }
    ],
    edges: [
      { f: '引擎配置 risk_engine_config', to: '风险状态 risk_state', lf: '1', lt: 'N', l: 'engine_version' },
      { f: '教师尺度 teacher_style', to: '风险状态 risk_state', lf: 'N', lt: 'N', k: 'dash', l: 'Δ 校正' },
      { f: '课程挂科率 course_fail_rate_history', to: '风险状态 risk_state', lf: '1', lt: 'N', k: 'dash', l: '课程因子 c' },
      { f: '学生历史 student_fail_history', to: '风险状态 risk_state', lf: 'N', lt: '1', k: 'dash', l: '历史先验（非新生）' }
    ],
    caption: '注：risk_state 一人一课一行，承载“当前风险”；变化过程不再按天写快照，而由预警生命周期时间线追溯。'
  },
  {
    file: '215_alert', fig: '图2.15', title: '预警与干预闭环域实体关系',
    cols: 3, cw: 272,
    entities: [
      { c: '风险状态 risk_state', n: 'risk_state', rows: ['id*', 'enrollment_id / student_id', 'risk_score / risk_level', 'active_channel / confidence', 'engine_version'] },
      { c: '预警记录 alert_record', n: 'alert_record', rows: ['id*', 'student_id / enrollment_id', 'offering_id / course_id / term_id', 'alert_level / trigger_channel', 'trigger_rule / risk_score', 'confidence / output_mode', 'status / is_current / escalated', 'expire_time / engine_version'] },
      { c: '预警时间线 alert_flow_log', n: 'alert_flow_log', rows: ['id*', 'alert_id / student_id', 'event_type', 'from_level / to_level', 'from_status / to_status', 'operator_id / event_time'] },
      { c: '干预记录 intervention_record', n: 'intervention_record', rows: ['id*', 'student_id / term_id', 'alert_id / related_alert_ids', 'stage / operator_id / operator_role', 'action_type / content / result', 'effective / effect_check_time', 'refer_status / admin_feedback'] },
      { c: '通知 notification', n: 'notification', rows: ['id*', 'biz_type / biz_id', 'title / content', 'aggregate_key / aggregate_count', 'priority / expire_time'] },
      { c: '通知回执 notification_receipt', n: 'notification_receipt', rows: ['id*', 'notification_id', 'recipient_id / recipient_role', 'channel / send_status', 'is_read / ack_status', 'action_taken'] },
      { c: '班集体提醒 class_alert_notification', n: 'class_alert_notification', rows: ['id*', 'admin_class_id / term_id', 'counselor_id', 'rule_level / week_no', 'alert_rate / alert_student_count', 'is_sent / cc_admin'] }
    ],
    edges: [
      { f: '风险状态 risk_state', to: '预警记录 alert_record', lf: '1', lt: 'N', l: '达到阈值时生成' },
      { f: '预警记录 alert_record', to: '预警时间线 alert_flow_log', lf: '1', lt: 'N' },
      { f: '预警记录 alert_record', to: '干预记录 intervention_record', lf: '1', lt: 'N' },
      { f: '预警记录 alert_record', to: '通知 notification', lf: '1', lt: 'N', k: 'dash', l: 'biz_id 关联' },
      { f: '通知 notification', to: '通知回执 notification_receipt', lf: '1', lt: 'N' }
    ],
    caption: '注：通知按 aggregate_key 聚合去重，辅导员一次只收到一条汇总提醒；干预记录以 stage 区分教师、辅导员、教务三级。'
  },
  {
    file: '216_admin', fig: '图2.16', title: '教务流程、问卷与审计域实体关系',
    cols: 3, cw: 272,
    entities: [
      { c: '覆盖率体检 coverage_check_log', n: 'coverage_check_log', rows: ['id*', 'teaching_class_id / offering_id', 'teacher_id / term_id', 'check_type / week_no', 'due_sessions / recorded_sessions', 'coverage_rate / below_threshold', 'notified / resolved'] },
      { c: '考核人数核对 exam_headcount', n: 'exam_headcount', rows: ['id*', 'offering_id / term_id', 'phase', 'roster_count / expected_count', 'confirmed_count / deferred_count', 'confirm_status / check_result'] },
      { c: '导入批次 import_batch', n: 'import_batch', rows: ['id*', 'batch_no / import_type', 'term_id / file_name', 'total_rows / success_rows / failed_rows', 'status / operator_id', 'rollback_by / rollback_time'] },
      { c: '审计日志 audit_log', n: 'audit_log', rows: ['id*', 'biz_type / biz_id', 'action', 'operator_id / operator_role', 'before_json / after_json', 'ip / create_time'] },
      { c: '运营配置 system_config', n: 'system_config', rows: ['id*', 'config_key / config_value', 'config_type / config_group', 'is_editable'] },
      { c: '问卷 questionnaire', n: 'questionnaire', rows: ['id*', 'offering_id / course_id / term_id', 'questionnaire_type / title', 'question_json / is_anonymous', 'collector_type / collector_id', 'open_time / close_time / status'] },
      { c: '问卷作答 questionnaire_answer', n: 'questionnaire_answer', rows: ['id*', 'questionnaire_id', 'respondent_token / student_id', 'target_teacher_id', 'item_code / answer_type', 'score_value / text_value / kp_id'] },
      { c: '问卷流转 questionnaire_flow', n: 'questionnaire_flow', rows: ['id*', 'questionnaire_id / term_id', 'stage / stage_order', 'holder_id / holder_role', 'deadline / status / done_at'] },
      { c: '通知 notification', n: 'notification', rows: ['id*', 'biz_type / biz_id', 'aggregate_key', 'priority / expire_time'] }
    ],
    edges: [
      { f: '问卷 questionnaire', to: '问卷作答 questionnaire_answer', lf: '1', lt: 'N' },
      { f: '问卷 questionnaire', to: '问卷流转 questionnaire_flow', lf: '1', lt: '1' },
      { f: '导入批次 import_batch', to: '考核人数核对 exam_headcount', lf: '1', lt: 'N', k: 'dash', l: '导入来源' },
      { f: '覆盖率体检 coverage_check_log', to: '通知 notification', lf: 'N', lt: '1', k: 'dash', l: '催办通知' }
    ],
    caption: '注：导入批次可整批回滚；问卷按“辅导员收集 → 教务分发 → 课程组全体教师”四个环节流转，流转状态留在问卷流转表中。'
  }
];

for (const d of DOMAINS) {
  const cw = d.cw || 262, cols = d.cols || 3;
  const W = marginX * 2 + cols * cw + (cols - 1) * 40;
  const rows = Math.ceil(d.entities.length / cols);
  const hOf = e => (e.rows && e.rows.length) ? 26 + e.rows.length * 17 : 48;
  const rowH = [];
  for (let r = 0; r < rows; r++) {
    let mx = 0;
    for (let c = 0; c < cols; c++) {
      const e = d.entities[r * cols + c];
      if (e) mx = Math.max(mx, hOf(e));
    }
    rowH.push(mx);
  }
  const bodyH = rowH.reduce((a, b) => a + b, 0) + (rows - 1) * gapY;
  const H = top + bodyH + 96;
  const nodes = [];
  const map = {};
  d.entities.forEach((e, i) => {
    const r = Math.floor(i / cols), c = i % cols;
    let y = top;
    for (let k = 0; k < r; k++) y += rowH[k] + gapY;
    const id = 'e' + i;
    map[e.c] = id;
    nodes.push({ id: id, t: 'entity', x: marginX + c * (cw + 40), y: y, w: cw, h: hOf(e), label: e.c, rows: e.rows || [], fs: 12.5 });
  });
  nodes.unshift({ id: 'bd', t: 'boundary', x: 16, y: 42, w: W - 32, h: top + bodyH + 30 - 42, label: d.title });
  const edges = (d.edges || []).map(e => ({
    f: map[e.f], to: map[e.to], k: e.k || 'assoc', l: e.l || '', lf: e.lf, lt: e.lt, lw: 170
  }));
  const spec = { title: d.fig + ' ' + d.title, w: W, h: H, nodes: nodes, edges: edges, caption: d.caption };
  fs.writeFileSync(path.join(dir, 'spec', d.file + '.json'), JSON.stringify(spec, null, 1), 'utf8');
  console.log('WROTE ' + d.file + '.json  ' + W + 'x' + H + '  entities=' + d.entities.length + ' edges=' + edges.length);
}
