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
  /** 名单：teachingClassId / offeringId / level / status */
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
  exportFile: (params) => http.download('/teacher/alerts/export' + query(params), '预警名单.xlsx')
}

/** 判定引擎：重新判定（范围优先级 enrollmentId > teachingClassId > offeringId > termId，教师只能判自己的教学班） */
export const riskEngineApi = {
  evaluate: (params) => http.post('/engine/evaluate' + query(params))
}
