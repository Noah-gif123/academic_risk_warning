/* seed_demo.js — 生成 20_rebuild_demo.sql（演示库「一条主线 + 一段历史」重建）
 *
 * 主线 term 5（2026-2027-1 · current · ACTIVE）
 *   3 个行政班 × 45 人 = 135 学生（CS-2025-1 / CS-2025-2 / CS-2024-1）
 *   5 门课 · 12 个教学班 · 选课 540 · 课次 192（点名 12/16）· 考勤 6480
 *   分项得分 4320 · 成绩单 540（挂科含 1/3 硬门槛）
 *   试卷 10 张（8 已批 / 1 张 3 人待批 / 1 张 4 人缺考）· 提交 450 · 逐题 2700
 *   风险状态 540 · 预警 40 · 时间线 80 · 干预 12 · 三维挂科率 · 问卷 + 四环节
 * 历史 term 4（2025-2026-2 · CLOSED）
 *   2024 级 45 人 × 2 门课 = 90 成绩，12 人挂科 → 学生挂科历史
 * 合成 term 8：不触碰（由 19_cleanup_demo.sql 规整）
 *
 * 用法: node seed_demo.js → 20_rebuild_demo.sql（再 mysql source 执行）
 */
'use strict';
const fs = require('fs');
const path = require('path');

// ---------- 确定性随机 ----------
let seed = 20261022;
const rnd = () => { seed = (seed * 1664525 + 1013904223) % 4294967296; return seed / 4294967296; };
const between = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const r1 = (x) => Math.round(x * 10) / 10;
const r2 = (x) => Math.round(x * 100) / 100;

// ---------- SQL 输出（对象式：只写必要列，其余走默认值） ----------
const out = [];
out.push('USE study_warning_system_v2;');
out.push('-- 20_rebuild_demo.sql — 由 seed_demo.js 生成，请勿手改');
const esc = (v) => {
  if (v === null || v === undefined) return 'NULL';
  if (typeof v === 'number') return String(v);
  if (typeof v === 'boolean') return v ? '1' : '0';
  return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
};
const del = (s) => out.push(s + ';');
function ins(table, rows, chunk = 150) {
  if (!rows.length) return;
  const cols = Object.keys(rows[0]);
  for (let i = 0; i < rows.length; i += chunk) {
    out.push('INSERT INTO ' + table + ' (' + cols.join(', ') + ') VALUES\n'
      + rows.slice(i, i + chunk).map((r) => '  (' + cols.map((c) => esc(r[c])).join(', ') + ')').join(',\n') + ';');
  }
}

// ---------- 基础 ----------
const TERM5_START = new Date('2026-09-01T08:00:00');
const weekDate = (start, w) => {
  const d = new Date(start.getTime());
  d.setDate(d.getDate() + (w - 1) * 7);
  return d.toISOString().slice(0, 10);
};
const T5 = '2026-09-01 00:00:00';
const T_NOW = '2026-10-20 08:00:00';

const ADMIN_CLASSES = [
  { id: 1, code: 'CS-2025-1', name: '计算机1班', grade: 2025, size: 45, prefix: '2025', from: 1 },
  { id: 2, code: 'CS-2025-2', name: '计算机2班', grade: 2025, size: 45, prefix: '2025', from: 46 },
  { id: 3, code: 'CS-2024-1', name: '计算机1班', grade: 2024, size: 45, prefix: '2024', from: 1 }
];
const SURNAME = '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜'.split('');
const GIVEN = ['伟', '芳', '娜', '敏', '静', '丽', '强', '磊', '洋', '艳', '勇', '军', '杰', '娟', '涛', '明', '超', '秀英', '霞', '平'];

const STUDENTS = [];
let nextSid = 1001;
ADMIN_CLASSES.forEach((ac) => {
  for (let i = 0; i < ac.size; i++) {
    const keep = ac.id === 1 && i === 0;
    STUDENTS.push({
      id: keep ? 1 : nextSid++,
      no: keep ? '20250001' : ac.prefix + String(ac.from + i).padStart(4, '0'),
      name: keep ? '学生1' : pick(SURNAME) + pick(GIVEN),
      gender: rnd() < 0.55 ? 1 : 2,
      year: ac.grade,
      acId: ac.id
    });
  }
});
const studentsOf = (acId) => STUDENTS.filter((s) => s.acId === acId);

const COURSES = [
  { key: 'A', courseId: 1, name: '大学计算机基础', offeringId: 101, schemeId: 601 },
  { key: 'B', courseId: 4, name: '高等数学', offeringId: 102, schemeId: 602 },
  { key: 'C', courseId: 6, name: '程序设计基础', offeringId: 103, schemeId: 603 },
  { key: 'D', courseId: 7, name: '数据结构', offeringId: 104, schemeId: 604 },
  { key: 'E', courseId: 8, name: '计算机网络', offeringId: 105, schemeId: 605 }
];

let tcid = 201;
const CLASSES = [];
COURSES.forEach((c) => {
  const teacherBase = { A: 1, B: 2, C: 3, D: 4, E: 5 }[c.key];
  [1, 2].forEach((acId) => CLASSES.push({
    id: tcid++, offeringId: c.offeringId, key: c.key, acId, retake: 0, teacherId: teacherBase,
    code: (acId === 1 ? 'CS-01-' : 'CS-02-') + c.key,
    name: c.name + (acId === 1 ? ' 01班' : ' 02班')
  }));
  if (c.key === 'C' || c.key === 'D') CLASSES.push({
    id: tcid++, offeringId: c.offeringId, key: c.key, acId: 3, retake: 1, teacherId: teacherBase,
    code: 'CS-RT-' + c.key, name: c.name + ' 重修班'
  });
});

// ---------- 0) 清理 ----------
out.push('-- 0) 清空主线（term 4/5）与派生数据');
[
  'DELETE sa FROM submission_answer sa JOIN submission s ON s.id = sa.submission_id WHERE s.term_id IN (4,5)',
  'DELETE FROM submission WHERE term_id IN (4,5)',
  'DELETE FROM question WHERE paper_id IN (SELECT id FROM paper WHERE term_id IN (4,5))',
  'DELETE FROM paper WHERE term_id IN (4,5)',
  'DELETE FROM attendance_record WHERE term_id IN (4,5)',
  'DELETE FROM attendance_archive WHERE term_id IN (4,5)',
  'DELETE FROM attendance_record_log',
  'DELETE FROM course_session WHERE term_id IN (4,5)',
  'DELETE FROM score_item WHERE term_id IN (4,5)',
  'DELETE FROM score_result WHERE term_id IN (4,5)',
  'DELETE FROM score_scheme_item WHERE scheme_id IN (SELECT id FROM score_scheme WHERE term_id IN (4,5))',
  'DELETE FROM score_scheme WHERE term_id IN (4,5)',
  'DELETE FROM enrollment WHERE term_id IN (4,5)',
  'DELETE FROM risk_state WHERE term_id IN (4,5)',
  'DELETE FROM alert_flow_log',
  'DELETE FROM intervention_record',
  'DELETE FROM alert_record WHERE term_id IN (4,5)',
  'DELETE FROM class_alert_notification',
  'DELETE FROM student_fail_history',
  'DELETE FROM course_fail_rate_history',
  'DELETE FROM teaching_class_fail_rate',
  'DELETE FROM admin_class_fail_rate',
  'DELETE FROM teacher_style',
  'DELETE FROM questionnaire_answer',
  'DELETE FROM questionnaire_flow',
  'DELETE FROM questionnaire',
  'DELETE FROM counselor_class WHERE term_id IN (4,5)',
  'DELETE FROM teaching_class_admin_class WHERE teaching_class_id >= 100',
  'DELETE FROM teaching_class WHERE id >= 100',
  'DELETE FROM course_lesson',
  'DELETE FROM course_guide',
  'DELETE FROM teaching_objective',
  'DELETE FROM course_offering WHERE term_id IN (4,5)',
  'DELETE FROM course_group WHERE term_id IN (4,5)',
  'DELETE FROM project_contribution',
  'DELETE FROM project_submission',
  'DELETE FROM project_team',
  'DELETE FROM project_task',
  'DELETE FROM course_join_request',
  'DELETE FROM exam_headcount',
  'DELETE FROM coverage_check_log',
  'DELETE FROM import_batch',
  'DELETE FROM audit_log',
  'DELETE FROM notification_receipt',
  'DELETE FROM notification',
  'DELETE FROM sys_account WHERE id IN (SELECT account_id FROM student WHERE id <> 1 AND account_id IS NOT NULL)',
  'DELETE FROM student WHERE id <> 1',
  'DELETE FROM administrative_class WHERE id = 4'
].forEach(del);
del('DELETE FROM course WHERE id = 2'); // 大学计算机基础（二）：无开课、无引用

