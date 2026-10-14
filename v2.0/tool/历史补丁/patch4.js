/* patch4.js — 修掉两处遗留的“基础设置”，并自检关键段落 */
'use strict';
const fs = require('fs'), path = require('path');
const p = path.join(__dirname, 'feature_content.json');
const d = JSON.parse(fs.readFileSync(p, 'utf8'));
const B = d.blocks;
let fixed = 0;

// 1) 3.1 页面层级：课程详情只有三个页签
const i1 = B.findIndex(b => b.t === 'h2' && b.x.indexOf('3.1 ') === 0);
for (let k = i1; k < i1 + 4 && k < B.length; k++) {
  if (B[k].t === 'p' && B[k].x.indexOf('页面层级共三层') === 0) {
    B[k].x = '页面层级共三层：① 首页（我的教学课程）→ ② 课程详情（页签：教学班级 / 教学统计 / 课程考核）→ ③ 教学详情（从某个教学班点“开始教学”进入，页签：导学 / 教学目标 / 目录 / 测验 / 作业 / 项目）。教师端与学生端使用同一套视觉风格。';
    fixed++;
  }
}
// 2) 3.7 开头一句不要再提“基础设置”
const i7 = B.findIndex(b => b.t === 'h2' && b.x.indexOf('3.7 ') === 0);
for (let k = i7; k < i7 + 4 && k < B.length; k++) {
  if (B[k].t === 'p' && B[k].x.indexOf('导航栏只有一个预警入口') === 0) {
    B[k].x = '导航栏只有一个预警入口“预警监控”，把预警相关的事情集中在这里，不散落在课程详情或教学详情里。';
    fixed++;
  }
}
fs.writeFileSync(p, JSON.stringify(d, null, 1), 'utf8');
console.log('fixed blocks: ' + fixed);

// 自检
const check = (label, cond) => console.log((cond ? 'OK   ' : 'FAIL ') + label);
const all = JSON.stringify(B);
check('3.1 不再含基础设置', B[i1 + 1] ? B[i1 + 1].x.indexOf('基础设置') < 0 : false);
check('3.4 标题为三个页签', B.some(b => b.t === 'h2' && b.x === '3.4 课程详情：三个页签'));
check('3.4.4 基础设置小节已删除', !B.some(b => b.t === 'h3' && b.x.indexOf('3.4.4') === 0));
check('3.5 明确每位老师各自维护', B.some(b => b.t === 'p' && b.x.indexOf('每个老师都对自己的教学班内容独立维护') > 0));
check('覆盖率统一为三分之一', !B.some(b => b.t === 'ul' && Array.isArray(b.x) && b.x.some(s => s.indexOf('低于二分之一') >= 0)) && B.some(b => b.t === 'table' && b.rows && b.rows.some(r => r[0] === '覆盖率' && r[2].indexOf('三分之一') >= 0)));
check('结课机制已写入首页', B.some(b => b.t === 'ol' && Array.isArray(b.x) && b.x.some(s => s.indexOf('结课') >= 0)));
const i8 = B.findIndex(b => b.t === 'h1' && b.x.indexOf('第8章') === 0);
let pend = 0; for (let k = i8; k < i8 + 6; k++) if (B[k] && B[k].t === 'ol') { pend = B[k].x.length; break; }
console.log('待确认条数: ' + pend);
