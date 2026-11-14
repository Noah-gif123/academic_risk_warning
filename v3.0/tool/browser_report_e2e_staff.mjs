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
  return new Promise((resolve, reject) => {
    const id = ++seq
    const on = ev => { const m=JSON.parse(ev.data); if(m.id===id){ ws.removeEventListener('message',on); m.error?reject(new Error(JSON.stringify(m.error))):resolve(m.result) } }
    ws.addEventListener('message', on); ws.send(JSON.stringify({id,method,params}))
  })
}
async function check(ws, user, role, hash, buttonText) {
  const page = await (await fetch(`http://127.0.0.1:${PORT}/json/new?http://localhost:5173/`, {method:'PUT'})).json()
  const ws2 = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise((res,rej)=>{ws2.onopen=res;ws2.onerror=rej})
  await send(ws2,'Page.enable'); await send(ws2,'Runtime.enable'); await sleep(1200)
  await send(ws2,'Runtime.evaluate',{expression:`(()=>{localStorage.setItem('token',${JSON.stringify(user.token)});localStorage.setItem('role',${JSON.stringify(role)});localStorage.setItem('realName',${JSON.stringify(user.realName||role)});localStorage.setItem('refId',${JSON.stringify(String(user.refId))});location.hash=${JSON.stringify(hash)};location.reload();return true})()`,awaitPromise:true})
  await sleep(4000)
  const body = (await send(ws2,'Runtime.evaluate',{expression:'document.body.innerText',returnByValue:true})).result.value || ''
  if (!body.includes(buttonText)) throw new Error(role+' 页面未出现 '+buttonText)
  const clicked = (await send(ws2,'Runtime.evaluate',{expression:`(()=>{const b=[...document.querySelectorAll('button')].find(x=>x.innerText.includes(${JSON.stringify(buttonText)}));if(b){b.click();return true}return false})()`,returnByValue:true})).result.value
  if (!clicked) throw new Error(role+' 未找到按钮 '+buttonText)
  await sleep(3000)
  const v = (await send(ws2,'Runtime.evaluate',{expression:`(()=>{const ov=[...document.querySelectorAll('div')].find(x=>x.className.includes('backdrop-blur'));return !!ov})()`,returnByValue:true})).result.value
  if (!v) throw new Error(role+' 弹窗未出现')
  console.log('PASS', role, buttonText)
  ws2.close()
}
const teacher = await login('1001')
const counselor = await login('C001')
await check(null, teacher, 'TEACHER', '#/teacher/courses', '课程总报告')
await check(null, counselor, 'COUNSELOR', '#/counselor/classes', '班级报告')