// ---------- 1) 行政班 ----------
out.push('-- 1) 行政班（3 个班 × 45 人）');
del('UPDATE student SET admin_class_id=1, student_name=\'学生1\', enroll_year=2025, gender=1, status=\'IN_SCHOOL\' WHERE id=1');
del('UPDATE sys_account SET login_no=\'20250001\', real_name=\'学生1\', status=\'ACTIVE\' WHERE id=631');
del('DELETE FROM administrative_class');
ins('administrative_class', [
  { id: 1, class_code: 'CS-2025-1', class_name: '计算机1班', grade_year: 2025, major_id: 1, status: 'ACTIVE', create_time: T5, update_time: T5 },
  { id: 2, class_code: 'CS-2025-2', class_name: '计算机2班', grade_year: 2025, major_id: 1, status: 'ACTIVE', create_time: T5, update_time: T5 },
  { id: 3, class_code: 'CS-2024-1', class_name: '计算机1班', grade_year: 2024, major_id: 1, status: 'ACTIVE', create_time: T5, update_time: T5 }
]);

// ---------- 2) 学生 + 账号 ----------
out.push('-- 2) 学生与账号（135 人；演示学生 20250001 复用 id=1）');
ins('student', STUDENTS.filter((s) => s.id !== 1).map((s) => ({
  id: s.id, account_id: s.id + 1000, student_no: s.no, student_name: s.name, gender: s.gender,
  enroll_year: s.year, major_id: 1, admin_class_id: s.acId, status: 'IN_SCHOOL',
  create_time: '2026-08-25 10:00:00', update_time: '2026-08-25 10:00:00'
})));
out.push("INSERT INTO sys_account (id, login_no, password_hash, role, real_name, status, create_time, update_time)\n"
  + "SELECT s.id + 1000, s.student_no, a.password_hash, 'STUDENT', s.student_name, 'ACTIVE', NOW(), NOW()\n"
  + "  FROM student s JOIN sys_account a ON a.login_no = '20250001'\n"
  + " WHERE s.id <> 1;");
del('UPDATE student SET account_id = 631 WHERE id = 1');

// ---------- 3) 课程组 / 开课 / 教学班 ----------
out.push('-- 3) 课程组、开课实例、教学班、导员带班');
ins('course_group', COURSES.map((c) => ({
  id: c.offeringId, course_id: c.courseId, term_id: 5, level: 'COLLEGE',
  level_request_status: 'APPROVED', status: 'ACTIVE', create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
})));
ins('course_offering', COURSES.map((c) => ({
  id: c.offeringId, course_id: c.courseId, term_id: 5, group_id: c.offeringId,
  planned_weeks: 16, planned_times_per_week: 1, planned_sessions: 16,
  actual_weeks: 16, actual_sessions: 16, actual_submit_status: 'SUBMITTED',
  actual_submit_by: 1442, actual_submit_time: '2026-12-18 09:00:00', actual_confirm_by: 1442,
  actual_confirm_time: '2026-12-19 09:00:00', effective_sessions: 16, effective_source: 'ACTUAL',
  pass_score: 60, status: 'ACTIVE', create_by: 1442,
  create_time: '2026-08-20 09:10:00', update_time: '2026-12-19 09:00:00'
})));
ins('teaching_class', CLASSES.map((c, i) => ({
  id: c.id, offering_id: c.offeringId, teacher_id: c.teacherId, class_code: c.code, class_name: c.name,
  capacity: 60, is_retake_class: c.retake, invite_code: String(310100 + i), join_review_enabled: 0,
  status: 'ACTIVE', create_by: 1, create_time: '2026-08-21 09:00:00', update_time: '2026-08-21 09:00:00'
})));
ins('teaching_class_admin_class', CLASSES.map((c) => ({ teaching_class_id: c.id, admin_class_id: c.acId })));
ins('counselor_class', [
  { admin_class_id: 1, term_id: 5, counselor_id: 1, create_time: T5, update_time: T5 },
  { admin_class_id: 3, term_id: 5, counselor_id: 1, create_time: T5, update_time: T5 },
  { admin_class_id: 2, term_id: 5, counselor_id: 2, create_time: T5, update_time: T5 }
]);

// ---------- 4) 选课 ----------
out.push('-- 4) 选课（540：2025 级 90×5 + 2024 级重修 45×2）');
const ENROLL = [];
let eid = 20001;
CLASSES.forEach((c) => studentsOf(c.acId).forEach((s) => ENROLL.push({
  id: eid++, studentId: s.id, no: s.no, name: s.name, tcId: c.id, offeringId: c.offeringId,
  key: c.key, acId: c.acId, retake: c.retake
})));
ins('enrollment', ENROLL.map((e) => ({
  id: e.id, student_id: e.studentId, teaching_class_id: e.tcId, offering_id: e.offeringId, term_id: 5,
  status: 'ENROLLED', retake_times: e.retake, enroll_time: '2026-08-25 14:00:00',
  create_by: 1, create_time: '2026-08-25 14:00:00', update_time: '2026-08-25 14:00:00'
})));

// ---------- 5) 成绩构成 ----------
out.push('-- 5) 成绩构成（5 项合计 100%，已锁定；考勤不计入评分项）');
const IT = [
  { code: 'HW', name: '课后作业', cat: 'HOMEWORK', w: 20, times: 3, auto: 'AUTO', subj: 0 },
  { code: 'LAB', name: '实验', cat: 'EXPERIMENT', w: 15, times: 2, auto: 'MANUAL', subj: 1 },
  { code: 'PERF', name: '课堂表现', cat: 'PERFORMANCE', w: 10, times: 1, auto: 'MANUAL', subj: 1 },
  { code: 'MID', name: '期中测验', cat: 'MIDTERM', w: 20, times: 1, auto: 'AUTO', subj: 0 },
  { code: 'FINAL', name: '期末考核', cat: 'FINAL', w: 35, times: 1, auto: 'AUTO', subj: 0 }
];
ins('score_scheme', COURSES.map((c) => ({
  id: c.schemeId, group_id: c.offeringId, term_id: 5, version: 1, level: 'COLLEGE',
  total_weight: 100, confirm_status: 'LOCKED', confirm_total: 100, confirm_done: 1,
  locked: 1, locked_by: 1431, locked_at: '2026-08-28 10:00:00', lock_type: 'COLLEGE',
  create_by: 1431, create_time: '2026-08-28 09:00:00', update_time: '2026-08-28 10:00:00'
})));
const ITEM_ID = {};
let siid = 701;
const SI = [];
COURSES.forEach((c) => {
  ITEM_ID[c.schemeId] = {};
  IT.forEach((it, i) => {
    ITEM_ID[c.schemeId][it.code] = siid;
    SI.push({
      id: siid++, scheme_id: c.schemeId, parent_id: null, item_level: 1, item_code: it.code, item_name: it.name,
      category: it.cat, weight: it.w, full_score: 100, data_source: it.auto, score_type: 'NUMERIC',
      expected_times: it.times, aggregate_mode: 'AVG', is_anchor: 0, is_subjective: it.subj,
      scorer_role: 'TEACHER', sort_no: (i + 1) * 10, status: 'ACTIVE',
      create_by: 1431, create_time: '2026-08-28 09:00:00', update_time: '2026-08-28 09:00:00'
    });
  });
});
ins('score_scheme_item', SI);

