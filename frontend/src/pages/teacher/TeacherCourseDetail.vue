<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../../composables/useAuth.js'
import { getJson } from '../../api/http.js'
import { useAIContext } from '../../composables/useAIContext.js'
import TeacherStudentList from '../../components/teacher/TeacherStudentList.vue'
import TeacherGradingView from '../../components/teacher/TeacherGradingView.vue'
import TeacherWarningPanel from '../../components/teacher/TeacherWarningPanel.vue'
import TeacherDashboardView from '../../components/teacher/TeacherDashboardView.vue'
import TeacherSummaryView from '../../components/teacher/TeacherSummaryView.vue'
import TeacherKnowledgePanel from '../../components/teacher/TeacherKnowledgePanel.vue'
import TeacherExercisePanel from '../../components/teacher/TeacherExercisePanel.vue'
import TeacherImportView from '../../components/teacher/TeacherImportView.vue'

const router = useRouter()
const route = useRoute()
const { teacherToken, teacher, isTeacherLoggedIn, clearAllAuth } = useAuth()
const { setTeacherContext, clearContext } = useAIContext()

const selectedCourse = ref(JSON.parse(localStorage.getItem('selectedCourse') || 'null'))
const message = ref('')
const messageType = ref('info')
// 优先从路由 query 参数读取菜单状态（由 AppLayout 侧边栏点击传递）
const sidebarMenu = ref(route.query.menu || 'students')
const loading = ref(false)

// 监听路由 query 变化，同步菜单状态（用于侧边栏切换到同一课程的不同视图）
watch(() => route.query.menu, (newMenu) => {
  if (newMenu) sidebarMenu.value = newMenu
})

const emits = defineEmits(['select-menu'])

// 退出
function backToCourses() {
  selectedCourse.value = null
  localStorage.removeItem('selectedCourse')
  router.push('/teacher/courses')
}

function teacherLogout() {
  clearAllAuth()
  router.push('/teacher/login')
}

function setMessage(text, type = 'info') { message.value = text; messageType.value = type }

// 暴露给子组件的事件处理
function onSelectMenu(key) {
  sidebarMenu.value = key
  emits('select-menu', key)
}

// 监听课程变化
watch(selectedCourse, (val) => {
  if (val) {
    localStorage.setItem('selectedCourse', JSON.stringify(val))
    setTeacherContext(teacher.value?.id, val.courseId, val.courseName, teacher.value?.teacherName)
  } else {
    clearContext()
  }
})

// 组件挂载时初始化 AI 上下文
onMounted(() => {
  if (selectedCourse.value && teacher.value) {
    setTeacherContext(teacher.value.id, selectedCourse.value.courseId, selectedCourse.value.courseName, teacher.value.teacherName)
  }
})
</script>

<template>
  <div class="page page-logged-in" v-if="isTeacherLoggedIn && selectedCourse">
    <!-- 工具栏 -->
    <section class="toolbar card">
      <div class="user-info">
        <div class="avatar">{{ teacher?.teacherName?.[0] || 'T' }}</div>
        <div>
          <strong>{{ teacher?.teacherName || teacher?.teacherNo }}</strong>
          <span>工号：{{ teacher?.teacherNo }}</span>
          <span v-if="teacher?.phone" class="phone">📱 {{ teacher.phone }}</span>
        </div>
      </div>
      <div class="actions">
        <button class="btn-ghost toolbar-btn" @click="backToCourses">📚 返回课程列表</button>
        <button class="btn-ghost" @click="teacherLogout">🚪 退出</button>
      </div>
    </section>

    <!-- 当前课程标签 -->
    <div class="current-course-tag">📖 当前课程：<strong>{{ selectedCourse.courseName }}</strong></div>

    <p v-if="message" class="msg top-msg" :class="messageType">{{ message }}</p>

    <!-- 子页面切换 -->
    <TeacherStudentList v-if="sidebarMenu === 'students'" :course-id="selectedCourse.courseId" />
    <TeacherGradingView v-else-if="sidebarMenu === 'grading'" :course-id="selectedCourse.courseId" @message="setMessage" />
    <TeacherWarningPanel v-else-if="sidebarMenu === 'warnings'" :course-id="selectedCourse.courseId" @message="setMessage" @switch-tab="onSelectMenu" />
    <TeacherDashboardView v-else-if="sidebarMenu === 'dashboard'" :course-id="selectedCourse.courseId" />
    <TeacherSummaryView v-else-if="sidebarMenu === 'summary'" :course-id="selectedCourse.courseId" />
    <TeacherKnowledgePanel v-else-if="sidebarMenu === 'knowledge'" :course-id="selectedCourse.courseId" :course-name="selectedCourse.courseName" />
    <TeacherExercisePanel v-else-if="sidebarMenu === 'exercises'" :course-id="selectedCourse.courseId" :course-name="selectedCourse.courseName" />
    <TeacherImportView v-else-if="sidebarMenu === 'import'" :course-id="selectedCourse.courseId" @message="setMessage" />
    <TeacherStudentList v-else :course-id="selectedCourse.courseId" />
  </div>
  <div v-else-if="!isTeacherLoggedIn" style="padding:40px;text-align:center;">
    <p>请先登录</p>
    <button class="btn-primary" @click="router.push('/teacher/login')">前往登录</button>
  </div>
  <div v-else style="padding:40px;text-align:center;">
    <p>请先选择课程</p>
    <button class="btn-primary" @click="backToCourses">返回课程列表</button>
  </div>
</template>

<style scoped>
/* TeacherCourseDetail — 继承全局 .page / .toolbar / .card / .msg 等样式 */
.page-logged-in { padding-bottom: 100px; }
</style>
