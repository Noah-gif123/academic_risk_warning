/* patch8.js — 第8章由“待确认”转为“已确认的开发默认值”，并更新 0.1 的表述 */
'use strict';
const fs = require('fs'), path = require('path');
const p = path.join(__dirname, 'feature_content.json');
const d = JSON.parse(fs.readFileSync(p, 'utf8'));
const B = d.blocks;
let n = 0;

B.forEach(b => {
  if (b.t === 'h1' && b.x.indexOf('第8章') === 0) { b.x = '第8章　开发默认值（已确认）'; n++; }
  if (b.t === 'p' && b.x.indexOf('此前所有待确认项都已确认完毕') === 0) {
    b.x = '本章 12 条是开发中才会碰到的细节，**已全部确认，按下列默认值执行**；开发过程中如需调整，单独记录变更即可。';
    n++;
  }
  if (b.t === 'p' && b.x.indexOf('口径来源：v2.0 数据库设计') === 0) {
    b.x = b.x.replace('**本轮问答把此前所有待确认项确认完毕，本文档即口径冻结版**；我仍然拿不准的细节集中在第 8 章，供你最后过一遍。',
      '**本轮问答把此前所有待确认项确认完毕，本文档即口径冻结版**；第 8 章列出的是开发细节的默认取值，已一并确认。');
    n++;
  }
});
fs.writeFileSync(p, JSON.stringify(d, null, 1), 'utf8');
console.log('updated blocks: ' + n);
const i8 = B.findIndex(b => b.t === 'h1' && b.x.indexOf('第8章') === 0);
console.log('第8章标题: ' + B[i8].x);
console.log('确认条目: ' + B[i8 + 2].x.length);
