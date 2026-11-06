#!/usr/bin/env node
/**
 * AI 群聊工作台（零依赖）
 * 用途：多个国内大模型以不同视角并行为 v3.0 做头脑风暴，浏览器实时看它们干活。
 * 启动：node server.mjs   然后打开 http://127.0.0.1:8099
 * 协议：R1 独立发散 → R2 匿名交叉 → R3 红队 → R4 收敛裁决
 */
import http from 'node:http';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.AI_ROOM_PORT || 8099);
const CONFIG_PATH = path.join(__dirname, 'room.config.json');
const KEYS_PATH = path.join(__dirname, 'keys.json');
const OUT_DIR = path.resolve(__dirname, '../../eval');
const PUBLIC_DIR = path.join(__dirname, 'public');

/* ---------------------------------------------------------------- 配置读取 */
let CONFIG = {
  providers: [],
  roles: [],
  redTeam: [],
  judge: null,
  brief: '',
};
try {
  CONFIG = JSON.parse(await readFile(CONFIG_PATH, 'utf8'));
} catch (e) {
  console.error('[FATAL] 读不到 room.config.json：', e.message);
  process.exit(1);
}

let KEYS = {};
function loadKeys() {
  try { KEYS = existsSync(KEYS_PATH) ? JSON.parse(readFileSync(KEYS_PATH, 'utf8')) : {}; }
  catch (e) { KEYS = {}; console.error('[WARN] keys.json 解析失败：', e.message); }
}
loadKeys();

function keyOf(providerId) {
  const p = CONFIG.providers.find((x) => x.id === providerId) || {};
  const fromFile = KEYS[providerId] || '';
  const fromEnv = p.keyEnv ? process.env[p.keyEnv] || '' : '';
  return (fromFile || fromEnv || '').trim();
}
function providerInfo(id) {
  const p = CONFIG.providers.find((x) => x.id === id) || {};
  return { id, label: p.label || id, baseUrl: p.baseUrl || '', model: p.model || '', ready: Boolean(keyOf(id)) };
}

/* ---------------------------------------------------------------- 并发闸门 */
class Semaphore {
  constructor(n) { this.n = n; this.q = []; }
  async acquire() {
    if (this.n > 0) { this.n -= 1; return; }
    await new Promise((r) => this.q.push(r));
  }
  release() {
    this.n += 1;
    const next = this.q.shift();
    if (next) { this.n -= 1; next(); }
  }
}
const providerSem = new Map();
function semFor(providerId) {
  const p = CONFIG.providers.find((x) => x.id === providerId) || {};
  const limit = Number(p.concurrency || CONFIG.concurrency || 2);
  if (!providerSem.has(providerId)) providerSem.set(providerId, new Semaphore(limit));
  return providerSem.get(providerId);
}

/* ---------------------------------------------------------------- 运行态 */
const runs = new Map();

function createRun(opts) {
  const runId = crypto.randomBytes(6).toString('hex');
  const run = {
    id: runId,
    opts,
    status: 'running',
    createdAt: Date.now(),
    events: [],            // 供后连上的客户端补发
    clients: new Set(),    // SSE res
    transcript: [],        // { phase, phaseTitle, items:[{roleId,roleName,provider,text,think,ms,chars,mock,error}] }
    finalText: '',
    aborter: new AbortController(),
  };
  runs.set(runId, run);
  // 30 分钟后清理
  setTimeout(() => runs.delete(runId), 30 * 60 * 1000).unref?.();
  return run;
}

function emit(run, type, data = {}) {
  const ev = { type, t: Date.now(), ...data };
  run.events.push(ev);
  if (run.events.length > 5000) run.events.splice(0, 1000);
  const payload = `event: ${type}\ndata: ${JSON.stringify(ev)}\n\n`;
  for (const res of run.clients) {
    try { res.write(payload); } catch { run.clients.delete(res); }
  }
}

