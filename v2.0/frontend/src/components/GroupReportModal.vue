<template>
  <ReportModalShell :open="open" :title="title || '群体学情报告'" :subtitle="scopeLabel" @close="close">
    <template #actions>
      <button v-if="report" class="btn-ghost text-sm" @click="exportPdf">导出 PDF</button>
      <button class="btn-ghost text-sm" :disabled="busy" @click="generate">
        {{ busy ? '生成中…' : (report ? '重新生成' : '生成报告') }}
      </button>
    </template>
    <div class="report-body space-y-4">

          <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

          <div v-if="!report && !busy" class="text-sm text-slate-400 py-12 text-center">
            还没有这份报告，点右上「生成报告」（约 3–15 秒）
          </div>
          <div v-if="busy && !report" class="text-sm text-slate-400 py-12 text-center">正在汇总事实并生成…</div>

          <template v-if="report">
            <div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 px-5 py-4">
              <div class="flex items-center gap-2 mb-2">
                <span class="tag bg-brand-50 text-brand-700">多智能体报告</span>
                <span class="text-[10px] text-slate-400">生成于 {{ fmt(report.createTime) }}</span>
                <span class="text-[10px] text-slate-400">{{ sourceLabel }}</span>
                <span v-if="report.promptVersion" class="text-[10px] text-slate-400">Prompt {{ report.promptVersion }}</span>
                <span v-if="report.model" class="text-[10px] text-slate-400">{{ report.model }}</span>
              </div>
              <p class="text-sm text-slate-700 leading-7 whitespace-pre-line">{{ report.summary }}</p>
            </div>

            <div v-if="report.checksTotal" class="rounded-xl border px-4 py-3"
                 :class="allChecksOk ? 'border-emerald-100 bg-emerald-50/60' : 'border-amber-200 bg-amber-50/60'">
              <div class="flex items-center justify-between gap-2">
                <p class="text-sm font-medium" :class="allChecksOk ? 'text-emerald-700' : 'text-amber-700'">
                  数字对账 {{ report.checksPassed }}/{{ report.checksTotal }} 项一致
                  <span v-if="!allChecksOk" class="font-normal">（有不一致，见明细）</span>
                </p>
                <button class="text-xs text-slate-500 hover:underline shrink-0" @click="showChecks = !showChecks">
                  {{ showChecks ? '收起明细' : '查看明细' }}
                </button>
              </div>
              <div v-if="showChecks" class="mt-2 divide-y divide-slate-100">
                <div v-for="(c, i) in report.checks" :key="i" class="flex items-center justify-between gap-3 py-1.5 text-xs">
                  <span class="text-slate-500">{{ c.label }}</span>
                  <span class="flex items-center gap-3 flex-wrap justify-end">
                    <span class="text-slate-600">报告写 {{ c.claim }}</span>
                    <span class="text-slate-400">事实 {{ c.expected }}</span>
                    <span class="px-1.5 py-0.5 rounded" :class="c.ok ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'">
                      {{ c.ok ? '一致' : '不一致' }}
                    </span>
                  </span>
                </div>
              </div>
            </div>



            <div v-if="report.persons && report.persons.length" class="rounded-xl border border-slate-200 px-4 py-3">
              <p class="text-xs text-slate-400 mb-2">报告涉及的学生（点击查看详情）</p>
              <div class="flex flex-wrap gap-1.5">
                <button v-for="(p, i) in report.persons" :key="i"
                        class="text-xs px-2.5 py-1 rounded-full border hover:border-brand-400 hover:text-brand-600"
                        :class="p.riskLevel === 'RED' ? 'border-red-200 text-red-600 bg-red-50/40' : 'border-amber-200 text-amber-600 bg-amber-50/40'"
                        @click="emit('openStudent', p)">
                  {{ p.studentName }}<span v-if="p.className" class="opacity-60">· {{ p.className }}</span>
                </button>
              </div>
            </div>

            <section v-for="(s, i) in report.sections" :key="i" class="report-section">
              <h3 class="text-[15px] font-extrabold text-slate-900 border-b border-slate-100 pb-2 mb-2">
                {{ sectionNo(i) }}、{{ s.title }}
              </h3>
              <p class="text-sm text-slate-600 whitespace-pre-line leading-7">{{ s.text }}</p>

              <div v-if="chartKind(s.title) === 'risk'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3">
                <div class="flex items-center gap-4">
                  <div class="w-24 h-24 rounded-full relative shrink-0" :style="{ background: donutStyle }"><div class="absolute inset-4 rounded-full bg-white"></div><div class="absolute inset-0 flex flex-col items-center justify-center"><b class="text-lg text-slate-800">{{ riskTotal }}</b><span class="text-[9px] text-slate-400">学生</span></div></div>
                  <div class="space-y-1.5 text-[11px] flex-1">
                    <div class="flex justify-between"><span class="text-slate-500">红灯</span><b class="text-red-600">{{ fact.redCount || 0 }}</b></div>
                    <div class="flex justify-between"><span class="text-slate-500">黄灯</span><b class="text-amber-600">{{ fact.yellowCount || 0 }}</b></div>
                    <div class="flex justify-between"><span class="text-slate-500">绿灯</span><b class="text-emerald-600">{{ fact.greenCount || 0 }}</b></div>
                  </div>
                </div>
              </div>

              <div v-else-if="chartKind(s.title) === 'score'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3 space-y-2">
                <div v-for="row in scoreRows" :key="row.label">
                  <div class="flex justify-between text-[10px] mb-1"><span class="text-slate-500">{{ row.label }}</span><b class="text-slate-700">{{ row.text }}</b></div>
                  <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full rounded-full" :style="{ width: row.pct + '%', background: row.color }"></div></div>
                </div>
                <div v-if="assessmentRows.length" class="pt-2 mt-2 border-t border-dashed border-[#e6edf5]">
                  <p class="text-[10px] text-slate-500 mb-1">已录考核项权重构成</p>
                  <div class="flex h-2 rounded-full overflow-hidden mb-2">
                    <div v-for="(a, i) in assessmentRows" :key="'aw-'+a.name" :style="{ width: a.weight / assessmentWeightTotal * 100 + '%' }" :class="i % 3 === 0 ? 'bg-[#0f9d8a]' : i % 3 === 1 ? 'bg-[#7aa7c7]' : 'bg-amber-300'"></div>
                  </div>
                  <div class="space-y-1">
                    <div v-for="a in assessmentRows" :key="'ai-'+a.name" class="flex items-center justify-between text-[10px]">
                      <span class="text-slate-500 truncate">{{ a.name }} · 权重 {{ a.weight }}%</span>
                      <b class="text-slate-700">平均 {{ a.avgScore.toFixed(1) }}</b>
                    </div>
                  </div>
                  <p class="text-[10px] text-slate-400 mt-1">上方成绩条是成绩指标；这里才是考核项权重构成。</p>
                </div>
              </div>

              <div v-else-if="chartKind(s.title) === 'radar'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3">
                <div class="flex flex-col md:flex-row items-center gap-4">
                  <svg viewBox="0 0 300 250" class="w-64 h-52 shrink-0">
                    <polygon v-for="(ring, i) in radar.rings" :key="'ring-'+i" :points="ring" fill="none" stroke="#dbe7f2" />
                    <line v-for="(p, i) in radar.points" :key="'axis-'+i" x1="150" y1="120" :x2="p.x" :y2="p.y" stroke="#e6edf5" />
                    <polygon :points="radar.polygon" fill="rgba(15,157,138,.22)" stroke="#0f9d8a" stroke-width="2" />
                    <circle v-for="(p, i) in radar.points" :key="'pt-'+i" :cx="p.x" :cy="p.y" r="3" fill="#0f9d8a" />
                    <text v-for="(p, i) in radar.points" :key="'lb-'+i" :x="p.lx" :y="p.ly" text-anchor="middle" font-size="10" fill="#6b8296">{{ p.label }}</text>
                  </svg>
                  <div class="space-y-1.5 text-[11px] flex-1">
                    <div v-for="p in radar.points" :key="'rv-'+p.label" class="flex justify-between gap-4"><span class="text-slate-500">{{ p.label }}</span><b class="text-slate-700">{{ Math.round(p.value) }}</b></div>
                    <p class="text-[10px] text-slate-400 pt-1 border-t border-dashed border-[#e6edf5]">每个轴满分 100；成绩水平取当前过程分（期末未计入），风险控制 = 红灯全权重、黄灯半权重。</p>
                  </div>
                </div>
              </div>

              <div v-else-if="chartKind(s.title) === 'courseRisk'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3 space-y-2">
                <div class="flex justify-between text-[10px] text-slate-400"><span>条形长度 = 课程风险率</span><span>0 — {{ courseRiskMaxRate }}%</span></div>
                <div v-for="c in courseRiskRows" :key="'cr-'+c.name">
                  <div class="flex justify-between text-[10px] mb-1"><span class="text-slate-600 truncate">{{ c.name }}</span><span class="text-slate-400">风险率 <b class="text-slate-600">{{ c.rate }}%</b> · 红 {{ c.red }} / 黄 {{ c.yellow }}</span></div>
                  <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full rounded-full bg-gradient-to-r from-red-400 to-amber-400" :style="{ width: c.pct + '%' }"></div></div>
                </div>
                <p v-if="!courseRiskRows.length" class="text-[11px] text-slate-400 py-2 text-center">暂无集中风险课程</p>
              </div>

              <div v-else-if="chartKind(s.title) === 'multiRisk'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-4">
                <div class="flex items-end justify-around gap-4 h-36 border-b border-[#dbe7f2] pb-1">
                  <div v-for="b in multiRiskBuckets" :key="'mr-'+b.label" class="flex flex-col items-center justify-end gap-1 flex-1 max-w-[72px]">
                    <span class="text-[11px] font-semibold text-slate-700">{{ b.value }} 人</span>
                    <div class="w-full rounded-t-md bg-gradient-to-t from-red-400 to-amber-300" :style="{ height: Math.max(8, b.pct) + 'px' }"></div>
                  </div>
                </div>
                <div class="flex justify-around gap-4 mt-1">
                  <span v-for="b in multiRiskBuckets" :key="'ml-'+b.label" class="text-[10px] text-slate-500 flex-1 text-center max-w-[72px]">{{ b.label }}</span>
                </div>
                <p v-if="!multiRiskBuckets.length" class="text-[11px] text-slate-400 py-4 text-center">没有两门及以上预警学生</p>
                <p class="text-[10px] text-slate-400 mt-2">按同一名学生被预警的课程门数分组；柱高为人数。</p>
              </div>

              <div v-else-if="chartKind(s.title) === 'watchRank'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3 space-y-2">
                <div v-for="(w, i) in watchRows" :key="'wr-'+w.no" class="grid grid-cols-[22px_92px_1fr_86px] gap-2 items-center">
                  <span class="text-[10px] text-slate-400">{{ i + 1 }}</span>
                  <span class="text-[10px] text-slate-600 truncate" :title="w.name + ' ' + w.no">{{ w.name }}</span>
                  <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full rounded-full bg-gradient-to-r from-amber-300 to-red-400" :style="{ width: w.pct + '%' }"></div></div>
                  <span class="text-[10px] text-right text-slate-500"><b class="text-slate-700">{{ w.risk }} 门</b> · 最低 {{ num(w.min) }}</span>
                </div>
                <p v-if="!watchRows.length" class="text-[11px] text-slate-400 py-4 text-center">暂无重点学生</p>
                <p class="text-[10px] text-slate-400 mt-2">条形长度 = 预警门数；按预警门数降序、最低总评升序排列，最多展示 8 人。</p>
              </div>

              <div v-else-if="chartKind(s.title) === 'classCompare'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3">
                <div class="flex items-center justify-between text-[9px] text-slate-400 mb-2 px-[145px]">
                  <span>0</span><span>25</span><span>50</span><span>75</span><span>100 过程分</span>
                </div>
                <div class="space-y-3">
                  <div v-for="d in classCompareRows" :key="'cc-'+d.name" class="grid grid-cols-[130px_1fr_92px] gap-3 items-center">
                    <span class="text-[10px] text-slate-600 truncate" :title="d.fullName">{{ d.name }}</span>
                    <div class="relative h-6">
                      <div class="absolute left-0 right-0 top-1/2 h-px bg-[#dbe7f2]"></div>
                      <div class="absolute top-1/2 w-3 h-3 rounded-full bg-[#0f9d8a] ring-2 ring-white shadow -translate-y-1/2 -translate-x-1/2" :style="{ left: d.score + '%' }" :title="d.fullName + '：过程分 ' + d.score.toFixed(1)"></div>
                    </div>
                    <div class="text-right">
                      <b class="text-[11px] text-slate-700">{{ d.score.toFixed(1) }}</b>
                      <span class="text-[10px] text-slate-400 ml-1">红黄 {{ d.risk }}</span>
                    </div>
                  </div>
                </div>
                <p class="text-[10px] text-slate-400 mt-2">点越靠右过程分越高；右侧为红黄灯人数，悬停可看教学班全名。</p>
              </div>

              <div v-else-if="chartKind(s.title) === 'breakdown'" class="mt-3 rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3 space-y-2">
                <div v-for="row in breakdownRows" :key="row.name">
                  <div class="flex justify-between text-[10px] mb-1"><span class="text-slate-500 truncate">{{ row.name }}</span><b class="text-slate-700">{{ row.text }}</b></div>
                  <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full rounded-full bg-gradient-to-r from-[#20b7a4] to-[#7aa7c7]" :style="{ width: row.pct + '%' }"></div></div>
                </div>
                <p v-if="!breakdownRows.length" class="text-[11px] text-slate-400 py-2 text-center">暂无分组数据</p>
              </div>
            </section>

            <p class="text-[10px] text-slate-400 border-t border-dashed border-slate-200 pt-3 leading-5">
              生成时间 {{ fmt(report.createTime) }} ｜ 报告里的数字均来自系统判定与成绩数据，可逐项对账；PDF 会保留本页脚。
            </p>
          </template>

    </div>

    <GenerationOverlay :open="busy" :step="generationStep" title="正在生成群体学情报告"
                       subtitle="汇总班级事实、生成研判结论并做数字对账" :agents="generationAgents"
                       :messages="generationMessages" flow="体检 → 汇总 → 研判 → 生成 → 对账" />
  </ReportModalShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { groupReportApi } from '../api/ai'
import ReportModalShell from './ReportModalShell.vue'
import GenerationOverlay from './GenerationOverlay.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  scope: { type: String, default: 'CLASS' },
  targetId: { type: [Number, String], default: null },
  title: { type: String, default: '' },
  generateOnOpen: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'openStudent'])

