import { http } from './http'

/** 成绩与批改 · 教师端 */
export const scoreMatrixApi = {
  pending: (teachingClassId) => http.get(`/teacher/grading/pending?teachingClassId=${teachingClassId}`),
  matrix: (teachingClassId) => http.get(`/teacher/scores/matrix?teachingClassId=${teachingClassId}`),
  saveEntries: (body) => http.post('/teacher/scores/items', body),
  commit: (teachingClassId) => http.post(`/teacher/scores/summary?teachingClassId=${teachingClassId}`),
  exportCsv: (teachingClassId) => http.download(`/teacher/scores/export?teachingClassId=${teachingClassId}`, '成绩矩阵.csv')
}

/** 学生端 · 我的成绩 */
export const myScoreApi = {
  mine: () => http.get('/student/scores')
}
