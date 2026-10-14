/* build_docx.js — assemble the v2 requirements & design document
 * Reads: doc_content.json (narrative blocks), cases.json (use case inventory), out/*.png (diagrams)
 * Usage: node build_docx.js [outPath]
 */
'use strict';
const fs = require('fs');
const path = require('path');
const dx = require('docx');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, BorderStyle, ImageRun, TableLayoutType, VerticalAlign, PageBreak, Footer, PageNumber
} = dx;

const gen = __dirname;
const content = JSON.parse(fs.readFileSync(path.join(gen, 'doc_content.json'), 'utf8'));
const cases = JSON.parse(fs.readFileSync(path.join(gen, 'cases.json'), 'utf8'));
const outPath = process.argv[2] || path.join(gen, '..', '..', '学情预警系统v2_需求与设计文档.docx');

const CJK = { eastAsia: '宋体', ascii: 'Times New Roman', hAnsi: 'Times New Roman' };
const CJK_B = { eastAsia: '黑体', ascii: 'Times New Roman', hAnsi: 'Times New Roman' };
const BODY = 21;      // 10.5pt
const SMALL = 19;     // 9.5pt
const CONTENT_W = 9070; // twips = 16cm

const BORDER = { style: BorderStyle.SINGLE, size: 4, color: '808080' };
const BORDERS = {
  top: BORDER, bottom: BORDER, left: BORDER, right: BORDER,
  insideHorizontal: BORDER, insideVertical: BORDER
};

function run(text, o = {}) {
  return new TextRun({ text: String(text), font: o.font || CJK, size: o.size || BODY, bold: !!o.bold, italics: !!o.italics, color: o.color });
}
function para(text, o = {}) {
  let kids;
  if (Array.isArray(text)) kids = text;
  else if (text instanceof TextRun) kids = [text];
  else kids = [run(text, o)];
  return new Paragraph({
    children: kids,
    alignment: o.align,
    spacing: o.spacing || { line: 340, before: o.before === undefined ? 0 : o.before, after: o.after === undefined ? 80 : o.after },
    indent: o.indent,
    heading: o.heading
  });
}
function cell(children, w, o = {}) {
  const arr = (Array.isArray(children) ? children : [children]).map(c =>
    (c instanceof Paragraph || c instanceof Table) ? c : para(c, { size: o.size || SMALL, align: o.align, spacing: { line: 280, after: 0 } }));
  return new TableCell({
    children: arr,
    width: { size: w, type: WidthType.DXA },
    verticalAlign: VerticalAlign.CENTER,
    margins: { top: 60, bottom: 60, left: 90, right: 90 },
    shading: o.shading ? { fill: o.shading } : undefined
  });
}
function makeTable(head, rows, widths, opt = {}) {
  const trs = [];
  if (head) {
    trs.push(new TableRow({
      tableHeader: true,
      children: head.map((h, i) => cell(run(h, { bold: true, size: SMALL }), widths[i], { align: AlignmentType.CENTER, shading: 'EDF2F7' }))
    }));
  }
  rows.forEach(r => {
    trs.push(new TableRow({
      children: r.map((c, i) => cell(c, widths[i], { align: (opt.centerCols && opt.centerCols.indexOf(i) >= 0) ? AlignmentType.CENTER : undefined }))
    }));
  });
  return new Table({
    rows: trs,
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: widths,
    layout: TableLayoutType.FIXED,
    borders: BORDERS
  });
}
function caption(text) {
  return para(run(text, { bold: true, size: SMALL }), { align: AlignmentType.CENTER, before: 60, after: 60, spacing: { line: 300, before: 120, after: 60 } });
}
function image(file, cap) {
  const full = path.join(gen, file);
  const png = fs.readFileSync(full);
  // logical size comes from the spec next to the png
  const specFile = path.join(gen, 'spec', path.basename(file, '.png') + '.json');
  let w = 1000, h = 600;
  if (fs.existsSync(specFile)) {
    const s = JSON.parse(fs.readFileSync(specFile, 'utf8'));
    w = s.w; h = s.h;
  }
  const maxW = 600, maxH = 640;
  let dw = maxW, dh = maxW * h / w;
  if (dh > maxH) { dh = maxH; dw = maxH * w / h; }
  const out = [new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 40, line: 260 },
    children: [new ImageRun({ type: 'png', data: png, transformation: { width: Math.round(dw), height: Math.round(dh) } })]
  })];
  if (cap) out.push(caption(cap));
  return out;
}