const report = ref(null)
const busy = ref(false)
const generationStep = ref(0)
const error = ref('')
const showChecks = ref(false)
const generationAgents = [
  { icon: '🩺', name: '数据体检' },
  { icon: '📊', name: '数据汇总' },
  { icon: '🧭', name: '学情研判' },
  { icon: '📚', name: 'RAG 取证' },
  { icon: '✅', name: '数字对账' },
  { icon: '📄', name: '报告生成' }
]
const generationMessages = [
  '正在读取班级与课程快照…',
  '正在汇总成绩、考勤与风险分布…',
  '正在生成班级研判结论…',
  '正在绑定课程资料与依据…',
  '正在逐项核对报告数字…',
  '报告已生成，正在准备预览…'
]

const allChecksOk = computed(() =>
  !!report.value && (report.value.checksTotal || 0) === (report.value.checksPassed || 0))
const fact = computed(() => report.value?.fact || {})
const riskTotal = computed(() => Number(fact.value.redCount || 0) + Number(fact.value.yellowCount || 0) + Number(fact.value.greenCount || 0))
const donutStyle = computed(() => {
  const total = Math.max(1, riskTotal.value)
  const red = Number(fact.value.redCount || 0), yellow = Number(fact.value.yellowCount || 0)
  const a = red / total * 100, b = a + yellow / total * 100
  if (!riskTotal.value) return '#e5e7eb'
  return `conic-gradient(#ef4444 0 ${a}%, #f59e0b ${a}% ${b}%, #22c55e ${b}% 100%)`
})
const scoreRows = computed(() => {
  const f = fact.value
  const n = (v) => Number(v || 0)
  return [
    { label: '过程分平均', text: num(f.avgProcessScore ?? f.avgScore), pct: Math.min(100, n(f.avgProcessScore ?? f.avgScore)), color: '#20b7a4' },
    { label: '总评最低', text: num(f.minScore), pct: Math.min(100, n(f.minScore)), color: '#7aa7c7' },
    { label: '总评最高', text: num(f.maxScore), pct: Math.min(100, n(f.maxScore)), color: '#0f9d8a' },
    { label: '缺勤超限', text: n(f.absentOverLimitCount) + ' 人', pct: Math.min(100, n(f.absentOverLimitCount) / Math.max(1, n(f.studentCount)) * 100), color: '#ef4444' }
  ]
})
const assessmentRows = computed(() => {
  const raw = (fact.value.weakItems && fact.value.weakItems.length) ? fact.value.weakItems : (fact.value.items || [])
  return raw.map(i => ({
    name: i.name,
    weight: Number(i.weight || 0),
    avgScore: Number(i.avgScore || 0)
  }))
})
const assessmentWeightTotal = computed(() => Math.max(1, assessmentRows.value.reduce((n, i) => n + i.weight, 0)))
const breakdownRows = computed(() => {
  const f = fact.value
  if (report.value?.scope === 'OFFERING') {
    return (f.classes || []).slice(0, 5).map(c => ({
      name: c.className || c.classCode || '教学班',
      text: num(c.avgProcessScore ?? c.avgScore) + ' 分',
      pct: Math.min(100, Number(c.avgProcessScore ?? c.avgScore ?? 0))
    }))
  }
  return (f.weakGroups || []).slice(0, 5).map(g => ({
    name: g.name,
    text: pct(g.rate),
    pct: Math.min(100, Number(g.rate || 0))
  }))
})
const classScatter = computed(() => {
  const rows = (fact.value.classes || []).slice(0, 10)
  const maxRisk = Math.max(1, ...rows.map(c => Number(c.red || 0) + Number(c.yellow || 0)))
  const dots = rows.map(c => {
    const score = Math.max(0, Math.min(100, Number(c.avgProcessScore ?? c.avgScore ?? 0)))
    const risk = Number(c.red || 0) + Number(c.yellow || 0)
    const students = Number(c.studentCount || 0)
    return {
      name: c.classCode || c.className || '教学班',
      fullName: c.className || c.classCode || '教学班',
      score,
      risk,
      students,
      x: 35 + score / 100 * 285,
      y: 170 - risk / maxRisk * 130,
      r: Math.max(7, Math.min(15, 6 + students / 12))
    }
  })
  return { dots, maxRisk, ticks: [0, 25, 50, 75, 100] }
})
const classCompareRows = computed(() => (fact.value.classes || []).map(c => ({
  name: c.classCode || c.className || '教学班',
  fullName: c.className || c.classCode || '教学班',
  score: Math.max(0, Math.min(100, Number(c.avgProcessScore ?? c.avgScore ?? 0))),
  risk: Number(c.red || 0) + Number(c.yellow || 0),
  students: Number(c.studentCount || 0)
})))
const courseRiskRows = computed(() => {
  const rows = (fact.value.courseRisks || []).slice(0, 6)
  const maxRate = Math.max(1, ...rows.map(c => Number(c.failRate || 0)))
  return rows.map(c => ({
    name: c.courseName || '课程',
    red: Number(c.red || 0),
    yellow: Number(c.yellow || 0),
    rate: Number(c.failRate || 0),
    pct: Math.round(Number(c.failRate || 0) / maxRate * 100)
  }))
})
const courseRiskMaxRate = computed(() => Math.max(1, ...(fact.value.courseRisks || []).map(c => Number(c.failRate || 0))))
const adminStudents = computed(() => (fact.value.multiRiskStudents || []))
const multiRiskBuckets = computed(() => {
  const map = new Map()
  for (const s of adminStudents.value) {
    const n = Number(s.riskCount || 0)
    if (n < 2) continue
    map.set(n, (map.get(n) || 0) + 1)
  }
  const rows = [...map.entries()].sort((a, b) => a[0] - b[0]).map(([label, value]) => ({ label: label + ' 门', value }))
  const max = Math.max(1, ...rows.map(r => r.value))
  return rows.map(r => ({ ...r, pct: Math.round(r.value / max * 100) }))
})
const watchRows = computed(() => {
  const rows = adminStudents.value.slice().sort((a, b) => {
    const d = Number(b.riskCount || 0) - Number(a.riskCount || 0)
    return d !== 0 ? d : Number(a.minScore ?? 999) - Number(b.minScore ?? 999)
  }).slice(0, 8)
  const max = Math.max(1, ...rows.map(s => Number(s.riskCount || 0)))
  return rows.map(s => ({
    name: s.studentName || '学生',
    no: s.studentNo || '',
    risk: Number(s.riskCount || 0),
    min: s.minScore,
    pct: Math.round(Number(s.riskCount || 0) / max * 100)
  }))
})
const radar = computed(() => {  const f = fact.value
  const n = v => Number(v || 0)
  const students = Math.max(1, n(f.studentCount))
  const attendance = f.plannedWeeks ? Math.max(0, Math.min(100, 100 - n(f.avgAbsent) / n(f.plannedWeeks) * 100)) : 0
  const axes = [
    { label: '成绩水平', value: Math.max(0, Math.min(100, n(f.avgProcessScore ?? f.avgScore))) },
    { label: '出勤', value: attendance },
    { label: '风险控制', value: Math.max(0, Math.min(100, 100 - (n(f.redCount) + n(f.yellowCount) * 0.5) / students * 100)) },
    { label: '硬线安全', value: Math.max(0, Math.min(100, 100 - n(f.absentOverLimitCount) / students * 100)) }
  ]
  const cx = 150, cy = 120, radius = 82
  const points = axes.map((a, i) => {
    const angle = -Math.PI / 2 + i * 2 * Math.PI / axes.length
    return {
      ...a,
      x: cx + Math.cos(angle) * radius * a.value / 100,
      y: cy + Math.sin(angle) * radius * a.value / 100,
      lx: cx + Math.cos(angle) * (radius + 25),
      ly: cy + Math.sin(angle) * (radius + 25)
    }
  })
  const ring = (factor) => axes.map((_, i) => {
    const angle = -Math.PI / 2 + i * 2 * Math.PI / axes.length
    return `${cx + Math.cos(angle) * radius * factor},${cy + Math.sin(angle) * radius * factor}`
  }).join(' ')
  return { axes, points, polygon: points.map(p => `${p.x},${p.y}`).join(' '), rings: [0.25, 0.5, 0.75, 1].map(ring) }
})
const chartReady = computed(() => !!report.value && !!report.value.fact && (riskTotal.value > 0 || fact.value.avgScore != null))
const scopeLabel = computed(() => props.scope === 'ADMIN_CLASS'
  ? '行政班视角（一个班 × 多门课）'
  : (props.scope === 'OFFERING' ? '课程视角（一门课 × 我教的全部教学班）' : '教学班视角（一门课 × 一个班）'))
