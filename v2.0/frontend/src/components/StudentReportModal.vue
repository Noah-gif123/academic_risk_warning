<template>
  <ReportModalShell :open="open" title="学生报告" wide
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
              <div>
                <div class="flex items-center justify-between mb-2"><h3 class="text-[15px] font-bold text-[#17324b]">一、六维画像对比</h3><span class="text-[10px] text-slate-400">学生 / 班级均值</span></div>
                <div v-if="radarDims.length" class="grid md:grid-cols-2 gap-3 items-center">
                  <svg viewBox="0 0 340 280" class="w-full max-w-[360px] mx-auto">
                    <polygon v-for="level in [0.25,0.5,0.75,1]" :key="'grid-'+level" :points="radarGrid(level)" fill="none" stroke="#e6edf5" />
                    <line v-for="(d, i) in radarDims" :key="'axis-'+i" x1="170" y1="140" :x2="radarPoint(i, 1).x" :y2="radarPoint(i, 1).y" stroke="#eef4f9" />
                    <polygon :points="radarPolygon('classAvg')" fill="#b9cad9" fill-opacity=".25" stroke="#b9cad9" stroke-width="1.5" />
                    <polygon :points="radarPolygon('student')" fill="#0f9d8a" fill-opacity=".22" stroke="#0f9d8a" stroke-width="2" />
                    <text v-for="(d, i) in radarDims" :key="'label-'+i" :x="radarLabel(i).x" :y="radarLabel(i).y" text-anchor="middle" font-size="9" fill="#607586">{{ d.label }}</text>
                  </svg>
                  <div class="space-y-2">
                    <div v-for="d in radarDims" :key="'full-'+d.key" class="grid grid-cols-[64px_1fr_80px] gap-2 items-center text-xs">
                      <span class="text-slate-500">{{ d.label }}</span>
                      <div class="relative h-2 rounded-full bg-slate-100 overflow-hidden"><div class="absolute inset-y-0 left-0 bg-slate-300" :style="{ width: Math.min(100, Number(d.classAvg ?? 0)) + '%' }"></div><div class="absolute inset-y-0 left-0 bg-[#0f9d8a]" :style="{ width: Math.min(100, Number(d.student ?? 0)) + '%' }"></div></div>
                      <span class="text-right text-slate-600">{{ d.student ?? '—' }} / {{ d.classAvg ?? '—' }}</span>
                    </div>
                    <p class="text-[10px] text-slate-400 leading-5">实线为本人，浅色为班级均值；0–100 归一化。</p>
                  </div>
                </div>
                <p v-else class="text-xs text-slate-400">该报告没有六维指标。</p>
              </div>

              <h3 class="text-[15px] font-bold text-[#17324b] mt-1">二、课程与预警结论</h3>
              <div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-[#f7fbfd] p-4 flex items-center gap-5">
                <div><p class="text-4xl font-semibold" :class="riskColor">{{ num(activeCourse?.riskScore ?? fact.riskScore ?? activeReport.totalScore) }}</p><p class="text-[10px] text-slate-400 mt-1">课程风险分 / 100</p></div>
                <div class="flex-1 text-sm text-slate-600 leading-6"><span class="tag" :class="riskTagClass">{{ riskLevelText }}</span><br>{{ activeReport.summary }}<br><span class="text-[11px] text-slate-400">有效课次 {{ fact.effectiveSessions ?? '—' }} · 置信度 {{ fact.confidenceLevel ?? '—' }}<span v-if="fact.classRank"> · 班内排名 {{ fact.classRank }} / {{ fact.classSize }}</span></span></div>
              </div>

              <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">缺课 / 允许</p><p class="text-lg font-semibold" :class="activeCourse?.hardFail ? 'text-red-600' : 'text-slate-700'">{{ activeCourse?.absentCount ?? '—' }} / {{ activeCourse?.allowedAbsent ?? '—' }}</p></div>
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">过程分 / 总评</p><p class="text-lg font-semibold text-slate-700">{{ activeCourse?.processScore ?? fact.processScore ?? '—' }} / {{ activeCourse?.totalScore ?? fact.totalScore ?? '—' }}</p></div>
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">考勤扣分</p><p class="text-lg font-semibold text-slate-700">{{ activeCourse?.deductPoints ?? fact.deductionPoints ?? '—' }}</p></div>
                <div class="rounded-lg bg-[#f8fbfd] border border-[#eef4f9] p-3"><p class="text-[10px] text-slate-400">加权分</p><p class="text-lg font-semibold text-slate-700">{{ activeCourse?.weightedScore ?? fact.weightedScore ?? '—' }}</p></div>
              </div>

              <div class="rounded-xl border border-[#e6edf5] overflow-hidden">
                <div class="px-4 py-2 bg-[#f7fbfd] border-b border-[#e6edf5]"><h3 class="text-[15px] font-bold text-[#17324b]">三、成绩与考勤明细</h3></div>
                <table>
                  <thead><tr><th>项目</th><th>当前值</th><th>说明</th></tr></thead>
                  <tbody>
                    <tr><td>缺课 / 允许</td><td :class="activeCourse?.hardFail ? 'text-red-600 font-medium' : ''">{{ activeCourse?.absentCount ?? '—' }} / {{ activeCourse?.allowedAbsent ?? '—' }}</td><td>{{ activeCourse?.hardFail ? '已超过允许上限，触发硬性不合格' : '缺课在允许范围内' }}</td></tr>
                    <tr><td>迟到 / 请假</td><td>{{ activeCourse?.lateCount ?? fact.lateCount ?? '—' }} / {{ activeCourse?.leaveCount ?? fact.leaveCount ?? '—' }}</td><td>按课程考勤记录汇总</td></tr>
                    <tr><td>过程分</td><td>{{ activeCourse?.processScore ?? fact.processScore ?? '—' }}</td><td>作业、测验、课堂表现等已录项加权</td></tr>
                    <tr><td>当前总评</td><td>{{ activeCourse?.totalScore ?? fact.totalScore ?? activeReport.totalScore ?? '—' }}</td><td>加权分 − 考勤扣分</td></tr>
                    <tr><td>加权分 / 扣分</td><td>{{ activeCourse?.weightedScore ?? fact.weightedScore ?? '—' }} / {{ activeCourse?.deductPoints ?? fact.deductionPoints ?? '—' }}</td><td>过程性成绩与考勤扣分</td></tr>
                  </tbody>
                </table>
              </div>

              <h3 class="text-[15px] font-bold text-[#17324b] mt-1">四、成绩趋势与考核项对比</h3>
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
                    <div class="h-2 rounded-full bg-slate-100 overflow-hidden"><div class="h-full bg-[#0f9d8a]" :style="{ width: barWidth(it.rate ?? it.score) + '%' }"></div></div>
                    <span class="text-right text-slate-600">{{ it.score ?? '—' }} / {{ it.weight ?? '—' }}</span>
                  </div>
                </div>
              </div>

              <div v-if="pipeline.length" class="rounded-xl border border-[#e6edf5] p-4">
                <h3 class="text-[15px] font-bold text-[#17324b] mb-2">五、多智能体研判链路</h3>
                <div class="flex flex-wrap items-center gap-2 text-[10px]">
                  <template v-for="(s, i) in pipeline" :key="'pipe-'+i">
                    <span class="rounded-full border border-[#dbe7f2] bg-[#f7fbfd] px-2 py-1 text-[#3c6f9b]">{{ s.name }} <b class="text-[#0f9d8a]">{{ s.status }}</b></span>
                    <span v-if="i < pipeline.length - 1" class="text-slate-300">→</span>
                  </template>
                </div>
              </div>

              <h3 class="text-[15px] font-bold text-[#17324b] mt-1">六、报告正文</h3>
              <div class="space-y-3">
                <div v-for="(sec, si) in activeReport.sections" :key="'full-sec-'+si" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-4">
                  <h3 class="text-[13px] font-bold text-[#2c465c]">{{ sec.title }}</h3>
                  <p class="text-sm text-slate-600 leading-7 whitespace-pre-line mt-1">{{ sec.text }}</p>
                </div>
              </div>

              <div class="rounded-xl border border-[#dbe7f2] bg-[#fbfdff] p-4">
                <div class="flex items-center justify-between mb-3">
                  <h3 class="text-[15px] font-bold text-[#17324b]">七、动作清单</h3>
                  <span class="text-[10px] text-slate-400">来自本次报告的可执行建议</span>
                </div>
                <div v-if="activeReport.actions?.length" class="space-y-2">
                  <div v-for="(a, i) in activeReport.actions" :key="'action-'+i"
                       class="flex items-center gap-3 rounded-lg border border-[#e6edf5] bg-white px-3 py-2.5">
                    <span class="text-base">{{ actionIcon(a.type) }}</span>
                    <div class="flex-1 min-w-0">
                      <p class="text-xs font-medium text-[#2c465c]">{{ a.label }}</p>
                      <p class="text-[10px] text-slate-400 mt-0.5">{{ actionDesc(a.type) }}</p>
                    </div>
                    <button class="btn-ghost text-xs shrink-0" @click="runAction(a)">去做 →</button>
                  </div>
                </div>
                <p v-else class="text-xs text-slate-400">暂无结构化动作，请参考正文中的「下一步做什么」。</p>
                <p class="text-[10px] text-slate-400 mt-3 pt-2 border-t border-dashed border-[#e6edf5]">正文里的「下一步做什么」是详细说明；上方是可点击执行的动作。</p>
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
                <button class="text-xs text-brand-600 hover:underline" :disabled="generating" @click="toggleReport(c)">
                  {{ generating ? '生成中…' : (courseReport(c) ? '预览' : '生成报告') }}
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
                    <div class="relative h-1.5 rounded-full bg-slate-200 overflow-hidden">
                      <div class="absolute inset-y-0 left-0 bg-slate-400" :style="{ width: Math.min(100, Number(d.classAvg ?? 0)) + '%' }"></div>
                      <div class="absolute inset-y-0 left-0 bg-brand-500" :style="{ width: Math.min(100, Number(d.student ?? 0)) + '%' }"></div>
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

  <Teleport to="body">
    <div v-if="generating" class="gen-overlay fixed inset-0 z-[90] flex items-center justify-center p-4">
      <div class="gen-card relative w-full max-w-[760px] rounded-[26px] overflow-hidden text-white">
        <div class="gen-aurora"></div>
        <div class="gen-grid"></div>
        <div class="relative p-7 md:p-9">
          <p class="text-[10px] tracking-[.28em] text-[#7dd3c7]">COURSE INTELLIGENCE · GENERATING</p>
          <h3 class="text-2xl font-semibold mt-3 text-white">正在生成课程学情报告</h3>
          <p class="text-sm text-slate-300 mt-2">{{ generationMessages[Math.min(generationStep, generationMessages.length - 1)] }}</p>

          <div class="grid grid-cols-4 md:grid-cols-7 gap-2 mt-6">
            <div v-for="(a, i) in generationAgents" :key="a.name"
                 class="gen-node" :class="generationStep > i ? 'done' : (generationStep === i ? 'active' : '')">
              <span class="text-xl">{{ a.icon }}</span>
              <p class="text-[10px] mt-1">{{ a.name }}</p>
              <p class="text-[8px] mt-1" :class="generationStep > i ? 'text-[#7dd3c7]' : 'text-slate-500'">
                {{ generationStep > i ? 'DONE' : (generationStep === i ? 'RUNNING' : 'WAIT') }}
              </p>
            </div>
          </div>

          <div class="mt-7">
            <div class="gen-track"><div class="gen-fill" :style="{ width: Math.min(100, generationStep / 7 * 100) + '%' }"></div></div>
            <div class="flex items-center justify-between mt-2 text-[10px] text-slate-400"><span>数据 → 研判 → 取证 → 生成 → 校验</span><span>{{ Math.min(100, Math.round(generationStep / 7 * 100)) }}%</span></div>
          </div>

          <div class="flex items-center gap-2 mt-6 text-[11px] text-slate-300">
            <span class="gen-dot"></span> Agent 正在协同工作，请稍候，不要关闭窗口
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { staffReportApi, reportApi } from '../api/ai'
import ReportModalShell from './ReportModalShell.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  student: { type: Object, default: () => ({}) },
  courses: { type: Array, default: () => [] },
  mode: { type: String, default: 'counselor' },
  generateOnOpen: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])
