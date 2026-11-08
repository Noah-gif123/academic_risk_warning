#!/usr/bin/env node
/**
 * v3.0 独立拟真数据生成器（P0-4）。
 *
 * 设计目标（对应 PLAN §B2 / 规划清单 P0-4）：
 *   - 参数化：所有规模/口径来自 v3.0/sql/seeds/params.json；
 *   - 可复现：同一 seed 生成同一份 SQL；
 *   - 独立库：只写 v3.0/sql/seeds/v3_seed.sql，由 bootstrap_v3.ps1 导入 study_warning_system_v3，
 *     v2.0 演示库 study_warning_system_v2 完全不动；
 *   - 主链路优先：学期/教师/学生/课程/教学班/选课/成绩构成/课次/考勤/成绩单/
 *     历史挂科/课程挂科率/教师尺度/对照组；试卷·项目·问卷暂不生成（P2 前补齐）。
 *
 * 用法：node v3.0/tool/gen_v3_seed.js
 */
'use strict';

const fs = require('fs');
const path = require('path');

const V3 = path.join(__dirname, '..');                 // v3.0
const PARAMS = JSON.parse(fs.readFileSync(path.join(V3, 'sql', 'seeds', 'params.json'), 'utf8'));
const OUT_SQL = path.join(V3, 'sql', 'seeds', 'v3_seed.sql');
const OUT_COHORT = path.join(V3, 'sql', 'seeds', 'v3_cohort.json');
const OUT_STORY = path.join(V3, 'sql', 'seeds', 'data_story.md');

// ============================================================ 确定性随机
let seed = PARAMS.seed >>> 0;
function rnd() {
  seed = (seed * 1664525 + 1013904223) >>> 0;
  return seed / 4294967296;
}
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const rf = (a, b) => a + rnd() * (b - a);
const r1 = (x) => Math.round(x * 10) / 10;
const r2 = (x) => Math.round(x * 100) / 100;
const r3 = (x) => Math.round(x * 1000) / 1000;
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));
function gauss() {
  // 三个均匀分布之和，近似 N(0, 0.5)；用固定公式保证可复现
  return (rnd() + rnd() + rnd() - 1.5) * 0.8165;
}
function gaussZ() {
  // 标准正态近似（sd=1）
  return gauss() * 2.449;
}
function normInv(p) {
  // Acklam 逆正态近似（p∈(0,1)）
  const a = [-39.69683028665376, 220.9460984245205, -275.9285104469687,
    138.357751867269, -30.66479806614716, 2.506628277459239];
  const b = [-54.47609879822406, 161.5858368580409, -155.6989798598866,
    66.80131188771972, -13.28068155288572];
  const c = [-0.007784894002430293, -0.3223964580411365, -2.400758277161838,
    -2.549732539343734, 4.374664141464968, 2.938163982698783];
  const d = [0.007784695709041462, 0.3224671290700398, 2.445134137142996,
    3.754408661907416];
  const pl = 0.02425;
  if (p < pl) {
    const q = Math.sqrt(-2 * Math.log(p));
    return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
      ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
  }
  if (p > 1 - pl) {
    const q = Math.sqrt(-2 * Math.log(1 - p));
    return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
      ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
  }
  const q = p - 0.5;
  const r = q * q;
  return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
}

// ============================================================ SQL 输出
const out = [];
const esc = (v) => {
  if (v === null || v === undefined) return 'NULL';
  if (typeof v === 'number') return String(v);
  return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
};
const del = (sql) => out.push(sql + ';');
function ins(table, rows, chunk = 200) {
  if (!rows || rows.length === 0) return;
  const cols = Object.keys(rows[0]);
  for (let i = 0; i < rows.length; i += chunk) {
    out.push('INSERT INTO ' + table + ' (' + cols.join(', ') + ') VALUES\n' +
      rows.slice(i, i + chunk).map((r) => '  (' + cols.map((c) => esc(r[c])).join(', ') + ')').join(',\n') + ';');
  }
}
const pad = (n, w = 2) => String(n).padStart(w, '0');
const dateStr = (d) => d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
const dtStr = (d, hm = '10:00:00') => dateStr(d) + ' ' + hm;
function addDays(base, days) {
  const d = new Date(base.getTime());
  d.setDate(d.getDate() + days);
  return d;
}

// ============================================================ 基础参数
const CUR = PARAMS.terms.current;
const HIST = PARAMS.terms.history;
const COURSES = PARAMS.courses;
const ACS = PARAMS.adminClasses;
const COURSE_BY_KEY = {};
COURSES.forEach((c) => { COURSE_BY_KEY[c.key] = c; });
const TERM_START = new Date(CUR.startDate + 'T08:00:00');
const T_NOW = '2026-10-20 08:00:00';

const POOLS = { A: [1, 6], B: [2, 12], C: [3], D: [4, 13], E: [5, 10], F: [7], G: [8], H: [9, 14], I: [11] };
const teacherFor = (course, ac) => {
  const pool = POOLS[course.key];
  if (!pool) return 1;
  return pool.length > 1 ? pool[ac.grade === 2025 ? 0 : 1] : pool[0];
};

// ============================================================ 学期
const TERMS = [
  ...HIST.map((h) => ({
    id: h.id, term_code: h.code,
    school_year: h.code.slice(0, 9), term_no: Number(h.code.slice(-1)),
    term_name: h.name, start_date: h.startDate, end_date: h.endDate,
    total_weeks: 16, is_current: 0, is_synthetic: 0, status: 'CLOSED',
    create_time: '2024-08-20 09:00:00', update_time: '2024-08-20 09:00:00'
  })),
  {
    id: CUR.id, term_code: CUR.code,
    school_year: CUR.code.slice(0, 9), term_no: Number(CUR.code.slice(-1)),
    term_name: CUR.name, start_date: CUR.startDate, end_date: CUR.endDate,
    total_weeks: 16, is_current: 1, is_synthetic: 0, status: 'ACTIVE',
    create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
  }
];

// ============================================================ 教师
const TEACHERS = PARAMS.teachers.names.slice(0, PARAMS.teachers.count).map((name, i) => ({
  id: i + 1,
  no: String(1001 + i),
  name,
  accountId: 1501 + i,
  gender: i % 3 === 0 ? 2 : 1,
  strict: r2(0.6 - (i % 6) * 0.4 + rf(-0.15, 0.15)) // +0.6(松) … -1.6(严)
}));
const TEACHER_BY_ID = {};
TEACHERS.forEach((t) => { TEACHER_BY_ID[t.id] = t; });

// ============================================================ 学生
const SURNAME = '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜'.split('');
const GIVEN = ['伟', '芳', '娜', '敏', '静', '丽', '强', '磊', '洋', '艳', '勇', '军', '杰', '娟', '涛',
  '明', '超', '秀英', '霞', '平', '鹏', '辉', '雪', '琳', '晨', '宇', '鑫', '婷', '浩', '雨'];
