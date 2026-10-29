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
/** 干预闭环度量（M10）：role = 'admin' | 'counselor' */
export const interventionMetricsApi = {
  get: (role = 'admin', termId) =>
    http.get(`/${role === 'counselor' ? 'counselor' : 'admin'}/metrics/intervention` + (termId ? `?termId=${termId}` : ''))
}
/** 课程答疑（RAG，M11）：只能由学生端调用，回答带引用、无依据拒答 */
export const qaApi = {
  ask: (body) => http.post('/student/ai/qa', body || {})
}

/**
 * AI 出题（M14）：按所选资料出题 → 五道闸门 → 审题草稿 → 采纳入卷
 * 成题责任链不变：草稿由模型出、由教师改，题目进试卷依旧走试卷保存接口。
 */
export const aiQuestionApi = {
  sources: (teachingClassId) => http.get(`/teacher/ai/questions/sources?teachingClassId=${teachingClassId}`),
  generate: (body) => http.post('/teacher/ai/questions/generate', body || {}),
  drafts: (teachingClassId, status) =>
    http.get(`/teacher/ai/questions/drafts?teachingClassId=${teachingClassId}` + (status ? `&status=${status}` : '')),
  updateDraft: (id, body) => http.post(`/teacher/ai/questions/drafts/${id}`, body || {}),
  reject: (id) => http.post(`/teacher/ai/questions/drafts/${id}/reject`, {}),
  adopt: (draftIds) => http.post('/teacher/ai/questions/adopt', { draftIds })
}
