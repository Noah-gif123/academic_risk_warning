#!/usr/bin/env node
/* check_consistency.js —— 交付前口径一致性自检（M13）
 *
 * 答辩最怕的不是功能少，而是"同一个东西在不同页面叫不同名字/不同数字"。
 * 这个脚本把已经冻结的几条口径写成可执行检查，改完代码跑一遍，全 PASS 再演示：
 *
 *   1. 禁用词：知识点 / 学习时长 / 向量库 / 心理辅导 / 通用聊天窗（范围确认书里明确不做）
 *   2. 预警等级中文名只能在 labels.js 定义一次（四档：红/橙/黄/绿），页面里不许再各写一份
 *   3. AI 来源标签统一为「AI 生成 / 规则生成」（不出现"模型生成""AI生成"）
 *   4. 答疑引用必须带「依据：」与页码格式（第N页）
 *   5. 迁移脚本编号连续、SQL 目录与 README 清单对得上
 *
 * 用法：node tool/check_consistency.js   （失败时退出码 1，可挂到提交前）
 */
'use strict'
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..')
const FRONTEND = path.join(ROOT, 'frontend', 'src')
const BACKEND = path.join(ROOT, 'backend', 'src', 'main', 'java')
const SQL = path.join(ROOT, 'sql')

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
}
function walk(dir, exts, out = []) {
  if (!fs.existsSync(dir)) return out
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walk(p, exts, out)
    else if (exts.some((x) => e.name.endsWith(x))) out.push(p)
  }
  return out
}
const rel = (p) => path.relative(ROOT, p).replace(/\\/g, '/')
const read = (p) => fs.readFileSync(p, 'utf8')

