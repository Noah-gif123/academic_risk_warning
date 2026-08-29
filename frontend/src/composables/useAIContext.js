import { ref, computed } from 'vue'
import { useAuth } from './useAuth.js'

/**
 * 全局 AI 助手上下文 — 在 AppLayout 层级共享当前教学上下文
 *
 * 教师端：TeacherCourseDetail 挂载时写入 courseId/teacherId/courseName/teacherName，
 *         GlobalAIRegion 读取后传给内部 ChatAssistant。
 * 学生端：直接用 useAuth() 中的 studentToken / studentInfo，无需额外上下文。
 */

// ========== 教师上下文 ==========
const teacherId = ref(null)
const courseId = ref(null)
const courseName = ref('')
const teacherUserName = ref('')

export function useAIContext() {
  const auth = useAuth()

  // 当前应该使用的 token
  const activeToken = computed(() => {
    if (auth.isTeacherLoggedIn.value) return auth.teacherToken.value
    if (auth.isStudentLoggedIn.value) return auth.studentToken.value
    return ''
  })

  // 设置教师课程上下文（由 TeacherCourseDetail 调用）
  function setTeacherContext(tId, cId, cName, tName) {
    teacherId.value = tId
    courseId.value = cId
    courseName.value = cName
    teacherUserName.value = tName || ''
  }

  // 清除上下文（离开课程详情页时）
  function clearContext() {
    teacherId.value = null
    courseId.value = null
    courseName.value = ''
    teacherUserName.value = ''
  }

  return {
    // 教师上下文
    teacherId,
    courseId,
    courseName,
    teacherUserName,
    setTeacherContext,
    clearContext,
    // 当前 token
    activeToken
  }
}
