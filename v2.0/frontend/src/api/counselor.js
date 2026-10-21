import { http } from './http'

/** 导员端：看板 / 名单与单科详情 / 干预记录 / 问卷 / 通知 */
export const counselorApi = {
  profile: () => http.get('/counselor/profile'),
  terms: () => http.get('/counselor/terms'),
  overview: (termId) => http.get('/counselor/overview' + (termId ? `?termId=${termId}` : '')),
  students: (params = {}) => {
    const q = new URLSearchParams()
    if (params.adminClassId) q.set('adminClassId', params.adminClassId)
    if (params.termId) q.set('termId', params.termId)
    const s = q.toString()
    return http.get('/counselor/students' + (s ? '?' + s : ''))
  },
  studentDetail: (studentId) => http.get(`/counselor/students/${studentId}`),
  addIntervention: (body) => http.post('/counselor/interventions', body),
  refer: (id, note) => http.post(`/counselor/interventions/${id}/refer`, { note }),
  terminate: (id, reason) => http.post(`/counselor/interventions/${id}/terminate`, { reason }),
  interventionRecords: (adminClassId) =>
    http.get('/counselor/intervention-records' + (adminClassId ? `?adminClassId=${adminClassId}` : '')),
  questionnaires: (termId) => http.get('/counselor/questionnaires' + (termId ? `?termId=${termId}` : '')),
  offeringOptions: (termId) => http.get('/counselor/offering-options' + (termId ? `?termId=${termId}` : '')),
  publishQuestionnaire: (body) => http.post('/counselor/questionnaires', body),
  notices: () => http.get('/counselor/notices'),
  markRead: (id) => http.post(`/counselor/notices/${id}/read`)
}
