<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600"
                @click="$router.push(offeringId ? { path: '/teacher/course/' + offeringId } : '/teacher/courses')">
          ← 返回{{ offeringId ? '课程详情' : '全部课程' }}
        </button>
        <h1 class="text-lg font-semibold text-slate-800">教学详情</h1>
        <span class="tag bg-slate-100 text-slate-500">教学班 #{{ teachingClassId }}</span>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm"
                @click="$router.push({ path: '/teacher/attendance', query: { teachingClassId } })">考勤与归档</button>
        <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
      </div>
    </div>

    <p class="text-xs text-slate-400">
      每位老师独立维护自己教学班的内容：导学与教学目标、目录（课前 / 课中 / 课后挂资料与任务）、
      测验与作业（导入 → 校对 → 发布）、项目（收文件、给总分、贡献度只读）。
    </p>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key">{{ t.label }}</button>
    </div>

    <!-- 导学 / 教学目标 / 目录 / 资料库 都用课程内容页，只是初始页签不同（页签由本页统一控制） -->
    <CourseContent v-if="['guide', 'objectives', 'lessons', 'files'].includes(tab)"
                   :key="tab"
                   :teaching-class-id="teachingClassId"
                   :offering-id="offeringId"
                   :initial-tab="tab"
                   embedded />

    <!-- 测验 / 作业：同一套试卷管理，按类型过滤 -->
    <Papers v-if="tab === 'quiz' || tab === 'homework'"
            :key="tab"
            :teaching-class-id="teachingClassId"
            :offering-id="offeringId"
            :paper-type-filter="tab === 'quiz' ? 'QUIZ' : 'HOMEWORK'"
            embedded />

    <!-- 项目 -->
    <Projects v-if="tab === 'project'" :teaching-class-id="teachingClassId" embedded />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import CourseContent from './CourseContent.vue'
import Papers from './Papers.vue'
import Projects from './Projects.vue'

const route = useRoute()
const teachingClassId = Number(route.query.teachingClassId || 1)
const offeringId = Number(route.query.offeringId || 0)

const tabs = [
  { key: 'guide', label: '导学' },
  { key: 'objectives', label: '教学目标' },
  { key: 'lessons', label: '目录' },
  { key: 'files', label: '资料库' },
  { key: 'quiz', label: '测验' },
  { key: 'homework', label: '作业' },
  { key: 'project', label: '项目' }
]
const tab = ref('guide')
const message = ref('')
const ok = ref(true)

onMounted(() => {
  const t = route.query.tab
  if (t && tabs.some((x) => x.key === t)) tab.value = t
})
</script>
