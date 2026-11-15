<template>
  <div class="space-y-4">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <button class="text-sm text-slate-400 hover:text-slate-600"
                  @click="$router.push(offeringId ? { path: '/teacher/course/' + offeringId } : '/teacher/courses')">
            ← 返回{{ offeringId ? '课程详情' : '全部课程' }}
          </button>
          <h1>教学详情</h1>
          <span class="tag bg-white text-slate-500 border border-[#e6edf5]">{{ className || ('教学班 #' + teachingClassId) }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2">{{ courseName || '课程' }} · 导学、目录、测验、作业、项目统一教学入口</p>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" @click="$router.push('/teacher/reports')">学情报告</button>
        <button class="btn-ghost text-sm"
                @click="$router.push({ path: '/teacher/attendance', query: { teachingClassId } })">考勤与归档</button>
      </div>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span><p class="text-[11px] text-slate-400">学生</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ stats?.studentCount ?? '—' }}</p><p class="text-[11px] text-slate-400 mt-2">教学班人数</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">出勤率</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ stats?.attendanceRate == null ? '—' : stats.attendanceRate + '%' }}</p><p class="text-[11px] text-slate-400 mt-2">已点名 {{ stats?.calledSessions ?? '—' }} / {{ stats?.effectiveSessions ?? '—' }}</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-indigo-300"></span><p class="text-[11px] text-slate-400">教学任务</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ taskCount }}</p><p class="text-[11px] text-slate-400 mt-2">资料 / 测验 / 作业 / 项目</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-300"></span><p class="text-[11px] text-slate-400">平均成绩</p><p class="text-[28px] leading-none font-semibold text-amber-600 mt-3">{{ stats?.avgScore ?? '未定稿' }}</p><p class="text-[11px] text-slate-400 mt-2">通过率 {{ stats?.passRate ?? '—' }}%</p></div>
    </section>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-[#0f9d8a] text-white shadow-sm' : 'bg-white text-slate-600 border border-[#e6edf5]'"
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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { teacherPortalApi } from '../../api/teacherPortal'
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
const stats = ref(null)
const card = ref(null)
const courseName = computed(() => card.value?.courseName || '')
const className = computed(() => {
  const c = (card.value?.classes || []).find(x => Number(x.teachingClassId) === Number(teachingClassId))
  return c ? c.classCode : ''
})
const taskCount = computed(() => {
  const s = stats.value
  if (!s) return 0
  return Number(s.materialCount || 0) + Number(s.quizCount || 0) + Number(s.homeworkCount || 0) + Number(s.projectCount || 0)
})

onMounted(async () => {
  const t = route.query.tab
  if (t && tabs.some((x) => x.key === t)) tab.value = t
  try {
    const h = await teacherPortalApi.home()
    card.value = (h.courses || []).find(c => (c.classes || []).some(x => Number(x.teachingClassId) === Number(teachingClassId))) || null
    if (card.value) stats.value = await teacherPortalApi.stats(teachingClassId)
  } catch (e) { message.value = e?.message || '教学数据加载失败'; ok.value = false }
})
</script>
