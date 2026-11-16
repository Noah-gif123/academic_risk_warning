// Cross-platform P1 smoke test used by GitHub Actions.
// Usage: BASE_URL=http://127.0.0.1:18101 node v3.0/tool/verify_p1.mjs
const BASE = process.env.BASE_URL || 'http://127.0.0.1:18101'
let pass = 0, fail = 0
const ok = (name, detail = '') => { pass++; console.log(`[PASS] ${name}${detail ? '：' + detail : ''}`) }
const bad = (name, detail = '') => { fail++; console.log(`[FAIL] ${name}${detail ? '：' + detail : ''}`) }

async function json(path, options = {}) {
  const res = await fetch(BASE + path, options)
  let data = null
  try { data = await res.json() } catch { }
  return { status: res.status, data }
}
async function login(loginNo) {
  const cap = await json('/api/auth/captcha')
  const debug = await json(`/api/auth/captcha/debug?captchaId=${cap.data.data.captchaId}`)
  if (!debug.data || debug.data.code !== 0) throw new Error('验证码调试验证失败（需要 --app.auth.captcha-debug=true）')
  const body = JSON.stringify({ loginNo, password: '000000', captchaId: cap.data.data.captchaId, captchaCode: debug.data.data })
  const r = await json('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body })
  if (!r.data || r.data.code !== 0) throw new Error(`登录失败：${loginNo}`)
  return r.data.data
}
const auth = (t) => ({ Authorization: 'Bearer ' + t })

const health = await json('/api/ai/health')
if (health.status === 200 && health.data.code === 0) ok('后端健康检查', `enabled=${health.data.data.enabled}`)
else bad('后端健康检查')

const users = {}
for (const no of ['20250001', '1001', 'C001', 'admin001']) {
  try { users[no] = await login(no); ok('登录 ' + no, users[no].role) }
  catch (e) { bad('登录 ' + no, e.message) }
}
if (fail > 0) { console.log(`结果：PASS=${pass} FAIL=${fail}`); process.exit(1) }

const denied = async (name, path, token) => {
  const r = await json(path, { headers: auth(token) })
  if (r.status === 403 || (r.data && r.data.code === 403)) ok(name)
  else bad(name, `HTTP ${r.status}`)
}
await denied('学生禁止访问辅导员接口', '/api/counselor/overview', users['20250001'].token)
await denied('教师禁止访问行政班报告', '/api/counselor/reports/admin-class/1/latest', users['1001'].token)
await denied('辅导员禁止访问教学班报告', '/api/teacher/reports/class/201/latest', users['C001'].token)
await denied('辅导员禁止访问单课程学生报告', '/api/reports/student/1', users['C001'].token)

const own = await json('/api/counselor/reports/admin-class/1/latest', { headers: auth(users['C001'].token) })
if (own.status === 200 && own.data.code === 0) ok('辅导员读取本人行政班报告')
else bad('辅导员读取本人行政班报告')

const term = await json('/api/counselor/reports/students/1/term-report', { method: 'POST', headers: { ...auth(users['C001'].token), 'Content-Type': 'application/json' }, body: '{}' })
if (term.status === 200 && term.data.code === 0 && term.data.data.sections.length >= 4) ok('学生学期报告生成', `sections=${term.data.data.sections.length}`)
else bad('学生学期报告生成')

const pdfRes = await fetch(`${BASE}/api/counselor/reports/students/1/term-report/pdf`, { headers: auth(users['C001'].token) })
const pdf = Buffer.from(await pdfRes.arrayBuffer())
if (pdfRes.ok && pdf.subarray(0, 4).toString() === '%PDF') ok('学生学期报告 PDF', `bytes=${pdf.length}`)
else bad('学生学期报告 PDF')

const q = await json('/api/counselor/questionnaires', { headers: auth(users['C001'].token) })
const n = await json('/api/counselor/notices', { headers: auth(users['C001'].token) })
if (q.data && q.data.code === 0) ok('问卷方案 B'); else bad('问卷方案 B')
if (n.data && n.data.code === 0) ok('系统通知'); else bad('系统通知')

console.log(`\n结果：PASS=${pass} FAIL=${fail}`)
if (fail > 0) process.exit(1)