/* ---------------------------------------------------------------- 模型调用 */
async function callModel({ providerId, model, system, user, temperature = 0.7, maxTokens = 2000 }, onText, onThink, signal) {
  const p = CONFIG.providers.find((x) => x.id === providerId) || {};
  const key = keyOf(providerId);
  if (!key) throw new Error(`NO_KEY:${providerId}`);
  const url = (p.baseUrl || '').replace(/\/+$/, '') + '/chat/completions';
  // 有些模型（如 kimi-k3）只接受 temperature=1；provider 里配了 temperature 就以它为准
  const temp = p.temperature !== undefined && p.temperature !== null ? p.temperature : temperature;
  const body = {
    model: model || p.model,
    messages: [
      ...(system ? [{ role: 'system', content: system }] : []),
      { role: 'user', content: user },
    ],
    temperature: temp,
    max_tokens: Math.max(maxTokens, Number(p.maxTokens || 0)),
    stream: true,
    // provider 级额外请求字段（如 kimi 的 thinking:{type:"disabled"}、qwen 的 enable_thinking 等）
    ...(p.extraBody || {}),
  };

  const sem = semFor(providerId);
  await sem.acquire();
  try {
    const send = (bodyOverride) => fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
      body: JSON.stringify(bodyOverride || body),
      signal,
    });
    let res = await send();
    if (!res.ok) {
      const txt = await res.text().catch(() => '');
      // 自适应兜底：部分模型只接受固定 temperature（报错里会写明数值），读出它重试一次
      const m = /only\s+([0-9]*\.?[0-9]+)\s+is\s+allowed/i.exec(txt);
      if (res.status === 400 && m && Number(m[1]) !== body.temperature) {
        res = await send({ ...body, temperature: Number(m[1]) });
      }
      if (!res.ok) {
        const txt2 = res.bodyUsed ? txt : await res.text().catch(() => txt);
        throw new Error(`HTTP ${res.status} ${String(txt2).slice(0, 300)}`);
      }
    }
    if (!res.body) throw new Error('HTTP 空响应体');
    const reader = res.body.getReader();
    const dec = new TextDecoder('utf-8');
    let buf = '';
    let full = '';
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buf += dec.decode(value, { stream: true });
      let idx;
      while ((idx = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, idx).trim();
        buf = buf.slice(idx + 1);
        if (!line || !line.startsWith('data:')) continue;
        const data = line.slice(5).trim();
        if (data === '[DONE]') return full;
        let j;
        try { j = JSON.parse(data); } catch { continue; }
        const d = j.choices?.[0]?.delta || {};
        const think = d.reasoning_content || d.reasoning || '';
        const text = d.content || '';
        if (think) onThink && onThink(think);
        if (text) { full += text; onText && onText(text); }
      }
    }
    return full;
  } finally {
    sem.release();
  }
}

/* ---------------------------------------------------------------- Mock 流 */
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
async function mockStream(text, onText, signal) {
  const chunks = text.match(/[\s\S]{1,6}/g) || [];
  for (const c of chunks) {
    if (signal?.aborted) throw new Error('STOPPED');
    onText && onText(c);
    await sleep(8);
  }
  return text;
}

/* ---------------------------------------------------------------- 提示词 */
function head(run) {
  return `# 背景 Brief（务必读完再作答）\n${run.opts.brief || CONFIG.brief || ''}\n`;
}

function r1Task(role) {
  return `${head(role.__run)}
【你的角色】${role.name} —— ${role.description || ''}
【本轮任务：独立发散（你暂时看不到别人的答案，这是刻意的）】
请从你这个视角出发，列出这份 v3.0 规划最该补的 6–8 条「缺口 / 机会」。每条严格一行，格式：
N. 【缺口】… ｜【补法】… ｜【人日】… ｜【动已定决策】是/否 ｜【答辩价值】高/中/低
硬性要求：
1) 只写你这个视角能看到、别的视角看不到的；
2) 不要复述 Brief 里已有的内容；
3) 不写「建议引入大模型/微调/上云/知识图谱/做平台」这类没有落地细节的空话；
4) 人日取 0.25 的倍数且 ≤3；说不清人日就写「>3」并说明原因；
5) 最后不要总结，不要客套。`;
}

function r2Task(role, corpus) {
  return `${head(role.__run)}
【你的角色】${role.name} —— ${role.description || ''}
【本轮任务：匿名交叉评审】
下面是其他视角上一轮产出的清单（已匿名打乱，编号不代表作者）。你只做两件事：
A. 指出其中在你视角下明显站不住的 3 条：写「编号 + 一句理由」；
B. 补充 3 条你视角独有、下面清单里完全没有的（沿用 R1 的六字段格式）。
禁止：附和、复述原文、评价清单整体质量、写客套话。
（清单开始）
${corpus}
（清单结束）`;
}

function r3Task(agent, corpus) {
  return `${head(agent.__run)}
【你的角色】${agent.name} —— ${agent.description || ''}
【本轮任务：红队攻击（只许挑刺）】
已知其他视角的清单与增补：
${corpus}
请给出 10 个「会在答辩现场把这份 v3.0 问死」的问题，按致命度从高到低排。每条一行：
N. 【问题】… ｜【为什么致命】… ｜【一句话怎么补】… ｜【致命度】高/中/低
要求：不给任何优点、不缓和语气、不写「总体不错」；能落到具体数字/场景的优先。`;
}

function r4Task(judge, corpus) {
  return `${head(judge.__run)}
【你的角色】${judge.name} —— ${judge.description || ''}
【本轮任务：收敛裁决】
输入是 10 个视角的 R1 清单、R2 匿名增补、R3 红队 10 问（见下）。
请去重合并成一张 markdown 表，按「答辩价值 × 可落地性」从高到低排序，最多 25 行：
| 编号 | 缺口/机会 | 补法 | 人日 | 是否动已定决策 | 答辩价值 | 争议 |
规则：
1) 合并同类项，同一件事只留一行；
2) 与 Brief 里「不可重议」的决策冲突的，在【争议】列写明冲突点，不要擅自删；
3) 人日沿用原值，取区间取上限；
4) 表格之后另起一节「## 三个最该先做的」，3 条，各一句理由；
5) 除表格与该节外不要输出别的。
（输入开始）
${corpus}
（输入结束）`;
}

