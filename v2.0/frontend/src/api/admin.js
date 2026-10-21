import { http } from './http'

/** 教务端 · 基础数据接口 */
export const adminApi = {
  // 学期
  listTerms: () => http.get('/admin/terms'),
  currentTerm: () => http.get('/admin/terms/current'),
  saveTerm: (body) => http.post('/admin/terms', body),
  setCurrentTerm: (id) => http.post(`/admin/terms/${id}/current`),
  // 周历
  listCalendar: (termId) => http.get(`/admin/terms/${termId}/calendar`),
  generateCalendar: (termId, force = false) =>
    http.post(`/admin/terms/${termId}/calendar/generate?force=${force}`),
  updateWeek: (weekId, body) => http.post(`/admin/calendar/${weekId}`, body),
  // 专业
  listMajors: () => http.get('/admin/majors'),
  saveMajor: (body) => http.post('/admin/majors', body),
  // 行政班
  listClasses: (params = {}) => {
    const q = new URLSearchParams()
    if (params.majorId) q.set('majorId', params.majorId)
    if (params.gradeYear) q.set('gradeYear', params.gradeYear)
    const s = q.toString()
    return http.get('/admin/classes' + (s ? '?' + s : ''))
  },
  saveClass: (body) => http.post('/admin/classes', body),
  // 辅导员分配
  listCounselors: () => http.get('/admin/counselors'),
  listAssignments: (params = {}) => {
    const q = new URLSearchParams()
    if (params.termId) q.set('termId', params.termId)
    if (params.counselorId) q.set('counselorId', params.counselorId)
    const s = q.toString()
    return http.get('/admin/counselor-assignments' + (s ? '?' + s : ''))
  },
  assignCounselor: (body) => http.post('/admin/counselor-assignments', body),
  unassign: (id) => http.delete(`/admin/counselor-assignments/${id}`),

  // ===================== 排课与教学班 =====================
  listCourses: () => http.get('/admin/courses'),
  listTeachers: () => http.get('/admin/teachers'),
  listOfferings: (termId) => http.get('/admin/offerings' + (termId ? `?termId=${termId}` : '')),
  saveOffering: (body) => http.post('/admin/offerings', body),
  confirmActual: (offeringId) => http.post(`/admin/offerings/${offeringId}/confirm-actual`),
  generateSessions: (offeringId, teachingClassId) =>
    http.post(`/admin/offerings/${offeringId}/sessions/generate?teachingClassId=${teachingClassId}`),
  listSessions: (offeringId, teachingClassId) =>
    http.get(`/admin/offerings/${offeringId}/sessions` + (teachingClassId ? `?teachingClassId=${teachingClassId}` : '')),
  listTeachingClasses: (offeringId) =>
    http.get('/admin/teaching-classes' + (offeringId ? `?offeringId=${offeringId}` : '')),
  saveTeachingClass: (body) => http.post('/admin/teaching-classes', body),
  enrollByAdminClasses: (classId) => http.post(`/admin/teaching-classes/${classId}/enroll/admin-classes`),
  enrollByStudents: (classId, studentIds) =>
    http.post(`/admin/teaching-classes/${classId}/enroll/students`, { studentIds }),
  listClassStudents: (classId) => http.get(`/admin/teaching-classes/${classId}/students`)
}