const STUDENTS = [];
const usedNames = new Set();
let nextSid = 1001;
ACS.forEach((ac) => {
  for (let i = 0; i < ac.size; i++) {
    const keep = ac.id === 1 && i === 0;
    let name = '学生1';
    if (!keep) {
      for (let k = 0; k < 50; k++) {
        const cand = pick(SURNAME) + pick(GIVEN);
        if (!usedNames.has(cand)) { usedNames.add(cand); name = cand; break; }
      }
      if (name === '学生1') name = '学生' + nextSid;
    }
    STUDENTS.push({
      id: keep ? 1 : nextSid++,
      no: keep ? '20250001' : ac.prefix + pad(ac.from + i, 4),
      name,
      gender: rnd() < 0.55 ? 1 : 2,
      year: ac.grade,
      acId: ac.id,
      accountId: keep ? 631 : null,
      ability: clamp(gaussZ(), -2.6, 2.6)
    });
  }
});
STUDENTS.filter((s) => s.id !== 1).forEach((s) => { s.accountId = s.id + 1000; });
const studentsOf = (acId) => STUDENTS.filter((s) => s.acId === acId);

// ============================================================ 教学班
const CLASSES = [];
let tcid = 201;
ACS.forEach((ac) => {
  const keys = PARAMS.classPlan.coursesByClass[String(ac.grade)];
  keys.forEach((key) => {
    const c = COURSE_BY_KEY[key];
    // 每班 3 名学生标记为高缺勤倾向（用于制造缺课硬线/红色预警），确定性按 id 取
    CLASSES.push({
      id: tcid++, offeringId: c.offeringId, key, acId: ac.id, retake: 0,
      teacherId: teacherFor(c, ac),
      code: ac.code + '-' + key,
      name: c.name + ' ' + ac.name,
      invite: String(310100 + tcid)
    });
  });
});
PARAMS.classPlan.retake.forEach((r) => {
  const c = COURSE_BY_KEY[r.courseKey];
  const ac = ACS.find((a) => a.id === r.adminClassId);
  CLASSES.push({
    id: tcid++, offeringId: c.offeringId, key: r.courseKey, acId: ac.id, retake: 1,
    teacherId: POOLS[r.courseKey][0],
    code: ac.code + '-RT-' + r.courseKey,
    name: c.name + ' ' + ac.name + '（重修）',
    invite: String(310100 + tcid)
  });
});
const CLASS_BY_ID = {};
CLASSES.forEach((c) => { CLASS_BY_ID[c.id] = c; });

// 每班高缺勤学生
const HIGH_ABSENT = new Set();
CLASSES.forEach((cl) => {
  studentsOf(cl.acId).slice(0, 3).forEach((s) => HIGH_ABSENT.add(s.id + '#' + cl.id));
});

// ============================================================ 课程成绩构成模板
const TEMPLATES = {
  A: { usual: 40, finalW: 60, items: [['QUIZ', '课堂测试', 'QUIZ', 35, 7, 0], ['P3', '三级项目', 'PROJECT', 20, 1, 1], ['PERF', '课堂表现', 'PERFORMANCE', 5, 1, 0], ['HW', '课后作业', 'HOMEWORK', 40, 4, 0]], final: ['FINAL_EXAM', '期末考试', 'FINAL', 100, 1] },
  B: { usual: 40, finalW: 60, items: [['QUIZ', '课堂测试', 'QUIZ', 30, 6, 0], ['MID', '期中考试', 'MIDTERM', 20, 1, 0], ['HW', '课后作业', 'HOMEWORK', 50, 5, 0]], final: ['FINAL_EXAM', '期末考试', 'FINAL', 100, 1] },
  C: { usual: 60, finalW: 40, items: [['QUIZ', '课堂测试', 'QUIZ', 35, 7, 0], ['P3', '三级项目', 'PROJECT', 20, 1, 1], ['P4', '四级项目', 'PROJECT', 40, 1, 1], ['PERF', '课堂表现', 'PERFORMANCE', 5, 1, 0]], final: ['FINAL_EXAM', '期末考试', 'FINAL', 100, 1] },
  D: { usual: 40, finalW: 60, items: [['QUIZ', '课堂测试', 'QUIZ', 35, 7, 0], ['P3', '三级项目', 'PROJECT', 25, 1, 1], ['HW', '课后作业', 'HOMEWORK', 40, 4, 0]], final: ['ASSIGN', '大作业', 'FINAL', 100, 1] },
  E: { usual: 50, finalW: 50, items: [['QUIZ', '课堂测试', 'QUIZ', 30, 6, 0], ['MID', '期中考试', 'MIDTERM', 20, 1, 0], ['HW', '课后作业', 'HOMEWORK', 50, 5, 0]], final: ['FINAL_EXAM', '期末考试', 'FINAL', 100, 1] }
};

// ============================================================ 班/课效应（先算分再校准）
const CLASS_EFFECT = {};       // key -> { acId -> effect }
const CLASS_SD_EXTRA = {};     // key -> { acId -> extra noise scale }
COURSES.forEach((c) => {
  CLASS_EFFECT[c.key] = {};
  CLASS_SD_EXTRA[c.key] = {};
  // 该课程本学期开给哪些行政班：按名单排序后均匀铺开到 [-4.5, +4.5]，
  // 使同课程班间差稳定落在 6–12 分（配 0.9 系数与随机性后约 7–11）。
  const acs = ACS.filter((ac) => PARAMS.classPlan.coursesByClass[String(ac.grade)].includes(c.key));
  acs.sort((a, b) => a.id - b.id);
  acs.forEach((ac, i) => {
    const base = acs.length > 1 ? (i / (acs.length - 1) - 0.5) * 9.0 : 0;
    const jitter = ((ac.id * 7 + c.id * 3) % 3 - 1) * 0.6;
    // 抵消教师尺度对班均的影响（score 里是 -te*3，这里补偿 te*3/0.9），保证同课程班间差稳定
    const te = TEACHER_BY_ID[teacherFor(c, ac)].strict;
    CLASS_EFFECT[c.key][ac.id] = r2(base + jitter + (te * 3.0) / 0.9);
    CLASS_SD_EXTRA[c.key][ac.id] = 0.85 + ((ac.id * 7 + c.id * 5) % 5) * 0.08;
  });
});

// ============================================================ 生成成绩分项（当前学期）
const ENROLL = [];
let eid = 20001;
CLASSES.forEach((cl) => {
  studentsOf(cl.acId).forEach((s) => {
    ENROLL.push({ id: eid++, student: s, cl, course: COURSE_BY_KEY[cl.key], key: cl.key, acId: cl.acId });
  });
});

