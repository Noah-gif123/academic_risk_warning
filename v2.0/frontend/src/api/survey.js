import { http } from './http'

/** 学生端问卷调查（只有「教师风格判定」一种，匿名） */
export const surveyApi = {
  list: () => http.get('/student/questionnaires'),
  detail: (id) => http.get(`/student/questionnaires/${id}`),
  answer: (id, items) => http.post(`/student/questionnaires/${id}/answer`, { items })
}

export const scoreOptions = [
  { value: 1, label: '1 很不满意' },
  { value: 2, label: '2 不太满意' },
  { value: 3, label: '3 一般' },
  { value: 4, label: '4 比较满意' },
  { value: 5, label: '5 非常满意' }
]
