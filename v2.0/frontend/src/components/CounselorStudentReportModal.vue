<template>
  <ReportModalShell :open="open" wide title="学生学期学情汇总报告"
                    :subtitle="`${student?.studentName || ''} · ${student?.studentNo || ''} · ${student?.adminClassName || ''} ｜ 当前学期全部课程`"
                    @close="close">
    <template #actions>
      <button class="btn-ghost text-sm" @click="printReport">导出 PDF</button>
    </template>

    <div v-if="!ready" class="rounded-xl border border-[#dbe7f2] bg-[#fbfdff] p-8 text-center">
      <div class="flex items-center justify-center gap-2 text-sm text-[#24435c]"><span class="w-4 h-4 rounded-full border-2 border-[#0f9d8a] border-t-transparent animate-spin"></span>正在汇总本学期全部课程…</div>
      <div class="h-1.5 rounded-full bg-slate-200 overflow-hidden mt-4 max-w-md mx-auto"><div class="h-full bg-[#0f9d8a] transition-all" :style="{ width: Math.round(generationStep / 7 * 100) + '%' }"></div></div>
      <p class="text-[11px] text-slate-400 mt-3">{{ generationMessages[Math.min(generationStep, generationMessages.length - 1)] }}</p>
    </div>

    <div v-else class="space-y-4">
      <div class="rounded-xl border border-slate-200 bg-[#fbfdff] p-4">
        <div class="flex items-center justify-between gap-2 flex-wrap">
          <div><p class="text-[11px] text-slate-400">{{ student?.termName || '当前学期' }}</p><h2 class="text-lg font-semibold text-[#17324b] mt-1">{{ student?.studentName }} · 学期学情汇总</h2></div>
          <span class="tag" :class="riskTag">{{ riskLabel }}</span>
        </div>
        <p class="text-sm text-slate-600 leading-7 mt-3">{{ summaryText }}</p>
        <div class="grid grid-cols-2 md:grid-cols-5 gap-2 mt-3">
          <div class="rounded-lg bg-white border border-[#eef4f9] p-2.5"><p class="text-[10px] text-slate-400">课程</p><b class="text-lg text-slate-700">{{ courses.length }}</b></div>
          <div class="rounded-lg bg-white border border-[#eef4f9] p-2.5"><p class="text-[10px] text-slate-400">风险课程</p><b class="text-lg text-red-600">{{ riskCount }}</b></div>
          <div class="rounded-lg bg-white border border-[#eef4f9] p-2.5"><p class="text-[10px] text-slate-400">硬门槛</p><b class="text-lg text-red-600">{{ hardCount }}</b></div>
          <div class="rounded-lg bg-white border border-[#eef4f9] p-2.5"><p class="text-[10px] text-slate-400">累计缺课</p><b class="text-lg text-slate-700">{{ absentTotal }}</b></div>
          <div class="rounded-lg bg-white border border-[#eef4f9] p-2.5"><p class="text-[10px] text-slate-400">平均总评</p><b class="text-lg text-slate-700">{{ avgScore }}</b></div>
        </div>
      </div>

      <div class="grid lg:grid-cols-3 gap-3">
        <div class="rounded-xl border border-[#e6edf5] bg-white p-4">
          <div class="flex justify-between mb-2"><p class="text-xs font-semibold text-[#24435c]">课程风险分布</p><span class="text-[10px] text-slate-400">{{ courses.length }} 门</span></div>
          <div class="flex items-center gap-4">
            <div class="w-24 h-24 rounded-full relative shrink-0" :style="{ background: donutStyle }"><div class="absolute inset-4 rounded-full bg-white"></div><div class="absolute inset-0 flex flex-col items-center justify-center"><b class="text-lg">{{ courses.length }}</b><span class="text-[9px] text-slate-400">课程</span></div></div>
            <div class="space-y-1.5 text-[11px] flex-1"><div class="flex justify-between"><span class="text-slate-500">红 / 橙</span><b class="text-red-600">{{ riskCount }}</b></div><div class="flex justify-between"><span class="text-slate-500">正常</span><b class="text-emerald-600">{{ normalCount }}</b></div></div>
          </div>
        </div>
        <div class="rounded-xl border border-[#e6edf5] bg-white p-4 lg:col-span-2">
          <p class="text-xs font-semibold text-[#24435c] mb-2">各课程总评</p>
          <div v-for="c in courses" :key="'score-'+c.teachingClassId" class="mb-1.5">
            <div class="flex justify-between text-[10px] mb-1"><span class="text-slate-500 truncate">{{ c.courseName }}</span><b class="text-slate-700">{{ c.totalScore == null ? '未定稿' : c.totalScore }}</b></div>
            <div class="h-1.5 rounded-full bg-slate-100 overflow-hidden"><div class="h-full rounded-full" :class="c.fail ? 'bg-red-400' : 'bg-[#20b7a4]'" :style="{ width: Math.min(100, Number(c.totalScore || 0)) + '%' }"></div></div>
          </div>
          <p class="text-[10px] text-slate-400 mt-2">总评为当前已录入分项折算值；期末未计入时显示过程结果。</p>
        </div>
      </div>

      <div class="rounded-xl border border-[#e6edf5] bg-white p-4">
        <div class="flex justify-between mb-2"><h3 class="text-sm font-bold text-[#17324b]">一、学期课程总览</h3><span class="text-[10px] text-slate-400">共 {{ courses.length }} 门</span></div>
        <div class="overflow-x-auto border border-[#eef4f9] rounded-lg">
          <table><thead><tr><th>课程</th><th>教师</th><th class="text-right">缺课/允许</th><th class="text-right">扣分</th><th class="text-right">加权</th><th class="text-right">总评</th><th>结论</th></tr></thead><tbody>
            <tr v-for="c in courses" :key="'c-'+c.teachingClassId"><td>{{ c.courseName }}</td><td class="text-slate-500">{{ c.teacherName }}</td><td class="text-right" :class="c.hardFail ? 'text-red-600 font-medium' : ''">{{ c.absentCount ?? 0 }} / {{ c.allowedAbsent ?? 0 }}</td><td class="text-right">{{ c.deductPoints ?? '—' }}</td><td class="text-right">{{ c.weightedScore ?? '—' }}</td><td class="text-right" :class="c.fail ? 'text-red-600 font-medium' : ''">{{ c.totalScore ?? '未定稿' }}</td><td class="text-xs">{{ c.failReasonText || c.riskHint || '—' }}</td></tr>
            <tr v-if="!courses.length"><td colspan="7" class="py-6 text-center text-slate-400">当前学期没有选课记录</td></tr>
          </tbody></table>
        </div>
      </div>

      <div class="grid lg:grid-cols-2 gap-3">
        <div class="rounded-xl border border-[#e6edf5] bg-white p-4">
          <h3 class="text-sm font-bold text-[#17324b] mb-2">二、重点关注课程</h3>
          <div v-if="focusCourses.length" class="space-y-2">
            <div v-for="c in focusCourses" :key="'f-'+c.teachingClassId" class="rounded-lg border border-[#f3e3e3] bg-[#fffafa] p-2.5">
              <div class="flex justify-between gap-2"><b class="text-xs text-slate-700">{{ c.courseName }}</b><span class="tag bg-red-50 text-red-600">{{ c.riskLevel === 'RED' ? '红灯' : (c.riskLevel === 'ORANGE' ? '橙色' : '黄色') }}</span></div>
              <p class="text-[10px] text-slate-500 mt-1">缺课 {{ c.absentCount ?? 0 }}/{{ c.allowedAbsent ?? 0 }} · 总评 {{ c.totalScore ?? '未定稿' }} · {{ c.failReasonText || '需关注' }}</p>
            </div>
          </div>
          <p v-else class="text-xs text-slate-400 py-4">当前学期没有风险课程。</p>
        </div>
        <div class="rounded-xl border border-[#e6edf5] bg-white p-4">
          <h3 class="text-sm font-bold text-[#17324b] mb-2">三、处理记录</h3>
          <div v-if="interventions.length" class="space-y-2">
            <div v-for="r in interventions.slice(0, 4)" :key="'i-'+r.id" class="rounded-lg bg-[#f8fbfd] p-2.5"><p class="text-[10px] text-slate-500">{{ fmt(r.createTime) }} · {{ r.actionTypeText }}</p><p class="text-xs text-slate-600 mt-1">{{ r.content }}</p></div>
          </div>
          <p v-else class="text-xs text-slate-400 py-4">暂无处理记录。</p>
        </div>
      </div>

      <div class="rounded-xl border border-[#dbe7f2] bg-[#fbfdff] p-4">
        <h3 class="text-sm font-bold text-[#17324b]">四、辅导员动作清单</h3>
        <div class="space-y-1.5 mt-2">
          <p v-for="(a, i) in actions" :key="'a-'+i" class="text-xs text-slate-600">□ {{ a }}</p>
        </div>
      </div>
    </div>

    <GenerationOverlay :open="generating" :step="generationStep" title="正在生成学生学期汇总"
                       subtitle="汇总当前学期全部课程、风险与干预记录" :agents="generationAgents"
                       :messages="generationMessages" flow="课程汇总 → 风险研判 → 成绩考勤 → 报告生成" />
  </ReportModalShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import ReportModalShell from './ReportModalShell.vue'