/* ---------------------------------------------------------------- 编排 */
async function runOrchestration(run, state) {
  const { roleIds, phases } = run.opts;
  const roles = CONFIG.roles.filter((r) => roleIds.includes(r.id));
  const mockAll = Boolean(run.opts.mock);
  const sig = run.aborter.signal;

  const byPhase = new Map();
  const newPhase = (id, title, total) => {
    const p = { phase: id, phaseTitle: title, items: [] };
    run.transcript.push(p);
    byPhase.set(id, p);
    emit(run, 'phase', { phase: id, title, total });
    return p;
  };

  const call = async ({ phase, who, prompt, temperature, maxTokens }) => {
    const item = {
      roleId: who.id, roleName: who.name, provider: who.provider, model: who.model,
      text: '', think: '', ms: 0, chars: 0, mock: false, error: '',
    };
    const t0 = Date.now();
    const willMock = mockAll || !keyOf(who.provider);
    item.mock = willMock;
    emit(run, 'role_start', {
      phase, roleId: who.id, roleName: who.name, provider: who.provider,
      model: who.model, mock: willMock,
    });
    try {
      if (willMock) {
        const fake = mockAnswerFor(who, phase);
        await mockStream(fake, (t) => { item.text += t; emit(run, 'delta', { phase, roleId: who.id, text: t }); }, sig);
      } else {
        const onText = (t) => { item.text += t; emit(run, 'delta', { phase, roleId: who.id, text: t }); };
        const onThink = (t) => { item.think += t; emit(run, 'think', { phase, roleId: who.id, text: t }); };
        await callModel(
          { providerId: who.provider, model: who.model, system: who.system, user: prompt, temperature, maxTokens },
          onText, onThink, sig,
        );
        // 思考型模型（kimi-k3 等）可能把输出预算全用在 reasoning 上、正文为空 → 用思考内容兜底，不再重试（太慢）
        if (!item.text.trim() && item.think.trim()) {
          const fallback = item.think.trim();
          item.text = `（该模型把预算用尽在思考上，以下为思考内容兜底）\n${fallback}`;
          item.error = '正文为空·已用思考内容兜底';
          emit(run, 'delta', { phase, roleId: who.id, text: item.text });
          emit(run, 'log', { message: `${who.name}：正文为空，已用思考内容兜底` });
        }
      }
    } catch (e) {
      if (sig.aborted || String(e.message).includes('STOPPED')) {
        item.error = '已停止';
      } else {
        item.error = String(e.message || e).slice(0, 400);
        // 单角色失败不终止全局，降级为占位文本
        item.text = item.text || `（本角色调用失败：${item.error}）`;
      }
      emit(run, 'role_error', { phase, roleId: who.id, message: item.error });
    }
    item.ms = Date.now() - t0;
    item.chars = item.text.length;
    byPhase.get(phase)?.items.push(item);
    emit(run, 'role_done', { phase, roleId: who.id, ms: item.ms, chars: item.chars, mock: item.mock, error: item.error });
    return item;
  };

  try {
    // ---------- R1 独立发散（并行、互相不可见）
    if (phases.r1 !== false) {
      newPhase('R1', 'R1 独立发散', roles.length);
      emit(run, 'log', { message: `R1 开始：${roles.length} 个视角并行作答（互相看不到）` });
      await Promise.all(roles.map((role) => {
        role.__run = run;
        return call({ phase: 'R1', who: role, prompt: r1Task(role), temperature: 0.8, maxTokens: 1600 });
      }));
      emit(run, 'phase_done', { phase: 'R1' });
      if (sig.aborted) throw new Error('STOPPED');
    }

    const r1Items = (run.transcript.find((p) => p.phase === 'R1')?.items) || [];
    const corpus = anonymize(r1Items);

    // ---------- R2 匿名交叉评审
    if (phases.r2 !== false) {
      newPhase('R2', 'R2 匿名交叉评审', roles.length);
      emit(run, 'log', { message: 'R2 开始：清单已匿名打乱，各视角只许挑刺与补漏' });
      await Promise.all(roles.map((role) => {
        role.__run = run;
        return call({ phase: 'R2', who: role, prompt: r2Task(role, corpus), temperature: 0.6, maxTokens: 1400 });
      }));
      emit(run, 'phase_done', { phase: 'R2' });
      if (sig.aborted) throw new Error('STOPPED');
    }

    const r2Text = (run.transcript.find((p) => p.phase === 'R2')?.items || [])
      .map((i) => `—— ${i.roleName} ——\n${trim(i.text, 1600)}`).join('\n\n');

    // ---------- R3 红队
    if (phases.r3 !== false) {
      const reds = (CONFIG.redTeam || []).filter((r) => r.enabled !== false);
      newPhase('R3', 'R3 红队攻击', reds.length);
      emit(run, 'log', { message: `R3 开始：${reds.length} 个红队只负责把这个方案问死` });
      await Promise.all(reds.map((agent) => {
        agent.__run = run;
        return call({
          phase: 'R3', who: agent,
          prompt: r3Task(agent, `${corpus}\n\n（R2 增补）\n${trim(r2Text, 4000)}`),
          temperature: 0.5, maxTokens: 1600,
        });
      }));
      emit(run, 'phase_done', { phase: 'R3' });
      if (sig.aborted) throw new Error('STOPPED');
    }

    // ---------- R4 收敛裁决
    if (phases.r4 !== false) {
      const judge = CONFIG.judge || { id: 'judge', name: '裁决者', provider: CONFIG.providers[0]?.id, system: '你是严格的方案裁决者。' };
      judge.__run = run;
      const r3Text = (run.transcript.find((p) => p.phase === 'R3')?.items || [])
        .map((i) => `—— ${i.roleName} ——\n${trim(i.text, 2500)}`).join('\n\n');
      newPhase('R4', 'R4 收敛裁决', 1);
      emit(run, 'log', { message: 'R4 开始：去重合并成一张可勾选总表' });
      const item = await call({
        phase: 'R4', who: { ...judge, provider: judge.provider || CONFIG.providers[0]?.id },
        prompt: r4Task(judge, `${corpus}\n\n（R2 增补）\n${trim(r2Text, 4000)}\n\n（R3 红队）\n${r3Text}`),
        temperature: 0.3, maxTokens: 2600,
      });
      run.finalText = item.text;
      emit(run, 'final', { text: item.text });
      emit(run, 'phase_done', { phase: 'R4' });
    }

    run.status = 'done';
    emit(run, 'run_done', { status: 'done', message: '全部阶段完成' });
  } catch (e) {
    const stopped = sig.aborted || String(e.message).includes('STOPPED');
    run.status = stopped ? 'stopped' : 'error';
    emit(run, 'run_done', { status: run.status, message: stopped ? '已停止' : String(e.message || e) });
  }
}

