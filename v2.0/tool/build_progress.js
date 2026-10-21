/* build_progress.js — 生成《开发进度说明（三阶段）》文档
 * 内容源: progress_content.json    Usage: node build_progress.js [outPath]
 */
'use strict';
const fs = require('fs');
const path = require('path');
const dx = require('docx');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, BorderStyle, TableLayoutType, VerticalAlign, PageBreak, Footer, PageNumber, TableOfContents
} = dx;

const gen = __dirname;
const content = JSON.parse(fs.readFileSync(path.join(gen, 'progress_content.json'), 'utf8'));
const outPath = process.argv[2] || path.join(gen, 'out_progress.docx');

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
function caption(text) {
  return para(run(text, { bold: true, size: SMALL }), { align: AlignmentType.CENTER, spacing: { line: 300, before: 120, after: 60 } });
}

const tblNo = {};
let chapter = '0';
function tblnum(ch) { tblNo[ch] = (tblNo[ch] || 0) + 1; return '表' + ch + '-' + tblNo[ch]; }
function txt(s) {
  return String(s).replace(/\{tbl\}/g, () => tblnum(chapter));
}

const m = content.meta;
const children = [];
children.push(para('', { spacing: { line: 400, after: 0 } }));
children.push(para(run(m.title, { bold: true, size: 48, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { line: 600, after: 120 } }));
children.push(para(run(m.subtitle, { bold: true, size: 36, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { line: 520, after: 300 } }));
[
  ['组长 / 学号', m.leader],
  ['班级', m.school_class],
  ['日期', m.date]
].forEach(([k, v]) => children.push(para(run(k + '：' + v, { size: 24 }), { align: AlignmentType.CENTER, spacing: { line: 380, after: 40 } })));
children.push(new Paragraph({ children: [new PageBreak()] }));
children.push(para(run('目录', { bold: true, size: 32, font: CJK_B }), { align: AlignmentType.CENTER, spacing: { before: 200, after: 160 } }));
children.push(new TableOfContents('目录', { hyperlink: true, headingStyleRange: '1-2' }));
children.push(new Paragraph({ children: [new PageBreak()] }));

const stats = { p: 0, tbl: 0 };
for (const b of content.blocks) {
  if (b.t === 'h1') {
    const mm = String(b.x).match(/第\s*(\d+)\s*章/); chapter = mm ? mm[1] : chapter;
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [run(b.x, { bold: true, size: 32, font: CJK_B, color: '000000' })], spacing: { before: 240, after: 180, line: 420 } }));
  }
  else if (b.t === 'h2') children.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [run(b.x, { bold: true, size: 28, font: CJK_B, color: '000000' })], spacing: { before: 220, after: 120, line: 380 } }));
  else if (b.t === 'h3') children.push(new Paragraph({ heading: HeadingLevel.HEADING_3, children: [run(b.x, { bold: true, size: 24, font: CJK_B, color: '000000' })], spacing: { before: 180, after: 100, line: 360 } }));
  else if (b.t === 'p') { children.push(para(txt(b.x), { indent: { firstLine: 420 } })); stats.p++; }
  else if (b.t === 'note') { children.push(para(run(txt(b.x), { size: SMALL, color: '555555' }), { indent: { left: 420 } })); stats.p++; }
  else if (b.t === 'ul') b.x.forEach(s => { children.push(para([run('● ')].concat(rich(txt(s))), { indent: { left: 420, hanging: 200 }, spacing: { line: 340, after: 60 } })); stats.p++; });
  else if (b.t === 'ol') b.x.forEach((s, i) => { children.push(para([run((i + 1) + '．')].concat(rich(txt(s))), { indent: { left: 420, hanging: 240 }, spacing: { line: 340, after: 60 } })); stats.p++; });
  else if (b.t === 'table') { children.push(caption(txt('{tbl}') + ' ' + b.cap)); children.push(makeTable(b.head, b.rows.map(r => r.map(c => txt(c))), widthsOf(b.widths))); stats.tbl++; }
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
  console.log('paragraphs=' + stats.p + ' tables=' + stats.tbl);
}).catch(e => { console.error('FAILED', e); process.exit(1); });