const SCHEME = {}; // key -> { roots, leaves }
COURSES.forEach((c, ci) => {
  const tpl = TEMPLATES[c.schemeTemplate];
  const schemeId = c.schemeId;
  const usualRoot = schemeId * 100 + 1;
  const finalRoot = schemeId * 100 + 2;
  const roots = [
    { id: usualRoot, scheme_id: schemeId, parent_id: null, item_level: 1, item_code: 'USUAL', item_name: '平时成绩', category: 'USUAL', weight: tpl.usual, full_score: 100, data_source: 'MANUAL', score_type: 'NUMERIC', expected_times: 1, sort_no: 1 },
    { id: finalRoot, scheme_id: schemeId, parent_id: null, item_level: 1, item_code: 'FINAL', item_name: '终结性考核', category: 'FINAL', weight: tpl.finalW, full_score: 100, data_source: 'MANUAL', score_type: 'NUMERIC', expected_times: 1, sort_no: 2 }
  ];
  const leaves = [];
  tpl.items.forEach((it, i) => {
    leaves.push({ id: schemeId * 100 + 10 + i, scheme_id: schemeId, parent_id: usualRoot, item_level: 2, item_code: it[0], item_name: it[1], category: it[2], weight: it[3], full_score: 100, data_source: 'MANUAL', score_type: 'NUMERIC', expected_times: it[4], sort_no: 10 + i, project: it[5] });
  });
  leaves.push({ id: schemeId * 100 + 90, scheme_id: schemeId, parent_id: finalRoot, item_level: 2, item_code: tpl.final[0], item_name: tpl.final[1], category: tpl.final[2], weight: tpl.final[3], full_score: 100, data_source: 'MANUAL', score_type: 'NUMERIC', expected_times: tpl.final[4], sort_no: 90, terminal: true });
  SCHEME[c.key] = { roots, leaves };
});

// 每门课历史挂科率目标（3%–25%），按课程×学期确定性取值
const HIST_TARGET = {};
COURSES.forEach((c, ci) => {
  HIST_TARGET[c.key] = HIST.map((h, ti) => r2(4 + ((ci * 7 + ti * 11 + Number(h.id)) % 17)));
});

// 学生分数：能力 + 班效应 + 教师尺度 + 噪声
function itemScore(stu, cl, key, itemIdx, terminal) {
  const c = COURSE_BY_KEY[key];
  const ce = CLASS_EFFECT[key][cl.acId] || 0;
  const sdExtra = CLASS_SD_EXTRA[key][cl.acId] || 1;
  const te = TEACHER_BY_ID[cl.teacherId].strict;
  const p = SCHEME[key];
  const item = p.leaves[itemIdx] || p.leaves[0];
  const base = terminal ? 76 : 78;
  const v = base + stu.ability * 9.5 * sdExtra + ce * 0.9 - te * 3.0 + gaussZ() * 4.2 * sdExtra;
  return r1(clamp(v, 25, 100));
}

// 学生×课程分项与加权
const ENROLL_SCORE = {};
ENROLL.forEach((e) => {
  const p = SCHEME[e.key];
  const scores = {};
  p.leaves.forEach((leaf, li) => {
    // 当前学期是第 9 周（期中）：EXAM 课期末未考 → 终结项缺失
    const isTerminal = !!leaf.terminal;
    if (isTerminal && e.course.type === 'EXAM') return;
    const n = leaf.expected_times || 1;
    if (n <= 1) {
      scores[leaf.id] = itemScore(e.student, e.cl, e.key, li, isTerminal);
    } else {
      let sum = 0;
      for (let t = 0; t < n; t++) sum += itemScore(e.student, e.cl, e.key, li, isTerminal);
      scores[leaf.id] = r1(sum / n);
    }
  });
  ENROLL_SCORE[e.id] = scores;
});

// ============================================================ 课次与考勤
const SESS = [];
let sessId = 40001;
const ATT = [];
let attId = 500001;
const ATT_COUNT = {}; // enrollmentId -> {absent, late, leave}
CLASSES.forEach((cl) => {
  const c = COURSE_BY_KEY[cl.key];
  const totalWeeks = c.weeks;
  const heldWeeks = Math.min(9, totalWeeks); // 当前第 9 周
  const teacherAcc = TEACHER_BY_ID[cl.teacherId].accountId;
  for (let w = 1; w <= totalWeeks; w++) {
    const day = addDays(TERM_START, (w - 1) * 7);
    const held = w <= heldWeeks;
    SESS.push({
      id: sessId, offering_id: c.offeringId, teaching_class_id: cl.id, term_id: CUR.id,
      week_no: w, session_no: w, session_date: dateStr(day), start_time: '08:00', end_time: '09:40',
      location: '教1-' + (100 + (cl.id % 20)), session_type: 'NORMAL', status: 'NORMAL',
      adjust_note: null, roll_call_status: held ? 'CALLED' : 'NOT_CALLED',
      roll_call_time: held ? dateStr(day) + ' 09:45:00' : null,
      roll_call_by: held ? teacherAcc : null, roll_call_count: held ? studentsOf(cl.acId).length : 0,
      is_void: 0, create_by: teacherAcc, create_time: '2026-08-30 10:00:00',
      update_by: teacherAcc, update_time: '2026-08-30 10:00:00'
    });
    sessId++;
    if (!held) continue;
    const classAbs = (cl.id * 7 + w * 3) % 5 === 0 ? 0.05 : 0;
    studentsOf(cl.acId).forEach((s) => {
      const isHigh = HIGH_ABSENT.has(s.id + '#' + cl.id);
      const pAbs = 0.03 + classAbs + (isHigh ? 0.62 : 0) + (s.ability < -1 ? 0.02 : 0);
      const r = rnd();
      let status = 'PRESENT';
      let leaveType = null;
      if (r < pAbs) status = 'ABSENT';
      else if (r < pAbs + 0.03) status = 'LATE';
      else if (r < pAbs + 0.045) { status = 'LEAVE'; leaveType = pick(['SICK', 'PERSONAL', 'OFFICIAL']); }
      ATT.push({
        id: attId, enrollment_id: ENROLL.find((x) => x.student.id === s.id && x.cl.id === cl.id).id,
        student_id: s.id, session_id: SESS[SESS.length - 1].id, term_id: CUR.id,
        status, leave_type: leaveType, phase: 'NORMAL', source: 'TEACHER', archive_id: null,
        recorded_by: teacherAcc, recorded_role: 'TEACHER', recorded_at: dateStr(day) + ' 09:45:00',
        updated_by: null, updated_role: null, update_note: null, batch_key: 'RC-' + SESS.length,
        is_void: 0, void_reason: null,
        create_time: dateStr(day) + ' 10:00:00', update_time: dateStr(day) + ' 10:00:00'
      });
      attId++;
    });
  }
});
ATT.forEach((a) => {
  a.enrollment_id = a.enrollment_id; // no-op，保留可读性
  const c = ATT_COUNT[a.enrollment_id] || (ATT_COUNT[a.enrollment_id] = { absent: 0, late: 0, leave: 0 });
  if (a.status === 'ABSENT') c.absent++;
  else if (a.status === 'LATE') c.late++;
  else if (a.status === 'LEAVE') c.leave++;
});

