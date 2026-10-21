import { reactive } from 'vue'

export const ROLES = {
  STUDENT: { key: 'STUDENT', label: '学生', home: '/student/home' },
  TEACHER: { key: 'TEACHER', label: '教师', home: '/teacher/home' },
  COUNSELOR: { key: 'COUNSELOR', label: '辅导员', home: '/counselor/home' },
  ADMIN: { key: 'ADMIN', label: '教务', home: '/admin/home' }
}

export const session = reactive({
  token: localStorage.getItem('token') || '',
  role: localStorage.getItem('role') || '',
  realName: localStorage.getItem('realName') || '',
  refId: localStorage.getItem('refId') || ''
})

export function saveSession(vo) {
  session.token = vo.token || session.token
  session.role = vo.role || ''
  session.realName = vo.realName || ''
  session.refId = vo.refId == null ? '' : String(vo.refId)
  localStorage.setItem('token', session.token)
  localStorage.setItem('role', session.role)
  localStorage.setItem('realName', session.realName)
  localStorage.setItem('refId', session.refId)
}

export function clearSession() {
  session.token = ''
  session.role = ''
  session.realName = ''
  session.refId = ''
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('realName')
  localStorage.removeItem('refId')
}

export function homeOf(role) {
  return (ROLES[role] || ROLES.STUDENT).home
}
