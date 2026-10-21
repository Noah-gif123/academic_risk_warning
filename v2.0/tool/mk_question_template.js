/* mk_question_template.js — 生成《题目导入模板.docx》（也是导入功能的样例文件） */
'use strict';
const fs = require('fs');
const path = require('path');
const dx = require('docx');

const { Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType } = dx;

const CJK = '微软雅黑';

function p(text, opts = {}) {
  return new Paragraph({
    spacing: { after: opts.after === undefined ? 60 : opts.after, line: 320 },
    children: [new TextRun({ text, font: CJK, size: opts.size || 22, bold: !!opts.bold, color: opts.color })],
    indent: opts.indent ? { left: opts.indent } : undefined
  });
}

const blocks = [
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 200 },
    children: [new TextRun({ text: '题目导入模板', font: CJK, size: 34, bold: true })]
  }),
  p('说明：把题目按下面的格式写进 Word（.docx）或纯文本（.txt），在「测验与作业 → 导入题目」里上传，系统先解析成草稿，教师在校对页修正后再保存发布。', { color: '555555' }),
  p('格式约定：', { bold: true }),
  p('1）大题标题写成「一、单选题（每题2分）」这种形式，题型与每题分值会自动带下来；', { indent: 300 }),
  p('2）每题以「1.」开头，选项以「A.」「B.」开头，答案行写「答案：B」；', { indent: 300 }),
  p('3）可选行：「解析：……」「分值：3」；题型也可在题干里写「（多选）」直接指定；', { indent: 300 }),
  p('4）填空题多个空的答案用「；」分隔，按顺序逐个比对；', { indent: 300 }),
  p('5）判断题答案写「对 / 错」（也接受 正确 / 错误 / √ / ×）。', { indent: 300 }),
  p('6）目前支持 .docx 与 .txt；PDF 需额外解析库，请先另存为 Word。', { indent: 300, color: '555555' }),
  new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 260, after: 120 }, children: [new TextRun({ text: '一、单选题（每题2分）', font: CJK, size: 26, bold: true })] }),
  p('1. 下列哪个是操作系统？'),
  p('A. Word', { indent: 300 }), p('B. Windows', { indent: 300 }), p('C. Excel', { indent: 300 }), p('D. Photoshop', { indent: 300 }),
  p('答案：B'),
  p('解析：Windows 是操作系统，其余为应用软件。', { color: '555555' }),
  p('2. 十进制数 255 对应的十六进制是？'),
  p('A. FE', { indent: 300 }), p('B. FF', { indent: 300 }), p('C. 100', { indent: 300 }), p('D. 2FF', { indent: 300 }),
  p('答案：B'),
  new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 120 }, children: [new TextRun({ text: '二、多选题（每题3分）', font: CJK, size: 26, bold: true })] }),
  p('1. 下列属于计算机输入设备的有？'),
  p('A. 键盘', { indent: 300 }), p('B. 鼠标', { indent: 300 }), p('C. 显示器', { indent: 300 }), p('D. 扫描仪', { indent: 300 }),
  p('答案：A,B,D'),
  new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 120 }, children: [new TextRun({ text: '三、判断题（每题2分）', font: CJK, size: 26, bold: true })] }),
  p('1. 计算机中 1 个字节等于 8 个二进制位。'),
  p('答案：对'),
  new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 120 }, children: [new TextRun({ text: '四、填空题（每题3分）', font: CJK, size: 26, bold: true })] }),
  p('1. 计算机的运算器和控制器合称为＿＿＿＿；内存储器断电后数据会＿＿＿＿。'),
  p('答案：中央处理器；丢失'),
  new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 120 }, children: [new TextRun({ text: '五、简答题（每题10分）', font: CJK, size: 26, bold: true })] }),
  p('1. 简述操作系统的三大基本功能。'),
  p('答案：处理器管理、存储管理、设备管理（含文件管理与作业管理亦可）')
];

const doc = new Document({
  styles: { default: { document: { run: { font: CJK, size: 22 } } } },
  sections: [{ properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1418, right: 1418, bottom: 1418, left: 1418 } } }, children: blocks }]
});

const out = path.join('E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/word', '题目导入模板.docx');
Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(out, buf);
  console.log('WROTE ' + out + ' bytes=' + buf.length);
});
