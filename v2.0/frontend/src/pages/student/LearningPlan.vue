<template>
  <div class="space-y-4 plan-page">
    <div class="flex items-end justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-xl font-semibold text-slate-800">学习计划</h1>
        <p class="text-xs text-slate-400 mt-1">
          基于当前学情预警、课程风险和成绩数据生成 · 不替代辅导员/任课教师建议
        </p>
      </div>
      <div class="flex items-center gap-2 print:hidden">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">
          {{ loading ? '加载中…' : '重新生成' }}
        </button>
        <button class="btn-primary text-sm" @click="exportPdf">导出 PDF</button>
      </div>
    </div>

    <p v-if="message" class="text-sm text-red-600 print:hidden">{{ message }}</p>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <div class="card p-4">
        <p class="text-xs text-slate-400">本学期课程</p>
        <p class="text-3xl font-semibold text-slate-800 mt-2">{{ courses.length }}</p>
        <p class="text-xs text-slate-400 mt-1">{{ termName || '当前学期' }}</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">风险课程</p>
        <p class="text-3xl font-semibold text-red-600 mt-2">{{ riskCourses.length }}</p>
        <p class="text-xs text-slate-400 mt-1">优先处理 P0 / P1</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">本周建议时长</p>
        <p class="text-3xl font-semibold text-brand-600 mt-2">{{ totalHours }}h</p>
        <p class="text-xs text-slate-400 mt-1">按课程风险分配</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">复评节点</p>
        <p class="text-3xl font-semibold text-amber-600 mt-2">T+7</p>
        <p class="text-xs text-slate-400 mt-1">{{ reviewDate }} 复评</p>
      </div>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-sm font-medium text-slate-700">课程优先级</h2>
        <span class="text-xs text-slate-400">P0 最高 · 按风险等级排序</span>
      </div>
      <div class="space-y-2">
        <div v-for="c in planCourses" :key="c.enrollmentId || c.courseName"
             class="border border-slate-200 rounded-xl p-3 flex items-start gap-3">
          <span class="tag shrink-0" :class="priorityClass(c.priority)">{{ c.priority }}</span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <p class="text-sm font-medium text-slate-800 truncate">{{ c.courseName }}</p>
              <span class="tag" :class="levelClass(c.level)">{{ levelText(c.level) }}</span>
              <span class="text-[10px] text-slate-400">{{ c.teacherName || '—' }} · {{ c.classCode || '—' }}</span>
            </div>
            <p class="text-xs text-slate-500 mt-1">{{ c.conclusion || '暂无结论' }}</p>
            <div class="flex flex-wrap gap-1.5 mt-2">
              <span v-for="(t, i) in c.tasks" :key="i" class="text-[10px] rounded-full px-2 py-0.5 bg-slate-50 text-slate-500">{{ t }}</span>
            </div>
          </div>
          <div class="text-right shrink-0">
            <p class="text-[10px] text-slate-400">建议时长</p>
            <p class="text-sm font-medium text-slate-700">{{ c.hours }}h</p>
          </div>
        </div>
        <p v-if="!planCourses.length" class="text-sm text-slate-400 py-8 text-center">本学期暂无课程，暂不能生成计划</p>
      </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
      <div class="card p-4">
        <h2 class="text-sm font-medium text-slate-700 mb-3">本周 / 下周安排</h2>
        <div class="space-y-3">
          <div v-for="w in weeks" :key="w.title" class="border-l-2 border-brand-300 pl-3">
            <p class="text-xs font-medium text-slate-700">{{ w.title }}</p>
            <p class="text-xs text-slate-500 mt-1 leading-6">{{ w.text }}</p>
          </div>
        </div>
      </div>
      <div class="card p-4">
        <h2 class="text-sm font-medium text-slate-700 mb-3">检查点</h2>
        <div class="space-y-3">
          <div v-for="c in checkpoints" :key="c.title" class="flex items-start gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-amber-400 mt-2 shrink-0"></span>
            <div>
              <p class="text-xs font-medium text-slate-700">{{ c.title }}</p>
              <p class="text-xs text-slate-500 mt-0.5 leading-5">{{ c.text }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card p-4">
      <h2 class="text-sm font-medium text-slate-700 mb-2">来自学情预警的建议</h2>
      <ul class="space-y-1.5">
        <li v-for="(a, i) in advice" :key="i" class="text-sm text-slate-600 flex gap-2">
          <span class="text-brand-500">·</span><span>{{ a }}</span>
        </li>
        <li v-if="!advice.length" class="text-sm text-slate-400">暂无额外建议</li>
      </ul>
      <p class="text-[10px] text-slate-400 mt-3 border-t border-dashed border-slate-200 pt-2">
        计划为建议稿：完成情况需要学生自评/教师确认；T+7 复评后按最新风险重新生成。
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { studentWarningApi } from '../../api/alert'
import { riskLevelBadge, riskLevelLabel } from '../../labels'

const loading = ref(false)
const message = ref('')
const termName = ref('')
const courses = ref([])
const advice = ref([])

const riskCourses = computed(() => courses.value.filter((c) => c.level !== 'GREEN'))
const planCourses = computed(() => {
  const rank = { RED: 0, ORANGE: 1, YELLOW: 2, GREEN: 3 }
  return courses.value.slice().sort((a, b) => (rank[a.level] ?? 9) - (rank[b.level] ?? 9))
    .map((c) => ({ ...c, ...planOf(c) }))
})
const totalHours = computed(() => planCourses.value.reduce((n, c) => n + c.hours, 0))
const reviewDate = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + 7)
  return `${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')}`
})
const weeks = computed(() => {
  const p0 = planCourses.value.find((c) => c.priority === 'P0')
  const p1 = planCourses.value.find((c) => c.priority === 'P1')
  return [
    { title: '本周', text: p0 ? `优先处理《${p0.courseName}》：${p0.tasks.join('、')}。` : '保持当前学习节奏，完成本周作业。' },
    { title: '下周', text: p1 ? `跟进《${p1.courseName}》：${p1.tasks.join('、')}；同时保持其余课程出勤。` : '复盘本周错题，按课程报告调整下一周计划。' },
    { title: '期末前', text: riskCourses.value.length ? '两门风险课程各完成一套模拟/复习清单，确认需考分变化。' : '保持出勤与作业提交，准备期末复习。' }
  ]
})
const checkpoints = computed(() => [
  { title: `T+7（${reviewDate.value}）`, text: '复评缺课是否停止、作业是否补齐；未完成则升级为每日检查。' },
  { title: 'T+14', text: '检查风险课程是否降级；根据最新学情预警重新生成本计划。' },
  { title: '期末前', text: '核对两门风险课程是否解除，确认考试/大作业安排。' }
])