// ============================================================ 成绩单（当前学期）
const SCORE_ITEM_ROWS = [];
const RESULTS = [];
let scoreItemId = 600001;
let resultId = 700001;
CLASSES.forEach((cl) => {
  const c = COURSE_BY_KEY[cl.key];
  const tpl = TEMPLATES[c.schemeTemplate];
  const p = SCHEME[cl.key];
  const teacherAcc = TEACHER_BY_ID[cl.teacherId].accountId;
  const allowed = Math.floor(c.weeks / 3);
  studentsOf(cl.acId).forEach((s) => {
    const e = ENROLL.find((x) => x.student.id === s.id && x.cl.id === cl.id);
    const scores = ENROLL_SCORE[e.id];
    const ac = ATT_COUNT[e.id] || { absent: 0, late: 0, leave: 0 };
    // 分项行
    p.leaves.forEach((leaf) => {
      if (scores[leaf.id] === undefined) return;
      SCORE_ITEM_ROWS.push({
        id: scoreItemId++, enrollment_id: e.id, student_id: s.id, scheme_item_id: leaf.id,
        term_id: CUR.id, times_no: 1, raw_value: null, score: scores[leaf.id], full_score: 100,
        is_absent: 0, source: 'MANUAL', scorer_id: teacherAcc, scorer_role: 'TEACHER',
        scored_at: '2026-10-15 18:00:00', is_void: 0, void_reason: null, remark: null,
        create_by: teacherAcc, create_time: '2026-10-15 18:00:00',
        update_by: null, update_time: '2026-10-15 18:00:00'
      });
    });
    // 加权
    let usualPct = 0;
    tpl.items.forEach((it, i) => {
      const leaf = p.leaves[i];
      const v = scores[leaf.id] == null ? 0 : scores[leaf.id];
      usualPct += v * (it[3] / 100);
    });
    const finalLeaf = p.leaves[p.leaves.length - 1];
    const finalPct = scores[finalLeaf.id];
    const complete = finalPct !== undefined;
    const weighted = usualPct * tpl.usual / 100 + (complete ? finalPct : 0) * tpl.finalW / 100;
    const P = 2 * ac.absent + ac.late + Math.max(0, ac.leave - 3);
    const T = r2(weighted - P);
    const hard = ac.absent > allowed;
    RESULTS.push({
      id: resultId++, enrollmentId: e.id, studentId: s.id, offeringId: c.offeringId,
      tcId: cl.id, acId: cl.acId, key: cl.key, studentNo: s.no, studentName: s.name,
      W: r1(weighted), usual: r1(usualPct), P: r1(P), T, absent: ac.absent, late: ac.late, leave: ac.leave,
      allowed, weeks: c.weeks, hard: hard ? 1 : 0,
      fail: hard || (complete && T < 60) ? 1 : 0,
      reason: hard ? 'HARD_LINE' : (complete && T < 60 ? 'SCORE' : null),
      status: complete ? 'FINAL' : 'PARTIAL', complete
    });
  });
});

// ============================================================ 历史学期：挂科历史 + 课程挂科率
const HISTORY_ROWS = [];
let histId = 720001;
const HIST_STATS = {}; // key#termId -> {examinee, fail, hard}
HIST.forEach((term, ti) => {
  COURSES.forEach((c, ci) => {
    const target = HIST_TARGET[c.key][ti] / 100;
    const mean = 60 - 9.6 * normInv(target); // 目标挂科率对应的均值（历史分 sd≈9.6）
    const grades = [2025, 2024];
    grades.forEach((g) => {
      const acs = ACS.filter((a) => a.grade === g);
      const keys = PARAMS.classPlan.coursesByClass[String(g)];
      if (!keys.includes(c.key)) return;
      acs.forEach((ac) => {
        studentsOf(ac.id).forEach((s) => {
          const score = r2(clamp(mean + s.ability * 7.0 + gaussZ() * 6.5 + (ac.id - 4.5) * 0.6, 20, 100));
          const isFail = score < 60 ? 1 : 0;
          HISTORY_ROWS.push({
            id: histId++, student_id: s.id, term_id: term.id, course_id: c.id,
            // uk_student_offering 不含 term：用「开课ID×100+学期ID」做历史行的唯一键
            offering_id: c.offeringId * 100 + term.id,
            final_score: score, pass_score: 60, is_fail: isFail, hard_fail: 0, is_retake: 0,
            score_status: 'FINAL', engine_version: 'v3.seed',
            archived_at: term.endDate + ' 18:00:00',
            create_time: term.endDate + ' 18:00:00', update_time: term.endDate + ' 18:00:00'
          });
          const st = HIST_STATS[c.key + '#' + term.id] || (HIST_STATS[c.key + '#' + term.id] = { examinee: 0, fail: 0, hard: 0 });
          st.examinee++;
          st.fail += isFail;
        });
      });
    });
  });
});

// ============================================================ 教师尺度（当前学期）
const STYLE_PAIRS = [];
CLASSES.forEach((cl) => {
  const k = cl.teacherId + '#' + cl.offeringId;
  if (!STYLE_PAIRS.some((x) => x.k === k)) {
    STYLE_PAIRS.push({ k, teacherId: cl.teacherId, offeringId: cl.offeringId, key: cl.key, tcIds: [] });
  }
  STYLE_PAIRS.find((x) => x.k === k).tcIds.push(cl.id);
});
const STYLES = STYLE_PAIRS.map((p, i) => {
  const t = TEACHER_BY_ID[p.teacherId];
  const students = p.tcIds.reduce((n, id) => n + studentsOf(CLASS_BY_ID[id].acId).length, 0);
  const raw = r2(t.strict + rf(-0.12, 0.12));
  return {
    id: 6001 + i, teacher_id: p.teacherId, offering_id: p.offeringId,
    course_id: COURSE_BY_KEY[p.key].id, term_id: CUR.id, scheme_item_id: null, item_code: 'OVERALL',
    raw_delta: raw, delta: r2(raw * 0.62), shrink_lambda: 0.620, sample_size: students,
    std_dev: r1(rf(9.0, 12.0)), fused: 0, fuse_reason: null, review_by: null, review_time: null,
    status: 'ACTIVE', engine_version: 'v3.seed', calc_time: '2026-10-20 09:00:00',
    create_time: '2026-10-18 10:00:00', update_time: '2026-10-20 09:00:00'
  };
});

// ============================================================ 生成 SQL
out.push('-- ============================================================');
out.push('-- v3.0 独立库种子（由 v3.0/tool/gen_v3_seed.js 生成）');
out.push('-- seed=' + PARAMS.seed + '（确定性生成：同一 seed + params 输出逐字节一致）');
out.push('-- 只导入 study_warning_system_v3；v2.0 演示库 study_warning_system_v2 不受影响');
out.push('-- ============================================================');
out.push('SET FOREIGN_KEY_CHECKS=0;');