// ---------- 6) 课次 ----------
out.push('-- 6) 课次（每班 16 周；第 13~16 周未点名 → 覆盖率 75%）');
const SESS = [];
let sessId = 40001;
CLASSES.forEach((c) => {
  for (let w = 1; w <= 16; w++) SESS.push({
    id: sessId++, tcId: c.id, offeringId: c.offeringId, week: w,
    date: weekDate(TERM5_START, w), called: w <= 12
  });
});
ins('course_session', SESS.map((s) => ({
  id: s.id, offering_id: s.offeringId, teaching_class_id: s.tcId, term_id: 5, week_no: s.week, session_no: s.week,
  session_date: s.date, start_time: '08:00', end_time: '09:40',
  location: '教1-' + (101 + (s.tcId % 18)), session_type: 'NORMAL', status: 'NORMAL',
  roll_call_status: s.called ? 'CALLED' : 'NOT_CALLED', roll_call_time: s.called ? s.date + ' 09:45:00' : null,
  roll_call_by: s.called ? 1431 : null, roll_call_count: s.called ? 45 : 0, is_void: 0,
  create_by: 1, create_time: '2026-08-26 09:00:00', update_time: '2026-08-26 09:00:00'
})));

// ---------- 7) 考勤 ----------
out.push('-- 7) 考勤（已点名课次逐人一条；CS-01 前 9 人预留 1/3 硬门槛）');
const HARD = new Set(studentsOf(1).slice(0, 9).map((s) => s.id));
const ATT = [];
const absentCount = {};
let attId = 500001;
SESS.filter((s) => s.called).forEach((s) => {
  const c = CLASSES.find((x) => x.id === s.tcId);
  studentsOf(c.acId).forEach((st) => {
    const e = ENROLL.find((x) => x.studentId === st.id && x.tcId === c.id);
    const hard = HARD.has(st.id) && c.key === 'A';
    const r = rnd();
    let status = 'PRESENT';
    if (r < (hard ? 0.55 : 0.02)) status = 'ABSENT';
    else if (r < (hard ? 0.60 : 0.06)) status = 'LATE';
    else if (r < (hard ? 0.63 : 0.09)) status = 'LEAVE';
    if (status === 'ABSENT') absentCount[e.id] = (absentCount[e.id] || 0) + 1;
    ATT.push({
      id: attId++, enrollment_id: e.id, student_id: st.id, session_id: s.id, term_id: 5, status,
      leave_type: status === 'LEAVE' ? pick(['PERSONAL', 'SICK', 'OFFICIAL']) : null,
      phase: 'NORMAL', source: 'TEACHER', recorded_by: 1431, recorded_role: 'TEACHER',
      recorded_at: s.date + ' 09:45:00', batch_key: 'RC-' + s.id, is_void: 0,
      create_time: '2026-09-01 10:00:00', update_time: '2026-09-01 10:00:00'
    });
  });
});
ins('attendance_record', ATT);

// 期中归档批次（已审核，覆盖率 75%）
ins('attendance_archive', [{
  id: 1, offering_id: 101, teaching_class_id: 201, teacher_id: 1, term_id: 5, phase: 'MID',
  window_open_time: '2026-10-12 08:00:00', deadline: '2026-10-16 23:59:00',
  submit_time: '2026-10-15 10:00:00', submit_by: 1431, submit_note: '期中前 8 周课次已全部点名',
  session_from: 1, session_to: 8, session_count: 8, record_count: 360, coverage_rate: 75.00,
  status: 'REVIEWED', review_by: 1442, review_time: '2026-10-16 09:00:00', review_note: '记录完整，通过',
  remind_count: 0, is_void: 0, create_time: '2026-10-12 08:00:00', update_time: '2026-10-16 09:00:00'
}]);

// ---------- 8) 分项得分 + 成绩单 ----------
out.push('-- 8) 分项得分与成绩单（W − P；缺课 > 1/3 判硬门槛）');
const ALLOWED = 5; // floor(16/3)
// 3 名“多门不及格”学生：让导员端「重点关注（≥3 门风险课程）」有数据
const WEAK = new Set(studentsOf(1).slice(10, 13).map((s) => s.id));
const SCORE_ITEMS = [];
const RESULTS = [];
let sciId = 600001, resId = 700001;
ENROLL.forEach((e) => {
  const schemeId = COURSES.find((c) => c.key === e.key).schemeId;
  const hard = HARD.has(e.studentId) && e.key === 'A';
  const weak = WEAK.has(e.studentId);
  const ability = hard || weak ? 45 + rnd() * 8 : 64 + rnd() * 34;
  let weighted = 0;
  IT.forEach((it) => {
    let sum = 0;
    for (let t = 1; t <= it.times; t++) {
      const sc = Math.max(0, Math.min(100, r1(ability + (rnd() - 0.5) * 18)));
      sum += sc;
      SCORE_ITEMS.push({
        id: sciId++, enrollment_id: e.id, student_id: e.studentId, scheme_item_id: ITEM_ID[schemeId][it.code],
        term_id: 5, times_no: t, score: sc, full_score: 100, is_absent: 0, source: it.auto,
        scorer_id: 1431, scorer_role: 'TEACHER', scored_at: '2026-10-10 10:00:00', is_void: 0,
        create_by: 1431, create_time: '2026-10-10 10:00:00', update_time: '2026-10-10 10:00:00'
      });
    }
    weighted += (sum / it.times) * (it.w / 100);
  });
  const absent = absentCount[e.id] || 0;
  const P = absent * 2;
  const T = r2(weighted - P);
  const isHard = absent > ALLOWED;
  RESULTS.push({
    id: resId++, enrollmentId: e.id, studentId: e.studentId, no: e.no, offeringId: e.offeringId,
    tcId: e.tcId, acId: e.acId, key: e.key, W: r1(weighted), P: r1(P), T, absent,
    hard: isHard ? 1 : 0, fail: isHard || T < 60 ? 1 : 0,
    reason: isHard ? 'HARD_LINE' : (T < 60 ? 'SCORE' : null)
  });
});
ins('score_item', SCORE_ITEMS);
ins('score_result', RESULTS.map((r) => ({
  id: r.id, enrollment_id: r.enrollmentId, student_id: r.studentId, offering_id: r.offeringId, term_id: 5,
  weighted_score: r.W, deduction_points: r.P, total_score: r.T, pass_score: 60, absent_count: r.absent,
  late_count: 0, leave_count: 0, allowed_absent: ALLOWED, effective_sessions: 16,
  hard_fail: r.hard, is_fail: r.fail, fail_reason: r.reason, score_status: 'FINAL', retake_times: 0,
  engine_version: 'V2026.1', calculated_at: '2026-10-15 18:00:00', is_void: 0,
  create_time: '2026-10-15 18:00:00', update_time: '2026-10-15 18:00:00'
})));

// ---------- 9) 试卷 / 题目 / 提交 ----------
out.push('-- 9) 试卷 10 张（8 已批 / 1 张 3 人待批 / 1 张 4 人缺考）');
const PAPERS = [];
let pid = 2001;
COURSES.forEach((c) => {
  [['QUIZ', '随堂测验', '2026-09-22'], ['HOMEWORK', '课后作业', '2026-10-06']].forEach(([type, label, day]) => {
    const tc = CLASSES.find((x) => x.key === c.key && x.acId === 1);
    PAPERS.push({
      id: pid++, key: c.key, offeringId: c.offeringId, tcId: tc.id, type, day,
      name: c.name + ' · 第1次' + label,
      pending: c.key === 'A' && type === 'HOMEWORK',
      absent: c.key === 'E' && type === 'QUIZ'
    });
  });
});
ins('paper', PAPERS.map((p) => ({
  id: p.id, teaching_class_id: p.tcId, offering_id: p.offeringId, term_id: 5, paper_type: p.type,
  paper_name: p.name, start_time: p.day + ' 08:00:00', end_time: p.day + ' 23:59:00', duration_minutes: 20,
  full_score: 100, publisher_role: 'TEACHER', publisher_id: 1431,
  allow_view_paper: 1, show_score: 1, show_answer: 'AFTER_MARK', status: 'CLOSED',
  create_by: 1431, create_time: p.day + ' 07:00:00', update_time: p.day + ' 23:59:00'
})));

