#!/usr/bin/env node
/**
 * 验 key：对 room.config.json 里每一家做一次最小调用，报告 可用/不可用/模型名对不对。
 * 用法：node check.mjs
 * 只读 keys.json，不回显 key 内容。
 */
import { readFileSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CFG = JSON.parse(readFileSync(path.join(__dirname, 'room.config.json'), 'utf8'));
const KEYS_PATH = path.join(__dirname, 'keys.json');
let KEYS = {};
if (existsSync(KEYS_PATH)) {
  try { KEYS = JSON.parse(readFileSync(KEYS_PATH, 'utf8')); }
  catch (e) { console.error('keys.json 解析失败：', e.message); process.exit(1); }
}

const mask = (k) => (k ? `${k.slice(0, 6)}…${k.slice(-4)}（${k.length} 位）` : '未配置');
const keyOf = (p) => (KEYS[p.id] || (p.keyEnv ? process.env[p.keyEnv] : '') || '').trim();

async function checkOne(p) {
  const key = keyOf(p);
  if (!key) return { id: p.id, status: 'SKIP', msg: '没有 key' };
  const t0 = Date.now();
  try {
    const res = await fetch(p.baseUrl.replace(/\/+$/, '') + '/chat/completions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
      body: JSON.stringify({
        model: p.model,
        messages: [{ role: 'user', content: '回答一个字：好' }],
        max_tokens: 8,
        stream: false,
      }),
      signal: AbortSignal.timeout(25000),
    });
    const ms = Date.now() - t0;
    if (!res.ok) {
      const txt = (await res.text().catch(() => '')).slice(0, 220).replace(/\s+/g, ' ');
      return { id: p.id, status: 'FAIL', msg: `HTTP ${res.status} ${txt}`, ms };
    }
    const j = await res.json().catch(() => ({}));
    const said = j.choices?.[0]?.message?.content ?? '';
    return { id: p.id, status: 'OK', msg: `模型=${j.model || p.model} 回复「${String(said).trim().slice(0, 12)}」`, ms };
  } catch (e) {
    return { id: p.id, status: 'FAIL', msg: String(e.message || e).slice(0, 220), ms: Date.now() - t0 };
  }
}

console.log('\n=== 验 key ===');
console.log('配置文件：' + path.join(__dirname, 'room.config.json'));
console.log('key 文件：' + (existsSync(KEYS_PATH) ? KEYS_PATH : '（无，可只用环境变量）') + '\n');

const results = [];
for (const p of CFG.providers) {
  process.stdout.write(`· ${p.label.padEnd(18, ' ')} ${mask(keyOf(p)).padEnd(28, ' ')} `);
  const r = await checkOne(p);
  results.push(r);
  const icon = r.status === 'OK' ? '✅' : r.status === 'SKIP' ? '⏭ ' : '❌';
  console.log(`${icon} ${r.status}${r.ms ? ' ' + r.ms + 'ms' : ''}`);
  if (r.status !== 'OK' && r.status !== 'SKIP') console.log(`     ↳ ${r.msg}`);
}

const ok = results.filter((r) => r.status === 'OK');
console.log(`\n结论：${ok.length} 家可用 / ${CFG.providers.length} 家配置`);
if (ok.length === 0) {
  console.log('提示：页面仍能跑（Mock 模式），只是输出是占位内容。');
} else {
  const roles = CFG.roles.filter((r) => ok.some((o) => o.id === r.provider)).length;
  console.log(`可覆盖视角：${roles} / ${CFG.roles.length}（其余会自动降级为 Mock 输出）`);
}
console.log('');