// 0) 清空克隆自 v2 的主线数据（保留 major / risk_engine_config / system_config / admin / counselor / 知识库）
out.push('-- 0) 清空 v2 克隆的主线数据');
[
  'submission_answer', 'submission', 'question', 'paper',
  'project_contribution', 'project_submission', 'project_team', 'project_task',
  'score_item', 'score_result', 'score_scheme_item', 'score_scheme',
  'risk_state', 'alert_flow_log', 'intervention_record', 'alert_record', 'class_alert_notification',
  'attendance_record', 'attendance_archive', 'attendance_record_log', 'course_session',
  'student_fail_history', 'course_fail_rate_history', 'teaching_class_fail_rate', 'admin_class_fail_rate',
  'teacher_style', 'questionnaire_answer', 'questionnaire_flow', 'questionnaire',
  'counselor_class', 'teaching_class_admin_class', 'teaching_class',
  'course_lesson', 'course_guide', 'teaching_objective',
  'course_offering', 'course_group', 'course_join_request', 'exam_headcount', 'coverage_check_log',
  'import_batch', 'audit_log', 'notification_receipt', 'notification',
  'ai_run_step', 'ai_run', 'ai_usage', 'ai_report', 'ai_report_feedback', 'ai_alert_explain',
  'ai_qa_log', 'ai_question_draft', 'ai_grading_suggest', 'ai_group_report'
].forEach((t) => del('DELETE FROM ' + t));
del('DELETE FROM enrollment');
del('DELETE FROM sys_account WHERE role = \'TEACHER\'');
del('DELETE FROM sys_account WHERE role = \'STUDENT\' AND id <> 631');
del('DELETE FROM student WHERE id <> 1');
del('DELETE FROM teacher');
del('DELETE FROM administrative_class');
del('DELETE FROM term');
del('DELETE FROM course');

// 1) 学期
out.push('-- 1) 学期：当前 + 3 个历史');
ins('term', TERMS);

// 2) 教师 + 账号
out.push('-- 2) 教师（' + TEACHERS.length + ' 人）与账号');
ins('teacher', TEACHERS.map((t) => ({
  id: t.id, account_id: t.accountId, teacher_no: t.no, teacher_name: t.name,
  gender: t.gender, title: pick(['讲师', '副教授', '教授']), college_name: '计算机学院',
  phone: '138' + pad(10000000 + t.id * 137, 8), email: 'teacher' + t.no + '@example.edu',
  status: 'ACTIVE', create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
})));
out.push("INSERT INTO sys_account (id, login_no, password_hash, role, real_name, status, create_time, update_time)\n" +
  "SELECT t.account_id, t.teacher_no, a.password_hash, 'TEACHER', t.teacher_name, 'ACTIVE', NOW(), NOW()\n" +
  "  FROM teacher t JOIN sys_account a ON a.login_no = '20250001';");

// 3) 行政班
out.push('-- 3) 行政班（' + ACS.length + ' 个）');
ins('administrative_class', ACS.map((ac) => ({
  id: ac.id, class_code: ac.code, class_name: ac.name, grade_year: ac.grade, major_id: 1,
  status: 'ACTIVE', create_time: '2024-08-25 10:00:00', update_time: '2026-08-25 10:00:00'
})));

// 4) 学生 + 账号
out.push('-- 4) 学生（' + STUDENTS.length + ' 人）与账号');
del('UPDATE student SET admin_class_id=1, student_name=\'学生1\', enroll_year=2025, gender=1, status=\'IN_SCHOOL\' WHERE id=1');
ins('student', STUDENTS.filter((s) => s.id !== 1).map((s) => ({
  id: s.id, account_id: s.accountId, student_no: s.no, student_name: s.name, gender: s.gender,
  enroll_year: s.year, major_id: 1, admin_class_id: s.acId, status: 'IN_SCHOOL',
  create_time: '2026-08-25 10:00:00', update_time: '2026-08-25 10:00:00'
})));
out.push("INSERT INTO sys_account (id, login_no, password_hash, role, real_name, status, create_time, update_time)\n" +
  "SELECT s.account_id, s.student_no, a.password_hash, 'STUDENT', s.student_name, 'ACTIVE', NOW(), NOW()\n" +
  "  FROM student s JOIN sys_account a ON a.login_no = '20250001' WHERE s.id <> 1;");

// 5) 课程
out.push('-- 5) 课程（' + COURSES.length + ' 门）');
ins('course', COURSES.map((c, i) => ({
  id: c.id, course_name: c.name, course_type: c.type, credit: c.credit,
  total_hours: c.weeks * 2, prerequisite_course_id: null, college_name: '计算机学院',
  difficulty_factor_code: c.type === 'EXAM' ? 'EXAM' : 'ASSESS', status: 'ACTIVE',
  create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
})));

// 6) 课程组 / 开课 / 教学班
out.push('-- 6) 课程组、开课、教学班、导员带班');
ins('course_group', COURSES.map((c) => ({
  id: c.offeringId, course_id: c.id, term_id: CUR.id, level: 'COLLEGE',
  level_request_status: 'APPROVED', status: 'ACTIVE',
  create_time: '2026-08-20 09:00:00', update_time: '2026-08-20 09:00:00'
})));
ins('course_offering', COURSES.map((c) => ({
  id: c.offeringId, course_id: c.id, term_id: CUR.id, group_id: c.offeringId,
  planned_weeks: c.weeks, planned_times_per_week: 1, planned_sessions: c.weeks,
  actual_weeks: c.weeks, actual_sessions: c.weeks, actual_submit_status: 'SUBMITTED',
  actual_submit_by: 1442, actual_submit_time: '2026-12-18 09:00:00', actual_confirm_by: 1442,
  actual_confirm_time: '2026-12-19 09:00:00', effective_sessions: c.weeks, effective_source: 'ACTUAL',
  pass_score: 60, status: 'ACTIVE', create_by: 1442,
  create_time: '2026-08-20 09:10:00', update_time: '2026-12-19 09:00:00'
})));
ins('teaching_class', CLASSES.map((cl) => ({
  id: cl.id, offering_id: cl.offeringId, teacher_id: cl.teacherId, class_code: cl.code,
  class_name: cl.name, capacity: 60, is_retake_class: cl.retake, invite_code: cl.invite,
  join_review_enabled: 0, status: 'ACTIVE', create_by: 1,
  create_time: '2026-08-21 09:00:00', update_time: '2026-08-21 09:00:00'
})));
ins('teaching_class_admin_class', CLASSES.map((cl) => ({ teaching_class_id: cl.id, admin_class_id: cl.acId })));
ins('counselor_class', ACS.map((ac) => ({
  admin_class_id: ac.id, term_id: CUR.id, counselor_id: ac.id <= 5 ? 1 : 2,
  create_time: '2026-08-25 10:00:00', update_time: '2026-08-25 10:00:00'
})));

// 7) 选课
out.push('-- 7) 选课（' + ENROLL.length + ' 条）');
ins('enrollment', ENROLL.map((e) => ({
  id: e.id, student_id: e.student.id, teaching_class_id: e.cl.id, offering_id: e.course.offeringId,
  term_id: CUR.id, status: 'ENROLLED', retake_times: e.cl.retake, enroll_time: '2026-08-25 14:00:00',
  withdraw_time: null, withdraw_reason: null, withdraw_approve_by: null, remark: null,
  create_by: 1, create_time: '2026-08-25 14:00:00', update_by: null, update_time: '2026-08-25 14:00:00'
})));

