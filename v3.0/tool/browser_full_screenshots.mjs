import fs from 'node:fs'
const PORT = 9227
const BASE = 'http://127.0.0.1:18099'
const sleep = ms => new Promise(r => setTimeout(r, ms))
async function login(loginNo) {
  const cap = await (await fetch(`${BASE}/api/auth/captcha`)).json()
  const code = await (await fetch(`${BASE}/api/auth/captcha/debug?captchaId=${cap.data.captchaId}`)).json()
  const r = await (await fetch(`${BASE}/api/auth/login`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ loginNo, password:'000000', captchaId:cap.data.captchaId, captchaCode:code.data }) })).json()
  if (r.code !== 0) throw new Error('login ' + loginNo + ': ' + r.message)
  return r.data
}
let seq = 0
function send(ws, method, params={}) {
  return new Promise((res, rej) => {
    const id = ++seq
    const on = e => { const m=JSON.parse(e.data); if(m.id===id){ ws.removeEventListener('message',on); m.error?rej(new Error(JSON.stringify(m.error))):res(m.result) } }
    ws.addEventListener('message', on); ws.send(JSON.stringify({id,method,params}))
  })
}
const pages = {
  STUDENT: [['warnings','/student/warnings'],['home','/student/home'],['courses','/student/courses'],['exams','/student/exams'],['projects','/student/projects'],['questionnaires','/student/questionnaires'],['reports','/student/reports'],['qa','/student/qa']],
  TEACHER: [['home','/teacher/home'],['courses','/teacher/courses'],['monitor','/teacher/monitor']],
  COUNSELOR: [['home','/counselor/home'],['focus','/counselor/focus'],['classes','/counselor/classes'],['interventions','/counselor/interventions'],['stats','/counselor/stats']]
}
const users = { STUDENT:'20250001', TEACHER:'1001', COUNSELOR:'C001' }
for (const [role, list] of Object.entries(pages)) {
  const u = await login(users[role])
  const target = await (await fetch(`http://127.0.0.1:${PORT}/json/new?http://localhost:5173/`, {method:'PUT'})).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j})
  await send(ws,'Page.enable'); await send(ws,'Runtime.enable')
  await send(ws,'Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false})
  await sleep(1000)
  for (const [name, hash] of list) {
    await send(ws,'Runtime.evaluate',{expression:`(()=>{localStorage.setItem('token',${JSON.stringify(u.token)});localStorage.setItem('role',${JSON.stringify(role)});localStorage.setItem('realName',${JSON.stringify(u.realName||role)});localStorage.setItem('refId',${JSON.stringify(String(u.refId))});location.hash=${JSON.stringify('#'+hash)};location.reload();return true})()`})
    await sleep(3000)
    const res = await send(ws,'Page.captureScreenshot',{format:'png',captureBeyondViewport:false})
    fs.writeFileSync(`v3.0/design/ui-final-${role.toLowerCase()}-${name}.png`, Buffer.from(res.data,'base64'))
    console.log('saved', role, name)
  }
  ws.close()
}
