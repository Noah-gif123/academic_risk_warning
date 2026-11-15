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
    const err = new Error(data.message || '请求失败')
    err.code = data.code
    err.status = res.status
    throw err
  }
  return data.data
}

/** 统一超时：避免后端/网络异常时页面一直转圈 */
async function fetchWithTimeout(input, init = {}, timeoutMs = 30000) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(input, { ...init, signal: controller.signal })
  } catch (e) {
    if (e && e.name === 'AbortError') throw new Error('请求超时，请稍后重试')
    throw new Error('网络连接失败，请检查后端服务')
  } finally {
    clearTimeout(timer)
  }
}

async function request(method, url, body, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  const res = await fetchWithTimeout(BASE + url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  }, options.timeoutMs || 30000)
  return handle(res)
}

/** 上传：body 为 FormData，不能手工设置 Content-Type（要让浏览器带 boundary） */
async function upload(url, formData) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetchWithTimeout(BASE + url, { method: 'POST', headers, body: formData }, 120000)
  return handle(res)
}

/** 下载：文件接口需要令牌，故用 fetch 取 blob 再触发保存 */
async function download(url, fallbackName = 'download') {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetchWithTimeout(BASE + url, { headers }, 120000)
  if (!res.ok) {
    if (res.status === 401) return handle(res)
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

/** 下载并允许 Chrome/Edge 选择保存目录 */
async function downloadWithPicker(url, fallbackName = 'download') {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  let handle = null
  if (typeof window !== 'undefined' && window.showSaveFilePicker) {
    try {
      handle = await window.showSaveFilePicker({
        suggestedName: fallbackName,
        types: [{ description: 'PDF', accept: { 'application/pdf': ['.pdf'] } }]
      })
    } catch (e) {
      if (e && e.name === 'AbortError') return
      handle = null
    }
  }

  const res = await fetchWithTimeout(BASE + url, { headers }, 120000)
  if (!res.ok) {
    if (res.status === 401) return handle(res)
    const data = await res.json().catch(() => ({ message: '下载失败' }))
    throw new Error(data.message || '下载失败')
  }
  const blob = await res.blob()
  if (handle) {
    const writable = await handle.createWritable()
    await writable.write(blob)
    await writable.close()
    return
  }
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = fallbackName
  a.click()
  setTimeout(() => URL.revokeObjectURL(a.href), 3000)
}

export const http = {
  get: (url, options) => request('GET', url, undefined, options),
  post: (url, body, options) => request('POST', url, body, options),
  patch: (url, body, options) => request('PATCH', url, body, options),
  delete: (url, options) => request('DELETE', url, undefined, options),
  upload,
  download,
  downloadWithPicker
}