function trim(s, n) { s = s || ''; return s.length > n ? s.slice(0, n) + '…' : s; }

function anonymize(items) {
  const lines = [];
  for (const it of items) {
    for (const ln of (it.text || '').split('\n')) {
      const t = ln.trim();
      if (!t) continue;
      if (/^\d+[.、)]/.test(t) || /^【/.test(t)) lines.push(t.replace(/^\d+[.、)]\s*/, ''));
    }
  }
  // 稳定打乱（同一 run 内可复现）
  let seed = 20261103;
  const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
  for (let i = lines.length - 1; i > 0; i--) {
    const j = Math.floor(rnd() * (i + 1));
    [lines[i], lines[j]] = [lines[j], lines[i]];
  }
  return lines.map((t, i) => `#${i + 1} ${t}`).join('\n');
}

function mockAnswerFor(who, phase) {
  if (phase === 'R1') {
    return Array.from({ length: 6 }, (_, i) =>
      `${i + 1}. 【缺口】Mock 占位 ${i + 1}（${who.name} 视角，未接真实模型）｜【补法】在 keys.json 填 ${who.provider} 的 key 后重跑｜【人日】0.25｜【动已定决策】否｜【答辩价值】低`
    ).join('\n');
  }
  if (phase === 'R2') {
    return `A. #3 在我视角下站不住：缺少失败场景的定义。\nA. #7 站不住：人日估算没有依据。\nA. #11 站不住：与已定决策冲突。\nB. 1. 【缺口】Mock 增补 1｜【补法】接真实模型｜【人日】0.25｜【动已定决策】否｜【答辩价值】低`;
  }
  if (phase === 'R3') {
    return Array.from({ length: 10 }, (_, i) =>
      `${i + 1}. 【问题】Mock 红队问题 ${i + 1}｜【为什么致命】占位说明｜【一句话怎么补】接真实模型后重跑｜【致命度】中`
    ).join('\n');
  }
  return `| 编号 | 缺口/机会 | 补法 | 人日 | 是否动已定决策 | 答辩价值 | 争议 |\n|---|---|---|---|---|---|---|\n| 1 | Mock 总表占位（未接真实模型） | 在 keys.json 填入至少一个 key 后重跑 | 0 | 否 | 低 | 无 |\n\n## 三个最该先做的\n1. 填 key（占位）\n2. 重跑（占位）\n3. 复核（占位）`;
}

/* ---------------------------------------------------------------- 导出 */
async function exportMarkdown(run) {
  await mkdir(OUT_DIR, { recursive: true });
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const name = `brainstorm_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}.md`;
  const full = path.join(OUT_DIR, name);
  const L = [];
  L.push(`# v3.0 多视角头脑风暴记录\n`);
  L.push(`- 时间：${d.toLocaleString('zh-CN')}`);
  L.push(`- 角色：${run.opts.roleIds.join(', ')}`);
  L.push(`- 模式：${run.opts.mock ? 'Mock' : '真实模型'}`);
  L.push(`- 阶段：${run.transcript.map((p) => p.phaseTitle || p.phase).join(' → ')}\n`);
  for (const p of run.transcript) {
    L.push(`\n## ${p.phaseTitle || p.phase}\n`);
    for (const it of p.items) {
      L.push(`### ${it.roleName}（${it.provider}/${it.model}${it.mock ? ' · Mock' : ''}${it.error ? ' · 失败' : ''}）`);
      L.push('');
      if (it.think) { L.push('<details><summary>思考</summary>\n\n' + it.think + '\n\n</details>\n'); }
      L.push(it.text || '（空）');
      L.push('');
    }
  }
  if (run.finalText) L.push(`\n## 最终总表（待人工勾选）\n\n${run.finalText}\n`);
  await writeFile(full, L.join('\n'), 'utf8');
  return full;
}

