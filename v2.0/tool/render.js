/* render.js — JSON graph spec -> SVG (+ .drawio)
 * Usage: node render.js <spec.json> <out.svg> [out.drawio]
 * No external deps. CJK-aware text metrics. Chrome-headless friendly.
 */
'use strict';
const fs = require('fs');

const CW = 1.0;   // CJK char width factor (x font-size)
const AW = 0.53;  // ASCII char width factor

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&apos;');
}
function isWide(ch) { return ch.charCodeAt(0) > 0x2000; }
function tw(s, fs) {
  let w = 0;
  for (const ch of String(s)) w += (isWide(ch) ? CW : AW) * fs;
  return w;
}
function tokens(s) {
  const out = []; let buf = '';
  for (const ch of String(s)) {
    if (isWide(ch) || ch === ' ') { if (buf) { out.push(buf); buf = ''; } out.push(ch); }
    else buf += ch;
  }
  if (buf) out.push(buf);
  return out;
}
function wrap(s, maxW, fs) {
  const lines = []; let cur = '';
  for (const tk of tokens(s)) {
    if (tk === '\n') { lines.push(cur); cur = ''; continue; }
    const cand = cur + tk;
    if (tw(cand, fs) > maxW && cur !== '') {
      lines.push(cur.trimEnd());
      cur = (tk === ' ') ? '' : tk;
    } else cur = cand;
  }
  if (cur.trim() !== '' || lines.length === 0) lines.push(cur.trimEnd());
  return lines;
}
function text(cx, cy, str, fs, opt) {
  opt = opt || {};
  const anchor = opt.anchor || 'middle';
  const fill = opt.fill || '#1a202c';
  const weight = opt.bold ? ' font-weight="700"' : '';
  return `<text x="${cx}" y="${cy}" font-size="${fs}" text-anchor="${anchor}" fill="${fill}"${weight} font-family="${opt.font || 'Microsoft YaHei, Segoe UI, sans-serif'}">${esc(str)}</text>`;
}
function block(cx, cy, lines, fs, opt) {
  opt = opt || {};
  const lh = opt.lh || fs * 1.45;
  const y0 = cy - ((lines.length - 1) * lh) / 2 + fs * 0.36;
  return lines.map((l, i) => text(cx + (opt.dx || 0), y0 + i * lh, l, fs, opt)).join('\n');
}

// ---------- shapes ----------
const P = {
  blue:   ['#eef4ff', '#2b6cb0'],
  green:  ['#e8f7ee', '#2f855a'],
  orange: ['#fff8f0', '#c05621'],
  grey:   ['#f2f4f7', '#4a5568'],
  purple: ['#f6f2ff', '#6b46c1'],
  red:    ['#fff5f5', '#c53030'],
  teal:   ['#e6fffa', '#2c7a7b'],
  yellow: ['#fefce8', '#b7791f'],
  white:  ['#ffffff', '#cbd5e0']
};

