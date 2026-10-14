/* check.js — verify every spec's shapes fit inside the declared canvas */
'use strict';
const fs = require('fs'), path = require('path');
const dir = path.join(__dirname, 'spec');
let bad = 0;
for (const f of fs.readdirSync(dir).filter(x => x.endsWith('.json'))) {
  const s = JSON.parse(fs.readFileSync(path.join(dir, f), 'utf8'));
  const issues = [];
  (s.nodes || []).forEach(n => {
    let h = n.h;
    if (n.t === 'cls' && !h) h = 24 + ((n.attrs || []).length + (n.methods || []).length) * 16;
    if (n.t === 'entity' && (n.rows || []).length && !h) h = 26 + n.rows.length * 17;
    if (h === undefined) h = 56;
    if (n.x + n.w > s.w - 6) issues.push(`${n.id}: right ${n.x + n.w} > ${s.w}`);
    if (n.y + h > s.h - 8) issues.push(`${n.id}: bottom ${n.y + h} > ${s.h}`);
  });
  (s.groups || []).forEach((g, i) => {
    if (g.x + g.w > s.w - 4) issues.push(`group${i}: right ${g.x + g.w} > ${s.w}`);
    if (g.y + g.h > s.h - 4) issues.push(`group${i}: bottom ${g.y + g.h} > ${s.h}`);
  });
  (s.seqs || []).forEach(sp => {
    sp.lifelines.forEach(l => {
      if (l.x + (l.w || 170) / 2 > s.w - 6) issues.push(`lifeline ${l.id}: right ${l.x + (l.w || 170) / 2} > ${s.w}`);
    });
    sp.msgs.forEach(m => { if (m.y > s.h - 20) issues.push(`msg@${m.y} exceeds canvas`); });
  });
  if (issues.length) { bad++; console.log('!! ' + f + '\n   ' + issues.join('\n   ')); }
}
console.log(bad ? ('SPECS WITH ISSUES: ' + bad) : 'all specs fit');
