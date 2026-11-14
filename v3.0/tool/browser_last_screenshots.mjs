import fs from 'node:fs'
const PORT=9227, BASE='http://127.0.0.1:18099'
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
async function login(loginNo){
 const cap=await(await fetch(`${BASE}/api/auth/captcha`)).json()
 const code=await(await fetch(`${BASE}/api/auth/captcha/debug?captchaId=${cap.data.captchaId}`)).json()
 const r=await(await fetch(`${BASE}/api/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({loginNo,password:'000000',captchaId:cap.data.captchaId,captchaCode:code.data})})).json()
 if(r.code!==0) throw new Error(loginNo+': '+r.message)
 return r.data
}
let seq=0
function send(ws,method,params={}){return new Promise((res,rej)=>{const id=++seq;const on=e=>{const m=JSON.parse(e.data);if(m.id===id){ws.removeEventListener('message',on);m.error?rej(new Error(JSON.stringify(m.error))):res(m.result)}};ws.addEventListener('message',on);ws.send(JSON.stringify({id,method,params}))})}
async function shot(loginNo, role, name, hash){
 const u=await login(loginNo)
 const page=await(await fetch(`http://127.0.0.1:${PORT}/json/new?http://localhost:5173/`,{method:'PUT'})).json()
 const ws=new WebSocket(page.webSocketDebuggerUrl)
 await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j})
 await send(ws,'Page.enable');await send(ws,'Runtime.enable');await send(ws,'Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});await sleep(800)
 await send(ws,'Runtime.evaluate',{expression:`(()=>{localStorage.setItem('token',${JSON.stringify(u.token)});localStorage.setItem('role',${JSON.stringify(role)});localStorage.setItem('realName',${JSON.stringify(u.realName||role)});localStorage.setItem('refId',${JSON.stringify(String(u.refId))});location.hash=${JSON.stringify('#'+hash)};location.reload();return true})()`})
 await sleep(3500)
 const res=await send(ws,'Page.captureScreenshot',{format:'png',captureBeyondViewport:false})
 fs.writeFileSync(`v3.0/design/ui-final-${role.toLowerCase()}-${name}.png`,Buffer.from(res.data,'base64'))
 console.log('saved',role,name)
 ws.close()
}
await shot('20250001','STUDENT','take','/student/take?paperId=9001')
await shot('1002','TEACHER','grading','/teacher/grading?paperId=9001')
await shot('20250001','STUDENT','attendance','/student/attendance')
await shot('20250001','STUDENT','scores','/student/scores')
await shot('20250001','STUDENT','profile','/student/profile')
await shot('20250001','STUDENT','plan','/student/plan')