function planOf(c) {
  const level = c.level || 'GREEN'
  const priority = level === 'RED' ? 'P0' : level === 'ORANGE' ? 'P1' : level === 'YELLOW' ? 'P2' : 'P3'
  const hours = level === 'RED' ? 5 : level === 'ORANGE' ? 4 : level === 'YELLOW' ? 2 : 1
  const tasks = []
  if ((c.absentCount ?? 0) > 0) tasks.push('核对缺课记录')
  if (c.finalExamNeeded != null && !c.finalUnreachable) tasks.push('确认期末需考分')
  if (c.finalUnreachable) tasks.push('与教师确认补救路径')
  tasks.push(level === 'GREEN' ? '保持作业提交' : '补交/完成作业')
  tasks.push('完成一次复习')
  return { priority, hours, tasks }
}
function levelText(l) { return riskLevelLabel(l) }
function levelClass(l) { return riskLevelBadge(l) }
function priorityClass(p) {
  return p === 'P0' ? 'bg-red-50 text-red-600' : p === 'P1' ? 'bg-amber-50 text-amber-600' : p === 'P2' ? 'bg-yellow-50 text-yellow-700' : 'bg-emerald-50 text-emerald-600'
}
function exportPdf() { window.print() }

async function load() {
  loading.value = true
  message.value = ''
  try {
    const d = await studentWarningApi.overview()
    termName.value = d?.termName || ''
    courses.value = d?.items || []
    advice.value = d?.advice || []
  } catch (e) {
    message.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
@media print {
  .plan-page { padding: 0 !important; }
  .plan-page button { display: none !important; }
}
</style>