// 7.5) P2-0b Grader 最小数据：2 门课 × 5 道主观题 × 30 份提交
const GRADER_QUESTIONS = {
  B: [
    { stem: '求极限 lim(x→0) sin(3x)/x，并说明使用的等价无穷小。', ref: 'sin(3x)~3x，故极限为 3', rubric: ['等价无穷小', '极限结果'] },
    { stem: '说明导数与函数单调性的关系，并给出一个单调递增的判定条件。', ref: 'f\'(x)>0 则严格递增；f\'(x)<0 则递减', rubric: ['导数符号', '单调性结论'] },
    { stem: '计算定积分 ∫₀¹ (2x+1) dx，并写出牛顿-莱布尼茨公式。', ref: '原函数 x²+x，结果 2；∫ₐᵇ f=F(b)−F(a)', rubric: ['原函数', '公式', '结果'] },
    { stem: '判断级数 Σ 1/n² 的敛散性，并说明依据。', ref: 'p=2>1 的 p 级数收敛', rubric: ['p 级数', '收敛结论'] },
    { stem: '简述矩阵可逆的两种判定方法。', ref: '行列式非零；秩等于阶数', rubric: ['行列式', '秩'] }
  ],
  D: [
    { stem: '简述顺序表与链表的优缺点，并说明各自适用场景。', ref: '顺序表随机访问快、插入删除慢；链表相反', rubric: ['访问复杂度', '插入删除', '适用场景'] },
    { stem: '写出二叉树前序遍历的递归定义，并给出一个例子。', ref: '根→左→右；例子略', rubric: ['遍历顺序', '递归', '例子'] },
    { stem: '说明栈与队列的区别，并各举一个应用。', ref: '栈 LIFO（表达式求值）；队列 FIFO（BFS）', rubric: ['LIFO/FIFO', '应用'] },
    { stem: '简述哈希表冲突处理的两种方法。', ref: '开放定址法；链地址法', rubric: ['开放定址', '链地址'] },
    { stem: '分析快速排序平均时间复杂度及最坏情况。', ref: '平均 O(n log n)，最坏 O(n²)', rubric: ['平均', '最坏', '原因'] }
  ]
};
const graderPapers = [];
const graderQuestions = [];
const graderSubs = [];
const graderAnswers = [];
let gpid = 9001;
['B', 'D'].forEach((key, pi) => {
  const cl = CLASSES.find((c) => c.key === key && c.acId === 1 && c.retake === 0);
  const course = COURSE_BY_KEY[key];
  const pid = gpid++;
  graderPapers.push({
    id: pid, teaching_class_id: cl.id, offering_id: cl.offeringId, term_id: CUR.id,
    paper_type: 'HOMEWORK', paper_name: course.name + ' · 主观题联调卷（P2-0b）', lesson_id: null,
    start_time: '2026-10-01 08:00:00', end_time: '2026-10-15 23:59:59', duration_minutes: 60,
    full_score: 50, scheme_item_id: null, publisher_role: 'TEACHER', publisher_id: cl.teacherId,
    allow_view_paper: 1, show_score: 1, show_answer: 'AFTER', status: 'PUBLISHED',
    remark: 'P2 Grader 联调数据', create_by: cl.teacherId, create_time: '2026-09-28 09:00:00',
    update_by: cl.teacherId, update_time: '2026-10-01 08:00:00'
  });
  const qids = [];
  GRADER_QUESTIONS[key].forEach((q, qi) => {
    const qid = 90001 + pi * 10 + qi;
    qids.push(qid);
    graderQuestions.push({
      id: qid, paper_id: pid, q_no: qi + 1, q_type: 'SHORT', stem: q.stem, options_json: null,
      answer_json: JSON.stringify({ ref: q.ref, rubric: q.rubric }), analysis: q.ref,
      full_score: 10, group_name: '一、简答题', sort_no: qi + 1,
      create_time: '2026-09-28 09:00:00', update_time: '2026-09-28 09:00:00'
    });
  });
  ENROLL.filter((e) => e.cl.id === cl.id).slice(0, 30).forEach((e, si) => {
    const sid = 900000 + pi * 1000 + si + 1;
    let total = 0;
    GRADER_QUESTIONS[key].forEach((q, qi) => {
      const score = 5 + ((e.student.id * 13 + qi * 7 + pi * 3) % 6); // 5..10，人工分
      total += score;
      const quality = score >= 9 ? '完整' : score >= 7 ? '基本完整' : '要点不足';
      const text = `（${quality}作答）${q.ref.slice(0, 30)}${score >= 9 ? '，并补充了推导与例子。' : score >= 7 ? '。' : '，部分步骤缺失。'}`;
      graderAnswers.push({
        id: 910000 + pi * 1000 + si * 10 + qi, submission_id: sid, question_id: qids[qi],
        answer_json: JSON.stringify({ text }), score, is_correct: null,
        comment: quality === '完整' ? '要点齐全' : quality === '基本完整' ? '基本正确' : '要点不足',
        marked_by_type: 'TEACHER', marked_by: cl.teacherId, marked_at: '2026-10-16 10:00:00',
        create_time: '2026-10-15 20:00:00', update_time: '2026-10-16 10:00:00'
      });
    });
    graderSubs.push({
      id: sid, paper_id: pid, teaching_class_id: cl.id, term_id: CUR.id, student_id: e.student.id,
      enrollment_id: e.id, submit_status: 'SUBMITTED', start_time: '2026-10-15 19:00:00',
      submit_time: '2026-10-15 20:00:00', duration_seconds: 3600, objective_score: 0,
      subjective_score: total, total_score: total, marked_status: 'ALL_MARKED',
      marked_by: cl.teacherId, marked_at: '2026-10-16 10:00:00', is_absent: 0,
      remark: 'P2-0b 联调', create_time: '2026-10-15 20:00:00', update_time: '2026-10-16 10:00:00'
    });
  });
});
ins('paper', graderPapers);
ins('question', graderQuestions);
ins('submission', graderSubs);
ins('submission_answer', graderAnswers);
out.push('-- P2-0b Grader：' + graderPapers.length + ' 套卷 / ' + graderQuestions.length + ' 题 / ' +
  graderSubs.length + ' 份提交 / ' + graderAnswers.length + ' 条作答');

// 8) 成绩构成
out.push('-- 8) 成绩构成（每门课一套，全部手工录入口径）');
ins('score_scheme', COURSES.map((c) => ({
  id: c.schemeId, group_id: c.offeringId, term_id: CUR.id, version: 1, level: 'L2',
  source_template_id: null, total_weight: 100, confirm_status: 'LOCKED', confirm_total: 1,
  confirm_done: 1, initiator_id: null, confirmed_by_json: '[]', locked: 1, locked_by: null,
  locked_at: '2026-08-30 10:00:00', lock_type: 'ADMIN', remark: 'v3.0 生成：手工录入口径',
  create_by: 1, create_time: '2026-08-30 09:00:00', update_by: null, update_time: '2026-08-30 10:00:00'
})));
const schemeItemRows = [];
COURSES.forEach((c) => {
  const p = SCHEME[c.key];
  const all = [...p.roots, ...p.leaves];
  all.forEach((it) => {
    schemeItemRows.push({
      id: it.id, scheme_id: it.scheme_id, parent_id: it.parent_id, item_level: it.item_level,
      item_code: it.item_code, item_name: it.item_name, category: it.category, weight: it.weight,
      full_score: it.full_score, data_source: it.data_source, score_type: it.score_type,
      mapping_json: null, expected_times: it.expected_times, aggregate_mode: 'AVG',
      is_anchor: 0, anchor_layer: null, is_subjective: 0, scorer_role: 'TEACHER',
      sort_no: it.sort_no, status: 'ACTIVE', create_by: 1,
      create_time: '2026-08-30 09:00:00', update_by: null, update_time: '2026-08-30 09:00:00'
    });
  });
});
ins('score_scheme_item', schemeItemRows);

