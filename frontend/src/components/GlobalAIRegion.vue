<template>
  <!-- 全局 AI 右侧面板区域 — 位于 AppLayout 主体的 flex 流中 -->
  <div class="global-ai-region" v-if="showAssistant">
    <!-- ===== 教师 AI 助手 ===== -->
    <ChatAssistant
      v-if="isTeacher"
      ref="teacherAssistantRef"
      :token="teacherToken"
      :user-name="ctx.teacherUserName.value"
      :teacher-id="ctx.teacherId.value"
      :course-id="ctx.courseId.value"
      :course-name="ctx.courseName.value"
      @close="onAssistantClosed"
    />

    <!-- ===== 学生 AI 助手 ===== -->
    <StudentChatAssistant
      v-else-if="isStudent"
      ref="studentAssistantRef"
      :token="studentToken"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useAuth } from '../composables/useAuth.js'
import { useAIContext } from '../composables/useAIContext.js'
import ChatAssistant from './ChatAssistant.vue'
import StudentChatAssistant from './StudentChatAssistant.vue'

const auth = useAuth()
const ctx = useAIContext()

const teacherAssistantRef = ref(null)
const studentAssistantRef = ref(null)

const isTeacher = computed(() => auth.isTeacherLoggedIn.value)
const isStudent = computed(() => auth.isStudentLoggedIn.value)
const teacherToken = computed(() => auth.teacherToken.value)
const studentToken = computed(() => auth.studentToken.value)

// 只有教师（在课程详情页）和学生登录时才显示 AI 助手
const showAssistant = computed(() => {
  if (auth.isTeacherLoggedIn.value && ctx.courseId.value) return true
  if (auth.isStudentLoggedIn.value) return true
  return false
})

/** 通过 ref 打开对应助手（供 AppLayout 顶栏按钮调用） */
function open() {
  if (isTeacher.value && teacherAssistantRef.value) {
    teacherAssistantRef.value.open()
  } else if (isStudent.value && studentAssistantRef.value) {
    studentAssistantRef.value.open()
  }
}

/** 关闭助手 */
function close() {
  if (isTeacher.value && teacherAssistantRef.value) {
    teacherAssistantRef.value.close()
  } else if (isStudent.value && studentAssistantRef.value) {
    studentAssistantRef.value.close()
  }
}

/** 切换展开/收起 */
function toggle() {
  const ref = isTeacher.value ? teacherAssistantRef.value : studentAssistantRef.value
  if (!ref) return
  if (ref.isOpen) {
    ref.close()
  } else {
    ref.open()
  }
}

/** 助手是否处于展开状态 */
function isOpen() {
  const ref = isTeacher.value ? teacherAssistantRef.value : studentAssistantRef.value
  return ref ? ref.isOpen : false
}

// 当教师切换课程时，如果助手已打开，保持打开
watch(() => ctx.courseId.value, (newId, oldId) => {
  if (newId && newId !== oldId && teacherAssistantRef.value?.isOpen) {
    // 课程变了助手继续开着
  }
})

// 助手组件内部的关闭按钮会调用此函数
function onAssistantClosed() {
  // ChatAssistant 内部 close() 已处理 UI 状态，此处仅做同步
}

defineExpose({ open, close, toggle, isOpen })
</script>

<style scoped>
.global-ai-region {
  display: flex;
  height: 100%;
  overflow: hidden;
}
</style>
