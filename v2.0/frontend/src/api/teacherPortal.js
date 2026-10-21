import { http } from './http'

/** 教师端门户：首页汇总 / 教学统计 / 导出学生数据 / 结课 */
export const teacherPortalApi = {
  terms: () => http.get('/teacher/terms'),
  home: (termId) => http.get('/teacher/home' + (termId ? `?termId=${termId}` : '')),
  stats: (teachingClassId) => http.get(`/teacher/course-stats?teachingClassId=${teachingClassId}`),
  exportStudents: (teachingClassId) =>
    http.download(`/teacher/course-export?teachingClassId=${teachingClassId}`, '学生数据.csv'),
  closeClass: (teachingClassId) => http.post(`/teacher/teaching-classes/${teachingClassId}/close`)
}
