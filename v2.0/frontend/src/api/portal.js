import { http } from './http'

/** 学生端门户：学期 / 课程卡片 / 课程详情 / 加入课程 / 个人中心 */
export const portalApi = {
  terms: () => http.get('/student/terms'),
  cards: (params = {}) => {
    const q = new URLSearchParams()
    if (params.termId) q.set('termId', params.termId)
    if (params.keyword) q.set('keyword', params.keyword)
    if (params.status) q.set('status', params.status)
    if (params.page) q.set('page', params.page)
    if (params.size) q.set('size', params.size)
    const s = q.toString()
    return http.get('/student/course-cards' + (s ? '?' + s : ''))
  },
  detail: (teachingClassId) => http.get(`/student/course/${teachingClassId}`),
  profile: () => http.get('/student/profile'),
  apply: (inviteCode) => http.post('/student/join-requests', { inviteCode }),
  myRequests: () => http.get('/student/join-requests')
}

/** 教师端：入班邀请码与申请审批 */
export const joinApi = {
  setInvite: (teachingClassId, body) => http.post(`/teacher/teaching-classes/${teachingClassId}/invite`, body || {}),
  requests: (teachingClassId, status) =>
    http.get(`/teacher/join-requests?teachingClassId=${teachingClassId}` + (status ? `&status=${status}` : '')),
  review: (id, approve, note) =>
    http.post(`/teacher/join-requests/${id}/review?approve=${approve}`, { note })
}