/* ---------------------------------------------------------------- HTTP */
function json(res, code, obj) {
  const s = JSON.stringify(obj);
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(s);
}

/* ================================================================ 群聊房间 */
const ROOM_DIR = path.join(__dirname, 'rooms');
const rooms = new Map();
const ROOM_LIMITS = { windowSize: 20, autoRounds: 2, maxCalls: 60, maxPeers: 4, sayChars: 900 };

function agentById(id) {
  const all = [...CONFIG.roles, ...(CONFIG.redTeam || []), ...(CONFIG.judge ? [CONFIG.judge] : [])];
  return all.find((x) => x.id === id) || null;
}
function newId(pfx) { return pfx + crypto.randomBytes(4).toString('hex'); }

function newRoom({ title, memberIds, dutyIds } = {}) {
  const all = CONFIG.roles.filter((r) => r.enabled !== false);
  const room = {
    id: newId('r'),
    title: title || 'v3.0 研讨群',
    createdAt: Date.now(),
    memberIds: memberIds && memberIds.length ? memberIds : all.map((r) => r.id),
    dutyIds: dutyIds && dutyIds.length ? dutyIds : all.slice(0, 3).map((r) => r.id),
    messages: [],
    tasks: [],
    settings: { ...ROOM_LIMITS },
    calls: 0,
    clients: new Set(),
    events: [],
    aborter: new AbortController(),
    busy: false,
  };
  rooms.set(room.id, room);
  return room;
}

function roomPublic(room) {
  return {
    id: room.id, title: room.title, createdAt: room.createdAt,
    memberIds: room.memberIds, dutyIds: room.dutyIds,
    messages: room.messages, tasks: room.tasks,
    settings: room.settings, calls: room.calls, busy: room.busy,
  };
}

function roomEmit(room, type, data = {}) {
  const ev = { type, t: Date.now(), ...data };
  room.events.push(ev);
  if (room.events.length > 3000) room.events.splice(0, 500);
  const payload = `event: ${type}\ndata: ${JSON.stringify(ev)}\n\n`;
  for (const res of room.clients) { try { res.write(payload); } catch { room.clients.delete(res); } }
}

async function saveRoom(room) {
  try {
    await mkdir(ROOM_DIR, { recursive: true });
    const { clients, aborter, events, ...keep } = room;
    await writeFile(path.join(ROOM_DIR, room.id + '.json'), JSON.stringify(keep, null, 2), 'utf8');
  } catch (e) { console.error('[WARN] 房间存档失败', e.message); }
}

/** 房间上下文：把最近 N 条压缩成一段文本 */
function roomContext(room, opts = {}) {
  const n = opts.n || room.settings.windowSize;
  const skipId = opts.skipId;
  const lines = room.messages
    .filter((m) => m.id !== skipId)
    .slice(-n)
    .map((m) => {
      const who = m.kind === 'user' ? '我（项目作者）' : `${m.name}`;
      const t = String(m.text || '').replace(/\s+/g, ' ').slice(0, 700);
      return `${who}：${t}`;
    });
  return lines.join('\n') || '（还没人说话）';
}

function roomSystemPrompt(agent, room) {
  return `${agent.system || ''}

你正在一个多人研讨群里，群名「${room.title}」。成员各有专长（产品作者 + 若干不同视角的顾问）。
群规则（必须遵守）：
1) 只看当前问题和你视角的独有判断，不复述别人已经说过的；
2) 如果你没有新增内容，就只输出 [PASS]，不要客套；
3) 需要具体人干活时，用 @名字 指定（可用成员：${room.memberIds.map((id) => (agentById(id) || {}).name).filter(Boolean).join('、')}）；
4) 单次发言 ≤260 字，尽量可落地（给字段 / 接口 / 脚本 / 人日）；
5) 不确定就说不确定；绝不编造代码路径、文件名、文献引用（本项目是 Spring Boot 4 + Java 17 + Vue 3，不是 Python）；
6) 不写鼓励语、不写"总体不错"。`;
}

function willMockFor(providerId) { return !keyOf(providerId); }

