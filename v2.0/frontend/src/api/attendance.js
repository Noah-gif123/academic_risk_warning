import { http } from './http'

/** 考勤 · 教师端 */
export const attendanceApi = {
  sessions: (teachingClassId) => http.get(`/teacher/attendance/sessions?teachingClassId=${teachingClassId}`),
  roster: (teachingClassId, sessionId) =>
    http.get(`/teacher/attendance/roster?teachingClassId=${teachingClassId}&sessionId=${sessionId}`),
  call: (body) => http.post('/teacher/attendance/call', body),
  updateRecord: (id, body) => http.post(`/teacher/attendance/records/${id}`, body),
  stats: (teachingClassId) => http.get(`/teacher/attendance/stats?teachingClassId=${teachingClassId}`),
  importFile: (teachingClassId, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.upload(`/teacher/attendance/import?teachingClassId=${teachingClassId}`, fd)
  },
  template: (teachingClassId) =>
    http.download(`/teacher/attendance/import-template?teachingClassId=${teachingClassId}`, '考勤导入模板.xlsx'),
  archives: (teachingClassId) => http.get(`/teacher/attendance/archives?teachingClassId=${teachingClassId}`),
  submitArchive: (body) => http.post('/teacher/attendance/archives', body)
}

/** 考勤 · 教务端 */
export const adminAttendanceApi = {
  archives: (termId, status) => {
    const q = new URLSearchParams()
    if (termId) q.set('termId', termId)
    if (status) q.set('status', status)
    const s = q.toString()
    return http.get('/admin/attendance/archives' + (s ? '?' + s : ''))
  },
  review: (id, approve, body) =>
    http.post(`/admin/attendance/archives/${id}/review?approve=${approve}`, body || {}),
  remind: (id) => http.post(`/admin/attendance/archives/${id}/remind`),
  coverage: (termId) => http.get('/admin/attendance/coverage' + (termId ? `?termId=${termId}` : ''))
}

/** 考勤 · 学生端 */
export const myAttendanceApi = {
  mine: () => http.get('/student/attendance')
}

export const attendanceStatuses = [
  { value: 'PRESENT', label: '出勤', cls: 'text-emerald-600' },
  { value: 'LATE', label: '迟到', cls: 'text-amber-600' },
  { value: 'ABSENT', label: '缺课', cls: 'text-red-600' },
  { value: 'LEAVE', label: '请假', cls: 'text-brand-600' }
]

export function statusLabel(s) {
  return (attendanceStatuses.find((x) => x.value === s) || {}).label || s
}
export function statusClass(s) {
  return (attendanceStatuses.find((x) => x.value === s) || {}).cls || 'text-slate-500'
}
