import { http } from './http'

/** 测验与作业 · 教师端 */
export const paperApi = {
  list: (teachingClassId) => http.get(`/teacher/papers?teachingClassId=${teachingClassId}`),
  get: (id) => http.get(`/teacher/papers/${id}`),
  save: (body) => http.post('/teacher/papers', body),
  remove: (id) => http.delete(`/teacher/papers/${id}`),
  publish: (id, publish) => http.post(`/teacher/papers/${id}/publish?publish=${publish}`),
  close: (id) => http.post(`/teacher/papers/${id}/close`),
  importQuestions: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.upload('/teacher/papers/import', fd)
  },
  submissions: (paperId) => http.get(`/teacher/papers/${paperId}/submissions`),
  submission: (id) => http.get(`/teacher/submissions/${id}`),
  grade: (id, body) => http.post(`/teacher/submissions/${id}/grade`, body),
  absent: (paperId, studentId) => http.post(`/teacher/papers/${paperId}/absent?studentId=${studentId}`)
}

/** 测验与作业 · 学生端 */
export const myPaperApi = {
  list: () => http.get('/student/papers'),
  get: (id) => http.get(`/student/papers/${id}`),
  submit: (id, body) => http.post(`/student/papers/${id}/submit`, body),
  result: (id) => http.get(`/student/papers/${id}/result`)
}

export const questionTypes = [
  { value: 'SINGLE', label: '单选' },
  { value: 'MULTI', label: '多选' },
  { value: 'JUDGE', label: '判断' },
  { value: 'BLANK', label: '填空' },
  { value: 'SHORT', label: '简答' }
]

export const paperTypes = [
  { value: 'QUIZ', label: '随堂测验' },
  { value: 'HOMEWORK', label: '课后作业' },
  { value: 'EXAM', label: '线上考试' }
]
