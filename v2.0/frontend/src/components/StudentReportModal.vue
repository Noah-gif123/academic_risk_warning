<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-[60] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 overflow-hidden print:static print:p-0 print:bg-white print:block"
         @click.self="$emit('close')">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden my-0 print:shadow-none print:rounded-none print:my-0 print:max-w-none print:max-h-none print:overflow-visible">
        <div class="shrink-0 sticky top-0 z-10 bg-white/95 backdrop-blur border-b border-slate-200 px-5 py-3 flex items-center justify-between gap-3 print:static print:border-0">
          <div class="min-w-0">
            <h2 class="font-semibold text-slate-800">学生报告</h2>
            <p class="text-xs text-slate-400 mt-0.5">
              {{ student?.studentName }} · {{ student?.studentNo }} · {{ student?.adminClassName }}
              <span class="ml-1">｜ {{ mode === 'teacher' ? '教师视角' : (mode === 'student' ? '学生视角' : '辅导员视角') }}</span>
            </p>
          </div>
          <div class="flex items-center gap-2 print:hidden">
            <button class="btn-ghost text-sm" @click="exportPdf">导出 PDF</button>
            <button class="text-slate-400 hover:text-slate-600 px-2" title="关闭" @click="$emit('close')">✕</button>
          </div>
        </div>

        <div class="p-5 space-y-4 overflow-y-auto print:p-0 print:overflow-visible">
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
            {{ mode === 'student' ? '当前入口使用本学期课程数据；PDF 使用浏览器打印。' : '教师/辅导员读取 ai_report 独立快照；PDF 由服务端 OpenPDF 生成，可选择保存目录。' }}
            报告数字可回溯到 risk_state / 成绩 / 考勤。
          </p>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { staffReportApi } from '../api/ai'

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
  if (props.mode === 'student' || !props.student?.studentId) return
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
    activeReport.value = activeReport.value?.id === r.id ? null : r
    return
  }
  try {
    const created = await staffReportApi.generate(props.student.studentId, c.teachingClassId)
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
    await staffReportApi.pdf(props.student.studentId, r.id)
  } catch (e) {
    reportMessage.value = e?.message || 'PDF 导出失败'
  }
}

async function exportPdf() {
  if (props.mode === 'student') {
    window.print()
    return
  }
  const r = activeReport.value || reports.value[0]
  if (!r) {
    reportMessage.value = '暂无独立报告，请先点击课程「生成报告」'
    return
  }
  try {
    await staffReportApi.pdf(props.student.studentId, r.id)
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
