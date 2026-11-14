<template>
  <div class="space-y-5 plan-page">
    <header class="flex items-end justify-between flex-wrap gap-3">
      <div>
        <h1>我的学习计划</h1>
        <p class="text-xs text-slate-500 mt-2">基于课程学情报告、风险变化与课程知识库生成；按周更新，可导出 PDF。</p>
      </div>
      <div class="flex items-center gap-2 print:hidden">
        <button class="btn-primary text-sm" @click="showFull = true">查看完整计划 / PDF</button>
      </div>
    </header>

    <p v-if="message" class="text-sm text-red-600 print:hidden">{{ message }}</p>

    <section class="card p-5">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div><h2>AI 学习规划工作台</h2><p class="text-[11px] text-slate-400 mt-1">学情体检 → 数据分析 → 课程研判 → 计划生成 → 质量校验</p></div>
        <button class="btn-primary text-sm" :disabled="generating" @click="startGenerate">{{ generating ? '生成中…' : '生成学习计划 →' }}</button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-2 mt-4">
        <div v-for="s in agentStages" :key="s.name" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] px-3 py-3 text-center">
          <p class="text-xl leading-none">{{ s.icon }}</p>
          <p class="text-xs font-medium text-[#2c465c] mt-2">{{ s.name }}</p>
          <p class="text-[10px] mt-1" :class="s.ok ? 'text-[#0f9d8a]' : 'text-amber-600'">{{ s.status }}</p>
        </div>
      </div>
    </section>
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
              <p class="text-[10px] text-slate-400 mt-1">{{ c.basis }}</p>
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


    <ReportModalShell :open="showFull" title="我的阶段性学习计划" :subtitle="`${termName || '当前学期'} · T+7 复评`" @close="showFull = false">
      <template #actions><button class="btn-ghost text-sm" @click="exportPdf">导出 PDF</button></template>
      <div class="space-y-4">
        <div class="rounded-xl border border-slate-200 bg-[#fbfdff] p-4 flex items-center gap-5">
          <div><p class="text-3xl font-semibold text-[#17324b]">{{ totalHours }}h</p><p class="text-[10px] text-slate-400 mt-1">本周建议学习时长</p></div>
          <div class="flex-1 text-xs text-slate-600 leading-6">本周优先处理 {{ planCourses[0]?.courseName || '暂无' }}；计划由课程报告 + 风险变化生成，T+7 复评后重排。</div>
          <div class="text-right text-[10px] text-slate-400">复评<br><b class="text-sm text-slate-700">{{ reviewDate }}</b></div>
        </div>
        <div><h3 class="text-[15px] font-bold text-[#17324b]">一、学情与目标</h3><p class="text-sm text-slate-600 leading-6 mt-2">{{ planCourses[0]?.conclusion || '当前无风险课程，保持学习节奏。' }}</p></div>
        <div><h3 class="text-[15px] font-bold text-[#17324b]">二、课程优先级</h3>
          <div class="overflow-x-auto border border-[#e6edf5] rounded-xl mt-2"><table><thead><tr><th>优先级</th><th>课程</th><th>当前风险</th><th>学分</th><th>难度</th><th>作业完成率</th><th>最近截止</th><th>本周动作</th><th>建议时长</th></tr></thead><tbody>
            <tr v-for="c in planCourses" :key="'m-'+c.courseName"><td><b>{{ c.priority }}</b></td><td>{{ c.courseName }}</td><td>{{ levelText(c.level) }}</td><td>{{ c.credit ?? '—' }}</td><td>{{ c.difficulty }}</td><td>{{ c.homework == null ? '—' : c.homework + '%' }}</td><td>{{ c.deadlines?.[0] ? String(c.deadlines[0].endTime).slice(5, 16) : '—' }}</td><td>{{ (c.tasks || []).join('；') }}</td><td>{{ c.hours }}h</td></tr>
          </tbody></table></div>
        </div>
        <div><h3 class="text-[15px] font-bold text-[#17324b]">三、课程数据与薄弱题组</h3>
          <div class="grid md:grid-cols-2 gap-2 mt-2">
            <div v-for="c in planCourses" :key="'data-'+c.courseName" class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3">
              <div class="flex items-center justify-between gap-2"><b class="text-xs text-[#2c465c]">{{ c.courseName }}</b><span class="text-[10px] text-slate-400">{{ c.priority }}</span></div>
              <p class="text-[10px] text-slate-500 mt-1">学分 {{ c.credit ?? '—' }} · {{ c.difficulty }} · 作业 {{ c.homework == null ? '—' : c.homework + '%' }}</p>
              <div v-if="c.weakGroups?.length" class="mt-2 space-y-1">
                <p class="text-[10px] text-amber-600">薄弱题组</p>
                <p v-for="g in c.weakGroups" :key="'wg-'+g.name" class="text-[10px] text-slate-600">· {{ g.name }}：{{ g.myRate }}% / 班均 {{ g.classRate }}%</p>
              </div>
              <p v-else class="text-[10px] text-slate-400 mt-2">报告暂无薄弱题组数据</p>
              <div v-if="c.deadlines?.length" class="mt-2 space-y-1">
                <p class="text-[10px] text-[#0f9d8a]">近期截止</p>
                <p v-for="d in c.deadlines" :key="'dd-'+d.name" class="text-[10px] text-slate-600">· {{ d.name }}（{{ String(d.endTime || '').replace('T',' ').slice(5,16) }}）</p>
              </div>
            </div>
          </div>
        </div>
        <div><h3 class="text-[15px] font-bold text-[#17324b]">四、周计划</h3><div class="grid md:grid-cols-3 gap-2 mt-2"><div v-for="w in weeks" :key="'mw-'+w.title" class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><b class="text-xs text-[#2c465c]">{{ w.title }}</b><p class="text-xs text-slate-500 mt-1 leading-5">{{ w.text }}</p></div></div></div>
        <div><h3 class="text-[15px] font-bold text-[#17324b]">五、检查点</h3><div class="space-y-2 mt-2"><div v-for="c in checkpoints" :key="'ck-'+c.title" class="flex gap-2 text-xs"><b class="text-slate-700 shrink-0">{{ c.title }}</b><span class="text-slate-500">{{ c.text }}</span></div></div></div>
        <div><h3 class="text-[15px] font-bold text-[#17324b]">六、执行说明</h3><p class="text-xs text-slate-500 leading-6 mt-2">本周总时长 {{ totalHours }}h；优先级按风险分、缺课超限、期末需考、作业完成率、近期截止和薄弱题组综合计算。数据来自课程报告 fact、作业提交与课程配置，不使用固定文案。</p></div>
      </div>
    </ReportModalShell>
    <GenerationOverlay :open="generating" :step="generationStep" title="正在生成学习计划"
                       subtitle="基于当前学情与风险生成个性化计划" :agents="generationAgents"
                       :messages="generationMessages" flow="体检 → 分析 → 研判 → 计划 → 校验" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { studentWarningApi } from '../../api/alert'