function node(g, n) {
  if (n.t === 'cls' && !n.h) n.h = 24 + ((n.attrs || []).length + (n.methods || []).length) * 16;
  if (n.t === 'entity' && (n.rows || []).length && !n.h) n.h = (n.hh || 26) + n.rows.length * (n.rh || 17);
  const x = n.x, y = n.y, w = n.w, h = n.h;
  const pal = P[n.pal || 'blue'];
  const fill = n.fill || pal[0], stroke = n.stroke || pal[1];
  const sw = n.sw || 1.6;
  const cx = x + w / 2, cy = y + h / 2;
  const fs = n.fs || 15;
  let s = '';
  switch (n.t) {
    case 'actor': {
      const hx = cx, hy = y + 14, r = 9;
      s += `<circle cx="${hx}" cy="${hy}" r="${r}" fill="#fff" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<line x1="${hx}" y1="${hy + r}" x2="${hx}" y2="${hy + r + 26}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<line x1="${hx - 18}" y1="${hy + r + 10}" x2="${hx + 18}" y2="${hy + r + 10}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<line x1="${hx}" y1="${hy + r + 26}" x2="${hx - 15}" y2="${hy + r + 48}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<line x1="${hx}" y1="${hy + r + 26}" x2="${hx + 15}" y2="${hy + r + 48}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, y + h - 12, wrap(n.label, w + 40, fs), fs);
      break;
    }
    case 'uc': {
      s += `<ellipse cx="${cx}" cy="${cy}" rx="${w / 2}" ry="${h / 2}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, cy, wrap(n.label, w * 0.80, fs), fs);
      break;
    }
    case 'boundary': {
      s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="6" fill="${n.fill || 'none'}" stroke="${stroke}" stroke-width="${sw}" stroke-dasharray="7 5"/>`;
      if (n.label) {
        const lw = tw(n.label, fs) + 16;
        s += `<rect x="${cx - lw / 2}" y="${y - 12}" width="${lw}" height="24" rx="4" fill="#fff"/>`;
        s += text(cx, y + 5, n.label, fs, { bold: true, fill: stroke });
      }
      break;
    }
    case 'pkg': {
      s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<rect x="${x}" y="${y - 16}" width="${Math.min(w * 0.5, tw(n.label, fs) + 26)}" height="16" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += text(x + 8, y - 4, n.label, fs, { anchor: 'start', bold: true });
      break;
    }
    case 'proc':
    case 'term':
    case 'state': {
      const rx = n.t === 'proc' ? 8 : h / 2;
      s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, cy, wrap(n.label, w - 20, fs), fs);
      break;
    }
    case 'rect': {
      s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, cy, wrap(n.label, w - 18, fs), fs);
      break;
    }
    case 'dec': {
      s += `<polygon points="${cx},${y} ${x + w},${cy} ${cx},${y + h} ${x},${cy}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, cy, wrap(n.label, w * 0.62, fs), fs);
      break;
    }
    case 'circle': {
      s += `<circle cx="${cx}" cy="${cy}" r="${w / 2}" fill="${n.fill || '#2f855a'}" stroke="${stroke}" stroke-width="${sw}"/>`;
      if (n.label) s += text(cx, cy + 4, n.label, fs, { fill: '#fff', bold: true });
      break;
    }
    case 'db': {
      const ry = 9;
      s += `<path d="M${x},${y + ry} A${w / 2},${ry} 0 0 1 ${x + w},${y + ry} L${x + w},${y + h - ry} A${w / 2},${ry} 0 0 1 ${x},${y + h - ry} Z" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<path d="M${x},${y + ry} A${w / 2},${ry} 0 0 0 ${x + w},${y + ry}" fill="none" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, cy + ry / 2, wrap(n.label, w - 16, fs), fs);
      break;
    }
    case 'cloud': {
      const r = h / 2.6;
      s += `<path d="M${x + w * 0.22},${y + h - r * 0.4}
        a${r},${r} 0 0 1 ${r * 0.35},${-r * 1.5}
        a${r * 1.05},${r * 1.05} 0 0 1 ${r * 1.7},${-r * 0.5}
        a${r},${r} 0 0 1 ${r * 1.25},${r * 0.75}
        a${r * 0.85},${r * 0.85} 0 0 1 ${-r * 0.25},${r * 1.15}
        Z" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += block(cx, cy, wrap(n.label, w - 24, fs), fs);
      break;
    }
    case 'note': {
      const c = 14;
      s += `<path d="M${x},${y} L${x + w - c},${y} L${x + w},${y + c} L${x + w},${y + h} L${x},${y + h} Z" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
      s += `<path d="M${x + w - c},${y} L${x + w - c},${y + c} L${x + w},${y + c}" fill="none" stroke="${stroke}" stroke-width="1.2"/>`;
      s += block(cx - 2, cy, wrap(n.label, w - 18, fs), n.fs || 13, { fill: '#744210' });
      break;
    }
    case 'entity': {
      const rows = n.rows || [];
      const rh = n.rh || 17, hh = n.hh || 26;
      if (!rows.length) {
        s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="6" fill="${n.hfill || '#eef4ff'}" stroke="${stroke}" stroke-width="1.5"/>`;
        s += block(cx, cy, wrap(n.label, w - 20, 15), 15, { bold: true, fill: '#1a365d' });
        break;
      }
      s += `<rect x="${x}" y="${y}" width="${w}" height="${hh}" fill="${n.hfill || '#2b6cb0'}"/>`;
      s += text(cx, y + hh - 8, n.label, n.fs || 15, { fill: '#fff', bold: true });
      s += `<rect x="${x}" y="${y + hh}" width="${w}" height="${h - hh}" fill="#fff" stroke="${stroke}" stroke-width="1.2"/>`;
      s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="none" stroke="${stroke}" stroke-width="1.4"/>`;
      rows.forEach((r, i) => {
        const yy = y + hh + (i + 1) * rh;
        if (i) s += `<line x1="${x}" y1="${yy - rh}" x2="${x + w}" y2="${yy - rh}" stroke="#e2e8f0" stroke-width="1"/>`;
        const txt = String(r);
        const key = txt.endsWith('*');
        const body = key ? txt.slice(0, -1) : txt;
        s += text(x + 7, yy - 4.5, body, n.fs || 12.5, { anchor: 'start', bold: key, fill: key ? '#2b6cb0' : '#2d3748' });
      });
      break;
    }
    case 'cls': {
      const attrs = n.attrs || [], ms = n.methods || [];
      const rh = 16, hh = 24;
      const h2 = attrs.length * rh, h3 = ms.length * rh;
      s += `<rect x="${x}" y="${y}" width="${w}" height="${hh}" fill="${n.hfill || '#553c9a'}"/>`;
      s += text(cx, y + hh - 7, n.label, 14.5, { fill: '#fff', bold: true });
      s += `<rect x="${x}" y="${y + hh}" width="${w}" height="${h2}" fill="#fff"/>`;
      attrs.forEach((a, i) => { s += text(x + 7, y + hh + (i + 1) * rh - 4.5, a, 12, { anchor: 'start', fill: '#2d3748' }); });
      s += `<rect x="${x}" y="${y + hh + h2}" width="${w}" height="${h3}" fill="#fbfbfd"/>`;
      ms.forEach((m, i) => { s += text(x + 7, y + hh + h2 + (i + 1) * rh - 4.5, m, 12, { anchor: 'start', fill: '#4a5568' }); });
      s += `<rect x="${x}" y="${y}" width="${w}" height="${hh + h2 + h3}" fill="none" stroke="${stroke}" stroke-width="1.4"/>`;
      s += `<line x1="${x}" y1="${y + hh}" x2="${x + w}" y2="${y + hh}" stroke="${stroke}" stroke-width="1.2"/>`;
      s += `<line x1="${x}" y1="${y + hh + h2}" x2="${x + w}" y2="${y + hh + h2}" stroke="${stroke}" stroke-width="1.2"/>`;
      break;
    }
    default:
      s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" stroke="${stroke}"/>`;
  }
  if (n.badge) {
    s += `<circle cx="${x + w - 6}" cy="${y + 6}" r="10" fill="#c53030"/>`;
    s += text(x + w - 6, y + 10, n.badge, 11, { fill: '#fff', bold: true });
  }
  return s;
}

