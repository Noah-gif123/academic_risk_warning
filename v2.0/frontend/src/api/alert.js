import { http } from './http'

function query(params = {}) {
  const q = new URLSearchParams()
  Object.keys(params).forEach((k) => {
    const v = params[k]
    if (v !== undefined && v !== null && v !== '') q.set(k, v)
  })
  const s = q.toString()
  return s ? '?' + s : ''
}

/** 预警 · 教师端（仅本人教学班） */
export const teacherAlertApi = {
  /** 名单：teachingClassId / offeringId / level / status / appealPending */
  list: (params) => http.get('/teacher/alerts' + query(params)),
  /** 总览：红橙黄、待处理/处理中/已关闭、今日新增、最近判定、引擎版本 */
  stats: (teachingClassId) => http.get('/teacher/alerts/stats' + query({ teachingClassId })),
  /** 详情：预警 + 时间线 + 处置记录 + 当前风险状态 */
  detail: (id) => http.get(`/teacher/alerts/${id}`),
  claim: (id) => http.post(`/teacher/alerts/${id}/claim`),
  handle: (id, body) => http.post(`/teacher/alerts/${id}/handle`, body),
  close: (id, note) => http.post(`/teacher/alerts/${id}/close`, { note }),
  dismiss: (id, reason) => http.post(`/teacher/alerts/${id}/dismiss`, { reason }),
  remind: (id, note) => http.post(`/teacher/alerts/${id}/remind`, { note }),
  /** 回复学生申诉（学生端可见） */
  reply: (id, reply) => http.post(`/teacher/alerts/${id}/reply`, { reply }),
  exportFile: (params) => http.download('/teacher/alerts/export' + query(params), '预警名单.xlsx')
}

/** 判定引擎：重新判定（范围优先级 enrollmentId > teachingClassId > offeringId > termId，教师只能判自己的教学班） */
export const riskEngineApi = {
  evaluate: (params) => http.post('/engine/evaluate' + query(params))
}

/** 预警 · 学生端（只读结论 + 申诉） */
export const studentWarningApi = {
  /** 概览：每门课一条结论 + 建议 + 判定口径（termId 不传为当前学期） */
  overview: (termId) => http.get('/student/warnings' + query({ termId })),
  /** 单课详情：结论 + 学生可见时间线 + 建议 */
  detail: (enrollmentId) => http.get(`/student/warnings/${enrollmentId}`),
  /** 提交申诉（每门课一次） */
  appeal: (alertId, reason) => http.post(`/student/warnings/${alertId}/appeal`, { reason })
}