// ---------- generated tables ----------
const detailIds = cases.cases.filter(c => c.d).map(c => c.id);
const star = id => detailIds.indexOf(id) >= 0 ? id + ' ★' : id;

function summaryTable() {
  const rows = cases.cases.map(c => [star(c.id), c.name, c.actors.join('、'), c.brief]);
  return [caption('表2.1 用例摘要描述'),
    makeTable(['用例标识', '用例名称', '参与者', '摘要描述'], rows, [1150, 1900, 1550, 4470])];
}

const GROUPS = [
  { key: 'gen', title: '2.1.3.1 通用用例', head: '通用' },
  { key: 'T', title: '2.1.3.2 教师端核心用例', head: '教师端' },
  { key: 'S', title: '2.1.3.3 学生端核心用例', head: '学生端' },
  { key: 'A', title: '2.1.3.4 教务端核心用例', head: '教务端' },
  { key: 'C', title: '2.1.3.5 辅导员端核心用例', head: '辅导员端' },
  { key: 'Y', title: '2.1.3.6 系统服务核心用例', head: '系统服务' }
];

// 表号：表2.1 为用例摘要表，其后按出现顺序自动编号
let tblNo = 1;
function nextTbl() { tblNo += 1; return tblNo; }
function capWithNum(cap) { return cap.indexOf('{n}') >= 0 ? cap.replace('{n}', String(nextTbl())) : cap; }
// 正文中的“如表2.{n}所示”指向紧随其后的那张表
function txt(s) { return String(s).indexOf('{n}') >= 0 ? String(s).replace(/\{n\}/g, String(tblNo + 1)) : s; }

