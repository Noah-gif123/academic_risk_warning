/* build_projdoc5.js — 生成《项目文档》第 1~5 章
 * 第 1~2 章: proj_doc_strict.json；第 3 章: proj_doc_ch3.json；第 4 章: proj_doc_ch4.json；第 5 章: proj_doc_ch5.json
 * 每章末尾按模板补【本章自评（学习自检）】表（plain_table，不计入表号）
 * Usage: node build_projdoc5.js [outPath]
 */
'use strict';
const fs = require('fs');
const path = require('path');
const dx = require('docx');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, BorderStyle, ImageRun, TableLayoutType, VerticalAlign, PageBreak, Footer, PageNumber,
  TableOfContents
} = dx;

const gen = __dirname;
const c12 = JSON.parse(fs.readFileSync(path.join(gen, 'proj_doc_strict.json'), 'utf8'));
const c3 = JSON.parse(fs.readFileSync(path.join(gen, 'proj_doc_ch3.json'), 'utf8'));
const c4 = JSON.parse(fs.readFileSync(path.join(gen, 'proj_doc_ch4.json'), 'utf8'));
const c5 = JSON.parse(fs.readFileSync(path.join(gen, 'proj_doc_ch5.json'), 'utf8'));
const meta = c12.meta;
let blocks = c12.blocks.concat(c3.blocks, c4.blocks, c5.blocks);

// 修订记录同步为“第 1~5 章”
blocks.forEach(b => {
  if (b.t === 'plain_table' && b.cap === '文档修订记录') {
    b.rows[0][2] = '初稿：完成第 1 章 项目概述、第 2 章 业务分析、第 3 章 子系统设计、第 4 章 数据设计、第 5 章 技术选型与开发环境';
  }
});

// ---------- 本章自评（学习自检）：按模板逐章补 ----------
const SELF = {
  1: [
    ['能清晰说明项目背景、目标用户与项目边界', '✓', '1.1 给出痛点、目标用户与边界（不含自动排课与学费管理）'],
    ['功能需求与非功能需求已分别列出且可验证', '✓', '1.3 用用户故事 + 非功能指标（响应时间、口令加密、并发量），第 6 章按此设计测试'],
    ['项目目标符合 SMART 原则并包含 AI 辅助目标', '△', '业务与技术目标可度量；AI 辅助目标目前只有粗略表述，待第 8 章量化'],
    ['术语表已覆盖全文关键缩写', '✓', '表1-1 覆盖 ER、RBAC、PBKDF2、RAG、双轨制等全文高频词']
  ],
  2: [
    ['用例图覆盖主要参与者与核心用例', '✓', '图2-1、图2-2 覆盖学生/教师/导员/教务四端；教务端用例复用总体图，未单独出图'],
    ['总体架构图清晰且选型有理由', '✓', '图2-3 分层结构 + 图2-4 功能边界；选型理由在第 5 章展开'],
    ['业务模块职责边界明确、依赖合理', '✓', '表2-1 按“数据来源→判定→处置”三条链划分，模块间为单向依赖'],
    ['核心业务流程含异常分支', '✓', '图2-5、图2-6 含停课、未点名、超时升级等分支'],
    ['已识别 AI 依赖等新型风险并给出缓解措施', '✓', '表2-2 含“AI 生成代码无人能讲清”“口径被框架黑盒化”两类风险及缓解措施']
  ],
  3: [
    ['各子系统职责与接口契约完整清晰', '✓', '表3-1、表3-3、表3-5 给出接口、入参、出参与 401/403/400/409 错误码'],
    ['核心类职责单一、关系合理（无上帝类）', '△', '图3-3 核心领域类职责清晰；TeacherPortalService 仍承担多个页面的数据组装，计划下一阶段拆分'],
    ['关键流程含异常处理与事务边界', '✓', '图3-1、图3-2、图3-4；考勤录入在同一事务内完成校验、写记录与写留痕'],
    ['AI 生成的接口/类已人工核对安全性', '✓', '全部接口逐个核对鉴权与数据范围，越权（跨教学班、跨行政班）已实测验证']
  ],
  4: [
    ['ER 图实体/联系/基数正确', '✓', '图4-1 覆盖学情预警子系统 13 张表，1:N 关系与外部锚点均标注，与 4.4 的十三张表结构一一对应'],
    ['建表语句字段、类型、约束经人工核对', '✓', '字段以线上库 information_schema 导出为准逐表核对；核对中修正了两处注释口径（17_dict_align.sql）'],
    ['敏感字段已设计脱敏/加密方案', '✓', '本子系统不存姓名与联系方式，只存 student_id 与聚合值；证据快照不含个人信息，配置变更留审计'],
    ['已考虑查询性能（索引/缓存）', '✓', '表4-17 的 14 组索引按页面实际查询建齐；风险状态本身即判定快照，充当缓存、不重复计算']
  ],
  5: [
    ['技术选型有对比且有明确理由', '✓', '表5-2 对六个关键决策点给出候选、结论与理由，并说明两处反常规选择的取舍'],
    ['Git 分支策略与 CI/CD 流程清晰', '△', '已用 Git + GitHub（main 单分支、72 次提交）；未配置 CI/CD，构建与验证靠本地脚本与人工回归清单'],
    ['已调研同类方案并说明差异', '✓', '表5-4 按三类方案对比数据来源、判定时机、口径透明度与处置闭环，并说明未做的两件事'],
    ['引入的开源/AI 服务已关注许可证与合规', '✓', '表5-5 标注许可；百炼输入脱敏、输出不入库、超时降级，仅第三阶段使用']
  ]
};
const SELF_HEAD = ['自检项（对照本章要求逐项勾选）', '自评 ✓/△/✗', '说明（不足处简要记录）'];
const SELF_W = [50, 10, 40];