const Q_DEFS = [
  { t: 'SINGLE', s: 10, stem: '下列哪一项属于输入设备？', opt: [{ key: 'A', text: '显示器' }, { key: 'B', text: '键盘' }, { key: 'C', text: '打印机' }, { key: 'D', text: '音箱' }], ans: { keys: ['B'] } },
  { t: 'SINGLE', s: 10, stem: '1 KB 等于多少字节？', opt: [{ key: 'A', text: '1000' }, { key: 'B', text: '1024' }, { key: 'C', text: '512' }, { key: 'D', text: '2048' }], ans: { keys: ['B'] } },
  { t: 'MULTI', s: 15, stem: '下列属于操作系统的有？', opt: [{ key: 'A', text: 'Linux' }, { key: 'B', text: 'Windows' }, { key: 'C', text: 'Word' }, { key: 'D', text: 'macOS' }], ans: { keys: ['A', 'B', 'D'] } },
  { t: 'JUDGE', s: 15, stem: '断电后内存中的数据不会丢失。', ans: { value: false } },
  { t: 'BLANK', s: 20, stem: '计算机的运算器和控制器合称为＿＿＿＿；内存储器断电后数据会＿＿＿＿。', ans: { answers: ['中央处理器', '丢失'], ignoreCase: true, trim: true } },
  { t: 'SHORT', s: 30, stem: '简述结构化程序设计的基本思想。', ans: { ref: '自顶向下、逐步细化、模块化，限制使用 goto。' } }
];
const QS = [];
let qid = 2101;
PAPERS.forEach((p) => Q_DEFS.forEach((d, i) => QS.push({
  id: qid++, paperId: p.id, no: i + 1, type: d.t, full: d.s,
  stem: p.name + '：' + d.stem,
  options: d.opt ? JSON.stringify(d.opt) : null,
  answer: JSON.stringify(d.ans)
})));
ins('question', QS.map((q) => ({
  id: q.id, paper_id: q.paperId, q_no: q.no, q_type: q.type, stem: q.stem, options_json: q.options,
  answer_json: q.answer, full_score: q.full, group_name: '一、基础题', sort_no: q.no,
  create_time: '2026-09-20 10:00:00', update_time: '2026-09-20 10:00:00'
})));

const SUBS = [];
const SUB_ANS = [];
let subId = 800001, ansId = 900001;
PAPERS.forEach((p) => {
  const tc = CLASSES.find((x) => x.id === p.tcId);
  const list = studentsOf(tc.acId);
  const pendingSet = new Set(p.pending ? list.slice(0, 3).map((s) => s.id) : []);
  const absentSet = new Set(p.absent ? list.slice(0, 4).map((s) => s.id) : []);
  list.forEach((st) => {
    const e = ENROLL.find((x) => x.studentId === st.id && x.tcId === tc.id);
    const isAbsent = absentSet.has(st.id);
    const isPending = pendingSet.has(st.id);
    const marked = !isAbsent && !isPending;
    let obj = 0, subj = 0;
    QS.filter((q) => q.paperId === p.id).forEach((q) => {
      const right = rnd() < 0.75;
      let given = null, score = 0, correct = null, byType = 'SYSTEM';
      if (isAbsent) { given = null; score = 0; correct = 0; byType = 'SYSTEM'; }
      else if (q.type === 'SINGLE') { given = { keys: [right ? 'B' : pick(['A', 'C', 'D'])] }; score = right ? q.full : 0; correct = right ? 1 : 0; }
      else if (q.type === 'MULTI') { given = { keys: right ? ['A', 'B', 'D'] : ['A', 'B'] }; score = right ? q.full : 0; correct = right ? 1 : 0; }
      else if (q.type === 'JUDGE') { given = { value: right ? false : true }; score = right ? q.full : 0; correct = right ? 1 : 0; }
      else if (q.type === 'BLANK') { given = { answers: [right ? '中央处理器' : '控制器', right ? '丢失' : '保留'] }; }
      else { given = { text: '答：自顶向下、逐步细化、模块化，限制使用 goto。' }; }
      const manual = q.type === 'BLANK' || q.type === 'SHORT';
      if (!isAbsent && manual) {
        if (marked) { score = Math.max(0, q.full - between(0, 4)); correct = score >= q.full * 0.6 ? 1 : 0; byType = 'TEACHER'; subj += score; }
        else { score = null; correct = null; byType = null; }
      } else if (!isAbsent && !manual) { obj += score; }
      SUB_ANS.push({
        id: ansId++, submission_id: null, question_id: q.id,
        answer_json: given ? JSON.stringify(given) : null, score, is_correct: correct,
        marked_by_type: byType, marked_by: byType ? 1431 : null, marked_at: byType ? p.day + ' 23:59:00' : null,
        create_time: p.day + ' 08:02:00', update_time: p.day + ' 08:02:00'
      });
    });
    SUBS.push({
      id: subId++, paperId: p.id, tcId: tc.id, studentId: st.id, enrollmentId: e.id, day: p.day,
      status: isAbsent ? 'ABSENT' : (marked ? 'MARKED' : 'SUBMITTED'),
      markedStatus: (isAbsent || marked) ? 'ALL_MARKED' : 'SYSTEM_MARKED',
      obj: r1(obj), subj: r1(subj), total: isAbsent ? 0 : (marked ? r1(obj + subj) : null),
      absent: isAbsent ? 1 : 0, duration: isAbsent ? 0 : between(600, 1180)
    });
  });
});
let cur = 0;
SUBS.forEach((s) => Q_DEFS.forEach(() => { SUB_ANS[cur].submission_id = s.id; cur++; }));
ins('submission', SUBS.map((s) => ({
  id: s.id, paper_id: s.paperId, teaching_class_id: s.tcId, term_id: 5, student_id: s.studentId,
  enrollment_id: s.enrollmentId, submit_status: s.status,
  start_time: s.absent ? null : s.day + ' 08:02:00', submit_time: s.absent ? null : s.day + ' 08:20:00',
  duration_seconds: s.duration, objective_score: s.obj, subjective_score: s.subj, total_score: s.total,
  marked_status: s.markedStatus, marked_by: s.markedStatus === 'ALL_MARKED' ? 1431 : null,
  marked_at: s.markedStatus === 'ALL_MARKED' ? s.day + ' 23:59:00' : null, is_absent: s.absent,
  create_time: s.day + ' 08:02:00', update_time: s.day + ' 23:59:00'
})));
ins('submission_answer', SUB_ANS);

