/* patch5.js — 重新写入 3.5 导语（每位老师各自维护），并做最终自检 */
'use strict';
const fs = require('fs'), path = require('path');
const p = path.join(__dirname, 'feature_content.json');
const d = JSON.parse(fs.readFileSync(p, 'utf8'));
const B = d.blocks;
const NEW = '从教学班级点“开始教学”进入教学详情页，页签与学生端一致：导学、教学目标、目录、测验、作业、项目（参考平台上的教材、知识图谱、FAQ、学习资料、试题等页签全部删除）。**每个老师都对自己的教学班内容独立维护、可自行修改**（发布后也能改，改动留痕），不同老师的教学班互不影响；测验、作业、项目三个页签是否出现由该课程的成绩折算规则（成绩构成）决定。';

const i5 = B.findIndex(b => b.t === 'h2' && b.x.indexOf('3.5 ') === 0);
let done = 0;
for (let k = i5 + 1; k < i5 + 4 && k < B.length; k++) {
  if (B[k].t === 'p' && B[k].x.indexOf('从教学班级点') === 0) { B[k].x = NEW; done++; break; }
}
fs.writeFileSync(p, JSON.stringify(d, null, 1), 'utf8');
console.log('3.5 导语更新: ' + done);

const check = (label, cond) => console.log((cond ? 'OK   ' : 'FAIL ') + label);
check('3.5 写明每位老师各自维护', B.some(b => b.t === 'p' && b.x.indexOf('每个老师都对自己的教学班内容独立维护') > 0));
check('3.5.1 条目完整', (() => { const k = B.findIndex(b => b.t === 'h3' && b.x.indexOf('3.5.1') === 0); return k >= 0 && B[k + 1] && B[k + 1].t === 'ul' && B[k + 1].x.length === 2; })());
check('3.1 无基础设置', (() => { const k = B.findIndex(b => b.t === 'h2' && b.x.indexOf('3.1 ') === 0); return B[k + 1].x.indexOf('基础设置') < 0; })());
check('3.4 = 三个页签', B.some(b => b.t === 'h2' && b.x === '3.4 课程详情：三个页签'));
check('3.4.4 已删除', !B.some(b => b.t === 'h3' && b.x.indexOf('3.4.4') === 0));
check('覆盖率口径 = 三分之一', B.some(b => b.t === 'table' && b.rows && b.rows.some(r => r[0] === '覆盖率' && r.length > 2 && r[2].indexOf('三分之一') >= 0)));
check('结课机制已写入', B.some(b => b.t === 'ol' && Array.isArray(b.x) && b.x.some(s => s.indexOf('结课') >= 0)));
check('无 malformed 列表', !B.some(b => (b.t === 'ul' || b.t === 'ol') && !Array.isArray(b.x)));
const i8 = B.findIndex(b => b.t === 'h1' && b.x.indexOf('第8章') === 0);
let pend = 0; for (let k = i8; k < i8 + 6; k++) if (B[k] && B[k].t === 'ol') { pend = B[k].x.length; break; }
console.log('待确认条数: ' + pend);