function anchorPoint(n, tx, ty) {
  const cx = n.x + n.w / 2, cy = n.y + n.h / 2;
  let dx = tx - cx, dy = ty - cy;
  if (dx === 0 && dy === 0) return [cx, cy];
  if (n.t === 'uc' || n.t === 'circle') {
    const rx = n.w / 2, ry = n.h / 2;
    const k = 1 / Math.sqrt((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry));
    return [cx + dx * k, cy + dy * k];
  }
  const hw = n.w / 2, hh = n.h / 2;
  const sx = hw / Math.abs(dx || 1e-6), sy = hh / Math.abs(dy || 1e-6);
  const k = Math.min(sx, sy);
  return [cx + dx * k, cy + dy * k];
}

function edge(nodes, e) {
  const a = nodes[e.f], b = nodes[e.to];
  if (!a || !b) throw new Error('bad edge ' + JSON.stringify(e));
  let x1, y1, x2, y2, pts = [];
  if (e.pts) {
    pts = e.pts.slice();
    const first = pts[0], last = pts[pts.length - 1];
    const p1 = anchorPoint(a, first[0], first[1]);
    const p2 = anchorPoint(b, last[0], last[1]);
    pts = [p1].concat(pts, [p2]);
  } else {
    const ac = [a.x + a.w / 2, a.y + a.h / 2], bc = [b.x + b.w / 2, b.y + b.h / 2];
    const p1 = anchorPoint(a, bc[0], bc[1]);
    const p2 = anchorPoint(b, ac[0], ac[1]);
    pts = [p1, p2];
  }
  const d = pts.map((p, i) => (i ? 'L' : 'M') + p[0].toFixed(1) + ',' + p[1].toFixed(1)).join(' ');
  const dash = (e.k === 'dash' || e.k === 'return' || e.dash) ? ' stroke-dasharray="6 4"' : '';
  const col = e.color || '#4a5568';
  let marker = '';
  if (e.k === 'arrow') marker = ' marker-end="url(#arrow)"';
  else if (e.k === 'open' || e.k === 'return') marker = ' marker-end="url(#open)"';
  else if (e.k === 'inherit') marker = ' marker-end="url(#hollow)"';
  else if (e.k === 'both') marker = ' marker-end="url(#arrow)" marker-start="url(#arrowStart)"';
  let s = `<path d="${d}" fill="none" stroke="${col}" stroke-width="${e.sw || 1.6}"${dash}${marker}/>`;
  const mid = pts[Math.floor(pts.length / 2)];
  const a1 = pts[Math.max(0, Math.floor(pts.length / 2) - 1)];
  const midX = (a1[0] + mid[0]) / 2, midY = (a1[1] + mid[1]) / 2;
  if (e.l) {
    const lines = wrap(e.l, e.lw || 150, 12.5);
    const wmax = Math.max(...lines.map(l => tw(l, 12.5)));
    const bh = lines.length * 17 + 4;
    s += `<rect x="${midX - wmax / 2 - 4}" y="${midY - bh / 2}" width="${wmax + 8}" height="${bh}" fill="#fff" opacity="0.93"/>`;
    s += block(midX, midY, lines, 12.5, { fill: '#2d3748' });
  }
  const p0 = pts[0], pn = pts[pts.length - 1];
  if (e.lf) s += text(p0[0] + (p0[0] <= pn[0] ? 12 : -12), p0[1] - 8, e.lf, 12.5, { fill: '#c05621', bold: true });
  if (e.lt) s += text(pn[0] + (pn[0] <= p0[0] ? 12 : -12), pn[1] - 8, e.lt, 12.5, { fill: '#c05621', bold: true });
  return s;
}