// ---------- 10) 风险状态 / 预警 / 时间线 / 干预 ----------
out.push('-- 10) 风险状态 540、预警 40、时间线 80、干预 12');
const BY_TC = {};
RESULTS.forEach((r) => { (BY_TC[r.tcId] = BY_TC[r.tcId] || []).push(r); });
Object.values(BY_TC).forEach((a) => a.sort((x, y) => x.T - y.T));
const RISK = [];
let riskId = 6001;
RESULTS.forEach((r) => {
  const arr = BY_TC[r.tcId];
  const rank = arr.findIndex((x) => x.id === r.id) + 1;
  const remaining = ALLOWED - r.absent;
  const aLevel = r.hard ? 'FAIL' : (remaining <= 1 ? 'DANGER' : (remaining <= 2 ? 'WATCH' : 'SAFE'));
  // 风险分口径：以总评为主（T 越低越危险），缺课次数加重；排名只作为 B 通道展示字段
  const bScore = r2(Math.min(100, Math.max(0, 100 - r.T) + r.absent * 3));
  const score = r.hard ? 95 : bScore;
  const level = score >= 75 ? 'RED' : score >= 60 ? 'ORANGE' : score >= 40 ? 'YELLOW' : 'GREEN';
  RISK.push({
    id: riskId++, enrollmentId: r.enrollmentId, studentId: r.studentId, offeringId: r.offeringId,
    tcId: r.tcId, absent: r.absent, remaining, hard: r.hard, aLevel, bScore, rank, size: arr.length,
    score, level
  });
});
ins('risk_state', RISK.map((k) => ({
  id: k.id, enrollment_id: k.enrollmentId, student_id: k.studentId, offering_id: k.offeringId,
  teaching_class_id: k.tcId, term_id: 5, absent_count: k.absent, late_count: 0, leave_count: 0,
  allowed_absent: ALLOWED, remaining_absent: k.remaining, hard_fail: k.hard, near_fail: k.remaining <= 1 ? 1 : 0,
  final_exam_needed: null, final_unreachable: 0, channel_a_level: k.aLevel,
  channel_b_score: k.bScore, channel_b_rank: k.rank, channel_b_class_size: k.size, channel_b_level: 'SAME_AS_A',
  channel_c_score: null, delta_used: null, course_factor_used: null, channel_c_level: null,
  risk_score: k.score, risk_level: k.level, active_channel: 'B', confidence: 0.86, confidence_level: 'HIGH',
  output_mode: 'FULL_SCORE', trigger_source: 'EVALUATION', last_eval_time: T_NOW,
  engine_version: 'V2026.1', is_void: 0, create_time: T_NOW, update_time: T_NOW
})));

const sorted = RISK.slice().sort((a, b) => b.score - a.score);
const ALERT_PICK = [
  ...sorted.filter((x) => x.level === 'RED').slice(0, 8),
  ...sorted.filter((x) => x.level === 'ORANGE').slice(0, 12),
  ...sorted.filter((x) => x.level === 'YELLOW').slice(0, 20)
];
const ALERTS = [], FLOWS = [], INTERV = [];
let aid = 3101, fid = 3201, iid = 3301;
ALERT_PICK.forEach((k, i) => {
  const rule = k.hard ? 'A_HARD_LINE' : (k.remaining <= 1 ? 'A_NEAR_LINE' : 'B_RANK');
  const reason = k.hard ? '缺课已达 6 次、超过允许上限 5 次，按缺课超 1/3 硬门槛判定挂科'
    : (k.remaining <= 1 ? '剩余可缺次数不足，再缺一次即触发硬门槛' : '班内相对风险排名靠前');
  const st = i < 8 ? 'ACTIVE' : (i < 14 ? 'HANDLING' : 'ACKNOWLEDGED');
  const handled = st !== 'ACTIVE';
  ALERTS.push({
    id: aid++, student_id: k.studentId, enrollment_id: k.enrollmentId, offering_id: k.offeringId,
    teaching_class_id: k.tcId, course_id: COURSES.find((c) => c.offeringId === k.offeringId).courseId, term_id: 5, alert_level: k.level,
    trigger_channel: k.hard ? 'A' : 'B', trigger_rule: rule, risk_score: k.score, confidence: 0.86,
    output_mode: 'FULL_SCORE', alert_reason: reason,
    evidence_json: JSON.stringify({ absent: k.absent, allowed: ALLOWED, remaining: k.remaining, rank: k.rank, classSize: k.size }),
    channel_snapshot_json: JSON.stringify({ a: k.aLevel, b: k.bScore, c: null }),
    status: st, is_current: 1, escalated: 0, escalated_at: null, previous_level: null, notified: 1,
    first_notify_time: '2026-10-20 08:05:00', expire_time: '2026-10-25 08:00:00',
    handle_by: handled ? 1431 : null, handle_role: handled ? 'TEACHER' : null,
    handle_time: handled ? '2026-10-20 15:00:00' : null,
    handle_note: handled ? '已联系学生，约定课后谈话' : null,
    close_by: null, close_time: null, close_note: null, dismiss_reason: null,
    student_response: null, student_response_time: null, engine_version: 'V2026.1', is_void: 0,
    create_time: T_NOW, update_time: T_NOW
  });
  FLOWS.push({
    id: fid++, alert_id: aid - 1, student_id: k.studentId, term_id: 5, event_type: 'CREATED',
    from_level: null, to_level: k.level, from_status: null, to_status: 'ACTIVE',
    detail: '生成预警：' + reason, operator_role: 'SYSTEM',
    event_time: '2026-10-20 08:00:00', create_time: '2026-10-20 08:00:00'
  });
  FLOWS.push({
    id: fid++, alert_id: aid - 1, student_id: k.studentId, term_id: 5, event_type: 'NOTIFIED',
    from_level: k.level, to_level: k.level, from_status: 'ACTIVE', to_status: 'ACTIVE',
    detail: '已推送给任课教师与辅导员', operator_id: 1431, operator_role: 'SYSTEM',
    event_time: '2026-10-20 08:05:00', create_time: '2026-10-20 08:05:00'
  });
});
ALERT_PICK.slice(0, 12).forEach((k, i) => {
  const effective = i < 4 ? 'EFFECTIVE' : (i < 8 ? 'PENDING' : 'INEFFECTIVE');
  INTERV.push({
    id: iid++, student_id: k.studentId, term_id: 5, alert_id: ALERTS[i].id, related_alert_ids: null,
    stage: i < 6 ? 'TEACHER' : 'COUNSELOR_1', operator_id: 1431, operator_role: i < 6 ? 'TEACHER' : 'COUNSELOR',
    action_type: pick(['TALK', 'CLASS_MEETING', 'PLAN', 'SUPPLEMENT']),
    content: '课后个别谈话，了解缺课原因并约定补交计划',
    result: '学生承诺按时出勤，作业已补交', effective,
    effect_check_time: '2026-11-03 10:00:00', risk_score_before: k.score,
    risk_score_after: effective === 'EFFECTIVE' ? r1(k.score - 12) : null,
    risk_score_change: effective === 'EFFECTIVE' ? -12 : null,
    refer_status: 'NONE', is_void: 0, create_by: 1431,
    create_time: '2026-10-21 10:00:00', update_by: 1431, update_time: '2026-10-21 10:00:00'
  });
});
ins('alert_record', ALERTS);
ins('alert_flow_log', FLOWS);
ins('intervention_record', INTERV);

// ---------- 11) 三维挂科率 / 教师尺度 ----------
out.push('-- 11) 三维挂科率与教师尺度（真实口径，无合成标记）');
ins('course_fail_rate_history', COURSES.map((c, i) => {
  const rows = RESULTS.filter((r) => r.key === c.key);
  const fail = rows.filter((r) => r.fail).length;
  return {
    id: 2001 + i, course_id: c.courseId, term_id: 5, examinee_count: rows.length, fail_count: fail,
    fail_rate: r2(fail * 100 / rows.length), hard_fail_count: rows.filter((r) => r.hard).length,
    teaching_class_count: CLASSES.filter((x) => x.key === c.key).length, is_initialized: 0,
    engine_version: 'V2026.1', calc_time: '2026-10-20 09:00:00',
    create_time: '2026-10-20 09:00:00', update_time: '2026-10-20 09:00:00'
  };
}));
ins('teaching_class_fail_rate', CLASSES.map((c, i) => {
  const rows = RESULTS.filter((r) => r.tcId === c.id);
  const fail = rows.filter((r) => r.fail).length;
  return {
    id: 4001 + i, teaching_class_id: c.id, offering_id: c.offeringId, teacher_id: c.teacherId, term_id: 5,
    examinee_count: rows.length, fail_count: fail, fail_rate: r2(fail * 100 / rows.length),
    hard_fail_count: rows.filter((r) => r.hard).length, is_retake_class: c.retake,
    engine_version: 'V2026.1', calc_time: '2026-10-20 09:00:00',
    create_time: '2026-10-20 09:00:00', update_time: '2026-10-20 09:00:00'
  };
}));
ins('admin_class_fail_rate', ADMIN_CLASSES.map((ac, i) => {
  const rows = RESULTS.filter((r) => r.acId === ac.id);
  const fail = rows.filter((r) => r.fail).length;
  return {
    id: 5001 + i, admin_class_id: ac.id, offering_id: 101, course_id: 1, term_id: 5,
    examinee_count: rows.length, fail_count: fail, fail_rate: r2(fail * 100 / rows.length),
    hard_fail_count: rows.filter((r) => r.hard).length, engine_version: 'V2026.1',
    calc_time: '2026-10-20 09:00:00', create_time: '2026-10-20 09:00:00', update_time: '2026-10-20 09:00:00'
  };
}));
const STYLE = [];
let styId = 6001;
COURSES.forEach((c) => {
  [...new Set(CLASSES.filter((x) => x.key === c.key).map((x) => x.teacherId))].forEach((t) => {
    const raw = r2((rnd() - 0.5) * 6);
    STYLE.push({
      id: styId++, teacher_id: t, offering_id: c.offeringId, course_id: c.courseId, term_id: 5,
      item_code: 'OVERALL', raw_delta: raw, delta: r2(raw * 0.62), shrink_lambda: 0.62, sample_size: 45,
      std_dev: r2(8 + rnd() * 4), fused: 0, status: 'ACTIVE', engine_version: 'V2026.1',
      calc_time: '2026-10-18 10:00:00', create_time: '2026-10-18 10:00:00', update_time: '2026-10-18 10:00:00'
    });
  });
});
ins('teacher_style', STYLE);
del("UPDATE term SET status='CLOSED' WHERE id=4");