// ---------- 1. 禁用词 ----------
const BANNED = ['知识点', '学习时长', '向量库', '心理辅导', '通用聊天窗']
{
  const files = [...walk(FRONTEND, ['.vue', '.js']), ...walk(BACKEND, ['.java'])]
  const hits = []
  for (const f of files) {
    const text = read(f)
    for (const w of BANNED) {
      let idx = -1
      while ((idx = text.indexOf(w, idx + 1)) !== -1) {
        const line = text.slice(0, idx).split('\n').length
        const snippet = text.split('\n')[line - 1].trim().slice(0, 100)
        hits.push(`${rel(f)}:${line} 「${w}」 ${snippet}`)
      }
    }
  }
  // 注释里提到"不用知识点"是允许的（那是在讲口径），只有出现在字符串/模板里才算违规
  const real = hits.filter((h) => !/^\S+:\d+ 「[^」]+」\s*(\/\/|\*|\/\*|#|<!--)/.test(h))
  check('1 禁用词（知识点/学习时长/向量库/心理辅导/通用聊天窗）',
    real.length === 0,
    real.length ? real.join('\n    ') : `扫描 ${files.length} 个文件，未发现禁用词（注释里的口径说明已豁免）`)
}

// ---------- 2. 等级中文名只在 labels.js 定义 ----------
{
  const vueFiles = walk(FRONTEND, ['.vue'])
  const bad = []
  const pattern = /\b(RED|ORANGE|YELLOW|GREEN)\s*:\s*['"][^'"]*[红橙黄绿]/
  for (const f of vueFiles) {
    read(f).split('\n').forEach((line, i) => {
      if (pattern.test(line) && !/class=|:class/.test(line)) bad.push(`${rel(f)}:${i + 1} ${line.trim().slice(0, 100)}`)
    })
  }
  const labels = read(path.join(FRONTEND, 'labels.js'))
  const hasAll = ['RED', 'ORANGE', 'YELLOW', 'GREEN'].every((l) => labels.includes(l))
  check('2 预警等级中文名统一在 labels.js（四档：红/橙/黄/绿）',
    bad.length === 0 && hasAll,
    bad.length ? `发现重复定义的等级中文名（应改用 labels.js）：\n    ${bad.join('\n    ')}`
      : (hasAll ? 'labels.js 已覆盖 RED/ORANGE/YELLOW/GREEN，页面无重复定义' : 'labels.js 缺少等级定义'))
}

// ---------- 3. AI 来源标签文案 ----------
{
  const files = walk(FRONTEND, ['.vue', '.js'])
  const bad = []
  for (const f of files) {
    const text = read(f)
    if (/模型生成/.test(text)) bad.push(`${rel(f)} 出现「模型生成」`)
    if (/AI生成/.test(text)) bad.push(`${rel(f)} 出现「AI生成」（应为「AI 生成」，中间有空格）`)
  }
  const ok = walk(FRONTEND, ['.vue']).filter((f) => read(f).includes("'规则生成'")).length
  check('3 AI 来源标签统一（AI 生成 / 规则生成）', bad.length === 0,
    bad.length ? bad.join('\n    ') : `未发现"模型生成/AI生成"；有 ${ok} 个页面按「规则生成 / AI 生成」展示`)
}

// ---------- 4. 答疑引用格式 ----------
{
  const qa = path.join(FRONTEND, 'components', 'CourseQaBox.vue')
  const studentQa = path.join(BACKEND, 'com/example/academic_risk_warning/service/ai/StudentQaService.java')
  const qaText = fs.existsSync(qa) ? read(qa) : ''
  const svcText = fs.existsSync(studentQa) ? read(studentQa) : ''
  const frontOk = qaText.includes('依据：') && qaText.includes('第N页') === false && /pageNo/.test(qaText)
  const backOk = svcText.includes('依据') && svcText.includes('MIN_LEX_SUPPORT')
  check('4 答疑引用格式（依据：文档名 第N段，PPTX 带第N页）', frontOk && backOk,
    frontOk && backOk ? '前端展示「依据：…第N段（第N页）」，后端有引用校验与词面支撑阈值'
      : `前端=${frontOk} 后端=${backOk}（检查 CourseQaBox.vue / StudentQaService.java）`)
}

// ---------- 5. SQL 脚本编号连续 + README 清单 ----------
{
  const files = fs.readdirSync(SQL).filter((f) => /^\d+_.*\.sql$/.test(f))
  const nums = files.map((f) => parseInt(f.split('_')[0], 10)).sort((a, b) => a - b)
  const toolDir = path.join(ROOT, 'tool')
  const inTool = fs.existsSync(toolDir)
    ? fs.readdirSync(toolDir).filter((f) => /^\d+_.*\.sql$/.test(f))
    : []
  const missing = []
  for (let i = 1; i < nums.length; i++) {
    for (let n = nums[i - 1] + 1; n < nums[i]; n++) {
      // 允许由生成器产出、放在 tool/ 下的脚本（例如 20_rebuild_demo.sql 由 seed_demo.js 生成）
      if (!inTool.some((f) => f.startsWith(`${n}_`))) missing.push(String(n))
    }
  }
  const readme = read(path.join(SQL, 'README.md'))
  const missingInReadme = files.filter((f) => !readme.includes(f))
  check('5 SQL 脚本编号连续且 README 有登记',
    missing.length === 0 && missingInReadme.length === 0,
    missing.length || missingInReadme.length
      ? `缺失编号：${missing.join('、') || '无'}（sql/ 与 tool/ 都没有）；README 未登记：${missingInReadme.join('、') || '无'}`
      : `${files.length} 个脚本编号连续（${nums[0]}–${nums[nums.length - 1]}），` +
        `${inTool.length ? `其中 ${inTool.join('、')} 在 tool/ 下由生成器产出；` : ''}README 全部登记`)
}

// ---------- 汇总 ----------
let failed = 0
console.log('\n===== 口径一致性自检（M13）=====\n')
for (const r of results) {
  if (!r.ok) failed++
  console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}`)
  console.log(`      ${r.detail.replace(/\n/g, '\n      ')}`)
  console.log('')
}
console.log(failed === 0 ? `全部通过（${results.length}/${results.length}）` : `未通过 ${failed} 项（共 ${results.length} 项）`)
process.exit(failed === 0 ? 0 : 1)
