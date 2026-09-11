import { ref, computed } from 'vue'
import { setActiveToken, getActiveToken, onTokenExpired, onTokenRefreshed } from '../api/http.js'

// ==================== 全局认证状态 ====================
const isLoggedIn = ref(false)
const identity = ref('teacher') // 'teacher' | 'student' | 'admin'

// 教师
const teacherToken = ref(localStorage.getItem('token') || '')
const teacher = ref(JSON.parse(localStorage.getItem('teacher') || 'null'))
const isTeacherLoggedIn = computed(() => !!teacherToken.value)

// 学生
const studentToken = ref(localStorage.getItem('studentToken') || '')
const studentInfo = ref(JSON.parse(localStorage.getItem('studentInfo') || 'null'))
const isStudentLoggedIn = computed(() => !!studentToken.value)

// 管理员
const adminToken = ref(localStorage.getItem('adminToken') || '')
const adminInfo = ref(JSON.parse(localStorage.getItem('adminInfo') || 'null'))
const isAdminLoggedIn = computed(() => !!adminToken.value)

// 综合登录状态
const activeLoggedIn = computed(() => isTeacherLoggedIn.value || isStudentLoggedIn.value || isAdminLoggedIn.value)
const activeTokenRef = computed(() => teacherToken.value || studentToken.value || adminToken.value)

// 用户显示名
const userDisplayName = computed(() => {
  if (isTeacherLoggedIn.value) return teacher.value?.teacherName || teacher.value?.teacherNo || '教师'
  if (isStudentLoggedIn.value) return studentInfo.value?.studentName || studentInfo.value?.studentNo || '学生'
  if (isAdminLoggedIn.value) return adminInfo.value?.adminName || adminInfo.value?.adminNo || '管理员'
  return ''
})

// ==================== 初始化 ====================
export function initAuth() {
  const savedTeacherToken = localStorage.getItem('token')
  const savedStudentToken = localStorage.getItem('studentToken')
  const savedAdminToken = localStorage.getItem('adminToken')

  if (savedTeacherToken) {
    identity.value = 'teacher'
  } else if (savedStudentToken) {
    identity.value = 'student'
  } else if (savedAdminToken) {
    identity.value = 'admin'
  }

  const savedToken = savedTeacherToken || savedStudentToken || savedAdminToken
  if (savedToken) {
    setActiveToken(savedToken)
  }

  // 注册 Token 过期回调
  onTokenExpired(() => {
    clearAllAuth()
  })

  // 自动续期成功后同步 ref：否则组件仍拿着旧 token 发请求，会反复 401
  onTokenRefreshed(({ token, key }) => {
    if (key === 'adminToken') adminToken.value = token
    else if (key === 'studentToken') studentToken.value = token
    else teacherToken.value = token
    setActiveToken(token)
  })
}

/** 登出时若清掉的正是当前活跃 token，则一并清空，避免退出后仍带着旧 token 请求 */
function clearActiveIfMatches(token) {
  if (token && getActiveToken() === token) setActiveToken('')
}

// ==================== 教师登录 ====================
export function setTeacherLogin(token, data) {
  teacherToken.value = token
  teacher.value = data
  identity.value = 'teacher'
  localStorage.setItem('token', token)
  localStorage.setItem('teacher', JSON.stringify(data))
  setActiveToken(token)
}

// ==================== 学生登录 ====================
export function setStudentLogin(token, data) {
  studentToken.value = token
  studentInfo.value = data
  identity.value = 'student'
  localStorage.setItem('studentToken', token)
  localStorage.setItem('studentInfo', JSON.stringify(data))
  setActiveToken(token)
}

// ==================== 管理员登录 ====================
export function setAdminLogin(token, data) {
  adminToken.value = token
  adminInfo.value = data
  identity.value = 'admin'
  localStorage.setItem('adminToken', token)
  localStorage.setItem('adminInfo', JSON.stringify(data))
  setActiveToken(token)
}

// ==================== 登出 ====================
export function teacherLogout() {
  clearActiveIfMatches(teacherToken.value)
  teacherToken.value = ''
  teacher.value = null
  localStorage.removeItem('token')
  localStorage.removeItem('teacher')
}

export function studentLogout() {
  clearActiveIfMatches(studentToken.value)
  studentToken.value = ''
  studentInfo.value = null
  localStorage.removeItem('studentToken')
  localStorage.removeItem('studentInfo')
}

export function adminLogout() {
  clearActiveIfMatches(adminToken.value)
  adminToken.value = ''
  adminInfo.value = null
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminInfo')
}

export function clearAllAuth() {
  teacherLogout()
  studentLogout()
  adminLogout()
}

// ==================== 身份切换 ====================
export function switchIdentity(type) {
  identity.value = type
  // 清除对方数据
  if (type !== 'teacher') teacherLogout()
  if (type !== 'student') studentLogout()
  if (type !== 'admin') adminLogout()
}

// ==================== 导出 ====================
export function useAuth() {
  return {
    identity,
    isLoggedIn: activeLoggedIn,
    activeToken: activeTokenRef,
    userDisplayName,
    // 教师
    teacherToken, teacher,
    isTeacherLoggedIn,
    // 学生
    studentToken, studentInfo,
    isStudentLoggedIn,
    // 管理员
    adminToken, adminInfo,
    isAdminLoggedIn,
    // 方法
    initAuth,
    setTeacherLogin, setStudentLogin, setAdminLogin,
    teacherLogout, studentLogout, adminLogout,
    clearAllAuth, switchIdentity
  }
}
