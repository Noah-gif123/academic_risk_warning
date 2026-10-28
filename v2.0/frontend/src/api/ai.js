import { http } from './http'

/**
 * 学生端 · 学习评价报告（AI）
 *
 * 数字全部由后端规则计算，模型只负责写"人话"；模型不可用时后端自动退回模板，
 * 所以这里的 source 字段只用来展示"AI 生成 / 规则生成"的标签，不影响可用性。
 */
export const reportApi = {
  list: () => http.get('/student/reports'),
  detail: (id) => http.get(`/student/reports/${id}`),
  generate: (body) => http.post('/student/reports/generate', body || {}),
  feedback: (id, useful, reason) =>
    http.post(`/student/reports/${id}/feedback`, { useful, reason: reason || '' })
}

/** AI 通道体检（部署后用 curl http://localhost:8080/api/ai/health 也可以） */
export const aiApi = {
  health: () => http.get('/ai/health')
}

/**
 * 群体报告（M8）：教学班（教师端）与行政班（辅导员端）
 * scope: 'CLASS' | 'ADMIN_CLASS'
 */
const groupPath = (scope, id) =>
  scope === 'ADMIN_CLASS' ? `/counselor/reports/admin-class/${id}` : `/teacher/reports/class/${id}`

export const groupReportApi = {
  latest: (scope, id) => http.get(`${groupPath(scope, id)}/latest`),
  generate: (scope, id) => http.post(groupPath(scope, id), {}),
  history: (scope, id, limit = 10) => http.get(`${groupPath(scope, id)}/history?limit=${limit}`)
}
/**
 * 预警解释卡（M9）
 * role: 'teacher' | 'student'
 */
const explainPath = (alertId, role) =>
  `${role === 'student' ? '/student' : '/teacher'}/alerts/${alertId}/explain`

export const alertExplainApi = {
  latest: (alertId, role = 'teacher') => http.get(explainPath(alertId, role)),
  generate: (alertId, role = 'teacher') => http.post(explainPath(alertId, role), {})
}