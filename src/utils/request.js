import axios from 'axios'
import qs from 'qs'
import { ElMessage } from 'element-plus'

// 统一请求实例：baseURL 与后端 context-path 保持一致
const request = axios.create({
  baseURL: '/springbootdemo',
  timeout: 10000,
  // 后端用 session 保存登录状态，跨端口时必须携带 cookie
  withCredentials: true
})

// 请求拦截：后端按表单方式接收参数，统一用 qs 序列化
request.interceptors.request.use(config => {
  if (config.data && !(config.data instanceof FormData)) {
    config.headers['Content-Type'] = 'application/x-www-form-urlencoded'
    // indices:false 让数组序列化成 userids=1&userids=2，而不是 userids[0]=1
    config.data = qs.stringify(config.data, { indices: false })
  }
  return config
})

// 响应拦截：直接返回业务数据，统一处理 401（登录拦截器判定未登录）
request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response && error.response.status === 401) {
      ElMessage.warning('登录已失效，请重新登录')
      window.location.href = '/login'
    } else {
      ElMessage.error('请求失败：' + (error.message || '未知错误'))
    }
    return Promise.reject(error)
  }
)

export default request