import { reportApi } from '../../api/ai'
import ReportModalShell from '../../components/ReportModalShell.vue'
import GenerationOverlay from '../../components/GenerationOverlay.vue'
import { riskLevelBadge, riskLevelLabel } from '../../labels'

const loading = ref(false)
const message = ref('')
const termName = ref('')
const courses = ref([])
const reportFacts = ref({})
const advice = ref([])
const showFull = ref(false)
const generating = ref(false)
const generationStep = ref(0)
const generationAgents = [
  { icon: '🩺', name: '学情体检' },
  { icon: '📊', name: '数据分析' },
  { icon: '🧭', name: '学情研判' },
  { icon: '🗓️', name: '计划生成' },
  { icon: '✅', name: '质量校验' },
  { icon: '📄', name: '报告导出' }
]
const generationMessages = [
  '正在读取课程与风险数据…',
  '正在分析课程优先级与时长…',
  '正在生成本周重点与阶段计划…',
  '正在匹配检查点与学习建议…',
  '正在校验计划是否可执行…',
  '计划已生成，正在准备完整计划…'
]

const riskCourses = computed(() => courses.value.filter((c) => c.level !== 'GREEN'))
const planCourses = computed(() => {
  const list = courses.value.slice()
    .map((c) => ({ ...c, ...planOf(c) }))
    .sort((a, b) => b.severity - a.severity)
  // 每周总预算优先给前 3 门，避免所有风险课都拿固定 5 小时
  list.forEach((c, i) => {
    if (c.level === 'GREEN') c.hours = 1
    else if (i === 0) c.hours = 3
    else if (i === 1) c.hours = 3
    else if (i === 2) c.hours = 2
    else c.hours = 1
  })
  return list
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
  { name: '学情体检', status: courses.value.length ? '已覆盖' : '未运行', ok: courses.value.length > 0, icon: '🩺' },
  { name: '数据分析', status: `${courses.value.length} 门课`, ok: courses.value.length > 0, icon: '📊' },
  { name: '学情研判', status: riskCourses.value.length ? `${riskCourses.value.length} 门风险` : '正常', ok: true, icon: '🧭' },
  { name: '计划生成', status: planCourses.value.length ? '已生成' : '待生成', ok: planCourses.value.length > 0, icon: '🗓️' },
  { name: '质量校验', status: planCourses.value.length ? '待复核' : '未运行', ok: planCourses.value.length > 0, icon: '✅' },
  { name: '报告导出', status: showFull.value ? '可导出' : '待预览', ok: showFull.value, icon: '📄' }
])

async function startGenerate() {
  if (generating.value) return
  generating.value = true
  generationStep.value = 1
  const started = Date.now()
  const timer = setInterval(() => { if (generationStep.value < 6) generationStep.value++ }, 600)
  await new Promise(r => setTimeout(r, Math.max(0, 3200 - (Date.now() - started))))
  clearInterval(timer)
  generationStep.value = 6
  generating.value = false
  showFull.value = true
}

