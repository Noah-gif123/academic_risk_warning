import fs from 'node:fs'
const PORT=9227, BASE='http://127.0.0.1:18099'
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const cap=await(await fetch(`${BASE}/api/auth/captcha`)).json()
const code=await(await fetch(`${BASE}/api/auth/captcha/debug?captchaId=${cap.data.captchaId}`)).json()
const lr=await(await fetch(`${BASE}/api/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({loginNo:'1001',password:'000000',captchaId:cap.data.captchaId,captchaCode:code.data})})).json()
const u=lr.data
const page=await(await fetch(`http://127.0.0.1:${PORT}/json/new?http://localhost:5173/`,{method:'PUT'})).json()
const ws=new WebSocket(page.webSocketDebuggerUrl)
let id=0
const send=(m,p={})=>new Promise((res,rej)=>{const n=++id;const on=e=>{const x=JSON.parse(e.data);if(x.id===n){ws.removeEventListener('message',on);x.error?rej(new Error(JSON.stringify(x.error))):res(x.result)}};ws.addEventListener('message',on);ws.send(JSON.stringify({id:n,method:m,params:p}))})
await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j})
await send('Page.enable');await send('Runtime.enable');await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false})
await sleep(1000)
await send('Runtime.evaluate',{expression:`(()=>{localStorage.setItem('token',${JSON.stringify(u.token)});localStorage.setItem('role','TEACHER');localStorage.setItem('realName',${JSON.stringify(u.realName||'教师')});localStorage.setItem('refId',${JSON.stringify(String(u.refId))});location.hash='#/teacher/course/101';location.reload();return true})()`})
await sleep(4000)
const r=await send('Page.captureScreenshot',{format:'png'})
fs.writeFileSync('v3.0/design/ui-final-teacher-course-detail.png',Buffer.from(r.data,'base64'))
console.log('saved')
ws.close()
