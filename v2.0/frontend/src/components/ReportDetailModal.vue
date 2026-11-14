<template>
  <ReportModalShell :open="open && !!report" :title="report ? report.courseName + ' · 学习评价' : '学习评价'"
                    :subtitle="report ? `${report.termName || ''} · ${report.className || ''} · 报告 #${report.id}` : ''"
                    @close="$emit('close')">
    <template #actions>
      <button class="btn-ghost text-sm" @click="pdf">导出 PDF</button>
    </template>
    <div v-if="report" class="space-y-4">
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="flex items-center justify-between">
          <span class="tag" :class="report.riskLevel === 'RED' ? 'bg-red-50 text-red-600' : 'bg-amber-50 text-amber-600'">{{ report.riskLevel || '—' }}</span>
          <span class="text-sm text-slate-500">总分 {{ report.totalScore ?? '—' }}</span>
        </div>
        <p class="text-sm text-slate-600 mt-2 whitespace-pre-line">{{ report.summary }}</p>
      </div>
      <div v-if="report.metrics?.dimensions?.length" class="rounded-xl border border-slate-200 p-4">
        <p class="text-sm font-medium text-slate-700 mb-2">六维画像</p>
        <div v-for="d in report.metrics.dimensions" :key="d.key" class="grid grid-cols-[64px_1fr_78px] gap-2 items-center text-xs mb-1">
          <span class="text-slate-500">{{ d.label }}</span>
          <div class="h-2 rounded-full bg-slate-100 overflow-hidden flex gap-0.5">
            <div class="h-full bg-brand-500" :style="{ width: (d.student ?? 0) + '%' }"></div>
            <div class="h-full bg-slate-300" :style="{ width: (d.classAvg ?? 0) + '%' }"></div>
          </div>
          <span class="text-right text-slate-600">{{ d.student ?? '—' }} / {{ d.classAvg ?? '—' }}</span>
        </div>
      </div>
      <div v-for="(s, i) in report.sections" :key="i" class="rounded-lg bg-slate-50 px-4 py-3">
        <p class="text-sm font-medium text-slate-700 mb-1">{{ s.title }}</p>
        <p class="text-sm text-slate-600 whitespace-pre-line leading-relaxed">{{ s.text }}</p>
      </div>
    </div>
  </ReportModalShell>
</template>
<script setup>
import { reportApi } from '../api/ai'
import ReportModalShell from './ReportModalShell.vue'
const props = defineProps({ open: Boolean, report: Object })
defineEmits(['close'])
async function pdf() {
  if (props.report?.id) await reportApi.pdf(props.report.id)
}
</script>