// 数据表清单（按层，48 张）
const TABLE_LIST = [
  ['基础层', 'term', '学期表（全库时间轴）'],
  ['基础层', 'term_calendar', '教学周历表（周次 / 假期 / 考试周）'],
  ['基础层', 'sys_account', '统一账号表（四端登录）'],
  ['基础层', 'major', '专业 / 学院表'],
  ['基础层', 'administrative_class', '行政班表'],
  ['基础层', 'student', '学生表'],
  ['基础层', 'teacher', '教师表'],
  ['基础层', 'counselor', '辅导员表'],
  ['基础层', 'admin', '教务管理员表'],
  ['基础层', 'counselor_class', '辅导员—行政班关联表（带学期）'],
  ['基础层', 'course', '课程（科目）表：类型 / 学分 / 难度档位'],
  ['基础层', 'course_knowledge_point', '课程知识点表'],
  ['组织与开课', 'course_group', '课程组表（课程 × 学期，无组长）'],
  ['组织与开课', 'course_offering', '开课实例表（计划 / 实际 / 有效课次、及格线）'],
  ['组织与开课', 'teaching_class', '教学班表（一位任课教师）'],
  ['组织与开课', 'teaching_class_admin_class', '教学班—行政班关联表'],
  ['组织与开课', 'score_template', '成绩构成模板表'],
  ['组织与开课', 'score_scheme', '成绩构成表（版本 / 全体确认 / 锁定）'],
  ['组织与开课', 'score_scheme_item', '评分项定义表（两级，禁止考勤项）'],
  ['选课与课次', 'enrollment', '选课表（学生 × 教学班 × 学期）'],
  ['选课与课次', 'course_session', '课次表（一次课一行）'],
  ['考勤', 'attendance_record', '考勤明细表（双轨：平时 / 归档）'],
  ['考勤', 'attendance_archive', '考勤归档批次表（期中 / 期末）'],
  ['考勤', 'attendance_record_log', '考勤改动留痕表（行级前后值）'],
  ['成绩', 'score_item', '分项得分明细表（原始分）'],
  ['成绩', 'homework_item', '作业提交事实表（行为信号）'],
  ['成绩', 'score_result', '最终成绩单（判定结果留档）'],
  ['引擎产物', 'risk_engine_config', '风险引擎配置表（版本化参数）'],
  ['引擎产物', 'risk_state', '当前风险状态表（一人一课一行）'],
  ['引擎产物', 'teacher_style', '教师评分尺度表（分项级 Δ）'],
  ['引擎产物', 'student_fail_history', '学生跨学期成绩 / 挂科历史表'],
  ['引擎产物', 'teaching_class_fail_rate', '教学班挂科率表（反馈给教师）'],
  ['引擎产物', 'admin_class_fail_rate', '行政班挂科率表（反馈给辅导员）'],
  ['引擎产物', 'course_fail_rate_history', '课程挂科率历史表（课程侧因子）'],
  ['预警与干预', 'alert_record', '预警记录表（等级 / 通道 / 依据 / 状态）'],
  ['预警与干预', 'alert_flow_log', '预警生命周期时间线'],
  ['预警与干预', 'intervention_record', '干预记录表（教师 / 辅导员 / 教务三级）'],
  ['预警与干预', 'notification', '通知主体表（聚合去重）'],
  ['预警与干预', 'notification_receipt', '通知投递与回执表'],
  ['预警与干预', 'class_alert_notification', '班集体提醒去重表'],
  ['教务流程与审计', 'coverage_check_log', '覆盖率体检记录表'],
  ['教务流程与审计', 'exam_headcount', '实际考核人数确认与成绩核对表'],
  ['教务流程与审计', 'import_batch', '导入批次表（可查可回滚）'],
  ['教务流程与审计', 'audit_log', '统一审计日志表'],
  ['教务流程与审计', 'system_config', '运营配置表'],
  ['教务流程与审计', 'questionnaire', '问卷表（教师评价 / 知识点反馈）'],
  ['教务流程与审计', 'questionnaire_answer', '问卷作答明细表（匿名去重）'],
  ['教务流程与审计', 'questionnaire_flow', '问卷流转状态表（四环节防断链）']
];
function tableList() {
  return [caption('表2.' + nextTbl() + ' 数据表清单（按层，共 48 张）'),
    makeTable(['所属层', '表名', '说明'], TABLE_LIST, [1500, 2700, 4870])];
}
function groupOf(id) {
  if (id === 'UC-01' || id === 'UC-02' || id === 'UC-03') return 'gen';
  const seg = id.split('-')[1] || '';
  if (/^[0-9]+$/.test(seg)) return 'gen';
  return seg.charAt(0);
}
function detailTables() {
  const out = [];
  GROUPS.forEach(g => {
    const items = cases.cases.filter(c => c.d && groupOf(c.id) === g.key);
    if (!items.length) return;
    out.push(para(run(g.title, { bold: true, size: 24, font: CJK_B }), { before: 160, after: 80, spacing: { line: 320, before: 200, after: 80 } }));
    items.forEach(c => {
      const d = c.d;
      const rows = [
        ['用例标识', c.id],
        ['用例名称', c.name],
        ['参与者', c.actors.join('、')],
        ['前置条件', d.pre],
        ['后置条件', d.post],
        ['用例概述', d.ov],
        ['基本事件流', d.flow.map((s, i) => (i + 1) + '. ' + s)],
        ['备选事件流', d.alt && d.alt.length ? d.alt : ['无']],
        ['备注', d.note || '无']
      ];
      out.push(caption('表2.' + nextTbl() + ' ' + c.name + '用例'));
      out.push(makeTable(null, rows, [1500, 7570]));
      out.push(para('', { spacing: { line: 200, after: 60 } }));
    });
  });
  return out;
}

