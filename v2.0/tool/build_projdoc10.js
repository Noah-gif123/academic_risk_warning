/* build_projdoc10.js — 生成完整版《项目文档》（第 1~10 章，按课程模板结构）
 * 内容源：
 *   第 1~2 章 proj_doc_strict.json ｜ 第 3 章 proj_doc_ch3.json + proj_doc_ch3_ai.json（3.4）
 *   第 4 章 proj_doc_ch4.json ｜ 第 5 章 proj_doc_ch5.json
 *   第 6~7 章 proj_doc_ch6_7.json ｜ 第 8~10 章 proj_doc_ch8_10.json
 * 与 build_projdoc5.js 的差别：全文项目名统一替换为新名称；封面标题分两行；
 * 修订记录补 V1.1；第 6~9 章按模板补【本章自评（学习自检）】表。
 * Usage: node build_projdoc10.js [outPath]
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
const read = f => JSON.parse(fs.readFileSync(path.join(gen, f), 'utf8'));
const c12 = read('proj_doc_strict.json');
const c3 = read('proj_doc_ch3.json');
const c3ai = read('proj_doc_ch3_ai.json');
const c4 = read('proj_doc_ch4.json');
const c5 = read('proj_doc_ch5.json');
const c67 = read('proj_doc_ch6_7.json');
const c810 = read('proj_doc_ch8_10.json');
const meta = c12.meta;
let blocks = c12.blocks.concat(c3.blocks, c3ai.blocks, c4.blocks, c5.blocks, c67.blocks, c810.blocks);

// ---------- 全文统一改名 ----------
const NEW_NAME = '基于RAG与多智能体的学情预警与个性化学习管理系统的设计与实现';
const OLD_NAME = '学情预警系统';
function renameStr(s) {
  if (typeof s !== 'string') return s;
  // 已经带上新名称的串（封面等）不再替换，避免出现“基于…的…系统与个性化…”这类重复
  if (s.indexOf(NEW_NAME) >= 0) return s;
  return s.split(OLD_NAME).join(NEW_NAME);
}
function renameBlock(b) {
  const out = Object.assign({}, b);
  if (typeof out.x === 'string') out.x = renameStr(out.x);
  else if (Array.isArray(out.x)) out.x = out.x.map(renameStr);
  if (typeof out.cap === 'string') out.cap = renameStr(out.cap);
  if (Array.isArray(out.head)) out.head = out.head.map(renameStr);
  if (Array.isArray(out.rows)) out.rows = out.rows.map(r => r.map(c => (typeof c === 'string' ? renameStr(c) : c)));
  return out;
}
blocks = blocks.map(renameBlock);

// ---------- 修订记录：V1.0（1~5 章）+ V1.1（补 6~10 章并改名） ----------
blocks.forEach(b => {
  if (b.t === 'plain_table' && b.cap === '文档修订记录') {
    b.rows = [
      ['V1.0', '2026-09', '初稿：完成第 1 章 项目概述、第 2 章 业务分析、第 3 章 子系统设计、第 4 章 数据设计、第 5 章 技术选型与开发环境', '狄珂'],
      ['V1.1', '2026-11', '按《项目文档模板-2026》补全第 6~10 章（测试部署与运维、非功能性分析、AI 辅助开发实践、项目实践与能力成长、AI 运用能力自评与举证），新增 3.4 RAG 检索与智能体子系统，并统一改名为“' + NEW_NAME + '”', '狄珂']
    ];
  }
  if (b.t === 'plain_table' && b.cap === '项目基本信息') {
    b.rows.forEach(r => {
      if (r[0] === '项目名称') r[1] = NEW_NAME + '（v2.0）';
    });
  }
});

// ---------- 本章自评（学习自检）：第 1~9 章（模板第 10 章无自检表） ----------
const SELF = {
  1: [
    ['能清晰说明项目背景、目标用户与项目边界', '✓', '1.1 给出痛点、目标用户与边界（不含自动排课与学费管理）'],
    ['功能需求与非功能需求已分别列出且可验证', '✓', '1.3 用用户故事 + 非功能指标（响应时间、口令加密、并发量），第 6 章按此设计测试'],
    ['项目目标符合 SMART 原则并包含 AI 辅助目标', '✓', '1.4 业务/技术/AI 三类目标均可度量（含 60% 样板代码、关键模块 100% 人工复核）'],
    ['术语表已覆盖全文关键缩写', '✓', '表1-1 覆盖 ER、RBAC、PBKDF2、RAG、RRF、多智能体、双轨制等全文高频词']
  ],
  2: [
    ['用例图覆盖主要参与者与核心用例', '✓', '图2-1、图2-2 覆盖学生/教师/导员/教务四端；教务端用例复用总体图，未单独出图'],
    ['总体架构图清晰且选型有理由', '✓', '图2-3 分层结构 + 图2-4 功能边界；选型理由在第 5 章展开'],
    ['业务模块职责边界明确、依赖合理', '✓', '表2-1 按“数据来源→判定→处置”三条链划分，模块间为单向依赖'],
    ['核心业务流程含异常分支', '✓', '图2-5、图2-6 含停课、未点名、超时升级等分支'],
    ['已识别 AI 依赖等新型风险并给出缓解措施', '✓', '表2-2 含“AI 生成代码无人能讲清”“口径被框架黑盒化”两类风险及缓解措施']
  ],
  3: [
    ['各子系统职责与接口契约完整清晰', '✓', '表3-1/3-3/3-5 给出接口、入参、出参与错误码；3.4 补 AI 子系统五个接口的契约与降级行为'],
    ['核心类职责单一、关系合理（无上帝类）', '△', '图3-3 核心领域类职责清晰；TeacherPortalService 仍承担多个页面的数据组装，计划下一阶段拆分'],
    ['关键流程含异常处理与事务边界', '✓', '图3-1/3-2/3-4；3.4 给出报告生成的六步流程与三层单向降级边界'],
    ['AI 生成的接口/类已人工核对安全性', '✓', '全部接口逐个核对鉴权与数据范围，越权（跨教学班、跨行政班、跨课程）已实测返回 400']
  ],
  4: [
    ['ER 图实体/联系/基数正确', '✓', '图4-1 覆盖学情预警子系统 13 张表，1:N 关系与外部锚点均标注，与 4.4 的十三张表结构一一对应'],
    ['建表语句字段、类型、约束经人工核对', '✓', '字段以线上库 information_schema 导出为准逐表核对；核对中修正了两处注释口径（17_dict_align.sql）'],
    ['敏感字段已设计脱敏/加密方案', '✓', '本子系统不存姓名与联系方式，只存 student_id 与聚合值；证据快照不含个人信息，配置变更留审计'],
    ['已考虑查询性能（索引/缓存）', '✓', '表4-17 的 14 组索引按页面实际查询建齐；风险状态本身即判定快照，充当缓存、不重复计算']
  ],
  5: [
    ['技术选型有对比且有明确理由', '✓', '表5-2 对六个关键决策点给出候选、结论与理由，并说明两处反常规选择的取舍'],
    ['Git 分支策略与 CI/CD 流程清晰', '△', '已用 Git + GitHub/Gitee（main 单分支）；未配置 CI/CD，构建与验证靠本地脚本与人工回归清单，方案见 6.5'],
    ['已调研同类方案并说明差异', '✓', '表5-4 按三类方案对比数据来源、判定时机、口径透明度与处置闭环，并说明未做的两件事'],
    ['引入的开源/AI 服务已关注许可证与合规', '✓', '表5-5 标注许可；百炼输入脱敏、输出不入库、超时降级，仅第三阶段使用']
  ],
  6: [
    ['已制定测试策略（分层/覆盖率/准出标准）', '✓', '6.1 给出四层策略（单测/脚本回归/E2E/口径校验）与准出标准；单测覆盖率以“关键算法 100% 覆盖”表述而非行覆盖率'],
    ['单元/集成/接口/E2E 测试规划完整且 AI 用例已人工 review', '✓', '25 例单测、7 例接口用例、9 节 E2E 清单；AI 生成的用例逐条核对断言是否真正验证行为'],
    ['性能与安全测试有关键场景与目标', '△', '安全测试完成（越权/注入/口令/AI 特有风险）；性能只测单请求耗时，未做压测，已在 6.4 标注'],
    ['测试已接入 CI 形成门禁', '✗', '未接入 CI（环境不支持进程派生与包管理），6.5 给出可直接落地的流水线草案'],
    ['已产出测试报告（用例汇总 + 缺陷统计，数据真实）', '✓', '表6-6 用例汇总、表6-7 缺陷统计均取自实跑结果，含“未运行/遗留”项，不虚报通过率'],
    ['部署拓扑与回滚方案明确', '✓', '6.7 给出拓扑、五阶段发布流程与逐阶段的回滚方式'],
    ['系统级与业务级监控点已设计', '✓', '表6-9 系统监控 6 个点、表6-10 业务监控 7 个点，含降级率与对账通过率'],
    ['日志已排除敏感信息', '✓', '口令哈希不回显、AppKey 掩码、AI 留痕只存摘要；日志滚动未配置（记为遗留）'],
    ['有数据备份策略与应急预案', '✓', '6.11 区分可重建与不可重建数据，给出 RPO/RTO 与三条应急预案；定时备份未自动化（遗留）']
  ],
  7: [
    ['已设计容错/补偿与幂等方案', '✓', '7.1 六个风险点逐条给出措施：单生事务隔离、通知幂等、三层降级、截断重试、数字校验、检索空回退'],
    ['安全三层面（业务/管理/系统）均覆盖且 AI 代码已审计', '✓', '7.2 三小节分别覆盖数据范围、管理留痕与五类系统风险；AI 代码逐项核查鉴权与数据范围'],
    ['性能与并发有量化分析与对策', '△', '给出单请求实测与设计目标，7.4 识别出“重复生成无互斥”等并发缺口；缺压测数据'],
    ['系统可测且测试规划完整', '✓', '7.5 说明可测性由“事实包与语言分离 + 固定段落 + 留痕表”设计保证，并指出不可测部分（报告写得好不好）']
  ],
  8: [
    ['团队 AI 工具配置统一且有禁用清单', '✓', '8.1 给出工具分工、统一提示词前缀与禁止上传清单（姓名/手机号/密钥/ai.env）'],
    ['人机协作流程形成闭环、关键节点人工把关', '✓', '8.2 六步闭环 + 表8-2 分工矩阵，明确“口径、选型、口径校验、降级顺序”必须人工拍板'],
    ['能写出结构化、可验收的提示词', '✓', '8.3 四个真实模板均含角色/任务/上下文/约束/输出格式/验收标准，并给出一个反例'],
    ['AI 代码执行了双人复核与隐私检查', '△', '单人项目以“机器通道 + 人工通道”替代双人复核；隐私检查逐项执行并记录姓名入提示词的风险'],
    ['认识到 AI 局限并保持批判性使用', '✓', '8.7 列出五类局限与应对，并在 10.4 给出“先自己定位、再让 AI 验证”的使用顺序']
  ],
  9: [
    ['已对照 9.1 九项能力做自我评估', '✓', '10.1 逐项自评并给出佐证，与 10.2 举证表一致'],
    ['9.2–9.4 反思有具体事例支撑、非空话', '✓', '9.2 给出“重复解释上下文”和“为三天后的自己留痕”两个具体教训；9.3 给出初期与末期对比'],
    ['明确列出了不足与改进计划', '✓', '9.3 表9-2 列出测试体系与性能意识两项不足及计划，9.4 给出六条改进清单并标优先级']
  ]
};
const SELF_HEAD = ['自检项（对照本章要求逐项勾选）', '自评 ✓/△/✗', '说明（不足处简要记录）'];
const SELF_W = [50, 10, 40];

// 定位每章结束位置（下一个 h1 之前），倒序插入；已有自评表的章节跳过
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

const outPath = process.argv[2] || path.join(gen, '..', 'word', NEW_NAME + '_项目文档.docx');

// 调试用：ONLY=6,7 只渲染指定章节（定位排版问题）
if (process.env.ONLY) {
  const want = String(process.env.ONLY).split(',').map(s => s.trim());
  const kept = [];
  let cur = null;
  for (const b of blocks) {
    if (b.t === 'h1') cur = (String(b.x).match(/(\d+)/) || [])[1] || null;
    if (cur === null || want.indexOf(cur) >= 0) kept.push(b);
  }
  blocks = kept;
  console.log('ONLY=' + process.env.ONLY + ' blocks=' + blocks.length);
}

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
// 封面标题：长标题分两行，避免挤成一行
const titleLines = meta.titleLines || [NEW_NAME];
titleLines.forEach(line => {
  children.push(para(run(line, { bold: true, size: 36, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { line: 560, after: 60 } }));
});
children.push(para(run(meta.subtitle, { size: 24 }), { align: AlignmentType.CENTER, spacing: { line: 420, before: 180, after: 240 } }));
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
  else if (b.t === 'note') {
    String(txt(b.x)).split('\n').forEach(line => {
      children.push(para(run(line, { size: SMALL, color: '555555', font: { eastAsia: '宋体', ascii: 'Consolas', hAnsi: 'Consolas' } }), { indent: { left: 420 } }));
      stats.p++;
    });
  }
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