// ---------- 12) 问卷 + 四环节 ----------
out.push('-- 12) 教师风格问卷：四环节齐全 + 5 份匿名作答');
ins('questionnaire', [{
  id: 1, offering_id: 101, course_id: 1, term_id: 5, questionnaire_type: 'TEACHER_STYLE',
  title: '2026-2027-1 教师风格判定（大学计算机基础）',
  question_json: JSON.stringify([
    { itemCode: 'Q1', text: '老师讲课的进度对你来说', type: 'SCORE' },
    { itemCode: 'Q2', text: '作业量与难度', type: 'SCORE' },
    { itemCode: 'Q3', text: '老师给分总体偏', type: 'SCORE' },
    { itemCode: 'Q4', text: '课后答疑是否及时', type: 'SCORE' },
    { itemCode: 'Q5', text: '还有其他想说的', type: 'TEXT' }
  ]),
  is_anonymous: 1, collector_type: 'COUNSELOR', collector_id: 1440,
  open_time: '2026-10-10 09:00:00', close_time: '2026-10-18 23:59:00',
  status: 'PUBLISHED', answer_count: 5,
  submit_to_admin_by: 1440, submit_to_admin_time: '2026-10-12 10:00:00',
  distributed_by: 1442, distributed_time: '2026-10-14 09:00:00',
  create_by: 1440, create_time: '2026-10-10 09:00:00', update_time: '2026-10-18 15:00:00'
}]);
ins('questionnaire_flow', [
  { questionnaire_id: 1, term_id: 5, stage: 'COUNSELOR_COLLECT', stage_order: 1, holder_id: 1440, holder_role: 'COUNSELOR', entered_at: '2026-10-10 09:00:00', deadline: '2026-10-12 23:59:00', status: 'DONE', done_at: '2026-10-12 10:00:00', note: '导员收集完成', create_time: '2026-10-10 09:00:00', update_time: '2026-10-12 10:00:00' },
  { questionnaire_id: 1, term_id: 5, stage: 'ADMIN_RECEIVE', stage_order: 2, holder_id: 1442, holder_role: 'ADMIN', entered_at: '2026-10-12 10:00:00', deadline: '2026-10-13 23:59:00', status: 'DONE', done_at: '2026-10-13 09:00:00', note: '教务已接收', create_time: '2026-10-12 10:00:00', update_time: '2026-10-13 09:00:00' },
  { questionnaire_id: 1, term_id: 5, stage: 'ADMIN_DISTRIBUTE', stage_order: 3, holder_id: 1442, holder_role: 'ADMIN', entered_at: '2026-10-13 09:00:00', deadline: '2026-10-14 23:59:00', status: 'DONE', done_at: '2026-10-14 09:00:00', note: '已分发至课程组', create_time: '2026-10-13 09:00:00', update_time: '2026-10-14 09:00:00' },
  { questionnaire_id: 1, term_id: 5, stage: 'TEACHER_VIEW', stage_order: 4, holder_id: 1431, holder_role: 'TEACHER', entered_at: '2026-10-14 09:00:00', deadline: '2026-10-18 23:59:00', status: 'DONE', done_at: '2026-10-15 09:00:00', note: '授课组已查看', create_time: '2026-10-14 09:00:00', update_time: '2026-10-15 09:00:00' }
]);
const ANS = [];
let ansRow = 8001;
['tok-a1f3', 'tok-b2e7', 'tok-c9d1', 'tok-d4a8', 'tok-e6b5'].forEach((tk) => {
  ['Q1', 'Q2', 'Q3', 'Q4', 'Q5'].forEach((code) => {
    const isText = code === 'Q5';
    ANS.push({
      id: ansRow++, questionnaire_id: 1, respondent_token: tk, student_id: null, enrollment_id: null,
      target_teacher_id: 1431, item_code: code, answer_type: isText ? 'TEXT' : 'SCORE',
      score_value: isText ? null : between(3, 5), text_value: isText ? '希望多些上机练习' : null,
      submit_time: '2026-10-16 10:00:00', create_time: '2026-10-16 10:00:00'
    });
  });
});
ins('questionnaire_answer', ANS);

// ---------- 13) 历史学期 term 4 ----------
out.push('-- 13) 历史学期 2025-2026-2：2024 级 45 人 × 2 门课（12 人挂科 → 学生挂科历史）');
ins('course_group', [
  { id: 111, course_id: 6, term_id: 4, level: 'COLLEGE', level_request_status: 'APPROVED', status: 'ARCHIVED', create_time: '2026-01-10 09:00:00', update_time: '2026-07-10 09:00:00' },
  { id: 112, course_id: 7, term_id: 4, level: 'COLLEGE', level_request_status: 'APPROVED', status: 'ARCHIVED', create_time: '2026-01-10 09:00:00', update_time: '2026-07-10 09:00:00' }
]);
ins('course_offering', [
  { id: 111, course_id: 6, term_id: 4, group_id: 111, planned_weeks: 16, planned_times_per_week: 1, planned_sessions: 16, actual_weeks: 16, actual_sessions: 16, actual_submit_status: 'CONFIRMED', effective_sessions: 16, effective_source: 'ACTUAL', pass_score: 60, status: 'ARCHIVED', create_by: 1442, create_time: '2026-01-12 09:00:00', update_time: '2026-07-10 09:00:00' },
  { id: 112, course_id: 7, term_id: 4, group_id: 112, planned_weeks: 16, planned_times_per_week: 1, planned_sessions: 16, actual_weeks: 16, actual_sessions: 16, actual_submit_status: 'CONFIRMED', effective_sessions: 16, effective_source: 'ACTUAL', pass_score: 60, status: 'ARCHIVED', create_by: 1442, create_time: '2026-01-12 09:00:00', update_time: '2026-07-10 09:00:00' }
]);
ins('teaching_class', [
  { id: 221, offering_id: 111, teacher_id: 3, class_code: 'H-2024-C', class_name: '程序设计基础 2024级班', capacity: 60, is_retake_class: 0, invite_code: '220011', join_review_enabled: 0, status: 'CLOSED', create_by: 1, create_time: '2026-01-13 09:00:00', update_time: '2026-07-10 09:00:00' },
  { id: 222, offering_id: 112, teacher_id: 4, class_code: 'H-2024-D', class_name: '数据结构 2024级班', capacity: 60, is_retake_class: 0, invite_code: '220012', join_review_enabled: 0, status: 'CLOSED', create_by: 1, create_time: '2026-01-13 09:00:00', update_time: '2026-07-10 09:00:00' }
]);
const H_ENROLL = [];
let hId = 21001;
studentsOf(3).forEach((s) => {
  H_ENROLL.push({ id: hId++, studentId: s.id, offeringId: 111, tcId: 221, courseId: 6 });
  H_ENROLL.push({ id: hId++, studentId: s.id, offeringId: 112, tcId: 222, courseId: 7 });
});
ins('enrollment', H_ENROLL.map((e) => ({
  id: e.id, student_id: e.studentId, teaching_class_id: e.tcId, offering_id: e.offeringId, term_id: 4,
  status: 'ENROLLED', retake_times: 0, enroll_time: '2026-02-20 10:00:00', create_by: 1,
  create_time: '2026-02-20 10:00:00', update_time: '2026-02-20 10:00:00'
})));
const FAIL_SET = new Set(studentsOf(3).slice(0, 12).map((s) => s.id));
let hRes = 710001;
const H_RESULT = H_ENROLL.map((e) => {
  const T = FAIL_SET.has(e.studentId) ? r1(48 + rnd() * 10) : r1(65 + rnd() * 25);
  return {
    id: hRes++, enrollment_id: e.id, student_id: e.studentId, offering_id: e.offeringId, term_id: 4,
    weighted_score: T, deduction_points: 0, total_score: T, pass_score: 60, absent_count: 0, late_count: 0,
    leave_count: 0, allowed_absent: 5, effective_sessions: 16, hard_fail: 0, is_fail: T < 60 ? 1 : 0,
    fail_reason: T < 60 ? 'SCORE' : null, score_status: 'FINAL', retake_times: 0, engine_version: 'V2025.3',
    calculated_at: '2026-07-05 18:00:00', is_void: 0, create_time: '2026-07-05 18:00:00', update_time: '2026-07-05 18:00:00'
  };
});
ins('score_result', H_RESULT);
ins('student_fail_history', H_ENROLL.map((e, i) => {
  const r = H_RESULT[i];
  return {
    id: 7001 + i, student_id: e.studentId, term_id: 4, course_id: e.courseId, offering_id: e.offeringId,
    final_score: r.total_score, pass_score: 60, is_fail: r.is_fail, hard_fail: 0, is_retake: 0,
    score_status: 'FINAL', engine_version: 'V2025.3', archived_at: '2026-07-06 09:00:00',
    create_time: '2026-07-06 09:00:00', update_time: '2026-07-06 09:00:00'
  };
}));