// 定位每章的结束位置（下一个 h1 之前），倒序插入，避免下标漂移
// 第 1~3 章的正文源里已自带【本章自评】表（形式为 p 标题 + 无 caption 的 plain_table），
// 检测到就跳过，只给缺的章节补，且补齐时保持同样的块形式
const h1Idx = [];
blocks.forEach((b, i) => { if (b.t === 'h1') h1Idx.push(i); });
for (let k = h1Idx.length - 1; k >= 0; k--) {
  const start = h1Idx[k];
  const end = (k + 1 < h1Idx.length) ? h1Idx[k + 1] : blocks.length;
  const num = Number((String(blocks[start].x).match(/(\d+)/) || [])[1]);
  if (!SELF[num]) continue;
  const has = blocks.slice(start, end).some(b =>
    String(b.x || '').indexOf('本章自评') >= 0 || String(b.cap || '').indexOf('本章自评') >= 0);
  if (has) continue;
  blocks.splice(end, 0,
    { t: 'p', x: '**【本章自评（学习自检）】**' },
    { t: 'plain_table', head: SELF_HEAD, widths: SELF_W, rows: SELF[num] });
}

const outPath = process.argv[2] || path.join(gen, 'out_projdoc5.docx');

const CJK = { eastAsia: '宋体', ascii: 'Times New Roman', hAnsi: 'Times New Roman' };
const CJK_B = { eastAsia: '黑体', ascii: 'Times New Roman', hAnsi: 'Times New Roman' };
const BODY = 21, SMALL = 19, CONTENT_W = 9070;
const BORDER = { style: BorderStyle.SINGLE, size: 4, color: '808080' };
const BORDERS = { top: BORDER, bottom: BORDER, left: BORDER, right: BORDER, insideHorizontal: BORDER, insideVertical: BORDER };

function run(text, o = {}) {
  return new TextRun({ text: String(text), font: o.font || CJK, size: o.size || BODY, bold: !!o.bold, color: o.color });
}
function rich(text, o = {}) {
  const parts = String(text).split('**');
  if (parts.length === 1) return [run(text, o)];
  const out = [];
  parts.forEach((s, i) => { if (s !== '') out.push(run(s, Object.assign({}, o, { bold: (i % 2 === 1) || !!o.bold }))); });
  return out;
}
function para(text, o = {}) {
  let kids;
  if (Array.isArray(text)) kids = text;
  else if (text instanceof TextRun) kids = [text];
  else kids = rich(text, o);
  return new Paragraph({
    children: kids, alignment: o.align,
    spacing: o.spacing || { line: 340, before: o.before === undefined ? 0 : o.before, after: o.after === undefined ? 80 : o.after },
    indent: o.indent, heading: o.heading
  });
}
function cell(children, w, o = {}) {
  const arr = (Array.isArray(children) ? children : [children]).map(c =>
    (c instanceof Paragraph || c instanceof Table) ? c : para(c, { size: o.size || SMALL, align: o.align, spacing: { line: 280, after: 0 } }));
  return new TableCell({
    children: arr, width: { size: w, type: WidthType.DXA }, verticalAlign: VerticalAlign.CENTER,
    margins: { top: 60, bottom: 60, left: 90, right: 90 },
    shading: o.shading ? { fill: o.shading } : undefined
  });
}
function makeTable(head, rows, widths) {
  const trs = [];
  if (head) trs.push(new TableRow({
    tableHeader: true,
    children: head.map((h, i) => cell(run(h, { bold: true, size: SMALL }), widths[i], { align: AlignmentType.CENTER, shading: 'EDF2F7' }))
  }));
  rows.forEach(r => trs.push(new TableRow({ children: r.map((c, i) => cell(c, widths[i])) })));
  return new Table({ rows: trs, width: { size: CONTENT_W, type: WidthType.DXA }, columnWidths: widths, layout: TableLayoutType.FIXED, borders: BORDERS });
}
function widthsOf(pct) { return pct.map(w => Math.round(CONTENT_W * w / 100)); }
function caption(text, o = {}) {
  return para(run(text, { bold: true, size: SMALL }), { align: o.align || AlignmentType.CENTER, spacing: { line: 300, before: 120, after: 60 } });
}
function image(file, cap, maxWpx) {
  const png = fs.readFileSync(path.join(gen, file));
  const specFile = path.join(gen, 'spec', path.basename(file, '.png') + '.json');
  let w = 1000, h = 600;
  if (fs.existsSync(specFile)) { const s = JSON.parse(fs.readFileSync(specFile, 'utf8')); w = s.w; h = s.h; }
  const maxW = maxWpx || 580, maxH = 600;
  let dw = maxW, dh = maxW * h / w;
  if (dh > maxH) { dh = maxH; dw = maxH * w / h; }
  const out = [new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 120, after: 40, line: 260 },
    children: [new ImageRun({ type: 'png', data: png, transformation: { width: Math.round(dw), height: Math.round(dh) } })]
  })];
  if (cap) out.push(caption(cap));
  return out;
}

