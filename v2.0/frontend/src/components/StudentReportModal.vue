<template>
  <ReportModalShell :open="open" title="学生报告"
                    :subtitle="`${student?.studentName || ''} · ${student?.studentNo || ''} · ${student?.adminClassName || ''} ｜ ${mode === 'teacher' ? '教师视角' : (mode === 'student' ? '学生视角' : '辅导员视角')}`"
                    @close="$emit('close')">
    <template #actions>
      <button class="btn-ghost text-sm" @click="exportPdf">导出 PDF</button>
    </template>
    <div class="space-y-4">

          <p v-if="reportMessage" class="text-xs text-amber-600 print:hidden">{{ reportMessage }}</p>
          <div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-4 flex items-center gap-5">
            <div>
              <p class="text-3xl font-semibold" :class="riskColor">{{ num(student?.riskScore) }}</p>
              <p class="text-[10px] text-slate-400 mt-1">综合危险度</p>
            </div>
            <div class="flex-1 text-sm text-slate-600 leading-6">
              <span class="tag" :class="riskTagClass">{{ riskLevelText }}</span>
              <span class="ml-1">共 {{ courses.length }} 门课程，{{ riskCourses.length }} 门有风险。</span><br>
              <span class="text-xs text-slate-400">报告按课程拆分；数字来自当前风险快照与成绩/考勤数据。</span>
            </div>
            <div class="text-right text-xs text-slate-400 shrink-0">
              报告类型<br><b class="text-sm text-slate-700">{{ mode === 'teacher' ? '课程预警' : '课程报告' }}</b>
            </div>
          </div>

          <div v-if="activeReport" class="rounded-2xl border border-[#dbe7f2] bg-white overflow-hidden">
            <div class="flex items-center justify-between gap-3 px-5 py-3 bg-[#f7fbfd] border-b border-[#e6edf5]">
              <div>
                <h2>{{ activeCourse?.courseName || activeReport.courseName }} · 课程学情报告</h2>
                <p class="text-[11px] text-slate-400 mt-1">{{ activeCourse?.teacherName || activeReport.teacherName }} · {{ activeCourse?.classCode || activeReport.className }} · {{ activeReport.termName }}</p>
              </div>
              <div class="flex items-center gap-2">
                <button class="btn-ghost text-sm" @click="downloadCourse(activeCourse)">导出 PDF</button>
                <button class="text-xs text-slate-400 hover:text-slate-600" @click="activeReport = null">返回课程列表</button>
              </div>
            </div>
            <div class="p-5 space-y-4">
              <div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-[#f7fbfd] p-4 flex items-center gap-5">
                <div><p class="text-4xl font-semibold" :class="riskColor">{{ num(activeReport.totalScore ?? activeCourse?.riskScore) }}</p><p class="text-[10px] text-slate-400 mt-1">课程风险分 / 100</p></div>
                <div class="flex-1 text-sm text-slate-600 leading-6"><span class="tag" :class="riskTagClass">{{ riskLevelText }}</span><br>{{ activeReport.summary }}<br><span class="text-[11px] text-slate-400">有效课次 {{ fact.effectiveSessions ?? '—' }} · 置信度 {{ fact.confidenceLevel ?? '—' }}<span v-if="fact.classRank"> · 班内排名 {{ fact.classRank }} / {{ fact.classSize }}</span></span></div>
              </div>

              <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">缺课 / 允许</p><p class="text-lg font-semibold" :class="activeCourse?.hardFail ? 'text-red-600' : 'text-slate-700'">{{ activeCourse?.absentCount ?? '—' }} / {{ activeCourse?.allowedAbsent ?? '—' }}</p></div>
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">过程分 / 总评</p><p class="text-lg font-semibold text-slate-700">{{ activeCourse?.processScore ?? '—' }} / {{ activeCourse?.totalScore ?? '—' }}</p></div>
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">考勤扣分</p><p class="text-lg font-semibold text-slate-700">{{ activeCourse?.deductPoints ?? '—' }}</p></div>
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">加权分</p><p class="text-lg font-semibold text-slate-700">{{ activeCourse?.weightedScore ?? '—' }}</p></div>
              </div>

              <div class="rounded-xl border border-[#e6edf5] overflow-hidden">
                <div class="px-4 py-2 bg-[#f7fbfd] border-b border-[#e6edf5]"><h3>成绩与考勤明细</h3></div>
                <table>
                  <thead><tr><th>项目</th><th>当前值</th><th>说明</th></tr></thead>
                  <tbody>
                    <tr><td>缺课 / 允许</td><td :class="activeCourse?.hardFail ? 'text-red-600 font-medium' : ''">{{ activeCourse?.absentCount ?? '—' }} / {{ activeCourse?.allowedAbsent ?? '—' }}</td><td>{{ activeCourse?.hardFail ? '已超过允许上限，触发硬性不合格' : '缺课在允许范围内' }}</td></tr>
                    <tr><td>迟到 / 请假</td><td>{{ activeCourse?.lateCount ?? '—' }} / {{ activeCourse?.leaveCount ?? '—' }}</td><td>按课程考勤记录汇总</td></tr>
                    <tr><td>过程分</td><td>{{ activeCourse?.processScore ?? '—' }}</td><td>作业、测验、课堂表现等已录项加权</td></tr>
                    <tr><td>当前总评</td><td>{{ activeCourse?.totalScore ?? activeReport.totalScore ?? '—' }}</td><td>加权分 − 考勤扣分</td></tr>
                    <tr><td>加权分 / 扣分</td><td>{{ activeCourse?.weightedScore ?? '—' }} / {{ activeCourse?.deductPoints ?? '—' }}</td><td>过程性成绩与考勤扣分</td></tr>
                  </tbody>
                </table>
              </div>

              <div>
                <div class="flex items-center justify-between mb-2"><h3>六维画像对比</h3><span class="text-[10px] text-slate-400">学生 / 班级均值</span></div>
                <div v-if="activeReport.metrics?.dimensions?.length" class="space-y-2">
                  <div v-for="d in activeReport.metrics.dimensions" :key="'full-'+d.key" class="grid grid-cols-[72px_1fr_86px] gap-2 items-center text-xs">
                    <span class="text-slate-500">{{ d.label }}</span>
                    <div class="h-2 rounded-full bg-slate-100 overflow-hidden flex gap-0.5"><div class="h-full bg-[#0f9d8a]" :style="{ width: (d.student ?? 0) + '%' }"></div><div class="h-full bg-slate-300" :style="{ width: (d.classAvg ?? 0) + '%' }"></div></div>
                    <span class="text-right text-slate-600">{{ d.student ?? '—' }} / {{ d.classAvg ?? '—' }}</span>
                  </div>
                </div>
                <p v-else class="text-xs text-slate-400">该报告没有六维指标。</p>
              </div>

              <div v-if="trendRows.length" class="rounded-xl border border-[#e6edf5] p-4">
                <div class="flex items-center justify-between mb-2"><h3>课程成绩趋势</h3><span class="text-[10px] text-slate-400">最近 {{ trendRows.length }} 次 · 学生 / 班级均分</span></div>
                <svg viewBox="0 0 620 150" class="w-full h-36">
                  <line x1="0" y1="130" x2="620" y2="130" stroke="#e6edf5" />
                  <polyline :points="trendPath(trendRows, 'myRate')" fill="none" stroke="#0f9d8a" stroke-width="2" />
                  <polyline :points="trendPath(trendRows, 'classRate')" fill="none" stroke="#b9cad9" stroke-width="2" stroke-dasharray="5 4" />
                  <circle v-for="(p, i) in trendPoints(trendRows, 'myRate')" :key="'tp-'+i" :cx="p.x" :cy="p.y" r="3" fill="#0f9d8a" />
                </svg>
                <div class="flex justify-between text-[10px] text-slate-400 mt-1"><span v-for="t in trendRows" :key="'tl-'+t.week">第{{ t.week }}周</span></div>
              </div>

              <div v-if="assessmentRows.length" class="rounded-xl border border-[#e6edf5] p-4">
                <div class="flex items-center justify-between mb-2"><h3>考核项对比</h3><span class="text-[10px] text-slate-400">当前得分 / 权重</span></div>
                <div class="space-y-2">
                  <div v-for="it in assessmentRows" :key="it.name" class="grid grid-cols-[90px_1fr_70px] gap-2 items-center text-xs">
                    <span class="text-slate-500 truncate" :title="it.name">{{ it.name }}</span>
                    <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full bg-[#0f9d8a]" :style="{ width: Math.max(0, Math.min(100, Number(it.rate ?? it.score ?? 0))) + '%' }"></div></div>
                    <span class="text-right text-slate-600">{{ it.score ?? '—' }} / {{ it.weight ?? '—' }}</span>
                  </div>
                </div>
              </div>

              <div v-if="pipeline.length" class="rounded-xl border border-[#e6edf5] p-4">
                <h3 class="mb-2">多智能体研判链路</h3>
                <div class="flex flex-wrap items-center gap-2 text-[10px]">
                  <template v-for="(s, i) in pipeline" :key="'pipe-'+i">
                    <span class="rounded-full border border-[#dbe7f2] bg-[#f7fbfd] px-2 py-1 text-[#3c6f9b]">{{ s.name }} <b class="text-[#0f9d8a]">{{ s.status }}</b></span>
                    <span v-if="i < pipeline.length - 1" class="text-slate-300">→</span>
                  </template>
                </div>
              </div>

              <div class="space-y-3">
                <div v-for="(sec, si) in activeReport.sections" :key="'full-sec-'+si" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-4">
                  <h3 class="text-[13px]">{{ ['一','二','三','四','五','六','七','八'][si] || (si+1) }}、{{ sec.title }}</h3>
                  <p class="text-sm text-slate-600 leading-7 whitespace-pre-line mt-1">{{ sec.text }}</p>
                </div>
              </div>

              <div v-if="activeReport.actions?.length" class="flex flex-wrap gap-2 pt-1">
                <button v-for="(a, i) in activeReport.actions" :key="i" class="btn-ghost text-xs" @click="activeReport = null">{{ a.label }}</button>
              </div>
            </div>
          </div>

          <div class="flex items-center justify-between">
            <h3 class="text-sm font-medium text-slate-700">课程报告</h3>
            <span class="text-xs text-slate-400">{{ mode === 'teacher' ? '仅显示本人任教课程' : (mode === 'student' ? '本学期课程' : '行政班全部课程') }}</span>
          </div>

          <div class="space-y-2">
            <div v-for="c in courses" :key="c.teachingClassId || c.courseName"
                 class="border border-slate-200 rounded-xl p-3 hover:border-brand-300 transition">
              <div class="flex items-center justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-sm font-medium text-slate-800 truncate">{{ c.courseName }}</p>
                  <p class="text-[11px] text-slate-400 mt-0.5">
                    {{ c.teacherName || '—' }} · {{ c.termName || '当前学期' }} · {{ c.riskHint || '暂无风险提示' }}
                  </p>
                </div>
                <span class="tag shrink-0" :class="courseTag(c)">{{ courseLevel(c) }}</span>
              </div>
              <div class="grid grid-cols-4 gap-2 mt-3 text-center text-xs">
                <div><p class="text-[10px] text-slate-400">缺课/允许</p><b :class="c.hardFail ? 'text-red-600' : 'text-slate-700'">{{ c.absentCount ?? '—' }}/{{ c.allowedAbsent ?? '—' }}</b></div>
                <div><p class="text-[10px] text-slate-400">总评</p><b :class="c.fail ? 'text-red-600' : 'text-slate-700'">{{ c.totalScore ?? '—' }}</b></div>
                <div><p class="text-[10px] text-slate-400">扣分</p><b class="text-slate-700">{{ c.deductPoints ?? '—' }}</b></div>
                <div><p class="text-[10px] text-slate-400">加权</p><b class="text-slate-700">{{ c.weightedScore ?? '—' }}</b></div>
              </div>
              <div class="flex justify-end gap-2 mt-2 print:hidden">
                <span v-if="courseReport(c)" class="text-[10px] text-slate-400 self-center">
                  报告 #{{ courseReport(c).id }} · run #{{ courseReport(c).runId || '—' }}
                </span>
                <button class="text-xs text-brand-600 hover:underline" @click="toggleReport(c)">
                  {{ courseReport(c) ? '查看六维' : '生成报告' }}
                </button>
                <button v-if="courseReport(c)" class="text-xs text-brand-600 hover:underline" @click="downloadCourse(c)">导出 PDF</button>
              </div>
              <div v-if="activeReport && activeReport.teachingClassId === c.teachingClassId" class="mt-2 rounded-lg bg-slate-50 px-3 py-2">
                <div class="flex items-center justify-between text-[10px] text-slate-400 mb-1">
                  <span>六维画像 · 学生 / 班级均值</span>
                  <span v-if="activeReport.stale" class="text-amber-600">报告已过期，建议重新生成</span>
                </div>
                <div v-if="activeReport.metrics?.dimensions?.length" class="space-y-1">
                  <div v-for="d in activeReport.metrics.dimensions" :key="d.key" class="grid grid-cols-[64px_1fr_72px] gap-2 items-center text-[10px]">
                    <span class="text-slate-500">{{ d.label }}</span>
                    <div class="h-1.5 rounded-full bg-slate-200 overflow-hidden flex gap-0.5">
                      <div class="h-full bg-brand-500" :style="{ width: (d.student ?? 0) + '%' }"></div>
                      <div class="h-full bg-slate-400" :style="{ width: (d.classAvg ?? 0) + '%' }"></div>
                    </div>
                    <span class="text-right text-slate-600">{{ d.student ?? '—' }} / {{ d.classAvg ?? '—' }}</span>
                  </div>
                </div>
                <p v-else class="text-[10px] text-slate-400">该报告没有六维指标，重新生成后可获得。</p>
                <div v-if="activeReport.sections?.length" class="mt-2 space-y-2 border-t border-slate-200 pt-2">
                  <div v-for="(sec, si) in activeReport.sections" :key="si">
                    <p class="text-[11px] font-medium text-slate-700">{{ sec.title }}</p>
                    <p class="text-[11px] text-slate-500 whitespace-pre-line leading-5">{{ sec.text }}</p>
                  </div>
                </div>
              </div>
              <div v-if="selected === c" class="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600 leading-6">
                <p><b class="text-slate-700">风险结论：</b>{{ c.riskHint || '暂无' }}</p>
                <p><b class="text-slate-700">判定说明：</b>{{ c.hardFail ? '缺课超过允许上限，触发硬性不合格。' : (c.fail ? '成绩未达到及格线。' : '当前未触发硬性风险。') }}</p>
                <p><b class="text-slate-700">数据口径：</b>风险快照 + 成绩/考勤；完整多智能体报告将在接入报告服务后生成。</p>
              </div>
            </div>
            <p v-if="!courses.length" class="text-sm text-slate-400 py-8 text-center">暂无可展示课程</p>
          </div>

          <p class="text-[10px] text-slate-400 border-t border-dashed border-slate-200 pt-3 leading-5">
            学生 / 教师 / 辅导员均读取 ai_report 独立快照；PDF 由服务端 OpenPDF 生成，可选择保存目录。
            报告数字可回溯到 risk_state / 成绩 / 考勤。
          </p>
    </div>
  </ReportModalShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { staffReportApi, reportApi } from '../api/ai'
import ReportModalShell from './ReportModalShell.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  student: { type: Object, default: () => ({}) },
  courses: { type: Array, default: () => [] },
  mode: { type: String, default: 'counselor' }
})
const emit = defineEmits(['close'])