// 9) 课次 + 考勤
out.push('-- 9) 课次（' + SESS.length + '）与考勤（' + ATT.length + '）');
ins('course_session', SESS);
ins('attendance_record', ATT, 500);

// 10) 分项成绩 + 成绩单
out.push('-- 10) 分项成绩（' + SCORE_ITEM_ROWS.length + '）与成绩单（' + RESULTS.length + '）');
ins('score_item', SCORE_ITEM_ROWS, 500);
ins('score_result', RESULTS.map((r) => ({
  id: r.id, enrollment_id: r.enrollmentId, student_id: r.studentId, offering_id: r.offeringId,
  term_id: CUR.id, weighted_score: r.W, deduction_points: r.P, total_score: r.T, pass_score: 60,
  absent_count: r.absent, late_count: r.late, leave_count: r.leave, allowed_absent: r.allowed,
  effective_sessions: r.weeks, hard_fail: r.hard, is_fail: r.fail, fail_reason: r.reason,
  score_status: r.status, retake_times: 0, engine_version: 'v3.seed',
  calculated_at: '2026-10-15 18:00:00', is_void: 0, remark: null,
  create_time: '2026-10-15 18:00:00', update_time: '2026-10-15 18:00:00'
})));

// 11) 历史挂科
out.push('-- 11) 历史学期挂科（' + HISTORY_ROWS.length + ' 条，' + HIST.length + ' 个历史学期）');
ins('student_fail_history', HISTORY_ROWS, 500);

// 12) 课程挂科率历史（历史学期来自挂科历史；当前学期来自成绩单）
out.push('-- 12) 课程挂科率历史（3 历史 + 当前）');
const CFR = [];
let cfrId = 2001;
HIST.forEach((term) => {
  COURSES.forEach((c) => {
    const st = HIST_STATS[c.key + '#' + term.id] || { examinee: 0, fail: 0, hard: 0 };
    if (st.examinee === 0) return;
    CFR.push({
      id: cfrId++, course_id: c.id, term_id: term.id, examinee_count: st.examinee,
      fail_count: st.fail, fail_rate: r2(st.fail * 100 / st.examinee), hard_fail_count: 0,
      teaching_class_count: ACS.filter((a) => PARAMS.classPlan.coursesByClass[String(a.grade)].includes(c.key)).length,
      is_initialized: 0, course_factor: null, engine_version: 'v3.seed',
      calc_time: term.endDate + ' 18:00:00',
      create_time: term.endDate + ' 18:00:00', update_time: term.endDate + ' 18:00:00'
    });
  });
});
COURSES.forEach((c) => {
  const rows = RESULTS.filter((r) => r.key === c.key);
  const fail = rows.filter((r) => r.fail).length;
  CFR.push({
    id: cfrId++, course_id: c.id, term_id: CUR.id, examinee_count: rows.length, fail_count: fail,
    fail_rate: rows.length ? r2(fail * 100 / rows.length) : 0,
    hard_fail_count: rows.filter((r) => r.hard).length,
    teaching_class_count: CLASSES.filter((x) => x.key === c.key).length,
    is_initialized: 0, course_factor: null, engine_version: 'v3.seed',
    calc_time: '2026-10-20 09:00:00',
    create_time: '2026-10-20 09:00:00', update_time: '2026-10-20 09:00:00'
  });
});
ins('course_fail_rate_history', CFR);

// 13) 教师尺度
out.push('-- 13) 教师尺度 Δ（当前学期；λ 由 P0-3 脚本重算）');
ins('teacher_style', STYLES);

out.push('SET FOREIGN_KEY_CHECKS=1;');

// ============================================================ 写文件
fs.writeFileSync(OUT_SQL, out.join('\n'), 'utf8');

// 对照组（60:40）：确定性哈希分桶，P0-5 生成干预记录时按此执行
const cohort = {};
STUDENTS.forEach((s) => {
  const h = (s.id * 2654435761 + PARAMS.seed) >>> 0;
  cohort[s.id] = (h % 100) < Math.round((1 - PARAMS.quality.controlRatio) * 100) ? 'TREAT' : 'CONTROL';
});
fs.writeFileSync(OUT_COHORT, JSON.stringify({
  seed: PARAMS.seed,
  controlRatio: PARAMS.quality.controlRatio,
  interventionCoverage: PARAMS.quality.interventionCoverage,
  cohort
}, null, 2), 'utf8');

// ============================================================ 质量自检 + 数据故事（P0-6）
const meanOf = (xs) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : 0);
const sdOf = (xs) => {
  if (xs.length < 2) return 0;
  const m = meanOf(xs);
  return Math.sqrt(xs.reduce((a, b) => a + (b - m) * (b - m), 0) / xs.length);
};
// 口径：期末未录（PARTIAL）用过程百分制 usual，已结课（FINAL）用总评 T
const metricOf = (r) => (r.complete ? r.T : r.usual);

const courseStats = COURSES.map((c) => {
  const rows = RESULTS.filter((r) => r.key === c.key);
  const classIds = [...new Set(rows.map((r) => r.tcId))];
  const classAvg = classIds.map((id) => meanOf(rows.filter((r) => r.tcId === id).map(metricOf)));
  const within = classIds.map((id) => sdOf(rows.filter((r) => r.tcId === id).map(metricOf)));
  const histRates = HIST.map((h) => {
    const st = HIST_STATS[c.key + '#' + h.id] || { examinee: 0, fail: 0 };
    return st.examinee ? r2(st.fail * 100 / st.examinee) : null;
  });
  return {
    key: c.key, name: c.name, classes: classIds.length, students: rows.length,
    spread: r1(Math.max(...classAvg) - Math.min(...classAvg)),
    sd: r1(meanOf(within)),
    failCurrent: r2(rows.filter((r) => r.fail).length * 100 / rows.length),
    failHistory: histRates
  };
});

