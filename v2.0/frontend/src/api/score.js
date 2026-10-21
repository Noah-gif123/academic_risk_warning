import { http } from './http'

/** 成绩构成 · 教师端 */
export const scoreApi = {
  // 模板（教师可读）
  templates: (courseType) =>
    http.get('/teacher/score-templates' + (courseType ? `?courseType=${courseType}` : '')),
  // 某开课实例的成绩构成（无则 null）
  scheme: (offeringId) => http.get(`/teacher/score-scheme?offeringId=${offeringId}`),
  saveScheme: (body) => http.post('/teacher/score-scheme', body),
  confirm: (id) => http.post(`/teacher/score-scheme/${id}/confirm`),
  lock: (id) => http.post(`/teacher/score-scheme/${id}/lock`)
}

/** 成绩构成 · 教务端 */
export const adminScoreApi = {
  templates: (courseType) =>
    http.get('/admin/score-templates' + (courseType ? `?courseType=${courseType}` : '')),
  schemes: (termId) => http.get('/admin/score-schemes' + (termId ? `?termId=${termId}` : '')),
  lock: (id) => http.post(`/admin/score-schemes/${id}/lock`)
}