// ---------- 14) 四端消息 ----------
out.push('-- 14) 四端消息（学生 2 / 教师 2 / 导员 1 / 教务 1，含未读）');
const NTF = [
  { id: 9001, biz_type: 'ALERT_NEW', title: '课程学情预警：缺课次数已超允许上限', content: '你在《大学计算机基础》的缺课次数已达 6 次，超过允许上限 5 次，按缺课超 1/3 硬门槛判定挂科。', audience_desc: '学生 20250001', priority: 'HIGH' },
  { id: 9002, biz_type: 'SCORE_TASK', title: '《大学计算机基础》期末成绩已发布', content: '总评已发布，可到「我的成绩」查看分项构成。', audience_desc: '学生 20250001', priority: 'NORMAL' },
  { id: 9003, biz_type: 'SCORE_TASK', title: '程序设计基础·第1次课后作业：3 份待批阅', content: '还有 3 名学生的填空/简答题未给分。', audience_desc: '教师 1001', priority: 'HIGH' },
  { id: 9004, biz_type: 'COVERAGE_REMIND', title: '点名覆盖率提醒：CS-01-A 第 13~16 周未点名', content: '最近 4 次课未点名，点名覆盖率 75%，请在课后补录。', audience_desc: '教师 1001', priority: 'NORMAL' },
  { id: 9005, biz_type: 'CLASS_ALERT', title: '班集体提醒：计算机1班 9 人缺课超限', content: '本周风险学生 9 人、占比 20%，建议召开班会。', audience_desc: '导员 马思国', priority: 'NORMAL' },
  { id: 9006, biz_type: 'ARCHIVE_TASK', title: '归档待审：CS-01-A 期中考勤待审核', content: '覆盖 8 次课、360 条记录，覆盖率 75%。', audience_desc: '教务处', priority: 'HIGH' }
];
ins('notification', NTF.map((n) => ({
  ...n, aggregate_count: 1, create_by: 1442, create_time: T_NOW, update_time: T_NOW
})));
const ACC = { '学生 20250001': [631, 'STUDENT'], '教师 1001': [1431, 'TEACHER'], '导员 马思国': [1440, 'COUNSELOR'], '教务处': [1442, 'ADMIN'] };
let rcId = 9101;
ins('notification_receipt', NTF.map((n, i) => {
  const a = ACC[n.audience_desc];
  const read = i % 3 === 0 ? 1 : 0;
  return {
    id: rcId++, notification_id: n.id, recipient_id: a[0], recipient_role: a[1], channel: 'IN_APP',
    send_status: 'SENT', send_time: T_NOW, is_read: read,
    read_time: read ? '2026-10-20 09:00:00' : null, ack_required: 0, ack_status: 'NONE',
    create_time: T_NOW, update_time: T_NOW
  };
}));

// ---------- 14.5) 两张「进行中」的试卷（无提交，供演示 学生答题 → 教师批改 全流程） ----------
out.push('-- 14.5) 进行中的试卷 2 张（无提交，学生可直接作答）');
const OPEN = [
  { id: 2011, offeringId: 101, tcId: 201, name: '大学计算机基础 · 第2次随堂测验（进行中）' },
  { id: 2012, offeringId: 102, tcId: 203, name: '高等数学 · 第2次随堂测验（进行中）' }
];
ins('paper', OPEN.map((p) => ({
  id: p.id, teaching_class_id: p.tcId, offering_id: p.offeringId, term_id: 5, paper_type: 'QUIZ',
  paper_name: p.name, start_time: '2026-10-20 08:00:00', end_time: '2026-12-10 23:59:00',
  duration_minutes: 30, full_score: 100, publisher_role: 'TEACHER', publisher_id: 1431,
  allow_view_paper: 1, show_score: 1, show_answer: 'AFTER_MARK', status: 'PUBLISHED',
  create_by: 1431, create_time: '2026-10-20 07:00:00', update_time: '2026-10-20 07:10:00'
})));
let oqId = 2161;
const OQ = [];
OPEN.forEach((p) => Q_DEFS.forEach((d, i) => OQ.push({
  id: oqId++, paper_id: p.id, q_no: i + 1, q_type: d.t, stem: p.name + '：' + d.stem,
  options_json: d.opt ? JSON.stringify(d.opt) : null, answer_json: JSON.stringify(d.ans),
  full_score: d.s, group_name: '一、基础题', sort_no: i + 1,
  create_time: '2026-10-20 07:05:00', update_time: '2026-10-20 07:05:00'
})));
ins('question', OQ);