function seqPart(sp) {
  let s = '';
  const top = 46;
  for (const l of sp.lifelines) {
    const w = l.w || 170, x = l.x - w / 2;
    s += `<rect x="${x}" y="${sp.top || top}" width="${w}" height="34" rx="4" fill="${l.fill || '#eef4ff'}" stroke="${l.stroke || '#2b6cb0'}" stroke-width="1.5"/>`;
    s += block(l.x, (sp.top || top) + 17, wrap(l.label, w - 12, 13.5), 13.5, { bold: !!l.bold });
    s += `<line x1="${l.x}" y1="${(sp.top || top) + 34}" x2="${l.x}" y2="${sp.bottom || 600}" stroke="#a0aec0" stroke-width="1.4" stroke-dasharray="6 5"/>`;
  }
  for (const m of sp.msgs) {
    const a = sp.lifelines.find(l => l.id === m.f), b = sp.lifelines.find(l => l.id === m.to);
    if (m.self || a === b) {
      const x = a.x; const y = m.y;
      s += `<path d="M${x},${y} h54 v26 h-54" fill="none" stroke="#4a5568" stroke-width="1.5" marker-end="url(#arrow)"/>`;
      s += text(x + 62, y + 16, m.l, 12.5, { anchor: 'start' });
      continue;
    }
    const dir = b.x > a.x ? 1 : -1;
    const k = m.ret ? 'return' : (m.k || 'arrow');
    const ax = a.x + dir * 3, bx = b.x - dir * 3;
    s += `<path d="M${ax},${m.y} L${bx},${m.y}" fill="none" stroke="#4a5568" stroke-width="1.5"${k === 'return' ? ' stroke-dasharray="6 4" marker-end="url(#open)"' : ' marker-end="url(#arrow)"'}/>`;
    s += text((a.x + b.x) / 2, m.y - 7, m.l, 12.5, { fill: '#2d3748' });
    if (m.note) s += text((a.x + b.x) / 2, m.y + 15, m.note, 11.5, { fill: '#718096' });
  }
  return s;
}