/** 让一个成员在房间里说一次；返回文字 */
async function speak(room, agent, userPrompt, { phase = 'chat', peerRound = 0 } = {}) {
  if (room.aborter.signal.aborted) throw new Error('STOPPED');
  if (room.calls >= room.settings.maxCalls) {
    roomEmit(room, 'system', { message: `已达本房间调用上限 ${room.settings.maxCalls} 次，停止发言。可新建房间或调高上限。` });
    throw new Error('MAX_CALLS');
  }
  room.calls += 1;
  const msg = {
    id: newId('m'), kind: 'agent', roleId: agent.id, name: agent.name, emoji: agent.emoji || '🤖',
    provider: agent.provider, model: agent.model, text: '', think: '', ts: Date.now(),
    peerRound, phase, mock: willMockFor(agent.provider), err: '',
  };
  room.messages.push(msg);
  roomEmit(room, 'msg_start', { msg: { ...msg }, calls: room.calls });
  const onText = (t) => { msg.text += t; roomEmit(room, 'delta', { msgId: msg.id, text: t }); };
  const onThink = (t) => { msg.think += t; roomEmit(room, 'think', { msgId: msg.id, text: t }); };
  try {
    if (msg.mock) {
      await mockStream(mockRoomReply(agent, userPrompt), onText, room.aborter.signal);
    } else {
      await callModel(
        { providerId: agent.provider, model: agent.model, system: roomSystemPrompt(agent, room), user: userPrompt, temperature: 0.7, maxTokens: 1200 },
        onText, onThink, room.aborter.signal,
      );
    }
    if (!msg.text.trim() && msg.think.trim()) {
      msg.text = `（正文为空·思考兜底）${msg.think.slice(0, 500)}`;
      msg.err = '正文为空';
    }
  } catch (e) {
    if (String(e.message).includes('STOPPED')) msg.err = '已停止';
    else if (String(e.message).includes('MAX_CALLS')) msg.err = '超出调用预算';
    else msg.err = String(e.message || e).slice(0, 300);
    if (!msg.text) msg.text = `（调用失败：${msg.err}）`;
    roomEmit(room, 'msg_err', { msgId: msg.id, err: msg.err });
  }
  // [PASS] = 主动弃权：保留记录但标记，不占版面
  if (/^\s*\[?PASS\]?\s*$/i.test(msg.text.trim())) { msg.abstain = true; }
  roomEmit(room, 'msg_done', { msgId: msg.id, chars: msg.text.length, abstain: !!msg.abstain, err: msg.err, calls: room.calls });
  await saveRoom(room);
  return msg;
}

function mockRoomReply(agent, prompt) {
  return `（Mock）${agent.name}：没接真实模型，先占位。你填了 keys.json 后这一句会变成真内容。`;
}

/** 处理一条用户消息：决定谁回 → 依次发言 → 可选自动接话轮 */
async function handleUserMessage(room, text, mentions = []) {
  if (room.busy) throw new Error('房间正忙：等当前这轮说完');
  room.busy = true;
  try {
    const userMsg = { id: newId('m'), kind: 'user', name: '我', text, ts: Date.now(), mentions };
    room.messages.push(userMsg);
    roomEmit(room, 'msg_start', { msg: userMsg });

    const named = mentions.filter((m) => m !== 'all' && room.memberIds.includes(m));
    const wantAll = mentions.includes('all');
    const responders = named.length ? named : wantAll ? room.memberIds : room.dutyIds.filter((id) => room.memberIds.includes(id));
    roomEmit(room, 'plan', { responders, mode: named.length ? 'mention' : wantAll ? 'all' : 'duty' });

    const spoken = [];
    for (const id of responders) {
      const agent = agentById(id);
      if (!agent) continue;
      const prompt = `房间里最近的发言：\n${roomContext(room)}\n\n现在请你（${agent.name}）针对上面这个讨论发言。`;
      const m = await speak(room, agent, prompt, { phase: 'reply' });
      spoken.push(m);
    }

    // 自动接话：让刚才说过话的人再互相补充 1–N 轮，没有新增就 [PASS]
    const rounds = Math.min(Number(room.settings.autoRounds) || 0, 2);
    const peers = spoken.filter((m) => !m.abstain && !m.err).slice(0, room.settings.maxPeers);
    for (let r = 1; r <= rounds && peers.length >= 2; r++) {
      roomEmit(room, 'peer_round', { round: r });
      let spokeThisRound = 0;
      for (const prev of peers) {
        const agent = agentById(prev.roleId);
        if (!agent) continue;
        const prompt = `房间里最近的发言：\n${roomContext(room)}\n\n这是第 ${r} 轮互相补充。请只补充你视角下别人还没提到的点；没有新增就只输出 [PASS]。`;
        const m = await speak(room, agent, prompt, { phase: 'peer', peerRound: r });
        if (!m.abstain && !m.err) spokeThisRound += 1;
      }
      if (spokeThisRound === 0) { roomEmit(room, 'system', { message: '本轮全员 [PASS]，讨论已收敛，自动停止接话。' }); break; }
    }
    roomEmit(room, 'idle', { calls: room.calls });
  } finally {
    room.busy = false;
    await saveRoom(room);
  }
}

async function exportRoom(room) {
  await mkdir(OUT_DIR, { recursive: true });
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const name = `room_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}.md`;
  const full = path.join(OUT_DIR, name);
  const L = [];
  L.push(`# 群聊记录：${room.title}`);
  L.push(`- 时间：${new Date(room.createdAt).toLocaleString('zh-CN')} → ${d.toLocaleString('zh-CN')}`);
  L.push(`- 成员：${room.memberIds.map((id) => (agentById(id) || {}).name).filter(Boolean).join('、')}`);
  L.push(`- 值班：${room.dutyIds.map((id) => (agentById(id) || {}).name).filter(Boolean).join('、')}`);
  L.push(`- 调用次数：${room.calls}\n`);
  if (room.tasks.length) {
    L.push('## 任务板\n');
    L.push('| 编号 | 任务 | 负责人 | 状态 |');
    L.push('|---|---|---|---|');
    for (const t of room.tasks) L.push(`| ${t.id} | ${t.title} | ${(agentById(t.owner) || {}).name || t.owner} | ${t.status} |`);
    L.push('');
  }
  L.push('## 对话\n');
  for (const m of room.messages) {
    if (m.kind === 'user') { L.push(`**我**：${m.text}\n`); continue; }
    const badge = m.abstain ? '（弃权）' : m.err ? `（${m.err}）` : '';
    L.push(`**${m.name}**${badge}：\n\n${m.text}\n`);
  }
  await writeFile(full, L.join('\n'), 'utf8');
  return full;
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let b = '';
    req.on('data', (c) => { b += c; if (b.length > 2e6) reject(new Error('body too large')); });
    req.on('end', () => { try { resolve(b ? JSON.parse(b) : {}); } catch (e) { reject(e); } });
    req.on('error', reject);
  });
}