const classDetails = CLASSES.map((cl) => {
  const rows = RESULTS.filter((r) => r.tcId === cl.id);
  const teacher = TEACHER_BY_ID[cl.teacherId];
  return {
    code: cl.code, courseKey: cl.key, courseName: COURSE_BY_KEY[cl.key].name,
    teacherName: teacher.name, strict: teacher.strict,
    effect: CLASS_EFFECT[cl.key][cl.acId], n: rows.length,
    avg: r1(meanOf(rows.map(metricOf))), sd: r1(sdOf(rows.map(metricOf))),
    absentOver: rows.filter((r) => r.absent > r.allowed).length,
    hardFail: rows.filter((r) => r.hard === 1).length,
    retake: cl.retake === 1
  };
});
function causeOf(d) {
  const parts = [];
  if (d.effect <= -2) parts.push('班效应偏低（' + d.effect + '），整体基础较弱');
  else if (d.effect >= 2) parts.push('班效应偏高（+' + d.effect + '），基础较好');
  if (d.strict <= -1.0) parts.push('教师尺度偏严（Δ=' + d.strict + '）');
  else if (d.strict >= 0.3) parts.push('教师尺度偏松（Δ=+' + d.strict + '）');
  if (d.absentOver >= 6) parts.push('缺勤超限 ' + d.absentOver + ' 人，缺课硬线拉高风险');
  if (d.sd >= 11.5) parts.push('班内差异大（sd=' + d.sd + '）');
  if (d.retake) parts.push('重修班，基础分层明显');
  if (!parts.length) parts.push('无明显异常，接近课程平均');
  return parts.join('；');
}

const treatN = Object.values(cohort).filter((v) => v === 'TREAT').length;
const controlN = Object.values(cohort).filter((v) => v === 'CONTROL').length;
const story = [
  '# v3.0 数据故事（参数、成因与自检）', '',
  '> 由 `v3.0/tool/gen_v3_seed.js` 生成；seed=`' + PARAMS.seed + '`；库=`' + PARAMS.db + '`。',
  '> v2.0 演示仍用 `study_warning_system_v2`（原数据）；v3.0 用 `run_v3_backend.ps1` 连本库。', '',
  '## 一、规模与口径', '',
  '- 教师 ' + TEACHERS.length + ' / 课程 ' + COURSES.length + ' / 行政班 ' + ACS.length +
  ' / 学生 ' + STUDENTS.length + ' / 教学班 ' + CLASSES.length + ' / 选课 ' + ENROLL.length +
  ' / 考勤 ' + ATT.length + ' / 分项 ' + SCORE_ITEM_ROWS.length + ' / 成绩单 ' + RESULTS.length,
  '- 学期：当前 ' + CUR.code + ' + 历史 ' + HIST.map((h) => h.code).join(' / '),
  '- 评测口径：期末未录的课用**过程百分制**（PARTIAL），已结课的课用**总评 T**（FINAL）；' +
  '班间差=同课程各班均值极差；班内 sd=各班标准差均值。',
  '- 对照组：干预 ' + treatN + ' 人 / 延迟干预 ' + controlN + ' 人（TREAT:CONTROL=60:40，见 `v3_cohort.json`）。', '',
  '## 二、课程总表（当前学期）', '',
  '| 课程 | 类型 | 周次 | 教学班 | 学生 | 班间差 | 班内 sd | 当前挂科率% | 历史挂科率%（近→远） |',
  '|---|---|---|---|---|---|---|---|---|'];
COURSES.forEach((c, i) => {
  const s = courseStats.find((x) => x.key === c.key);
  story.push('| ' + c.key + ' ' + c.name + ' | ' + (c.type === 'EXAM' ? '考试' : '考查') + ' | ' + c.weeks +
    ' | ' + s.classes + ' | ' + s.students + ' | ' + s.spread + ' | ' + s.sd + ' | ' + s.failCurrent +
    ' | ' + s.failHistory.slice().reverse().join(' / ') + ' |');
});
story.push('', '## 三、分班成因说明（答辩可直接引用）', '');
COURSES.forEach((c) => {
  story.push('### ' + c.key + ' ' + c.name + '（' + (c.type === 'EXAM' ? '考试课' : '考查课') + '，' + c.weeks + ' 周）', '');
  story.push('| 教学班 | 教师 | 学生 | 过程/总评分均值 | 班内 sd | 缺勤超限 | 当前挂科 | 成因 |');
  story.push('|---|---|---|---|---|---|---|---|');
  classDetails.filter((d) => d.courseKey === c.key).forEach((d) => {
    story.push('| ' + d.code + ' | ' + d.teacherName + ' | ' + d.n + ' | ' + d.avg + ' | ' + d.sd +
      ' | ' + d.absentOver + ' | ' + d.hardFail + ' | ' + causeOf(d) + ' |');
  });
  story.push('');
});
story.push('## 四、数据口径与复现', '',
  '- 当前学期为第 9 周：EXAM 课期末未考（PARTIAL），ASSESS 课大作业已批（FINAL）；',
  '- 缺课硬线、需考不可达等 A 通道信号由考勤与成绩自然产生；',
  '- 课程因子（course_fail_rate_history.course_factor）由 P0-2 脚本按 clamp(1+k*(fail_rate-baseline),0.90,1.15) 初始化；',
  '- 教师尺度 λ=n/(n+k) 与熔断由 P0-3 脚本重算；',
  '- 复现：`node v3.0/tool/gen_v3_seed.js` → `bootstrap_v3.ps1 -Force` → `run_v3_pipeline.ps1`；',
  '- 试卷 / 项目 / 问卷等演示模块暂不生成（P2 前补齐）。', '');
fs.writeFileSync(OUT_STORY, story.join('\n'), 'utf8');

// ============================================================ 控制台输出
console.log('生成 v3_seed.sql：seed=' + PARAMS.seed);
console.log('  学期=' + TERMS.length + ' 教师=' + TEACHERS.length + ' 课程=' + COURSES.length +
  ' 行政班=' + ACS.length + ' 学生=' + STUDENTS.length + ' 教学班=' + CLASSES.length +
  ' 选课=' + ENROLL.length);
console.log('  课次=' + SESS.length + ' 考勤=' + ATT.length + ' 分项=' + SCORE_ITEM_ROWS.length +
  ' 成绩单=' + RESULTS.length + ' 历史挂科=' + HISTORY_ROWS.length + ' 挂科率行=' + CFR.length +
  ' 教师尺度=' + STYLES.length);
console.log('  输出：' + OUT_SQL);
classStatsLine(courseStats);
function classStatsLine(stats) {
  let ok = true;
  stats.forEach((s) => {
    const spreadOk = s.spread >= PARAMS.quality.classAvgSpreadMin && s.spread <= PARAMS.quality.classAvgSpreadMax;
    const sdOk = s.sd >= PARAMS.quality.classSdMin && s.sd <= PARAMS.quality.classSdMax;
    if (!spreadOk || !sdOk) ok = false;
    console.log('  ' + s.key + ' ' + s.name.slice(0, 8) + '：班间差=' + s.spread + (spreadOk ? '✓' : '✗') +
      ' 班内sd=' + s.sd + (sdOk ? '✓' : '✗') + ' 当前挂科率=' + s.failCurrent + '% 历史=' + s.failHistory.join('/'));
  });
  console.log(ok ? '  统计口径自检：通过' : '  统计口径自检：有超区间项，需调参');
}
