/* patch6.js — 首页区块改为“待批改”（作业 / 测试 / 项目 三类），并输出剩余待确认清单 */
'use strict';
const fs = require('fs'), path = require('path');
const p = path.join(__dirname, 'feature_content.json');
const d = JSON.parse(fs.readFileSync(p, 'utf8'));
const B = d.blocks;

// 1) 表5 中的“首页·我的教学项目” → “首页·待批改”
const t = B.find(b => b.t === 'table' && b.cap.indexOf('教师端功能总览') >= 0);
if (t) {
  t.rows.forEach(r => {
    if (r[1] === '首页·我的教学项目') {
      r[1] = '首页·待批改';
      r[2] = '分作业 / 测试 / 项目三类列出待批改任务（版式与学生端“我的项目”区块相近），点击直接进入批改，并显示待批改数量';
    }
  });
}

// 2) 3.3 首页第 4 条
const i3 = B.findIndex(b => b.t === 'h2' && b.x.indexOf('3.3 ') === 0);
for (let k = i3; k < i3 + 6 && k < B.length; k++) {
  if (B[k].t === 'ol') {
    B[k].x = B[k].x.map(s => s.indexOf('我的教学项目') === 0
      ? '待批改：分**作业 / 测试 / 项目**三类展示本人待批改的任务（版式与学生端的“我的项目”区块相近），显示每类的待批改数量，点击可直接进入批改；具体字段与排序规则在开发阶段再细化。'
      : s);
    break;
  }
}

// 3) 3.8 里补一句：待批改是首页入口
const i8 = B.findIndex(b => b.t === 'h2' && b.x.indexOf('3.8 ') === 0);
for (let k = i8; k < i8 + 5 && k < B.length; k++) {
  if (B[k].t === 'ul') {
    if (!B[k].x.some(s => s.indexOf('待批改') >= 0)) B[k].x.unshift('待批改入口：首页“待批改”区块按作业 / 测试 / 项目三类汇总待批改任务，点击进入对应批改界面；');
    break;
  }
}

fs.writeFileSync(p, JSON.stringify(d, null, 1), 'utf8');

// 输出剩余待确认清单
const i = B.findIndex(b => b.t === 'h1' && b.x.indexOf('第8章') === 0);
let ol = null;
for (let k = i; k < i + 6 && k < B.length; k++) if (B[k].t === 'ol') { ol = B[k]; break; }
console.log('=== 剩余待确认（共 ' + ol.x.length + ' 条）===');
ol.x.forEach((s, n) => console.log((n + 1) + '. ' + s));