function render(spec) {
  const W = spec.w, H = spec.h;
  const map = {};
  (spec.nodes || []).forEach(n => {
    if (n.t === 'cls' && !n.h) n.h = 24 + ((n.attrs || []).length + (n.methods || []).length) * 16;
    if (n.t === 'entity' && (n.rows || []).length && !n.h) n.h = (n.hh || 26) + n.rows.length * (n.rh || 17);
    if (n.t === 'note' && !n.h) n.h = 60;
    if (n.t === 'db' && !n.h) n.h = 100;
    if (!n.h && (n.t === 'rect' || n.t === 'proc' || n.t === 'term' || n.t === 'state')) n.h = 56;
    if (n.t === 'actor' && !n.h) n.h = 100;
    map[n.id] = n;
  });
  let defs = `<defs>
    <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M0,0 L10,5 L0,10 z" fill="#4a5568"/></marker>
    <marker id="arrowStart" viewBox="0 0 10 10" refX="1" refY="5" markerWidth="7" markerHeight="7" orient="auto">
      <path d="M10,0 L0,5 L10,10 z" fill="#4a5568"/></marker>
    <marker id="open" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
      <path d="M0,0 L11,6 L0,12" fill="none" stroke="#4a5568" stroke-width="1.6"/></marker>
    <marker id="hollow" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="11" markerHeight="11" orient="auto-start-reverse">
      <path d="M0,0 L12,6 L0,12 z" fill="#fff" stroke="#4a5568" stroke-width="1.4"/></marker>
  </defs>`;
  let body = '';
  if (spec.groups) {
    for (const g of spec.groups) {
      body += `<rect x="${g.x}" y="${g.y}" width="${g.w}" height="${g.h}" rx="8" fill="${g.fill || '#fafbfc'}" stroke="${g.stroke || '#cbd5e0'}" stroke-width="1.4"${g.dash ? ' stroke-dasharray="6 5"' : ''}/>`;
      if (g.label) body += text(g.x + g.w / 2, g.y + 22, g.label, g.fs || 14, { bold: true, fill: g.stroke || '#4a5568' });
    }
  }
  (spec.edges || []).forEach(e => { body += edge(map, e) + '\n'; });
  (spec.nodes || []).forEach(n => { body += node(map, n) + '\n'; });
  (spec.seqs || []).forEach(sp => { body += seqPart(sp) + '\n'; });
  if (spec.caption) body += text(W / 2, H - 8, spec.caption, 12.5, { fill: '#718096' });
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${defs}
<rect width="${W}" height="${H}" fill="#ffffff"/>${body}</svg>`;
}

// ---------- drawio ----------
function dStyle(n) {
  const pal = P[n.pal || 'blue'];
  const base = `fillColor=${n.fill || pal[0]};strokeColor=${n.stroke || pal[1]};strokeWidth=${n.sw || 1.6};`;
  switch (n.t) {
    case 'actor': return 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;' + base;
    case 'uc': return 'ellipse;whiteSpace=wrap;html=1;' + base;
    case 'boundary': return 'rounded=1;dashed=1;verticalAlign=top;html=1;fillColor=none;strokeColor=' + (n.stroke || '#4a5568') + ';';
    case 'proc': return 'rounded=1;arcSize=12;whiteSpace=wrap;html=1;' + base;
    case 'term': return 'rounded=1;arcSize=50;whiteSpace=wrap;html=1;' + base;
    case 'state': return 'rounded=1;arcSize=30;whiteSpace=wrap;html=1;' + base;
    case 'rect': return 'whiteSpace=wrap;html=1;' + base;
    case 'dec': return 'rhombus;whiteSpace=wrap;html=1;' + base;
    case 'circle': return 'ellipse;whiteSpace=wrap;html=1;' + base;
    case 'db': return 'shape=cylinder3;boundedLbl=1;backgroundOutline=1;size=10;html=1;' + base;
    case 'cloud': return 'ellipse;shape=cloud;whiteSpace=wrap;html=1;' + base;
    case 'note': return 'shape=note;size=14;whiteSpace=wrap;html=1;' + base;
    case 'pkg': return 'shape=folder;tabWidth=90;html=1;' + base;
    case 'entity': return 'swimlane;startSize=26;html=1;fillColor=#2b6cb0;fontColor=#ffffff;strokeColor=#2b6cb0;';
    case 'cls': return 'swimlane;startSize=24;html=1;fillColor=#553c9a;fontColor=#ffffff;strokeColor=#553c9a;';
    default: return 'whiteSpace=wrap;html=1;' + base;
  }
}
function drawio(spec) {
  let cells = '';
  let id = 2;
  const mapId = {};
  (spec.nodes || []).forEach(n => {
    mapId[n.id] = 'n' + (id++);
    let v = n.label;
    if (n.t === 'entity') v = n.label + '\n' + (n.rows || []).join('\n');
    if (n.t === 'cls') v = n.label + '\n' + (n.attrs || []).join('\n') + '\n' + (n.methods || []).join('\n');
    let h = n.h;
    if (n.t === 'entity') h = (n.hh || 26) + (n.rows || []).length * (n.rh || 17);
    if (n.t === 'cls') h = 24 + ((n.attrs || []).length + (n.methods || []).length) * 16;
    cells += `<mxCell id="${mapId[n.id]}" value="${esc(v)}" style="${dStyle(n)}" vertex="1" parent="1"><mxGeometry x="${n.x}" y="${n.y}" width="${n.w}" height="${h}" as="geometry"/></mxCell>`;
  });
  (spec.edges || []).forEach(e => {
    const st = (e.k === 'dash' || e.k === 'return') ? 'endArrow=open;dashed=1;html=1;' : (e.k === 'open' ? 'endArrow=open;html=1;' : (e.k === 'inherit' ? 'endArrow=block;endFill=0;html=1;' : 'endArrow=block;html=1;'));
    cells += `<mxCell id="e${id++}" value="${esc(e.l || '')}" style="${st}edgeStyle=orthogonalEdgeStyle;rounded=0;strokeColor=#4a5568;" edge="1" parent="1" source="${mapId[e.f]}" target="${mapId[e.to]}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
  });
  if (spec.seqs) {
    spec.seqs.forEach(sp => {
      sp.lifelines.forEach(l => {
        mapId[l.id] = 'l' + (id++);
        const w = l.w || 170;
        cells += `<mxCell id="${mapId[l.id]}" value="${esc(l.label)}" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;rounded=0;size=34;" vertex="1" parent="1"><mxGeometry x="${l.x - w / 2}" y="${sp.top || 46}" width="${w}" height="${(sp.bottom || 600) - (sp.top || 46)}" as="geometry"/></mxCell>`;
      });
      sp.msgs.forEach(m => {
        const a = sp.lifelines.find(l => l.id === m.f), b = sp.lifelines.find(l => l.id === m.to);
        const st = m.ret ? 'endArrow=open;dashed=1;html=1;' : 'endArrow=block;html=1;';
        cells += `<mxCell id="m${id++}" value="${esc(m.l)}" style="${st}html=1;" edge="1" parent="1" source="${mapId[m.f]}" target="${mapId[m.to]}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
        void a; void b;
      });
    });
  }
  return `<mxfile host="app.diagrams.net"><diagram name="${esc(spec.title || 'diagram')}"><mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1169" pageHeight="826" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells}</root></mxGraphModel></diagram></mxfile>`;
}

// ---------- main ----------
const [specPath, svgPath, drawioPath] = process.argv.slice(2);
const spec = JSON.parse(fs.readFileSync(specPath, 'utf8'));
fs.writeFileSync(svgPath, render(spec), 'utf8');
if (drawioPath) fs.writeFileSync(drawioPath, drawio(spec), 'utf8');
console.log('OK ' + specPath + ' -> ' + svgPath);