const sourceLabel = computed(() => report.value?.source === 'TEMPLATE' ? '规则生成' : 'AI 生成')

function chartKind(title) {
  const t = title || ''
  if (t.includes('班级画像') && report.value?.scope === 'CLASS') return 'radar'
  if (t.includes('风险结构')) return 'risk'
  if (t.includes('成绩与考核项')) return 'score'
  if (t.includes('集中风险课程')) return 'courseRisk'
  if (t.includes('多门预警学生') && report.value?.scope === 'ADMIN_CLASS') return 'multiRisk'
  if (t.includes('重点关注学生') && report.value?.scope === 'ADMIN_CLASS') return 'watchRank'
  if (t.includes('教学班对比')) return 'classCompare'
  if (t.includes('薄弱点')) return 'breakdown'
  return ''
}

function fmt(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}
function num(v) {
  return v == null ? '—' : (Number.isFinite(Number(v)) ? Number(v).toFixed(1) : v)
}
function pct(v) {
  return v == null ? '—' : Number(v).toFixed(1) + '%'
}
function sectionNo(i) {
  return ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'][i] || String(i + 1)
}
function close() { emit('close') }
async function exportPdf() {
  if (!report.value || !props.targetId) return
  try { await groupReportApi.pdf(props.scope, props.targetId) } catch (e) { error.value = e?.message || 'PDF 导出失败' }
}

