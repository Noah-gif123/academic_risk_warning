import { http } from './http'

/** 教师端 · 我的教学课程 */
export const teacherApi = {
  myOfferings: () => http.get('/teacher/offerings')
}

/** 学生端 · 我的课程 */
export const studentApi = {
  myCourses: () => http.get('/student/courses')
}
