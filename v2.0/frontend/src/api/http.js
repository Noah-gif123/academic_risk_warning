const BASE = '/api'

export function getToken() {
  return localStorage.getItem('token') || ''
}

export function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('realName')
  localStorage.removeItem('refId')
}

async function handle(res) {
  if (res.status === 401) {
    clearSession()
    if (location.hash !== '#/login') location.hash = '#/login'
    throw new Error('未登录或登录已过期')
  }
  const data = await res.json().catch(() => ({ code: res.status, message: '响应解析失败' }))
  if (data.code !== 0) {
    throw new Error(data.message || '请求失败')
  }
  return data.data
}

async function request(method, url, body, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  const res = await fetch(BASE + url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  return handle(res)
}

/** 上传：body 为 FormData，不能手工设置 Content-Type（要让浏览器带 boundary） */
async function upload(url, formData) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetch(BASE + url, { method: 'POST', headers, body: formData })
  return handle(res)
}

/** 下载：文件接口需要令牌，故用 fetch 取 blob 再触发保存 */
async function download(url, fallbackName = 'download') {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetch(BASE + url, { headers })
  if (!res.ok) {
    const data = await res.json().catch(() => ({ message: '下载失败' }))
    throw new Error(data.message || '下载失败')
  }
  const disp = res.headers.get('Content-Disposition') || ''
  let name = fallbackName
  const m = /filename\*=UTF-8''([^;]+)/i.exec(disp)
  if (m) name = decodeURIComponent(m[1])
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = name
  a.click()
  setTimeout(() => URL.revokeObjectURL(a.href), 3000)
}

export const http = {
  get: (url, options) => request('GET', url, undefined, options),
  post: (url, body, options) => request('POST', url, body, options),
  delete: (url, options) => request('DELETE', url, undefined, options),
  upload,
  download
}