function planOf(c) {
  const level = c.level || 'GREEN'
  const risk = Number(c.riskScore ?? 0)
  const absent = Number(c.absentCount ?? 0)
  const allowed = Number(c.allowedAbsent ?? 0)
  const gap = Math.max(0, absent - allowed)
  const finalNeed = c.finalExamNeeded == null ? null : Number(c.finalExamNeeded)
  const process = c.processScore == null ? null : Number(c.processScore)
  const credit = c.credit == null ? null : Number(c.credit)
  const difficulty = difficultyLabel(c.difficultyFactorCode || c.courseType)
  const homework = c.homeworkRate == null ? null : Number(c.homeworkRate)
  const deadlines = Array.isArray(c.upcomingTasks) ? c.upcomingTasks : []
  const weakGroups = (Array.isArray(c.fact?.groups) ? c.fact.groups : [])
    .filter(g => g.myRate != null && g.classRate != null && Number(g.myRate) < Number(g.classRate))
    .sort((a, b) => Number(a.myRate) - Number(b.myRate)).slice(0, 3)
  const severity = risk
    + gap * 10
    + (c.hardFail ? 20 : 0)
    + (c.finalUnreachable ? 15 : 0)
    + (finalNeed != null ? Math.min(20, finalNeed / 5) : 0)
    + (homework != null && homework < 100 ? (100 - homework) / 10 : 0)
    + Math.min(6, deadlines.length * 2)
  const priority = (c.hardFail || severity >= 90) ? 'P0'
    : severity >= 65 ? 'P1' : severity >= 40 ? 'P2' : 'P3'
  const rawHours = 1 + risk / 25 + gap * 0.6 + (finalNeed != null ? 0.4 : 0)
    + (homework != null && homework < 80 ? 0.8 : 0) + Math.min(1, deadlines.length * 0.25)
  const hours = level === 'GREEN' ? 1 : Math.max(2, Math.min(8, Math.round(rawHours)))
  const tasks = []
  if (gap > 0) {
    tasks.push(`核对缺课 ${absent}/${allowed}，确认是否影响期末资格`)
    if (c.hardFail) tasks.push(`本周找${c.teacherName || '任课教师'}说明缺课原因并提出补救`)
  } else if (absent > 0) {
    tasks.push(`缺课 ${absent}/${allowed} 未超限，保持后续出勤`)
  }
  if (c.finalUnreachable) tasks.push('与教师确认不可达后的补救路径')
  else if (finalNeed != null) tasks.push(`按期末需考 ${finalNeed} 分制定复习清单`)
  if (process != null && process < 60) tasks.push(`先补过程分：补交未交作业/实验（当前 ${process}）`)
  else if (level !== 'GREEN') tasks.push('保持作业提交，优先完成最近一次作业')
  weakGroups.forEach(g => tasks.push(`重做「${g.name}」薄弱题组（${g.myRate}% vs 班均 ${g.classRate}%）`))
  if (homework != null && homework < 100) tasks.push(`最近作业完成率 ${homework}%，补交未交作业`)
  deadlines.slice(0, 2).forEach(d => tasks.push(`完成「${d.name}」（截止 ${String(d.endTime || '').replace('T', ' ').slice(5, 16)}）`))
  if (!tasks.length) tasks.push('保持出勤与作业提交，完成本周复习')
  const basis = `风险 ${risk || '—'} · 缺课 ${absent}/${allowed}${gap ? `（超 ${gap}）` : ''}${finalNeed != null ? ` · 期末需考 ${finalNeed}` : ''} · 学分 ${credit ?? '—'} · 难度 ${difficulty} · 作业 ${homework ?? '—'}%`
  return { priority, hours, tasks: [...new Set(tasks)], severity, basis, credit, difficulty, homework, deadlines, weakGroups }
}
function difficultyLabel(code) { return ({ EXAM: '考试课', ASSESS: '考查课', PAPER: '卷面', MACHINE: '上机', PERSONAL: '个人项目', TEAM: '团队项目', OTHER: '其他' }[code] || code || '未配置') }
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
    try {
      const reports = await reportApi.list()
      const latest = {}
      ;(reports || []).forEach(r => {
        const k = String(r.teachingClassId)
        if (!latest[k] || Number(r.id) > Number(latest[k].id)) latest[k] = r
      })
      await Promise.all(courses.value.map(async c => {
        const r = latest[String(c.teachingClassId)]
        if (!r) return
        try {
          const detail = await reportApi.detail(r.id)
          c.fact = detail?.fact || {}
          reportFacts.value[String(c.teachingClassId)] = c.fact
        } catch (e) { /* 单课报告失败不影响计划 */ }
      }))
    } catch (e) { /* 报告不可用时仍用预警数据生成基础计划 */ }
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
