/* seed_demo.js — 生成 20_rebuild_demo.sql（演示库「一条主线 + 一段历史」重建）
 *
 * 主线 term 5（2026-2027-1 · current · ACTIVE）
 *   3 个行政班 × 45 人 = 135 学生
 *   5 门课 · 12 个教学班 · 选课 540（含 2 个重修班）
 *   课次按课程周次生成（12 周 / 16 周），点名 75%；考勤逐人一条
 *   成绩构成：每门课「平时 + 终结性」两级构成（各课权重不同，见 SCHEMES）
 *   分项得分按能力分布生成 · 成绩单 540（含 1/3 缺课硬门槛，每班 0~2 人）
 *   试卷 10 张（8 已批 / 1 张 3 人待批 / 1 张 4 人缺考）· 提交 450 · 逐题 2700
 *   风险状态 / 预警 / 时间线 / 干预由风险引擎在重建后真实生成（本脚本不手写）
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

// 课程与周次/学分：A 大学计算机基础 12 周 · B 高等数学 16 周 · C 四史概述 12 周 · D 数据结构(Java) 16 周 · E 计算机网络 12 周
// 重修班挂在高数之外的核心课上：数据结构（D）与计算机网络（E）各一个重修班
const COURSES = [
  { key: 'A', courseId: 1, name: '大学计算机基础（一）', offeringId: 101, schemeId: 601, weeks: 12, credit: 2.0, type: 'EXAM' },
  { key: 'B', courseId: 4, name: '高等数学（一）', offeringId: 102, schemeId: 602, weeks: 16, credit: 4.0, type: 'EXAM' },
  { key: 'C', courseId: 10, name: '四史概述', offeringId: 103, schemeId: 603, weeks: 12, credit: 1.0, type: 'EXAM' },
  { key: 'D', courseId: 7, name: '数据结构（Java）', offeringId: 104, schemeId: 604, weeks: 16, credit: 2.0, type: 'ASSESS' },
  { key: 'E', courseId: 8, name: '计算机网络', offeringId: 105, schemeId: 605, weeks: 12, credit: 2.0, type: 'EXAM' }
];

let tcid = 201;
const CLASSES = [];
COURSES.forEach((c) => {
  const teacherBase = { A: 1, B: 2, C: 3, D: 4, E: 5 }[c.key];
  [1, 2].forEach((acId) => {
    // 计基 02 班换成另一位老师（1006 赵明）：同一个课程组两位老师，用来演示「资料库课程组共享」
    const teacherId = (c.key === 'A' && acId === 2) ? 6 : teacherBase;
    CLASSES.push({
      id: tcid++, offeringId: c.offeringId, key: c.key, acId, retake: 0, teacherId,
      code: (acId === 1 ? 'CS-01-' : 'CS-02-') + c.key,
      name: c.name + (acId === 1 ? ' 01班' : ' 02班')
    });
  });
  if (c.key === 'D' || c.key === 'E') CLASSES.push({
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

// ---------- 2.1) 课程口径（考核方式 / 学分 / 周次） ----------
out.push('-- 2.1) 课程口径：考核方式、学分、总学时（12 周 = 24 学时，16 周 = 32 学时）');
del("UPDATE course SET course_name='大学计算机基础（一）', course_type='EXAM', credit=2.0, total_hours=24, update_time=NOW() WHERE id=1");
del("UPDATE course SET course_name='高等数学（一）', course_type='EXAM', credit=4.0, total_hours=32, update_time=NOW() WHERE id=4");
del("UPDATE course SET course_name='数据结构（Java）', course_type='ASSESS', credit=2.0, total_hours=32, update_time=NOW() WHERE id=7");
del("UPDATE course SET course_name='计算机网络', course_type='EXAM', credit=2.0, total_hours=24, update_time=NOW() WHERE id=8");
del('DELETE FROM course WHERE id = 10');
ins('course', [{
  id: 10, course_name: '四史概述', course_type: 'EXAM', credit: 1.0, total_hours: 24,
  college_name: '计算机学院', status: 'ACTIVE',
  create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
}]);

// ---------- 3) 课程组 / 开课 / 教学班 ----------
out.push('-- 3) 课程组、开课实例、教学班、导员带班');
ins('course_group', COURSES.map((c) => ({
  id: c.offeringId, course_id: c.courseId, term_id: 5, level: 'COLLEGE',
  level_request_status: 'APPROVED', status: 'ACTIVE', create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
})));
ins('course_offering', COURSES.map((c) => ({
  id: c.offeringId, course_id: c.courseId, term_id: 5, group_id: c.offeringId,
  planned_weeks: c.weeks, planned_times_per_week: 1, planned_sessions: c.weeks,
  actual_weeks: c.weeks, actual_sessions: c.weeks, actual_submit_status: 'SUBMITTED',
  actual_submit_by: 1442, actual_submit_time: '2026-12-18 09:00:00', actual_confirm_by: 1442,
  actual_confirm_time: '2026-12-19 09:00:00', effective_sessions: c.weeks, effective_source: 'ACTUAL',
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

// ---------- 5) 成绩构成（两级：平时 + 终结性；各课权重按培养方案） ----------
// 口径来源：培养方案讨论稿（2026-10）——
//   A 大学计算机基础：平时 40（课堂测试 35 / 三级项目 20 / 课堂表现 5 / 课后作业 40）+ 期末 60
//   B 高等数学：      平时 40（课堂测试 30 / 期中考试 20 / 课后作业 50）+ 期末 60
//   C 四史概述：      平时 60（课堂测试 35 / 三级项目 20 / 四级项目 40 / 课堂表现 5）+ 期末 40
//   D 数据结构(Java)：平时 40（课堂测试 35 / 三级项目 25 / 课后作业 40）+ 大作业 60
//   E 计算机网络：    平时 50（课堂测试 30 / 期中考试 20 / 课后作业 50）+ 期末 50
out.push('-- 5) 成绩构成（两级：平时成绩 + 终结性考核，合计 100；考勤不计入评分项）');
const SCHEMES = {
  A: { usual: 40, finalW: 60, finalName: '终结性考核',
    items: [
      { code: 'QUIZ', name: '课堂测试', cat: 'QUIZ', w: 35, times: 7, src: 'paper' },
      { code: 'P3', name: '三级项目', cat: 'PROJECT', w: 20, times: 1, src: 'project',
        desc: '个人项目（Office 基础）：Excel 数据处理、函数、透视表；提交以「学号 + 姓名」命名的 Excel 文件，选题不可重复' },
      { code: 'PERF', name: '课堂表现', cat: 'PERFORMANCE', w: 5, times: 1, src: 'manual' },
      { code: 'HW', name: '课后作业', cat: 'HOMEWORK', w: 40, times: 4, src: 'paper' }
    ],
    final:  { code: 'FINAL_EXAM', name: '期末考试', cat: 'FINAL', times: 1, src: 'manual' } },
  B: { usual: 40, finalW: 60, finalName: '终结性考核',
    items: [
      { code: 'QUIZ', name: '课堂测试', cat: 'QUIZ', w: 30, times: 6, src: 'paper' },
      { code: 'MID', name: '期中考试', cat: 'MIDTERM', w: 20, times: 1, src: 'manual' },
      { code: 'HW', name: '课后作业', cat: 'HOMEWORK', w: 50, times: 5, src: 'paper' }
    ],
    final:  { code: 'FINAL_EXAM', name: '期末考试', cat: 'FINAL', times: 1, src: 'manual' } },
  C: { usual: 60, finalW: 40, finalName: '终结性考核',
    items: [
      { code: 'QUIZ', name: '课堂测试', cat: 'QUIZ', w: 35, times: 7, src: 'paper' },
      { code: 'P3', name: '三级项目', cat: 'PROJECT', w: 20, times: 1, src: 'project', team: true, teamSize: 5,
        desc: '团队项目（4–6 人）：参观或走访历史遗址 / 相关场馆，收集口述、影像与文献材料，完成选题与资料整理并做阶段汇报' },
      { code: 'P4', name: '四级项目', cat: 'PROJECT', w: 40, times: 1, src: 'project', team: true, teamSize: 5,
        desc: '团队项目（同一小组）：在三级项目材料基础上完成 PPT 制作与课堂答辩，按内容、逻辑与表达打分' },
      { code: 'PERF', name: '课堂表现', cat: 'PERFORMANCE', w: 5, times: 1, src: 'manual' }
    ],
    final:  { code: 'FINAL_EXAM', name: '期末考试', cat: 'FINAL', times: 1, src: 'manual' } },
  D: { usual: 40, finalW: 60, finalName: '终结性考核',
    items: [
      { code: 'QUIZ', name: '课堂测试', cat: 'QUIZ', w: 35, times: 7, src: 'paper' },
      { code: 'P3', name: '三级项目', cat: 'PROJECT', w: 25, times: 1, src: 'project',
        desc: '个人项目：课上一节课完成，按教师给定的数据库表结构与任务要求编写对应需求的代码结构' },
      { code: 'HW', name: '课后作业', cat: 'HOMEWORK', w: 40, times: 4, src: 'paper' }
    ],
    final:  { code: 'ASSIGN', name: '大作业', cat: 'FINAL', times: 1, src: 'project', team: true, teamSize: 3,
      desc: '团队项目（2–3 人）：从教师给定题目中选题自主开发，按完成度打分' } },
  E: { usual: 50, finalW: 50, finalName: '终结性考核',
    items: [
      { code: 'QUIZ', name: '课堂测试', cat: 'QUIZ', w: 30, times: 6, src: 'paper' },
      { code: 'MID', name: '期中考试', cat: 'MIDTERM', w: 20, times: 1, src: 'manual' },
      { code: 'HW', name: '课后作业', cat: 'HOMEWORK', w: 50, times: 5, src: 'paper' }
    ],
    final:  { code: 'FINAL_EXAM', name: '期末考试', cat: 'FINAL', times: 1, src: 'manual' } }
};

ins('score_scheme', COURSES.map((c) => ({
  id: c.schemeId, group_id: c.offeringId, term_id: 5, version: 2, level: 'COLLEGE',
  total_weight: 100, confirm_status: 'LOCKED', confirm_total: 100, confirm_done: 1,
  locked: 1, locked_by: 1431, locked_at: '2026-08-28 10:00:00', lock_type: 'COLLEGE',
  create_by: 1431, create_time: '2026-08-28 09:00:00', update_time: '2026-08-28 10:00:00'
})));
const ITEM_ID = {};
const LEAVES = {};
let siid = 701;
const SI = [];
const T8 = '2026-08-28 09:00:00';
COURSES.forEach((c) => {
  const sc = SCHEMES[c.key];
  ITEM_ID[c.schemeId] = {};
  LEAVES[c.key] = [];
  const usualRootId = siid++;
  SI.push({
    id: usualRootId, scheme_id: c.schemeId, parent_id: null, item_level: 1, item_code: 'USUAL',
    item_name: '平时成绩', category: 'USUAL', weight: sc.usual, full_score: 100, data_source: 'MANUAL',
    score_type: 'NUMERIC', expected_times: 0, aggregate_mode: 'WEIGHTED', is_anchor: 0, is_subjective: 0,
    scorer_role: 'TEACHER', sort_no: 10, status: 'ACTIVE', create_by: 1431, create_time: T8, update_time: T8
  });
  sc.items.forEach((it, i) => {
    const id = siid++;
    ITEM_ID[c.schemeId][it.code] = id;
    LEAVES[c.key].push({ ...it, isFinal: false });
    SI.push({
      id, scheme_id: c.schemeId, parent_id: usualRootId, item_level: 2, item_code: it.code, item_name: it.name,
      category: it.cat, weight: it.w, full_score: 100,
      data_source: it.src === 'manual' ? 'MANUAL' : 'AUTO', score_type: 'NUMERIC',
      expected_times: it.times, aggregate_mode: 'AVG', is_anchor: 0,
      is_subjective: it.cat === 'PROJECT' || it.cat === 'PERFORMANCE' ? 1 : 0,
      scorer_role: 'TEACHER', sort_no: (i + 1) * 10, status: 'ACTIVE',
      create_by: 1431, create_time: T8, update_time: T8
    });
  });
  const finalRootId = siid++;
  SI.push({
    id: finalRootId, scheme_id: c.schemeId, parent_id: null, item_level: 1, item_code: 'FINAL_ROOT',
    item_name: sc.finalName, category: 'FINAL', weight: sc.finalW === undefined ? 60 : sc.finalW,
    full_score: 100, data_source: 'MANUAL', score_type: 'NUMERIC', expected_times: 0,
    aggregate_mode: 'WEIGHTED', is_anchor: 0, is_subjective: 0, scorer_role: 'TEACHER', sort_no: 20,
    status: 'ACTIVE', create_by: 1431, create_time: T8, update_time: T8
  });
  const fid = siid++;
  ITEM_ID[c.schemeId][sc.final.code] = fid;
  LEAVES[c.key].push({ ...sc.final, w: 100, isFinal: true });
  SI.push({
    id: fid, scheme_id: c.schemeId, parent_id: finalRootId, item_level: 2, item_code: sc.final.code,
    item_name: sc.final.name, category: sc.final.cat, weight: 100, full_score: 100,
    data_source: sc.final.src === 'manual' ? 'MANUAL' : 'AUTO',
    score_type: 'NUMERIC', expected_times: sc.final.times, aggregate_mode: 'AVG', is_anchor: 0,
    is_subjective: 0, scorer_role: 'TEACHER', sort_no: 10, status: 'ACTIVE',
    create_by: 1431, create_time: T8, update_time: T8
  });
});
ins('score_scheme_item', SI);

// ---------- 6) 课次 ----------
out.push('-- 6) 课次（按课程周次：12 周或 16 周；点名到 75%，其余未点名 → 覆盖率 75%）');
const SESS = [];
let sessId = 40001;
CLASSES.forEach((c) => {
  const course = COURSES.find((x) => x.key === c.key);
  const calledWeeks = Math.round(course.weeks * 0.75); // 12 周 → 9 次，16 周 → 12 次
  for (let w = 1; w <= course.weeks; w++) SESS.push({
    id: sessId++, tcId: c.id, offeringId: c.offeringId, week: w, weeks: course.weeks,
    date: weekDate(TERM5_START, w), called: w <= calledWeeks
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
// 现实分布：绝大多数学生缺课 0~2 次；每班只留 0~2 人越过 1/3 硬线（12 周允许 4 次、16 周允许 5 次）
out.push('-- 7) 考勤（已点名课次逐人一条；每班 0~2 人超缺课硬线）');
const allowedOf = (weeks) => Math.floor(weeks / 3); // 12 周 → 4，16 周 → 5
const hardOfClass = {};
CLASSES.forEach((c) => {
  // 演示学生 20250001 在四史概述里占一个「超限」名额，其余班保持 1~2 人
  const list = studentsOf(c.acId).filter((s) => s.id !== 1);
  const course = COURSES.find((x) => x.key === c.key);
  const allowed = allowedOf(course.weeks);
  let cnt = c.retake ? 1 : (c.id % 3 === 1 ? 2 : 1); // 每班 1~2 人越过硬线
  if (c.key === 'C' && c.acId === 1) cnt = Math.max(0, cnt - 1);
  const offset = (c.id % 5) * 3;
  const set = new Set();
  for (let k = 0; k < cnt && k < list.length; k++) set.add(list[(offset + k) % list.length].id);
  // 目标缺课次数：超上限 1~2 次
  hardOfClass[c.id] = { set, target: allowed + 1 + Math.floor(rnd() * 2) };
});
const ATT = [];
const absentCount = {};
const lateCount = {};
const leaveCount = {};
let attId = 500001;
const byTc = {};
SESS.filter((s) => s.called).forEach((s) => { (byTc[s.tcId] = byTc[s.tcId] || []).push(s); });
Object.entries(byTc).forEach(([tcIdStr, sessions]) => {
  const c = CLASSES.find((x) => x.id === Number(tcIdStr));
  const course = COURSES.find((x) => x.key === c.key);
  const allowed = allowedOf(course.weeks);
  const hardInfo = hardOfClass[c.id];
  studentsOf(c.acId).forEach((st) => {
    const e = ENROLL.find((x) => x.studentId === st.id && x.tcId === c.id);
    if (!e) return;
    const isHard = hardInfo.set.has(st.id);
    const isDemoStudent = st.id === 1;
    // 目标次数：硬线学生超上限；普通学生 0~3 次（不超过上限）
    const r = rnd();
    let targetAbsent;
    if (isDemoStudent) {
      // 演示学生：计基刚好到上限（再缺一次就挂）、四史已超限、其余正常出勤
      targetAbsent = c.key === 'A' ? allowed : (c.key === 'C' ? allowed + 1 : 0);
    } else if (isHard) targetAbsent = hardInfo.target;
    else if (r < 0.58) targetAbsent = 0;
    else if (r < 0.82) targetAbsent = 1;
    else if (r < 0.93) targetAbsent = 2;
    else targetAbsent = Math.min(allowed, 3);
    let targetLate = isDemoStudent ? 1 : (rnd() < 0.55 ? 0 : (rnd() < 0.8 ? 1 : 2));
    let targetLeave = isDemoStudent ? 2 : (rnd() < 0.7 ? 0 : (rnd() < 0.85 ? 1 : 2));
    let leftAbsent = targetAbsent, leftLate = targetLate, leftLeave = targetLeave;
    const n = sessions.length;
    sessions.forEach((sess, idx) => {
      const remaining = n - idx;
      let status = 'PRESENT';
      if (leftAbsent > 0 && rnd() < leftAbsent / remaining) {
        status = 'ABSENT'; leftAbsent--;
      } else if (leftLate > 0 && rnd() < leftLate / remaining) {
        status = 'LATE'; leftLate--;
      } else if (leftLeave > 0 && rnd() < leftLeave / remaining) {
        status = 'LEAVE'; leftLeave--;
      }
      if (status === 'ABSENT') absentCount[e.id] = (absentCount[e.id] || 0) + 1;
      if (status === 'LATE') lateCount[e.id] = (lateCount[e.id] || 0) + 1;
      if (status === 'LEAVE') leaveCount[e.id] = (leaveCount[e.id] || 0) + 1;
      ATT.push({
        id: attId++, enrollment_id: e.id, student_id: st.id, session_id: sess.id, term_id: 5, status,
        leave_type: status === 'LEAVE' ? pick(['PERSONAL', 'SICK', 'OFFICIAL']) : null,
        phase: 'NORMAL', source: 'TEACHER', recorded_by: 1431, recorded_role: 'TEACHER',
        recorded_at: sess.date + ' 09:45:00', batch_key: 'RC-' + sess.id, is_void: 0,
        create_time: '2026-09-01 10:00:00', update_time: '2026-09-01 10:00:00'
      });
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

// ---------- 8) 分项得分（试卷/项目自动 + 手工项）+ 成绩单 ----------
// 课堂测试、课后作业 ← 试卷（每份卷绑定 scheme_item_id，按次均分）
// 三级/四级项目、大作业 ← 项目模块（个人提交 / 团队提交 + 贡献表）
// 课堂表现、期中考试、期末考试 ← 教师手工录入（score_item）
out.push('-- 8) 分项得分：课堂测试/作业来自试卷、项目类来自项目模块、课堂表现/期中/期末手工录入');
// 3 名“多门不及格”学生：让导员端「重点关注（≥3 门风险课程）」有数据
const WEAK = new Set(studentsOf(1).slice(10, 13).map((s) => s.id));
// 各评分项的相对松紧（add 偏移、sd 标准差）：课堂测试略严、作业偏松、期末最严
const ITEM_TUNE = {
  QUIZ: { add: -4, sd: 9 },
  HW: { add: 11, sd: 7 },
  P3: { add: 5, sd: 8 },
  P4: { add: 4, sd: 8 },
  PERF: { add: 9, sd: 6 },
  MID: { add: -3, sd: 10 },
  FINAL_EXAM: { add: -2, sd: 10 },
  ASSIGN: { add: 3, sd: 8 }
};
const clampScore = (v) => Math.max(0, Math.min(100, v));
// ---- 生成规则 ----
// ① 课堂测试 / 课后作业：每个班每项生成 N 份试卷，卷面绑定 scheme_item_id；分项得分 = 各次均分
// ② 三级/四级项目、大作业：项目模块落任务 + 个人/团队提交 + 贡献表；个人得分 = 团队分 × 占比 × 人数
// ③ 课堂表现 / 期中 / 期末考试：教师手工录入（score_item）
const PAPERS = [], QS = [], SUBS = [], SUB_ANS = [];
const TASKS = [], TEAMS = [], TSUBS = [], TCONTRIS = [];
const SCORE_ITEMS = [];
const ITEM_SCORE = {};
const RESULTS = [];
let pid = 20001, qid = 21001, subId = 800001, ansId = 900001;
let taskId = 3001, teamId = 3501, tsubId = 36001, contriId = 37001;
let sciId = 600001, resId = 700001;

const ABILITY = {};
ENROLL.forEach((e) => {
  let a = 66 + rnd() * 30;
  if (WEAK.has(e.studentId)) a -= 12;
  if (rnd() < 0.04) a -= 6;
  ABILITY[e.id] = a;
});
const scoreOf = (e, code) => {
  const t = ITEM_TUNE[code] || { add: 0, sd: 10 };
  return clampScore(ABILITY[e.id] + t.add + (rnd() - 0.5) * 2 * t.sd);
};
const paperWeek = (weeks, idx, total) => {
  const step = Math.max(1, Math.floor((weeks - 2) / total));
  const w = 2 + idx * step;
  return Math.min(w, Math.max(2, weeks - 1));
};

CLASSES.forEach((c) => {
  const course = COURSES.find((x) => x.key === c.key);
  const sc = SCHEMES[c.key];
  const list = studentsOf(c.acId).map((st) => ENROLL.find((x) => x.studentId === st.id && x.tcId === c.id));
  list.forEach((e) => { ITEM_SCORE[e.id] = {}; });

  // ① 试卷类分项
  sc.items.filter((it) => it.src === 'paper').forEach((it) => {
    const isQuiz = it.code === 'QUIZ';
    const perTime = [];
    for (let t = 1; t <= it.times; t++) {
      const day = weekDate(TERM5_START, paperWeek(course.weeks, t, it.times));
      const p = {
        id: pid++, tcId: c.id, offeringId: c.offeringId, type: isQuiz ? 'QUIZ' : 'HOMEWORK',
        itemId: ITEM_ID[course.schemeId][it.code], day,
        name: course.name + (isQuiz ? ' · 第' + t + '次随堂测验' : ' · 第' + t + '次课后作业')
      };
      PAPERS.push(p);
      const q1 = { id: qid++, paperId: p.id, no: 1, full: 50, stem: p.name + '：简答题（一）' };
      const q2 = { id: qid++, paperId: p.id, no: 2, full: 50, stem: p.name + '：简答题（二）' };
      QS.push(q1, q2);
      // 演示数据：计基 01 班最后一次作业留 3 人待批；计网 01 班第一次随堂测验留 4 人缺考
      const pendingSet = (c.id === 201 && !isQuiz && t === it.times)
        ? new Set(list.slice(0, 3).map((e) => e.studentId)) : new Set();
      const absentSet = (c.id === 210 && isQuiz && t === 1)
        ? new Set(list.slice(0, 4).map((e) => e.studentId)) : new Set();
      perTime[t] = {};
      list.forEach((e) => {
        const sid = e.studentId;
        const isAbsent = absentSet.has(sid);
        const isPending = pendingSet.has(sid);
        const target = isAbsent || isPending ? 0 : r1(scoreOf(e, it.code));
        perTime[t][e.id] = target;
        const marked = !isAbsent && !isPending;
        const subIdVal = subId++;
        const a1 = marked ? Math.round(target / 2) : null;
        const a2 = marked ? Math.round(target) - a1 : null;
        SUBS.push({
          id: subIdVal, paperId: p.id, tcId: c.id, studentId: sid, enrollmentId: e.id, day: p.day,
          status: isAbsent ? 'ABSENT' : (marked ? 'MARKED' : 'SUBMITTED'),
          markedStatus: marked ? 'ALL_MARKED' : 'SYSTEM_MARKED',
          obj: 0, subj: marked ? target : 0, total: marked ? target : null,
          absent: isAbsent ? 1 : 0, duration: isAbsent ? 0 : between(600, 1180)
        });
        SUB_ANS.push({
          id: ansId++, subId: subIdVal, qId: q1.id,
          text: isAbsent ? null : (isQuiz ? '答：要点见课堂讲解。' : '答：作业按要求完成。'),
          score: isAbsent ? 0 : a1, byType: isAbsent ? 'SYSTEM' : (marked ? 'TEACHER' : null)
        });
        SUB_ANS.push({
          id: ansId++, subId: subIdVal, qId: q2.id,
          text: isAbsent ? null : (isQuiz ? '答：补充说明。' : '答：推导过程略。'),
          score: isAbsent ? 0 : a2, byType: isAbsent ? 'SYSTEM' : (marked ? 'TEACHER' : null)
        });
      });
    }
    list.forEach((e) => {
      let sum = 0;
      for (let t = 1; t <= it.times; t++) sum += perTime[t][e.id];
      ITEM_SCORE[e.id][it.code] = r2(sum / it.times);
    });
  });

  // ② 项目类分项（三级/四级项目、大作业）
  sc.items.filter((it) => it.src === 'project')
    .concat(sc.final.src === 'project' ? [sc.final] : []).forEach((it) => {
      const task = {
        id: taskId++, tcId: c.id, offeringId: c.offeringId, itemId: ITEM_ID[course.schemeId][it.code],
        name: course.name + ' · ' + it.name, isTeam: it.team ? 1 : 0, full: 100, desc: it.desc
      };
      TASKS.push(task);
      const add = (ITEM_TUNE[it.code] || { add: 0 }).add;
      if (it.team) {
        const size = it.teamSize || 5;
        for (let i = 0; i < list.length; i += size) {
          const members = list.slice(i, i + size);
          const tid = teamId++;
          const teamScore = r1(clampScore(ABILITY[members[0].id] + add + (rnd() - 0.5) * 8));
          const ratios = members.map(() => 1 / members.length);
          if (members.length > 2) {
            ratios[0] += 0.02;
            for (let k = 1; k < ratios.length; k++) ratios[k] -= 0.02 / (ratios.length - 1);
          }
          TEAMS.push({
            id: tid, taskId: task.id, name: '第' + (Math.floor(i / size) + 1) + '组',
            leader: members[0].studentId, members, ratios
          });
          TSUBS.push({
            id: tsubId++, taskId: task.id, teamId: tid, studentId: members[0].studentId,
            score: teamScore, files: '[{"name":"汇报PPT.pdf","size":2048000}]'
          });
          TCONTRIS.push({
            id: contriId++, taskId: task.id, teamId: tid, leader: members[0].studentId, members, ratios
          });
          members.forEach((e, k) => {
            ITEM_SCORE[e.id][it.code] = r2(clampScore(teamScore * ratios[k] * members.length));
          });
        }
      } else {
        list.forEach((e) => {
          const s = r1(scoreOf(e, it.code));
          TSUBS.push({
            id: tsubId++, taskId: task.id, teamId: null, studentId: e.studentId,
            score: s, files: '[{"name":"项目提交.zip","size":1048576}]'
          });
          ITEM_SCORE[e.id][it.code] = s;
        });
      }
    });

  // ③ 手工项
  sc.items.filter((it) => it.src === 'manual')
    .concat(sc.final.src === 'manual' ? [sc.final] : []).forEach((it) => {
      list.forEach((e) => {
        const s = r1(scoreOf(e, it.code));
        ITEM_SCORE[e.id][it.code] = s;
        SCORE_ITEMS.push({
          id: sciId++, enrollment_id: e.id, student_id: e.studentId,
          scheme_item_id: ITEM_ID[course.schemeId][it.code], term_id: 5, times_no: 1, score: s,
          full_score: 100, is_absent: 0, source: 'MANUAL', scorer_id: 1431, scorer_role: 'TEACHER',
          scored_at: '2026-10-10 10:00:00', is_void: 0,
          create_by: 1431, create_time: '2026-10-10 10:00:00', update_time: '2026-10-10 10:00:00'
        });
      });
    });
});

// ④ 成绩单：两级折算 W，扣分 P，T = W − P
const COURSE_BY_KEY = {};
COURSES.forEach((c) => { COURSE_BY_KEY[c.key] = c; });
ENROLL.forEach((e) => {
  const sc = SCHEMES[e.key];
  const course = COURSE_BY_KEY[e.key];
  const allowed = allowedOf(course.weeks);
  const items = ITEM_SCORE[e.id] || {};
  let usualPct = 0;
  sc.items.forEach((it) => { usualPct += (items[it.code] == null ? 0 : items[it.code]) * (it.w / 100); });
  const finalPct = items[sc.final.code] == null ? 0 : items[sc.final.code];
  const weighted = usualPct * sc.usual / 100 + finalPct * sc.finalW / 100;
  const absent = absentCount[e.id] || 0;
  const late = lateCount[e.id] || 0;
  const leave = leaveCount[e.id] || 0;
  const P = 2 * absent + late + Math.max(0, leave - 3);
  const T = r2(weighted - P);
  const isHard = absent > allowed;
  RESULTS.push({
    id: resId++, enrollmentId: e.id, studentId: e.studentId, no: e.no, offeringId: e.offeringId,
    tcId: e.tcId, acId: e.acId, key: e.key, W: r1(weighted), P: r1(P), T, absent, late, leave,
    allowed, weeks: course.weeks, hard: isHard ? 1 : 0, fail: isHard || T < 60 ? 1 : 0,
    reason: isHard ? 'HARD_LINE' : (T < 60 ? 'SCORE' : null)
  });
});

// ---- 落库（按外键依赖顺序）----
ins('paper', PAPERS.map((p) => ({
  id: p.id, teaching_class_id: p.tcId, offering_id: p.offeringId, term_id: 5, paper_type: p.type,
  paper_name: p.name, start_time: p.day + ' 08:00:00', end_time: p.day + ' 23:59:00', duration_minutes: 20,
  full_score: 100, scheme_item_id: p.itemId, publisher_role: 'TEACHER', publisher_id: 1431,
  allow_view_paper: 1, show_score: 1, show_answer: 'AFTER_MARK', status: 'CLOSED',
  create_by: 1431, create_time: p.day + ' 07:00:00', update_time: p.day + ' 23:59:00'
})));
ins('question', QS.map((q) => ({
  id: q.id, paper_id: q.paperId, q_no: q.no, q_type: 'SHORT', stem: q.stem,
  answer_json: JSON.stringify({ ref: '按课堂讲解要点给分。' }), full_score: q.full,
  group_name: '简答题', sort_no: q.no, create_time: '2026-09-20 10:00:00', update_time: '2026-09-20 10:00:00'
})));
ins('submission', SUBS.map((s) => ({
  id: s.id, paper_id: s.paperId, teaching_class_id: s.tcId, term_id: 5, student_id: s.studentId,
  enrollment_id: s.enrollmentId, submit_status: s.status,
  start_time: s.absent ? null : s.day + ' 08:02:00', submit_time: s.absent ? null : s.day + ' 08:20:00',
  duration_seconds: s.duration, objective_score: s.obj, subjective_score: s.subj, total_score: s.total,
  marked_status: s.markedStatus, marked_by: s.markedStatus === 'ALL_MARKED' ? 1431 : null,
  marked_at: s.markedStatus === 'ALL_MARKED' ? s.day + ' 23:59:00' : null, is_absent: s.absent,
  create_time: s.day + ' 08:02:00', update_time: s.day + ' 23:59:00'
})));
ins('submission_answer', SUB_ANS.map((a) => ({
  id: a.id, submission_id: a.subId, question_id: a.qId,
  answer_json: a.text == null ? null : JSON.stringify({ text: a.text }),
  score: a.score, is_correct: null, marked_by_type: a.byType,
  marked_by: a.byType === 'TEACHER' ? 1431 : null,
  marked_at: a.byType ? '2026-10-10 23:59:00' : null,
  create_time: '2026-10-10 08:02:00', update_time: '2026-10-10 23:59:00'
})));
ins('project_task', TASKS.map((t) => ({
  id: t.id, teaching_class_id: t.tcId, offering_id: t.offeringId, term_id: 5,
  project_name: t.name, description: t.desc, start_time: '2026-09-15 08:00:00', end_time: '2026-12-20 23:59:00',
  is_team: t.isTeam, full_score: t.full, allow_formats: '.zip,.pdf,.pptx,.docx,.java',
  scheme_item_id: t.itemId, publisher_id: 1431, status: 'CLOSED',
  create_by: 1431, create_time: '2026-09-10 09:00:00', update_time: '2026-12-20 23:59:00'
})));
ins('project_team', TEAMS.map((t) => ({
  id: t.id, project_id: t.taskId, team_name: t.name, leader_student_id: t.leader,
  member_json: JSON.stringify(t.members.map((e, i) => ({
    studentId: e.studentId, studentNo: e.no, studentName: e.name, isLeader: i === 0
  }))),
  member_count: t.members.length,
  create_time: '2026-09-20 10:00:00', update_time: '2026-09-20 10:00:00'
})));
ins('project_submission', TSUBS.map((s) => ({
  id: s.id, project_id: s.taskId, team_id: s.teamId, student_id: s.studentId,
  submit_time: '2026-12-15 20:00:00', files_json: s.files, submit_status: 'SUBMITTED',
  project_score: s.score, comment: '完成度良好',
  marked_by: 1431, marked_at: '2026-12-20 18:00:00',
  create_time: '2026-12-15 20:00:00', update_time: '2026-12-20 18:00:00'
})));
ins('project_contribution', TCONTRIS.map((c) => ({
  id: c.id, project_id: c.taskId, team_id: c.teamId, leader_student_id: c.leader,
  contribution_json: JSON.stringify(c.members.map((e, i) => ({
    studentId: e.studentId, studentNo: e.no, studentName: e.name,
    ratio: c.ratios[i].toFixed(4), note: i === 0 ? '组长，负责统筹与答辩' : '按分工完成'
  }))),
  total_ratio: 1.0, submit_time: '2026-12-16 21:00:00', is_locked: 1,
  create_time: '2026-12-16 21:00:00'
})));
ins('score_item', SCORE_ITEMS);
ins('score_result', RESULTS.map((r) => ({
  id: r.id, enrollment_id: r.enrollmentId, student_id: r.studentId, offering_id: r.offeringId, term_id: 5,
  weighted_score: r.W, deduction_points: r.P, total_score: r.T, pass_score: 60, absent_count: r.absent,
  late_count: r.late, leave_count: r.leave, allowed_absent: r.allowed, effective_sessions: r.weeks,
  hard_fail: r.hard, is_fail: r.fail, fail_reason: r.reason, score_status: 'FINAL', retake_times: 0,
  engine_version: '1-10.v1', calculated_at: '2026-10-15 18:00:00', is_void: 0,
  create_time: '2026-10-15 18:00:00', update_time: '2026-10-15 18:00:00'
})));

// ---------- 9) 题目模板（供 14.5 两张「进行中」的试卷使用） ----------
out.push('-- 9) 题目模板：进行中的试卷沿用同一套 6 道题');
const Q_DEFS = [
  { t: 'SINGLE', s: 10, stem: '下列哪一项属于输入设备？', opt: [{ key: 'A', text: '显示器' }, { key: 'B', text: '键盘' }, { key: 'C', text: '打印机' }, { key: 'D', text: '音箱' }], ans: { keys: ['B'] } },
  { t: 'SINGLE', s: 10, stem: '1 KB 等于多少字节？', opt: [{ key: 'A', text: '1000' }, { key: 'B', text: '1024' }, { key: 'C', text: '512' }, { key: 'D', text: '2048' }], ans: { keys: ['B'] } },
  { t: 'MULTI', s: 15, stem: '下列属于操作系统的有？', opt: [{ key: 'A', text: 'Linux' }, { key: 'B', text: 'Windows' }, { key: 'C', text: 'Word' }, { key: 'D', text: 'macOS' }], ans: { keys: ['A', 'B', 'D'] } },
  { t: 'JUDGE', s: 15, stem: '断电后内存中的数据不会丢失。', ans: { value: false } },
  { t: 'BLANK', s: 20, stem: '计算机的运算器和控制器合称为＿＿＿＿；内存储器断电后数据会＿＿＿＿。', ans: { answers: ['中央处理器', '丢失'], ignoreCase: true, trim: true } },
  { t: 'SHORT', s: 30, stem: '简述结构化程序设计的基本思想。', ans: { ref: '自顶向下、逐步细化、模块化，限制使用 goto。' } }
];

// ---------- 10) 风险状态 / 预警 / 时间线 / 干预 ----------
// 不再手写：重建后调用 POST /api/engine/evaluate（admin）由风险引擎按冻结口径真实生成
//   risk_state（一人一课一行）+ alert_record（绿→黄/橙/红生成、升级、自动关闭）+ alert_flow_log
//   演示用的「处理中 / 已申诉」由 tool/stage2_demo_data.ps1 通过真实接口补造
out.push('-- 10) 风险状态与预警：由风险引擎在重建后生成（本脚本不手写）');

// ---------- 11) 三维挂科率 / 教师尺度 ----------
out.push('-- 11) 三维挂科率与教师尺度（真实口径，无合成标记）');
ins('course_fail_rate_history', COURSES.map((c, i) => {
  const rows = RESULTS.filter((r) => r.key === c.key);
  const fail = rows.filter((r) => r.fail).length;
  return {
    id: 2001 + i, course_id: c.courseId, term_id: 5, examinee_count: rows.length, fail_count: fail,
    fail_rate: r2(fail * 100 / rows.length), hard_fail_count: rows.filter((r) => r.hard).length,
    teaching_class_count: CLASSES.filter((x) => x.key === c.key).length, is_initialized: 0,
    engine_version: '1-10.v1', calc_time: '2026-10-20 09:00:00',
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
    engine_version: '1-10.v1', calc_time: '2026-10-20 09:00:00',
    create_time: '2026-10-20 09:00:00', update_time: '2026-10-20 09:00:00'
  };
}));
ins('admin_class_fail_rate', ADMIN_CLASSES.map((ac, i) => {
  const rows = RESULTS.filter((r) => r.acId === ac.id);
  const fail = rows.filter((r) => r.fail).length;
  return {
    id: 5001 + i, admin_class_id: ac.id, offering_id: 101, course_id: 1, term_id: 5,
    examinee_count: rows.length, fail_count: fail, fail_rate: r2(fail * 100 / rows.length),
    hard_fail_count: rows.filter((r) => r.hard).length, engine_version: '1-10.v1',
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
      std_dev: r2(8 + rnd() * 4), fused: 0, status: 'ACTIVE', engine_version: '1-10.v1',
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
  { id: 9001, biz_type: 'ALERT_NEW', title: '课程学情预警：缺课次数已接近允许上限', content: '你在《大学计算机基础（一）》的缺课次数已接近允许上限，按缺课超 1/3 硬门槛会直接挂科。', audience_desc: '学生 20250001', priority: 'HIGH' },
  { id: 9002, biz_type: 'SCORE_TASK', title: '《大学计算机基础（一）》平时成绩已更新', content: '课堂测试、课后作业与三级项目均已录入，可到「我的成绩」查看分项构成。', audience_desc: '学生 20250001', priority: 'NORMAL' },
  { id: 9003, biz_type: 'SCORE_TASK', title: '四史概述·第1次课后作业：3 份待批阅', content: '还有 3 名学生的填空/简答题未给分。', audience_desc: '教师 1001', priority: 'HIGH' },
  { id: 9004, biz_type: 'COVERAGE_REMIND', title: '点名覆盖率提醒：CS-01-A 后 3 周未点名', content: '最近 3 次课未点名，点名覆盖率 75%，请在课后补录。', audience_desc: '教师 1001', priority: 'NORMAL' },
  { id: 9005, biz_type: 'CLASS_ALERT', title: '班集体提醒：计算机1班本周有 2 人缺课超限', content: '本周风险学生 2 人、占比 4%，建议课后个别谈话。', audience_desc: '导员 马思国', priority: 'NORMAL' },
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
  retake_times: 0, engine_version: 'SYN.v1', calculated_at: '2028-01-10 18:00:00', is_void: 0,
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
  + ' 课次=' + SESS.length + ' 考勤=' + ATT.length + ' 分项(手工)=' + SCORE_ITEMS.length
  + ' 成绩单=' + RESULTS.length + ' 试卷=' + PAPERS.length + ' 题=' + QS.length
  + ' 提交=' + SUBS.length + ' 逐题=' + SUB_ANS.length
  + ' 项目任务=' + TASKS.length + ' 团队=' + TEAMS.length + ' 项目提交=' + TSUBS.length
  + ' 历史成绩=' + H_RESULT.length + '（风险状态与预警由引擎重建后生成）');
// 生成质量自检：每门课挂科率、综合分区间（用于把数据调到"像真的"）
COURSES.forEach((c) => {
  const rows = RESULTS.filter((r) => r.key === c.key);
  const fail = rows.filter((r) => r.fail).length;
  const hard = rows.filter((r) => r.hard).length;
  const ts = rows.map((r) => r.T).sort((a, b) => a - b);
  const pct = (q) => ts[Math.min(ts.length - 1, Math.floor(ts.length * q))];
  console.log('  ' + c.name + '：' + rows.length + ' 人，挂科 ' + fail + '（' + (fail * 100 / rows.length).toFixed(1)
    + '%），缺课硬线 ' + hard + '，综合分 P10=' + pct(0.1) + ' 中位=' + pct(0.5) + ' P90=' + pct(0.9));
});