// ---------- document assembly ----------
const children = [];
const m = content.meta;
children.push(para('', { spacing: { line: 400, after: 0 } }));
children.push(para('', { spacing: { line: 400, after: 0 } }));
children.push(para(run(m.title, { bold: true, size: 52, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { line: 600, after: 120 } }));
children.push(para(run(m.subtitle, { bold: true, size: 40, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { line: 500, after: 400 } }));
children.push(para(run('班级：' + m.school_class, { size: 28 }), { align: AlignmentType.CENTER, spacing: { line: 400, after: 60 } }));
children.push(para(run('学号：' + m.student_no, { size: 28 }), { align: AlignmentType.CENTER, spacing: { line: 400, after: 60 } }));
children.push(para(run('姓名：' + m.author, { size: 28 }), { align: AlignmentType.CENTER, spacing: { line: 400, after: 60 } }));
children.push(para(run(m.date, { size: 28 }), { align: AlignmentType.CENTER, spacing: { line: 400, after: 0 } }));
children.push(new Paragraph({ children: [new PageBreak()] }));

let stats = { p: 0, tbl: 0, img: 0 };
for (const b of content.blocks) {
  if (b.t === 'h1') { children.push(new Paragraph({ children: [new PageBreak()] })); children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [run(b.x, { bold: true, size: 32, font: CJK_B, color: '000000' })], spacing: { before: 60, after: 180, line: 420 } })); }
  else if (b.t === 'h2') { children.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [run(b.x, { bold: true, size: 28, font: CJK_B, color: '000000' })], spacing: { before: 220, after: 120, line: 380 } })); }
  else if (b.t === 'h3') { children.push(new Paragraph({ heading: HeadingLevel.HEADING_3, children: [run(b.x, { bold: true, size: 24, font: CJK_B, color: '000000' })], spacing: { before: 180, after: 100, line: 360 } })); }
  else if (b.t === 'p') { children.push(para(txt(b.x), { indent: { firstLine: 420 } })); stats.p++; }
  else if (b.t === 'note') { children.push(para(run(txt(b.x), { size: SMALL, color: '555555' }), { indent: { left: 420 } })); stats.p++; }
  else if (b.t === 'ul') { b.x.forEach(s => { children.push(para(run('● ' + txt(s)), { indent: { left: 420, hanging: 200 }, spacing: { line: 340, after: 60 } })); stats.p++; }); }
  else if (b.t === 'ol') { b.x.forEach((s, i) => { children.push(para(run((i + 1) + '．' + txt(s)), { indent: { left: 420, hanging: 240 }, spacing: { line: 340, after: 60 } })); stats.p++; }); }
  else if (b.t === 'table') { children.push(caption(capWithNum(b.cap))); children.push(makeTable(b.head, b.rows, b.widths.map(w => Math.round(CONTENT_W * w / 100)))); stats.tbl++; }
  else if (b.t === 'gen_table_list') { tableList().forEach(x => children.push(x)); stats.tbl++; }
  else if (b.t === 'img') { image(b.file, b.cap).forEach(x => children.push(x)); stats.img++; }
  else if (b.t === 'gen_summary_table') { summaryTable().forEach(x => children.push(x)); stats.tbl++; }
  else if (b.t === 'gen_detail_tables') { detailTables().forEach(x => { children.push(x); if (x instanceof Table) stats.tbl++; }); }
  else throw new Error('unknown block type ' + b.t);
}

const doc = new Document({
  styles: {
    default: {
      document: {
        run: { font: CJK, size: BODY },
        paragraph: { spacing: { line: 340, after: 80 } }
      }
    }
  },
  sections: [{
    properties: {
      page: { size: { width: 11906, height: 16838 }, margin: { top: 1418, right: 1418, bottom: 1418, left: 1418 } }
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ children: [PageNumber.CURRENT], font: CJK, size: 18 })] })]
      })
    },
    children: children
  }]
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync(outPath, buf);
  console.log('WROTE ' + outPath + '  bytes=' + buf.length);
  console.log('blocks: paragraphs=' + stats.p + ' tables=' + stats.tbl + ' images=' + stats.img);
  console.log('cases=' + cases.cases.length + ' detail=' + detailIds.length);
}).catch(e => { console.error('FAILED', e); process.exit(1); });