const server = http.createServer(async (req, res) => {
  const u = new URL(req.url, `http://127.0.0.1:${PORT}`);
  const p = u.pathname;

  try {
    if (p === '/' || p === '/index.html') {
      const html = await readFile(path.join(PUBLIC_DIR, 'index.html'), 'utf8');
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
      return res.end(html);
    }
    if (p === '/room' || p === '/room.html') {
      const html = await readFile(path.join(PUBLIC_DIR, 'room.html'), 'utf8');
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
      return res.end(html);
    }
    if (p === '/favicon.ico') { res.writeHead(204); return res.end(); }

    /* ---------------- 群聊房间 API ---------------- */
    if (p === '/api/room/create' && req.method === 'POST') {
      const body = await readBody(req);
      const room = newRoom({ title: body.title, memberIds: body.memberIds, dutyIds: body.dutyIds });
      return json(res, 200, { room: roomPublic(room) });
    }
    if (p === '/api/room/state') {
      const room = rooms.get(u.searchParams.get('roomId'));
      if (!room) return json(res, 404, { error: '房间不存在（服务重启过？新建一个）' });
      return json(res, 200, { room: roomPublic(room) });
    }
    if (p === '/api/room/config' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      if (Array.isArray(body.memberIds)) room.memberIds = body.memberIds;
      if (Array.isArray(body.dutyIds)) room.dutyIds = body.dutyIds;
      if (body.settings) room.settings = { ...room.settings, ...body.settings };
      if (body.title) room.title = body.title;
      roomEmit(room, 'config', { room: roomPublic(room) });
      await saveRoom(room);
      return json(res, 200, { room: roomPublic(room) });
    }
    if (p === '/api/room/say' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      if (!String(body.text || '').trim()) return json(res, 400, { error: '消息为空' });
      room.aborter = new AbortController();
      handleUserMessage(room, String(body.text), Array.isArray(body.mentions) ? body.mentions : [])
        .catch((e) => roomEmit(room, 'error', { message: String(e.message || e) }));
      return json(res, 200, { ok: true });
    }
    if (p === '/api/room/assign' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      const agent = agentById(body.owner);
      if (!agent) return json(res, 400, { error: '没有这个成员' });
      const task = {
        id: 't' + (room.tasks.length + 1), title: String(body.title || '未命名任务').slice(0, 120),
        owner: body.owner, status: 'todo', ts: Date.now(),
      };
      room.tasks.push(task);
      roomEmit(room, 'task', { tasks: room.tasks });
      room.aborter = new AbortController();
      (async () => {
        room.busy = true;
        try {
          const prompt = `群里刚给你（${agent.name}）派了一个任务：「${task.title}」。\n房间里最近的发言：\n${roomContext(room)}\n\n请认领并给出你的执行计划：3 条，每条 ≤40 字，必须包含①你要动哪个文件/表/接口 ②你怎么自验 ③你需要谁配合（用 @名字）。`;
          await speak(room, agent, prompt, { phase: 'assign' });
        } catch (e) { roomEmit(room, 'error', { message: String(e.message || e) }); }
        finally { room.busy = false; roomEmit(room, 'idle', { calls: room.calls }); }
      })();
      return json(res, 200, { task, tasks: room.tasks });
    }
    if (p === '/api/room/task' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      const t = room.tasks.find((x) => x.id === body.taskId);
      if (!t) return json(res, 404, { error: '任务不存在' });
      if (body.status) t.status = body.status;
      roomEmit(room, 'task', { tasks: room.tasks });
      await saveRoom(room);
      return json(res, 200, { tasks: room.tasks });
    }
    if (p === '/api/room/round' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      if (room.busy) return json(res, 409, { error: '房间正忙' });
      room.aborter = new AbortController();
      (async () => {
        room.busy = true;
        try {
          const recent = room.messages.filter((m) => m.kind === 'agent' && !m.abstain).slice(-room.settings.maxPeers);
          const ids = [...new Set(recent.map((m) => m.roleId))];
          const speakers = ids.length ? ids : room.dutyIds;
          roomEmit(room, 'peer_round', { round: Number(body.rounds) || 1 });
          for (const id of speakers) {
            const agent = agentById(id);
            if (!agent) continue;
            const prompt = `房间里最近的发言：\n${roomContext(room)}\n\n请继续讨论：只补充你视角下别人还没提到的点；没有新增就只输出 [PASS]。`;
            await speak(room, agent, prompt, { phase: 'peer', peerRound: Number(body.rounds) || 1 });
          }
          roomEmit(room, 'idle', { calls: room.calls });
        } catch (e) { roomEmit(room, 'error', { message: String(e.message || e) }); }
        finally { room.busy = false; }
      })();
      return json(res, 200, { ok: true });
    }
    if (p === '/api/room/stop' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      room.aborter.abort();
      roomEmit(room, 'system', { message: '已手动停止' });
      return json(res, 200, { ok: true });
    }
    if (p === '/api/room/save' && req.method === 'POST') {
      const body = await readBody(req);
      const room = rooms.get(body.roomId);
      if (!room) return json(res, 404, { error: '房间不存在' });
      const file = await exportRoom(room);
      return json(res, 200, { path: file });
    }

    if (p === '/api/state') {
      loadKeys();
      return json(res, 200, {
        port: PORT,
        providers: CONFIG.providers.map((x) => providerInfo(x.id)),
        roles: CONFIG.roles.map(({ id, name, emoji, provider, model, description, system }) => ({ id, name, emoji, provider, model, description, system })),
        redTeam: (CONFIG.redTeam || []).map(({ id, name, provider, model, description }) => ({ id, name, provider, model, description })),
        judge: CONFIG.judge ? { id: CONFIG.judge.id, name: CONFIG.judge.name, provider: CONFIG.judge.provider, model: CONFIG.judge.model } : null,
        brief: CONFIG.brief || '',
        concurrency: CONFIG.concurrency || 2,
      });
    }

    if (p === '/api/run' && req.method === 'POST') {
      const body = await readBody(req);
      const roleIds = Array.isArray(body.roleIds) && body.roleIds.length
        ? body.roleIds
        : CONFIG.roles.map((r) => r.id);
      const opts = {
        roleIds,
        brief: body.brief || CONFIG.brief || '',
        mock: Boolean(body.mock),
        phases: body.phases || { r1: true, r2: true, r3: true, r4: true },
      };
      const run = createRun(opts);
      runOrchestration(run, {}); // 不 await，后台跑
      return json(res, 200, { runId: run.id });
    }

    if (p === '/api/stream') {
      const roomParam = u.searchParams.get('roomId');
      if (roomParam) {
        const room = rooms.get(roomParam);
        if (!room) return json(res, 404, { error: '房间不存在' });
        res.writeHead(200, {
          'Content-Type': 'text/event-stream; charset=utf-8',
          'Cache-Control': 'no-cache, no-transform',
          Connection: 'keep-alive',
          'X-Accel-Buffering': 'no',
        });
        res.write(': connected\n\n');
        for (const ev of room.events) res.write(`event: ${ev.type}\ndata: ${JSON.stringify(ev)}\n\n`);
        room.clients.add(res);
        const rping = setInterval(() => { try { res.write(': ping\n\n'); } catch {} }, 15000);
        req.on('close', () => { clearInterval(rping); room.clients.delete(res); });
        return;
      }
      const runId = u.searchParams.get('runId');
      const run = runs.get(runId);
      if (!run) return json(res, 404, { error: 'run not found' });
      res.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
        'X-Accel-Buffering': 'no',
      });
      res.write(': connected\n\n');
      // 补发历史事件
      for (const ev of run.events) {
        res.write(`event: ${ev.type}\ndata: ${JSON.stringify(ev)}\n\n`);
      }
      run.clients.add(res);
      const ping = setInterval(() => { try { res.write(': ping\n\n'); } catch {} }, 15000);
      req.on('close', () => { clearInterval(ping); run.clients.delete(res); });
      return;
    }

    if (p === '/api/stop' && req.method === 'POST') {
      const body = await readBody(req);
      const run = runs.get(body.runId);
      if (!run) return json(res, 404, { error: 'run not found' });
      run.aborter.abort();
      run.status = 'stopped';
      return json(res, 200, { ok: true });
    }

    if (p === '/api/export' && req.method === 'POST') {
      const body = await readBody(req);
      const run = runs.get(body.runId);
      if (!run) return json(res, 404, { error: 'run not found' });
      const file = await exportMarkdown(run);
      return json(res, 200, { path: file });
    }

    if (p === '/api/reload' && req.method === 'POST') {
      loadKeys();
      return json(res, 200, { ok: true, providers: CONFIG.providers.map((x) => providerInfo(x.id)) });
    }

    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('404');
  } catch (e) {
    console.error('[ERR]', e);
    try { json(res, 500, { error: String(e.message || e) }); } catch {}
  }
});

server.listen(PORT, '127.0.0.1', () => {
  const ready = CONFIG.providers.filter((x) => keyOf(x.id)).length;
  console.log(`\n  AI 群聊工作台已启动`);
  console.log(`  打开：http://127.0.0.1:${PORT}`);
  console.log(`  模型：${CONFIG.providers.length} 家，已配置 key：${ready} 家`);
  console.log(`  角色：${CONFIG.roles.length} 个视角 + ${(CONFIG.redTeam || []).length} 个红队 + 1 裁决`);
  console.log(`  导出目录：${OUT_DIR}`);
  if (ready === 0) console.log(`  ⚠ 还没有任何 key，页面会用 Mock 模式演示流程（不产生费用）\n`);
});
