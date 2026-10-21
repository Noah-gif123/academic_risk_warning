import { http } from './http'

/** 课程内容 · 教师端（导学 / 教学目标 / 目录 / 课时资源 / 资料库） */
export const contentApi = {
  // 导学
  guide: (teachingClassId) => http.get(`/teacher/guide?teachingClassId=${teachingClassId}`),
  saveGuide: (body) => http.post('/teacher/guide', body),
  // 教学目标
  objectives: (teachingClassId) => http.get(`/teacher/objectives?teachingClassId=${teachingClassId}`),
  saveObjectives: (body) => http.post('/teacher/objectives', body),
  // 目录（课时）
  lessons: (teachingClassId) => http.get(`/teacher/lessons?teachingClassId=${teachingClassId}`),
  syncLessons: (teachingClassId) => http.post(`/teacher/lessons/sync?teachingClassId=${teachingClassId}`),
  updateLesson: (id, body) => http.post(`/teacher/lessons/${id}`, body),
  // 课时三段资源
  addResource: (body) => http.post('/teacher/lesson-resources', body),
  publishResource: (id, publish) => http.post(`/teacher/lesson-resources/${id}/publish?publish=${publish}`),
  deleteResource: (id) => http.delete(`/teacher/lesson-resources/${id}`),
  // 资料库
  resources: (teachingClassId) => http.get(`/teacher/resources?teachingClassId=${teachingClassId}`),
  papers: (teachingClassId) => http.get(`/teacher/papers?teachingClassId=${teachingClassId}`),
  upload: (teachingClassId, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.upload(`/teacher/resources/upload?teachingClassId=${teachingClassId}`, fd)
  },
  voidFile: (id, reason) => http.post(`/teacher/resources/${id}/void`, { reason }),
  downloadFile: (id, name) => http.download(`/files/${id}`, name)
}