import GenerationOverlay from './GenerationOverlay.vue'

const props = defineProps({
  open: Boolean,
  student: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['close'])
const ready = ref(false)
const generating = ref(false)
const generationStep = ref(0)
const generationMessages = [
  '正在读取当前学期全部课程…',
  '正在汇总风险等级与成绩…',
  '正在核对考勤与硬门槛…',
  '正在整理重点关注课程…',
  '正在生成学期汇总报告…'
]
const generationAgents = [
  { icon: '🩺', name: '课程汇总' },
  { icon: '📊', name: '成绩分析' },
  { icon: '🧭', name: '风险研判' },
  { icon: '📝', name: '干预汇总' },
  { icon: '✅', name: '质量校验' },
  { icon: '📄', name: '报告生成' }
]
const courses = computed(() => props.student?.courses || [])
const interventions = computed(() => props.student?.interventions || [])
const riskCourses = computed(() => courses.value.filter(c => ['RED', 'ORANGE', 'YELLOW'].includes(c.riskLevel) || c.fail))
const riskCount = computed(() => riskCourses.value.length)
const hardCount = computed(() => courses.value.filter(c => c.hardFail).length)
const normalCount = computed(() => courses.value.length - riskCount.value)
const absentTotal = computed(() => courses.value.reduce((n, c) => n + Number(c.absentCount || 0), 0))
const avgScore = computed(() => {
  const vals = courses.value.map(c => Number(c.totalScore)).filter(Number.isFinite)
  return vals.length ? (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1) : '—'
})
const focusCourses = computed(() => riskCourses.value.slice().sort((a, b) => Number(a.totalScore ?? 999) - Number(b.totalScore ?? 999)).slice(0, 4))
const riskLabel = computed(() => hardCount.value ? '高风险' : riskCount.value ? '中风险' : '正常')
const riskTag = computed(() => hardCount.value ? 'bg-red-50 text-red-600' : riskCount.value ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600')
const summaryText = computed(() => `本学期共 ${courses.value.length} 门课程，${riskCount.value} 门存在风险，${hardCount.value} 门触发硬门槛；累计缺课 ${absentTotal.value} 次。`)
const donutStyle = computed(() => {
  const total = Math.max(1, courses.value.length)
  const risk = riskCount.value / total * 100
  return `conic-gradient(#ef4444 0 ${risk}%, #22c55e ${risk}% 100%)`
})
const actions = computed(() => {
  const arr = []
  if (hardCount.value) arr.push(`优先核对 ${hardCount.value} 门硬门槛课程的缺课与考试资格`)
  if (riskCount.value) arr.push(`逐一约谈 ${riskCount.value} 门风险课程对应的学习问题`)
  if (props.student?.interventions?.some(i => i.effective === 'INEFFECTIVE')) arr.push('复核无效干预，必要时上报教务')
  arr.push('T+7 复评本学期风险课程变化')
  return arr
})
function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '—' }
function close() { emit('close') }
function printReport() { window.print() }
let timer = null
watch(() => props.open, (v) => {
  if (!v) { ready.value = false; generating.value = false; generationStep.value = 0; clearInterval(timer); return }
  ready.value = false
  generating.value = true
  generationStep.value = 0
  clearInterval(timer)
  timer = setInterval(() => { if (generationStep.value < 7) generationStep.value++ }, 450)
  setTimeout(() => { clearInterval(timer); generating.value = false; ready.value = true }, 3200)
})
</script>
