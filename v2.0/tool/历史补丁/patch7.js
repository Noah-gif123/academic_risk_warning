/* patch7.js — 移除已确认的两条，改写目录共享那一条，并输出最终待确认清单 */
'use strict';
const fs = require('fs'), path = require('path');
const p = path.join(__dirname, 'feature_content.json');
const d = JSON.parse(fs.readFileSync(p, 'utf8'));
const B = d.blocks;
const i = B.findIndex(b => b.t === 'h1' && b.x.indexOf('第8章') === 0);
let ol = null;
for (let k = i; k < i + 6 && k < B.length; k++) if (B[k].t === 'ol') { ol = B[k]; break; }

const before = ol.x.length;
ol.x = ol.x.filter(s => !(s.indexOf('教师课程卡片上的“快捷”') === 0 || s.indexOf('“我的教学项目”放什么') === 0));
ol.x = ol.x.map(s => s.indexOf('目录是否需要“复制上一学期') === 0
  ? '目录是否需要“复制上一学期”的能力？（同一门课多班是否共用目录已确认：不共用，每位老师各自维护自己那份；只剩“从上一学期复制一套目录”这个便利功能要不要做。）'
  : s);

fs.writeFileSync(p, JSON.stringify(d, null, 1), 'utf8');
console.log('待确认：' + before + ' → ' + ol.x.length);
console.log('=== 剩余待确认（共 ' + ol.x.length + ' 条）===');
ol.x.forEach((s, n) => console.log((n + 1) + '. ' + s));