const counters = {};
function fignum(ch) { counters['f' + ch] = (counters['f' + ch] || 0) + 1; return '图' + ch + '-' + counters['f' + ch]; }
function tblnum(ch) { counters['t' + ch] = (counters['t' + ch] || 0) + 1; return '表' + ch + '-' + counters['t' + ch]; }
let chapter = '1';
function txt(s) {
  return String(s).replace(/\{fig\}/g, () => fignum(chapter)).replace(/\{tbl\}/g, () => tblnum(chapter));
}

const children = [];
children.push(para('', { spacing: { line: 400, after: 0 } }));
children.push(para(run(meta.title, { bold: true, size: 48, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { line: 600, after: 120 } }));
children.push(para(run(meta.subtitle, { size: 24 }), { align: AlignmentType.CENTER, spacing: { line: 420, after: 240 } }));
children.push(new Paragraph({ children: [new PageBreak()] }));

const stats = { p: 0, tbl: 0, img: 0, self: 0 };
for (const b of blocks) {
  if (b.t === 'h1') {
    const mm = String(b.x).match(/(\d+)/); if (mm) chapter = mm[1];
    children.push(new Paragraph({ children: [new PageBreak()] }));
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [run(b.x, { bold: true, size: 32, font: CJK_B, color: '000000' })], spacing: { before: 60, after: 180, line: 420 } }));
  }
  else if (b.t === 'h2') children.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [run(b.x, { bold: true, size: 28, font: CJK_B, color: '000000' })], spacing: { before: 220, after: 120, line: 380 } }));
  else if (b.t === 'h3') children.push(new Paragraph({ heading: HeadingLevel.HEADING_3, children: [run(b.x, { bold: true, size: 24, font: CJK_B, color: '000000' })], spacing: { before: 180, after: 100, line: 360 } }));
  else if (b.t === 'p') { children.push(para(txt(b.x), { indent: { firstLine: 420 } })); stats.p++; }
  else if (b.t === 'note') { children.push(para(run(txt(b.x), { size: SMALL, color: '555555', font: { eastAsia: '宋体', ascii: 'Consolas', hAnsi: 'Consolas' } }), { indent: { left: 420 } })); stats.p++; }
  else if (b.t === 'ul') b.x.forEach(s => { children.push(para([run('● ')].concat(rich(txt(s))), { indent: { left: 420, hanging: 200 }, spacing: { line: 340, after: 60 } })); stats.p++; });
  else if (b.t === 'ol') b.x.forEach((s, i) => { children.push(para([run((i + 1) + '．')].concat(rich(txt(s))), { indent: { left: 420, hanging: 240 }, spacing: { line: 340, after: 60 } })); stats.p++; });
  else if (b.t === 'table') {
    children.push(caption(b.cap.indexOf('{tbl}') >= 0 ? txt(b.cap) : txt('{tbl}') + ' ' + b.cap));
    children.push(makeTable(b.head, b.rows.map(r => r.map(c => txt(c))), widthsOf(b.widths)));
    stats.tbl++;
  }
  else if (b.t === 'plain_table') {
    if (b.cap) children.push(para(run(b.cap, { bold: true, size: 24, font: CJK_B }), { spacing: { before: 180, after: 80, line: 360 } }));
    children.push(makeTable(b.head, b.rows, widthsOf(b.widths)));
    if (b.cap && b.cap.indexOf('本章自评') >= 0) stats.self++; else stats.tbl++;
  }
  else if (b.t === 'toc') {
    children.push(para(run('目录', { bold: true, size: 32, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { before: 200, after: 160 } }));
    children.push(new TableOfContents('目录', { hyperlink: true, headingStyleRange: '1-3' }));
  }
  else if (b.t === 'img') { image(b.file, (b.cap ? txt('{fig}') + ' ' + b.cap : ''), b.maxW).forEach(x => children.push(x)); stats.img++; }
  else throw new Error('unknown block type ' + b.t);
}

const doc = new Document({
  styles: { default: { document: { run: { font: CJK, size: BODY }, paragraph: { spacing: { line: 340, after: 80 } } } } },
  features: { updateFields: true },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1418, right: 1418, bottom: 1418, left: 1418 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ children: [PageNumber.CURRENT], font: CJK, size: 18 })] })] }) },
    children: children
  }]
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync(outPath, buf);
  console.log('WROTE ' + outPath + '  bytes=' + buf.length);
  console.log('paragraphs=' + stats.p + ' tables=' + stats.tbl + ' selfcheck=' + stats.self + ' images=' + stats.img);
}).catch(e => { console.error('FAILED', e); process.exit(1); });
