<template>
  <div class="space-y-5">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <h1>学情预警</h1>
          <span v-if="data.termName" class="tag bg-white text-slate-500 border border-[#e6edf5]">{{ data.termName }}</span>
          <span v-if="data.engineVersion" class="text-[11px] text-slate-400">引擎 {{ data.engineVersion }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2 max-w-3xl leading-6">结论由考勤与成绩自动判定；Agent 状态来自最近一次真实运行，未运行不会伪装成完成。</p>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新数据' }}</button>
        <button class="btn-primary text-sm" :disabled="!data.items?.length" @click="showReportPicker = true">生成报告</button>
      </div>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <div><h2>多智能体研判</h2><p class="text-[11px] text-slate-400 mt-1">本次运行 7 个角色 · 真实步骤可追溯</p></div>
        <span class="text-[11px] text-slate-400">最近报告 {{ dashboard.latestReport ? '#' + dashboard.latestReport.id : '暂无' }}</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-7 gap-2">
        <div v-for="(a, i) in dashboard.agent" :key="i" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] px-3 py-3">
          <p class="text-base leading-none">{{ agentIcons[i] || '•' }}</p>
          <p class="text-xs font-medium text-[#2c465c] truncate mt-2">{{ a.name }}</p>
          <p class="text-[10px] mt-1" :class="a.ok ? 'text-[#0f9d8a]' : 'text-slate-400'">{{ a.status }}</p>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div v-for="c in tiles" :key="c.key" class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1" :class="c.key === 'red' ? 'bg-red-400' : c.key === 'orange' ? 'bg-amber-400' : c.key === 'yellow' ? 'bg-yellow-400' : 'bg-emerald-400'"></span>
        <p class="text-[11px] text-slate-400">{{ c.label }}</p>
        <p class="text-[28px] leading-none font-semibold mt-3" :class="c.color">{{ c.value }}</p>
        <p class="text-[11px] text-slate-400 mt-2">{{ c.hint }}</p>
      </div>
    </section>

    <section class="card p-5">
      <div class="flex items-center justify-between mb-2"><h2>风险趋势</h2><span class="text-[11px] text-slate-400">最近 {{ trendRows.length }} 份课程报告总评</span></div>
      <div v-if="trendRows.length >= 2">
        <svg viewBox="0 0 640 180" class="w-full h-44">
          <line x1="0" y1="150" x2="640" y2="150" stroke="#e6edf5" />
          <polyline :points="trendPath" fill="none" stroke="#0f9d8a" stroke-width="2.5" />
          <circle v-for="(p, i) in trendPoints" :key="'tp-'+i" :cx="p.x" :cy="p.y" r="3.5" fill="#0f9d8a" />
        </svg>
        <div class="flex justify-between text-[10px] text-slate-400"><span v-for="t in trendRows" :key="'tl-'+t.id">{{ fmt(t.time).slice(5) }}</span></div>
      </div>
      <div v-else class="py-12 text-center text-sm text-slate-400">历史报告不足 2 份，暂不能画趋势；不会用假数据补线。</div>
    </section>

    <section class="grid md:grid-cols-2 gap-4">
      <div class="card p-5">
        <div class="flex items-center justify-between mb-3"><h2>预警分布</h2><span class="text-[11px] text-slate-400">按课程</span></div>
        <div class="flex items-center gap-4">
          <div class="w-28 h-28 rounded-full relative shrink-0" :style="{ background: donutStyle }"><div class="absolute inset-5 rounded-full bg-white"></div><div class="absolute inset-0 flex flex-col items-center justify-center"><b class="text-xl">{{ data.items?.length || 0 }}</b><span class="text-[9px] text-slate-400">课程</span></div></div>
          <div class="space-y-2 text-xs flex-1">
            <div v-for="d in distributionRows" :key="d.key" class="flex items-center justify-between"><span class="flex items-center gap-1.5 text-slate-500"><i class="w-2 h-2 rounded-full inline-block" :style="{ background: d.color }"></i>{{ d.label }}</span><b>{{ d.value }}</b></div>
          </div>
        </div>
        <div class="mt-4 pt-3 border-t border-[#eef4f9]">
          <p class="text-[10px] font-semibold text-slate-500 mb-2">优先处理</p>
          <div v-for="it in rankedItems.slice(0, 2)" :key="'prio-'+it.enrollmentId" class="flex items-center justify-between gap-2 py-1">
            <span class="text-[11px] text-slate-600 truncate">{{ it.courseName }}</span>
            <span class="text-[10px] text-red-500 shrink-0">{{ it.riskScore ?? '—' }} 分</span>
          </div>
          <p v-if="!rankedItems.length" class="text-[10px] text-slate-400">暂无风险课程</p>
        </div>
      </div>

      <div class="card p-5">
        <div class="flex items-center justify-between mb-2"><h2>课程风险排名</h2><span class="text-[11px] text-slate-400">按风险分排序</span></div>
        <div class="overflow-x-auto"><table><thead><tr><th>课程</th><th>等级</th><th class="text-right">风险分</th><th class="text-right">缺课</th><th class="text-right">报告</th></tr></thead><tbody>
          <tr v-for="it in rankedItems" :key="'rank-'+it.enrollmentId">
            <td class="max-w-[160px] truncate" :title="it.courseName">{{ it.courseName }}</td>
            <td><span class="tag" :class="riskLevelBadge(it.level)">{{ riskLevelLabel(it.level) }}</span></td>
            <td class="text-right font-medium">{{ it.riskScore ?? '—' }}</td>
            <td class="text-right" :class="it.hardFail ? 'text-red-600' : ''">{{ it.absentCount ?? 0 }} / {{ it.allowedAbsent ?? '—' }}</td>
            <td class="text-right"><button class="text-xs text-[#0f9d8a] hover:underline" @click="openReport(it)">预览</button></td>
          </tr>
        </tbody></table></div>
      </div>
    </section>

    <section>
      <div class="flex items-end justify-between mb-3"><div><h2>课程风险</h2><p class="text-[11px] text-slate-400 mt-1">按风险等级排序；课程报告是唯一详情入口</p></div><span class="text-[11px] text-slate-400">共 {{ data.items?.length || 0 }} 门</span></div>
      <div class="grid md:grid-cols-2 xl:grid-cols-3 gap-3">
        <article v-for="it in sortedItems" :key="it.enrollmentId" class="card p-4 relative overflow-hidden">
          <span class="absolute left-0 top-0 bottom-0 w-1" :class="it.level === 'RED' ? 'bg-red-400' : it.level === 'ORANGE' ? 'bg-amber-400' : it.level === 'YELLOW' ? 'bg-yellow-400' : 'bg-emerald-400'"></span>
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0"><p class="font-semibold text-[#24435c] truncate">{{ it.courseName }}</p><p class="text-[11px] text-slate-400 mt-1">{{ it.classCode }} · {{ it.teacherName }}</p></div>
            <span class="tag shrink-0" :class="riskLevelBadge(it.level)">{{ riskLevelLabel(it.level) }}</span>
          </div>
          <div class="mt-3 rounded-lg bg-[#f8fbfd] border border-[#eef4f9] px-3 py-2"><p class="text-xs leading-5" :class="textColor(it.level)">{{ it.conclusion }}</p></div>
          <div class="grid grid-cols-3 gap-2 mt-3">
            <div><p class="text-[10px] text-slate-400">缺课 / 上限</p><p class="text-sm font-semibold" :class="it.hardFail ? 'text-red-600' : 'text-slate-700'">{{ it.absentCount ?? 0 }} / {{ it.allowedAbsent ?? '—' }}</p></div>
            <div><p class="text-[10px] text-slate-400">过程分</p><p class="text-sm font-semibold text-slate-700">{{ it.processScore ?? '—' }}</p></div>
            <div><p class="text-[10px] text-slate-400">需考 / 还差</p><p class="text-sm font-semibold" :class="it.finalUnreachable ? 'text-red-600' : 'text-slate-700'">{{ it.finalUnreachable ? '不可达' : (it.finalExamNeeded ?? '—') }}</p></div>
          </div>
          <div class="flex items-center gap-2 mt-4 pt-3 border-t border-[#eef4f9]">
            <button class="btn-primary text-xs" @click="openReport(it)">预览</button>
            <button class="btn-ghost text-xs" @click="exportReport(it)">导出 PDF</button>
            <span v-if="it.teacherReply" class="tag bg-emerald-50 text-emerald-700">老师已回复</span>
            <span v-else-if="it.studentResponse" class="tag bg-amber-50 text-amber-700">已申诉·待核实</span>
          </div>
        </article>
      </div>
    </section>

    <section v-if="data.advice?.length" class="card p-5">
      <h2 class="mb-3">给你的建议</h2>
      <div class="grid md:grid-cols-3 gap-3"><div v-for="(a, i) in data.advice" :key="i" class="rounded-xl bg-[#f7fbfd] border border-[#eaf1f7] px-4 py-3"><p class="text-[10px] font-semibold text-[#0f9d8a] mb-1">建议 {{ i + 1 }}</p><p class="text-xs text-slate-600 leading-6">{{ a }}</p></div></div>
    </section>

    <section class="card p-4"><details><summary class="cursor-pointer">判定口径（怎么算出这个结论的）</summary><ul class="mt-3 space-y-2"><li v-for="(g, i) in data.guidelines" :key="i" class="text-xs text-slate-500 flex gap-2"><span class="text-slate-300">{{ i + 1 }}.</span><span>{{ g }}</span></li></ul></details></section>

    <Teleport to="body">
      <div v-if="showReportPicker" class="fixed inset-0 z-[60] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4" @click.self="showReportPicker = false">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[85vh] overflow-hidden flex flex-col">
          <div class="px-5 py-3 border-b border-[#e6edf5] flex items-center justify-between"><div><h2>选择要生成报告的课程</h2><p class="text-[11px] text-slate-400 mt-1">选择一门课程后进入报告生成</p></div><button class="text-slate-400" @click="showReportPicker = false">✕</button></div>
          <div class="p-4 overflow-y-auto space-y-2">
            <button v-for="it in rankedItems" :key="'pick-'+it.enrollmentId" class="w-full text-left rounded-xl border border-[#e6edf5] p-3 hover:border-[#0f9d8a] hover:bg-[#f7fbfd]" @click="showReportPicker = false; openReport(it, true)">
              <div class="flex items-center justify-between gap-2"><span class="font-medium text-sm text-[#24435c]">{{ it.courseName }}</span><span class="tag" :class="riskLevelBadge(it.level)">{{ riskLevelLabel(it.level) }}</span></div>
              <p class="text-[11px] text-slate-400 mt-1">{{ it.classCode }} · {{ it.teacherName }} · 缺课 {{ it.absentCount ?? 0 }}/{{ it.allowedAbsent ?? '—' }}</p>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <StudentReportModal :open="reportOpen" :student="reportStudent" :courses="reportCourses" :generate-on-open="reportGenerateMode" mode="student" @close="reportOpen = false; reportGenerateMode = false" />
  </div>
</template>

<script setup>
import AlertExplainCard from '../../components/AlertExplainCard.vue'
import CourseQaBox from '../../components/CourseQaBox.vue'
import StudentReportModal from '../../components/StudentReportModal.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { studentWarningApi } from '../../api/alert'
import { reportApi } from '../../api/ai'
import { outputModeLabel, riskLevelBadge, riskLevelLabel } from '../../labels'

const loading = ref(false)
const appealing = ref(false)
const message = ref('')
const ok = ref(true)

const data = reactive({ studentNo: '', studentName: '', termName: '', engineVersion: '', lastEvalTime: null, summary: {}, items: [], advice: [], guidelines: [] })
const dashboard = ref({ agent: [], trend: [], distribution: {}, latestReport: null })
const appealReason = ref('')
const reportOpen = ref(false)
const reportStudent = ref(null)
const reportCourses = ref([])
const reportGenerateMode = ref(false)
const showReportPicker = ref(false)
const agentIcons = ['🩺', '📊', '🧭', '📚', '💡', '📄', '✅']

const tiles = computed(() => {
  const s = data.summary || {}
  return [
    { key: 'red', label: '红色预警', value: s.red ?? 0, color: 'text-red-600', hint: '缺课超限或期末不可达' },
    { key: 'orange', label: '橙色预警', value: s.orange ?? 0, color: 'text-orange-500', hint: '风险偏高，需要改善' },
    { key: 'yellow', label: '黄色预警', value: s.yellow ?? 0, color: 'text-amber-600', hint: '存在风险，注意保持' },
    { key: 'green', label: '正常课程', value: s.green ?? 0, color: 'text-emerald-600', hint: '在安全区内' }
  ]
})

const rankedItems = computed(() => [...(data.items || [])].sort((a, b) => Number(b.riskScore ?? 0) - Number(a.riskScore ?? 0)))
const sortedItems = computed(() => [...(data.items || [])].sort((a, b) => {
  const rank = { RED: 0, ORANGE: 1, YELLOW: 2, GREEN: 3 }
  return (rank[a.level] ?? 9) - (rank[b.level] ?? 9)
}))
const trendRows = computed(() => dashboard.value.trend || [])
const trendPoints = computed(() => trendRows.value.map((r, i) => ({
  x: trendRows.value.length === 1 ? 320 : 10 + i * 620 / (trendRows.value.length - 1),
  y: 150 - Math.max(0, Math.min(100, Number(r.score ?? 0))) * 1.3
})))
const trendPath = computed(() => trendPoints.value.map(p => `${p.x},${p.y}`).join(' '))
const distributionRows = computed(() => {
  const s = data.summary || {}
  return [
    { key: 'red', label: '红色', value: s.red ?? 0, color: '#ef4444' },
    { key: 'orange', label: '橙色', value: s.orange ?? 0, color: '#f59e0b' },
    { key: 'yellow', label: '黄色', value: s.yellow ?? 0, color: '#eab308' },
    { key: 'green', label: '正常', value: s.green ?? 0, color: '#10b981' }
  ]
})
const donutStyle = computed(() => {
  const rows = distributionRows.value
  const total = rows.reduce((n, r) => n + Number(r.value || 0), 0) || 1
  let start = 0
  const parts = rows.map(r => {
    const end = start + Number(r.value || 0) / total * 100
    const s = `${r.color} ${start}% ${end}%`
    start = end
    return s
  })
  return `conic-gradient(${parts.join(',')})`
})

function num(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return Number.isFinite(n) ? (Math.round(n * 10) / 10).toString() : String(v)
}

function pct(v) {
  if (v === null || v === undefined) return '—'
  return Math.round(Number(v) * 100) + '%'
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

function textColor(level) {
  const m = {
    RED: 'text-red-600',
    ORANGE: 'text-orange-600',
    YELLOW: 'text-amber-700',
    GREEN: 'text-slate-600'
  }
  return m[level] || 'text-slate-600'
}

function canAppeal(it) {
  if (!it || !it.alertId) return false
  if (it.studentResponse) return false
  return it.status !== 'CLOSED' && it.status !== 'DISMISSED'
}

function flash(text, success = true) {
  message.value = text
  ok.value = success
  setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 4000)
}

async function load() {
  loading.value = true
  try {
    const [d, dash] = await Promise.all([
      studentWarningApi.overview(),
      studentWarningApi.dashboard().catch(() => null)
    ])
    Object.assign(data, d || {})
    if (dash) dashboard.value = dash
    data.items = d?.items || []
    data.advice = d?.advice || []
    data.guidelines = d?.guidelines || []
    data.summary = d?.summary || {}
  } catch (e) {
    flash(e.message, false)
  } finally {
    loading.value = false
  }
}

async function openDetail(it, focusAppeal = false) {
  try {
    detail.value = await studentWarningApi.detail(it.enrollmentId)
    appealReason.value = ''
    if (focusAppeal && canAppeal(it)) {
      setTimeout(() => {
        const box = document.querySelector('.fixed textarea')
        if (box) {
          box.focus()
          box.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }, 120)
    }
  } catch (e) {
    flash(e.message, false)
  }
}

function openReport(it, forceGenerate = false) {
  if (!it) { flash('请选择课程', false); return }
  reportGenerateMode.value = forceGenerate
  reportStudent.value = {
    studentName: data.studentName,
    studentNo: data.studentNo,
    adminClassName: it.classCode,
    riskScore: it.riskScore
  }
  reportCourses.value = [{
    teachingClassId: it.teachingClassId,
    courseName: it.courseName,
    teacherName: it.teacherName,
    riskLevel: it.level,
    riskHint: it.reason,
    absentCount: it.absentCount,
    allowedAbsent: it.allowedAbsent,
    hardFail: it.hardFail,
    fail: it.finalUnreachable,
    totalScore: it.totalScore,
    deductPoints: null,
    weightedScore: it.processScore
  }]
  reportOpen.value = true
}

async function exportReport(it) {
  try {
    const reports = await reportApi.list()
    const r = (reports || []).find(x => Number(x.teachingClassId) === Number(it.teachingClassId))
    if (!r) { flash('这门课还没有报告，请先预览生成', false); return }
    await reportApi.pdf(r.id)
  } catch (e) { flash(e?.message || 'PDF 导出失败', false) }
}

async function submitAppeal() {
  const it = detail.value?.item
  if (!it) return
  const reason = appealReason.value.trim()
  if (reason.length < 5) {
    flash('请把情况写清楚一些（至少 5 个字）', false)
    return
  }
  appealing.value = true
  try {
    await studentWarningApi.appeal(it.alertId, reason)
    await openDetail(it)
    await load()
    flash('申诉已提交，任课教师核实后会更新结论')
  } catch (e) {
    flash(e.message, false)
  } finally {
    appealing.value = false
  }
}

onMounted(load)
</script>
