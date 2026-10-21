import { http } from './http'

/** 项目与贡献度 · 教师端（注意：贡献表没有修改接口——提交后任何人不能改） */
export const projectApi = {
  list: (teachingClassId) => http.get(`/teacher/projects?teachingClassId=${teachingClassId}`),
  get: (id) => http.get(`/teacher/projects/${id}`),
  save: (body) => http.post('/teacher/projects', body),
  publish: (id, publish) => http.post(`/teacher/projects/${id}/publish?publish=${publish}`),
  close: (id) => http.post(`/teacher/projects/${id}/close`),
  grade: (submissionId, body) => http.post(`/teacher/project-submissions/${submissionId}/grade`, body)
}

/** 项目与贡献度 · 学生端 */
export const myProjectApi = {
  list: () => http.get('/student/projects'),
  get: (id) => http.get(`/student/projects/${id}`),
  teammates: (teachingClassId) => http.get(`/student/projects/teammates?teachingClassId=${teachingClassId}`),
  createTeam: (id, body) => http.post(`/student/projects/${id}/team`, body),
  submit: (id, body) => http.post(`/student/projects/${id}/submit`, body),
  contribution: (id, body) => http.post(`/student/projects/${id}/contribution`, body),
  upload: (teachingClassId, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.upload(`/student/projects/upload?teachingClassId=${teachingClassId}`, fd)
  }
}

export const formatOptions = [
  { value: 'WORD', label: 'Word 文档' },
  { value: 'PDF', label: 'PDF' },
  { value: 'CODE', label: '源代码' },
  { value: 'PPT', label: 'PPT' },
  { value: 'ZIP', label: '压缩包' },
  { value: 'LINK', label: '仓库链接' }
]