const router = useRouter()

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
const radarDims = computed(() => activeReport.value?.metrics?.dimensions || [])
function radarPoint(i, ratio) {
  const n = Math.max(1, radarDims.value.length)
  const angle = -Math.PI / 2 + i * Math.PI * 2 / n
  const r = 86 * ratio
  return { x: 170 + Math.cos(angle) * r, y: 140 + Math.sin(angle) * r }
}
function radarPolygon(key) {
  return radarDims.value.map((d, i) => {
    const p = radarPoint(i, Math.max(0, Math.min(1, Number(d[key] ?? 0) / 100)))
    return `${p.x},${p.y}`
  }).join(' ')
}
function radarGrid(ratio) {
  return radarDims.value.map((d, i) => {
    const p = radarPoint(i, ratio)
    return `${p.x},${p.y}`
  }).join(' ')
}
function radarLabel(i) {
  const n = Math.max(1, radarDims.value.length)
  const angle = -Math.PI / 2 + i * Math.PI * 2 / n
  return { x: 170 + Math.cos(angle) * 108, y: 140 + Math.sin(angle) * 103 + 3 }
}
function trendPoints(rows, key) {
  if (!rows.length) return []
  return rows.map((r, i) => ({ x: rows.length === 1 ? 300 : 10 + i * 600 / (rows.length - 1), y: 130 - Math.max(0, Math.min(100, Number(r[key] ?? 0))) * 1.2 }))
}
function barWidth(v) {
  let n = Number(v ?? 0)
  if (!Number.isFinite(n)) n = 0
  if (n > 0 && n <= 1) n *= 100
  return Math.max(0, Math.min(100, n))
}
function trendPath(rows, key) {
  return trendPoints(rows, key).map(p => `${p.x},${p.y}`).join(' ')
}
const reportMessage = ref('')
const loadingReports = ref(false)
const generating = ref(false)
const generationStep = ref(0)
const generationAgents = [
  { icon: '🩺', name: '数据体检' },
  { icon: '📊', name: '数据分析' },
  { icon: '🧭', name: '学情研判' },
  { icon: '📚', name: 'RAG 取证' },
  { icon: '💡', name: '报告生成' },
  { icon: '✅', name: '质量校验' },
  { icon: '📄', name: '导出准备' }
]
const generationMessages = [
  '正在建立数据快照，核对课程与成绩口径…',
  '正在计算六维画像与班级均值…',
  '正在研判风险等级与关键归因…',
  '正在检索课程资料并绑定引用…',
  '正在生成报告正文与学习建议…',
  '正在做数字对账与质量校验…',
  '报告已生成，正在准备预览…'
]
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
  return generateNow(c)
}