const selected = ref(null)
const reports = ref([])
const activeReport = ref(null)
const activeCourse = computed(() => props.courses.find((c) => Number(c.teachingClassId) === Number(activeReport.value?.teachingClassId)) || null)
const fact = computed(() => activeReport.value?.fact || {})
const trendRows = computed(() => Array.isArray(fact.value.weeks) ? fact.value.weeks : [])
const assessmentRows = computed(() => Array.isArray(fact.value.items) ? fact.value.items : [])
const pipeline = computed(() => {
  const r = activeReport.value
  if (!r) return []
  return [
    { name: '数据体检', status: fact.value.effectiveSessions ? 'READY' : '—' },
    { name: '数据分析', status: (r.metrics?.dimensions?.length || 0) + ' 维' },
    { name: '学情研判', status: r.riskLevel || '—' },
    { name: 'RAG 证据', status: (r.sections || []).some(s => s.title === '参考依据') ? '已附' : '—' },
    { name: '报告生成', status: r.source || '—' },
    { name: '质量校验', status: r.stale ? '需复核' : 'PASS' }
  ]
})
function trendPoints(rows, key) {
  if (!rows.length) return []
  return rows.map((r, i) => ({ x: rows.length === 1 ? 300 : 10 + i * 600 / (rows.length - 1), y: 130 - Math.max(0, Math.min(100, Number(r[key] ?? 0))) * 1.2 }))
}
function trendPath(rows, key) {
  return trendPoints(rows, key).map(p => `${p.x},${p.y}`).join(' ')
}
const reportMessage = ref('')
const loadingReports = ref(false)
const riskCourses = computed(() => props.courses.filter((c) => courseLevel(c) !== '正常'))
const riskLevelText = computed(() => {
  const v = Number(props.student?.riskScore || 0)
  if (v >= 80) return '高风险'
  if (v >= 60) return '中风险'
  if (v >= 40) return '低风险'
  return '正常'
})
const riskColor = computed(() => Number(props.student?.riskScore || 0) >= 80 ? 'text-red-600' : Number(props.student?.riskScore || 0) >= 60 ? 'text-amber-600' : 'text-slate-700')
const riskTagClass = computed(() => Number(props.student?.riskScore || 0) >= 80 ? 'bg-red-50 text-red-600' : Number(props.student?.riskScore || 0) >= 60 ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600')

function num(v) { return v == null ? '—' : Number(v).toFixed(1) }
function courseLevel(c) {
  if (c.riskLevel) return ({ RED: '高风险', ORANGE: '中风险', YELLOW: '低风险', GREEN: '正常' })[c.riskLevel] || c.riskLevel
  if (c.hardFail || c.fail) return '高风险'
  if (c.totalScore != null && Number(c.totalScore) < 60) return '低风险'
  return '正常'
}
function courseTag(c) {
  const lv = courseLevel(c)
  if (lv === '高风险') return 'bg-red-50 text-red-600'
  if (lv === '中风险') return 'bg-amber-50 text-amber-600'
  if (lv === '低风险') return 'bg-yellow-50 text-yellow-700'
  return 'bg-emerald-50 text-emerald-600'
}
function courseReport(c) {
  if (!c.teachingClassId) return null
  return reports.value.find((r) => r.teachingClassId === c.teachingClassId)
}

async function loadReports() {
  if (props.mode === 'student') {
    try { reports.value = await reportApi.list() } catch (e) { reportMessage.value = e?.message || '报告读取失败' }
    return
  }
  if (!props.student?.studentId) return
  loadingReports.value = true
  reportMessage.value = ''
  try {
    reports.value = await staffReportApi.list(props.student.studentId)
  } catch (e) {
    reportMessage.value = e?.message || '报告读取失败'
  } finally {
    loadingReports.value = false
  }
}

async function toggleReport(c) {
  const r = courseReport(c)
  if (r) {
    if (activeReport.value?.id === r.id) {
      activeReport.value = null
      return
    }
    try {
      activeReport.value = await reportApi.detail(r.id)
    } catch (e) {
      reportMessage.value = e?.message || '报告详情读取失败'
    }
    return
  }
  try {
    const created = props.mode === 'student'
      ? await reportApi.generate({ teachingClassId: c.teachingClassId, reportType: 'COURSE' })
      : await staffReportApi.generate(props.student.studentId, c.teachingClassId)
    reports.value = [created, ...reports.value]
    activeReport.value = created
  } catch (e) {
    reportMessage.value = e?.message || '报告生成失败'
  }
}

async function downloadCourse(c) {
  const r = courseReport(c)
  if (!r) return
  try {
    if (props.mode === 'student') await reportApi.pdf(r.id)
    else await staffReportApi.pdf(props.student.studentId, r.id)
  } catch (e) {
    reportMessage.value = e?.message || 'PDF 导出失败'
  }
}

async function exportPdf() {
  const sr = activeReport.value || reports.value[0]
  if (props.mode === 'student') {
    if (sr) await reportApi.pdf(sr.id)
    else window.print()
    return
  }
  const r = activeReport.value || reports.value[0]
  if (!r) {
    reportMessage.value = '暂无独立报告，请先点击课程「生成报告」'
    return
  }
  try {
    if (props.mode === 'student') await reportApi.pdf(r.id)
    else await staffReportApi.pdf(props.student.studentId, r.id)
  } catch (e) {
    reportMessage.value = e?.message || 'PDF 导出失败'
  }
}

watch(
  () => props.open,
  (v) => {
    if (!v) return
    activeReport.value = null
    reports.value = []
    loadReports()
  }
)

</script>
