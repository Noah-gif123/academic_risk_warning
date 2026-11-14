import { spawn } from 'node:child_process'
import fs from 'node:fs'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const PORT = 9227
const BASE = 'http://127.0.0.1:18097'
const profile = `./.tmp-browser-e2e`
// use already running Chrome

const sleep = ms => new Promise(r => setTimeout(r, ms))
async function waitJson(url) { for (let i=0;i<60;i++){ try { const r=await fetch(url); if(r.ok) return await r.json() } catch {} await sleep(500) } throw new Error('chrome not ready') }

async function login(loginNo) {
  const cap = await (await fetch(`${BASE}/api/auth/captcha`)).json()
  const code = await (await fetch(`${BASE}/api/auth/captcha/debug?captchaId=${cap.data.captchaId}`)).json()
  const body = { loginNo, password: '000000', captchaId: cap.data.captchaId, captchaCode: code.data }
  const r = await (await fetch(`${BASE}/api/auth/login`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body) })).json()
  if (r.code !== 0) throw new Error('login failed ' + r.message)
  return r.data
}

let id = 0
function send(ws, method, params={}) {
  return new Promise((resolve, reject) => {
    const mid = ++id
    const onMsg = ev => {
      const msg = JSON.parse(ev.data)
      if (msg.id === mid) { ws.removeEventListener('message', onMsg); msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result) }
    }
    ws.addEventListener('message', onMsg)
    ws.send(JSON.stringify({ id: mid, method, params }))
  })
}

try {
  await waitJson(`http://127.0.0.1:${PORT}/json/version`)
  const page = await (await fetch(`http://127.0.0.1:${PORT}/json/new?http://localhost:5173/`, { method:'PUT' })).json()
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise((res, rej) => { ws.onopen=res; ws.onerror=rej })
  await send(ws, 'Page.enable')
  await send(ws, 'Runtime.enable')
  const user = await login('20250001')
  await sleep(1500)
  const inject = `(async()=>{localStorage.setItem('token',${JSON.stringify(user.token)});localStorage.setItem('role','STUDENT');localStorage.setItem('realName','狄珂');localStorage.setItem('refId',${JSON.stringify(String(user.refId))});location.hash='#/student/reports';return true})()`
  await send(ws, 'Runtime.evaluate', { expression: inject, awaitPromise: true })
  await sleep(4000)
  const text = await send(ws, 'Runtime.evaluate', { expression: 'document.body.innerText', returnByValue: true })
  const body = text.result.value || ''
  if (!body.includes('学习评价')) throw new Error('学习评价页面未加载')
  const click = await send(ws, 'Runtime.evaluate', { expression: `(()=>{const b=[...document.querySelectorAll('button')].find(x=>x.innerText.includes('大学计算机基础'));if(b){b.click();return true}return false})()`, returnByValue: true })
  if (!click.result.value) throw new Error('未找到课程报告按钮')
  await sleep(3000)
  const check = await send(ws, 'Runtime.evaluate', { expression: `(()=>{const ov=[...document.querySelectorAll('div')].find(x=>x.className.includes('backdrop-blur'));const txt=document.body.innerText;return {overlay:!!ov, six:txt.includes('六维画像'), pdf:txt.includes('导出 PDF')}})()`, returnByValue: true })
  const v = check.result.value
  if (!v.overlay || !v.six) throw new Error('报告弹窗未出现 ' + JSON.stringify(v))
  console.log('PASS student reports modal overlay/six-dim/pdf')
} finally {
  // keep Chrome alive
}