async function generateNow(c) {
  if (!c) return
  try {
    generating.value = true
    generationStep.value = 1
    const startedAt = Date.now()
    const timer = setInterval(() => {
      if (generationStep.value < 7) generationStep.value++
    }, 600)
    const created = props.mode === 'student'
      ? await reportApi.generate({ teachingClassId: c.teachingClassId, reportType: 'COURSE' })
      : await staffReportApi.generate(props.student.studentId, c.teachingClassId)
    const remain = Math.max(0, 3200 - (Date.now() - startedAt))
    if (remain) await new Promise(resolve => setTimeout(resolve, remain))
    clearInterval(timer)
    generationStep.value = 7
    reports.value = [created, ...reports.value]
    activeReport.value = created
  } catch (e) {
    reportMessage.value = e?.message || '报告生成失败'
  } finally {
    setTimeout(() => { generating.value = false; generationStep.value = 0 }, 500)
  }
}

function actionIcon(type) {
  if (type === 'GUIDE') return '📘'
  if (type === 'ASK_TEACHER' || type === 'ATTENDANCE_CHECK' || type === 'ATTENDANCE_REPAIR') return '💬'
  if (type === 'EXAM_PLAN') return '🎯'
  if (type === 'HOMEWORK_FILL') return '📝'
  if (type === 'REDO') return '🔁'
  return '✅'
}
function actionDesc(type) {
  if (type === 'GUIDE') return '打开本课程导学，核对学习要求与考核方式'
  if (type === 'ASK_TEACHER') return '带着报告里的具体问题找任课老师答疑'
  if (type === 'ATTENDANCE_CHECK') return '先核对缺课记录，确认是否影响期末资格'
  if (type === 'ATTENDANCE_REPAIR') return '本周内找任课教师说明原因并提出补救'
  if (type === 'EXAM_PLAN') return '按报告里的需考分制定复习清单'
  if (type === 'HOMEWORK_FILL') return '优先补交未交作业/实验，把过程分拉回'
  if (type === 'REDO') return '重做薄弱题组，补上当前差距'
  return '按报告建议执行，并在 T+7 复评时回看'
}
function runAction(a) {
  const p = a?.payload || {}
  const tcId = p.teachingClassId || activeCourse.value?.teachingClassId || activeReport.value?.teachingClassId
  activeReport.value = null
  emit('close')
  if (a?.type === 'GUIDE' && tcId) {
    router.push('/student/course/' + tcId)
  } else if (a?.type === 'ASK_TEACHER' || a?.type === 'ATTENDANCE_CHECK' || a?.type === 'ATTENDANCE_REPAIR') {
    router.push({ path: '/student/qa', query: { teachingClassId: tcId || '' } })
  } else if (a?.type === 'EXAM_PLAN' || a?.type === 'HOMEWORK_FILL') {
    router.push('/student/plan')
  } else if (a?.type === 'REDO' && tcId) {
    router.push('/student/course/' + tcId)
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
  async (v) => {
    if (!v) return
    activeReport.value = null
    reports.value = []
    await loadReports()
    if (props.generateOnOpen && props.courses?.length) {
      await generateNow(props.courses[0])
    }
  }
)

</script>

<style scoped>
.gen-overlay { background: rgba(3, 12, 22, .72); backdrop-filter: blur(14px); }
.gen-card { background: radial-gradient(circle at 15% 0%, #123b4b 0, #081c2b 45%, #061520 100%); box-shadow: 0 30px 100px rgba(0,0,0,.55), inset 0 1px 0 rgba(255,255,255,.12); }
.gen-aurora { position: absolute; width: 520px; height: 520px; left: -160px; top: -240px; border-radius: 50%; background: conic-gradient(from 0deg, transparent, #20b7a4, #3b82c4, transparent); filter: blur(45px); opacity: .55; animation: gen-spin 7s linear infinite; }
.gen-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(125,211,199,.08) 1px, transparent 1px), linear-gradient(90deg, rgba(125,211,199,.08) 1px, transparent 1px); background-size: 38px 38px; mask-image: linear-gradient(to bottom, black, transparent 80%); }
.gen-node { border: 1px solid rgba(125,211,199,.16); background: rgba(8,34,48,.72); border-radius: 12px; padding: 9px 5px; text-align: center; transition: .35s; opacity: .45; transform: translateY(4px); }
.gen-node.active { opacity: 1; border-color: #20b7a4; background: rgba(15,157,138,.2); transform: translateY(0); animation: gen-pulse 1.15s ease-in-out infinite; }
.gen-node.done { opacity: 1; border-color: rgba(125,211,199,.45); background: rgba(15,157,138,.12); transform: translateY(0); }
.gen-track { height: 5px; border-radius: 999px; background: rgba(255,255,255,.1); overflow: hidden; }
.gen-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #20b7a4, #7dd3c7, #3b82c4); transition: width .6s ease; box-shadow: 0 0 18px rgba(32,183,164,.7); }
.gen-dot { width: 7px; height: 7px; border-radius: 50%; background: #20b7a4; box-shadow: 0 0 0 5px rgba(32,183,164,.14); animation: gen-pulse 1.2s infinite; }
@keyframes gen-spin { to { transform: rotate(360deg); } }
@keyframes gen-pulse { 50% { box-shadow: 0 0 24px rgba(32,183,164,.35); } }
</style>
