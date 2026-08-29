<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth.js'
import { getJson } from '../../api/http.js'

const router = useRouter()
const { teacherToken, teacher, isTeacherLoggedIn } = useAuth()

const teacherCourses = ref([])
const loading = ref(false)
const message = ref('')

onMounted(() => {
  if (!isTeacherLoggedIn.value) { router.replace('/teacher/login'); return }
  loadTeacherCourses()
})

async function loadTeacherCourses() {
  loading.value = true
  try {
    const res = await getJson('/teacher/my-courses', teacherToken.value)
    if (res.success) { teacherCourses.value = res.data || []; message.value = `共 ${teacherCourses.value.length} 门课程` }
    else { message.value = res.message || '获取课程失败' }
  } catch { message.value = '获取课程失败，请检查网络' }
  finally { loading.value = false }
}

function selectCourse(course) {
  localStorage.setItem('selectedCourse', JSON.stringify(course))
  router.push(`/teacher/course/${course.courseId}`)
}
</script>

<template>
  <div class="course-select-page" v-if="isTeacherLoggedIn">
    <div class="course-select-header">
      <div class="teacher-info-bar">
        <div class="teacher-info-bar-avatar">{{ teacher?.teacherName?.[0] || 'T' }}</div>
        <div class="teacher-info-bar-text">
          <strong>{{ teacher?.teacherName || teacher?.teacherNo }}</strong>
          <span>工号：{{ teacher?.teacherNo }}</span>
          <span v-if="teacher?.phone">📱 {{ teacher.phone }}</span>
        </div>
      </div>
    </div>

    <div v-if="teacherCourses.length" class="course-cards">
      <button v-for="course in teacherCourses" :key="course.courseId"
              class="course-card-btn" @click="selectCourse(course)">
        <div class="course-card-icon">📖</div>
        <div class="course-card-name">{{ course.courseName }}</div>
        <div class="course-card-stats">
          <span class="course-stat">👨‍🎓 {{ course.studentCount }} 人</span>
          <span class="course-stat" :class="{ 'has-alert': course.alertCount > 0 }">🚨 {{ course.alertCount }}</span>
        </div>
        <div class="course-card-arrow">→</div>
      </button>
    </div>

    <div v-else-if="loading" class="course-empty"><span class="spinner"></span> 加载中...</div>
    <div v-else class="course-empty">
      <p>暂无课程数据</p>
      <button class="btn-ghost" @click="loadTeacherCourses">重新加载</button>
    </div>
  </div>
</template>

<style scoped>
/* TeacherCourses — 课程选择页，继承全局 CSS 中的 .course-select-page / .course-cards 等 */
.course-select-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.course-select-header { margin-bottom: 24px; }
</style>
