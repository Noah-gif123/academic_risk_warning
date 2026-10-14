/* patch3.js — 学生端补一句“结课”机制说明 */
'use strict';
const fs = require('fs'), path = require('path');
const p = path.join(__dirname, 'feature_content.json');
const d = JSON.parse(fs.readFileSync(p, 'utf8'));
const i = d.blocks.findIndex(b => b.t === 'p' && b.x.indexOf('点击卡片上的“开始学习”进入该课程的详情页') === 0);
if (i >= 0) {
  d.blocks[i].x = '点击卡片上的“开始学习”进入该课程的详情页；进入后右上角显示该课程当前状态（进行中 / 未开课）。**教师对该课程执行“结课”后，学生端就不再显示这门课**（即过去学期与已结课的课程在界面上消失，数据仍保留在库中，用于课程历史挂科率与教师尺度的估计）。';
  console.log('patched student note at block ' + i);
} else console.log('target not found');
fs.writeFileSync(p, JSON.stringify(d, null, 1), 'utf8');
