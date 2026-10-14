/* mkuc.js — build per-role use case diagram specs from cases.json */
'use strict';
const fs = require('fs');
const path = require('path');
const dir = __dirname;
const data = JSON.parse(fs.readFileSync(path.join(dir, 'cases.json'), 'utf8'));
const byId = {};
data.cases.forEach(c => { byId[c.id] = c; });

const captions = {
  teacher: '注：为减少连线交叉，同一参与者在图中出现两次；教师只能访问本人教学班的数据。',
  student: '注：学生端的可见数据范围由登录令牌限定，只能查看本人数据，且按可见性规则裁剪数值。',
  admin: '注：教务端的数据范围为全校；成绩构成配置已下放课程组，教务只保留审批与裁定。',
  counselor: '注：辅导员端数据范围仅限本人名下行政班，跨教学班、跨课程聚合统计。',
  system: '注：系统服务由定时任务触发；大模型服务只提供解释与建议，不参与风险判定与分数计算。'
};

function wrapUC(g) { return g; }

for (const r of data.roles) {
  const ucs = r.ucs.map(id => byId[id]);
  const missing = ucs.filter(u => !u);
  if (missing.length) throw new Error('missing cases in role ' + r.key);
  const n = ucs.length;
  let spec;
  if (r.mode === 'one') {
    const rows = n;
    const bw = 520, bx = 250;
    const h = rows * 74 + 50;
    const H = h + 92;
    const rightActor = !!r.actor2;
    const W = rightActor ? 1080 : 840;
    const nodes = [
      { id: 'b', t: 'boundary', x: bx, y: 40, w: bw, h: h, label: '学情预警系统' },
      { id: 'ac', t: 'actor', x: 70, y: 40 + h / 2 - 50, w: 90, h: 100, label: r.actor }
    ];
    const edges = [];
    ucs.forEach((u, i) => {
      const id = 'u' + i;
      nodes.push({ id: id, t: 'uc', x: bx + 25, y: 96 + i * 74, w: bw - 50, h: 54, label: u.name });
      edges.push({ f: 'ac', to: id, k: 'assoc', sw: 1.3 });
    });
    if (rightActor) {
      nodes.push({ id: 'ac2', t: 'actor', x: 950, y: 40 + h / 2 - 50, w: 90, h: 100, label: r.actor2 });
      const last = n - 1;
      edges.push({ f: 'ac2', to: 'u' + last, k: 'assoc', sw: 1.3 });
      edges.push({ f: 'ac2', to: 'u' + (last - 1), k: 'assoc', sw: 1.3 });
    }
    spec = { title: r.fig + ' ' + r.title, w: W, h: H, nodes: nodes, edges: edges, caption: captions[r.key] };
  } else {
    const rows = Math.ceil(n / 2);
    const bx = 280, bw = 700;
    const c1 = 310, c2 = 650, cw = 310;
    const h = rows * 74 + 50;
    const H = h + 92;
    const W = 1150;
    const nodes = [
      { id: 'b', t: 'boundary', x: bx, y: 40, w: bw, h: h, label: '学情预警系统' },
      { id: 'ac', t: 'actor', x: 70, y: 40 + h / 2 - 50, w: 90, h: 100, label: r.actor },
      { id: 'ac2', t: 'actor', x: 1010, y: 40 + h / 2 - 50, w: 90, h: 100, label: r.actor }
    ];
    const edges = [];
    ucs.forEach((u, i) => {
      const col = i % 2, row = Math.floor(i / 2);
      const id = 'u' + i;
      nodes.push({ id: id, t: 'uc', x: col ? c2 : c1, y: 96 + row * 74, w: cw, h: 54, label: u.name });
      edges.push({ f: col ? 'ac2' : 'ac', to: id, k: 'assoc', sw: 1.3 });
    });
    spec = { title: r.fig + ' ' + r.title, w: W, h: H, nodes: nodes, edges: edges, caption: captions[r.key] };
  }
  const out = path.join(dir, 'spec', r.file + '.json');
  fs.writeFileSync(out, JSON.stringify(wrapUC(spec), null, 1), 'utf8');
  console.log('WROTE ' + r.file + '.json  ucs=' + n);
}