// ---------- 15) 合成样本学期 term 8（与主线学生池对齐；只放过程数据，不放试卷） ----------
out.push('-- 15) 合成学期 term 8：6 门课 × 135 人，仅过程数据（选课/课次/考勤/成绩），不放试卷');
[
  'DELETE sa FROM submission_answer sa JOIN submission s ON s.id = sa.submission_id WHERE s.term_id = 8',
  'DELETE FROM submission WHERE term_id = 8',
  'DELETE FROM question WHERE paper_id IN (SELECT id FROM paper WHERE term_id = 8)',
  'DELETE FROM paper WHERE term_id = 8',
  'DELETE FROM attendance_record WHERE term_id = 8',
  'DELETE FROM course_session WHERE term_id = 8',
  'DELETE FROM score_item WHERE term_id = 8',
  'DELETE FROM score_result WHERE term_id = 8',
  'DELETE FROM enrollment WHERE term_id = 8',
  'DELETE FROM risk_state WHERE term_id = 8',
  'DELETE FROM alert_record WHERE term_id = 8',
  'DELETE FROM teaching_class_admin_class WHERE teaching_class_id BETWEEN 301 AND 306',
  'DELETE FROM teaching_class WHERE id BETWEEN 301 AND 306',
  'DELETE FROM course_fail_rate_history WHERE term_id = 8',
  'DELETE FROM teaching_class_fail_rate WHERE term_id = 8',
  'DELETE FROM admin_class_fail_rate WHERE term_id = 8'
].forEach(del);
const SYN = [
  { id: 301, offeringId: 4, name: '高等数学（一）' },
  { id: 302, offeringId: 5, name: '大学物理（二）' },
  { id: 303, offeringId: 6, name: '程序设计基础' },
  { id: 304, offeringId: 7, name: '数据结构' },
  { id: 305, offeringId: 8, name: '计算机网络' },
  { id: 306, offeringId: 9, name: '数据库原理' }
];
ins('teaching_class', SYN.map((c, i) => ({
  id: c.id, offering_id: c.offeringId, teacher_id: 6 + (i % 4), class_code: 'SYN-' + (i + 1),
  class_name: c.name + ' 合成班', capacity: 200, is_retake_class: 0, invite_code: String(330100 + i),
  join_review_enabled: 0, status: 'ACTIVE', create_by: 1,
  create_time: '2027-08-20 09:00:00', update_time: '2027-08-20 09:00:00'
})));
ins('teaching_class_admin_class', SYN.map((c) => ({ teaching_class_id: c.id, admin_class_id: 1 })));
const SYN_ENROLL = [];
let synEnrollId = 300001;
SYN.forEach((c) => STUDENTS.forEach((s) => SYN_ENROLL.push({
  id: synEnrollId++, studentId: s.id, tcId: c.id, offeringId: c.offeringId, name: c.name
})));
ins('enrollment', SYN_ENROLL.map((e) => ({
  id: e.id, student_id: e.studentId, teaching_class_id: e.tcId, offering_id: e.offeringId, term_id: 8,
  status: 'ENROLLED', retake_times: 0, enroll_time: '2027-08-25 14:00:00', create_by: 1,
  create_time: '2027-08-25 14:00:00', update_time: '2027-08-25 14:00:00'
})));
const SYN_START = new Date('2027-09-06T08:00:00');
const synDate = (w) => {
  const d = new Date(SYN_START.getTime());
  d.setDate(d.getDate() + (w - 1) * 7);
  return d.toISOString().slice(0, 10);
};
const SYN_SESS = [];
let synSessId = 600001;
SYN.forEach((c) => {
  for (let w = 1; w <= 16; w++) SYN_SESS.push({
    id: synSessId++, tcId: c.id, offeringId: c.offeringId, week: w, date: synDate(w), called: w <= 12
  });
});
ins('course_session', SYN_SESS.map((s) => ({
  id: s.id, offering_id: s.offeringId, teaching_class_id: s.tcId, term_id: 8, week_no: s.week, session_no: s.week,
  session_date: s.date, start_time: '08:00', end_time: '09:40', location: '教2-' + (101 + (s.tcId % 18)),
  session_type: 'NORMAL', status: 'NORMAL', roll_call_status: s.called ? 'CALLED' : 'NOT_CALLED',
  roll_call_time: s.called ? s.date + ' 09:45:00' : null, roll_call_by: s.called ? 1442 : null,
  roll_call_count: s.called ? 135 : 0, is_void: 0, create_by: 1,
  create_time: '2027-08-26 09:00:00', update_time: '2027-08-26 09:00:00'
})));
const SYN_ATT = [];
let synAttId = 900001;
SYN_SESS.filter((s) => s.called).forEach((s) => {
  SYN_ENROLL.filter((e) => e.tcId === s.tcId).forEach((e) => {
    const r = rnd();
    const status = r < 0.03 ? 'ABSENT' : (r < 0.08 ? 'LATE' : (r < 0.11 ? 'LEAVE' : 'PRESENT'));
    SYN_ATT.push({
      id: synAttId++, enrollment_id: e.id, student_id: e.studentId, session_id: s.id, term_id: 8, status,
      leave_type: status === 'LEAVE' ? 'PERSONAL' : null, phase: 'NORMAL', source: 'ADMIN',
      recorded_by: 1442, recorded_role: 'ADMIN', recorded_at: s.date + ' 09:45:00',
      batch_key: 'SYN-RC-' + s.id, is_void: 0, create_time: '2027-09-01 10:00:00', update_time: '2027-09-01 10:00:00'
    });
  });
});
ins('attendance_record', SYN_ATT);
const SYN_RES = [];
let synResId = 950001;
SYN_ENROLL.forEach((e) => {
  const ability = 50 + rnd() * 45;
  const absent = rnd() < 0.06 ? between(1, 3) : 0;
  const T = r2(ability - absent * 2);
  SYN_RES.push({
    id: synResId++, enrollmentId: e.id, studentId: e.studentId, offeringId: e.offeringId, T, absent,
    fail: T < 60 ? 1 : 0
  });
});
ins('score_result', SYN_RES.map((r) => ({
  id: r.id, enrollment_id: r.enrollmentId, student_id: r.studentId, offering_id: r.offeringId, term_id: 8,
  weighted_score: r.T, deduction_points: r.absent * 2, total_score: r.T, pass_score: 60,
  absent_count: r.absent, late_count: 0, leave_count: 0, allowed_absent: 5, effective_sessions: 16,
  hard_fail: 0, is_fail: r.fail, fail_reason: r.fail ? 'SCORE' : null, score_status: 'FINAL',
  retake_times: 0, engine_version: 'V2026.1', calculated_at: '2028-01-10 18:00:00', is_void: 0,
  create_time: '2028-01-10 18:00:00', update_time: '2028-01-10 18:00:00'
})));

// ---------- 校验 ----------
out.push(`-- 15) 校验
SELECT '主线规模' AS r;
SELECT (SELECT COUNT(*) FROM student) AS students,
       (SELECT COUNT(*) FROM administrative_class) AS classes,
       (SELECT COUNT(*) FROM teaching_class) AS teaching_classes,
       (SELECT COUNT(*) FROM enrollment WHERE term_id=5) AS enroll_t5,
       (SELECT COUNT(*) FROM course_session WHERE term_id=5) AS sessions_t5,
       (SELECT COUNT(*) FROM attendance_record WHERE term_id=5) AS att_t5,
       (SELECT COUNT(*) FROM score_result WHERE term_id=5) AS results_t5,
       (SELECT COUNT(*) FROM submission WHERE term_id=5) AS subs_t5;
SELECT '演示要素' AS r;
SELECT (SELECT COUNT(*) FROM submission WHERE marked_status='SYSTEM_MARKED') AS pending_subs,
       (SELECT COUNT(*) FROM submission WHERE is_absent=1) AS absent_subs,
       (SELECT COUNT(*) FROM score_result WHERE term_id=5 AND is_fail=1) AS fail_t5,
       (SELECT COUNT(*) FROM score_result WHERE term_id=5 AND hard_fail=1) AS hard_t5,
       (SELECT COUNT(*) FROM risk_state WHERE term_id=5) AS risk_t5,
       (SELECT COUNT(*) FROM alert_record WHERE term_id=5) AS alerts_t5,
       (SELECT COUNT(*) FROM questionnaire_flow) AS q_flow,
       (SELECT COUNT(*) FROM student_fail_history) AS hist;
SELECT '学期' AS r;
SELECT id, term_code, term_name, is_current, status FROM term ORDER BY id;`);

fs.writeFileSync(path.join(__dirname, '20_rebuild_demo.sql'), out.join('\n'), 'utf8');
console.log('生成 20_rebuild_demo.sql：块=' + out.length
  + ' 学生=' + STUDENTS.length + ' 教学班=' + CLASSES.length + ' 选课=' + ENROLL.length
  + ' 课次=' + SESS.length + ' 考勤=' + ATT.length + ' 分项=' + SCORE_ITEMS.length
  + ' 成绩单=' + RESULTS.length + ' 试卷=' + PAPERS.length + ' 题=' + QS.length
  + ' 提交=' + SUBS.length + ' 逐题=' + SUB_ANS.length
  + ' 风险=' + RISK.length + ' 预警=' + ALERTS.length + ' 历史成绩=' + H_RESULT.length);