async function loadLatest() {
  report.value = null
  error.value = ''
  showChecks.value = false
  if (!props.targetId) return
  try {
    report.value = await groupReportApi.latest(props.scope, props.targetId)
  } catch (e) {
    error.value = e?.message || '读取失败'
  }
}

async function generate() {
  if (!props.targetId || busy.value) return
  busy.value = true
  generationStep.value = 1
  error.value = ''
  const startedAt = Date.now()
  const timer = setInterval(() => { if (generationStep.value < 6) generationStep.value++ }, 600)
  try {
    report.value = await groupReportApi.generate(props.scope, props.targetId)
    const remain = Math.max(0, 3200 - (Date.now() - startedAt))
    if (remain) await new Promise(r => setTimeout(r, remain))
    generationStep.value = 6
  } catch (e) {
    error.value = e?.message || '生成失败'
  } finally {
    clearInterval(timer)
    setTimeout(() => { busy.value = false; generationStep.value = 0 }, 500)
  }
}

watch(
  () => [props.open, props.targetId, props.scope],
  async ([isOpen]) => {
    if (!isOpen) return
    await loadLatest()
    if (props.generateOnOpen && props.targetId) await generate()
  },
  { immediate: true }
)
</script>

<style scoped>
@media print {
  :global(body) { background: #fff; }
  .report-overlay { position: static !important; padding: 0 !important; background: #fff !important; overflow: visible !important; }
  .report-card { box-shadow: none !important; border-radius: 0 !important; max-height: none !important; overflow: visible !important; width: 100% !important; max-width: none !important; }
  .report-head { border-bottom: 0 !important; }
  .report-head button { display: none !important; }
  .report-body { overflow: visible !important; padding: 0 !important; }
  .report-section { break-inside: avoid; }
}
</style>

