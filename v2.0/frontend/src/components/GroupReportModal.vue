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

            <div v-if="report.runId" class="rounded-xl border border-slate-200 px-4 py-3">
              <div class="flex items-center justify-between gap-2 mb-2">
                <p class="text-xs font-medium text-slate-700">生成链路</p>
                <span class="text-[10px] text-slate-400">run #{{ report.runId }} · {{ report.runStatus || '—' }}</span>
              </div>
              <div v-if="report.runSteps && report.runSteps.length" class="space-y-1.5">
                <div v-for="s in report.runSteps" :key="s.stepNo"
                     class="flex items-center gap-2 text-[11px] text-slate-600">
                  <span class="w-5 h-5 rounded-full bg-brand-50 text-brand-700 flex items-center justify-center shrink-0">{{ s.stepNo }}</span>
                  <span class="font-medium text-slate-700">{{ s.name }}</span>
                  <span class="text-slate-400">{{ s.stepRole || s.toolName || '—' }}</span>
                  <span class="ml-auto text-slate-400">{{ s.durationMs == null ? '—' : s.durationMs + 'ms' }}</span>
                  <span class="tag" :class="s.validationStatus === 'PASS' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
                    {{ s.validationStatus || s.gateStatus || '—' }}
                  </span>
                </div>
              </div>
              <p v-else class="text-[10px] text-slate-400">运行记录已关联，但该报告没有步骤明细。</p>
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
            </section>

            <p class="text-[10px] text-slate-400 border-t border-dashed border-slate-200 pt-3 leading-5">
              生成时间 {{ fmt(report.createTime) }} ｜ 报告里的数字均来自系统判定与成绩数据，可逐项对账；PDF 会保留本页脚。
            </p>
          </template>
    </div>
  </ReportModalShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { groupReportApi } from '../api/ai'
import ReportModalShell from './ReportModalShell.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  scope: { type: String, default: 'CLASS' },
  targetId: { type: [Number, String], default: null },
  title: { type: String, default: '' }
})
const emit = defineEmits(['close', 'openStudent'])

const report = ref(null)
const busy = ref(false)
const error = ref('')
const showChecks = ref(false)

const allChecksOk = computed(() =>
  !!report.value && (report.value.checksTotal || 0) === (report.value.checksPassed || 0))
const scopeLabel = computed(() => props.scope === 'ADMIN_CLASS'
  ? '行政班视角（一个班 × 多门课）'
  : (props.scope === 'OFFERING' ? '课程视角（一门课 × 我教的全部教学班）' : '教学班视角（一门课 × 一个班）'))
const sourceLabel = computed(() => report.value?.source === 'TEMPLATE' ? '规则生成' : 'AI 生成')

function fmt(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
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
  error.value = ''
  try {
    report.value = await groupReportApi.generate(props.scope, props.targetId)
  } catch (e) {
    error.value = e?.message || '生成失败'
  } finally {
    busy.value = false
  }
}

watch(
  () => [props.open, props.targetId, props.scope],
  ([isOpen]) => { if (isOpen) loadLatest() },
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

