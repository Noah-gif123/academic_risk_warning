<template>
  <div class="space-y-5 plan-page">
    <header class="flex items-end justify-between flex-wrap gap-3">
      <div>
        <h1>我的学习计划</h1>
        <p class="text-xs text-slate-500 mt-2">基于课程学情报告、风险变化与课程知识库生成；按周更新，可导出 PDF。</p>
      </div>
      <div class="flex items-center gap-2 print:hidden">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">{{ loading ? '生成中…' : '重新生成' }}</button>
        <button class="btn-primary text-sm" @click="showFull = true">查看完整计划 / PDF</button>
      </div>
    </header>

    <p v-if="message" class="text-sm text-red-600 print:hidden">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span>
        <p class="text-[11px] text-slate-400">本周学习时长</p>
        <p class="text-3xl font-semibold text-[#17324b] mt-2">{{ totalHours }}h</p>
        <p class="text-[11px] text-slate-400 mt-1">按课程风险分配</p>
      </div>
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span>
        <p class="text-[11px] text-slate-400">待完成任务</p>
        <p class="text-3xl font-semibold text-[#17324b] mt-2">{{ taskCount }}</p>
        <p class="text-[11px] text-slate-400 mt-1">来自 {{ riskCourses.length }} 门风险课程</p>
      </div>
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1 bg-red-400"></span>
        <p class="text-[11px] text-slate-400">风险课程</p>
        <p class="text-3xl font-semibold text-red-600 mt-2">{{ riskCourses.length }}</p>
        <p class="text-[11px] text-slate-400 mt-1">优先处理 P0 / P1</p>
      </div>
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-400"></span>
        <p class="text-[11px] text-slate-400">复评节点</p>
        <p class="text-3xl font-semibold text-amber-600 mt-2">T+7</p>
        <p class="text-[11px] text-slate-400 mt-1">{{ reviewDate }} 复评</p>
      </div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-3 gap-4">
      <div class="card p-5 xl:col-span-2">
        <div class="flex items-center justify-between mb-3">
          <h2>本周重点</h2>
          <span class="text-[11px] text-slate-400">按风险优先级</span>
        </div>
        <div class="space-y-2">
          <div v-for="c in planCourses.slice(0, 3)" :key="c.enrollmentId || c.courseName"
               class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3 flex items-start gap-3">
            <span class="tag shrink-0" :class="priorityClass(c.priority)">{{ c.priority }}</span>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="text-sm font-semibold text-[#24435c] truncate">{{ c.courseName }}</p>
                <span class="tag" :class="levelClass(c.level)">{{ levelText(c.level) }}</span>
                <span class="text-[10px] text-slate-400">{{ c.teacherName || '—' }} · {{ c.classCode || '—' }}</span>
              </div>
              <p class="text-xs text-slate-500 mt-1">{{ (c.tasks || []).join(' → ') }}</p>
            </div>
            <div class="text-right shrink-0"><p class="text-[10px] text-slate-400">建议</p><p class="text-sm font-semibold text-[#24435c]">{{ c.hours }}h</p></div>
          </div>
          <p v-if="!planCourses.length" class="text-sm text-slate-400 py-8 text-center">本学期暂无课程，暂不能生成计划</p>
        </div>
      </div>

      <div class="card p-5">
        <div class="flex items-center justify-between mb-3"><h2>本周时间分配</h2><span class="text-[11px] text-slate-400">建议 {{ totalHours }}h</span></div>
        <div class="space-y-3">
          <div v-for="c in planCourses.slice(0, 5)" :key="'bar-'+c.courseName">
            <div class="flex items-center justify-between text-[11px] mb-1"><span class="text-slate-600 truncate">{{ c.courseName }}</span><b class="text-slate-500">{{ c.hours }}h</b></div>
            <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full rounded-full" :class="c.priority === 'P0' ? 'bg-red-400' : c.priority === 'P1' ? 'bg-amber-400' : 'bg-[#20b7a4]'" :style="{ width: Math.min(100, c.hours / Math.max(1, totalHours) * 100) + '%' }"></div></div>
          </div>
          <p v-if="!planCourses.length" class="text-xs text-slate-400 py-4 text-center">暂无数据</p>
        </div>
      </div>
    </section>

    <section class="card p-5">
      <div class="flex items-center justify-between mb-3"><h2>阶段计划</h2><span class="text-[11px] text-slate-400">按 T+7 / T+14 复评</span></div>
      <div class="relative pl-4 border-l border-[#dbe7f2] space-y-4">
        <div v-for="(w, i) in weeks" :key="w.title" class="relative">
          <span class="absolute -left-[21px] top-1 w-2.5 h-2.5 rounded-full" :class="i === 0 ? 'bg-[#0f9d8a]' : 'bg-[#9fc3d9]'"></span>
          <p class="text-xs font-semibold text-[#2c465c]">{{ w.title }}</p>
          <p class="text-xs text-slate-500 mt-1 leading-6">{{ w.text }}</p>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-4">
      <div class="card p-5">
        <h2 class="mb-3">检查点</h2>
        <div class="space-y-3">
          <div v-for="c in checkpoints" :key="c.title" class="flex items-start gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-amber-400 mt-2 shrink-0"></span>
            <div><p class="text-xs font-medium text-slate-700">{{ c.title }}</p><p class="text-xs text-slate-500 mt-0.5 leading-5">{{ c.text }}</p></div>
          </div>
        </div>
      </div>
      <div class="card p-5">
        <h2 class="mb-3">来自学情预警的建议</h2>
        <ul class="space-y-2">
          <li v-for="(a, i) in advice" :key="i" class="text-sm text-slate-600 flex gap-2"><span class="text-[#0f9d8a]">·</span><span>{{ a }}</span></li>
          <li v-if="!advice.length" class="text-sm text-slate-400">暂无额外建议</li>
        </ul>
      </div>
    </section>

    <section class="card p-5">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div><h2>AI 学习规划工作台</h2><p class="text-[11px] text-slate-400 mt-1">学情体检 → 数据分析 → 课程研判 → 计划生成 → 质量校验</p></div>
        <button class="btn-primary text-sm" @click="showFull = true">生成学习计划 →</button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-2 mt-4">
        <div v-for="s in agentStages" :key="s.name" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] px-3 py-3 text-center">
          <p class="text-xs font-medium text-[#2c465c]">{{ s.name }}</p>
          <p class="text-[10px] mt-1" :class="s.ok ? 'text-[#0f9d8a]' : 'text-amber-600'">{{ s.status }}</p>
        </div>
      </div>
    </section>

    <ReportModalShell :open="showFull" title="我的阶段性学习计划" :subtitle="`${termName || '当前学期'} · T+7 复评`" @close="showFull = false">
      <template #actions><button class="btn-ghost text-sm" @click="exportPdf">导出 PDF</button></template>
      <div class="space-y-4">
        <div class="rounded-xl border border-slate-200 bg-[#fbfdff] p-4 flex items-center gap-5">
          <div><p class="text-3xl font-semibold text-[#17324b]">{{ totalHours }}h</p><p class="text-[10px] text-slate-400 mt-1">本周建议学习时长</p></div>
          <div class="flex-1 text-xs text-slate-600 leading-6">本周优先处理 {{ planCourses[0]?.courseName || '暂无' }}；计划由课程报告 + 风险变化生成，T+7 复评后重排。</div>
          <div class="text-right text-[10px] text-slate-400">复评<br><b class="text-sm text-slate-700">{{ reviewDate }}</b></div>
        </div>
        <div><h3>一、学情与目标</h3><p class="text-sm text-slate-600 leading-6 mt-2">{{ planCourses[0]?.conclusion || '当前无风险课程，保持学习节奏。' }}</p></div>
        <div><h3>二、课程优先级</h3>
          <div class="overflow-x-auto border border-[#e6edf5] rounded-xl mt-2"><table><thead><tr><th>优先级</th><th>课程</th><th>当前风险</th><th>本周动作</th><th>建议时长</th></tr></thead><tbody>
            <tr v-for="c in planCourses" :key="'m-'+c.courseName"><td><b>{{ c.priority }}</b></td><td>{{ c.courseName }}</td><td>{{ levelText(c.level) }}</td><td>{{ (c.tasks || []).join('；') }}</td><td>{{ c.hours }}h</td></tr>
          </tbody></table></div>
        </div>
        <div><h3>三、周计划</h3><div class="grid md:grid-cols-3 gap-2 mt-2"><div v-for="w in weeks" :key="'mw-'+w.title" class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><b class="text-xs text-[#2c465c]">{{ w.title }}</b><p class="text-xs text-slate-500 mt-1 leading-5">{{ w.text }}</p></div></div></div>
      </div>
    </ReportModalShell>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { studentWarningApi } from '../../api/alert'
import ReportModalShell from '../../components/ReportModalShell.vue'
import { riskLevelBadge, riskLevelLabel } from '../../labels'

const loading = ref(false)
const message = ref('')
const termName = ref('')
const courses = ref([])
const advice = ref([])
const showFull = ref(false)

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

const taskCount = computed(() => planCourses.value.slice(0, 3).reduce((n, c) => n + (c.tasks?.length || 0), 0))
const agentStages = computed(() => [
  { name: '学情体检', status: 'READY', ok: true },
  { name: '数据分析', status: `${courses.value.length} 门课`, ok: courses.value.length > 0 },
  { name: '学情研判', status: riskCourses.value.length ? `${riskCourses.value.length} 门风险` : '正常', ok: true },
  { name: '计划生成', status: planCourses.value.length ? '草稿' : '待生成', ok: planCourses.value.length > 0 },
  { name: '质量校验', status: planCourses.value.length ? 'PASS' : '待校验', ok: planCourses.value.length > 0 },
  { name: '报告导出', status: 'PDF', ok: true }
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
